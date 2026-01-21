# Outbound Adapters - Notification Providers

## Descripción

Los **Outbound Adapters** son implementaciones de la interfaz `NotificationProvider` que representan los adaptadores externos responsables de enviar notificaciones a través de diferentes canales (EMAIL, SMS, PUSH, etc.).

Estos adaptadores forman parte de la capa externa en la **Arquitectura Hexagonal**, permitiendo que el núcleo de la aplicación sea independiente de los detalles de implementación de los servicios externos.

## Providers Implementados

### 1. EmailNotificationProvider

Proveedor para el envío de notificaciones por correo electrónico.

#### Características

- **Canal soportado**: `ChannelType.EMAIL`
- **Validaciones**:
  - El destinatario debe tener un email válido (`Recipient.email()`)
  - La notificación debe incluir un `subject` en los metadatos del destinatario
- **Configuración**: Acepta una API Key opcional en el constructor

#### Ejemplo de Uso

```java
// Configurar el provider
EmailNotificationProvider provider = new EmailNotificationProvider("email-api-key-12345");

// Crear destinatario con email y subject
Recipient recipient = new Recipient(
    "usuario@example.com",
    null,
    Map.of("subject", "Bienvenido a PinApp")
);

// Crear notificación
Notification notification = Notification.create(
    recipient,
    "Hola, gracias por registrarte en nuestra plataforma.",
    NotificationPriority.HIGH
);

// Enviar
NotificationResult result = provider.send(notification);
```

#### Metadatos Requeridos

| Clave | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `subject` | String | Sí | Asunto del correo electrónico |

#### Logs Generados

```
[EMAIL PROVIDER] Sending to: usuario@example.com | Subject: Bienvenido a PinApp | Body: Hola, gracias... | MessageId: 47ccf359-...
```

---

### 2. SmsNotificationProvider

Proveedor para el envío de notificaciones por SMS.

#### Características

- **Canal soportado**: `ChannelType.SMS`
- **Validaciones**:
  - El destinatario debe tener un número de teléfono válido (`Recipient.phone()`)
- **Configuración**: 
  - API Key (opcional)
  - SenderId / Nombre del remitente (opcional, default: "PinApp")

#### Ejemplo de Uso

```java
// Configurar el provider
SmsNotificationProvider provider = new SmsNotificationProvider(
    "sms-api-key-67890",
    "PinApp"
);

// Crear destinatario con teléfono
Recipient recipient = new Recipient(
    null,
    "+56912345678",
    Map.of("userId", "user-123")
);

// Crear notificación
Notification notification = Notification.create(
    recipient,
    "Tu código de verificación es: 123456. Válido por 5 minutos.",
    NotificationPriority.CRITICAL
);

// Enviar
NotificationResult result = provider.send(notification);
```

#### Logs Generados

```
[SMS PROVIDER] Sending to: +56912345678 | From: PinApp | Message: Tu código de verificación... | MessageId: 5573fc46-...
```

---

### 3. PushNotificationProvider

Proveedor para el envío de notificaciones push a dispositivos móviles.

#### Características

- **Canal soportado**: `ChannelType.PUSH`
- **Validaciones**:
  - El destinatario debe tener un `deviceToken` en sus metadatos
- **Configuración**: 
  - Server Key (opcional, ej. FCM Server Key)
  - Application ID (opcional, default: "com.pinapp.default")

#### Ejemplo de Uso

```java
// Configurar el provider
PushNotificationProvider provider = new PushNotificationProvider(
    "push-server-key-abcdef",
    "com.pinapp.mobile"
);

// Crear destinatario con deviceToken y metadatos adicionales
Recipient recipient = new Recipient(
    null,
    null,
    Map.of(
        "deviceToken", "fcm-token-1234567890abcdef1234567890abcdef12345678",
        "title", "Nueva actualización disponible",
        "badge", "3",
        "sound", "notification.wav"
    )
);

// Crear notificación
Notification notification = Notification.create(
    recipient,
    "Hay una nueva versión de la app disponible.",
    NotificationPriority.NORMAL
);

// Enviar
NotificationResult result = provider.send(notification);
```

#### Metadatos Requeridos y Opcionales

