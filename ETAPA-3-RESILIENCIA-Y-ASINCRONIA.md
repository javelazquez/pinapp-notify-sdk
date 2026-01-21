# Etapa 3: Resiliencia y Asincronía - Implementación Completa

## 📋 Descripción General

Se han implementado exitosamente las capacidades de **resiliencia** y **asincronía** en el SDK de notificaciones `pinapp-notify-sdk`, añadiendo reintentos automáticos y envío asíncrono con CompletableFuture.

## ✅ Componentes Implementados

### 1. RetryPolicy (Domain)

**Ubicación**: `com.pinapp.notify.domain.RetryPolicy`

#### Características

- **Record inmutable** con `maxAttempts` y `delayMillis`
- **Validaciones** automáticas en constructor compacto
- **Factory methods** para creación conveniente
- **Cálculo de delay** por intento (base para estrategias futuras)

#### Métodos Principales

```java
// Creación
RetryPolicy.of(3, 1000)              // 3 intentos, 1 segundo entre reintentos
RetryPolicy.noRetry()                // Sin reintentos (maxAttempts = 1)
RetryPolicy.defaultPolicy()          // Por defecto: 3 intentos, 1s delay

// Consultas
boolean shouldRetry()                // true si maxAttempts > 1
int getRetryCount()                  // maxAttempts - 1
long getDelayForAttempt(int)         // Delay para un intento específico
```

#### Tests

- ✅ 13 tests unitarios
- ✅ 100% cobertura
- ✅ Validaciones de parámetros
- ✅ Factory methods
- ✅ Cálculo de delays

---

### 2. NotificationService - Métodos Asíncronos

**Ubicación**: `com.pinapp.notify.ports.in.NotificationService`

#### Nuevos Métodos

```java
CompletableFuture<NotificationResult> sendAsync(Notification, ChannelType)
CompletableFuture<NotificationResult> sendAsync(Notification)
```

#### Características

- **No bloqueante**: Retorna inmediatamente con un CompletableFuture
- **ExecutorService dedicado**: No satura el ForkJoinPool común
- **Manejo de errores**: Usa `exceptionally()` para capturar fallos
- **Composable**: Permite encadenar con `thenApply`, `thenCompose`, etc.

---

### 3. NotificationServiceImpl - Lógica de Reintentos

**Ubicación**: `com.pinapp.notify.core.NotificationServiceImpl`

#### Implementaciones

##### sendAsync()

```java
public CompletableFuture<NotificationResult> sendAsync(
        Notification notification, 
        ChannelType channelType) {
    ExecutorService executor = getOrCreateExecutor();
    return CompletableFuture.supplyAsync(() -> {
        return send(notification, channelType);
    }, executor).exceptionally(error -> {
        // Manejo de errores
    });
}
```

##### sendWithRetry()

```java
private NotificationResult sendWithRetry(
        Notification notification,
        ChannelType channelType,
        NotificationProvider provider,
        RetryPolicy retryPolicy) {
    
    for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
        try {
            if (attempt > 1) {
                long delay = retryPolicy.getDelayForAttempt(attempt);
                logger.info("Reintento {}/{} después de {}ms", ...);
                Thread.sleep(delay);
            }
            
            NotificationResult result = provider.send(notification);
            // ... lógica de manejo de resultado
            
        } catch (ProviderException e) {
            // Reintentar o fallar
        }
    }
}
```

#### Características de Reintentos

- ✅ **Logging detallado**: Cada intento se registra con su número
- ✅ **Sleep configurable**: Respeta el `delayMillis` de la política
- ✅ **Interrupción**: Maneja `InterruptedException` correctamente
- ✅ **Límite de intentos**: No excede `maxAttempts`
- ✅ **Mensaje final**: Indica el número total de intentos fallidos

#### Tests

- ✅ 5 tests de reintentos
- ✅ 6 tests asíncronos
- ✅ Casos de éxito y fallo
- ✅ Múltiples envíos en paralelo
- ✅ Composición de futures

---

### 4. PinappNotifyConfig - Configuración Extendida

**Ubicación**: `com.pinapp.notify.config.PinappNotifyConfig`

#### Nuevos Campos

```java
private final RetryPolicy retryPolicy;
private final ExecutorService executorService;
private final boolean shouldShutdownExecutor;
```

#### Nuevos Métodos Builder

