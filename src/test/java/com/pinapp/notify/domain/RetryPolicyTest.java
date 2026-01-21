package com.pinapp.notify.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para RetryPolicy.
 * 
 * @author PinApp Team
 */
@DisplayName("RetryPolicy Tests")
class RetryPolicyTest {
    
    @Test
    @DisplayName("Debe crear una política con parámetros válidos")
    void shouldCreatePolicyWithValidParameters() {
        // Act
        RetryPolicy policy = RetryPolicy.of(3, 1000);
        
        // Assert
        assertEquals(3, policy.maxAttempts());
        assertEquals(1000, policy.delayMillis());
    }
    
    @Test
    @DisplayName("Debe lanzar excepción si maxAttempts < 1")
    void shouldThrowExceptionWhenMaxAttemptsLessThanOne() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            RetryPolicy.of(0, 1000)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            RetryPolicy.of(-1, 1000)
        );
    }
    
    @Test
    @DisplayName("Debe lanzar excepción si delayMillis < 0")
    void shouldThrowExceptionWhenDelayMillisNegative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            RetryPolicy.of(3, -100)
        );
    }
    
    @Test
    @DisplayName("Debe permitir delayMillis = 0")
    void shouldAllowZeroDelay() {
        // Act
        RetryPolicy policy = RetryPolicy.of(3, 0);
        
        // Assert
        assertEquals(0, policy.delayMillis());
    }
    
    @Test
    @DisplayName("Debe crear política sin reintentos")
    void shouldCreateNoRetryPolicy() {
        // Act
        RetryPolicy policy = RetryPolicy.noRetry();
        
        // Assert
        assertEquals(1, policy.maxAttempts());
        assertEquals(0, policy.delayMillis());
        assertFalse(policy.shouldRetry());
        assertEquals(0, policy.getRetryCount());
    }
    
    @Test
    @DisplayName("Debe crear política por defecto")
    void shouldCreateDefaultPolicy() {
        // Act
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        
        // Assert
        assertEquals(3, policy.maxAttempts());
        assertEquals(1000, policy.delayMillis());
        assertTrue(policy.shouldRetry());
        assertEquals(2, policy.getRetryCount());
    }
    
    @Test
    @DisplayName("shouldRetry debe retornar true si maxAttempts > 1")
    void shouldRetryWhenMaxAttemptsGreaterThanOne() {
        // Arrange
        RetryPolicy policy1 = RetryPolicy.of(2, 500);
        RetryPolicy policy2 = RetryPolicy.of(5, 1000);
        
        // Assert
        assertTrue(policy1.shouldRetry());
        assertTrue(policy2.shouldRetry());
    }
    
    @Test
    @DisplayName("shouldRetry debe retornar false si maxAttempts = 1")
    void shouldNotRetryWhenMaxAttemptsIsOne() {
        // Arrange
        RetryPolicy policy = RetryPolicy.of(1, 1000);
        
        // Assert
        assertFalse(policy.shouldRetry());
    }
    
    @Test
    @DisplayName("getRetryCount debe retornar maxAttempts - 1")
    void shouldReturnCorrectRetryCount() {
        // Arrange & Act & Assert
        assertEquals(0, RetryPolicy.of(1, 0).getRetryCount());
        assertEquals(1, RetryPolicy.of(2, 500).getRetryCount());
        assertEquals(2, RetryPolicy.of(3, 1000).getRetryCount());
        assertEquals(4, RetryPolicy.of(5, 2000).getRetryCount());
    }
    
    @Test
    @DisplayName("getDelayForAttempt debe retornar 0 para el primer intento")
    void shouldReturnZeroDelayForFirstAttempt() {
        // Arrange
        RetryPolicy policy = RetryPolicy.of(3, 1000);
        
        // Act & Assert
        assertEquals(0, policy.getDelayForAttempt(1));
    }
    
    @Test
    @DisplayName("getDelayForAttempt debe retornar delayMillis para intentos posteriores")
    void shouldReturnDelayForSubsequentAttempts() {
        // Arrange
        RetryPolicy policy = RetryPolicy.of(5, 1500);
        
        // Act & Assert
        assertEquals(1500, policy.getDelayForAttempt(2));
        assertEquals(1500, policy.getDelayForAttempt(3));
        assertEquals(1500, policy.getDelayForAttempt(4));
        assertEquals(1500, policy.getDelayForAttempt(5));
    }
    
    @Test
    @DisplayName("Debe funcionar con valores grandes de maxAttempts")
    void shouldHandleLargeMaxAttempts() {
        // Act
        RetryPolicy policy = RetryPolicy.of(100, 500);
        
        // Assert
        assertEquals(100, policy.maxAttempts());
        assertEquals(99, policy.getRetryCount());
        assertTrue(policy.shouldRetry());
    }
    
    @Test
    @DisplayName("Debe funcionar con valores grandes de delayMillis")
    void shouldHandleLargeDelay() {
        // Act
        RetryPolicy policy = RetryPolicy.of(3, 60000); // 1 minuto
        
        // Assert
        assertEquals(60000, policy.delayMillis());
        assertEquals(60000, policy.getDelayForAttempt(2));
    }
}
