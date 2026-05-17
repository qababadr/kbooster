package com.badrqaba.kbooster.processor.validation

import com.badrqaba.kbooster.core.annotation.Validatable
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.*

class ValidationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("Kbooster validation processor started")

        val fieldRuleClass = ClassName(
            "com.badrqaba.kbooster.core.validation.core",
            "FieldRule"
        )

        val fieldErrorClass = ClassName(
            "com.badrqaba.kbooster.core.validation.core",
            "FieldError"
        )

        val validatorClass = ClassName(
            "com.badrqaba.kbooster.core.validation.core",
            "Validator"
        )

        val formStateClass = ClassName(
            "com.badrqaba.kbooster.core.validation.core",
            "FormState"
        )

        val symbols = resolver
            .getSymbolsWithAnnotation(Validatable::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        symbols.forEach { clazz ->

            val packageName = clazz.packageName.asString()
            val className = clazz.simpleName.asString()

            val validatorName = "${className}Validator"
            val formFunctionName =
                className.replaceFirstChar { it.lowercase() } + "Form"

            val modelClass = clazz.toClassName()
            val formStateType = formStateClass.parameterizedBy(modelClass)

            val rulesCode = mutableListOf<CodeBlock>()
            val extensionFunctions = mutableListOf<FunSpec>()
            val extensionProperties = mutableListOf<PropertySpec>()

            clazz.getAllProperties().forEach { prop ->

                val propName = prop.simpleName.asString()
                val capitalized = propName.replaceFirstChar { it.uppercase() }
                val propType = prop.type.toTypeName()

                // =====================
                // VALIDATION RULES
                // =====================

                prop.annotations.forEach { annotation ->

                    when (annotation.shortName.asString()) {

                        // =========================
                        // REQUIRED / NOT BLANK
                        // =========================
                        "Required", "NotBlank" -> {

                            val message = annotation.arguments
                                .firstOrNull()?.value as? String ?: "Required"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    { it.%L },
    {
        if (it == null || it.toString().isBlank())
            %T(%S, "required")
        else null
    }
)
""",
                                    fieldRuleClass,
                                    propName,
                                    propName,
                                    fieldErrorClass,
                                    message
                                )
                            )
                        }

                        // =========================
                        // EMAIL (regex configurable)
                        // =========================
                        "Email" -> {

                            val regex = annotation.arguments
                                .firstOrNull { it.name?.asString() == "regex" }
                                ?.value as? String
                                ?: "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"

                            val message = annotation.arguments
                                .firstOrNull { it.name?.asString() == "message" }
                                ?.value as? String ?: "Invalid email"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    { it.%L },
    {
        val regex = Regex(%S)
        val str = it as? String
        if (str == null || !regex.matches(str))
            %T(%S, "email")
        else null
    }
)
""",
                                    fieldRuleClass,
                                    propName,
                                    propName,
                                    regex,
                                    fieldErrorClass,
                                    message
                                )
                            )
                        }

                        // =========================
                        // PATTERN (generic regex)
                        // =========================
                        "Pattern" -> {

                            val regex = annotation.arguments
                                .firstOrNull()?.value as? String ?: ".*"

                            val message = annotation.arguments
                                .firstOrNull { it.name?.asString() == "message" }
                                ?.value as? String ?: "Invalid format"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    { it.%L },
    {
        val regex = Regex(%S)
        val str = it as? String
        if (str == null || !regex.matches(str))
            %T(%S, "pattern")
        else null
    }
)
""",
                                    fieldRuleClass,
                                    propName,
                                    propName,
                                    regex,
                                    fieldErrorClass,
                                    message
                                )
                            )
                        }

                        // =========================
                        // MIN LENGTH
                        // =========================
                        "MinLength" -> {

                            val value = annotation.arguments
                                .first { it.name?.asString() == "value" }
                                .value as Int

                            val message = annotation.arguments
                                .firstOrNull { it.name?.asString() == "message" }
                                ?.value as? String ?: "Too short"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    { it.%L },
    {
        val str = it as? String
        if (str == null || str.length < %L)
            %T(%S, "min_length")
        else null
    }
)
""",
                                    fieldRuleClass,
                                    propName,
                                    propName,
                                    value,
                                    fieldErrorClass,
                                    message
                                )
                            )
                        }

                        // =========================
                        // LENGTH (min + max)
                        // =========================
                        "Length" -> {

                            val min = annotation.arguments
                                .firstOrNull { it.name?.asString() == "min" }
                                ?.value as? Int ?: 0

                            val max = annotation.arguments
                                .firstOrNull { it.name?.asString() == "max" }
                                ?.value as? Int ?: Int.MAX_VALUE

                            val message = annotation.arguments
                                .firstOrNull { it.name?.asString() == "message" }
                                ?.value as? String ?: "Invalid length"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    { it.%L },
    {
        val str = it as? String
        if (str == null || str.length !in %L..%L)
            %T(%S, "length")
        else null
    }
)
""",
                                    fieldRuleClass,
                                    propName,
                                    propName,
                                    min,
                                    max,
                                    fieldErrorClass,
                                    message
                                )
                            )
                        }

                        // =========================
                        // MIN
                        // =========================
                        "Min" -> {

                            val value = annotation.arguments
                                .firstOrNull()?.value as? Number ?: 0

                            val message = annotation.arguments
                                .firstOrNull { it.name?.asString() == "message" }
                                ?.value as? String ?: "Too small"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    { it.%L },
    {
        val number = (it as? Number)?.toDouble()
        if (number == null || number < %L)
            %T(%S, "min")
        else null
    }
)
""",
                                    fieldRuleClass,
                                    propName,
                                    propName,
                                    value.toDouble(),
                                    fieldErrorClass,
                                    message
                                )
                            )
                        }

                        // =========================
                        // MAX
                        // =========================
                        "Max" -> {

                            val value = annotation.arguments
                                .firstOrNull()?.value as? Number ?: 0

                            val message = annotation.arguments
                                .firstOrNull { it.name?.asString() == "message" }
                                ?.value as? String ?: "Too large"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    { it.%L },
    {
        val number = (it as? Number)?.toDouble()
        if (number != null && number > %L)
            %T(%S, "max")
        else null
    }
)
""",
                                    fieldRuleClass,
                                    propName,
                                    propName,
                                    value.toDouble(),
                                    fieldErrorClass,
                                    message
                                )
                            )
                        }

                        // =========================
                        // RANGE
                        // =========================
                        "Range" -> {

                            val min = annotation.arguments
                                .first { it.name?.asString() == "min" }
                                .value as Number

                            val max = annotation.arguments
                                .first { it.name?.asString() == "max" }
                                .value as Number

                            val message = annotation.arguments
                                .firstOrNull { it.name?.asString() == "message" }
                                ?.value as? String ?: "Out of range"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    { it.%L },
    {
        val number = (it as? Number)?.toDouble()
        if (number == null || number < %L || number > %L)
            %T(%S, "range")
        else null
    }
)
""",
                                    fieldRuleClass,
                                    propName,
                                    propName,
                                    min.toDouble(),
                                    max.toDouble(),
                                    fieldErrorClass,
                                    message
                                )
                            )
                        }

                        // =========================
                        // EQUALS TO (cross-field)
                        // =========================
                        "EqualsTo" -> {

                            val otherField = annotation.arguments
                                .first { it.name?.asString() == "field" }
                                .value as String

                            val message = annotation.arguments
                                .firstOrNull { it.name?.asString() == "message" }
                                ?.value as? String ?: "Does not match"

                            rulesCode.add(
                                CodeBlock.of(
                                    """
%T(
    %S,
    %S,
    %S
)
""",
                                    ClassName(
                                        "com.badrqaba.kbooster.core.validation.rules",
                                        "EqualsRule"
                                    ),
                                    propName,
                                    otherField,
                                    message
                                )
                            )
                        }
                    }

                }

                // =====================
                // GENERATED updateX()
                // =====================

                val updateFun = FunSpec.builder("update$capitalized")
                    .receiver(formStateType)
                    .addParameter("value", propType)
                    .addStatement(
                        "updateField(%S) { copy(%L = value) }",
                        propName,
                        propName
                    )
                    .build()

                extensionFunctions.add(updateFun)

                // =====================
                // GENERATED xError
                // =====================

                val errorProp = PropertySpec.builder(
                    "${propName}Error",
                    String::class.asTypeName().copy(nullable = true)
                )
                    .receiver(formStateType)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return errors.value[%S]?.firstOrNull()?.message",
                                propName
                            )
                            .build()
                    )
                    .build()

                extensionProperties.add(errorProp)
            }

            // ---------- Validator ----------

            val validatorType = TypeSpec.classBuilder(validatorName)
                .superclass(validatorClass.parameterizedBy(modelClass))
                .addSuperclassConstructorParameter(
                    "listOf<%T>(\n%L\n)",
                    ClassName("com.badrqaba.kbooster.core.validation.rules", "ValidationRule").parameterizedBy(modelClass),
                    rulesCode.joinToCode(",\n")
                )
                .build()

            // ---------- Factory ----------

            val defaultArgs = clazz.getAllProperties()
                .joinToString(", ") { defaultValue(it) }

            val factoryFun = FunSpec.builder(formFunctionName)
                .returns(formStateType)
                .addStatement(
                    "return %T(%T($defaultArgs), %T())",
                    formStateClass,
                    modelClass,
                    ClassName(packageName, validatorName)
                )
                .build()

            // ---------- File ----------

            FileSpec.builder(packageName, "${className}Validation")
                .addType(validatorType)
                .addFunction(factoryFun)
                .addFunctions(extensionFunctions)
                .addProperties(extensionProperties)
                .build()
                .writeTo(codeGenerator, Dependencies(false, clazz.containingFile!!))
        }

        return emptyList()
    }

    private fun defaultValue(prop: KSPropertyDeclaration): String {
        val type = prop.type.resolve().declaration.simpleName.asString()

        return when (type) {
            "String" -> "\"\""
            "Int" -> "0"
            "Long" -> "0L"
            "Boolean" -> "false"
            else -> "null"
        }
    }
}