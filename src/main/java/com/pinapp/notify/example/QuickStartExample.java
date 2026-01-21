package com.pinapp.notify.example;

import com.pinapp.notify.adapters.mock.MockNotificationProvider;
import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.NotificationServiceImpl;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationPriority;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.in.NotificationService;

import java.util.Map;

/**
 * Ejemplo de uso rápido del SDK PinApp Notify.
 * 
 * <p>Esta clase demuestra cómo configurar y utilizar el SDK para
 * enviar notificaciones a través de diferentes canales.</p>
 * 
 * <p>Pasos básicos:</p>
 * <ol>
 *   <li>Configurar el SDK con proveedores usando el Builder</li>
 *   <li>Crear una instancia del NotificationService</li>
 *   <li>Crear notificaciones y enviarlas</li>
 * </ol>
 * 
 * @author PinApp Team
 */
public class QuickStartExample {
    
    public static void main(String[] args) {
        System.out.println("=== PinApp Notify SDK - Quick Start Example ===\n");
        
        // PASO 1: Configurar el SDK con proveedores
        System.out.println("1. Configurando el SDK...");
        PinappNotifyConfig config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, MockNotificationProvider.forEmail())
            .addProvider(ChannelType.SMS, MockNotificationProvider.forSms())
            .addProvider(ChannelType.PUSH, MockNotificationProvider.forPush())
            .addProvider(ChannelType.SLACK, MockNotificationProvider.forSlack())
            .build();
        System.out.println("   ✓ Configuración completada con 4 proveedores\n");
        
        // PASO 2: Crear el servicio de notificaciones
        System.out.println("2. Creando el servicio de notificaciones...");
        NotificationService notificationService = new NotificationServiceImpl(config);
        System.out.println("   ✓ Servicio inicializado\n");
        
        // PASO 3: Enviar notificaciones
        System.out.println("3. Enviando notificaciones...\n");
        
        // Ejemplo 1: Notificación por EMAIL
        enviarNotificacionEmail(notificationService);
        
        // Ejemplo 2: Notificación por SMS
        enviarNotificacionSms(notificationService);
        
        // Ejemplo 3: Notificación por PUSH
        enviarNotificacionPush(notificationService);
        
        // Ejemplo 4: Notificación por SLACK
        enviarNotificacionSlack(notificationService);
        
        // Ejemplo 5: Notificación con canal por defecto
        enviarNotificacionCanalesPorDefecto(notificationService);
        
        System.out.println("\n=== Ejemplo completado ===");
    }
    
    private static void enviarNotificacionEmail(NotificationService service) {
        System.out.println("   [EMAIL] Enviando notificación...");
        
        Recipient recipient = new Recipient(
            "usuario@example.com",
            null,
            Map.of()
        );
        
        Notification notification = Notification.create(
            recipient,
            "Hola! Este es un mensaje de prueba enviado por EMAIL",
            NotificationPriority.HIGH
        );
        
        NotificationResult result = service.send(notification, ChannelType.EMAIL);
        imprimirResultado(result);
    }
    
    private static void enviarNotificacionSms(NotificationService service) {
        System.out.println("   [SMS] Enviando notificación...");
        
        Recipient recipient = new Recipient(
            null,
            "+56912345678",
            Map.of()
        );
        
        Notification notification = Notification.create(
            recipient,
            "Mensaje de prueba por SMS"
        );
        
        NotificationResult result = service.send(notification, ChannelType.SMS);
        imprimirResultado(result);
    }
    
    private static void enviarNotificacionPush(NotificationService service) {
        System.out.println("   [PUSH] Enviando notificación...");
        
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("deviceToken", "abc123xyz789")
        );
        
        Notification notification = Notification.create(
            recipient,
            "Nueva actualización disponible!",
            NotificationPriority.CRITICAL
        );
        
        NotificationResult result = service.send(notification, ChannelType.PUSH);
        imprimirResultado(result);
    }
    
    private static void enviarNotificacionSlack(NotificationService service) {
        System.out.println("   [SLACK] Enviando notificación...");
        
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("slackChannelId", "#general")
        );
        
        Notification notification = Notification.create(
            recipient,
            "Deployment completado exitosamente! :rocket:"
        );
        
        NotificationResult result = service.send(notification, ChannelType.SLACK);
        imprimirResultado(result);
    }
    
    private static void enviarNotificacionCanalesPorDefecto(NotificationService service) {
        System.out.println("   [AUTO] Enviando notificación con canal por defecto...");
        
        // El servicio seleccionará EMAIL automáticamente
        Recipient recipientConEmail = new Recipient(
            "auto@example.com",
            "+56987654321",
            Map.of()
        );
        
        Notification notification1 = Notification.create(
            recipientConEmail,
            "Notificación con canal automático (debería usar EMAIL)"
        );
        
        NotificationResult result1 = service.send(notification1);
        imprimirResultado(result1);
        
        // El servicio seleccionará SMS si no hay email
        Recipient recipientSoloSms = new Recipient(
            null,
            "+56987654321",
            Map.of()
        );
        
        Notification notification2 = Notification.create(
            recipientSoloSms,
            "Notificación con canal automático (debería usar SMS)"
        );
        
        NotificationResult result2 = service.send(notification2);
        imprimirResultado(result2);
    }
    
    private static void imprimirResultado(NotificationResult result) {
        if (result.success()) {
            System.out.println("      ✓ Envío exitoso");
        } else {
            System.out.println("      ✗ Envío fallido: " + result.errorMessage());
        }
        System.out.println("      - ID: " + result.notificationId());
        System.out.println("      - Proveedor: " + result.providerName());
        System.out.println("      - Canal: " + result.channelType());
        System.out.println("      - Timestamp: " + result.timestamp());
        System.out.println();
    }
}
