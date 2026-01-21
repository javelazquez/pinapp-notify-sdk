package com.pinapp.notify.example;

import com.pinapp.notify.adapters.mock.MockNotificationProvider;
import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.NotificationServiceImpl;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationPriority;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ValidationException;
import com.pinapp.notify.ports.in.NotificationService;

import java.util.Map;

/**
 * Ejemplo de uso de validación y templates en el SDK de notificaciones.
 * 
 * <p>Este ejemplo demuestra:</p>
 * <ul>
 *   <li>Validación automática de notificaciones antes del envío</li>
 *   <li>Uso de plantillas con variables dinámicas</li>
 *   <li>Manejo de errores de validación</li>
 *   <li>Diferentes casos de uso con templates</li>
 * </ul>
 * 
 * @author PinApp Team
 */
public class ValidationAndTemplatesExample {
    
    public static void main(String[] args) {
        System.out.println("=== Ejemplo: Validación y Templates ===\n");
        
        // 1. Configurar el SDK con proveedores mock para EMAIL y SMS
        PinappNotifyConfig config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, MockNotificationProvider.forEmail())
            .addProvider(ChannelType.SMS, MockNotificationProvider.forSms())
            .build();
        
        NotificationService notificationService = new NotificationServiceImpl(config);
        
        // 2. Ejemplo de validación exitosa con template
        demonstrateValidTemplateNotification(notificationService);
        
        // 3. Ejemplo de validación fallida - email inválido
        demonstrateInvalidEmailValidation(notificationService);
        
        // 4. Ejemplo de validación fallida - teléfono inválido
        demonstrateInvalidPhoneValidation(notificationService);
        
        // 5. Ejemplo de template con múltiples variables
        demonstrateComplexTemplate(notificationService);
        
        // 6. Ejemplo de notificación sin template
        demonstrateNoTemplateNotification(notificationService);
        
        // 7. Ejemplo de template con variable faltante
        demonstrateMissingVariableTemplate(notificationService);
    }
    
    /**
     * Demuestra una notificación válida con template.
     */
    private static void demonstrateValidTemplateNotification(NotificationService service) {
        System.out.println("\n--- 1. Notificación válida con template ---");
        
        try {
            Recipient recipient = new Recipient(
                "usuario@example.com",
                null,
                Map.of()
            );
            
            Map<String, String> variables = Map.of(
                "nombre", "Juan Pérez",
                "codigo", "ABC-123"
            );
            
            Notification notification = Notification.create(
                recipient,
                "Hola {{nombre}}, tu código de verificación es: {{codigo}}",
                NotificationPriority.HIGH,
                variables
            );
            
            NotificationResult result = service.send(notification, ChannelType.EMAIL);
            
            System.out.println("✓ Notificación enviada exitosamente");
            System.out.println("  - ID: " + result.notificationId());
            System.out.println("  - Proveedor: " + result.providerName());
            System.out.println("  - Canal: " + result.channelType());
            
        } catch (ValidationException e) {
            System.err.println("✗ Error de validación: " + e.getMessage());
        }
    }
    
    /**
     * Demuestra validación fallida por email inválido.
     */
    private static void demonstrateInvalidEmailValidation(NotificationService service) {
        System.out.println("\n--- 2. Validación fallida: Email inválido ---");
        
        try {
            Recipient recipient = new Recipient(
                "email-invalido",  // Email sin formato válido
                null,
                Map.of()
            );
            
            Notification notification = Notification.create(
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL
            );
            
            service.send(notification, ChannelType.EMAIL);
            
        } catch (ValidationException e) {
            System.out.println("✓ Validación funcionó correctamente");
            System.out.println("  - Error capturado: " + e.getMessage());
        }
    }
    
    /**
     * Demuestra validación fallida por teléfono inválido.
     */
    private static void demonstrateInvalidPhoneValidation(NotificationService service) {
        System.out.println("\n--- 3. Validación fallida: Teléfono inválido ---");
        
        try {
            Recipient recipient = new Recipient(
                null,
                "123",  // Teléfono muy corto
                Map.of()
            );
            
            Notification notification = Notification.create(
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL
            );
            
            service.send(notification, ChannelType.SMS);
            
        } catch (ValidationException e) {
            System.out.println("✓ Validación funcionó correctamente");
            System.out.println("  - Error capturado: " + e.getMessage());
        }
    }
    
    /**
     * Demuestra un template complejo con múltiples variables.
     */
    private static void demonstrateComplexTemplate(NotificationService service) {
        System.out.println("\n--- 4. Template complejo con múltiples variables ---");
        
        try {
            Recipient recipient = new Recipient(
                "cliente@empresa.com",
                null,
                Map.of()
            );
            
            String template = """
                Estimado {{nombre}},
                
                Tu pedido #{{orden}} ha sido procesado.
                Total: {{moneda}}{{monto}}
                Fecha estimada de entrega: {{fecha}}
                
                Gracias por tu compra.
                """;
            
            Map<String, String> variables = Map.of(
                "nombre", "María García",
                "orden", "ORD-2024-001",
                "moneda", "$",
                "monto", "1,250.00",
                "fecha", "25 de Enero, 2026"
            );
            
            Notification notification = Notification.create(
                recipient,
                template,
                NotificationPriority.NORMAL,
                variables
            );
            
            NotificationResult result = service.send(notification, ChannelType.EMAIL);
            
            System.out.println("✓ Template complejo procesado exitosamente");
            System.out.println("  - Variables reemplazadas: " + variables.size());
            
        } catch (ValidationException e) {
            System.err.println("✗ Error de validación: " + e.getMessage());
        }
    }
    
    /**
     * Demuestra una notificación sin template.
     */
    private static void demonstrateNoTemplateNotification(NotificationService service) {
        System.out.println("\n--- 5. Notificación sin template ---");
        
        try {
            Recipient recipient = new Recipient(
                null,
                "+5215512345678",
                Map.of()
            );
            
            Notification notification = Notification.create(
                recipient,
                "Este es un mensaje simple sin variables de template",
                NotificationPriority.LOW
            );
            
            NotificationResult result = service.send(notification, ChannelType.SMS);
            
            System.out.println("✓ Notificación sin template enviada exitosamente");
            System.out.println("  - Mensaje enviado tal cual (sin procesamiento)");
            
        } catch (ValidationException e) {
            System.err.println("✗ Error de validación: " + e.getMessage());
        }
    }
    
    /**
     * Demuestra comportamiento cuando falta una variable en el template.
     */
    private static void demonstrateMissingVariableTemplate(NotificationService service) {
        System.out.println("\n--- 6. Template con variable faltante ---");
        
        try {
            Recipient recipient = new Recipient(
                "test@test.com",
                null,
                Map.of()
            );
            
            Map<String, String> variables = Map.of(
                "nombre", "Carlos"
                // Falta la variable "codigo"
            );
            
            Notification notification = Notification.create(
                recipient,
                "Hola {{nombre}}, tu código es: {{codigo}}",
                NotificationPriority.NORMAL,
                variables
            );
            
            NotificationResult result = service.send(notification, ChannelType.EMAIL);
            
            System.out.println("✓ Notificación enviada (variable faltante reemplazada por cadena vacía)");
            System.out.println("  - Se registra advertencia en logs sobre la variable faltante");
            
        } catch (ValidationException e) {
            System.err.println("✗ Error de validación: " + e.getMessage());
        }
    }
}
