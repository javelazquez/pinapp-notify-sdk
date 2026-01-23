# 🚀 PinApp Notify SDK

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Build Status](https://img.shields.io/badge/Build-Success-brightgreen.svg)](https://github.com/pinapp/pinapp-notify-sdk)

**PinApp Notify SDK** is a professional, framework-agnostic Java library designed to handle notifications (Email, SMS, Push, Slack) with a focus on **Hexagonal Architecture**, **Developer Experience (DX)**, and **Resilience**.

---

## 🚀 Introduction
This library is built using **Hexagonal Architecture (Ports & Adapters)** principles. It provides a clean, framework-agnostic core that can be easily integrated into any Java 21 environment. By decoupling the notification logic from the underlying infrastructure providers, the SDK ensures long-term maintainability and high extensibility.

## 📦 Installation
To publish the SDK to your local Maven repository (`~/.m2/repository`):

```bash
mvn clean install
```

## 🔧 Integration
Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.pinapp</groupId>
    <artifactId>pinapp-notify-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 🚀 Quick Start
Send your first notification in seconds. The API is designed for modern Java usage:

```java
import com.pinapp.notify.adapters.mock.MockNotificationProvider;
import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.NotificationServiceImpl;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.in.NotificationService;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // 1. Configure the SDK with providers
        PinappNotifyConfig config = PinappNotifyConfig.builder()
                .addProvider(ChannelType.EMAIL, MockNotificationProvider.forEmail())
                .addProvider(ChannelType.SMS, MockNotificationProvider.forSms())
                .build();

        // 2. Create the notification service
        NotificationService service = new NotificationServiceImpl(config);

        // 3. Create and send a notification
        Recipient recipient = new Recipient(
                "usuario@example.com",
                null,
                Map.of()
        );

        Notification notification = Notification.create(
                recipient,
                "Hola! Este es un mensaje de prueba"
        );

        // 4. Send synchronously
        var result = service.send(notification, ChannelType.EMAIL);
        
        System.out.println("Status: " + (result.success() ? "✅ Sent" : "❌ Failed"));
        System.out.println("ID: " + result.notificationId());
    }
}
```

---

## 📚 Examples

### Basic Usage - Multiple Channels

```java
import com.pinapp.notify.adapters.mock.MockNotificationProvider;
import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.NotificationServiceImpl;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationPriority;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.in.NotificationService;
import java.util.Map;

// Configure with multiple providers
PinappNotifyConfig config = PinappNotifyConfig.builder()
        .addProvider(ChannelType.EMAIL, MockNotificationProvider.forEmail())
        .addProvider(ChannelType.SMS, MockNotificationProvider.forSms())
        .addProvider(ChannelType.PUSH, MockNotificationProvider.forPush())
        .addProvider(ChannelType.SLACK, MockNotificationProvider.forSlack())
        .build();

NotificationService service = new NotificationServiceImpl(config);

// Send EMAIL
Recipient emailRecipient = new Recipient("usuario@example.com", null, Map.of());
Notification emailNotification = Notification.create(
        emailRecipient,
        "Mensaje por email",
        NotificationPriority.HIGH
);
service.send(emailNotification, ChannelType.EMAIL);

// Send SMS
Recipient smsRecipient = new Recipient(null, "+56912345678", Map.of());
Notification smsNotification = Notification.create(smsRecipient, "Mensaje por SMS");
service.send(smsNotification, ChannelType.SMS);

// Send PUSH
Recipient pushRecipient = new Recipient(
        null, null,
        Map.of("deviceToken", "abc123xyz789")
);
Notification pushNotification = Notification.create(
        pushRecipient,
        "Nueva actualización disponible!",
        NotificationPriority.CRITICAL
);
service.send(pushNotification, ChannelType.PUSH);

// Send SLACK
Recipient slackRecipient = new Recipient(
        null, null,
        Map.of("slackChannelId", "#general")
);
Notification slackNotification = Notification.create(
        slackRecipient,
        "Deployment completado exitosamente! :rocket:"
);
service.send(slackNotification, ChannelType.SLACK);

// Auto-select channel based on recipient
Recipient autoRecipient = new Recipient("auto@example.com", "+56987654321", Map.of());
Notification autoNotification = Notification.create(autoRecipient, "Canal automático");
service.send(autoNotification); // Will use EMAIL (preferred)
```

### Using Real Providers

```java
import com.pinapp.notify.adapters.email.EmailNotificationProvider;
import com.pinapp.notify.adapters.sms.SmsNotificationProvider;
import com.pinapp.notify.adapters.push.PushNotificationProvider;

// Email Provider
EmailNotificationProvider emailProvider = new EmailNotificationProvider("your-email-api-key");
Recipient emailRecipient = new Recipient(
        "usuario@example.com",
        null,
        Map.of("subject", "Bienvenido a PinApp")
);
Notification email = Notification.create(
        emailRecipient,
        "Hola, gracias por registrarte!",
        NotificationPriority.HIGH
);
emailProvider.send(email);

// SMS Provider
SmsNotificationProvider smsProvider = new SmsNotificationProvider(
        "your-sms-api-key",
        "PinApp" // Sender ID
);
Recipient smsRecipient = new Recipient(
        null,
        "+56912345678",
        Map.of("userId", "user-123")
);
Notification sms = Notification.create(
        smsRecipient,
        "Tu código de verificación es: 123456",
        NotificationPriority.CRITICAL
);
smsProvider.send(sms);

// Push Provider
PushNotificationProvider pushProvider = new PushNotificationProvider(
        "your-push-server-key",
        "com.pinapp.mobile" // Application ID
);
Recipient pushRecipient = new Recipient(
        null, null,
        Map.of(
                "deviceToken", "fcm-token-1234567890...",
                "title", "Nueva actualización disponible",
                "badge", "3",
                "sound", "notification.wav"
        )
);
Notification push = Notification.create(
        pushRecipient,
        "Hay una nueva versión disponible!",
        NotificationPriority.NORMAL
);
pushProvider.send(push);
```

### Resilience & Retry Policy

```java
import com.pinapp.notify.domain.RetryPolicy;

// Configure with custom retry policy
PinappNotifyConfig config = PinappNotifyConfig.builder()
        .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
        .withRetryPolicy(RetryPolicy.of(5, 500)) // 5 attempts, 500ms between retries
        .build();

NotificationService service = new NotificationServiceImpl(config);

// Send with automatic retries
Notification notification = Notification.create(
        new Recipient("usuario@example.com", null, Map.of("subject", "Test")),
        "Este email será enviado con hasta 5 intentos si falla",
        NotificationPriority.HIGH
);

NotificationResult result = service.send(notification, ChannelType.EMAIL);
```

### Asynchronous Sending

```java
import java.util.concurrent.CompletableFuture;

// Enable async support
PinappNotifyConfig config = PinappNotifyConfig.builder()
        .addProvider(ChannelType.SMS, new SmsNotificationProvider())
        .enableAsync() // Enable asynchronous sending
        .build();

NotificationService service = new NotificationServiceImpl(config);

// Send asynchronously
Notification notification = Notification.create(
        new Recipient(null, "+56912345678", Map.of()),
        "SMS enviado de forma asíncrona"
);

CompletableFuture<NotificationResult> future = service.sendAsync(notification, ChannelType.SMS);

// Process result with callbacks
future
        .thenAccept(result -> {
            if (result.success()) {
                System.out.println("✓ Notificación enviada: " + result.notificationId());
            } else {
                System.err.println("✗ Error: " + result.errorMessage());
            }
        })
        .exceptionally(error -> {
            System.err.println("Error: " + error.getMessage());
            return null;
        });

// Or wait for completion
NotificationResult result = future.join();

// Don't forget to shutdown
config.shutdown();
```

### Parallel Async Sending

```java
// Configure with custom thread pool
PinappNotifyConfig config = PinappNotifyConfig.builder()
        .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
        .withAsyncThreadPoolSize(4) // Pool of 4 threads
        .build();

NotificationService service = new NotificationServiceImpl(config);

// Send multiple notifications in parallel
CompletableFuture<?>[] futures = new CompletableFuture[5];

for (int i = 0; i < 5; i++) {
    Notification notification = Notification.create(
            new Recipient("usuario" + i + "@example.com", null, Map.of("subject", "Email #" + i)),
            "Mensaje número " + i
    );
    
    futures[i] = service.sendAsync(notification, ChannelType.EMAIL)
            .thenAccept(result -> {
                System.out.println("Email #" + i + " - " + 
                        (result.success() ? "ENVIADO" : "FALLIDO"));
            });
}

// Wait for all to complete
CompletableFuture.allOf(futures).join();
config.shutdown();
```

### Custom ExecutorService

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Create custom ExecutorService
ExecutorService customExecutor = Executors.newFixedThreadPool(
        2,
        r -> {
            Thread t = new Thread(r, "custom-notifier-" + System.nanoTime());
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        }
);

PinappNotifyConfig config = PinappNotifyConfig.builder()
        .addProvider(ChannelType.EMAIL, new EmailNotificationProvider())
        .withExecutorService(customExecutor)
        .withRetryPolicy(RetryPolicy.of(2, 100))
        .build();

NotificationService service = new NotificationServiceImpl(config);

// Use the service...
// Note: You must shutdown the executor manually if provided externally
customExecutor.shutdown();
```

### Future Composition

```java
// Send email and then SMS sequentially
service.sendAsync(
        Notification.create(
                new Recipient("user@example.com", null, Map.of("subject", "Confirmación")),
                "Email de confirmación"
        ),
        ChannelType.EMAIL
)
.thenCompose(emailResult -> {
    if (emailResult.success()) {
        System.out.println("Email enviado, ahora enviando SMS...");
        return service.sendAsync(
                Notification.create(
                        new Recipient(null, "+56987654321", Map.of()),
                        "Código: 123456"
                ),
                ChannelType.SMS
        );
    } else {
        System.out.println("Email falló, no se enviará SMS");
        return CompletableFuture.completedFuture(emailResult);
    }
})
.thenApply(finalResult -> {
    String status = finalResult.success() ? "COMPLETADO" : "FALLIDO";
    return String.format("Proceso: %s - Canal: %s", status, finalResult.channelType());
})
.thenAccept(System.out::println)
.join();
```

### Observability & Event Subscribers

```java
import com.pinapp.notify.core.events.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

// Create a metrics collector
class MetricsCollector implements NotificationSubscriber {
    private final AtomicInteger totalSent = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);
    
    @Override
    public void onEvent(NotificationEvent event) {
        switch (event) {
            case NotificationSentEvent sent -> {
                totalSent.incrementAndGet();
                System.out.println("✓ Sent: " + sent.notificationId());
            }
            case NotificationFailedEvent failed -> {
                totalFailed.incrementAndGet();
                System.err.println("✗ Failed: " + failed.errorMessage());
            }
            case NotificationRetryEvent retry -> {
                System.out.println("🔄 Retry: " + retry.attemptNumber() + "/" + retry.maxAttempts());
            }
        }
    }
    
    public int getTotalSent() { return totalSent.get(); }
    public int getTotalFailed() { return totalFailed.get(); }
}

