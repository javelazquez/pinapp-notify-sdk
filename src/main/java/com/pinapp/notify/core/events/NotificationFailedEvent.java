package com.pinapp.notify.core.events;

import com.pinapp.notify.domain.vo.ChannelType;

import java.time.Instant;

/**
 * Evento emitido cuando una notificación falla definitivamente después de agotar todos los reintentos.
 * 
 * <p>Este evento se publica cuando:</p>
 * <ul>
 *   <li>Se alcanzó el número máximo de reintentos sin éxito</li>
 *   <li>Ocurrió un error no recuperable</li>
 *   <li>El proveedor rechazó la notificación</li>
 * </ul>
 * 
 * <p>Información adicional incluida:</p>
 * <ul>
 *   <li>provider: nombre del proveedor que intentó el envío</li>
 *   <li>channel: canal utilizado</li>
 *   <li>errorMessage: descripción del error</li>
 *   <li>totalAttempts: número total de intentos realizados</li>
 * </ul>
 * 
 * @param notificationId identificador de la notificación
 * @param timestamp momento en que falló definitivamente
 * @param provider nombre del proveedor utilizado
 * @param channel canal por el que se intentó enviar
 * @param errorMessage mensaje descriptivo del error
 * @param totalAttempts número total de intentos realizados antes de fallar
 * 
 * @author PinApp Team
 */
public record NotificationFailedEvent(
    String notificationId,
    Instant timestamp,
    String provider,
    ChannelType channel,
    String errorMessage,
    int totalAttempts
) implements NotificationEvent {
    
    /**
     * Constructor compacto con validaciones.
     * 
     * @throws IllegalArgumentException si algún parámetro es inválido
     */
    public NotificationFailedEvent {
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
        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException("errorMessage no puede ser null o vacío");
        }
        if (totalAttempts < 1) {
            throw new IllegalArgumentException("totalAttempts debe ser mayor o igual a 1");
        }
    }
}