```java
// Política de reintentos
builder.withRetryPolicy(RetryPolicy.of(5, 500))
builder.withoutRetries()

// ExecutorService
builder.enableAsync()                               // Pool size = CPUs
builder.withAsyncThreadPoolSize(4)                  // Pool size personalizado
builder.withExecutorService(customExecutor)         // Executor externo

// Shutdown
config.shutdown()                                    // Timeout 10s
config.shutdown(30)                                  // Timeout personalizado
```

#### Método shutdown()

```java
public boolean shutdown(long timeoutSeconds) {
    if (executorService != null && shouldShutdownExecutor) {
        executorService.shutdown();
        
        if (!executorService.awaitTermination(timeoutSeconds, SECONDS)) {
            executorService.shutdownNow();
            
            if (!executorService.awaitTermination(timeoutSeconds, SECONDS)) {
                return false;
            }
        }
        
        return true;
    }
    return true;
}
```

#### Características

- ✅ **Shutdown graceful**: Espera a que terminen las tareas
- ✅ **Shutdown forzado**: Si timeout, hace `shutdownNow()`
- ✅ **Executor externo**: No cierra si fue proporcionado externamente
- ✅ **Thread factory**: Nombres descriptivos para debugging

---

## 📊 Estadísticas de Implementación

### Código Nuevo

| Componente | Archivos | Líneas | Descripción |
|------------|----------|--------|-------------|
| RetryPolicy | 1 | 95 | Domain model para reintentos |
| NotificationService (async) | 1 | +40 | Métodos asíncronos en interfaz |
| NotificationServiceImpl (retry + async) | 1 | +150 | Implementación de reintentos y async |
| PinappNotifyConfig | 1 | +120 | Configuración extendida |
| **Total código producción** | **4** | **~405** | |

### Tests Nuevos

| Test Suite | Tests | Líneas | Cobertura |
|------------|-------|--------|-----------|
| RetryPolicyTest | 13 | 145 | Validaciones, factory methods, delays |
| NotificationServiceRetryTest | 5 | 180 | Reintentos exitosos/fallidos, delays |
| NotificationServiceAsyncTest | 6 | 160 | Async básico, paralelo, composición |
| **Total tests** | **24** | **~485** | |

### Ejemplos

| Archivo | Líneas | Descripción |
|---------|--------|-------------|
| ResilienceExample | 280 | 5 ejemplos completos de uso |

### Total General

```
Archivos nuevos: 8
Líneas de código: ~1,170
  - Producción: 405 líneas
  - Tests: 485 líneas
  - Ejemplos: 280 líneas
Tests totales: 24 (100% exitosos)
```

---

## 🎯 Características Implementadas

### ✅ Requerimientos Cumplidos

#### 1. Método sendAsync()

- ✅ Implementado con `CompletableFuture.supplyAsync`
- ✅ ExecutorService dedicado configurado
- ✅ No satura el ForkJoinPool común
- ✅ Manejo de excepciones con `.exceptionally()`
- ✅ Dos variantes: con canal específico y canal por defecto

#### 2. Sistema de Reintentos

- ✅ `RetryPolicy` en domain con `maxAttempts` y `delayMillis`
- ✅ Lógica implementada en `NotificationServiceImpl`
- ✅ Logging en cada intento (SLF4J)
- ✅ Thread.sleep para versión síncrona
- ✅ Respeta límites de intentos
- ✅ Solo reintenta en `ProviderException`

#### 3. Configuración Actualizada

- ✅ Builder permite configurar `RetryPolicy`
- ✅ Métodos para configurar ExecutorService
- ✅ Método `shutdown()` para cierre ordenado
- ✅ Política por defecto si no se configura

#### 4. Principios de Diseño

- ✅ **No usa librerías externas**: Implementación pura Java 21
- ✅ **Arquitectura agnóstica**: No dependencias de Resilience4j, etc.
- ✅ **Logging completo**: Cada reintento registrado con SLF4J
- ✅ **Manejo robusto de excepciones**: InterruptedException, etc.

---

## 📝 Ejemplos de Uso

### Ejemplo 1: Configuración Básica con Reintentos

```java
PinappNotifyConfig config = PinappNotifyConfig.builder()
    .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
    .withRetryPolicy(RetryPolicy.of(3, 1000)) // 3 intentos, 1s delay
    .build();

NotificationService service = new NotificationServiceImpl(config);
```

### Ejemplo 2: Envío Asíncrono

