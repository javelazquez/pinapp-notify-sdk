package com.pinapp.notify.core;

import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.templating.TemplateEngine;
import com.pinapp.notify.core.validation.NotificationValidator;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.RetryPolicy;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.NotificationException;
import com.pinapp.notify.exception.ProviderException;
import com.pinapp.notify.exception.ValidationException;
import com.pinapp.notify.ports.in.NotificationService;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final TemplateEngine templateEngine;
    
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
        this.templateEngine = new TemplateEngine();
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
        
        // Validación de la notificación usando NotificationValidator (Fail-Fast)
        NotificationValidator.validate(notification, channelType);
        
        // Procesar templates si hay variables
        Notification processedNotification = processTemplate(notification);
        
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
        
        // Ejecutar con reintentos
        return sendWithRetry(processedNotification, channelType, provider, config.getRetryPolicy());
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
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<NotificationResult> sendAsync(Notification notification, ChannelType channelType) {
        logger.debug("Iniciando envío asíncrono de notificación [id={}] por canal {}", 
            notification.id(), channelType);
        
        // Obtener o crear el ExecutorService
        ExecutorService executor = getOrCreateExecutor();
        
        // Ejecutar el envío de forma asíncrona
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(notification, channelType);
            } catch (Exception e) {
                logger.error("Error en envío asíncrono de notificación [id={}]: {}", 
                    notification.id(), e.getMessage(), e);
                throw e;
            }
        }, executor).exceptionally(error -> {
            logger.error("CompletableFuture completado excepcionalmente para notificación [id={}]: {}", 
                notification.id(), error.getMessage());
            
            // Si es una ValidationException o NotificationException, la propagamos
            if (error instanceof ValidationException || error instanceof NotificationException) {
                throw (RuntimeException) error;
            }
            
            // Para otros errores, creamos un resultado de fallo
            ChannelType channel = channelType;
            return NotificationResult.failure(
                notification.id(),
                "AsyncService",
                channel,
                error.getMessage()
            );
        });
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<NotificationResult> sendAsync(Notification notification) {
        logger.debug("Enviando notificación [id={}] de forma asíncrona usando canal por defecto", 
            notification.id());
        
        // Determinar el canal por defecto
        ChannelType defaultChannel = determineDefaultChannel(notification.recipient());
        
        logger.info("Canal por defecto seleccionado: {} para notificación asíncrona [id={}]", 
            defaultChannel, notification.id());
        
        return sendAsync(notification, defaultChannel);
    }
    
    /**
     * Envía una notificación con lógica de reintentos.
     * 
     * @param notification la notificación a enviar
     * @param channelType el canal a utilizar
     * @param provider el proveedor que realizará el envío
     * @param retryPolicy la política de reintentos a aplicar
     * @return el resultado del envío
     */
    private NotificationResult sendWithRetry(
            Notification notification,
            ChannelType channelType,
            NotificationProvider provider,
            RetryPolicy retryPolicy) {
        
        int maxAttempts = retryPolicy.maxAttempts();
        NotificationResult lastResult = null;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    long delay = retryPolicy.getDelayForAttempt(attempt);
                    logger.info("Reintento {}/{} para notificación [id={}] después de {}ms", 
                        attempt, maxAttempts, notification.id(), delay);
                    
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                } else {
                    logger.debug("Intento {}/{} para notificación [id={}]", 
                        attempt, maxAttempts, notification.id());
                }
                
                // Intentar el envío
                NotificationResult result = provider.send(notification);
                
                if (result.success()) {
                    if (attempt > 1) {
                        logger.info("Notificación [id={}] enviada exitosamente en el intento {}/{}", 
                            notification.id(), attempt, maxAttempts);
                    } else {
                        logger.info("Notificación [id={}] enviada exitosamente por '{}' vía {}", 
                            notification.id(), provider.getName(), channelType);
                    }
                    return result;
                } else {
                    logger.warn("Notificación [id={}] falló en intento {}/{}: {}", 
                        notification.id(), attempt, maxAttempts, result.errorMessage());
                    lastResult = result;
                    
                    // Si el resultado indica fallo pero no hubo excepción, no reintentamos
                    if (attempt == maxAttempts) {
                        logger.error("Notificación [id={}] falló después de {} intentos", 
                            notification.id(), maxAttempts);
                        return result;
                    }
                }
                
            } catch (ProviderException e) {
                logger.warn("Error del proveedor '{}' en intento {}/{} para notificación [id={}]: {}", 
                    provider.getName(), attempt, maxAttempts, notification.id(), e.getMessage());
                lastException = e;
                
                if (attempt == maxAttempts) {
                    logger.error("Notificación [id={}] falló después de {} intentos: {}", 
                        notification.id(), maxAttempts, e.getMessage());
                    
                    return NotificationResult.failure(
                        notification.id(),
                        provider.getName(),
                        channelType,
                        String.format("Falló después de %d intentos: %s", maxAttempts, e.getMessage())
                    );
                }
                
            } catch (InterruptedException e) {
                logger.error("Envío interrumpido para notificación [id={}]", notification.id());
                Thread.currentThread().interrupt();
                
                return NotificationResult.failure(
                    notification.id(),
                    provider.getName(),
                    channelType,
                    "Envío interrumpido: " + e.getMessage()
                );
                
            } catch (Exception e) {
                logger.error("Error inesperado en intento {}/{} para notificación [id={}]: {}", 
                    attempt, maxAttempts, notification.id(), e.getMessage(), e);
                
                throw new NotificationException(
                    String.format("Error inesperado al enviar la notificación: %s", e.getMessage()),
                    e
                );
            }
        }
        
        // Si llegamos aquí, todos los intentos fallaron
        if (lastResult != null) {
            return lastResult;
        }
        
        if (lastException != null) {
            return NotificationResult.failure(
                notification.id(),
                provider.getName(),
                channelType,
                String.format("Falló después de %d intentos: %s", maxAttempts, lastException.getMessage())
            );
        }
        
        // Caso inesperado
        return NotificationResult.failure(
            notification.id(),
            provider.getName(),
            channelType,
            String.format("Falló después de %d intentos por razones desconocidas", maxAttempts)
        );
    }
    
    /**
     * Obtiene el ExecutorService configurado o crea uno por defecto.
     * 
     * @return el ExecutorService a utilizar para operaciones asíncronas
     */
    private ExecutorService getOrCreateExecutor() {
        ExecutorService executor = config.getExecutorService();
        
        if (executor != null) {
            return executor;
        }
        
        // Si no hay executor configurado, usar el ForkJoinPool común
        // Nota: Esto no es ideal para producción, se debería configurar uno dedicado
        logger.warn("No se configuró un ExecutorService dedicado. " +
            "Se recomienda usar PinappNotifyConfig.builder().enableAsync() o .withExecutorService()");
        
        // Retornamos el pool común de ForkJoin
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "pinapp-notify-default-async-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Procesa el template del mensaje si la notificación contiene variables.
     * 
     * <p>Si la notificación no tiene variables, retorna la notificación original sin modificar.</p>
     * 
     * @param notification la notificación a procesar
     * @return una nueva notificación con el mensaje procesado, o la original si no hay variables
     */
    private Notification processTemplate(Notification notification) {
        // Si no hay variables de template, retornar sin modificar
        if (!notification.hasTemplateVariables()) {
            logger.debug("Notificación [id={}] no tiene variables de template, se enviará sin procesamiento", 
                notification.id());
            return notification;
        }
        
        logger.debug("Procesando template para notificación [id={}] con {} variable(s)", 
            notification.id(), notification.templateVariables().size());
        
        // Procesar el mensaje usando el TemplateEngine
        String originalMessage = notification.message();
        String processedMessage = templateEngine.process(originalMessage, notification.templateVariables());
        
        logger.info("Template procesado para notificación [id={}]: '{}' -> '{}'", 
            notification.id(), originalMessage, processedMessage);
        
        // Crear una nueva notificación con el mensaje procesado
        // Mantenemos las variables originales por si se necesitan en el futuro
        return new Notification(
            notification.id(),
            notification.recipient(),
            processedMessage,
            notification.priority(),
            notification.templateVariables()
        );
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
