package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.push.PushEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Listener de Spring Application Events que captura eventos [PushEvent]
 * publicados por los servicios de aplicación y los delega al
 * [PushNotificationService] para procesamiento asíncrono.
 *
 * Al usar un solo método con el tipo base [PushEvent] (sealed class),
 * se capturan automáticamente todos los subtipos de evento sin necesidad
 * de métodos individuales por subtipo.
 *
 * Validates: Requirements 13.5
 */
@Component
class PushNotificationEventListener(
    private val pushNotificationService: PushNotificationService
) {

    private val logger = LoggerFactory.getLogger(PushNotificationEventListener::class.java)

    /**
     * Escucha todos los eventos de tipo [PushEvent] publicados vía
     * [org.springframework.context.ApplicationEventPublisher].
     *
     * Delega al [PushNotificationService.processPushEvent] que se ejecuta
     * de forma asíncrona en el thread pool `pushNotificationExecutor`,
     * garantizando que el envío de notificaciones no bloquea la operación
     * principal del usuario.
     */
    @EventListener
    fun handlePushEvent(event: PushEvent) {
        logger.debug("Received push event: {}", event::class.simpleName)
        pushNotificationService.processPushEvent(event)
    }
}
