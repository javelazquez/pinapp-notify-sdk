package com.pinapp.notify.core;

import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.NotificationException;
import com.pinapp.notify.ports.in.NotificationService;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para NotificationService.
 * 
 * <p>
 * Estos tests verifican la funcionalidad principal del servicio de
 * notificaciones,
 * incluyendo envíos exitosos y manejo de errores cuando no hay proveedor
 * configurado.
 * </p>
 * 
 * @author PinApp Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private NotificationProvider mockProvider;

    private PinappNotifyConfig config;
    private NotificationService service;
    private Notification notification;

    @BeforeEach
    void setUp() {
        // Configurar el mock del proveedor
        lenient().when(mockProvider.supports(ChannelType.EMAIL)).thenReturn(true);
        lenient().when(mockProvider.getName()).thenReturn("MockEmailProvider");

        // Crear una notificación de prueba
        var recipient = new Recipient(
                "test@example.com",
                null,
                Map.of());
        notification = Notification.create(recipient, "Test message");
    }

    @Test
    @DisplayName("Debe enviar notificación exitosamente y devolver success = true")
    void shouldSendNotificationSuccessfully() {
        // Arrange
        var successResult = NotificationResult.success(
                notification.id(),
                "MockEmailProvider",
                ChannelType.EMAIL);

        when(mockProvider.send(any(Notification.class))).thenReturn(successResult);

        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                .build();

        service = new NotificationServiceImpl(config);

        // Act
        var result = service.send(notification, ChannelType.EMAIL);

        // Assert
        assertAll(
                () -> assertTrue(result.success(), "El resultado debe indicar éxito"),
                () -> assertEquals(notification.id(), result.notificationId(),
                        "El ID de la notificación debe coincidir"),
                () -> assertEquals("MockEmailProvider", result.providerName(),
                        "El nombre del proveedor debe coincidir"),
                () -> assertEquals(ChannelType.EMAIL, result.channelType(),
                        "El tipo de canal debe coincidir"));

        // Verificar que se llamó al proveedor correcto
        verify(mockProvider, times(1)).send(any(Notification.class));
        verify(mockProvider, atLeastOnce()).getName();
    }

    @Test
    @DisplayName("Debe lanzar NotificationException cuando no hay proveedor configurado para el canal")
    void shouldThrowNotificationExceptionWhenProviderNotConfigured() {
        // Arrange - Configurar sin proveedor para el canal SMS
        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider) // Solo EMAIL configurado
                .build();

        service = new NotificationServiceImpl(config);

        var recipient = new Recipient(
                null,
                "+5215512345678",
                Map.of());
        var smsNotification = Notification.create(recipient, "Test SMS");

        // Act & Assert
        var exception = assertThrows(
                NotificationException.class,
                () -> service.send(smsNotification, ChannelType.SMS),
                "Debe lanzar NotificationException cuando no hay proveedor para el canal");

        assertTrue(
                exception.getMessage().contains("No hay proveedor configurado para el canal SMS"),
                "El mensaje de error debe indicar que no hay proveedor para SMS");

        // Verificar que nunca se llamó al proveedor
        verify(mockProvider, never()).send(any(Notification.class));
    }

    @Test
    @DisplayName("Debe llamar al proveedor correcto según el canal especificado")
    void shouldCallCorrectProviderForChannel() {
        // Arrange
        var smsProvider = mock(NotificationProvider.class);
        when(smsProvider.supports(ChannelType.SMS)).thenReturn(true);
        when(smsProvider.getName()).thenReturn("MockSmsProvider");

        var smsResult = NotificationResult.success(
                notification.id(),
                "MockSmsProvider",
                ChannelType.SMS);
        when(smsProvider.send(any(Notification.class))).thenReturn(smsResult);

        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                .addProvider(ChannelType.SMS, smsProvider)
                .build();

        service = new NotificationServiceImpl(config);

        var recipient = new Recipient(
                null,
                "+5215512345678",
                Map.of());
        var smsNotification = Notification.create(recipient, "Test SMS");

        // Act
        var result = service.send(smsNotification, ChannelType.SMS);

        // Assert
        assertAll(
                () -> assertTrue(result.success()),
                () -> assertEquals("MockSmsProvider", result.providerName()),
                () -> assertEquals(ChannelType.SMS, result.channelType()));

        // Verificar que se llamó al proveedor SMS, no al EMAIL
        verify(smsProvider, times(1)).send(any(Notification.class));
        verify(mockProvider, never()).send(any(Notification.class));
    }

    @Test
    @DisplayName("Debe usar el canal por defecto cuando no se especifica canal")
    void shouldUseDefaultChannelWhenNotSpecified() {
        // Arrange
        var successResult = NotificationResult.success(
                notification.id(),
                "MockEmailProvider",
                ChannelType.EMAIL);

        when(mockProvider.send(any(Notification.class))).thenReturn(successResult);

        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                .build();

        service = new NotificationServiceImpl(config);

        // Act
        var result = service.send(notification);

        // Assert
        assertAll(
                () -> assertTrue(result.success()),
                () -> assertEquals(ChannelType.EMAIL, result.channelType()));

        verify(mockProvider, times(1)).send(any(Notification.class));
    }
}
