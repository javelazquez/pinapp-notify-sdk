package com.pinapp.notify.example;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationPriority;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.providers.impl.EmailNotificationProvider;
import com.pinapp.notify.providers.impl.PushNotificationProvider;
import com.pinapp.notify.providers.impl.SmsNotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Ejemplo de uso de los Outbound Adapters (Providers).
 * 
 * <p>Esta clase demuestra cómo utilizar los diferentes providers
 * de notificación: EMAIL, SMS y PUSH, mostrando la configuración
 * necesaria y el manejo de errores.</p>
 * 
 * @author PinApp Team
 */
public class ProvidersExample {
    
    private static final Logger logger = LoggerFactory.getLogger(ProvidersExample.class);
    
    public static void main(String[] args) {
        logger.info("=== Ejemplo de uso de Outbound Adapters ===\n");
        
        // Ejemplo 1: Email Provider
        emailProviderExample();
        
        // Ejemplo 2: SMS Provider
        smsProviderExample();
        
        // Ejemplo 3: Push Provider
        pushProviderExample();
        
        // Ejemplo 4: Manejo de errores
        errorHandlingExample();
        
        logger.info("\n=== Fin de ejemplos ===");
    }
    
    /**
     * Ejemplo de uso del Email Provider.
     */
    private static void emailProviderExample() {
        logger.info("\n--- Ejemplo 1: Email Provider ---");
        
        // Configurar el provider con una API Key simulada
        EmailNotificationProvider emailProvider = new EmailNotificationProvider("email-api-key-12345");
        
        // Crear un destinatario con email y subject en metadata
        Recipient recipient = new Recipient(
            "usuario@example.com",
            null,
            Map.of("subject", "Bienvenido a PinApp")
        );
        
        // Crear la notificación
        Notification notification = Notification.create(
            recipient,
            "Hola, gracias por registrarte en nuestra plataforma. ¡Esperamos que disfrutes de nuestros servicios!",
            NotificationPriority.HIGH
        );
        
        // Enviar la notificación
        NotificationResult result = emailProvider.send(notification);
        
        logger.info("Resultado: success={}, provider={}, channel={}", 
            result.success(), result.providerName(), result.channelType());
    }
    
    /**
     * Ejemplo de uso del SMS Provider.
     */
    private static void smsProviderExample() {
        logger.info("\n--- Ejemplo 2: SMS Provider ---");
        
        // Configurar el provider con API Key y SenderId
        SmsNotificationProvider smsProvider = new SmsNotificationProvider(
            "sms-api-key-67890",
            "PinApp"
        );
        
        // Crear un destinatario con número de teléfono
        Recipient recipient = new Recipient(
            null,
            "+56912345678",
            Map.of("userId", "user-123")
        );
        
        // Crear la notificación
        Notification notification = Notification.create(
            recipient,
            "Tu código de verificación es: 123456. Válido por 5 minutos.",
            NotificationPriority.CRITICAL
        );
        
        // Enviar la notificación
        NotificationResult result = smsProvider.send(notification);
        
        logger.info("Resultado: success={}, provider={}, channel={}", 
            result.success(), result.providerName(), result.channelType());
    }
    
    /**
     * Ejemplo de uso del Push Provider.
     */
    private static void pushProviderExample() {
        logger.info("\n--- Ejemplo 3: Push Provider ---");
        
        // Configurar el provider con Server Key y Application ID
        PushNotificationProvider pushProvider = new PushNotificationProvider(
            "push-server-key-abcdef",
            "com.pinapp.mobile"
        );
        
        // Crear un destinatario con deviceToken y metadatos adicionales
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of(
                "deviceToken", "fcm-token-1234567890abcdef1234567890abcdef12345678",
                "title", "Nueva actualización disponible",
                "badge", "3",
                "sound", "notification.wav"
            )
        );
        
        // Crear la notificación
        Notification notification = Notification.create(
            recipient,
            "Hay una nueva versión de la app disponible. ¡Actualiza ahora para disfrutar de nuevas funciones!",
            NotificationPriority.NORMAL
        );
        
        // Enviar la notificación
        NotificationResult result = pushProvider.send(notification);
        
        logger.info("Resultado: success={}, provider={}, channel={}", 
            result.success(), result.providerName(), result.channelType());
    }
    
    /**
     * Ejemplo de manejo de errores.
     */
    private static void errorHandlingExample() {
        logger.info("\n--- Ejemplo 4: Manejo de Errores ---");
        
        // Intentar enviar email sin subject en metadata (debería fallar)
        try {
            EmailNotificationProvider emailProvider = new EmailNotificationProvider();
            Recipient recipient = new Recipient(
                "usuario@example.com",
                null,
                Map.of() // Sin subject
            );
            Notification notification = Notification.create(
                recipient,
                "Este email fallará porque no tiene subject"
            );
            emailProvider.send(notification);
        } catch (Exception e) {
            logger.error("Error esperado: {}", e.getMessage());
        }
        
        // Intentar enviar SMS sin teléfono (debería fallar)
        try {
            SmsNotificationProvider smsProvider = new SmsNotificationProvider();
            Recipient recipient = new Recipient(
                "usuario@example.com",
                null, // Sin teléfono
                Map.of()
            );
            Notification notification = Notification.create(
                recipient,
                "Este SMS fallará porque no tiene teléfono"
            );
            smsProvider.send(notification);
        } catch (Exception e) {
            logger.error("Error esperado: {}", e.getMessage());
        }
        
        // Intentar enviar Push sin deviceToken (debería fallar)
        try {
            PushNotificationProvider pushProvider = new PushNotificationProvider();
            Recipient recipient = new Recipient(
                null,
                null,
                Map.of("title", "Sin device token") // Sin deviceToken
            );
            Notification notification = Notification.create(
                recipient,
                "Esta push fallará porque no tiene deviceToken"
            );
            pushProvider.send(notification);
        } catch (Exception e) {
            logger.error("Error esperado: {}", e.getMessage());
        }
    }
}
