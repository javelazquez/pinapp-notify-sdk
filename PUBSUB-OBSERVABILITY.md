# Sistema de Pub/Sub para Observabilidad

## 📋 Descripción

El SDK de notificaciones de PinApp incluye un sistema robusto de Publicación/Suscripción (Pub/Sub) que permite observar en tiempo real el ciclo de vida completo de las notificaciones.

## 🎯 Características

- **Eventos del Ciclo de Vida**: Captura envíos exitosos, fallos y reintentos
- **Pattern Matching Java 21**: Uso de Sealed Interfaces para manejo exhaustivo de eventos
- **Thread-Safe**: Implementación segura para entornos multi-thread
- **Publicación Segura**: Los errores en suscriptores no afectan el flujo principal
- **Sin Dependencias Externas**: Implementación pura del patrón Observer

## 📦 Componentes

### 1. Eventos (`com.pinapp.notify.core.events`)

#### `NotificationEvent` (Sealed Interface)

Interfaz base para todos los eventos del sistema. Garantiza que solo los tipos conocidos puedan ser implementados.

```java
public sealed interface NotificationEvent 
    permits NotificationSentEvent, NotificationFailedEvent, NotificationRetryEvent {
    
    String notificationId();
    Instant timestamp();
}
```

#### `NotificationSentEvent` (Record)

Emitido cuando una notificación se envía exitosamente.

**Campos:**
- `notificationId`: ID de la notificación
- `timestamp`: Momento del envío
- `provider`: Nombre del proveedor utilizado
- `channel`: Canal (EMAIL, SMS, PUSH, SLACK)
- `attemptNumber`: Número de intento exitoso (1 si fue al primer intento)

```java
NotificationSentEvent(
    String notificationId,
    Instant timestamp,
    String provider,
    ChannelType channel,
    int attemptNumber
)
```

#### `NotificationFailedEvent` (Record)

Emitido cuando una notificación falla definitivamente después de agotar todos los reintentos.

**Campos:**
- `notificationId`: ID de la notificación
- `timestamp`: Momento del fallo definitivo
- `provider`: Nombre del proveedor
- `channel`: Canal utilizado
- `errorMessage`: Descripción del error
- `totalAttempts`: Número total de intentos realizados

```java
NotificationFailedEvent(
    String notificationId,
    Instant timestamp,
    String provider,
    ChannelType channel,
    String errorMessage,
    int totalAttempts
)
```

#### `NotificationRetryEvent` (Record)

Emitido antes de cada reintento de envío.

**Campos:**
- `notificationId`: ID de la notificación
- `timestamp`: Momento del reintento
- `provider`: Nombre del proveedor
- `channel`: Canal utilizado
- `attemptNumber`: Número del intento actual (≥ 2)
- `maxAttempts`: Número máximo de intentos configurado
- `delayMs`: Tiempo de espera antes del reintento (ms)
- `reason`: Razón del reintento (error del intento anterior)

```java
NotificationRetryEvent(
    String notificationId,
    Instant timestamp,
    String provider,
    ChannelType channel,
    int attemptNumber,
    int maxAttempts,
    long delayMs,
    String reason
)
```

### 2. Suscriptor (`NotificationSubscriber`)

Interfaz funcional que define un suscriptor de eventos.

```java
@FunctionalInterface
public interface NotificationSubscriber {
    void onEvent(NotificationEvent event);
}
```

### 3. Publisher (`NotificationEventPublisher`)

Gestiona la lista de suscriptores y publica eventos de forma segura.

**Métodos principales:**
- `subscribe(NotificationSubscriber)`: Registra un suscriptor
- `unsubscribe(NotificationSubscriber)`: Elimina un suscriptor
- `publish(NotificationEvent)`: Publica un evento a todos los suscriptores
- `getSubscriberCount()`: Obtiene el número de suscriptores activos
- `clear()`: Elimina todos los suscriptores

## 🚀 Uso

### Registro de Suscriptores Globales

La forma recomendada es registrar suscriptores durante la inicialización del SDK:

```java
PinappNotifyConfig config = PinappNotifyConfig.builder()
    .addProvider(ChannelType.EMAIL, emailProvider)
    .addProvider(ChannelType.SMS, smsProvider)
    
    // Registrar suscriptores
    .addSubscriber(event -> {
        // Tu lógica aquí
        System.out.println("Evento recibido: " + event);
    })
    
    .build();

NotificationService service = new NotificationServiceImpl(config);
```

### Pattern Matching (Java 21)

Aprovecha las Sealed Interfaces para manejar eventos de forma exhaustiva:

```java
NotificationSubscriber subscriber = event -> {
    switch (event) {
        case NotificationSentEvent sent -> 
            logger.info("✓ Enviado por {} vía {} en intento #{}", 
                sent.provider(), sent.channel(), sent.attemptNumber());
        
        case NotificationFailedEvent failed -> 
            logger.error("✗ Falló después de {} intentos: {}", 
                failed.totalAttempts(), failed.errorMessage());
        
        case NotificationRetryEvent retry -> 
            logger.warn("↻ Reintento {}/{} después de {}ms: {}", 
                retry.attemptNumber(), retry.maxAttempts(), 
                retry.delayMs(), retry.reason());
    }
};
```

