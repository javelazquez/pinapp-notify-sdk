package com.pinapp.notify.exception;

/**
 * Excepción lanzada cuando los datos de una notificación están mal formados
 * o no cumplen con las validaciones requeridas.
 * 
 * <p>Esta excepción debe ser lanzada cuando se detectan problemas de validación
 * antes de intentar enviar la notificación.</p>
 * 
 * @author PinApp Team
 */
public class ValidationException extends NotificationException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
