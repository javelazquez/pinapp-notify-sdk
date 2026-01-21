package com.pinapp.notify.adapters.email;

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
 * Tests unitarios para EmailNotificationProvider.
 * 
 * @author PinApp Team
 */
@DisplayName("EmailNotificationProvider Tests")
class EmailNotificationProviderTest {
    
    private EmailNotificationProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new EmailNotificationProvider("test-api-key");
    }
    
    @Test
    @DisplayName("Debe soportar solo el canal EMAIL")
    void shouldSupportOnlyEmailChannel() {
        assertTrue(provider.supports(ChannelType.EMAIL));
        assertFalse(provider.supports(ChannelType.SMS));
        assertFalse(provider.supports(ChannelType.PUSH));
        assertFalse(provider.supports(ChannelType.SLACK));
    }
    
    @Test
    @DisplayName("Debe retornar el nombre correcto del provider")
    void shouldReturnCorrectProviderName() {
        assertEquals("EmailProvider", provider.getName());
    }
    
    @Test
    @DisplayName("Debe enviar email exitosamente cuando todos los datos son válidos")
    void shouldSendEmailSuccessfully() {
        // Arrange
        Recipient recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "Test Subject")
        );
        Notification notification = Notification.create(
            recipient,
            "Test message",
            NotificationPriority.NORMAL
        );
        
        // Act
        NotificationResult result = provider.send(notification);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("EmailProvider", result.providerName());
        assertEquals(ChannelType.EMAIL, result.channelType());
        assertEquals(notification.id(), result.notificationId());
        assertNull(result.errorMessage());
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando el email es null")
    void shouldThrowExceptionWhenEmailIsNull() {
        // Arrange
        Recipient recipient = new Recipient(
            null,  // Sin email
            "+56912345678",
            Map.of("subject", "Test Subject")
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
        
        assertTrue(exception.getMessage().contains("email"));
        assertEquals("EmailProvider", exception.getProviderName());
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando el email está vacío")
    void shouldThrowExceptionWhenEmailIsBlank() {
        // Arrange
        Recipient recipient = new Recipient(
            "   ",  // Email en blanco
            null,
            Map.of("subject", "Test Subject")
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act & Assert
        assertThrows(ProviderException.class, () -> provider.send(notification));
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando falta el subject")
    void shouldThrowExceptionWhenSubjectIsMissing() {
        // Arrange
        Recipient recipient = new Recipient(
            "test@example.com",
            null,
            Map.of()  // Sin subject
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
        
        assertTrue(exception.getMessage().contains("subject"));
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando el subject está vacío")
    void shouldThrowExceptionWhenSubjectIsBlank() {
        // Arrange
        Recipient recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "  ")  // Subject en blanco
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act & Assert
        assertThrows(ProviderException.class, () -> provider.send(notification));
    }
    
    @Test
    @DisplayName("Debe funcionar con constructor sin parámetros")
    void shouldWorkWithDefaultConstructor() {
        // Arrange
        EmailNotificationProvider defaultProvider = new EmailNotificationProvider();
        Recipient recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "Test Subject")
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
    @DisplayName("Debe manejar mensajes largos correctamente")
    void shouldHandleLongMessages() {
        // Arrange
        String longMessage = "A".repeat(500);
        Recipient recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "Long Message Test")
        );
        Notification notification = Notification.create(
            recipient,
            longMessage,
            NotificationPriority.HIGH
        );
        
        // Act
        NotificationResult result = provider.send(notification);
        
        // Assert
        assertTrue(result.success());
    }
}
