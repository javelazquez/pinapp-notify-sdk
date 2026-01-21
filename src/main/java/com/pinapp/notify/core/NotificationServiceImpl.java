package com.pinapp.notify.core;

import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.NotificationException;
import com.pinapp.notify.exception.ProviderException;
import com.pinapp.notify.exception.ValidationException;
import com.pinapp.notify.ports.in.NotificationService;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Implementación del servicio de notificaciones (Orquestador Core).
 * 
 * <p>Esta clase es responsable de orquestar el envío de notificaciones,
 * seleccionando el proveedor adecuado basándose en el tipo de canal
 * y manejando los errores de forma apropiada.</p>
 * 
 * <p>Características principales:</p>
 * <ul>
 *   <li>Selección automática de proveedores basada en el canal</li>
 *   <li>Validación de notificaciones antes del envío</li>
 *   <li>Manejo robusto de errores con mensajes claros</li>
 *   <li>Logging detallado para trazabilidad</li>
 * </ul>
 * 
 * @author PinApp Team
 */
public class NotificationServiceImpl implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    
    private final PinappNotifyConfig config;
    
    /**
     * Constructor que recibe la configuración del SDK.
     * 
     * @param config la configuración con los proveedores registrados
     * @throws IllegalArgumentException si config es null
     */
    public NotificationServiceImpl(PinappNotifyConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("La configuración no puede ser null");
        }
        this.config = config;
        logger.info("NotificationServiceImpl inicializado con {} proveedor(es) configurado(s)", 
            config.getProviders().size());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationResult send(Notification notification, ChannelType channelType) {
        logger.debug("Iniciando envío de notificación [id={}] por canal {}", 
            notification.id(), channelType);
        
        // Validación de la notificación
        validateNotification(notification, channelType);
        
        // Buscar el proveedor adecuado
        NotificationProvider provider = findProvider(channelType)
            .orElseThrow(() -> {
                String errorMsg = String.format(
                    "No hay proveedor configurado para el canal %s. " +
                    "Por favor, configure un proveedor usando PinappNotifyConfig.builder().addProvider(...)",
                    channelType
                );
                logger.error("Error de configuración: {}", errorMsg);
                return new NotificationException(errorMsg);
            });
        
        logger.info("Proveedor seleccionado: '{}' para canal {}", provider.getName(), channelType);
        
        try {
            // Delegar el envío al proveedor
            NotificationResult result = provider.send(notification);
            
            if (result.success()) {
                logger.info("Notificación [id={}] enviada exitosamente por '{}' vía {}", 
                    notification.id(), provider.getName(), channelType);
            } else {
                logger.warn("Notificación [id={}] falló: {}", 
                    notification.id(), result.errorMessage());
            }
            
            return result;
            
        } catch (ProviderException e) {
            logger.error("Error del proveedor '{}' al enviar notificación [id={}]: {}", 
                provider.getName(), notification.id(), e.getMessage(), e);
            
            // Creamos un resultado de fallo con los detalles del error
            return NotificationResult.failure(
                notification.id(),
                provider.getName(),
                channelType,
                e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Error inesperado al enviar notificación [id={}]: {}", 
                notification.id(), e.getMessage(), e);
            
            throw new NotificationException(
                String.format("Error inesperado al enviar la notificación: %s", e.getMessage()),
                e
            );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationResult send(Notification notification) {
        logger.debug("Enviando notificación [id={}] usando canal por defecto", notification.id());
        
        // Determinar el canal por defecto basado en el destinatario
        ChannelType defaultChannel = determineDefaultChannel(notification.recipient());
        
        logger.info("Canal por defecto seleccionado: {} para notificación [id={}]", 
            defaultChannel, notification.id());
        
        return send(notification, defaultChannel);
    }
    
    /**
     * Valida que la notificación tenga todos los datos requeridos.
     * 
     * @param notification la notificación a validar
     * @param channelType el canal por el que se enviará
     * @throws ValidationException si la notificación es inválida
     */
    private void validateNotification(Notification notification, ChannelType channelType) {
        if (notification == null) {
            throw new ValidationException("La notificación no puede ser null");
        }
        
        if (channelType == null) {
            throw new ValidationException("El tipo de canal no puede ser null");
        }
        
        // Validaciones específicas por canal
        Recipient recipient = notification.recipient();
        switch (channelType) {
            case EMAIL -> {
                if (recipient.email() == null || recipient.email().isBlank()) {
                    throw new ValidationException(
                        "El destinatario debe tener un email válido para envío por EMAIL"
                    );
                }
            }
            case SMS -> {
                if (recipient.phone() == null || recipient.phone().isBlank()) {
                    throw new ValidationException(
                        "El destinatario debe tener un número de teléfono válido para envío por SMS"
                    );
                }
            }
            case PUSH -> {
                String deviceToken = recipient.metadata().get("deviceToken");
                if (deviceToken == null || deviceToken.isBlank()) {
                    throw new ValidationException(
                        "El destinatario debe tener un device token válido en metadata para envío por PUSH"
                    );
                }
            }
            case SLACK -> {
                String slackChannelId = recipient.metadata().get("slackChannelId");
                if (slackChannelId == null || slackChannelId.isBlank()) {
                    throw new ValidationException(
                        "El destinatario debe tener un channel ID de Slack válido en metadata para envío por SLACK"
                    );
                }
            }
        }
        
        logger.debug("Notificación [id={}] validada exitosamente para canal {}", 
            notification.id(), channelType);
    }
    
    /**
     * Busca el proveedor configurado para un canal específico.
     * 
     * @param channelType el tipo de canal
     * @return un Optional con el proveedor si existe
     */
    private Optional<NotificationProvider> findProvider(ChannelType channelType) {
        return config.getProvider(channelType);
    }
    
    /**
     * Determina el canal por defecto basándose en la información del destinatario.
     * 
     * <p>El orden de preferencia es: EMAIL > SMS > PUSH > SLACK</p>
     * 
     * @param recipient el destinatario
     * @return el canal por defecto a utilizar
     * @throws NotificationException si no se puede determinar un canal válido
     */
    private ChannelType determineDefaultChannel(Recipient recipient) {
        // Orden de preferencia
        if (recipient.email() != null && !recipient.email().isBlank() 
                && config.hasProvider(ChannelType.EMAIL)) {
            return ChannelType.EMAIL;
        }
        
        if (recipient.phone() != null && !recipient.phone().isBlank() 
                && config.hasProvider(ChannelType.SMS)) {
            return ChannelType.SMS;
        }
        
        String deviceToken = recipient.metadata().get("deviceToken");
        if (deviceToken != null && !deviceToken.isBlank() 
                && config.hasProvider(ChannelType.PUSH)) {
            return ChannelType.PUSH;
        }
        
        String slackChannelId = recipient.metadata().get("slackChannelId");
        if (slackChannelId != null && !slackChannelId.isBlank() 
                && config.hasProvider(ChannelType.SLACK)) {
            return ChannelType.SLACK;
        }
        
        throw new NotificationException(
            "No se pudo determinar un canal por defecto. " +
            "El destinatario no tiene información válida para ningún canal configurado"
        );
    }
}
