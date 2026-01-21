package com.pinapp.notify.domain;

import lombok.Builder;
import java.util.Map;
import java.util.UUID;

/**
 * Record que representa una notificación en el dominio.
 * 
 * <p>
 * Una notificación contiene toda la información necesaria para ser enviada
 * a través de un canal específico, incluyendo el destinatario, el contenido
 * del mensaje, la prioridad y opcionalmente variables para templates.
 * </p>
 * 
 * @param id                identificador único de la notificación
 * @param recipient         el destinatario de la notificación
 * @param message           el contenido del mensaje (puede contener variables
 *                          en formato {{key}})
 * @param priority          la prioridad de la notificación
 * @param templateVariables mapa de variables para reemplazar en el mensaje
 *                          (opcional)
 * 
 * @author PinApp Team
 */
@Builder
public record Notification(
        UUID id,
        Recipient recipient,
        String message,
        NotificationPriority priority,
        Map<String, String> templateVariables) {
    /**
     * Constructor compacto que valida los datos requeridos.
     */
    public Notification {
        if (id == null) {
            id = UUID.randomUUID();
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
        if (templateVariables == null) {
            templateVariables = Map.of();
        } else {
            templateVariables = Map.copyOf(templateVariables);
        }
    }

    /**
     * Crea una nueva notificación con un ID generado automáticamente.
     * 
     * @param recipient         el destinatario
     * @param message           el mensaje
     * @param priority          la prioridad
     * @param templateVariables las variables del template
     * @return una nueva instancia de Notification
     */
    public static Notification create(Recipient recipient, String message,
            NotificationPriority priority,
            Map<String, String> templateVariables) {
        return new Notification(UUID.randomUUID(), recipient, message, priority, templateVariables);
    }

    /**
     * Crea una nueva notificación con prioridad normal y variables de template.
     * 
     * @param recipient         el destinatario
     * @param message           el mensaje
     * @param templateVariables las variables del template
     * @return una nueva instancia de Notification
     */
    public static Notification create(Recipient recipient, String message,
            Map<String, String> templateVariables) {
        return create(recipient, message, NotificationPriority.NORMAL, templateVariables);
    }

    /**
     * Crea una nueva notificación con un ID generado automáticamente.
     * 
     * @param recipient el destinatario
     * @param message   el mensaje
     * @param priority  la prioridad
     * @return una nueva instancia de Notification
     */
    public static Notification create(Recipient recipient, String message,
            NotificationPriority priority) {
        return new Notification(UUID.randomUUID(), recipient, message, priority, Map.of());
    }

    /**
     * Crea una nueva notificación con prioridad normal.
     * 
     * @param recipient el destinatario
     * @param message   el mensaje
     * @return una nueva instancia de Notification
     */
    public static Notification create(Recipient recipient, String message) {
        return create(recipient, message, NotificationPriority.NORMAL);
    }

    /**
     * Verifica si esta notificación tiene variables de template.
     * 
     * @return true si tiene variables, false en caso contrario
     */
    public boolean hasTemplateVariables() {
        return templateVariables != null && !templateVariables.isEmpty();
    }
}
