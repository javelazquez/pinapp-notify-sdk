package com.pinapp.notify.domain;

/**
 * Enum que representa la prioridad de una notificación.
 * 
 * @author PinApp Team
 */
public enum NotificationPriority {
    /**
     * Prioridad baja. Las notificaciones pueden ser procesadas con menor urgencia.
     */
    LOW,
    
    /**
     * Prioridad normal. Prioridad por defecto para la mayoría de notificaciones.
     */
    NORMAL,
    
    /**
     * Prioridad alta. Las notificaciones deben ser procesadas con mayor urgencia.
     */
    HIGH,
    
    /**
     * Prioridad crítica. Las notificaciones deben ser procesadas inmediatamente.
     */
    CRITICAL
}
