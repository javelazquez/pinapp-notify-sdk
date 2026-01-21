package com.pinapp.notify.example;

import com.pinapp.notify.adapters.mock.MockNotificationProvider;
import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.NotificationServiceImpl;
import com.pinapp.notify.core.events.NotificationEvent;
import com.pinapp.notify.core.events.NotificationFailedEvent;
import com.pinapp.notify.core.events.NotificationRetryEvent;
import com.pinapp.notify.core.events.NotificationSentEvent;
import com.pinapp.notify.core.events.NotificationSubscriber;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.RetryPolicy;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.in.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ejemplo completo del sistema de Pub/Sub para observabilidad.
 * 
 * <p>Este ejemplo demuestra:</p>
 * <ul>
 *   <li>Cómo registrar suscriptores globales durante la inicialización</li>
 *   <li>Uso de Pattern Matching (Java 21) para manejar diferentes tipos de eventos</li>
 *   <li>Implementación de métricas personalizadas</li>
 *   <li>Logging estructurado de eventos</li>
 *   <li>Alertas basadas en eventos de fallo</li>
 * </ul>
 * 
 * @author PinApp Team
 */
public class ObservabilityExample {
    
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityExample.class);
    
    public static void main(String[] args) {
        logger.info("=== Ejemplo de Sistema Pub/Sub para Observabilidad ===\n");
        
        // 1. Crear un recolector de métricas personalizado
        MetricsCollector metricsCollector = new MetricsCollector();
        
        // 2. Crear un suscriptor para logging estructurado
        NotificationSubscriber structuredLogger = event -> {
            switch (event) {
                case NotificationSentEvent sent -> 
                    logger.info("[SUCCESS] Notificación {} enviada vía {} por {} en intento #{}", 
                        sent.notificationId(), 
                        sent.channel(), 
                        sent.provider(),
                        sent.attemptNumber());
                
                case NotificationFailedEvent failed -> 
                    logger.error("[FAILURE] Notificación {} falló después de {} intentos. " +
                        "Provider: {}, Channel: {}, Error: {}", 
                        failed.notificationId(),
                        failed.totalAttempts(),
                        failed.provider(),
                        failed.channel(),
                        failed.errorMessage());
                
                case NotificationRetryEvent retry -> 
                    logger.warn("[RETRY] Notificación {} - Intento {}/{} después de {}ms. " +
                        "Provider: {}, Channel: {}, Razón: {}", 
                        retry.notificationId(),
                        retry.attemptNumber(),
                        retry.maxAttempts(),
                        retry.delayMs(),
                        retry.provider(),
                        retry.channel(),
                        retry.reason());
            }
        };
        
        // 3. Crear un suscriptor para alertas de fallos críticos
        NotificationSubscriber alertingSystem = event -> {
            if (event instanceof NotificationFailedEvent failed) {
                // En producción, aquí enviarías una alerta a PagerDuty, Slack, etc.
                logger.error("[ALERT] ¡Notificación crítica falló! ID: {}, Error: {}", 
                    failed.notificationId(), 
                    failed.errorMessage());
            }
        };
        
        // 4. Configurar el SDK con múltiples suscriptores
        PinappNotifyConfig config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, MockNotificationProvider.forEmail())
            .withRetryPolicy(RetryPolicy.of(3, 100))
            .addSubscriber(metricsCollector)
            .addSubscriber(structuredLogger)
            .addSubscriber(alertingSystem)
            .build();
        
        NotificationService notificationService = new NotificationServiceImpl(config);
        
        logger.info("Sistema inicializado con {} suscriptores\n", 
            config.getEventPublisher().getSubscriberCount());
        
        // 5. Enviar notificaciones de prueba
        for (int i = 1; i <= 5; i++) {
            Notification notification = Notification.create(
                new Recipient(
                    "user" + i + "@example.com",
                    null,
                    Map.of("name", "Usuario " + i)
                ),
                "Prueba de observabilidad #" + i + ": Este es un mensaje de prueba para demostrar el sistema de eventos."
            );
            
            logger.info("\n--- Enviando notificación {} ---", notification.id());
            NotificationResult result = notificationService.send(notification, ChannelType.EMAIL);
            
            if (result.success()) {
                logger.info("✓ Notificación {} enviada exitosamente\n", notification.id());
            } else {
                logger.error("✗ Notificación {} falló: {}\n", 
                    notification.id(), 
                    result.errorMessage());
            }
            
            // Pequeña pausa para mejor legibilidad del log
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 6. Mostrar métricas recolectadas
        logger.info("\n=== Métricas Finales ===");
        metricsCollector.printMetrics();
        
        logger.info("\n=== Ejemplo completado ===");
    }
    
    /**
     * Recolector de métricas personalizado que implementa NotificationSubscriber.
     * 
     * <p>Esta clase demuestra cómo crear un suscriptor que recopila estadísticas
     * sobre el comportamiento del sistema de notificaciones.</p>
     */
    static class MetricsCollector implements NotificationSubscriber {
        
        private final AtomicInteger totalSent = new AtomicInteger(0);
        private final AtomicInteger totalFailed = new AtomicInteger(0);
        private final AtomicInteger totalRetries = new AtomicInteger(0);
        private final Map<ChannelType, AtomicInteger> sentByChannel = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> sentByProvider = new ConcurrentHashMap<>();
        
        @Override
        public void onEvent(NotificationEvent event) {
            switch (event) {
                case NotificationSentEvent sent -> {
                    totalSent.incrementAndGet();
                    sentByChannel
                        .computeIfAbsent(sent.channel(), k -> new AtomicInteger(0))
                        .incrementAndGet();
                    sentByProvider
                        .computeIfAbsent(sent.provider(), k -> new AtomicInteger(0))
                        .incrementAndGet();
                }
                
                case NotificationFailedEvent failed -> {
                    totalFailed.incrementAndGet();
                }
                
                case NotificationRetryEvent retry -> {
                    totalRetries.incrementAndGet();
                }
            }
        }
        
        /**
         * Imprime un resumen de las métricas recolectadas.
         */
        public void printMetrics() {
            logger.info("Total enviadas: {}", totalSent.get());
            logger.info("Total fallidas: {}", totalFailed.get());
            logger.info("Total reintentos: {}", totalRetries.get());
            
            if (!sentByChannel.isEmpty()) {
                logger.info("\nPor canal:");
                sentByChannel.forEach((channel, count) -> 
                    logger.info("  - {}: {}", channel, count.get())
                );
            }
            
            if (!sentByProvider.isEmpty()) {
                logger.info("\nPor proveedor:");
                sentByProvider.forEach((provider, count) -> 
                    logger.info("  - {}: {}", provider, count.get())
                );
            }
            
            int total = totalSent.get() + totalFailed.get();
            if (total > 0) {
                double successRate = (totalSent.get() * 100.0) / total;
                logger.info("\nTasa de éxito: {:.2f}%", successRate);
            }
        }
        
        // Métodos para acceder a las métricas (útil en tests)
        public int getTotalSent() { return totalSent.get(); }
        public int getTotalFailed() { return totalFailed.get(); }
        public int getTotalRetries() { return totalRetries.get(); }
    }
}
