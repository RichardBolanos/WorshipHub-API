plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.spring") version "2.1.0" apply false
    id("org.springframework.boot") version "3.5.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("plugin.jpa") version "2.1.0" apply false
}

allprojects {
    group = "com.worshiphub"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}