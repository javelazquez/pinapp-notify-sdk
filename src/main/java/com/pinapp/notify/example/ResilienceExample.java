package com.pinapp.notify.example;

import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.NotificationServiceImpl;
import com.pinapp.notify.adapters.email.EmailNotificationProvider;
import com.pinapp.notify.adapters.sms.SmsNotificationProvider;
import com.pinapp.notify.domain.*;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.in.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ejemplo de uso de las características de resiliencia y asincronía.
 * 
 * <p>Demuestra:</p>
 * <ul>
 *   <li>Configuración de política de reintentos</li>
 *   <li>Envío asíncrono con CompletableFuture</li>
 *   <li>Configuración de ExecutorService personalizado</li>
 *   <li>Manejo de múltiples notificaciones en paralelo</li>
 *   <li>Shutdown ordenado de recursos</li>
 * </ul>
 * 
 * @author PinApp Team
 */
public class ResilienceExample {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilienceExample.class);
    
    public static void main(String[] args) {
        logger.info("=== Ejemplo de Resiliencia y Asincronía ===\n");
        
        // Ejemplo 1: Configuración con reintentos
        retryPolicyExample();
        
        // Ejemplo 2: Envío asíncrono básico
        basicAsyncExample();
        
        // Ejemplo 3: Múltiples envíos asíncronos en paralelo
        parallelAsyncExample();
        
        // Ejemplo 4: ExecutorService personalizado
        customExecutorExample();
        
        // Ejemplo 5: Composición de futures
        futureCompositionExample();
        
        logger.info("\n=== Fin de ejemplos ===");
    }
    
    /**
     * Ejemplo 1: Configuración con política de reintentos.
     */
    private static void retryPolicyExample() {
        logger.info("\n--- Ejemplo 1: Política de Reintentos ---");
        
        // Configurar con política de reintentos personalizada
        PinappNotifyConfig config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
            .withRetryPolicy(RetryPolicy.of(5, 500)) // 5 intentos, 500ms entre reintentos
            .build();
        
        NotificationService service = new NotificationServiceImpl(config);
        
        Recipient recipient = new Recipient(
            "usuario@example.com",
            null,
            Map.of("subject", "Test con Reintentos")
        );
        
        Notification notification = Notification.create(
            recipient,
            "Este email será enviado con hasta 5 intentos si falla",
            NotificationPriority.HIGH
        );
        
        NotificationResult result = service.send(notification, ChannelType.EMAIL);
        
        logger.info("Resultado: {} - {}", 
            result.success() ? "ÉXITO" : "FALLO", 
            result.success() ? "Enviado" : result.errorMessage());
    }
    
    /**
     * Ejemplo 2: Envío asíncrono básico.
     */
    private static void basicAsyncExample() {
        logger.info("\n--- Ejemplo 2: Envío Asíncrono Básico ---");
        
        PinappNotifyConfig config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.SMS, new SmsNotificationProvider())
            .enableAsync() // Habilita envío asíncrono
            .build();
        
        NotificationService service = new NotificationServiceImpl(config);
        
        Recipient recipient = new Recipient(
            null,
            "+56912345678",
            Map.of()
        );
        
        Notification notification = Notification.create(
            recipient,
            "SMS enviado de forma asíncrona",
            NotificationPriority.NORMAL
        );
        
        logger.info("Iniciando envío asíncrono...");
        
        CompletableFuture<NotificationResult> future = service.sendAsync(notification, ChannelType.SMS);
        
        // Procesar el resultado con callbacks
        future
            .thenAccept(result -> {
                if (result.success()) {
                    logger.info("✓ Notificación asíncrona enviada exitosamente [id={}]", 
                        result.notificationId());
                } else {
                    logger.error("✗ Notificación asíncrona falló: {}", result.errorMessage());
                }
            })
            .exceptionally(error -> {
                logger.error("Error en el envío asíncrono: {}", error.getMessage());
                return null;
            });
        
        // Esperar a que se complete (solo para el ejemplo)
        try {
            future.join();
        } catch (Exception e) {
            logger.error("Error esperando el resultado: {}", e.getMessage());
        }
        
        // Cerrar recursos
        config.shutdown();
    }
    
    /**
     * Ejemplo 3: Múltiples envíos asíncronos en paralelo.
     */
    private static void parallelAsyncExample() {
        logger.info("\n--- Ejemplo 3: Envíos Asíncronos en Paralelo ---");
        
        PinappNotifyConfig config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
            .withAsyncThreadPoolSize(4) // Pool de 4 threads
            .build();
        
        NotificationService service = new NotificationServiceImpl(config);
        
        // Enviar 5 notificaciones en paralelo
        CompletableFuture<?>[] futures = new CompletableFuture[5];
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            Recipient recipient = new Recipient(
                "usuario" + i + "@example.com",
                null,
                Map.of("subject", "Email Paralelo #" + i)
            );
            
            Notification notification = Notification.create(
                recipient,
                "Mensaje número " + i + " enviado en paralelo"
            );
            
            futures[i] = service.sendAsync(notification, ChannelType.EMAIL)
                .thenAccept(result -> {
                    logger.info("Email #{} - {}", index, 
                        result.success() ? "ENVIADO" : "FALLIDO");
                });
        }
        
        // Esperar a que todas se completen
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        
        try {
            allFutures.join();
            logger.info("Todas las notificaciones paralelas completadas");
        } catch (Exception e) {
            logger.error("Error en envíos paralelos: {}", e.getMessage());
        }
        
        // Cerrar recursos
        config.shutdown();
    }
    
    /**
     * Ejemplo 4: Uso de ExecutorService personalizado.
     */
    private static void customExecutorExample() {
        logger.info("\n--- Ejemplo 4: ExecutorService Personalizado ---");
        
        // Crear un ExecutorService personalizado
        ExecutorService customExecutor = Executors.newFixedThreadPool(
            2,
            r -> {
                Thread t = new Thread(r, "custom-notifier-" + System.nanoTime());
                t.setPriority(Thread.MAX_PRIORITY); // Prioridad alta
                return t;
            }
        );
        
        PinappNotifyConfig config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
            .withExecutorService(customExecutor)
            .withRetryPolicy(RetryPolicy.of(2, 100))
            .build();
        
        NotificationService service = new NotificationServiceImpl(config);
        
        Recipient recipient = new Recipient(
            "vip@example.com",
            null,
            Map.of("subject", "Email con Executor Personalizado")
        );
        
        Notification notification = Notification.create(
            recipient,
            "Este email usa un ExecutorService con prioridad alta",
            NotificationPriority.CRITICAL
        );
        
        CompletableFuture<NotificationResult> future = service.sendAsync(notification, ChannelType.EMAIL);
        
        try {
            NotificationResult result = future.join();
            logger.info("Resultado con executor personalizado: {}", 
                result.success() ? "ÉXITO" : "FALLO");
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
        }
        
        // Nota: No se cierra automáticamente porque fue proporcionado externamente
        customExecutor.shutdown();
    }
    
    /**
     * Ejemplo 5: Composición y transformación de futures.
     */
    private static void futureCompositionExample() {
        logger.info("\n--- Ejemplo 5: Composición de Futures ---");
        
        PinappNotifyConfig config = PinappNotifyConfig.builder()
            .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
            .addProvider(ChannelType.SMS, new SmsNotificationProvider())
            .enableAsync()
            .build();
        
        NotificationService service = new NotificationServiceImpl(config);
        
        Recipient emailRecipient = new Recipient(
            "user@example.com",
            null,
            Map.of("subject", "Confirmación")
        );
        
        Recipient smsRecipient = new Recipient(
            null,
            "+56987654321",
            Map.of()
        );
        
        // Enviar email y luego SMS de forma secuencial
        service.sendAsync(Notification.create(emailRecipient, "Email de confirmación"), ChannelType.EMAIL)
            .thenCompose(emailResult -> {
                if (emailResult.success()) {
                    logger.info("Email enviado, ahora enviando SMS...");
                    return service.sendAsync(
                        Notification.create(smsRecipient, "Código: 123456"), 
                        ChannelType.SMS
                    );
                } else {
                    logger.warn("Email falló, no se enviará SMS");
                    return CompletableFuture.completedFuture(emailResult);
                }
            })
            .thenApply(finalResult -> {
                String status = finalResult.success() ? "COMPLETADO" : "FALLIDO";
                return String.format("Proceso de notificación: %s - Canal: %s", 
                    status, finalResult.channelType());
            })
            .thenAccept(logger::info)
            .exceptionally(error -> {
                logger.error("Error en la composición: {}", error.getMessage());
                return null;
            })
            .join(); // Esperar para el ejemplo
        
        config.shutdown();
    }
}
