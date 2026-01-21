package com.pinapp.notify.domain;

import java.util.UUID;

/**
 * Record que representa una notificación en el dominio.
 * 
 * <p>Una notificación contiene toda la información necesaria para ser enviada
 * a través de un canal específico, incluyendo el destinatario, el contenido
 * del mensaje y la prioridad.</p>
 * 
 * @param id identificador único de la notificación
 * @param recipient el destinatario de la notificación
 * @param message el contenido del mensaje
 * @param priority la prioridad de la notificación
 * 
 * @author PinApp Team
 */
public record Notification(
        UUID id,
        Recipient recipient,
        String message,
        NotificationPriority priority
) {
    /**
     * Constructor compacto que valida los datos requeridos.
     */
    public Notification {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la notificación no puede ser null");
        }
        if (recipient == null) {
            throw new IllegalArgumentException("El destinatario no puede ser null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede ser null o vacío");
        }
        if (priority == null) {
            priority = NotificationPriority.NORMAL;
        }
    }
    
    /**
     * Crea una nueva notificación con un ID generado automáticamente.
     * 
     * @param recipient el destinatario
     * @param message el mensaje
     * @param priority la prioridad
     * @return una nueva instancia de Notification
     */
    public static Notification create(Recipient recipient, String message, 
                                      NotificationPriority priority) {
        return new Notification(UUID.randomUUID(), recipient, message, priority);
    }
    
    /**
     * Crea una nueva notificación con prioridad normal.
     * 
     * @param recipient el destinatario
     * @param message el mensaje
     * @return una nueva instancia de Notification
     */
    public static Notification create(Recipient recipient, String message) {
        return create(recipient, message, NotificationPriority.NORMAL);
    }
}
