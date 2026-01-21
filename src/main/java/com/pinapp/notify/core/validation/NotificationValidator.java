package com.pinapp.notify.core.validation;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Validador de notificaciones que verifica la integridad de los datos antes del envío.
 * 
 * <p>Este validador implementa el principio de "Fail-Fast", deteniendo el proceso
 * en el primer error encontrado para proporcionar feedback inmediato.</p>
 * 
 * <p>Características principales:</p>
 * <ul>
 *   <li>Validación de formato de email usando expresiones regulares</li>
 *   <li>Validación de formato de teléfono internacional</li>
 *   <li>Validación de contenido del mensaje</li>
 *   <li>Validaciones específicas por tipo de canal</li>
 * </ul>
 * 
 * @author PinApp Team
 */
public final class NotificationValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationValidator.class);
    
    /**
     * Pattern RFC 5322 simplificado para validación de email.
     * Acepta la mayoría de formatos válidos de email.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    /**
     * Pattern para validación de teléfono internacional.
     * Acepta formatos: +52XXXXXXXXXX, +1XXXXXXXXXX, etc.
     * Mínimo 8 dígitos, máximo 15 (según E.164)
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{7,14}$"
    );
    
    /**
     * Longitud mínima requerida para el mensaje.
     */
    private static final int MIN_MESSAGE_LENGTH = 1;
    
    /**
     * Constructor privado para prevenir instanciación.
     * Esta clase solo contiene métodos estáticos.
     */
    private NotificationValidator() {
        throw new AssertionError("NotificationValidator no debe ser instanciada");
    }
    
    /**
     * Valida una notificación completa antes del envío.
     * 
     * <p>Este método coordina todas las validaciones necesarias según el canal.</p>
     * 
     * @param notification la notificación a validar
     * @param channelType el canal por el que se enviará
     * @throws ValidationException si alguna validación falla
     * @throws IllegalArgumentException si los parámetros son null
     */
    public static void validate(Notification notification, ChannelType channelType) {
        if (notification == null) {
            throw new IllegalArgumentException("La notificación no puede ser null");
        }
        
        if (channelType == null) {
            throw new IllegalArgumentException("El tipo de canal no puede ser null");
        }
        
        logger.debug("Validando notificación [id={}] para canal {}", 
            notification.id(), channelType);
        
        // Validaciones básicas
        validateMessage(notification.message());
        validateRecipient(notification.recipient());
        
        // Validaciones específicas por canal usando Pattern Matching (Java 21)
        validateChannelSpecific(notification.recipient(), channelType);
        
        logger.debug("Notificación [id={}] validada exitosamente", notification.id());
    }
    
    /**
     * Valida el contenido del mensaje.
     * 
     * @param message el mensaje a validar
     * @throws ValidationException si el mensaje es inválido
     */
    private static void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            String errorMsg = "El mensaje no puede ser null o vacío";
            logger.error("Validación fallida: {}", errorMsg);
            throw new ValidationException(errorMsg);
        }
        
        if (message.trim().length() < MIN_MESSAGE_LENGTH) {
            String errorMsg = String.format(
                "El mensaje debe tener al menos %d carácter(es). Recibido: '%s'",
                MIN_MESSAGE_LENGTH,
                message.trim()
            );
            logger.error("Validación fallida: {}", errorMsg);
            throw new ValidationException(errorMsg);
        }
    }
    
    /**
     * Valida que el destinatario no sea null.
     * 
     * @param recipient el destinatario a validar
     * @throws ValidationException si el destinatario es null
     */
    private static void validateRecipient(Recipient recipient) {
        if (recipient == null) {
            String errorMsg = "El destinatario no puede ser null";
            logger.error("Validación fallida: {}", errorMsg);
            throw new ValidationException(errorMsg);
        }
    }
    
    /**
     * Valida el destinatario según el canal específico usando Pattern Matching.
     * 
     * <p>Este método usa las características de Java 21 para un switch mejorado.</p>
     * 
     * @param recipient el destinatario a validar
     * @param channelType el tipo de canal
     * @throws ValidationException si el destinatario no cumple con los requisitos del canal
     */
    private static void validateChannelSpecific(Recipient recipient, ChannelType channelType) {
        switch (channelType) {
            case EMAIL -> validateEmailChannel(recipient);
            case SMS -> validateSmsChannel(recipient);
            case PUSH -> validatePushChannel(recipient);
            case SLACK -> validateSlackChannel(recipient);
        }
    }
    
    /**
     * Valida el destinatario para el canal EMAIL.
     * 
     * @param recipient el destinatario
     * @throws ValidationException si el email es inválido
     */
    private static void validateEmailChannel(Recipient recipient) {
        String email = recipient.email();
        
        if (email == null || email.isBlank()) {
            String errorMsg = "Para envío por EMAIL, el destinatario debe tener un email válido";
            logger.error("Validación EMAIL fallida: email es null o vacío");
            throw new ValidationException(errorMsg);
        }
        
        if (!isValidEmail(email)) {
            String errorMsg = String.format(
                "El email '%s' no tiene un formato válido. " +
                "Debe ser una dirección de correo electrónico RFC 5322 válida (ej: usuario@dominio.com)",
                email
            );
            logger.error("Validación EMAIL fallida: formato inválido para '{}'", email);
            throw new ValidationException(errorMsg);
        }
        
        logger.debug("Email '{}' validado exitosamente", email);
    }
    
    /**
     * Valida el destinatario para el canal SMS.
     * 
     * @param recipient el destinatario
     * @throws ValidationException si el teléfono es inválido
     */
    private static void validateSmsChannel(Recipient recipient) {
        String phone = recipient.phone();
        
        if (phone == null || phone.isBlank()) {
            String errorMsg = "Para envío por SMS, el destinatario debe tener un número de teléfono válido";
            logger.error("Validación SMS fallida: teléfono es null o vacío");
            throw new ValidationException(errorMsg);
        }
        
        if (!isValidPhone(phone)) {
            String errorMsg = String.format(
                "El teléfono '%s' no tiene un formato válido. " +
                "Debe ser un número internacional E.164 (ej: +5215512345678, mínimo 8 dígitos, máximo 15)",
                phone
            );
            logger.error("Validación SMS fallida: formato inválido para '{}'", phone);
            throw new ValidationException(errorMsg);
        }
        
        logger.debug("Teléfono '{}' validado exitosamente", phone);
    }
    
    /**
     * Valida el destinatario para el canal PUSH.
     * 
     * @param recipient el destinatario
     * @throws ValidationException si no hay device token válido
     */
    private static void validatePushChannel(Recipient recipient) {
        String deviceToken = recipient.metadata().get("deviceToken");
        
        if (deviceToken == null || deviceToken.isBlank()) {
            String errorMsg = "Para envío por PUSH, el destinatario debe tener un device token válido en metadata";
            logger.error("Validación PUSH fallida: deviceToken no encontrado o vacío");
            throw new ValidationException(errorMsg);
        }
        
        logger.debug("Device token validado exitosamente");
    }
    
    /**
     * Valida el destinatario para el canal SLACK.
     * 
     * @param recipient el destinatario
     * @throws ValidationException si no hay channel ID de Slack válido
     */
    private static void validateSlackChannel(Recipient recipient) {
        String slackChannelId = recipient.metadata().get("slackChannelId");
        
        if (slackChannelId == null || slackChannelId.isBlank()) {
            String errorMsg = "Para envío por SLACK, el destinatario debe tener un channel ID de Slack válido en metadata";
            logger.error("Validación SLACK fallida: slackChannelId no encontrado o vacío");
            throw new ValidationException(errorMsg);
        }
        
        logger.debug("Slack channel ID validado exitosamente");
    }
    
    /**
     * Valida el formato de un email usando expresiones regulares.
     * 
     * @param email el email a validar
     * @return true si el email es válido, false en caso contrario
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        
        // Trim y validación de longitud razonable
        String trimmedEmail = email.trim();
        if (trimmedEmail.length() > 254) { // RFC 5321
            return false;
        }
        
        return EMAIL_PATTERN.matcher(trimmedEmail).matches();
    }
    
    /**
     * Valida el formato de un número de teléfono.
     * 
     * <p>Acepta números en formato internacional E.164:</p>
     * <ul>
     *   <li>Puede empezar con + (opcional)</li>
     *   <li>Primer dígito del 1-9</li>
     *   <li>Total de 8 a 15 dígitos</li>
     * </ul>
     * 
     * @param phone el número de teléfono a validar
     * @return true si el teléfono es válido, false en caso contrario
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        
        // Remover espacios, guiones y paréntesis comunes
        String normalizedPhone = phone.trim()
            .replaceAll("[\\s\\-\\(\\)]", "");
        
        return PHONE_PATTERN.matcher(normalizedPhone).matches();
    }
}
