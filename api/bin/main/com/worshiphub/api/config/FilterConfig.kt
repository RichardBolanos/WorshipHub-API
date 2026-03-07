package com.worshiphub.api.config

import com.worshiphub.api.config.LoggingConfig.CorrelationIdFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class FilterConfig {
    
    @Bean
    fun correlationIdFilter(): FilterRegistrationBean<CorrelationIdFilter> {
        val registrationBean = FilterRegistrationBean<CorrelationIdFilter>()
        registrationBean.filter = CorrelationIdFilter()
        registrationBean.order = Ordered.HIGHEST_PRECEDENCE
        return registrationBean
    }
}