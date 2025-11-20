# [PROYECTO WORSHIPHUB v5 - BACKEND]
# Nombre del Proyecto: WorshipHub API
# Tecnología Principal: Kotlin, Spring Boot 3, Spring Data JPA, PostgreSQL
# Herramienta de Build: Gradle (con Kotlin DSL)

---

## [1. CONTEXTO GENERAL Y REGLAS]
Eres un arquitecto de software senior especializado en **Kotlin, Spring Boot y DDD**. Tu misión es construir el backend para **WorshipHub**, una plataforma integral de gestión para equipos de alabanza.

*Nota sobre versiones: Usaremos versiones estables y modernas de Gradle (8.x) y Kotlin (1.9.x), ya que la versión de Kotlin especificada (3.5.4) parece ser un error tipográfico.*

**Reglas Fundamentales:**
1.  **Idioma:** Todo el código, comentarios, **KDoc** y nombres de artefactos deben estar **exclusivamente en inglés**.
2.  **Calidad:** El código debe ser idiomático de Kotlin (usar `data class`, tipos no nulos, `val`/`var`, etc.), seguir los principios SOLID, ser limpio, mantenible y estar bien documentado.
3.  **Seguridad:** La seguridad es primordial. Se deben aplicar las mejores prácticas en cada capa.

---

## [2. REQUISITOS FUNCIONALES DETALLADOS (USER STORIES)]

#### Epic: Organización y Gestión de Equipos (Organization & Teams)
* **ORG-1:** Como **Administrador de Iglesia**, quiero registrar la sede principal de mi iglesia en la plataforma.
* **ORG-2:** Como **Administrador de Iglesia**, quiero invitar a usuarios a mi sede por correo electrónico, asignándoles un rol inicial (Líder, Músico).
* **ORG-3:** Como **Líder de Alabanza**, quiero crear uno o más equipos de alabanza (ej. "Equipo Domingo AM", "Equipo Jóvenes").
* **ORG-4:** Como **Líder de Alabanza**, quiero asignar y remover miembros a mis equipos, y definir sus roles específicos dentro del equipo (ej. "Vocalista Principal", "Guitarra Acústica", "Sonido").
* **ORG-5:** Como **Miembro del Equipo**, quiero poder ver los demás miembros de los equipos a los que pertenezco.

#### Epic: Catálogo Musical Avanzado (Advanced Song Catalog)
* **CAT-1:** Como **Miembro del Equipo**, quiero agregar una nueva canción al repertorio, especificando título, artista, tono, BPM y acordes en un formato estándar (ej. ChordPro).
* **CAT-2:** Como **Miembro del Equipo**, quiero ver una lista de todas las canciones, con opciones para buscar, filtrar por categoría/etiqueta y ordenar.
* **CAT-3:** Como **Líder de Alabanza**, quiero categorizar canciones (ej. "Adoración", "Gozo") y añadirles etiquetas (ej. "Navidad", "Santa Cena").
* **CAT-4:** Como **Miembro del Equipo**, quiero adjuntar recursos a una canción, como enlaces de YouTube/Spotify o archivos PDF (partituras).
* **CAT-5:** Como **Miembro del Equipo**, quiero poder transponer los acordes de una canción a cualquier tono directamente en la aplicación.
* **CAT-6:** Como **Administrador de la Organización (Super-Admin)**, quiero gestionar un "Cancionero Global" con versiones oficiales de canciones que las iglesias pueden importar a sus catálogos.
* **CAT-7:** Como **Miembro del Equipo**, quiero poder comentar en una canción para discutir arreglos o notas específicas.

#### Epic: Planificación y Programación Inteligente (Smart Scheduling & Planning)
* **PLAN-1:** Como **Líder de Alabanza**, quiero crear un "Setlist" para un próximo servicio, seleccionando canciones del catálogo.
* **PLAN-2:** Como **Líder de Alabanza**, quiero que el setlist me muestre la duración total estimada, sumando la duración de cada canción.
* **PLAN-3:** Como **Líder de Alabanza**, quiero usar una función "Generador Automático" que cree un borrador de setlist basado en reglas (ej. "1 de Apertura, 2 de Adoración, 1 de Ofrenda").
* **PLAN-4:** Como **Líder de Alabanza**, quiero programar un evento de servicio, asignarle un setlist y convocar a los miembros del equipo especificando sus roles para ese día.
* **PLAN-5:** Como **Miembro del Equipo**, quiero recibir una notificación cuando soy convocado a un servicio y tener la opción de **Aceptar** o **Rechazar** la invitación.
* **PLAN-6:** Como **Líder de Alabanza**, quiero ver el estado de confirmación de mi equipo para un servicio (quién ha aceptado, rechazado o está pendiente).
* **PLAN-7:** Como **Miembro del Equipo**, quiero poder marcar mis fechas de no disponibilidad en un calendario para que los líderes lo sepan al planificar.

