package com.pinapp.notify.exception;

/**
 * Excepción base para todos los errores relacionados con notificaciones.
 * 
 * <p>Esta es una RuntimeException que puede ser lanzada cuando ocurre
 * cualquier error durante el procesamiento de notificaciones.</p>
 * 
 * @author PinApp Team
 */
public class NotificationException extends RuntimeException {
    
    public NotificationException(String message) {
        super(message);
    }
    
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