| Clave | Tipo | Requerido | Default | Descripción |
|-------|------|-----------|---------|-------------|
| `deviceToken` | String | Sí | - | Token del dispositivo (FCM, APNS, etc.) |
| `title` | String | No | "Notificación" | Título de la notificación push |
| `badge` | String | No | "1" | Número de badge a mostrar |
| `sound` | String | No | "default" | Sonido a reproducir |

#### Logs Generados

```
[PUSH PROVIDER] Sending to device: fcm-toke...5678 | App: com.pinapp.mobile | Title: Nueva actualización disponible | Message: Hay una nueva... | MessageId: 25cdb31a-...
```

---

## Estructura de Directorios

```
src/main/java/com/pinapp/notify/
├── providers/
│   └── impl/
│       ├── EmailNotificationProvider.java
│       ├── SmsNotificationProvider.java
│       └── PushNotificationProvider.java
```

## Manejo de Errores

Todos los providers lanzan `ProviderException` cuando:

- Falta información obligatoria del destinatario (email, phone, deviceToken)
- Faltan metadatos requeridos (subject para email, deviceToken para push)
- Cualquier error durante el procesamiento del envío

### Ejemplo de Manejo de Errores

```java
try {
    NotificationResult result = provider.send(notification);
    if (result.success()) {
        logger.info("Notificación enviada exitosamente");
    }
} catch (ProviderException e) {
    logger.error("Error al enviar notificación: {}", e.getMessage());
    // Manejar el error según la lógica de negocio
}
```

## Logging

Todos los providers utilizan **SLF4J** para logging estructurado:

- **DEBUG**: Inicialización del provider, configuración
- **INFO**: Procesamiento y envío de notificaciones
- **ERROR**: Errores de validación o envío

## Testing

Cada provider cuenta con tests unitarios exhaustivos:

- `EmailNotificationProviderTest`: 9 tests
- `SmsNotificationProviderTest`: 10 tests
- `PushNotificationProviderTest`: 13 tests

### Ejecutar Tests

```bash
mvn test -Dtest=EmailNotificationProviderTest,SmsNotificationProviderTest,PushNotificationProviderTest
```

## Extensibilidad

Para crear un nuevo provider:

1. Implementar la interfaz `NotificationProvider`
2. Sobreescribir `supports(ChannelType)` para retornar `true` solo para el canal correspondiente
3. Implementar `send(Notification)` con la lógica de envío
4. Implementar `getName()` para retornar el nombre del provider
5. Validar datos obligatorios y lanzar `ProviderException` si faltan
6. Usar SLF4J para logging estructurado
7. Retornar `NotificationResult.success()` o `NotificationResult.failure()`

## Ejemplo Completo

Ver el archivo `ProvidersExample.java` para ejemplos completos de uso:

```bash
mvn exec:java -Dexec.mainClass="com.pinapp.notify.example.ProvidersExample"
```

## Arquitectura Hexagonal

Estos providers representan los **Adaptadores de Salida (Outbound Adapters)** en la Arquitectura Hexagonal:

```
┌─────────────────────────────────────────────┐
│           Application Core                  │
│  ┌─────────────────────────────────────┐   │
│  │  NotificationService (Domain)       │   │
│  └─────────────────────────────────────┘   │
│                    │                        │
│                    ▼                        │
│  ┌─────────────────────────────────────┐   │
│  │  NotificationProvider (Port)        │◄──┼─── Dependency Inversion
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│        Outbound Adapters                    │
│  ┌──────────────────────────────────────┐  │
│  │  EmailNotificationProvider           │  │
│  ├──────────────────────────────────────┤  │
│  │  SmsNotificationProvider             │  │
│  ├──────────────────────────────────────┤  │
│  │  PushNotificationProvider            │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

## Notas Importantes

- **Simulación**: Estos providers NO realizan conexiones HTTP reales, solo simulan el envío mediante logs.
- **Producción**: En un entorno real, deberías integrar con servicios reales como:
  - SendGrid, AWS SES, o SMTP para emails
  - Twilio, AWS SNS, o similar para SMS
  - Firebase Cloud Messaging (FCM), Apple Push Notification Service (APNS) para push notifications
- **Configuración**: Las API Keys y configuraciones se pasan en el constructor, pero en producción deberían venir de archivos de configuración o variables de entorno.

## Autor

PinApp Team - 2026
