package com.pinapp.notify.adapters.sms;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationPriority;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para SmsNotificationProvider.
 * 
 * @author PinApp Team
 */
@DisplayName("SmsNotificationProvider Tests")
class SmsNotificationProviderTest {
    
    private SmsNotificationProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new SmsNotificationProvider("test-api-key", "TestApp");
    }
    
    @Test
    @DisplayName("Debe soportar solo el canal SMS")
    void shouldSupportOnlySmsChannel() {
        assertTrue(provider.supports(ChannelType.SMS));
        assertFalse(provider.supports(ChannelType.EMAIL));
        assertFalse(provider.supports(ChannelType.PUSH));
        assertFalse(provider.supports(ChannelType.SLACK));
    }
    
    @Test
    @DisplayName("Debe retornar el nombre correcto del provider")
    void shouldReturnCorrectProviderName() {
        assertEquals("SmsProvider", provider.getName());
    }
    
    @Test
    @DisplayName("Debe enviar SMS exitosamente cuando todos los datos son válidos")
    void shouldSendSmsSuccessfully() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            "+56912345678",
            Map.of("userId", "user-123")
        );
        Notification notification = Notification.create(
            recipient,
            "Test SMS message",
            NotificationPriority.CRITICAL
        );
        
        // Act
        NotificationResult result = provider.send(notification);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("SmsProvider", result.providerName());
        assertEquals(ChannelType.SMS, result.channelType());
        assertEquals(notification.id(), result.notificationId());
        assertNull(result.errorMessage());
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando el teléfono es null")
    void shouldThrowExceptionWhenPhoneIsNull() {
        // Arrange
        Recipient recipient = new Recipient(
            "test@example.com",
            null,  // Sin teléfono
            Map.of()
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act & Assert
        ProviderException exception = assertThrows(
            ProviderException.class,
            () -> provider.send(notification)
        );
        
        assertTrue(exception.getMessage().contains("teléfono"));
        assertEquals("SmsProvider", exception.getProviderName());
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando el teléfono está vacío")
    void shouldThrowExceptionWhenPhoneIsBlank() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            "   ",  // Teléfono en blanco
            Map.of()
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act & Assert
        assertThrows(ProviderException.class, () -> provider.send(notification));
    }
    
    @Test
    @DisplayName("Debe funcionar con constructor que solo recibe API Key")
    void shouldWorkWithApiKeyConstructor() {
        // Arrange
        SmsNotificationProvider providerWithApiKey = new SmsNotificationProvider("api-key");
        Recipient recipient = new Recipient(
            null,
            "+56987654321",
            Map.of()
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act
        NotificationResult result = providerWithApiKey.send(notification);
        
        // Assert
        assertTrue(result.success());
    }
    
    @Test
    @DisplayName("Debe funcionar con constructor sin parámetros")
    void shouldWorkWithDefaultConstructor() {
        // Arrange
        SmsNotificationProvider defaultProvider = new SmsNotificationProvider();
        Recipient recipient = new Recipient(
            null,
            "+56912345678",
            Map.of()
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act
        NotificationResult result = defaultProvider.send(notification);
        
        // Assert
        assertTrue(result.success());
    }
    
    @Test
    @DisplayName("Debe manejar diferentes formatos de número de teléfono")
    void shouldHandleDifferentPhoneFormats() {
        // Arrange
        String[] phoneNumbers = {
            "+56912345678",
            "912345678",
            "+1-555-123-4567",
            "(555) 123-4567"
        };
        
        for (String phone : phoneNumbers) {
            Recipient recipient = new Recipient(null, phone, Map.of());
            Notification notification = Notification.create(recipient, "Test");
            
            // Act
            NotificationResult result = provider.send(notification);
            
            // Assert
            assertTrue(result.success(), "Debería funcionar con el formato: " + phone);
        }
    }
    
    @Test
    @DisplayName("Debe manejar mensajes cortos y largos")
    void shouldHandleShortAndLongMessages() {
        // Arrange - Mensaje corto
        Recipient recipient1 = new Recipient(null, "+56912345678", Map.of());
        Notification shortNotification = Notification.create(recipient1, "Hi");
        
        // Arrange - Mensaje largo
        String longMessage = "A".repeat(500);
        Recipient recipient2 = new Recipient(null, "+56912345678", Map.of());
        Notification longNotification = Notification.create(recipient2, longMessage);
        
        // Act & Assert
        assertTrue(provider.send(shortNotification).success());
        assertTrue(provider.send(longNotification).success());
    }
    
    @Test
    @DisplayName("Debe manejar diferentes prioridades")
    void shouldHandleDifferentPriorities() {
        // Arrange
        Recipient recipient = new Recipient(null, "+56912345678", Map.of());
        
        for (NotificationPriority priority : NotificationPriority.values()) {
            Notification notification = Notification.create(recipient, "Test", priority);
            
            // Act
            NotificationResult result = provider.send(notification);
            
            // Assert
            assertTrue(result.success(), "Debería funcionar con prioridad: " + priority);
        }
    }
}
