package com.badrqaba.kbooster.processor.validation

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ValidationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.warn("ValidationProcessorProvider created")
        return ValidationProcessor(
            environment.codeGenerator,
            logger = environment.logger
        )
    }
}