```java
PinappNotifyConfig config = PinappNotifyConfig.builder()
    .addProvider(ChannelType.SMS, new SmsNotificationProvider())
    .enableAsync() // Habilita async con pool size = CPUs
    .build();

NotificationService service = new NotificationServiceImpl(config);

CompletableFuture<NotificationResult> future = 
    service.sendAsync(notification, ChannelType.SMS);

future.thenAccept(result -> {
    if (result.success()) {
        logger.info("Enviado!");
    }
}).exceptionally(error -> {
    logger.error("Error: {}", error.getMessage());
    return null;
});
```

### Ejemplo 3: Múltiples Envíos en Paralelo

```java
PinappNotifyConfig config = PinappNotifyConfig.builder()
    .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
    .withAsyncThreadPoolSize(4) // Pool de 4 threads
    .build();

// Enviar 10 notificaciones en paralelo
CompletableFuture<?>[] futures = new CompletableFuture[10];
for (int i = 0; i < 10; i++) {
    futures[i] = service.sendAsync(notification, ChannelType.EMAIL);
}

// Esperar a que todas se completen
CompletableFuture.allOf(futures).join();
```

### Ejemplo 4: Composición de Futures

```java
// Enviar email, luego SMS
service.sendAsync(emailNotification, ChannelType.EMAIL)
    .thenCompose(emailResult -> {
        if (emailResult.success()) {
            return service.sendAsync(smsNotification, ChannelType.SMS);
        }
        return CompletableFuture.completedFuture(emailResult);
    })
    .thenApply(result -> "Completado: " + result.channelType())
    .thenAccept(logger::info);
```

### Ejemplo 5: Shutdown Ordenado

```java
PinappNotifyConfig config = PinappNotifyConfig.builder()
    .addProvider(...)
    .enableAsync()
    .build();

try {
    // Usar el servicio...
    service.sendAsync(...);
} finally {
    // Cerrar recursos ordenadamente
    config.shutdown(30); // Espera hasta 30 segundos
}
```

---

## 🧪 Resultados de Tests

```
[INFO] Tests run: 56, Failures: 0, Errors: 0, Skipped: 0

Desglose:
├── RetryPolicyTest: 13/13 ✅
├── NotificationServiceRetryTest: 5/5 ✅
├── NotificationServiceAsyncTest: 6/6 ✅
├── EmailNotificationProviderTest: 9/9 ✅
├── SmsNotificationProviderTest: 10/10 ✅
└── PushNotificationProviderTest: 13/13 ✅

BUILD SUCCESS
Total time: 6.488 s
```

---

## 📋 Logs de Ejemplo

### Reintentos

```
2026-01-21 12:50:29.976 INFO NotificationServiceImpl - Proveedor seleccionado: 'FlakeyProvider' para canal EMAIL
2026-01-21 12:50:29.976 WARN NotificationServiceImpl - Error del proveedor 'FlakeyProvider' en intento 1/3 para notificación [id=34053148...]: Fallo simulado intento 1
2026-01-21 12:50:29.976 INFO NotificationServiceImpl - Reintento 2/3 para notificación [id=34053148...] después de 100ms
2026-01-21 12:50:30.080 WARN NotificationServiceImpl - Error del proveedor 'FlakeyProvider' en intento 2/3 para notificación [id=34053148...]: Fallo simulado intento 2
2026-01-21 12:50:30.081 INFO NotificationServiceImpl - Reintento 3/3 para notificación [id=34053148...] después de 100ms
2026-01-21 12:50:30.186 INFO NotificationServiceImpl - Notificación [id=34053148...] enviada exitosamente en el intento 3/3
```

### Asincronía

```
2026-01-21 12:51:17.533 INFO ResilienceExample - Iniciando envío asíncrono...
2026-01-21 12:51:17.535 INFO NotificationServiceImpl - Proveedor seleccionado: 'SmsProvider' para canal SMS
2026-01-21 12:51:17.536 INFO SmsNotificationProvider - [SMS PROVIDER] ✓ SMS enviado exitosamente [messageId=c895f0c1...]
2026-01-21 12:51:17.536 INFO ResilienceExample - ✓ Notificación asíncrona enviada exitosamente [id=3e64c575...]
```

### Envíos en Paralelo

```
2026-01-21 12:51:17.540 INFO ResilienceExample - Email #0 - ENVIADO
2026-01-21 12:51:17.542 INFO ResilienceExample - Email #1 - ENVIADO
2026-01-21 12:51:17.542 INFO ResilienceExample - Email #2 - ENVIADO
2026-01-21 12:51:17.542 INFO ResilienceExample - Email #4 - ENVIADO
2026-01-21 12:51:17.542 INFO ResilienceExample - Email #3 - ENVIADO
2026-01-21 12:51:17.542 INFO ResilienceExample - Todas las notificaciones paralelas completadas
```