### Ejemplo: Recolector de Métricas

```java
public class MetricsCollector implements NotificationSubscriber {
    
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    
    @Override
    public void onEvent(NotificationEvent event) {
        switch (event) {
            case NotificationSentEvent sent -> 
                successCount.incrementAndGet();
            case NotificationFailedEvent failed -> 
                failureCount.incrementAndGet();
            case NotificationRetryEvent retry -> 
                retryCount.incrementAndGet();
        }
    }
    
    public void printMetrics() {
        System.out.println("Éxitos: " + successCount.get());
        System.out.println("Fallos: " + failureCount.get());
        System.out.println("Reintentos: " + retryCount.get());
    }
}

// Uso
MetricsCollector metrics = new MetricsCollector();
config = PinappNotifyConfig.builder()
    .addProvider(...)
    .addSubscriber(metrics)
    .build();
```

### Ejemplo: Sistema de Alertas

```java
NotificationSubscriber alertingSystem = event -> {
    if (event instanceof NotificationFailedEvent failed) {
        // Enviar alerta a PagerDuty, Slack, etc.
        alertService.sendCriticalAlert(
            "Notificación " + failed.notificationId() + " falló: " + 
            failed.errorMessage()
        );
    }
};

config = PinappNotifyConfig.builder()
    .addProvider(...)
    .addSubscriber(alertingSystem)
    .build();
```

### Ejemplo: Logging Estructurado

```java
NotificationSubscriber structuredLogger = event -> {
    Map<String, Object> logData = new HashMap<>();
    logData.put("eventType", event.getClass().getSimpleName());
    logData.put("notificationId", event.notificationId());
    logData.put("timestamp", event.timestamp());
    
    switch (event) {
        case NotificationSentEvent sent -> {
            logData.put("provider", sent.provider());
            logData.put("channel", sent.channel());
            logData.put("attemptNumber", sent.attemptNumber());
        }
        case NotificationFailedEvent failed -> {
            logData.put("errorMessage", failed.errorMessage());
            logData.put("totalAttempts", failed.totalAttempts());
        }
        case NotificationRetryEvent retry -> {
            logData.put("attemptNumber", retry.attemptNumber());
            logData.put("maxAttempts", retry.maxAttempts());
            logData.put("delayMs", retry.delayMs());
        }
    }
    
    // Enviar a sistema de logging estructurado (ELK, Datadog, etc.)
    structuredLogger.log(logData);
};
```

### Ejemplo: Integración con OpenTelemetry

```java
NotificationSubscriber telemetrySubscriber = event -> {
    Span span = tracer.spanBuilder("notification_event")
        .setAttribute("event.type", event.getClass().getSimpleName())
        .setAttribute("notification.id", event.notificationId())
        .startSpan();
    
    try (Scope scope = span.makeCurrent()) {
        switch (event) {
            case NotificationSentEvent sent -> {
                span.setAttribute("provider", sent.provider());
                span.setAttribute("channel", sent.channel().toString());
                span.setStatus(StatusCode.OK);
            }
            case NotificationFailedEvent failed -> {
                span.setAttribute("error.message", failed.errorMessage());
                span.setStatus(StatusCode.ERROR);
            }
            case NotificationRetryEvent retry -> {
                span.setAttribute("retry.attempt", retry.attemptNumber());
            }
        }
    } finally {
        span.end();
    }
};
```

## ⚠️ Best Practices

### 1. Mantener Suscriptores Rápidos

Los eventos se notifican **síncronamente** en el mismo thread. Mantén la lógica de `onEvent` rápida:

```java
// ✗ MAL - Operación bloqueante
NotificationSubscriber bad = event -> {
    httpClient.post("https://api.example.com/events", event); // Bloqueante!
};

// ✓ BIEN - Delegar a thread pool
NotificationSubscriber good = event -> {
    executorService.submit(() -> {
        httpClient.post("https://api.example.com/events", event);
    });
};
```

### 2. Manejar Excepciones Internamente

Aunque el publisher captura excepciones, es mejor práctica manejarlas dentro del suscriptor:

```java
NotificationSubscriber safe = event -> {
    try {
        // Tu lógica aquí
        processEvent(event);
    } catch (Exception e) {
        logger.error("Error procesando evento: {}", e.getMessage(), e);
        // Opcionalmente: enviar a sistema de monitoreo
    }
};
```

### 3. Usar Lambdas para Casos Simples

Para lógica simple, usa lambdas directamente:

```java
config.builder()
    .addSubscriber(event -> logger.info("Evento: {}", event))
    .addSubscriber(event -> metrics.record(event))
    .build();
```

### 4. Crear Clases para Lógica Compleja

