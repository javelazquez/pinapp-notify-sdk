package com.pinapp.notify.domain;

import java.util.Map;

/**
 * Record que representa al destinatario de una notificación.
 * 
 * <p>Un destinatario puede tener múltiples formas de contacto (email, teléfono)
 * y metadatos adicionales que pueden ser utilizados por los diferentes
 * proveedores de notificación.</p>
 * 
 * @param email dirección de correo electrónico (opcional)
 * @param phone número de teléfono (opcional)
 * @param metadata mapa de metadatos adicionales (ej. userId, slackId, etc.)
 * 
 * @author PinApp Team
 */
public record Recipient(
        String email,
        String phone,
        Map<String, String> metadata
) {
    /**
     * Constructor compacto que inicializa el mapa de metadatos si es null.
     */
    public Recipient {
        if (metadata == null) {
            metadata = Map.of();
        } else {
            metadata = Map.copyOf(metadata);
        }
    }
    
    /**
     * Verifica si el destinatario tiene al menos un método de contacto válido.
     * 
     * @return true si tiene email o teléfono, false en caso contrario
     */
    public boolean hasContactInfo() {
        return (email != null && !email.isBlank()) || 
               (phone != null && !phone.isBlank());
    }
}
