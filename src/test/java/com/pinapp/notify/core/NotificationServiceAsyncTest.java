package com.pinapp.notify.core;

import com.pinapp.notify.adapters.email.EmailNotificationProvider;
import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.domain.*;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.in.NotificationService;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests para las funcionalidades asíncronas del NotificationService.
 * 
 * @author PinApp Team
 */
@DisplayName("NotificationService Async Tests")
class NotificationServiceAsyncTest {

    private PinappNotifyConfig config;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
                .enableAsync()
                .build();

        service = new NotificationServiceImpl(config);
    }

    @AfterEach
    void tearDown() {
        // Cerrar el executor service
        config.shutdown(5);
    }

    @Test
    @DisplayName("Debe enviar notificación de forma asíncrona exitosamente")
    void shouldSendNotificationAsyncSuccessfully() throws Exception {
        // Arrange
        Recipient recipient = new Recipient(
                "test@example.com",
                null,
                Map.of("subject", "Test Async"));
        Notification notification = Notification.create(recipient, "Test async message");

        // Act
        CompletableFuture<NotificationResult> future = service.sendAsync(notification, ChannelType.EMAIL);

        // Assert
        assertNotNull(future);
        // No validamos isDone() porque puede completarse muy rápido en un test

        NotificationResult result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(notification.id(), result.notificationId());
        assertEquals(ChannelType.EMAIL, result.channelType());
    }

    @Test
    @DisplayName("Debe enviar notificación asíncrona usando canal por defecto")
    void shouldSendNotificationAsyncWithDefaultChannel() throws Exception {
        // Arrange
        Recipient recipient = new Recipient(
                "test@example.com",
                null,
                Map.of("subject", "Test Async Default"));
        Notification notification = Notification.create(recipient, "Test message");

        // Act
        CompletableFuture<NotificationResult> future = service.sendAsync(notification);
        NotificationResult result = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(result.success());
        assertEquals(ChannelType.EMAIL, result.channelType());
    }

    @Test
    @DisplayName("Debe manejar múltiples envíos asíncronos en paralelo")
    void shouldHandleMultipleAsyncSendsInParallel() throws Exception {
        // Arrange
        int numberOfNotifications = 10;
        CompletableFuture<NotificationResult>[] futures = new CompletableFuture[numberOfNotifications];

        // Act - Enviar múltiples notificaciones en paralelo
        for (int i = 0; i < numberOfNotifications; i++) {
            Recipient recipient = new Recipient(
                    "test" + i + "@example.com",
                    null,
                    Map.of("subject", "Test " + i));
            Notification notification = Notification.create(recipient, "Message " + i);
            futures[i] = service.sendAsync(notification, ChannelType.EMAIL);
        }

        // Esperar a que todas se completen
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(10, TimeUnit.SECONDS);

        // Assert - Todas deben haber tenido éxito
        for (CompletableFuture<NotificationResult> future : futures) {
            assertTrue(future.isDone());
            NotificationResult result = future.get();
            assertTrue(result.success());
        }
    }

    @Test
    @DisplayName("Debe procesar callbacks con thenApply")
    void shouldProcessCallbacksWithThenApply() throws Exception {
        // Arrange
        Recipient recipient = new Recipient(
                "test@example.com",
                null,
                Map.of("subject", "Test Callback"));
        Notification notification = Notification.create(recipient, "Test");

        // Act
        CompletableFuture<String> future = service.sendAsync(notification, ChannelType.EMAIL)
                .thenApply(result -> {
                    if (result.success()) {
                        return "Enviado exitosamente";
                    } else {
                        return "Fallo: " + result.errorMessage();
                    }
                });

        String message = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertEquals("Enviado exitosamente", message);
    }

    @Test
    @DisplayName("Debe funcionar sin ExecutorService configurado (usando pool por defecto)")
    void shouldWorkWithoutConfiguredExecutor() throws Exception {
        // Arrange - Crear config sin ExecutorService
        PinappNotifyConfig configWithoutExecutor = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
                .build();

        NotificationService serviceWithoutExecutor = new NotificationServiceImpl(configWithoutExecutor);

        Recipient recipient = new Recipient(
                "test@example.com",
                null,
                Map.of("subject", "Test"));
        Notification notification = Notification.create(recipient, "Test");

        // Act
        CompletableFuture<NotificationResult> future = serviceWithoutExecutor.sendAsync(notification,
                ChannelType.EMAIL);
        NotificationResult result = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(result.success());
    }

    @Test
    @DisplayName("Debe permitir composición de futures")
    void shouldAllowFutureComposition() throws Exception {
        // Arrange
        Recipient recipient = new Recipient(
                "test@example.com",
                null,
                Map.of("subject", "Test Composition"));
        Notification notification = Notification.create(recipient, "Test");

        // Act - Composición compleja
        CompletableFuture<String> composedFuture = service.sendAsync(notification, ChannelType.EMAIL)
                .thenApply(result -> result.success() ? "OK" : "FAIL")
                .thenApply(String::toLowerCase)
                .thenApply(s -> "Result: " + s);

        String finalResult = composedFuture.get(5, TimeUnit.SECONDS);

        // Assert
        assertEquals("Result: ok", finalResult);
    }

    @Test
    @DisplayName("sendAsync debe devolver CompletableFuture que se complete correctamente (con Mockito)")
    @ExtendWith(MockitoExtension.class)
    void shouldReturnCompletableFutureThatCompletesCorrectly() throws Exception {
        // Arrange - Usar Mockito para evitar conexiones reales
        NotificationProvider mockProvider = mock(NotificationProvider.class);
        lenient().when(mockProvider.supports(ChannelType.EMAIL)).thenReturn(true);
        lenient().when(mockProvider.getName()).thenReturn("MockProvider");

        Recipient recipient = new Recipient(
                "test@example.com",
                null,
                Map.of());
        Notification notification = Notification.create(recipient, "Test async message");

        NotificationResult successResult = NotificationResult.success(
                notification.id(),
                "MockProvider",
                ChannelType.EMAIL);
        when(mockProvider.send(any(Notification.class))).thenReturn(successResult);

        PinappNotifyConfig config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, mockProvider)
                .enableAsync()
                .build();

        NotificationService service = new NotificationServiceImpl(config);

        // Act
        CompletableFuture<NotificationResult> future = service.sendAsync(notification, ChannelType.EMAIL);

        // Assert
        assertNotNull(future, "El CompletableFuture no debe ser null");

        // Verificar que el future se completa correctamente
        NotificationResult result = future.get(5, TimeUnit.SECONDS);

        assertAll(
                () -> assertTrue(future.isDone(), "El future debe estar completado"),
                () -> assertFalse(future.isCancelled(), "El future no debe estar cancelado"),
                () -> assertTrue(result.success(), "El resultado debe ser exitoso"),
                () -> assertEquals(notification.id(), result.notificationId()),
                () -> assertEquals("MockProvider", result.providerName()),
                () -> assertEquals(ChannelType.EMAIL, result.channelType()));

        // Verificar que se llamó al proveedor
        verify(mockProvider, times(1)).send(any(Notification.class));

        // Cleanup
        config.shutdown(5);
    }
}
