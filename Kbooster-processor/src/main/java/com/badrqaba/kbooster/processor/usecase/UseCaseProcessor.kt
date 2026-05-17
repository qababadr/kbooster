package com.badrqaba.kbooster.processor.usecase

import com.badrqaba.kbooster.core.annotation.Usecaseable
import com.badrqaba.kbooster.core.model.DIScope
import com.badrqaba.kbooster.core.model.GeneratedUseCase
import com.badrqaba.kbooster.core.model.UsecaseableConfig
import com.badrqaba.kbooster.core.model.Visibility
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.*

class UseCaseProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("Kbooster usecase processor started")
        val symbols = resolver
            .getSymbolsWithAnnotation(Usecaseable::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        symbols.forEach { repo ->

            val config = repo.getUsecaseableConfig()

            val visibilityModifier = when (config.visibility) {
                Visibility.INTERNAL -> KModifier.INTERNAL
                Visibility.PUBLIC -> KModifier.PUBLIC
            }

            val packageName = repo.packageName.asString()
            val repoName = repo.simpleName.asString()
            val repoClass = repo.toClassName()

            val useCases = mutableListOf<GeneratedUseCase>()

            repo.getDeclaredFunctions().forEach { function ->

                val functionName = function.simpleName.asString()
                val capitalized = functionName.replaceFirstChar { it.uppercase() }

                val useCaseName =
                    if (config.prefixWithRepo) {
                        repoName + capitalized + "UseCase"
                    } else {
                        capitalized + "UseCase"
                    }

                val useCaseClass = ClassName(packageName, useCaseName)

                useCases.add(
                    GeneratedUseCase(
                        propertyName = functionName,
                        className = useCaseClass
                    )
                )

                // ---------- Generate UseCase ----------

                val constructor = FunSpec.constructorBuilder()
                    .addParameter("repository", repoClass)
                    .build()

                val property = PropertySpec.builder("repository", repoClass)
                    .initializer("repository")
                    .addModifiers(KModifier.PRIVATE)
                    .build()

                val funBuilder = FunSpec.builder("invoke")
                    .addModifiers(KModifier.OPERATOR)

                if (function.modifiers.contains(Modifier.SUSPEND)) {
                    funBuilder.addModifiers(KModifier.SUSPEND)
                }

                val params = function.parameters.map {
                    val name = it.name!!.asString()
                    val type = it.type.toTypeName()
                    funBuilder.addParameter(name, type)
                    name
                }

                val returnType = function.returnType?.toTypeName()

                if (returnType != null && returnType != UNIT) {
                    funBuilder.returns(returnType)
                }

                val call = "repository.$functionName(${params.joinToString()})"

                if (returnType == null || returnType == UNIT) {
                    funBuilder.addStatement(call)
                } else {
                    funBuilder.addStatement("return $call")
                }

                val typeSpec = TypeSpec.classBuilder(useCaseName)
                    .addModifiers(visibilityModifier)
                    .primaryConstructor(constructor)
                    .addProperty(property)
                    .addFunction(funBuilder.build())
                    .build()

                FileSpec.builder(packageName, useCaseName)
                    .addType(typeSpec)
                    .build()
                    .writeTo(
                        codeGenerator,
                        Dependencies(false, repo.containingFile!!)
                    )
            }

            // ---------- Generate Wrapper ----------

            if (config.generateWrapper) {

                val wrapperName = "${repoName}UseCases"

                val constructor = FunSpec.constructorBuilder()
                val wrapperBuilder = TypeSpec.classBuilder(wrapperName)
                    .addModifiers(KModifier.DATA, visibilityModifier)

                useCases.forEach { useCase ->
                    constructor.addParameter(useCase.propertyName, useCase.className)

                    wrapperBuilder.addProperty(
                        PropertySpec.builder(useCase.propertyName, useCase.className)
                            .initializer(useCase.propertyName)
                            .build()
                    )
                }

                val wrapperClass = wrapperBuilder
                    .primaryConstructor(constructor.build())
                    .build()

                FileSpec.builder(packageName, wrapperName)
                    .addType(wrapperClass)
                    .build()
                    .writeTo(
                        codeGenerator,
                        Dependencies(false, repo.containingFile!!)
                    )
            }

            // Generate DI module for usecases
            if (config.generateDI && config.generateWrapper) {

                val moduleName = "${repoName}UseCasesModule"
                val wrapperName = "${repoName}UseCases"

                val hiltModule = ClassName("dagger", "Module")
                val installIn = ClassName("dagger.hilt", "InstallIn")
                val provides = ClassName("dagger", "Provides")

                val singletonComponent = when (config.diScope) {
                    DIScope.SINGLETON ->
                        ClassName("dagger.hilt.components", "SingletonComponent")
                    DIScope.VIEWMODEL ->
                        ClassName("dagger.hilt.android.components", "ViewModelComponent")
                }

                val moduleBuilder = TypeSpec.objectBuilder(moduleName)
                    .addAnnotation(hiltModule)
                    .addAnnotation(
                        AnnotationSpec.builder(installIn)
                            .addMember("%T::class", singletonComponent)
                            .build()
                    )

                val provideFun = FunSpec.builder("provide${wrapperName}")
                    .addAnnotation(provides)
                    .addParameter("repository", repoClass)
                    .returns(ClassName(packageName, wrapperName))
                    .addStatement(
                        "return %L(%L)",
                        wrapperName,
                        useCases.joinToString {
                            "${it.propertyName} = ${it.className.simpleName}(repository)"
                        }
                    )
                    .build()

                moduleBuilder.addFunction(provideFun)

                FileSpec.builder(packageName, moduleName)
                    .addType(moduleBuilder.build())
                    .build()
                    .writeTo(codeGenerator, Dependencies(false, repo.containingFile!!))
            }
        }

        return emptyList()
    }

    private fun KSClassDeclaration.getUsecaseableConfig(): UsecaseableConfig {

        val annotation = annotations.first {
            it.shortName.asString() == "Usecaseable"
        }

        var prefixWithRepo = true
        var generateWrapper = true
        var visibility = Visibility.PUBLIC
        var generateDI = false
        var diScope = DIScope.SINGLETON

        annotation.arguments.forEach { arg ->
            when (arg.name?.asString()) {
                "prefixWithRepo" -> prefixWithRepo = arg.value as Boolean
                "generateWrapper" -> generateWrapper = arg.value as Boolean
                "visibility" -> {
                    val entry = arg.value as? KSClassDeclaration
                    visibility = when (entry?.simpleName?.asString()) {
                        "INTERNAL" -> Visibility.INTERNAL
                        else -> Visibility.PUBLIC
                    }
                }
                "generateDI" -> generateDI = arg.value as Boolean
                "diScope" -> {
                    val entry = arg.value as? KSClassDeclaration
                    diScope = when (entry?.simpleName?.asString()) {
                        "VIEWMODEL" -> DIScope.VIEWMODEL
                        else -> DIScope.SINGLETON
                    }
                }
            }
        }

        return UsecaseableConfig(
            prefixWithRepo,
            generateWrapper,
            visibility,
            generateDI,
            diScope
        )
    }
}