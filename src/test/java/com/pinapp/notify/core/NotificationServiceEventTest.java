package com.pinapp.notify.core;

import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.events.NotificationEvent;
import com.pinapp.notify.core.events.NotificationEventPublisher;
import com.pinapp.notify.core.events.NotificationSentEvent;
import com.pinapp.notify.core.events.NotificationSubscriber;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.in.NotificationService;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para el sistema de eventos del NotificationService.
 * 
 * <p>
 * Estos tests verifican que los eventos se publiquen correctamente
 * y que los errores en los listeners no afecten el flujo principal.
 * </p>
 * 
 * @author PinApp Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Event System Tests")
class NotificationServiceEventTest {

    @Mock
    private NotificationProvider mockProvider;

    @Mock
    private NotificationSubscriber mockSubscriber;

    private NotificationEventPublisher eventPublisher;
    private PinappNotifyConfig config;
    private NotificationService service;
    private Notification notification;

    @BeforeEach
    void setUp() {
        // Configurar el mock del proveedor
        lenient().when(mockProvider.supports(ChannelType.EMAIL)).thenReturn(true);
        lenient().when(mockProvider.getName()).thenReturn("MockEmailProvider");

        // Crear el event publisher y suscribir el mock
        eventPublisher = new NotificationEventPublisher();
        eventPublisher.subscribe(mockSubscriber);

        // Crear una notificación de prueba
        var recipient = new Recipient(
                "test@example.com",
                null,
                Map.of());
        notification = Notification.create(recipient, "Test message");
    }

    @Test
    @DisplayName("Debe publicar NotificationSentEvent tras un envío exitoso")
    void shouldPublishNotificationSentEventAfterSuccessfulSend() {
        // Arrange
        var successResult = NotificationResult.success(
                notification.id(),
                "MockEmailProvider",
                ChannelType.EMAIL);

        when(mockProvider.send(any(Notification.class))).thenReturn(successResult);

        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                .build();

        // Reemplazar el event publisher del config con el nuestro que tiene el
        // suscriptor
        // Nota: Como el config crea su propio publisher, necesitamos suscribirnos
        // después
        service = new NotificationServiceImpl(config);

        // Suscribir el mock al publisher del config
        config.getEventPublisher().subscribe(mockSubscriber);

        // Act
        var result = service.send(notification, ChannelType.EMAIL);

        // Assert
        assertTrue(result.success(), "El envío debe ser exitoso");

        // Verificar que se llamó al suscriptor con un NotificationSentEvent
        var eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(mockSubscriber, atLeastOnce()).onEvent(eventCaptor.capture());

        var capturedEvent = eventCaptor.getValue();
        assertInstanceOf(
                NotificationSentEvent.class,
                capturedEvent,
                "El evento debe ser NotificationSentEvent");

        var sentEvent = (NotificationSentEvent) capturedEvent;
        assertAll(
                () -> assertEquals(notification.id().toString(), sentEvent.notificationId(),
                        "El ID de la notificación debe coincidir"),
                () -> assertEquals("MockEmailProvider", sentEvent.provider(),
                        "El nombre del proveedor debe coincidir"),
                () -> assertEquals(ChannelType.EMAIL, sentEvent.channel(),
                        "El canal debe coincidir"),
                () -> assertEquals(1, sentEvent.attemptNumber(),
                        "Debe ser el primer intento (exitoso)"));
    }