// Register subscribers
MetricsCollector metrics = new MetricsCollector();

NotificationSubscriber logger = event -> {
    switch (event) {
        case NotificationSentEvent sent -> 
            System.out.println("[SUCCESS] " + sent.notificationId() + " via " + sent.channel());
        case NotificationFailedEvent failed -> 
            System.err.println("[FAILURE] " + failed.errorMessage());
        case NotificationRetryEvent retry -> 
            System.out.println("[RETRY] " + retry.attemptNumber() + "/" + retry.maxAttempts());
    }
};

PinappNotifyConfig config = PinappNotifyConfig.builder()
        .addProvider(ChannelType.EMAIL, MockNotificationProvider.forEmail())
        .withRetryPolicy(RetryPolicy.of(3, 100))
        .addSubscriber(metrics)
        .addSubscriber(logger)
        .build();

NotificationService service = new NotificationServiceImpl(config);

// Send notifications - events will be published automatically
for (int i = 0; i < 5; i++) {
    Notification notification = Notification.create(
            new Recipient("user" + i + "@example.com", null, Map.of()),
            "Prueba #" + i
    );
    service.send(notification, ChannelType.EMAIL);
}

System.out.println("Total sent: " + metrics.getTotalSent());
System.out.println("Total failed: " + metrics.getTotalFailed());
```

### Templates & Variables

```java
// Simple template
Map<String, String> variables = Map.of(
        "nombre", "Juan Pérez",
        "codigo", "ABC-123"
);

