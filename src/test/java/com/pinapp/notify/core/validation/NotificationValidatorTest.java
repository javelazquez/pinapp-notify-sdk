package com.pinapp.notify.core.validation;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationPriority;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para NotificationValidator.
 * Verifica las validaciones de notificaciones según el canal.
 */
@DisplayName("NotificationValidator")
class NotificationValidatorTest {
    
    @Nested
    @DisplayName("Validación de Email")
    class EmailValidation {
        
        @Test
        @DisplayName("Debe validar email correcto")
        void shouldValidateValidEmail() {
            // Given
            Recipient recipient = new Recipient("usuario@dominio.com", null, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            assertDoesNotThrow(() -> 
                NotificationValidator.validate(notification, ChannelType.EMAIL)
            );
        }
        
        @Test
        @DisplayName("Debe fallar con email null")
        void shouldFailWithNullEmail() {
            // Given
            Recipient recipient = new Recipient(null, null, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                NotificationValidator.validate(notification, ChannelType.EMAIL)
            );
            
            assertTrue(exception.getMessage().contains("email válido"));
        }
        
        @Test
        @DisplayName("Debe fallar con email vacío")
        void shouldFailWithEmptyEmail() {
            // Given
            Recipient recipient = new Recipient("", null, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                NotificationValidator.validate(notification, ChannelType.EMAIL)
            );
            
            assertTrue(exception.getMessage().contains("email válido"));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "invalido",
            "@dominio.com",
            "usuario@",
            "usuario@dominio",
            "usuario dominio.com",
            "usuario@@dominio.com"
        })
        @DisplayName("Debe fallar con formatos de email inválidos")
        void shouldFailWithInvalidEmailFormats(String invalidEmail) {
            // Given
            Recipient recipient = new Recipient(invalidEmail, null, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                NotificationValidator.validate(notification, ChannelType.EMAIL)
            );
            
            assertTrue(exception.getMessage().contains("formato válido"));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "usuario@dominio.com",
            "nombre.apellido@empresa.com.mx",
            "test_user+tag@example.org",
            "user123@sub.dominio.com"
        })
        @DisplayName("Debe aceptar formatos de email válidos")
        void shouldAcceptValidEmailFormats(String validEmail) {
            // When & Then
            assertTrue(NotificationValidator.isValidEmail(validEmail));
        }
    }
    
    @Nested
    @DisplayName("Validación de SMS")
    class SmsValidation {
        
        @Test
        @DisplayName("Debe validar teléfono correcto")
        void shouldValidateValidPhone() {
            // Given
            Recipient recipient = new Recipient(null, "+5215512345678", Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            assertDoesNotThrow(() -> 
                NotificationValidator.validate(notification, ChannelType.SMS)
            );
        }
        
        @Test
        @DisplayName("Debe fallar con teléfono null")
        void shouldFailWithNullPhone() {
            // Given
            Recipient recipient = new Recipient(null, null, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                NotificationValidator.validate(notification, ChannelType.SMS)
            );
            
            assertTrue(exception.getMessage().contains("teléfono válido"));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "123",           // Muy corto
            "abc1234567",    // Contiene letras
            "+01234567",     // Empieza con 0
            "1234567"        // Muy corto (menos de 8 dígitos)
        })
        @DisplayName("Debe fallar con formatos de teléfono inválidos")
        void shouldFailWithInvalidPhoneFormats(String invalidPhone) {
            // Given
            Recipient recipient = new Recipient(null, invalidPhone, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                NotificationValidator.validate(notification, ChannelType.SMS)
            );
            
            assertTrue(exception.getMessage().contains("formato válido"));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "+5215512345678",
            "+14155552671",
            "+442071838750",
            "14155552671",
            "+34612345678"
        })
        @DisplayName("Debe aceptar formatos de teléfono válidos")
        void shouldAcceptValidPhoneFormats(String validPhone) {
            // When & Then
            assertTrue(NotificationValidator.isValidPhone(validPhone));
        }
        
        @Test
        @DisplayName("Debe normalizar teléfono con espacios y guiones")
        void shouldNormalizePhoneWithSpacesAndDashes() {
            // When & Then
            assertTrue(NotificationValidator.isValidPhone("+52 155 1234 5678"));
            assertTrue(NotificationValidator.isValidPhone("+1-415-555-2671"));
            assertTrue(NotificationValidator.isValidPhone("+44 (20) 7183-8750"));
        }
    }
    
    @Nested
    @DisplayName("Validación de PUSH")
    class PushValidation {
        
        @Test
        @DisplayName("Debe validar device token correcto")
        void shouldValidateValidDeviceToken() {
            // Given
            Map<String, String> metadata = Map.of("deviceToken", "abc123xyz");
            Recipient recipient = new Recipient(null, null, metadata);
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            assertDoesNotThrow(() -> 
                NotificationValidator.validate(notification, ChannelType.PUSH)
            );
        }
        
        @Test
        @DisplayName("Debe fallar con device token faltante")
        void shouldFailWithMissingDeviceToken() {
            // Given
            Recipient recipient = new Recipient(null, null, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                NotificationValidator.validate(notification, ChannelType.PUSH)
            );
            
            assertTrue(exception.getMessage().contains("device token"));
        }
    }
    
    @Nested
    @DisplayName("Validación de SLACK")
    class SlackValidation {
        
        @Test
        @DisplayName("Debe validar Slack channel ID correcto")
        void shouldValidateValidSlackChannelId() {
            // Given
            Map<String, String> metadata = Map.of("slackChannelId", "C01234567");
            Recipient recipient = new Recipient(null, null, metadata);
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            assertDoesNotThrow(() -> 
                NotificationValidator.validate(notification, ChannelType.SLACK)
            );
        }
        
        @Test
        @DisplayName("Debe fallar con Slack channel ID faltante")
        void shouldFailWithMissingSlackChannelId() {
            // Given
            Recipient recipient = new Recipient(null, null, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                NotificationValidator.validate(notification, ChannelType.SLACK)
            );
            
            assertTrue(exception.getMessage().contains("channel ID"));
        }
    }
    
    @Nested
    @DisplayName("Validación de Mensaje")
    class MessageValidation {
        
        @Test
        @DisplayName("Debe fallar con mensaje vacío")
        void shouldFailWithEmptyMessage() {
            // Given
            Recipient recipient = new Recipient("test@test.com", null, Map.of());
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                new Notification(
                    UUID.randomUUID(),
                    recipient,
                    "",
                    NotificationPriority.NORMAL,
                    Map.of()
                )
            );
        }
        
        @Test
        @DisplayName("Debe fallar con mensaje null")
        void shouldFailWithNullMessage() {
            // Given
            Recipient recipient = new Recipient("test@test.com", null, Map.of());
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                new Notification(
                    UUID.randomUUID(),
                    recipient,
                    null,
                    NotificationPriority.NORMAL,
                    Map.of()
                )
            );
        }
    }
    
    @Nested
    @DisplayName("Validación de Parámetros Null")
    class NullParameterValidation {
        
        @Test
        @DisplayName("Debe fallar con notificación null")
        void shouldFailWithNullNotification() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                NotificationValidator.validate(null, ChannelType.EMAIL)
            );
        }
        
        @Test
        @DisplayName("Debe fallar con channelType null")
        void shouldFailWithNullChannelType() {
            // Given
            Recipient recipient = new Recipient("test@test.com", null, Map.of());
            Notification notification = new Notification(
                UUID.randomUUID(),
                recipient,
                "Mensaje de prueba",
                NotificationPriority.NORMAL,
                Map.of()
            );
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                NotificationValidator.validate(notification, null)
            );
        }
    }
}
