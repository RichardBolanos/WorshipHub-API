# Usar Cloud Native Buildpacks como recomienda Spring Boot
FROM paketobuildpacks/builder-jammy-base:latest AS builder

WORKDIR /workspace/app

# Copy source
COPY . .

# Build native image using Spring Boot buildpacks
RUN pack build worshiphub-api --builder paketobuildpacks/builder-jammy-base:latest --env BP_NATIVE_IMAGE=true

# Runtime - la imagen ya es nativa y optimizada
FROM worshiphub-api:latest

EXPOSE 8080
ENTRYPOINT ["/cnb/process/web"]