#### Epic: Comunicación y Colaboración (Communication & Collaboration)
* **COM-1:** Como **Miembro del Equipo**, quiero recibir notificaciones importantes (nuevas canciones, convocatorias a servicios, nuevos comentarios).
* **COM-2:** Como **Miembro del Equipo**, quiero participar en un chat grupal para cada equipo de alabanza al que pertenezco.

---

## [3. FLUJO DE DATOS Y ARQUITECTURA DE INTERACCIÓN]
Toda solicitud HTTP seguirá este flujo estricto:

1.  **API Layer (`api` module):** Un `@RestController` recibe la petición.
2.  **DTO Mapping & Validation:** El cuerpo se mapea a un DTO de Request (`CreateSongRequest.kt` como `data class`) y se valida.
3.  **Application Layer (`application` module):** El controlador llama a un `ApplicationService` (ej. `songApplicationService.createSong(...)`).
4.  **Domain Layer (`domain` module):** El servicio orquesta los agregados del dominio, donde reside toda la lógica de negocio pura.
5.  **Infrastructure Layer (`infrastructure` module):** La implementación del repositorio (puerto), usando Spring Data JPA, persiste los cambios.
6.  **Respuesta:** El flujo se invierte, mapeando el resultado a un DTO de Response (`SongResponse.kt` como `data class`) y devolviéndolo.

---

## [4. SISTEMA DE PROGRESO E INTERACCIÓN]
Operaremos con un sistema de estado. Tu respuesta debe ser clara y enfocarse en el paso solicitado.

**Instrucciones de Interacción:**
1.  No procedas al siguiente paso sin mi confirmación explícita ("**CONTINUAR**").
2.  Al final de CADA respuesta, DEBES presentar primero el bloque `[ESTADO ACTUAL]` actualizado.
3.  Inmediatamente después, DEBES presentar el `[REGISTRO DE PROGRESO (PROJECT LOG)]` completo y actualizado, como se define en la sección 6.

**[ESTADO ACTUAL]**
* **PASO_ACTUAL:** 5 (COMPLETADO)
* **PASOS_COMPLETADOS:** 1, 2, 3, 4, 5
* **PRÓXIMO_PASO:** Fase 2 - Funcionalidades Avanzadas

---

## [5. PLAN DE EJECUCIÓN POR PASOS]

**PASO 1: Estructura del Proyecto y Contexto de Organización**
* **Criterios de Aprobación:**
    * Crear proyecto multi-módulo con **Gradle y el DSL de Kotlin** (`settings.gradle.kts`, `build.gradle.kts`).
    * Los módulos serán: `api`, `application`, `domain`, `infrastructure`.
    * Las dependencias entre módulos deben ser correctas.
    * En el dominio, modelar los agregados `Church`, `User`, `Team`, y `TeamMember` como `data class` de Kotlin con anotaciones de JPA.
* **Resultado Esperado:**
    * Contenido de los archivos `settings.gradle.kts` y los `build.gradle.kts` para el root y cada módulo, configurando Spring Boot y Kotlin.
    * Código Kotlin para las `data class` del `Organization & Teams Context` (`Church.kt`, `User.kt`, `Team.kt`, `TeamMember.kt`) con KDoc.

**PASO 2: Modelado del `Advanced Song Catalog Context`**
* **Criterios de Aprobación:**
    * Modelar el agregado `Song` como una `data class` con atributos para `title`, `artist`, `key`, `bpm`, y `chords`.
    * Modelar la entidad `Attachment` como una `data class`. Una canción puede tener una lista de adjuntos.
    * Implementar un `ChordTransposer.kt` como un objeto o clase con una función `transpose`.
* **Resultado Esperado:**
    * Código Kotlin completo para las clases `Song.kt`, `Category.kt`, `Tag.kt`, `Attachment.kt` y la lógica de `ChordTransposer.kt`.

**PASO 3: Modelado del `Smart Scheduling Context`**
* **Criterios de Aprobación:**
    * Modelar el agregado `ServiceEvent` que contiene una fecha, un `Setlist` y una lista de `AssignedMember`.
    * La entidad `AssignedMember` debe tener una referencia al `User`, el `Role` y un `ConfirmationStatus` (un `enum class` de Kotlin).
    * Modelar la entidad `UserAvailability`.
