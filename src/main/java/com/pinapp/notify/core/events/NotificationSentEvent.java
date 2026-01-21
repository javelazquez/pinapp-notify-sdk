package com.pinapp.notify.core.events;

import com.pinapp.notify.domain.vo.ChannelType;

import java.time.Instant;

/**
 * Evento emitido cuando una notificación es enviada exitosamente.
 * 
 * <p>Este evento se publica después de que el proveedor confirma
 * el envío exitoso de la notificación.</p>
 * 
 * <p>Información adicional incluida:</p>
 * <ul>
 *   <li>provider: nombre del proveedor que realizó el envío</li>
 *   <li>channel: canal utilizado (EMAIL, SMS, PUSH, SLACK)</li>
 *   <li>attemptNumber: número de intento en el que tuvo éxito</li>
 * </ul>
 * 
 * @param notificationId identificador de la notificación
 * @param timestamp momento en que se envió la notificación
 * @param provider nombre del proveedor utilizado
 * @param channel canal por el que se envió
 * @param attemptNumber número de intento (1 si fue exitoso al primer intento)
 * 
 * @author PinApp Team
 */
public record NotificationSentEvent(
    String notificationId,
    Instant timestamp,
    String provider,
    ChannelType channel,
    int attemptNumber
) implements NotificationEvent {
    
    /**
     * Constructor compacto con validaciones.
     * 
     * @throws IllegalArgumentException si algún parámetro es inválido
     */
    public NotificationSentEvent {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId no puede ser null o vacío");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp no puede ser null");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider no puede ser null o vacío");
        }
        if (channel == null) {
            throw new IllegalArgumentException("channel no puede ser null");
        }
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber debe ser mayor o igual a 1");
        }
    }
}
