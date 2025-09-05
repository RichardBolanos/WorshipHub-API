# Configuración de Base de Datos - WorshipHub

## Requisitos Previos

- PostgreSQL 12+ instalado
- Puerto 5423 disponible (configurado en el proyecto)
- Usuario `postgres` con contraseña `admin`

## Configuración Inicial

### 1. Configurar PostgreSQL

```bash
# Conectar como superusuario
psql -U postgres -p 5423

# Ejecutar script de configuración
\i scripts/setup-database.sql
```

### 2. Verificar Configuración

```sql
-- Verificar bases de datos creadas
\l

-- Verificar usuario creado
\du

-- Conectar a la base de datos
\c worshiphub
```

## Perfiles de Configuración

### Desarrollo Local (`local`)
- **Base de datos**: `worshiphub` en localhost:5423
- **Usuario**: `postgres` / `admin`
- **Flyway**: Habilitado con baseline automático
- **JPA**: `ddl-auto=validate`
- **Logging**: DEBUG habilitado

```bash
# Iniciar con perfil local
./scripts/start-local.bat
```

### Desarrollo (`dev`)
- **Base de datos**: H2 en memoria (por defecto) o PostgreSQL
- **Flyway**: Configurable via variables de entorno
- **JPA**: `ddl-auto=create-drop` (H2) o `validate` (PostgreSQL)

### Producción (`prod`)
- **Base de datos**: PostgreSQL en servidor remoto
- **Usuario**: `worshiphub_user` (recomendado)
- **Flyway**: Habilitado, sin baseline automático
- **JPA**: `ddl-auto=validate` (solo validación)
- **SSL**: Habilitado y requerido

## Variables de Entorno

### Desarrollo Local
```bash
SPRING_PROFILES_ACTIVE=local
DATABASE_URL=jdbc:postgresql://localhost:5423/worshiphub
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=admin
```

### Producción
```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://prod-host:5432/worshiphub_prod
DATABASE_USERNAME=worshiphub_user
DATABASE_PASSWORD=secure_password
JWT_SECRET=your_secure_jwt_secret_32_chars_min
```

## Migraciones Flyway

Las migraciones se ejecutan automáticamente al iniciar la aplicación:

1. **V1**: Esquema completo inicial
2. **V2**: Claves foráneas
3. **V3**: Índices de rendimiento
4. **V4**: Tokens de autenticación
5. **V5**: Tokens de invitación

### Comandos Útiles

```bash
# Información de migraciones
curl http://localhost:9090/actuator/flyway

# Verificar estado de la base de datos
psql -U postgres -p 5423 -d worshiphub -c "\dt"
```

## Troubleshooting

### Error de Conexión
```
Caused by: org.postgresql.util.PSQLException: Connection refused
```
**Solución**: Verificar que PostgreSQL esté ejecutándose en puerto 5423

### Error de Autenticación
```
FATAL: password authentication failed for user "postgres"
```
**Solución**: Verificar credenciales en configuración

### Error de Base de Datos No Existe
```
FATAL: database "worshiphub" does not exist
```
**Solución**: Ejecutar script `setup-database.sql`

### Error de Migración Flyway
```
FlywayException: Validate failed: Migration checksum mismatch
```
**Solución**: 
- Desarrollo: Usar `flyway.clean-disabled=false`
- Producción: Revisar y corregir migraciones

## Mejores Prácticas

1. **Nunca** usar `ddl-auto=create-drop` en producción
2. **Siempre** usar Flyway para cambios de esquema
3. **Usar** usuario específico de aplicación en producción
4. **Habilitar** SSL en producción
5. **Configurar** pool de conexiones según carga esperada
6. **Monitorear** métricas de base de datos via Actuator

## Monitoreo

### Endpoints Disponibles
- `/actuator/health` - Estado de la aplicación y BD
- `/actuator/flyway` - Estado de migraciones
- `/actuator/metrics` - Métricas de rendimiento

### Métricas Importantes
- `hikaricp.connections.active` - Conexiones activas
- `hikaricp.connections.pending` - Conexiones pendientes
- `jvm.memory.used` - Uso de memoria