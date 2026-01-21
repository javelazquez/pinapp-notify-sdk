package com.pinapp.notify.domain.vo;

/**
 * Value Object que representa los tipos de canales de notificación soportados.
 * 
 * @author PinApp Team
 */
public enum ChannelType {
    /**
     * Canal de correo electrónico.
     */
    EMAIL,
    
    /**
     * Canal de mensajería SMS.
     */
    SMS,
    
    /**
     * Canal de notificaciones push.
     */
    PUSH,
    
    /**
     * Canal de Slack.
     */
    SLACK
}
