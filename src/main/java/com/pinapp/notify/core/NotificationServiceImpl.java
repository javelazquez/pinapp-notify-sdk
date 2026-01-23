package com.pinapp.notify.core;

import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.events.NotificationEventPublisher;
import com.pinapp.notify.core.events.NotificationFailedEvent;
import com.pinapp.notify.core.events.NotificationRetryEvent;
import com.pinapp.notify.core.events.NotificationSentEvent;
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
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del servicio de notificaciones (Orquestador Core).
 * 
 * <p>
 * Esta clase es responsable de orquestar el envío de notificaciones,
 * seleccionando el proveedor adecuado basándose en el tipo de canal
 * y manejando los errores de forma apropiada.
 * </p>
 * 
 * <p>
 * Características principales:
 * </p>
 * <ul>
 * <li>Selección automática de proveedores basada en el canal</li>
 * <li>Validación de notificaciones antes del envío</li>
 * <li>Manejo robusto de errores con mensajes claros</li>
 * <li>Logging detallado para trazabilidad</li>
 * </ul>
 * 
 * @author PinApp Team
 */
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final PinappNotifyConfig config;
    private final TemplateEngine templateEngine;
    private final NotificationEventPublisher eventPublisher;

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
        this.eventPublisher = config.getEventPublisher();
        log.info("NotificationServiceImpl inicializado con {} proveedor(es) configurado(s) y {} suscriptor(es)",
                config.getProviders().size(), eventPublisher.getSubscriberCount());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationResult send(Notification notification, ChannelType channelType) {
        log.debug("Iniciando envío de notificación [id={}] por canal {}",
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
                            channelType);
                    log.error("Error de configuración: {}", errorMsg);
                    return new NotificationException(errorMsg);
                });

        log.info("Proveedor seleccionado: '{}' para canal {}", provider.getName(), channelType);

        // Ejecutar con reintentos
        return sendWithRetry(processedNotification, channelType, provider, config.getRetryPolicy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationResult send(Notification notification) {
        log.debug("Enviando notificación [id={}] usando canal por defecto", notification.id());

        // Determinar el canal por defecto basado en el destinatario
        ChannelType defaultChannel = determineDefaultChannel(notification.recipient());

        log.info("Canal por defecto seleccionado: {} para notificación [id={}]",
                defaultChannel, notification.id());

        return send(notification, defaultChannel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<NotificationResult> sendAsync(Notification notification, ChannelType channelType) {
        log.debug("Iniciando envío asíncrono de notificación [id={}] por canal {}",
                notification.id(), channelType);

        // Obtener o crear el ExecutorService
        ExecutorService executor = getOrCreateExecutor();

        // Ejecutar el envío de forma asíncrona
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Validar (Fail-Fast)
                NotificationValidator.validate(notification, channelType);

                // 2. Procesar template
                Notification processed = processTemplate(notification);

                // 3. Buscar proveedor
                NotificationProvider provider = findProvider(channelType)
                        .orElseThrow(() -> new NotificationException(
                                "No hay proveedor configurado para el canal " + channelType));

                return new AsyncContext(processed, provider);
            } catch (Exception e) {
                log.error("Error en preparación envío asíncrono [id={}]: {}",
                        notification.id(), e.getMessage());
                throw e;
            }
        }, executor).thenCompose(ctx -> sendWithRetryAsync(ctx.notification, channelType, ctx.provider,
                config.getRetryPolicy(), 1, executor)).exceptionally(error -> {
                    Throwable cause = error instanceof java.util.concurrent.CompletionException ? error.getCause()
                            : error;

                    log.error("Error final en envío asíncrono para notificación [id={}]: {}",
                            notification.id(), cause.getMessage());

                    if (cause instanceof ValidationException || cause instanceof NotificationException) {
                        throw (RuntimeException) cause;
                    }

                    // Publicar evento de fallo antes de retornar
                    publishFailedEvent(
                            notification.id().toString(),
                            "AsyncService",
                            channelType,
                            cause.getMessage(),
                            1); // totalAttempts = 1 porque no llegó a intentar enviar

                    return NotificationResult.failure(
                            notification.id(),
                            "AsyncService",
                            channelType,
                            cause.getMessage());
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<NotificationResult> sendAsync(Notification notification) {
        log.debug("Enviando notificación [id={}] de forma asíncrona usando canal por defecto",
                notification.id());

        return CompletableFuture
                .supplyAsync(() -> determineDefaultChannel(notification.recipient()), getOrCreateExecutor())
                .thenCompose(channel -> {
                    log.info("Canal por defecto seleccionado: {} para notificación asíncrona [id={}]",
                            channel, notification.id());
                    return sendAsync(notification, channel);
                });
    }

    // Record auxiliar para pasar datos entre etapas del CompletableFuture
    private record AsyncContext(Notification notification, NotificationProvider provider) {
    }

    /**
     * Envía una notificación con lógica de reintentos.
     * 
     * @param notification la notificación a enviar
     * @param channelType  el canal a utilizar
     * @param provider     el proveedor que realizará el envío
     * @param retryPolicy  la política de reintentos a aplicar
     * @return el resultado del envío
     */
    /**
     * Envía una notificación de forma asíncrona con lógica de reintentos no
     * bloqueante.
     */
    private CompletableFuture<NotificationResult> sendWithRetryAsync(
            Notification notification,
            ChannelType channelType,
            NotificationProvider provider,
            RetryPolicy retryPolicy,
            int attempt,
            ExecutorService executor) {

        int maxAttempts = retryPolicy.maxAttempts();

        if (attempt > 1) {
            log.info("Reintento {}/{} (Async) para notificación [id={}]",
                    attempt, maxAttempts, notification.id());
        } else {
            log.debug("Intento {}/{} (Async) para notificación [id={}]",
                    attempt, maxAttempts, notification.id());
        }

        return CompletableFuture.supplyAsync(() -> provider.send(notification), executor)
                .handle((result, ex) -> {
                    if (ex == null && result != null && result.success()) {
                        // ÉXITO
                        if (attempt > 1) {
                            log.info("Notificación [id={}] enviada exitosamente en intento {}/{}",
                                    notification.id(), attempt, maxAttempts);
                        } else {
                            log.info("Notificación [id={}] enviada exitosamente por '{}' vía {}",
                                    notification.id(), provider.getName(), channelType);
                        }
                        publishSentEvent(notification.id().toString(), provider.getName(), channelType, attempt);
                        return CompletableFuture.completedFuture(result);
                    }

                    // FALLO - Determinar causa
                    String errorMessage = ex != null ? ex.getMessage()
                            : (result != null ? result.errorMessage() : "El proveedor retornó un resultado nulo");
                    log.warn("Notificación [id={}] falló en intento {}/{}: {}",
                            notification.id(), attempt, maxAttempts, errorMessage);

                    // Verificar si debemos rendirnos
                    if (attempt >= maxAttempts) {
                        log.error("Notificación [id={}] falló definitivamente tras {} intentos",
                                notification.id(), maxAttempts);

                        NotificationResult finalResult = result != null ? result
                                : NotificationResult.failure(
                                        notification.id(), provider.getName(), channelType,
                                        "Error asíncrono: " + errorMessage);

                        publishFailedEvent(notification.id().toString(), provider.getName(), channelType, errorMessage,
                                attempt);
                        return CompletableFuture.completedFuture(finalResult);
                    }

                    // REINTENTAR
                    long delay = retryPolicy.getDelayForAttempt(attempt + 1);
                    publishRetryEvent(notification.id().toString(), provider.getName(), channelType,
                            attempt + 1, maxAttempts, delay, errorMessage);

                    log.info("Programando reintento {}/{} para notificación [id={}] en {}ms",
                            attempt + 1, maxAttempts, notification.id(), delay);

                    // Programar el siguiente intento sin bloquear el thread
                    return CompletableFuture.runAsync(() -> {
                    },
                            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, executor))
                            .thenCompose(v -> sendWithRetryAsync(notification, channelType, provider, retryPolicy,
                                    attempt + 1, executor));
                })
                .thenCompose(future -> future); // Aplanar el CompletableFuture<CompletableFuture<T>>
    }

    /**
     * Envía una notificación con lógica de reintentos (Síncrono/Bloqueante).
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

                    String retryReason = lastException != null
                            ? lastException.getMessage()
                            : (lastResult != null ? lastResult.errorMessage() : "Error desconocido");

                    publishRetryEvent(
                            notification.id().toString(),
                            provider.getName(),
                            channelType,
                            attempt,
                            maxAttempts,
                            delay,
                            retryReason);

                    log.info("Reintento {}/{} para notificación [id={}] después de {}ms",
                            attempt, maxAttempts, notification.id(), delay);

                    if (delay > 0) {
                        Thread.sleep(delay); // Bloqueante solo en flujo síncrono
                    }
                } else {
                    log.debug("Intento {}/{} para notificación [id={}]",
                            attempt, maxAttempts, notification.id());
                }

                NotificationResult result = provider.send(notification);

                if (result != null && result.success()) {
                    if (attempt > 1) {
                        log.info("Notificación [id={}] enviada exitosamente en el intento {}/{}",
                                notification.id(), attempt, maxAttempts);
                    } else {
                        log.info("Notificación [id={}] enviada exitosamente por '{}' vía {}",
                                notification.id(), provider.getName(), channelType);
                    }

                    publishSentEvent(notification.id().toString(), provider.getName(), channelType, attempt);
                    return result;
                } else {
                    String errorMsg = (result != null) ? result.errorMessage()
                            : "El proveedor retornó un resultado nulo";
                    log.warn("Notificación [id={}] falló en intento {}/{}: {}",
                            notification.id(), attempt, maxAttempts, errorMsg);
                    lastResult = result;

                    if (attempt == maxAttempts) {
                        log.error("Notificación [id={}] falló después de {} intentos",
                                notification.id(), maxAttempts);

                        publishFailedEvent(
                                notification.id().toString(),
                                provider.getName(),
                                channelType,
                                errorMsg,
                                attempt);
                        return result;
                    }
                }

            } catch (ProviderException e) {
                log.warn("Error del proveedor '{}' en intento {}/{} para notificación [id={}]: {}",
                        provider.getName(), attempt, maxAttempts, notification.id(), e.getMessage());
                lastException = e;

                if (attempt == maxAttempts) {
                    log.error("Notificación [id={}] falló después de {} intentos: {}",
                            notification.id(), maxAttempts, e.getMessage());

                    NotificationResult failureResult = NotificationResult.failure(
                            notification.id(),
                            provider.getName(),
                            channelType,
                            String.format("Falló después de %d intentos: %s", maxAttempts, e.getMessage()));

                    publishFailedEvent(
                            notification.id().toString(),
                            provider.getName(),
                            channelType,
                            e.getMessage(),
                            attempt);
                    return failureResult;
                }

            } catch (InterruptedException e) {
                log.error("Envío interrumpido para notificación [id={}]", notification.id());
                Thread.currentThread().interrupt();

                NotificationResult interruptedResult = NotificationResult.failure(
                        notification.id(),
                        provider.getName(),
                        channelType,
                        "Envío interrumpido: " + e.getMessage());

                publishFailedEvent(
                        notification.id().toString(),
                        provider.getName(),
                        channelType,
                        "Envío interrumpido: " + e.getMessage(),
                        attempt);
                return interruptedResult;

            } catch (Exception e) {
                log.error("Error inesperado en intento {}/{} para notificación [id={}]: {}",
                        attempt, maxAttempts, notification.id(), e.getMessage(), e);

                publishFailedEvent(
                        notification.id().toString(),
                        provider.getName(),
                        channelType,
                        "Error inesperado: " + e.getMessage(),
                        attempt);

                throw new NotificationException(
                        String.format("Error inesperado al enviar la notificación: %s", e.getMessage()),
                        e);
            }
        }

        if (lastResult != null)
            return lastResult;

        String failureMsg = lastException != null ? lastException.getMessage() : "razones desconocidas";
        return NotificationResult.failure(
                notification.id(),
                provider.getName(),
                channelType,
                String.format("Falló después de %d intentos: %s", maxAttempts, failureMsg));
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
        log.warn("No se configuró un ExecutorService dedicado. " +
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
     * <p>
     * Si la notificación no tiene variables, retorna la notificación original sin
     * modificar.
     * </p>
     * 
     * @param notification la notificación a procesar
     * @return una nueva notificación con el mensaje procesado, o la original si no
     *         hay variables
     */
    private Notification processTemplate(Notification notification) {
        // Si no hay variables de template, retornar sin modificar
        if (!notification.hasTemplateVariables()) {
            log.debug("Notificación [id={}] no tiene variables de template, se enviará sin procesamiento",
                    notification.id());
            return notification;
        }

        log.debug("Procesando template para notificación [id={}] con {} variable(s)",
                notification.id(), notification.templateVariables().size());

        // Procesar el mensaje usando el TemplateEngine
        String originalMessage = notification.message();
        String processedMessage = templateEngine.process(originalMessage, notification.templateVariables());

        log.info("Template procesado para notificación [id={}]: '{}' -> '{}'",
                notification.id(), originalMessage, processedMessage);

        // Crear una nueva notificación con el mensaje procesado
        // Mantenemos las variables originales por si se necesitan en el futuro
        return new Notification(
                notification.id(),
                notification.recipient(),
                processedMessage,
                notification.priority(),
                notification.templateVariables());
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
     * <p>
     * El orden de preferencia es: EMAIL > SMS > PUSH > SLACK
     * </p>
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
                        "El destinatario no tiene información válida para ningún canal configurado");
    }

    /**
     * Publica un evento de envío exitoso.
     * 
     * @param notificationId ID de la notificación
     * @param provider       nombre del proveedor
     * @param channel        canal utilizado
     * @param attemptNumber  número de intento
     */
    private void publishSentEvent(
            String notificationId,
            String provider,
            ChannelType channel,
            int attemptNumber) {
        try {
            NotificationSentEvent event = new NotificationSentEvent(
                    notificationId,
                    Instant.now(),
                    provider,
                    channel,
                    attemptNumber);
            eventPublisher.publish(event);
        } catch (Exception e) {
            // No queremos que un error al publicar eventos afecte el flujo principal
            log.error("Error al publicar NotificationSentEvent para notificación [id={}]: {}",
                    notificationId, e.getMessage(), e);
        }
    }

    /**
     * Publica un evento de fallo definitivo.
     * 
     * @param notificationId ID de la notificación
     * @param provider       nombre del proveedor
     * @param channel        canal utilizado
     * @param errorMessage   mensaje de error
     * @param totalAttempts  número total de intentos
     */
    private void publishFailedEvent(
            String notificationId,
            String provider,
            ChannelType channel,
            String errorMessage,
            int totalAttempts) {
        try {
            NotificationFailedEvent event = new NotificationFailedEvent(
                    notificationId,
                    Instant.now(),
                    provider,
                    channel,
                    errorMessage,
                    totalAttempts);
            eventPublisher.publish(event);
        } catch (Exception e) {
            // No queremos que un error al publicar eventos afecte el flujo principal
            log.error("Error al publicar NotificationFailedEvent para notificación [id={}]: {}",
                    notificationId, e.getMessage(), e);
        }
    }

    /**
     * Publica un evento de reintento.
     * 
     * @param notificationId ID de la notificación
     * @param provider       nombre del proveedor
     * @param channel        canal utilizado
     * @param attemptNumber  número de intento actual
     * @param maxAttempts    número máximo de intentos
     * @param delayMs        delay en milisegundos
     * @param reason         razón del reintento
     */
    private void publishRetryEvent(
            String notificationId,
            String provider,
            ChannelType channel,
            int attemptNumber,
            int maxAttempts,
            long delayMs,
            String reason) {
        try {
            NotificationRetryEvent event = new NotificationRetryEvent(
                    notificationId,
                    Instant.now(),
                    provider,
                    channel,
                    attemptNumber,
                    maxAttempts,
                    delayMs,
                    reason);
            eventPublisher.publish(event);
        } catch (Exception e) {
            // No queremos que un error al publicar eventos afecte el flujo principal
            log.error("Error al publicar NotificationRetryEvent para notificación [id={}]: {}",
                    notificationId, e.getMessage(), e);
        }
    }
}
