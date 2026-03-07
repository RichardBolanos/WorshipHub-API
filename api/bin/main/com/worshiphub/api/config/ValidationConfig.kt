package com.worshiphub.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

@Configuration
class ValidationConfig {
    
    @Bean
    fun validator(): LocalValidatorFactoryBean {
        return LocalValidatorFactoryBean()
    }
    
    @Bean
    fun methodValidationPostProcessor(): MethodValidationPostProcessor {
        val processor = MethodValidationPostProcessor()
        processor.setValidator(validator())
        return processor
    }
}