Notification notification = Notification.create(
        new Recipient("usuario@example.com", null, Map.of()),
        "Hola {{nombre}}, tu código de verificación es: {{codigo}}",
        NotificationPriority.HIGH,
        variables
);

service.send(notification, ChannelType.EMAIL);
// Result: "Hola Juan Pérez, tu código de verificación es: ABC-123"

// Complex template
String template = """
        Estimado {{nombre}},
        
        Tu pedido #{{orden}} ha sido procesado.
        Total: {{moneda}}{{monto}}
        Fecha estimada de entrega: {{fecha}}
        
        Gracias por tu compra.
        """;

Map<String, String> complexVars = Map.of(
        "nombre", "María García",
        "orden", "ORD-2024-001",
        "moneda", "$",
        "monto", "1,250.00",
        "fecha", "25 de Enero, 2026"
);

Notification complexNotification = Notification.create(
        new Recipient("cliente@empresa.com", null, Map.of()),
        template,
        NotificationPriority.NORMAL,
        complexVars
);

service.send(complexNotification, ChannelType.EMAIL);
```

### Validation

```java
import com.pinapp.notify.exception.ValidationException;

try {
    // Invalid email - will throw ValidationException
    Recipient invalidEmail = new Recipient("email-invalido", null, Map.of());
    Notification notification = Notification.create(invalidEmail, "Mensaje");
    service.send(notification, ChannelType.EMAIL);
} catch (ValidationException e) {
    System.err.println("Validation error: " + e.getMessage());
}

