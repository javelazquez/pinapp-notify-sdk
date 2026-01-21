package com.pinapp.notify.providers.impl;

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
 * Tests unitarios para PushNotificationProvider.
 * 
 * @author PinApp Team
 */
@DisplayName("PushNotificationProvider Tests")
class PushNotificationProviderTest {
    
    private PushNotificationProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new PushNotificationProvider("test-server-key", "com.test.app");
    }
    
    @Test
    @DisplayName("Debe soportar solo el canal PUSH")
    void shouldSupportOnlyPushChannel() {
        assertTrue(provider.supports(ChannelType.PUSH));
        assertFalse(provider.supports(ChannelType.EMAIL));
        assertFalse(provider.supports(ChannelType.SMS));
        assertFalse(provider.supports(ChannelType.SLACK));
    }
    
    @Test
    @DisplayName("Debe retornar el nombre correcto del provider")
    void shouldReturnCorrectProviderName() {
        assertEquals("PushProvider", provider.getName());
    }
    
    @Test
    @DisplayName("Debe enviar push notification exitosamente con todos los metadatos")
    void shouldSendPushSuccessfullyWithAllMetadata() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of(
                "deviceToken", "fcm-token-1234567890abcdef",
                "title", "Test Title",
                "badge", "5",
                "sound", "notification.mp3"
            )
        );
        Notification notification = Notification.create(
            recipient,
            "Test push message",
            NotificationPriority.HIGH
        );
        
        // Act
        NotificationResult result = provider.send(notification);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("PushProvider", result.providerName());
        assertEquals(ChannelType.PUSH, result.channelType());
        assertEquals(notification.id(), result.notificationId());
        assertNull(result.errorMessage());
    }
    
    @Test
    @DisplayName("Debe enviar push exitosamente solo con deviceToken")
    void shouldSendPushSuccessfullyWithOnlyDeviceToken() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("deviceToken", "device-token-xyz")
        );
        Notification notification = Notification.create(
            recipient,
            "Simple push message"
        );
        
        // Act
        NotificationResult result = provider.send(notification);
        
        // Assert
        assertTrue(result.success());
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando falta el deviceToken")
    void shouldThrowExceptionWhenDeviceTokenIsMissing() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("title", "Test")  // Sin deviceToken
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
        
        assertTrue(exception.getMessage().contains("deviceToken"));
        assertEquals("PushProvider", exception.getProviderName());
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando el deviceToken está vacío")
    void shouldThrowExceptionWhenDeviceTokenIsBlank() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("deviceToken", "   ")  // deviceToken en blanco
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act & Assert
        assertThrows(ProviderException.class, () -> provider.send(notification));
    }
    
    @Test
    @DisplayName("Debe lanzar ProviderException cuando metadata está vacío")
    void shouldThrowExceptionWhenMetadataIsEmpty() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of()  // Metadata vacío
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act & Assert
        assertThrows(ProviderException.class, () -> provider.send(notification));
    }
    
    @Test
    @DisplayName("Debe funcionar con constructor que solo recibe Server Key")
    void shouldWorkWithServerKeyConstructor() {
        // Arrange
        PushNotificationProvider providerWithServerKey = new PushNotificationProvider("server-key");
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("deviceToken", "device-token-123")
        );
        Notification notification = Notification.create(
            recipient,
            "Test message"
        );
        
        // Act
        NotificationResult result = providerWithServerKey.send(notification);
        
        // Assert
        assertTrue(result.success());
    }
    
    @Test
    @DisplayName("Debe funcionar con constructor sin parámetros")
    void shouldWorkWithDefaultConstructor() {
        // Arrange
        PushNotificationProvider defaultProvider = new PushNotificationProvider();
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("deviceToken", "device-token-456")
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
    @DisplayName("Debe usar valores por defecto para metadatos opcionales")
    void shouldUseDefaultValuesForOptionalMetadata() {
        // Arrange - Solo deviceToken, sin title, badge, sound
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("deviceToken", "device-token-789")
        );
        Notification notification = Notification.create(
            recipient,
            "Test with defaults"
        );
        
        // Act - No debería lanzar excepción
        NotificationResult result = provider.send(notification);
        
        // Assert
        assertTrue(result.success());
    }
    
    @Test
    @DisplayName("Debe manejar deviceTokens largos")
    void shouldHandleLongDeviceTokens() {
        // Arrange
        String longToken = "a".repeat(200);
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("deviceToken", longToken)
        );
        Notification notification = Notification.create(
            recipient,
            "Test with long token"
        );
        
        // Act
        NotificationResult result = provider.send(notification);
        
        // Assert
        assertTrue(result.success());
    }
    
    @Test
    @DisplayName("Debe manejar diferentes prioridades")
    void shouldHandleDifferentPriorities() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of("deviceToken", "device-token-priority")
        );
        
        for (NotificationPriority priority : NotificationPriority.values()) {
            Notification notification = Notification.create(recipient, "Test", priority);
            
            // Act
            NotificationResult result = provider.send(notification);
            
            // Assert
            assertTrue(result.success(), "Debería funcionar con prioridad: " + priority);
        }
    }
    
    @Test
    @DisplayName("Debe permitir metadatos adicionales sin causar problemas")
    void shouldAllowAdditionalMetadata() {
        // Arrange
        Recipient recipient = new Recipient(
            null,
            null,
            Map.of(
                "deviceToken", "device-token-extra",
                "customKey1", "value1",
                "customKey2", "value2",
                "userId", "12345"
            )
        );
        Notification notification = Notification.create(
            recipient,
            "Test with extra metadata"
        );
        
        // Act
        NotificationResult result = provider.send(notification);
        
        // Assert
        assertTrue(result.success());
    }
}
