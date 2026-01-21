package com.pinapp.notify.core.events;

import com.pinapp.notify.domain.vo.ChannelType;

import java.time.Instant;

/**
 * Evento emitido cuando se va a realizar un reintento de envío de una notificación.
 * 
 * <p>Este evento se publica antes de cada reintento (no en el primer intento),
 * permitiendo observar la lógica de resiliencia en acción.</p>
 * 
 * <p>Información adicional incluida:</p>
 * <ul>
 *   <li>provider: nombre del proveedor que realizará el reintento</li>
 *   <li>channel: canal utilizado</li>
 *   <li>attemptNumber: número del intento actual (2, 3, etc.)</li>
 *   <li>maxAttempts: número máximo de intentos configurado</li>
 *   <li>delayMs: tiempo de espera antes de este reintento en milisegundos</li>
 *   <li>reason: razón del reintento (mensaje de error del intento anterior)</li>
 * </ul>
 * 
 * @param notificationId identificador de la notificación
 * @param timestamp momento en que se programa el reintento
 * @param provider nombre del proveedor que realizará el reintento
 * @param channel canal por el que se reintentará
 * @param attemptNumber número del intento actual
 * @param maxAttempts número máximo de intentos configurado
 * @param delayMs tiempo de espera antes de este reintento (ms)
 * @param reason razón del reintento (error del intento previo)
 * 
 * @author PinApp Team
 */
public record NotificationRetryEvent(
    String notificationId,
    Instant timestamp,
    String provider,
    ChannelType channel,
    int attemptNumber,
    int maxAttempts,
    long delayMs,
    String reason
) implements NotificationEvent {
    
    /**
     * Constructor compacto con validaciones.
     * 
     * @throws IllegalArgumentException si algún parámetro es inválido
     */
    public NotificationRetryEvent {
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
        if (attemptNumber < 2) {
            throw new IllegalArgumentException("attemptNumber debe ser mayor o igual a 2 (los reintentos empiezan desde el 2do intento)");
        }
        if (maxAttempts < attemptNumber) {
            throw new IllegalArgumentException("maxAttempts debe ser mayor o igual a attemptNumber");
        }
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs no puede ser negativo");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason no puede ser null o vacío");
        }
    }
}
