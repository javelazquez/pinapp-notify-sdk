package com.pinapp.notify.core;

import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.domain.*;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ProviderException;
import com.pinapp.notify.ports.in.NotificationService;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests para la funcionalidad de reintentos del NotificationService.
 * 
 * @author PinApp Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Retry Tests")
class NotificationServiceRetryTest {
    
    @Test
    @DisplayName("Debe reintentar cuando el provider falla")
    void shouldRetryWhenProviderFails() {
        // Arrange
        var attemptCounter = new AtomicInteger(0);
        
        var flakeyProvider = new NotificationProvider() {
            @Override
            public boolean supports(ChannelType channel) {
                return channel == ChannelType.EMAIL;
            }
            
            @Override
            public NotificationResult send(Notification notification) {
                int attempt = attemptCounter.incrementAndGet();
                if (attempt < 3) {
                    // Fallar los primeros 2 intentos
                    throw new ProviderException("FlakeyProvider", "Fallo simulado intento " + attempt);
                }
                // Éxito en el tercer intento
                return NotificationResult.success(notification.id(), "FlakeyProvider", ChannelType.EMAIL);
            }
            
            @Override
            public String getName() {
                return "FlakeyProvider";
            }
        };
        
        var config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, flakeyProvider)
            .withRetryPolicy(RetryPolicy.of(3, 100)) // 3 intentos, 100ms entre reintentos
            .build();
        
        var service = new NotificationServiceImpl(config);
        
