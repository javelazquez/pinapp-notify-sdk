package com.pinapp.notify.core.events;

import java.time.Instant;

/**
 * Interfaz sellada que representa un evento del ciclo de vida de una notificación.
 * 
 * <p>Esta interfaz utiliza las características de Java 21 (Sealed Classes) para
 * garantizar que solo los tipos de eventos conocidos puedan ser implementados.
 * Esto permite utilizar Pattern Matching exhaustivo en los suscriptores.</p>
 * 
 * <p>Todos los eventos contienen:</p>
 * <ul>
 *   <li>notificationId: identificador único de la notificación</li>
 *   <li>timestamp: momento exacto en que ocurrió el evento</li>
 * </ul>
 * 
 * <p>Ejemplo de uso con Pattern Matching:</p>
 * <pre>{@code
 * subscriber.onEvent(event -> {
 *     switch (event) {
 *         case NotificationSentEvent sent -> 
 *             logger.info("Enviado a través de: {}", sent.provider());
 *         case NotificationFailedEvent failed -> 
 *             logger.error("Falló: {}", failed.errorMessage());
 *         case NotificationRetryEvent retry -> 
 *             logger.warn("Reintento {}/{}", retry.attemptNumber(), retry.maxAttempts());
 *     }
 * });
 * }</pre>
 * 
 * @author PinApp Team
 * @see NotificationSentEvent
 * @see NotificationFailedEvent
 * @see NotificationRetryEvent
 */
public sealed interface NotificationEvent 
    permits NotificationSentEvent, NotificationFailedEvent, NotificationRetryEvent {
    
    /**
     * Identificador único de la notificación asociada a este evento.
     * 
     * @return el ID de la notificación
     */
    String notificationId();
    
    /**
     * Timestamp exacto en que ocurrió el evento.
     * 
     * @return el instante temporal del evento
     */
    Instant timestamp();
}