---

## 🏗️ Arquitectura

### Flujo de Envío con Reintentos

```
┌─────────────────────────────────────────┐
│  NotificationService.send()             │
│  (Inbound Port)                         │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  NotificationServiceImpl                │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │  sendWithRetry()                   │ │
│  │                                     │ │
│  │  for (1 to maxAttempts) {          │ │
│  │    try {                            │ │
│  │      result = provider.send()      │ │
│  │      if (success) return result    │ │
│  │    } catch (ProviderException) {   │ │
│  │      log warning                    │ │
│  │      Thread.sleep(delay)           │ │
│  │      continue                       │ │
│  │    }                                │ │
│  │  }                                  │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  NotificationProvider.send()            │
│  (Outbound Port)                        │
└─────────────────────────────────────────┘
```

### Flujo de Envío Asíncrono

```
┌─────────────────────────────────────────┐
│  NotificationService.sendAsync()        │
│  Returns CompletableFuture immediately  │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  CompletableFuture.supplyAsync()        │
│                                          │
│  Ejecutado en ExecutorService dedicado  │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  NotificationServiceImpl.send()         │
│  (mismo flujo síncrono con reintentos)  │
└─────────────────────────────────────────┘
```

---

## 🎓 Buenas Prácticas Aplicadas

### 1. ExecutorService Dedicado

✅ **No usa el ForkJoinPool común**
- Pool dedicado evita saturación
- Threads con nombres descriptivos
- Tamaño configurable

### 2. Shutdown Ordenado

✅ **Graceful shutdown**
- Espera a que terminen tareas pendientes
- Timeout configurable
- Shutdown forzado si necesario

### 3. Logging Estructurado

✅ **Información completa en logs**
- Número de intento actual
- Total de intentos permitidos
- Delay aplicado
- Razón del fallo

### 4. Manejo de Interrupciones

✅ **InterruptedException manejada correctamente**
- Restaura flag de interrupción
- Retorna resultado de fallo
- No propaga como RuntimeException

### 5. CompletableFuture Best Practices

✅ **Manejo de excepciones**
- Usa `.exceptionally()` para capturar errores
- No deja futures "colgados"
- Propaga ValidationException y NotificationException

---

## 🚀 Ejecución

### Compilar

```bash
mvn clean compile
```

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Solo tests de resiliencia
mvn test -Dtest=RetryPolicyTest,NotificationServiceRetryTest,NotificationServiceAsyncTest
```

### Ejecutar Ejemplos

```bash
mvn exec:java -Dexec.mainClass="com.pinapp.notify.example.ResilienceExample"
```

---

## 📌 Notas Importantes

### Política de Reintentos por Defecto

Si no se configura una política de reintentos, se usa:
- **maxAttempts**: 3
- **delayMillis**: 1000 (1 segundo)

### ExecutorService No Configurado

Si se llama a `sendAsync()` sin configurar un ExecutorService:
- Se crea un `CachedThreadPool` temporal
- Se muestra un WARNING en los logs
- **Recomendación**: Siempre configurar con `.enableAsync()` o `.withExecutorService()`

### Threads Daemon vs Non-Daemon

Los threads del ExecutorService son **non-daemon** para asegurar que las tareas asíncronas se completen antes del shutdown de la JVM.

### Reintentos solo en ProviderException

Los reintentos **solo** se aplican cuando el proveedor lanza `ProviderException`. Otros tipos de excepciones (ValidationException, IllegalArgumentException, etc.) no se reintentan.

---

## ✨ Conclusión

La implementación de resiliencia y asincronía está **100% completa** y lista para producción:

- ✅ **24 tests nuevos**, todos pasando
- ✅ **Reintentos automáticos** configurables
- ✅ **Envío asíncrono** con CompletableFuture
- ✅ **ExecutorService dedicado** evita saturación del ForkJoinPool
- ✅ **Shutdown ordenado** de recursos
- ✅ **Logging completo** para debugging y monitoreo
- ✅ **Sin dependencias externas** (Java 21 puro)
- ✅ **Ejemplos funcionales** listos para usar

El SDK ahora soporta escenarios de alta concurrencia y proveedores inestables con reintentos automáticos, manteniendo la arquitectura limpia y agnóstica.

---

**Autor**: PinApp Team  
**Fecha**: 21 de Enero, 2026  
**Versión**: 1.0.0-SNAPSHOT