        var recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "Test Retry")
        );
        var notification = Notification.create(recipient, "Test message");
        
        // Act
        var result = service.send(notification, ChannelType.EMAIL);
        
        // Assert
        assertEquals(3, attemptCounter.get()); // Debe haber intentado 3 veces
        assertTrue(result.success()); // Debe tener éxito en el tercer intento
    }
    
    @Test
    @DisplayName("Debe fallar después de agotar todos los reintentos")
    void shouldFailAfterExhaustingAllRetries() {
        // Arrange
        var attemptCounter = new AtomicInteger(0);
        
        var alwaysFailProvider = new NotificationProvider() {
            @Override
            public boolean supports(ChannelType channel) {
                return channel == ChannelType.EMAIL;
            }
            
            @Override
            public NotificationResult send(Notification notification) {
                attemptCounter.incrementAndGet();
                throw new ProviderException("AlwaysFailProvider", "Siempre falla");
            }
            
            @Override
            public String getName() {
                return "AlwaysFailProvider";
            }
        };
        
        var config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, alwaysFailProvider)
            .withRetryPolicy(RetryPolicy.of(3, 50))
            .build();
        
        var service = new NotificationServiceImpl(config);
        
        var recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "Test")
        );
        var notification = Notification.create(recipient, "Test");
        
        // Act
        var result = service.send(notification, ChannelType.EMAIL);
        
        // Assert
        assertEquals(3, attemptCounter.get()); // Debe haber intentado 3 veces
        assertFalse(result.success()); // Debe fallar
        assertTrue(result.errorMessage().contains("Falló después de 3 intentos"));
    }
    
    @Test
    @DisplayName("Debe funcionar sin reintentos cuando se configura noRetry")
    void shouldWorkWithoutRetriesWhenConfiguredNoRetry() {
        // Arrange
        var attemptCounter = new AtomicInteger(0);
        
        var provider = new NotificationProvider() {
            @Override
            public boolean supports(ChannelType channel) {
                return channel == ChannelType.EMAIL;
            }
            
            @Override
            public NotificationResult send(Notification notification) {
                attemptCounter.incrementAndGet();
                throw new ProviderException("Provider", "Error");
            }
            
            @Override
            public String getName() {
                return "Provider";
            }
        };
        
        var config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, provider)
            .withoutRetries() // Sin reintentos
            .build();
        
        var service = new NotificationServiceImpl(config);
        
        var recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "Test")
        );
        var notification = Notification.create(recipient, "Test");
        
        // Act
        var result = service.send(notification, ChannelType.EMAIL);
        
        // Assert
        assertEquals(1, attemptCounter.get()); // Solo un intento
        assertFalse(result.success());
    }
    
    @Test
    @DisplayName("Debe respetar el delay entre reintentos")
    void shouldRespectDelayBetweenRetries() {
        // Arrange
        var attemptCounter = new AtomicInteger(0);
        var attemptTimes = new long[3];
        
        var provider = new NotificationProvider() {
            @Override
            public boolean supports(ChannelType channel) {
                return channel == ChannelType.EMAIL;
            }
            
            @Override
            public NotificationResult send(Notification notification) {
                int attempt = attemptCounter.getAndIncrement();
                attemptTimes[attempt] = System.currentTimeMillis();
                
                if (attempt < 2) {
                    throw new ProviderException("Provider", "Error intento " + (attempt + 1));
                }
                return NotificationResult.success(notification.id(), "Provider", ChannelType.EMAIL);
            }
            
            @Override
            public String getName() {
                return "Provider";
            }
        };
        
        var config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, provider)
            .withRetryPolicy(RetryPolicy.of(3, 200)) // 200ms de delay
            .build();
        
        var service = new NotificationServiceImpl(config);
        
        var recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "Test")
        );
        var notification = Notification.create(recipient, "Test");
        
        // Act
        var result = service.send(notification, ChannelType.EMAIL);
        
        // Assert
        assertTrue(result.success());
        assertEquals(3, attemptCounter.get());
        
        // Verificar que hubo delay entre intentos (con tolerancia de 50ms)
        var delay1 = attemptTimes[1] - attemptTimes[0];
        var delay2 = attemptTimes[2] - attemptTimes[1];
        
        assertTrue(delay1 >= 150, "Delay entre intento 1 y 2 debe ser >= 150ms, fue: " + delay1);
        assertTrue(delay2 >= 150, "Delay entre intento 2 y 3 debe ser >= 150ms, fue: " + delay2);
    }
    
    @Test
    @DisplayName("Debe usar la política de reintentos por defecto si no se configura")
    void shouldUseDefaultRetryPolicyIfNotConfigured() {
        // Arrange
        var attemptCounter = new AtomicInteger(0);
        
        var provider = new NotificationProvider() {
            @Override
            public boolean supports(ChannelType channel) {
                return channel == ChannelType.EMAIL;
            }
            
            @Override
            public NotificationResult send(Notification notification) {
                int attempt = attemptCounter.incrementAndGet();
                if (attempt < 3) {
                    throw new ProviderException("Provider", "Error");
                }
                return NotificationResult.success(notification.id(), "Provider", ChannelType.EMAIL);
            }
            
            @Override
            public String getName() {
                return "Provider";
            }
        };
        
        // No configuramos retry policy, debe usar la por defecto (3 intentos, 1s delay)
        var config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, provider)
            .build();
        
        var service = new NotificationServiceImpl(config);
        
        var recipient = new Recipient(
            "test@example.com",
            null,
            Map.of("subject", "Test")
        );
        var notification = Notification.create(recipient, "Test");
        
        // Act
        var result = service.send(notification, ChannelType.EMAIL);
        
        // Assert
        assertTrue(result.success());
        assertEquals(3, attemptCounter.get()); // Política por defecto tiene 3 intentos
    }
    
    @Test
    @DisplayName("Debe realizar exactamente 3 intentos cuando el provider falla 2 veces y tiene éxito en la tercera (con Mockito)")
    void shouldPerformExactlyThreeAttemptsWhenProviderFailsTwiceThenSucceeds() {
        // Arrange - Mock del provider usando Mockito
        var mockProvider = mock(NotificationProvider.class);
        when(mockProvider.supports(ChannelType.EMAIL)).thenReturn(true);
        when(mockProvider.getName()).thenReturn("MockProvider");
        
        // Configurar el comportamiento: falla 2 veces, éxito en la tercera
        when(mockProvider.send(any(Notification.class)))
            .thenThrow(new ProviderException("MockProvider", "Fallo intento 1"))
            .thenThrow(new ProviderException("MockProvider", "Fallo intento 2"))
            .thenAnswer(invocation -> {
                Notification notification = invocation.getArgument(0);
                return NotificationResult.success(notification.id(), "MockProvider", ChannelType.EMAIL);
            });
        
        var config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, mockProvider)
            .withRetryPolicy(RetryPolicy.of(3, 50)) // 3 intentos, 50ms entre reintentos
            .build();
        
        var service = new NotificationServiceImpl(config);
        
        var recipient = new Recipient(
            "test@example.com",
            null,
            Map.of()
        );
        var notification = Notification.create(recipient, "Test message");
        
        // Act
        var result = service.send(notification, ChannelType.EMAIL);
        
        // Assert
        assertAll(
            () -> assertTrue(result.success(), "Debe tener éxito en el tercer intento"),
            () -> assertEquals(notification.id(), result.notificationId()),
            () -> assertEquals("MockProvider", result.providerName())
        );
        
        // Verificar que se llamó exactamente 3 veces
        verify(mockProvider, times(3)).send(any(Notification.class));
        verify(mockProvider, atLeastOnce()).getName();
    }
}