    @Test
    @DisplayName("Los errores en el listener no deben afectar el flujo principal")
    void shouldNotAffectMainFlowWhenListenerThrowsException() {
        // Arrange
        var successResult = NotificationResult.success(
                notification.id(),
                "MockEmailProvider",
                ChannelType.EMAIL);

        when(mockProvider.send(any(Notification.class))).thenReturn(successResult);

        // Crear un suscriptor que lanza excepción
        NotificationSubscriber failingSubscriber = event -> {
            throw new RuntimeException("Error simulado en el listener");
        };

        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                .addSubscriber(failingSubscriber) // Suscriptor que falla
                .addSubscriber(mockSubscriber) // Suscriptor que funciona
                .build();

        service = new NotificationServiceImpl(config);

        // Act - El envío debe completarse exitosamente a pesar del error en el listener
        var result = assertDoesNotThrow(
                () -> service.send(notification, ChannelType.EMAIL),
                "El envío debe completarse aunque el listener falle");

        // Assert
        assertTrue(result.success(), "El resultado debe ser exitoso");

        // Verificar que el suscriptor que funciona sí recibió el evento
        verify(mockSubscriber, atLeastOnce()).onEvent(any(NotificationSentEvent.class));
    }

    @Test
    @DisplayName("Debe notificar a múltiples suscriptores")
    void shouldNotifyMultipleSubscribers() {
        // Arrange
        var subscriber1 = mock(NotificationSubscriber.class);
        var subscriber2 = mock(NotificationSubscriber.class);
        var subscriber3 = mock(NotificationSubscriber.class);

        var successResult = NotificationResult.success(
                notification.id(),
                "MockEmailProvider",
                ChannelType.EMAIL);

        when(mockProvider.send(any(Notification.class))).thenReturn(successResult);

        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                .addSubscriber(subscriber1)
                .addSubscriber(subscriber2)
                .addSubscriber(subscriber3)
                .build();

        service = new NotificationServiceImpl(config);

        // Act
        var result = service.send(notification, ChannelType.EMAIL);

        // Assert
        assertTrue(result.success());

        // Verificar que todos los suscriptores recibieron el evento
        verify(subscriber1, times(1)).onEvent(any(NotificationSentEvent.class));
        verify(subscriber2, times(1)).onEvent(any(NotificationSentEvent.class));
        verify(subscriber3, times(1)).onEvent(any(NotificationSentEvent.class));
    }

    @Test
    @DisplayName("Debe publicar eventos incluso si algunos suscriptores fallan")
    void shouldPublishEventsEvenIfSomeSubscribersFail() {
        // Arrange
        var successCount = new AtomicInteger(0);
        var failureCount = new AtomicInteger(0);

        NotificationSubscriber workingSubscriber = event -> successCount.incrementAndGet();
        NotificationSubscriber failingSubscriber1 = event -> {
            failureCount.incrementAndGet();
            throw new RuntimeException("Error 1");
        };
        NotificationSubscriber failingSubscriber2 = event -> {
            failureCount.incrementAndGet();
            throw new IllegalStateException("Error 2");
        };

        var successResult = NotificationResult.success(
                notification.id(),
                "MockEmailProvider",
                ChannelType.EMAIL);

        when(mockProvider.send(any(Notification.class))).thenReturn(successResult);

        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                .addSubscriber(workingSubscriber)
                .addSubscriber(failingSubscriber1)
                .addSubscriber(failingSubscriber2)
                .build();

        service = new NotificationServiceImpl(config);

        // Act
        var result = service.send(notification, ChannelType.EMAIL);

        // Assert
        assertTrue(result.success(), "El envío debe ser exitoso");
        assertEquals(1, successCount.get(), "El suscriptor que funciona debe recibir el evento");
        assertEquals(2, failureCount.get(), "Los suscriptores que fallan también deben ser notificados");
    }

    @Test
    @DisplayName("Debe funcionar correctamente sin suscriptores")
    void shouldWorkCorrectlyWithoutSubscribers() {
        // Arrange
        var successResult = NotificationResult.success(
                notification.id(),
                "MockEmailProvider",
                ChannelType.EMAIL);

        when(mockProvider.send(any(Notification.class))).thenReturn(successResult);

        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                // Sin suscriptores
                .build();

        service = new NotificationServiceImpl(config);

        // Act & Assert
        var result = assertDoesNotThrow(
                () -> service.send(notification, ChannelType.EMAIL),
                "Debe funcionar sin suscriptores");

        assertTrue(result.success());
    }
}
