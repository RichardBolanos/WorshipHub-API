package com.worshiphub.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan("com.worshiphub.domain")
@EnableJpaRepositories("com.worshiphub.infrastructure.repository")
class JpaConfig