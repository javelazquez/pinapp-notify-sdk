package com.pinapp.notify.config;

import com.pinapp.notify.core.events.NotificationEventPublisher;
import com.pinapp.notify.core.events.NotificationSubscriber;
import com.pinapp.notify.domain.RetryPolicy;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.out.NotificationProvider;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Configuración principal del SDK de notificaciones PinApp.
 * 
 * <p>Esta clase permite configurar la librería mediante código Java puro,
 * sin necesidad de archivos YAML o properties. Utiliza el patrón Builder
 * para proporcionar una API fluida y fácil de usar.</p>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * PinappNotifyConfig config = PinappNotifyConfig.builder()
 *     .addProvider(ChannelType.EMAIL, new EmailProvider(apiKey))
 *     .addProvider(ChannelType.SMS, new SmsProvider(apiKey))
 *     .build();
 * }</pre>
 * 
 * @author PinApp Team
 */
@Getter
public class PinappNotifyConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(PinappNotifyConfig.class);
    
    /**
     * Configuración programática de SLF4J Simple Logger.
     * Reemplaza la necesidad de archivos .properties, .xml o .yaml.
     */
    static {
        // Configurar nivel de log por defecto
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        
        // Mostrar fecha y hora
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        
        // Mostrar el nombre del thread
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        
        // Mostrar el nombre corto del logger
        System.setProperty("org.slf4j.simpleLogger.showLogName", "true");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
        
        // Niveles específicos para los paquetes del SDK
        System.setProperty("org.slf4j.simpleLogger.log.com.pinapp.notify", "debug");
        System.setProperty("org.slf4j.simpleLogger.log.com.pinapp.notify.core", "info");
        System.setProperty("org.slf4j.simpleLogger.log.com.pinapp.notify.adapters.mock", "info");
    }
    
    /**
     * Mapa de proveedores indexados por tipo de canal.
     * Utilizamos EnumMap para mejor rendimiento y type-safety.
     */
    private final Map<ChannelType, NotificationProvider> providers;
    
    /**
     * Política de reintentos para envíos fallidos.
     */
    private final RetryPolicy retryPolicy;
    
    /**
     * ExecutorService dedicado para operaciones asíncronas.
     * Null si no se configuró ejecución asíncrona.
     */
    private final ExecutorService executorService;
    
    /**
     * Indica si el ExecutorService fue creado internamente y debe ser cerrado.
     */
    private final boolean shouldShutdownExecutor;
    
    /**
     * Publisher de eventos del ciclo de vida de notificaciones.
     * Permite suscribirse a eventos de envío exitoso, fallo y reintentos.
     */
    private final NotificationEventPublisher eventPublisher;
    
    /**
     * Constructor privado para forzar el uso del Builder.
     * 
     * @param providers mapa de proveedores configurados
     * @param retryPolicy política de reintentos
     * @param executorService executor para operaciones asíncronas
     * @param shouldShutdownExecutor si se debe cerrar el executor al hacer shutdown
     * @param eventPublisher publisher de eventos del ciclo de vida
     */
    private PinappNotifyConfig(
            Map<ChannelType, NotificationProvider> providers,
            RetryPolicy retryPolicy,
            ExecutorService executorService,
            boolean shouldShutdownExecutor,
            NotificationEventPublisher eventPublisher) {
        this.providers = new EnumMap<>(providers);
        this.retryPolicy = retryPolicy;
        this.executorService = executorService;
        this.shouldShutdownExecutor = shouldShutdownExecutor;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Obtiene el proveedor configurado para un tipo de canal específico.
     * 
     * @param channelType el tipo de canal
     * @return un Optional conteniendo el proveedor si existe, o vacío si no está configurado
     */
    public Optional<NotificationProvider> getProvider(ChannelType channelType) {
        return Optional.ofNullable(providers.get(channelType));
    }
    
    /**
     * Verifica si existe un proveedor configurado para el canal especificado.
     * 
     * @param channelType el tipo de canal a verificar
     * @return true si hay un proveedor configurado, false en caso contrario
     */
    public boolean hasProvider(ChannelType channelType) {
        return providers.containsKey(channelType);
    }
    
    /**
     * Cierra ordenadamente el ExecutorService y libera recursos.
     * 
     * <p>Este método debe ser llamado cuando la aplicación se está cerrando
     * para asegurar que todas las tareas asíncronas pendientes se completen
     * y los recursos se liberen adecuadamente.</p>
     * 
     * <p>Si el ExecutorService fue proporcionado externamente y no fue creado
     * por la configuración, no se cerrará automáticamente.</p>
     * 
     * @param timeoutSeconds tiempo máximo en segundos para esperar a que terminen las tareas
     * @return true si el shutdown fue exitoso, false si hubo timeout
     */
    public boolean shutdown(long timeoutSeconds) {
        if (executorService == null || !shouldShutdownExecutor) {
            logger.debug("No hay ExecutorService para cerrar o fue proporcionado externamente");
            return true;
        }
        
        logger.info("Iniciando shutdown del ExecutorService...");
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                logger.warn("El ExecutorService no terminó en {} segundos, forzando shutdown...", 
                    timeoutSeconds);
                executorService.shutdownNow();
                
                if (!executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    logger.error("El ExecutorService no pudo ser cerrado correctamente");
                    return false;
                }
            }
            
            logger.info("ExecutorService cerrado exitosamente");
            return true;
            
        } catch (InterruptedException e) {
            logger.error("Shutdown interrumpido", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Cierra ordenadamente el ExecutorService con un timeout por defecto de 10 segundos.
     * 
     * @return true si el shutdown fue exitoso, false si hubo timeout
     */
    public boolean shutdown() {
        return shutdown(10);
    }
    
    /**
     * Crea un nuevo Builder para construir la configuración.
     * 
     * @return una nueva instancia de Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder para construir instancias de PinappNotifyConfig de forma fluida.
     */
    public static class Builder {
        
        private final Map<ChannelType, NotificationProvider> providers;
        private RetryPolicy retryPolicy;
        private ExecutorService executorService;
        private Integer asyncThreadPoolSize;
        private final NotificationEventPublisher eventPublisher;
        
        private Builder() {
            this.providers = new EnumMap<>(ChannelType.class);
            this.retryPolicy = RetryPolicy.defaultPolicy(); // Por defecto: 3 intentos, 1s delay
            this.asyncThreadPoolSize = null; // Se creará bajo demanda si se usa async
            this.eventPublisher = new NotificationEventPublisher();
        }
        
        /**
         * Agrega un proveedor para un canal específico.
         * 
         * <p>Si ya existe un proveedor configurado para ese canal,
         * será reemplazado por el nuevo.</p>
         * 
         * @param channelType el tipo de canal
         * @param provider el proveedor a registrar
         * @return esta instancia del Builder para encadenamiento fluido
         * @throws IllegalArgumentException si channelType o provider son null
         */
        public Builder addProvider(ChannelType channelType, NotificationProvider provider) {
            if (channelType == null) {
                throw new IllegalArgumentException("El tipo de canal no puede ser null");
            }
            if (provider == null) {
                throw new IllegalArgumentException("El proveedor no puede ser null");
            }
            
            // Validamos que el proveedor soporte el canal
            if (!provider.supports(channelType)) {
                throw new IllegalArgumentException(
                    String.format("El proveedor '%s' no soporta el canal %s", 
                        provider.getName(), channelType)
                );
            }
            
            this.providers.put(channelType, provider);
            return this;
        }
        
        /**
         * Agrega un proveedor detectando automáticamente los canales soportados.
         * 
         * <p>Este método itera sobre todos los tipos de canal y registra
         * el proveedor para aquellos que soporte.</p>
         * 
         * @param provider el proveedor a registrar
         * @return esta instancia del Builder para encadenamiento fluido
         * @throws IllegalArgumentException si provider es null
         */
        public Builder addProvider(NotificationProvider provider) {
            if (provider == null) {
                throw new IllegalArgumentException("El proveedor no puede ser null");
            }
            
            // Registramos el proveedor para todos los canales que soporte
            for (ChannelType channelType : ChannelType.values()) {
                if (provider.supports(channelType)) {
                    this.providers.put(channelType, provider);
                }
            }
            
            return this;
        }
        
        /**
         * Configura la política de reintentos global.
         * 
         * <p>Esta política se aplicará a todos los envíos de notificaciones.
         * Si no se configura, se usa la política por defecto (3 intentos, 1s delay).</p>
         * 
         * @param retryPolicy la política de reintentos a utilizar
         * @return esta instancia del Builder para encadenamiento fluido
         * @throws IllegalArgumentException si retryPolicy es null
         */
        public Builder withRetryPolicy(RetryPolicy retryPolicy) {
            if (retryPolicy == null) {
                throw new IllegalArgumentException("La política de reintentos no puede ser null");
            }
            this.retryPolicy = retryPolicy;
            return this;
        }
        
        /**
         * Configura una política sin reintentos.
         * 
         * <p>Útil cuando se quiere deshabilitar completamente los reintentos.</p>
         * 
         * @return esta instancia del Builder para encadenamiento fluido
         */
        public Builder withoutRetries() {
            this.retryPolicy = RetryPolicy.noRetry();
            return this;
        }
        
        /**
         * Configura un ExecutorService personalizado para operaciones asíncronas.
         * 
         * <p>Si se proporciona un ExecutorService externo, la configuración NO lo
         * cerrará automáticamente en shutdown(). La responsabilidad de cerrar el
         * executor recae en el código que lo proporcionó.</p>
         * 
         * @param executorService el ExecutorService a utilizar
         * @return esta instancia del Builder para encadenamiento fluido
         * @throws IllegalArgumentException si executorService es null
         */
        public Builder withExecutorService(ExecutorService executorService) {
            if (executorService == null) {
                throw new IllegalArgumentException("El ExecutorService no puede ser null");
            }
            this.executorService = executorService;
            this.asyncThreadPoolSize = null; // Ignorar pool size si se proporciona un executor
            return this;
        }
        
        /**
         * Configura el tamaño del thread pool para operaciones asíncronas.
         * 
         * <p>Si no se especifica, se creará un thread pool con un tamaño igual
         * al número de procesadores disponibles.</p>
         * 
         * @param poolSize el número de threads en el pool (debe ser > 0)
         * @return esta instancia del Builder para encadenamiento fluido
         * @throws IllegalArgumentException si poolSize <= 0
         */
        public Builder withAsyncThreadPoolSize(int poolSize) {
            if (poolSize <= 0) {
                throw new IllegalArgumentException(
                    "El tamaño del pool debe ser mayor a 0, recibido: " + poolSize
                );
            }
            this.asyncThreadPoolSize = poolSize;
            return this;
        }
        
        /**
         * Habilita el envío asíncrono con un thread pool de tamaño por defecto.
         * 
         * <p>El tamaño por defecto es igual al número de procesadores disponibles.</p>
         * 
         * @return esta instancia del Builder para encadenamiento fluido
         */
        public Builder enableAsync() {
            this.asyncThreadPoolSize = Runtime.getRuntime().availableProcessors();
            return this;
        }
        
        /**
         * Registra un suscriptor global para recibir eventos del ciclo de vida de notificaciones.
         * 
         * <p>Los suscriptores registrados aquí recibirán eventos de todas las notificaciones
         * procesadas por la librería, incluyendo:</p>
         * <ul>
         *   <li>NotificationSentEvent - cuando una notificación se envía exitosamente</li>
         *   <li>NotificationFailedEvent - cuando una notificación falla definitivamente</li>
         *   <li>NotificationRetryEvent - cuando se realiza un reintento</li>
         * </ul>
         * 
         * <p>Ejemplo de uso:</p>
         * <pre>{@code
         * PinappNotifyConfig config = PinappNotifyConfig.builder()
         *     .addProvider(ChannelType.EMAIL, emailProvider)
         *     .addSubscriber(event -> {
         *         switch (event) {
         *             case NotificationSentEvent sent -> 
         *                 metricsCollector.recordSuccess(sent);
         *             case NotificationFailedEvent failed -> 
         *                 alertingService.sendAlert(failed);
         *             case NotificationRetryEvent retry -> 
         *                 logger.warn("Reintento: {}", retry);
         *         }
         *     })
         *     .build();
         * }</pre>
         * 
         * @param subscriber el suscriptor a registrar
         * @return esta instancia del Builder para encadenamiento fluido
         * @throws IllegalArgumentException si subscriber es null
         * @see NotificationSubscriber
         * @see com.pinapp.notify.core.events.NotificationEvent
         */
        public Builder addSubscriber(NotificationSubscriber subscriber) {
            if (subscriber == null) {
                throw new IllegalArgumentException("El suscriptor no puede ser null");
            }
            
            this.eventPublisher.subscribe(subscriber);
            logger.debug("Suscriptor global registrado durante la configuración");
            return this;
        }
        
        /**
         * Construye la instancia final de PinappNotifyConfig.
         * 
         * @return una nueva instancia de PinappNotifyConfig
         * @throws IllegalStateException si no se ha configurado ningún proveedor
         */
        public PinappNotifyConfig build() {
            if (providers.isEmpty()) {
                throw new IllegalStateException(
                    "Debe configurar al menos un proveedor antes de construir la configuración"
                );
            }
            
            // Crear el ExecutorService si se especificó un tamaño de pool pero no se proporcionó uno externo
            ExecutorService finalExecutor = executorService;
            boolean shouldShutdown = false;
            
            if (finalExecutor == null && asyncThreadPoolSize != null) {
                finalExecutor = Executors.newFixedThreadPool(
                    asyncThreadPoolSize,
                    r -> {
                        Thread t = new Thread(r, "pinapp-notify-async-" + System.nanoTime());
                        t.setDaemon(false); // No daemon para asegurar que las tareas se completen
                        return t;
                    }
                );
                shouldShutdown = true;
                logger.debug("ExecutorService creado con pool size: {}", asyncThreadPoolSize);
            }
            
            logger.info("PinappNotifyConfig construido con {} proveedor(es) y {} suscriptor(es)",
                providers.size(), eventPublisher.getSubscriberCount());
            
            return new PinappNotifyConfig(providers, retryPolicy, finalExecutor, shouldShutdown, eventPublisher);
        }
    }
}