Para lógica compleja, implementa la interfaz explícitamente:

```java
public class ComplexEventHandler implements NotificationSubscriber {
    
    private final MetricsService metrics;
    private final AlertService alerts;
    private final AuditService audit;
    
    public ComplexEventHandler(/* dependencies */) {
        // ...
    }
    
    @Override
    public void onEvent(NotificationEvent event) {
        // Lógica compleja aquí
    }
}
```

## 🔒 Thread Safety

El sistema es completamente thread-safe:

- `NotificationEventPublisher` usa `CopyOnWriteArrayList` para gestionar suscriptores
- Es seguro suscribirse/desuscribirse desde múltiples threads
- La publicación de eventos es thread-safe
- Múltiples threads pueden publicar eventos concurrentemente

## 🧪 Testing

### Ejemplo de Test con Suscriptor Mock

```java
@Test
void testEventPublishing() {
    // Arrange
    List<NotificationEvent> receivedEvents = new CopyOnWriteArrayList<>();
    NotificationSubscriber testSubscriber = receivedEvents::add;
    
    PinappNotifyConfig config = PinappNotifyConfig.builder()
        .addProvider(ChannelType.EMAIL, mockProvider)
        .addSubscriber(testSubscriber)
        .build();
    
    NotificationService service = new NotificationServiceImpl(config);
    
    // Act
    service.send(notification, ChannelType.EMAIL);
    
    // Assert
    assertThat(receivedEvents).isNotEmpty();
    assertThat(receivedEvents.get(0)).isInstanceOf(NotificationSentEvent.class);
}
```

### Verificar Tipos de Eventos

```java
@Test
void testRetryEventIsPublished() {
    AtomicInteger retryCount = new AtomicInteger(0);
    
    NotificationSubscriber retryCounter = event -> {
        if (event instanceof NotificationRetryEvent) {
            retryCount.incrementAndGet();
        }
    };
    
    // ... configurar con proveedor que falla
    
    service.send(failingNotification, ChannelType.EMAIL);
    
    assertThat(retryCount.get()).isGreaterThan(0);
}
```

## 📊 Casos de Uso

### 1. Observabilidad y Monitoreo
- Recopilar métricas de éxito/fallo
- Calcular tasas de entrega
- Identificar patrones de errores

### 2. Alerting
- Enviar alertas cuando fallan notificaciones críticas
- Notificar sobre tasas de fallo anormales
- Alertar sobre latencias altas

### 3. Auditoría
- Registrar todos los eventos para compliance
- Tracking completo del ciclo de vida
- Debugging de problemas en producción

### 4. Analytics
- Análisis de comportamiento de proveedores
- Optimización de políticas de reintentos
- Identificación de canales más efectivos

### 5. Integración con Herramientas Externas
- Datadog / New Relic para métricas
- ELK Stack para logging centralizado
- PagerDuty / OpsGenie para alertas
- OpenTelemetry para tracing distribuido

## 🔄 Flujo de Eventos

```
┌─────────────────────────────────────────────────────────────┐
│                  Envío de Notificación                      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │ Intento #1    │
                    └───────────────┘
                            │
                    ┌───────┴───────┐
                    │               │
                ✓ Éxito        ✗ Fallo
                    │               │
                    │               ▼
                    │    ┌──────────────────────┐
                    │    │ NotificationRetryEvent│
                    │    └──────────────────────┘
                    │               │
                    │               ▼
                    │       ┌───────────────┐
                    │       │ Intento #2    │
                    │       └───────────────┘
                    │               │
                    │       ┌───────┴───────┐
                    │       │               │
                    │   ✓ Éxito        ✗ Fallo
                    │       │               │
                    │       │               ▼
                    │       │    ┌──────────────────────┐
                    │       │    │ NotificationRetryEvent│
                    │       │    └──────────────────────┘
                    │       │               │
                    │       │               ▼
                    │       │      (más reintentos...)
                    │       │               │
                    ▼       ▼               ▼
        ┌────────────────────┐   ┌───────────────────────┐
        │NotificationSentEvent│   │NotificationFailedEvent│
        └────────────────────┘   └───────────────────────┘
```

## 📝 Notas Adicionales

- Los eventos se publican **después** de que la acción correspondiente ocurra
- Los suscriptores reciben eventos en el **mismo orden** en que fueron registrados
- Si un suscriptor falla, no afecta a otros suscriptores ni al flujo principal
- El sistema no mantiene historial de eventos; cada evento se notifica una sola vez

## 🔗 Ver También

- [ObservabilityExample.java](src/main/java/com/pinapp/notify/example/ObservabilityExample.java) - Ejemplo completo funcional
- [NotificationServiceImpl.java](src/main/java/com/pinapp/notify/core/NotificationServiceImpl.java) - Implementación de la integración
- [PinappNotifyConfig.java](src/main/java/com/pinapp/notify/config/PinappNotifyConfig.java) - Configuración con suscriptores
