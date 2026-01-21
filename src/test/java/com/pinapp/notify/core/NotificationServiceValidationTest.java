package com.pinapp.notify.core;

import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ValidationException;
import com.pinapp.notify.ports.in.NotificationService;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para la lógica de validación del NotificationService.
 * 
 * <p>Estos tests verifican que se lancen ValidationException cuando los datos
 * de entrada están mal formateados (emails o teléfonos inválidos).</p>
 * 
 * @author PinApp Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Validation Tests")
class NotificationServiceValidationTest {
    
    @Mock
    private NotificationProvider emailProvider;
    
    @Mock
    private NotificationProvider smsProvider;
    
    private PinappNotifyConfig config;
    private NotificationService service;
    
    @BeforeEach
    void setUp() {
        when(emailProvider.supports(ChannelType.EMAIL)).thenReturn(true);
        when(emailProvider.getName()).thenReturn("MockEmailProvider");
        
        when(smsProvider.supports(ChannelType.SMS)).thenReturn(true);
        when(smsProvider.getName()).thenReturn("MockSmsProvider");
        
        config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, emailProvider)
            .addProvider(ChannelType.SMS, smsProvider)
            .build();
        
        service = new NotificationServiceImpl(config);
    }
    
    @Test
    @DisplayName("Debe lanzar ValidationException con email mal formateado")
    void shouldThrowValidationExceptionForInvalidEmail() {
        // Arrange - Email sin @
        Recipient recipient = new Recipient(
            "invalid-email",
            null,
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> service.send(notification, ChannelType.EMAIL),
            "Debe lanzar ValidationException para email inválido"
        );
        
        assertTrue(
            exception.getMessage().contains("formato válido") || 
            exception.getMessage().contains("email"),
            "El mensaje debe indicar que el email es inválido"
        );
        
        // Verificar que nunca se llamó al proveedor
        verify(emailProvider, never()).send(any(Notification.class));
    }
    
    @Test
    @DisplayName("Debe lanzar ValidationException con email sin dominio")
    void shouldThrowValidationExceptionForEmailWithoutDomain() {
        // Arrange - Email sin dominio
        Recipient recipient = new Recipient(
            "user@",
            null,
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert
        assertThrows(
            ValidationException.class,
            () -> service.send(notification, ChannelType.EMAIL),
            "Debe lanzar ValidationException para email sin dominio"
        );
        
        verify(emailProvider, never()).send(any(Notification.class));
    }
    
    @Test
    @DisplayName("Debe lanzar ValidationException con email sin @")
    void shouldThrowValidationExceptionForEmailWithoutAtSymbol() {
        // Arrange
        Recipient recipient = new Recipient(
            "userexample.com",
            null,
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert
        assertThrows(
            ValidationException.class,
            () -> service.send(notification, ChannelType.EMAIL),
            "Debe lanzar ValidationException para email sin @"
        );
        
        verify(emailProvider, never()).send(any(Notification.class));
    }
    
    @Test
    @DisplayName("Debe aceptar email válido")
    void shouldAcceptValidEmail() {
        // Arrange
        Recipient recipient = new Recipient(
            "user@example.com",
            null,
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(
            () -> service.send(notification, ChannelType.EMAIL),
            "No debe lanzar excepción para email válido"
        );
    }
    
    @Test
    @DisplayName("Debe lanzar ValidationException con teléfono mal formateado")
    void shouldThrowValidationExceptionForInvalidPhone() {
        // Arrange - Teléfono sin código de país
        Recipient recipient = new Recipient(
            null,
            "1234567", // Muy corto y sin formato internacional
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> service.send(notification, ChannelType.SMS),
            "Debe lanzar ValidationException para teléfono inválido"
        );
        
        assertTrue(
            exception.getMessage().contains("formato válido") || 
            exception.getMessage().contains("teléfono"),
            "El mensaje debe indicar que el teléfono es inválido"
        );
        
        // Verificar que nunca se llamó al proveedor
        verify(smsProvider, never()).send(any(Notification.class));
    }
    
    @Test
    @DisplayName("Debe lanzar ValidationException con teléfono muy corto")
    void shouldThrowValidationExceptionForPhoneTooShort() {
        // Arrange - Teléfono con menos de 8 dígitos
        Recipient recipient = new Recipient(
            null,
            "+1234567", // Solo 7 dígitos después del +
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert
        assertThrows(
            ValidationException.class,
            () -> service.send(notification, ChannelType.SMS),
            "Debe lanzar ValidationException para teléfono muy corto"
        );
        
        verify(smsProvider, never()).send(any(Notification.class));
    }
    
    @Test
    @DisplayName("Debe lanzar ValidationException con teléfono sin código de país")
    void shouldThrowValidationExceptionForPhoneWithoutCountryCode() {
        // Arrange - Teléfono sin + y sin código de país válido
        Recipient recipient = new Recipient(
            null,
            "015512345678", // Formato local mexicano, no internacional
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert
        assertThrows(
            ValidationException.class,
            () -> service.send(notification, ChannelType.SMS),
            "Debe lanzar ValidationException para teléfono sin código de país"
        );
        
        verify(smsProvider, never()).send(any(Notification.class));
    }
    
    @Test
    @DisplayName("Debe aceptar teléfono válido con formato internacional")
    void shouldAcceptValidPhoneWithInternationalFormat() {
        // Arrange - Teléfono válido en formato E.164
        Recipient recipient = new Recipient(
            null,
            "+5215512345678", // Formato mexicano válido
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(
            () -> service.send(notification, ChannelType.SMS),
            "No debe lanzar excepción para teléfono válido"
        );
    }
    
    @Test
    @DisplayName("Debe aceptar teléfono válido sin +")
    void shouldAcceptValidPhoneWithoutPlus() {
        // Arrange - Teléfono válido sin +
        Recipient recipient = new Recipient(
            null,
            "5215512345678", // Formato válido sin +
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(
            () -> service.send(notification, ChannelType.SMS),
            "No debe lanzar excepción para teléfono válido sin +"
        );
    }
    
    @Test
    @DisplayName("Debe lanzar ValidationException cuando el email es null para canal EMAIL")
    void shouldThrowValidationExceptionWhenEmailIsNullForEmailChannel() {
        // Arrange
        Recipient recipient = new Recipient(
            null, // Email null
            "+5215512345678",
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> service.send(notification, ChannelType.EMAIL),
            "Debe lanzar ValidationException cuando email es null para canal EMAIL"
        );
        
        assertTrue(
            exception.getMessage().contains("email") || 
            exception.getMessage().contains("EMAIL"),
            "El mensaje debe indicar que se requiere email"
        );
        
        verify(emailProvider, never()).send(any(Notification.class));
    }
    
    @Test
    @DisplayName("Debe lanzar ValidationException cuando el teléfono es null para canal SMS")
    void shouldThrowValidationExceptionWhenPhoneIsNullForSmsChannel() {
        // Arrange
        Recipient recipient = new Recipient(
            "test@example.com",
            null, // Teléfono null
            Map.of()
        );
        Notification notification = Notification.create(recipient, "Test message");
        
        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> service.send(notification, ChannelType.SMS),
            "Debe lanzar ValidationException cuando teléfono es null para canal SMS"
        );
        
        assertTrue(
            exception.getMessage().contains("teléfono") || 
            exception.getMessage().contains("SMS"),
            "El mensaje debe indicar que se requiere teléfono"
        );
        
        verify(smsProvider, never()).send(any(Notification.class));
    }
}
