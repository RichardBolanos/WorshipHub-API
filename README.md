# WorshipHub API

> Sistema completo de gestión para equipos de alabanza y adoración

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Características](#-características)
- [Arquitectura](#-arquitectura)
- [Tecnologías](#-tecnologías)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Ejecución](#-ejecución)
- [API Documentation](#-api-documentation)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Estructura del Proyecto](#-estructura-del-proyecto)

## 🎯 Descripción

WorshipHub API es una plataforma completa para la gestión de equipos de alabanza y adoración en iglesias. Permite administrar equipos, catálogos de canciones, programación de servicios, y comunicación en tiempo real entre los miembros del equipo.

### Funcionalidades Principales

- **Gestión de Organizaciones**: Registro de iglesias, equipos y miembros
- **Catálogo de Canciones**: Biblioteca completa con transposición de acordes
- **Programación Inteligente**: Creación automática de setlists y programación de servicios
- **Comunicación en Tiempo Real**: Chat y notificaciones vía WebSocket
- **Sistema de Invitaciones**: Flujo completo de invitación y aceptación de usuarios
- **Gestión de Roles**: Sistema RBAC con 4 niveles de autorización

## ✨ Características

### Gestión de Organizaciones y Equipos
- ✅ Registro completo de iglesias con administrador
- ✅ Sistema de invitaciones con tokens seguros (7 días de expiración)
- ✅ Creación y gestión de equipos de alabanza
- ✅ Asignación de roles y permisos
- ✅ Gestión de perfiles de usuario

### Catálogo Avanzado de Canciones
- ✅ Formato ChordPro para canciones con acordes
- ✅ Transposición automática de tonalidades musicales
  - Soporte para notación con sostenidos (#) y bemoles (b)
  - Detección automática de tonalidades que usan bemoles (F, Bb, Eb, Ab, Db, Gb, Dm, Gm, Cm, Fm, Bbm, Ebm)
  - Preservación de sufijos de acordes durante transposición (maj7, sus4, etc.)
  - Manejo de formato ChordPro: `[C]texto [Am]más texto`
- ✅ Búsqueda y filtrado avanzado
- ✅ Categorización con tags
- ✅ Adjuntos (YouTube, PDF, Spotify)
- ✅ Comentarios y discusiones por canción
- ✅ Catálogo global compartido

### Programación y Planificación
- ✅ Creación de setlists personalizados
- ✅ Generación automática de setlists basada en reglas
- ✅ Cálculo de duración basado en BPM
- ✅ Programación de servicios con asignación de equipos
- ✅ Sistema de confirmación de asistencia
- ✅ Gestión de disponibilidad de miembros

### Comunicación y Colaboración
- ✅ Notificaciones en tiempo real
- ✅ Chat de equipo con WebSocket
- ✅ Historial de mensajes
- ✅ Sistema de eventos de dominio

### Seguridad y Autenticación
- ✅ Autenticación JWT con refresh tokens
- ✅ Verificación de email con tokens seguros (24h)
- ✅ Recuperación de contraseña (tokens de 1h)
- ✅ Sistema de roles jerárquico (SUPER_ADMIN, CHURCH_ADMIN, WORSHIP_LEADER, TEAM_MEMBER)
- ✅ Autorización a nivel de método
- ✅ Políticas de contraseñas robustas
- ✅ Blacklist de tokens para logout seguro
- ✅ Audit logging de eventos de seguridad
- ✅ Rate limiting (100 req/min por IP)

## 🏗️ Arquitectura

El proyecto implementa **Clean Architecture** con **Domain-Driven Design (DDD)**:

```
┌─────────────────────────────────────────────────────────┐
│                     API Layer                           │
│  (Controllers, DTOs, Security, WebSocket)               │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                Application Layer                        │
│  (Use Cases, Application Services, Commands)            │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  Domain Layer                           │
│  (Entities, Value Objects, Domain Services, Events)     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Infrastructure Layer                       │
│  (JPA Entities, Repositories, External Services)        │
└─────────────────────────────────────────────────────────┘
```

### Bounded Contexts

1. **Organization Context**: Iglesias, usuarios, equipos, invitaciones
2. **Catalog Context**: Canciones, categorías, adjuntos, comentarios
3. **Scheduling Context**: Servicios, setlists, asignaciones, disponibilidad
4. **Collaboration Context**: Notificaciones, chat, mensajes

### Patrones Implementados

- **Repository Pattern**: Interfaces de dominio con implementaciones de infraestructura
- **CQRS Elements**: Separación de modelos de lectura/escritura
- **Event-Driven Architecture**: Eventos de dominio para comunicación entre agregados
- **Dependency Inversion**: Todas las dependencias apuntan hacia el dominio
- **Entity Identity**: Entidades de dominio implementan equals() y hashCode() basados en ID para comparación correcta en colecciones y caché

## 🛠️ Tecnologías

### Backend Core
- **Kotlin 2.1.0**: Lenguaje principal
- **Spring Boot 3.5.5**: Framework de aplicación
- **Spring Security**: Autenticación y autorización
- **Spring Data JPA**: Persistencia de datos
- **Spring WebSocket**: Comunicación en tiempo real

### Base de Datos
- **PostgreSQL 16**: Base de datos principal
- **PostGIS 3.5**: Extensiones geoespaciales
- **Flyway**: Migraciones de base de datos
- **HikariCP**: Pool de conexiones

### Seguridad
- **JWT (jjwt 0.12.6)**: Tokens de autenticación
- **BCrypt**: Hash de contraseñas
- **Spring Security OAuth2**: Integración OAuth2

### Documentación y Testing
- **SpringDoc OpenAPI**: Documentación Swagger
- **JUnit 5**: Testing unitario
- **MockK**: Mocking para Kotlin
- **Kotest 5.8.0**: Property-based testing y assertions para Kotlin
- **Spring Boot Test**: Testing de integración

### Herramientas de Desarrollo
- **Gradle 8.x**: Build tool
- **Docker & Docker Compose**: Contenedorización
- **Mailpit**: Servidor SMTP de desarrollo

## 📦 Requisitos Previos

- **Java 21 LTS** (requerido)
  - ⚠️ **IMPORTANTE:** Usa Java 21 LTS específicamente
  - Java 25+ NO es compatible con Kotlin 2.1.0
  - Descarga: [Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21)
  - Verifica tu versión: `java -version`
- **Docker** y **Docker Compose**
- **Gradle 8.x** (incluido con wrapper)
- **Git**

## 🚀 Instalación

### 1. Clonar el repositorio

```bash
git clone <repository-url>
cd worship_hub_api
```

### 2. Iniciar servicios con Docker

```bash
# Iniciar PostgreSQL y Mailpit
./start-database.bat

# O manualmente con Docker Compose
docker-compose up -d
```

Esto iniciará:
- **PostgreSQL**: Puerto 5442
- **Mailpit**: Puerto 8025 (Web UI), 1025 (SMTP)

### 3. Verificar servicios

```bash
# Verificar PostgreSQL
docker exec WorshipHubPostgres pg_isready -U postgres -d worshiphub

# Acceder a Mailpit
# Abrir http://localhost:8025 en el navegador
```

## ⚙️ Configuración

### Perfiles de Spring Boot

El proyecto incluye múltiples perfiles de configuración:

- **local**: Desarrollo local con PostgreSQL (puerto 5442)
- **h2**: Desarrollo rápido con base de datos en memoria
- **neon**: Desarrollo con Neon PostgreSQL cloud
- **prod**: Producción con PostgreSQL
- **simple**: Configuración simplificada sin seguridad

### Variables de Entorno

Crear archivo `.env` en `api/` basado en `.env.local`:

```bash
# Perfil de Spring Boot
SPRING_PROFILES_ACTIVE=local

# Base de Datos
DATABASE_URL=jdbc:postgresql://localhost:5442/worshiphub
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# JWT
JWT_SECRET=your-secret-key-minimum-32-characters-long
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000

# Server
SERVER_PORT=9090
```

### Configuración de Perfiles

#### Perfil Local (Recomendado para desarrollo)

```yaml
# application-local.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5442/worshiphub
    username: postgres
    password: postgres
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: false
server:
  port: 9090
```

#### Perfil H2 (Desarrollo rápido / E2E tests)

```yaml
# application-h2.yml
spring:
  datasource:
    url: jdbc:h2:mem:worshiphub
  h2:
    console:
      enabled: true
      path: /h2-console
```

> **Servicios de email por perfil:** El perfil `h2` activa `NoOpEmailService` (`@Profile("h2") @Primary`) que simula el envío de emails sin conexión SMTP. La implementación real `EmailServiceImpl` se excluye automáticamente mediante `@Profile("!h2")`. Esto permite que los tests E2E y el desarrollo rápido funcionen sin configurar un servidor de correo. En los demás perfiles (`local`, `prod`, etc.), se usa `EmailServiceImpl` con JavaMailSender (Mailpit en local, SMTP real en producción).

## 🎮 Ejecución

### Opción 1: Script de inicio (Recomendado)

```bash
cd api/scripts
./start-local.bat
```

### Opción 2: Gradle directamente

```bash
# Con perfil local
./gradlew :api:bootRun --args="--spring.profiles.active=local"

# Con perfil H2
./gradlew :api:bootRun --args="--spring.profiles.active=h2"
```

### Opción 3: Ejecutar JAR compilado

```bash
# Compilar
./gradlew :api:bootJar

# Ejecutar
java -jar api/build/libs/api-1.0.0.jar --spring.profiles.active=local
```

### Verificar que está corriendo

```bash
# Health check
curl http://localhost:9090/api/v1/health

# System info
curl http://localhost:9090/api/v1/system/info
```

Respuesta esperada:
```json
{
  "status": "UP",
  "timestamp": "2024-12-27T10:00:00Z"
}
```

## 📚 API Documentation

### Swagger UI

Una vez que la aplicación esté corriendo, accede a la documentación interactiva:

```
http://localhost:9090/swagger-ui.html
```

### OpenAPI Specification

```
http://localhost:9090/v3/api-docs
```

### Endpoints Principales

#### Autenticación
```
POST   /api/v1/auth/login                    # Login
POST   /api/v1/auth/register                 # Registro de usuario
POST   /api/v1/auth/church/register          # Registro de iglesia + admin
POST   /api/v1/auth/logout                   # Logout
POST   /api/v1/auth/refresh                  # Refresh token
POST   /api/v1/auth/verify-email             # Verificar email
POST   /api/v1/auth/forgot-password          # Solicitar reset de contraseña
POST   /api/v1/auth/reset-password           # Resetear contraseña
```

#### Organizaciones
```
GET    /api/v1/churches                      # Listar iglesias
POST   /api/v1/churches                      # Crear iglesia
GET    /api/v1/churches/{id}                 # Obtener iglesia
PATCH  /api/v1/churches/{id}                 # Actualizar iglesia
DELETE /api/v1/churches/{id}                 # Eliminar iglesia
```

#### Equipos
```
GET    /api/v1/teams                         # Listar equipos
POST   /api/v1/teams                         # Crear equipo
GET    /api/v1/teams/{id}                    # Obtener equipo
PATCH  /api/v1/teams/{id}                    # Actualizar equipo
DELETE /api/v1/teams/{id}                    # Eliminar equipo
POST   /api/v1/teams/{id}/members            # Agregar miembro
DELETE /api/v1/teams/{id}/members/{userId}   # Remover miembro
```

#### Canciones
```
GET    /api/v1/songs                         # Listar canciones
POST   /api/v1/songs                         # Crear canción
GET    /api/v1/songs/{id}                    # Obtener canción
PATCH  /api/v1/songs/{id}                    # Actualizar canción
DELETE /api/v1/songs/{id}                    # Eliminar canción
GET    /api/v1/songs/search                  # Buscar canciones
POST   /api/v1/songs/{id}/attachments        # Agregar adjunto
POST   /api/v1/songs/{id}/comments           # Agregar comentario
```

#### Setlists
```
GET    /api/v1/setlists                      # Listar setlists
POST   /api/v1/setlists                      # Crear setlist
GET    /api/v1/setlists/{id}                 # Obtener setlist
PATCH  /api/v1/setlists/{id}                 # Actualizar setlist
DELETE /api/v1/setlists/{id}                 # Eliminar setlist
POST   /api/v1/setlists/generate             # Generar setlist automático
POST   /api/v1/setlists/{id}/songs           # Agregar canción
DELETE /api/v1/setlists/{id}/songs/{songId}  # Remover canción
```

#### Servicios
```
GET    /api/v1/services                      # Listar servicios
POST   /api/v1/services                      # Crear servicio
GET    /api/v1/services/{id}                 # Obtener servicio
PATCH  /api/v1/services/{id}                 # Actualizar servicio
DELETE /api/v1/services/{id}                 # Eliminar servicio
PATCH  /api/v1/services/{id}/assignments/{assignmentId}  # Confirmar asistencia
```

#### Invitaciones
```
POST   /api/v1/invitations/send              # Enviar invitación
GET    /api/v1/invitations/{token}           # Obtener detalles de invitación
POST   /api/v1/invitations/{token}/accept    # Aceptar invitación
```

#### Notificaciones
```
GET    /api/v1/notifications                 # Listar notificaciones
PATCH  /api/v1/notifications/{id}/read       # Marcar como leída
DELETE /api/v1/notifications/{id}            # Eliminar notificación
```

#### Chat (WebSocket + REST)
```
WS     /ws/chat                              # Conexión WebSocket
STOMP  /chat.sendMessage                     # Enviar mensaje vía WebSocket
GET    /api/v1/teams/{teamId}/chat/history   # Obtener historial de chat
POST   /api/v1/teams/{teamId}/messages       # Enviar mensaje vía REST (201)
```

### Autenticación en API

Todas las peticiones (excepto login, register y health) requieren un token JWT:

```bash
# 1. Login
curl -X POST http://localhost:9090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@church.com",
    "password": "SecurePass123!"
  }'

# 2. Usar el token en peticiones
curl http://localhost:9090/api/v1/songs \
  -H "Authorization: Bearer <your-jwt-token>"
```

## 🧪 Testing

### Ejecutar todos los tests

```bash
./gradlew test
```

### Ejecutar tests de un módulo específico

```bash
# Tests del módulo API
./gradlew :api:test

# Tests del módulo Application
./gradlew :application:test

# Tests del módulo Domain
./gradlew :domain:test
```

### Cobertura de Tests

```bash
./gradlew jacocoTestReport
```

El reporte se genera en: `build/reports/jacoco/test/html/index.html`

### Tests Implementados

- **Controller Tests**: 12 archivos con tests de integración
- **Service Tests**: 4 archivos con tests unitarios
- **Domain Tests**: 3 archivos con tests de lógica de negocio
- **Integration Tests**: 2 archivos con tests end-to-end
- **Cobertura**: 71% de endpoints (30/42+)

## 🚢 Deployment

### Build para Producción

```bash
# Compilar JAR
./gradlew :api:bootJar

# El JAR se genera en
# api/build/libs/api-1.0.0.jar
```

### Docker Build

```bash
# Build imagen Docker
docker build -t worshiphub-api:latest -f Dockerfile .

# Run container
docker run -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host:5432/worshiphub \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=secure-password \
  -e JWT_SECRET=your-production-secret-key \
  worshiphub-api:latest
```

### Native Build (GraalVM)

```bash
# Build native image
./build-native-local.bat

# O con Gradle
./gradlew :api:nativeCompile
```

### Variables de Entorno para Producción

```bash
# Obligatorias
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://production-host:5432/worshiphub
DATABASE_USERNAME=worshiphub_user
DATABASE_PASSWORD=<secure-password>
JWT_SECRET=<minimum-32-characters-secure-random-string>

# Opcionales
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000
SERVER_PORT=9090
LOG_LEVEL=INFO
```

### Health Checks

```bash
# Liveness probe
curl http://localhost:9090/actuator/health/liveness

# Readiness probe
curl http://localhost:9090/actuator/health/readiness

# Detailed health
curl http://localhost:9090/actuator/health
```

## 📁 Estructura del Proyecto

```
worship_hub_api/
├── api/                          # Capa de API (Controllers, Security, Config)
│   ├── src/main/kotlin/com/worshiphub/
│   │   ├── api/                  # REST Controllers
│   │   ├── config/               # Configuraciones Spring
│   │   ├── security/             # JWT, Security Config
│   │   └── WorshipHubApplication.kt
│   ├── src/main/resources/
│   │   ├── application.yml       # Configuración base
│   │   ├── application-local.yml # Perfil local
│   │   ├── application-prod.yml  # Perfil producción
│   │   └── db/migration/         # Migraciones Flyway
│   └── build.gradle.kts
│
├── application/                  # Capa de Aplicación (Use Cases, Services)
│   ├── src/main/kotlin/com/worshiphub/application/
│   │   ├── services/             # Application Services
│   │   ├── commands/             # Command objects
│   │   └── events/               # Event handlers
│   └── build.gradle.kts
│
├── domain/                       # Capa de Dominio (Entities, Business Logic)
│   ├── src/main/kotlin/com/worshiphub/domain/
│   │   ├── organization/         # Bounded Context: Organization
│   │   ├── catalog/              # Bounded Context: Catalog
│   │   ├── scheduling/           # Bounded Context: Scheduling
│   │   ├── collaboration/        # Bounded Context: Collaboration
│   │   └── shared/               # Shared kernel
│   └── build.gradle.kts
│
├── infrastructure/               # Capa de Infraestructura (JPA, Repositories)
│   ├── src/main/kotlin/com/worshiphub/infrastructure/
│   │   ├── persistence/          # JPA Entities
│   │   ├── repositories/         # Repository Implementations
│   │   └── external/             # External services
│   └── build.gradle.kts
│
├── docker-compose.yml            # Docker services
├── Dockerfile                    # Docker image
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Gradle settings
├── start-database.bat            # Script para iniciar DB
└── README.md                     # Este archivo
```

### Módulos

#### API Module
- Controllers REST
- Configuración de seguridad
- WebSocket configuration
- DTOs y mappers
- Exception handlers

#### Application Module
- Application Services (orquestación)
- Commands y Queries
- Event handlers
- Use cases

#### Domain Module
- Entidades de dominio
- Value objects
- Domain services
  - **ChordTransposer**: Utilidad para transposición de acordes musicales
    - Transpone acordes en formato ChordPro entre tonalidades
    - Calcula semitonos entre tonalidades origen y destino
    - Maneja notación con sostenidos (#) y bemoles (b)
    - Detecta automáticamente si usar bemoles según la tonalidad destino
    - Preserva sufijos de acordes (maj7, sus4, dim, etc.)
- Domain events
- Repository interfaces
- Entity identity: equals() and hashCode() based on ID for proper entity comparison

#### Infrastructure Module
- JPA entities
- Repository implementations
- External service integrations
- Database configurations

## 🔐 Seguridad

### Roles y Permisos

1. **SUPER_ADMIN**: Acceso total al sistema
2. **CHURCH_ADMIN**: Administración de iglesia y equipos
3. **WORSHIP_LEADER**: Gestión de setlists y servicios
4. **TEAM_MEMBER**: Acceso básico y confirmación de asistencia

### Políticas de Seguridad

- Contraseñas: Mínimo 8 caracteres, mayúsculas, minúsculas, números
- Tokens JWT: Expiración de 1 hora
- Refresh tokens: Expiración de 24 horas
- Email verification: Tokens de 24 horas
- Password reset: Tokens de 1 hora
- Invitations: Tokens de 7 días
- Rate limiting: 100 requests/minuto por IP

### Headers de Seguridad

- Content-Security-Policy
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- Strict-Transport-Security (HSTS)

## 📊 Métricas y Monitoreo

### Actuator Endpoints

```bash
# Health check
GET /actuator/health

# Metrics
GET /actuator/metrics

# Info
GET /actuator/info
```

### Logging

Los logs se guardan en:
- Consola: Nivel INFO (producción) / DEBUG (desarrollo)
- Archivo: `api/logs/worshiphub.log`
- Formato: JSON estructurado con correlation IDs

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📝 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 📞 Soporte

Para preguntas o soporte:
- Documentación completa: `WORSHIPHUB_DOCUMENTATION.md`
- Issues: GitHub Issues
- Email: support@worshiphub.com

## 🎯 Roadmap

### Completado ✅
- [x] Arquitectura Clean Architecture con DDD
- [x] Sistema completo de autenticación y autorización
- [x] 60+ endpoints REST
- [x] WebSocket para chat en tiempo real
- [x] Sistema de invitaciones
- [x] Transposición de acordes
- [x] Generación automática de setlists
- [x] 71% cobertura de tests

### En Progreso 🚧
- [ ] Account lockout y políticas de seguridad avanzadas
- [ ] Integración SMTP para emails en producción
- [ ] Audit logging mejorado con persistencia

### Próximas Funcionalidades 🎯
- [ ] Integración con servicios de streaming (Spotify, YouTube)
- [ ] Exportación de setlists a PDF
- [ ] Aplicación móvil (Flutter)
- [ ] Analytics y reportes
- [ ] Multi-tenancy
- [ ] Integración con calendarios (Google Calendar, Outlook)

---

**Desarrollado con ❤️ para la comunidad de adoración**
