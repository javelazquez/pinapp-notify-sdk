package com.pinapp.notify.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publisher de eventos del ciclo de vida de notificaciones.
 * 
 * <p>Esta clase implementa el patrón Observer de forma thread-safe,
 * permitiendo que múltiples suscriptores reciban notificaciones sobre
 * el estado de las notificaciones en tiempo real.</p>
 * 
 * <p>Características principales:</p>
 * <ul>
 *   <li><strong>Thread-Safe:</strong> Utiliza CopyOnWriteArrayList para soportar
 *       suscripciones/desuscripciones concurrentes</li>
 *   <li><strong>Publicación Segura:</strong> Si un suscriptor lanza una excepción,
 *       no afecta a otros suscriptores ni al flujo principal</li>
 *   <li><strong>Logging Detallado:</strong> Registra errores de suscriptores para debugging</li>
 *   <li><strong>Sin Dependencias Externas:</strong> Implementación pura en Java</li>
 * </ul>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * NotificationEventPublisher publisher = new NotificationEventPublisher();
 * 
 * // Registrar suscriptores
 * NotificationSubscriber metricsCollector = event -> collectMetrics(event);
 * NotificationSubscriber logger = event -> logEvent(event);
 * 
 * publisher.subscribe(metricsCollector);
 * publisher.subscribe(logger);
 * 
 * // Publicar eventos
 * publisher.publish(new NotificationSentEvent(...));
 * 
 * // Desuscribir cuando ya no sea necesario
 * publisher.unsubscribe(metricsCollector);
 * }</pre>
 * 
 * @author PinApp Team
 */
public class NotificationEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisher.class);
    
    /**
     * Lista thread-safe de suscriptores.
     * CopyOnWriteArrayList es ideal para este caso porque:
     * - Las lecturas son muy frecuentes (cada publicación)
     * - Las escrituras son raras (subscribe/unsubscribe)
     * - Proporciona iteración thread-safe sin necesidad de locks
     */
    private final List<NotificationSubscriber> subscribers;
    
    /**
     * Constructor por defecto.
     * Inicializa la lista de suscriptores vacía.
     */
    public NotificationEventPublisher() {
        this.subscribers = new CopyOnWriteArrayList<>();
        logger.debug("NotificationEventPublisher inicializado");
    }
    
    /**
     * Registra un nuevo suscriptor para recibir eventos.
     * 
     * <p>Es seguro llamar a este método desde múltiples threads concurrentemente.
     * Si el mismo suscriptor se registra múltiples veces, recibirá los eventos
     * múltiples veces (no se hace deduplicación).</p>
     * 
     * @param subscriber el suscriptor a registrar (no debe ser null)
     * @throws IllegalArgumentException si subscriber es null
     */
    public void subscribe(NotificationSubscriber subscriber) {
        if (subscriber == null) {
            throw new IllegalArgumentException("El suscriptor no puede ser null");
        }
        
        subscribers.add(subscriber);
        logger.debug("Nuevo suscriptor registrado. Total de suscriptores: {}", subscribers.size());
    }
    
    /**
     * Elimina un suscriptor de la lista de receptores de eventos.
     * 
     * <p>Es seguro llamar a este método desde múltiples threads concurrentemente.
     * Si el suscriptor no está registrado, este método no tiene efecto.</p>
     * 
     * <p>Si el mismo suscriptor fue registrado múltiples veces, solo se elimina
     * la primera ocurrencia.</p>
     * 
     * @param subscriber el suscriptor a eliminar (no debe ser null)
     * @return true si el suscriptor fue eliminado, false si no estaba registrado
     * @throws IllegalArgumentException si subscriber es null
     */
    public boolean unsubscribe(NotificationSubscriber subscriber) {
        if (subscriber == null) {
            throw new IllegalArgumentException("El suscriptor no puede ser null");
        }
        
        boolean removed = subscribers.remove(subscriber);
        
        if (removed) {
            logger.debug("Suscriptor eliminado. Total de suscriptores: {}", subscribers.size());
        } else {
            logger.debug("Intento de eliminar suscriptor no registrado");
        }
        
        return removed;
    }
    
    /**
     * Publica un evento a todos los suscriptores registrados.
     * 
     * <p>El evento se notifica síncronamente a cada suscriptor en el mismo thread.
     * Si un suscriptor lanza una excepción:</p>
     * <ul>
     *   <li>La excepción es capturada y registrada en el log</li>
     *   <li>No afecta la notificación a otros suscriptores</li>
     *   <li>No se propaga al caller</li>
     * </ul>
     * 
     * <p>Es seguro llamar a este método desde múltiples threads concurrentemente.</p>
     * 
     * @param event el evento a publicar (no debe ser null)
     * @throws IllegalArgumentException si event es null
     */
    public void publish(NotificationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento no puede ser null");
        }
        
        if (subscribers.isEmpty()) {
            logger.trace("No hay suscriptores registrados, evento no publicado: {}", 
                event.getClass().getSimpleName());
            return;
        }
        
        logger.debug("Publicando evento {} a {} suscriptor(es)", 
            event.getClass().getSimpleName(), subscribers.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        // Iterar sobre una copia segura de la lista
        // CopyOnWriteArrayList garantiza que la iteración es thread-safe
        for (NotificationSubscriber subscriber : subscribers) {
            try {
                subscriber.onEvent(event);
                successCount++;
                
            } catch (Exception e) {
                failureCount++;
                
                // Logging detallado del error sin interrumpir el flujo
                logger.error(
                    "Error al notificar suscriptor sobre evento {} [notificationId={}]. " +
                    "El suscriptor lanzó una excepción: {}. " +
                    "Este error no afecta el procesamiento de la notificación ni otros suscriptores.",
                    event.getClass().getSimpleName(),
                    event.notificationId(),
                    e.getMessage(),
                    e
                );
            }
        }
        
        if (failureCount > 0) {
            logger.warn(
                "Evento {} publicado con {} éxito(s) y {} fallo(s) [notificationId={}]",
                event.getClass().getSimpleName(),
                successCount,
                failureCount,
                event.notificationId()
            );
        } else {
            logger.trace(
                "Evento {} publicado exitosamente a {} suscriptor(es) [notificationId={}]",
                event.getClass().getSimpleName(),
                successCount,
                event.notificationId()
            );
        }
    }
    
    /**
     * Obtiene el número actual de suscriptores registrados.
     * 
     * <p>Este método es útil para debugging y testing.</p>
     * 
     * @return el número de suscriptores activos
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }
    
    /**
     * Elimina todos los suscriptores registrados.
     * 
     * <p>Este método es útil para limpieza en tests o para resetear
     * el estado del publisher.</p>
     */
    public void clear() {
        int count = subscribers.size();
        subscribers.clear();
        logger.debug("Todos los suscriptores eliminados. {} suscriptor(es) fueron removidos", count);
    }
}