try {
    // Invalid phone - will throw ValidationException
    Recipient invalidPhone = new Recipient(null, "123", Map.of()); // Too short
    Notification notification = Notification.create(invalidPhone, "Mensaje");
    service.send(notification, ChannelType.SMS);
} catch (ValidationException e) {
    System.err.println("Validation error: " + e.getMessage());
}
```

---

## ⚙️ Advanced Configuration
The `PinappNotifyConfig.Builder` provides a rich set of options to tailor the SDK to your needs:

| Method | Description | Default |
| :--- | :--- | :--- |
| `addProvider(ChannelType, Provider)` | Registers a provider for a specific channel | None |
| `addProvider(Provider)` | Auto-registers provider for all supported channels | None |
| `withEmailProvider(Provider)` | Registers an email provider | None |
| `withSmsProvider(Provider)` | Registers an SMS provider | None |
| `withPushProvider(Provider)` | Registers a push notification provider | None |
| `withSlackProvider(Provider)` | Registers a Slack provider | None |
| `withRetryPolicy(RetryPolicy)` | Sets a custom retry strategy | 3 attempts, 1s delay |
| `withoutRetries()` | Disables retries completely | Enabled (3 attempts) |
| `enableAsync()` | Enables asynchronous execution support | Disabled |
| `withAsyncThreadPoolSize(int)` | Sets thread pool size for async operations | CPU cores |
| `withExecutorService(ExecutorService)` | Custom ExecutorService for async tasks | Auto-created if async enabled |
| `addSubscriber(NotificationSubscriber)` | Registers an event subscriber for observability | None |

---

## 🏗 Architecture
The SDK follows the **Hexagonal Architecture** pattern to ensure maximum decoupling:

*   **⚓ Inbound Ports (`ports.in`)**: The contract defining what the core application can do (e.g., `NotificationService`).
*   **🔌 Outbound Ports (`ports.out`)**: Contracts for communication with external worlds, like third-party providers (`NotificationProvider`).
*   **🧩 Core Domain**: Contains the business logic, template processing, validation, and domain models (`Notification`, `Recipient`, etc.).

---

## 🧪 Tests
To verify the installation and run all unit tests:

```bash
mvn test
```

The test suite covers:
- ✅ Synchronous/Asynchronous message flows.
- ✅ Non-blocking wait-and-retry logic.
- ✅ Event-driven observability (Sent/Failed/Retry events).
- ✅ Recipient and payload validation.

---
Developed with ❤️ by **PinApp Staff Engineers**.
