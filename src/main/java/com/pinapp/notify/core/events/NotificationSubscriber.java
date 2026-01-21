package com.pinapp.notify.core.events;

/**
 * Interfaz funcional que define un suscriptor de eventos del ciclo de vida de notificaciones.
 * 
 * <p>Los suscriptores pueden implementar esta interfaz para recibir notificaciones
 * en tiempo real sobre el estado de las notificaciones: envíos exitosos, fallos y reintentos.</p>
 * 
 * <p>Esta interfaz es funcional, lo que permite utilizar lambdas y method references:</p>
 * 
 * <pre>{@code
 * // Como lambda
 * NotificationSubscriber subscriber = event -> {
 *     System.out.println("Evento recibido: " + event);
 * };
 * 
 * // Con Pattern Matching (Java 21)
 * NotificationSubscriber subscriber = event -> {
 *     switch (event) {
 *         case NotificationSentEvent sent -> 
 *             handleSuccess(sent);
 *         case NotificationFailedEvent failed -> 
 *             handleFailure(failed);
 *         case NotificationRetryEvent retry -> 
 *             handleRetry(retry);
 *     }
 * };
 * 
 * // Como method reference
 * NotificationSubscriber subscriber = this::handleEvent;
 * }</pre>
 * 
 * <p><strong>IMPORTANTE - Manejo de Excepciones:</strong></p>
 * <p>Las implementaciones deben ser seguras y no lanzar excepciones no controladas.
 * Si un suscriptor lanza una excepción, será capturada y registrada por el publisher,
 * pero no afectará el flujo principal de la librería ni a otros suscriptores.</p>
 * 
 * <p><strong>Best Practices:</strong></p>
 * <ul>
 *   <li>Mantener la implementación de onEvent rápida y no bloqueante</li>
 *   <li>Si requiere procesamiento pesado, delegar a un thread pool separado</li>
 *   <li>Manejar todas las excepciones internamente</li>
 *   <li>Usar logging apropiado para debugging</li>
 * </ul>
 * 
 * @author PinApp Team
 * @see NotificationEvent
 * @see NotificationEventPublisher
 */
@FunctionalInterface
public interface NotificationSubscriber {
    
    /**
     * Método llamado cuando ocurre un evento en el ciclo de vida de una notificación.
     * 
     * <p>Este método es invocado síncronamente por el publisher en el mismo thread
     * que está procesando la notificación. Las implementaciones deben:</p>
     * <ul>
     *   <li>Ejecutar rápidamente (evitar operaciones bloqueantes)</li>
     *   <li>No lanzar excepciones no controladas</li>
     *   <li>Delegar procesamiento pesado a threads separados</li>
     * </ul>
     * 
     * @param event el evento que ocurrió (nunca null)
     */
    void onEvent(NotificationEvent event);
}
