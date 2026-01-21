package com.pinapp.notify.domain;

import com.pinapp.notify.domain.vo.ChannelType;
import java.time.Instant;
import java.util.UUID;

/**
 * Record que representa el resultado de un envío de notificación.
 * 
 * <p>Contiene información sobre el éxito o fracaso del envío,
 * incluyendo detalles del proveedor utilizado y cualquier mensaje de error.</p>
 * 
 * @param notificationId el ID de la notificación procesada
 * @param success indica si el envío fue exitoso
 * @param providerName el nombre del proveedor utilizado
 * @param channelType el tipo de canal utilizado
 * @param timestamp el momento en que se procesó la notificación
 * @param errorMessage mensaje de error si el envío falló (opcional)
 * 
 * @author PinApp Team
 */
public record NotificationResult(
        UUID notificationId,
        boolean success,
        String providerName,
        ChannelType channelType,
        Instant timestamp,
        String errorMessage
) {
    /**
     * Constructor compacto que valida los datos requeridos.
     */
    public NotificationResult {
        if (notificationId == null) {
            throw new IllegalArgumentException("El ID de la notificación no puede ser null");
        }
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor no puede ser null o vacío");
        }
        if (channelType == null) {
            throw new IllegalArgumentException("El tipo de canal no puede ser null");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
    
    /**
     * Crea un resultado exitoso.
     * 
     * @param notificationId el ID de la notificación
     * @param providerName el nombre del proveedor
     * @param channelType el tipo de canal
     * @return un NotificationResult con success = true
     */
    public static NotificationResult success(UUID notificationId, String providerName,
                                            ChannelType channelType) {
        return new NotificationResult(
            notificationId,
            true,
            providerName,
            channelType,
            Instant.now(),
            null
        );
    }
    
    /**
     * Crea un resultado fallido.
     * 
     * @param notificationId el ID de la notificación
     * @param providerName el nombre del proveedor
     * @param channelType el tipo de canal
     * @param errorMessage el mensaje de error
     * @return un NotificationResult con success = false
     */
    public static NotificationResult failure(UUID notificationId, String providerName,
                                            ChannelType channelType,
                                            String errorMessage) {
        return new NotificationResult(
            notificationId,
            false,
            providerName,
            channelType,
            Instant.now(),
            errorMessage
        );
    }
}