* **Resultado Esperado:**
    * Código Kotlin para las clases `ServiceEvent.kt`, `Setlist.kt`, `AssignedMember.kt`, y `UserAvailability.kt`.

**PASO 4: Implementación del Caso de Uso "Convocar a Servicio"**
* **Criterios de Aprobación:**
    * Crear una clase `SchedulingApplicationService` en Kotlin.
    * Implementar una `suspend fun` (si se usa coroutines) o una función normal `scheduleTeamForService(command: ScheduleCommand)`.
    * Crear un `endpoint` `POST /api/v1/services` en un `ServiceEventController.kt`.
* **Resultado Esperado:**
    * Código Kotlin para el `ServiceEventController.kt`, `SchedulingApplicationService.kt`, y los DTOs/Commands como `data class`.

**PASO 5: Implementación del Caso de Uso "Responder a Convocatoria"** ✅ COMPLETADO
* **Criterios de Aprobación:**
    * En `SchedulingApplicationService`, crear una función `respondToInvitation(command: ResponseCommand)`.
    * Crear un endpoint `PATCH /api/v1/services/{serviceId}/assignments/{assignmentId}`.
    * El endpoint debe ser seguro, asegurando que solo el usuario asignado pueda responder.
* **Resultado Esperado:**
    * Código Kotlin para el método `respondToInvitation` con validaciones de negocio.
    * Endpoint REST con autenticación y autorización.
    * DTOs `ResponseCommand`, `InvitationResponseRequest` y `InvitationResponseResponse`.

---

## [6. REGISTRO DE PROGRESO (PROJECT LOG)]

### ✅ FASE 1 COMPLETADA - TODOS LOS PASOS IMPLEMENTADOS

**Fecha de Finalización:** 2025-11-18
**Status:** ✅ PRODUCTION READY

#### Pasos Completados:
- [✅] **PASO 1:** Estructura del Proyecto y Contexto de Organización
- [✅] **PASO 2:** Modelado del `Advanced Song Catalog Context`
- [✅] **PASO 3:** Modelado del `Smart Scheduling Context`
- [✅] **PASO 4:** Implementación del Caso de Uso "Convocar a Servicio"
- [✅] **PASO 5:** Implementación del Caso de Uso "Responder a Convocatoria"

#### Logros Técnicos:
- **Arquitectura DDD Pura:** Separación completa de capas con entidades del dominio
- **JPA Configuración:** Uso directo de entidades del dominio (sin persistence layer separado)
- **Spring Boot Integration:** Aplicación funcionando sin errores
- **Endpoints REST:** Casos de uso core implementados y documentados
- **Seguridad:** JWT + autorización por roles implementada

#### Endpoints Principales Implementados:
- `POST /api/v1/services` - Convocar equipo a servicio
- `PATCH /api/v1/services/{serviceId}/assignments/{assignmentId}` - Responder a convocatoria
- `GET /api/v1/services/{serviceId}/confirmations` - Ver estado de confirmaciones

#### Arquitectura Final:
```
api/ (Controllers + DTOs)
├── application/ (Application Services + Commands)
├── domain/ (Entities + Business Logic)
└── infrastructure/ (Repositories + External Services)
```

**Próximos Pasos Sugeridos:**
- Implementar notificaciones en tiempo real
- Agregar funcionalidades de chat grupal
- Desarrollar generador automático de setlists
- Implementar sistema de reportes y analyticsndo que solo el usuario asignado pueda responder.
* **Resultado Esperado:**
    * Código Kotlin para el nuevo método en el servicio, el nuevo endpoint en el controlador y los DTOs necesarios.

---

## [6. REGISTRO DE PROGRESO (PROJECT LOG)]
Para mantener un seguimiento claro y persistente del desarrollo, cada respuesta tuya debe incluir un registro de progreso formateado como un archivo de log en Markdown. Este registro debe visualizar el historial completo de los pasos y ser actualizado en cada respuesta.

**Formato del `PROJECT_LOG.md`:**

```markdown
# WorshipHub Project Log

**Project:** WorshipHub API
**Status:** In Progress
**Last Update:** (Aquí va la fecha y hora actual)

---
### Development Status

- [STATUS] PASO 1: Estructura del Proyecto y Contexto de Organización
- [STATUS] PASO 2: Modelado del `Advanced Song Catalog Context`
- [STATUS] PASO 3: Modelado del `Smart Scheduling Context`
- [STATUS] PASO 4: Implementación del Caso de Uso "Convocar a Servicio"
- [STATUS] PASO 5: Implementación del Caso de Uso "Responder a Convocatoria"