package com.pinapp.notify.providers.impl;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ProviderException;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Implementación del proveedor de notificaciones push.
 * 
 * <p>Este proveedor simula el envío de notificaciones push a dispositivos
 * móviles sin realizar conexiones HTTP reales. Valida que el destinatario
 * tenga un 'deviceToken' válido en sus metadatos antes de procesar el envío.</p>
 * 
 * <p>Características:</p>
 * <ul>
 *   <li>Validación de deviceToken en metadatos del destinatario</li>
 *   <li>Logging estructurado del envío simulado</li>
 *   <li>Generación de messageId único (UUID)</li>
 *   <li>Soporte para configuración de servidor y certificados</li>
 * </ul>
 * 
 * @author PinApp Team
 */
public class PushNotificationProvider implements NotificationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationProvider.class);
    private static final String PROVIDER_NAME = "PushProvider";
    private static final String METADATA_DEVICE_TOKEN_KEY = "deviceToken";
    
    private final String serverKey;
    private final String applicationId;
    
    /**
     * Constructor completo que acepta configuración del proveedor.
     * 
     * @param serverKey la clave del servidor para autenticación (ej. FCM Server Key)
     * @param applicationId el identificador de la aplicación
     */
    public PushNotificationProvider(String serverKey, String applicationId) {
        this.serverKey = serverKey;
        this.applicationId = applicationId != null ? applicationId : "com.pinapp.default";
        logger.debug("[PUSH PROVIDER] Inicializado con Server Key: {}*** | App ID: {}", 
            serverKey != null && serverKey.length() > 4 ? serverKey.substring(0, 4) : "****",
            this.applicationId);
    }
    
    /**
     * Constructor que acepta solo Server Key.
     * 
     * @param serverKey la clave del servidor para autenticación
     */
    public PushNotificationProvider(String serverKey) {
        this(serverKey, null);
    }
    
    /**
     * Constructor por defecto sin configuración.
     */
    public PushNotificationProvider() {
        this(null, null);
    }
    
    @Override
    public boolean supports(ChannelType channel) {
        return ChannelType.PUSH.equals(channel);
    }
    
    @Override
    public NotificationResult send(Notification notification) {
        logger.info("[PUSH PROVIDER] Procesando notificación [id={}]", notification.id());
        
        // Validar que el destinatario tenga deviceToken en metadata
        Recipient recipient = notification.recipient();
        String deviceToken = recipient.metadata().get(METADATA_DEVICE_TOKEN_KEY);
        
        if (deviceToken == null || deviceToken.isBlank()) {
            String errorMsg = "El destinatario no tiene un 'deviceToken' válido en los metadatos";
            logger.error("[PUSH PROVIDER] Error: {}", errorMsg);
            throw new ProviderException(PROVIDER_NAME, errorMsg);
        }
        
        // Extraer información adicional de los metadatos (opcional)
        String title = recipient.metadata().getOrDefault("title", "Notificación");
        String badge = recipient.metadata().getOrDefault("badge", "1");
        String sound = recipient.metadata().getOrDefault("sound", "default");
        
        // Simular el envío de la notificación push
        String message = notification.message();
        String messageId = UUID.randomUUID().toString();
        
        logger.info("[PUSH PROVIDER] Sending to device: {} | App: {} | Title: {} | Message: {} | MessageId: {}", 
            truncateToken(deviceToken), applicationId, title, truncateMessage(message), messageId);
        
        logger.debug("[PUSH PROVIDER] Priority: {} | Badge: {} | Sound: {} | Server Key configured: {}", 
            notification.priority(), badge, sound, serverKey != null);
        
        logger.info("[PUSH PROVIDER] ✓ Push notification enviada exitosamente [messageId={}]", messageId);
        
        return NotificationResult.success(
            notification.id(),
            PROVIDER_NAME,
            ChannelType.PUSH
        );
    }
    
    @Override
    public String getName() {
        return PROVIDER_NAME;
    }
    
    /**
     * Trunca el mensaje para logging si es muy largo.
     * 
     * @param message el mensaje a truncar
     * @return el mensaje truncado
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 100 ? message.substring(0, 100) + "..." : message;
    }
    
    /**
     * Trunca el device token para logging (por seguridad).
     * 
     * @param token el token a truncar
     * @return el token truncado
     */
    private String truncateToken(String token) {
        if (token == null || token.length() < 10) {
            return "****";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
