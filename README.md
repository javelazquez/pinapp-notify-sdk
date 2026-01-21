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
import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.vo.ChannelType;

public class Main {
    public static void main(String[] args) {
        // 1. Configure the SDK using the fluent Builder
        var config = PinappNotifyConfig.builder()
                .withEmailProvider(new SendGridAdapter()) // Register your concrete adapter
                .enableAsync()                            // Enable non-blocking retries & async sending
                .build();

        var service = config.getNotificationService();

        // 2. Build the notification domain object
        var email = Notification.builder()
                .to("developer@pinapp.com")
                .subject("DX is everything! 🚀")
                .content("Welcome to the new standard of notifications.")
                .build();

        // 3. Send synchronously or asynchronously
        var result = service.send(email, ChannelType.EMAIL);
        
        System.out.println("Status: " + (result.success() ? "✅ Sent" : "❌ Failed"));
    }
}
```

---

## ⚙️ Advanced Configuration
The `PinappNotifyConfig.Builder` provides a rich set of options to tailor the SDK to your needs:

| Method | Description | Default |
| :--- | :--- | :--- |
| `withEmailProvider(Provider)` | Registers an email adapter | None |
| `withSmsProvider(Provider)` | Registers an SMS adapter | None |
| `withRetryPolicy(RetryPolicy)` | Sets a custom retry strategy | 3 attempts, exponential backoff |
| `enableAsync()` | Enables asynchronous execution support | Disabled |
| `withExecutorService(Executor)` | Custom thread pool for async tasks | Cached Thread Pool |
| `withTemplateEngine(Engine)` | Injects custom template processing | Simple replacement |

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
