# PinApp Notify SDK

SDK de notificaciones multicanal para PinApp, diseñado con Arquitectura Hexagonal y siguiendo principios SOLID.

## 📋 Características

- ✅ **Multicanal**: Soporte para EMAIL, SMS, PUSH y SLACK
- ✅ **Configuración Fluida**: API Builder para configuración mediante código Java puro
- ✅ **Arquitectura Hexagonal**: Separación clara entre dominio, puertos y adaptadores
- ✅ **Type-Safe**: Uso extensivo de Records y Enums para seguridad de tipos
- ✅ **Logging Integrado**: Trazabilidad completa con SLF4J
- ✅ **Sin Dependencias de Framework**: Código agnóstico a Spring, Quarkus, etc.
- ✅ **Manejo Robusto de Errores**: Excepciones específicas y resultados detallados

## 🚀 Quick Start

### 1. Configurar el SDK

```java
// Importar las clases necesarias
import com.pinapp.notify.config.PinappNotifyConfig;
import com.pinapp.notify.core.NotificationServiceImpl;
import com.pinapp.notify.adapters.mock.MockNotificationProvider;
import com.pinapp.notify.domain.vo.ChannelType;

// Configurar proveedores usando el Builder
PinappNotifyConfig config = PinappNotifyConfig.builder()
    .addProvider(ChannelType.EMAIL, MockNotificationProvider.forEmail())
    .addProvider(ChannelType.SMS, MockNotificationProvider.forSms())
    .build();

// Crear el servicio de notificaciones
NotificationService notificationService = new NotificationServiceImpl(config);
```

### 2. Enviar una Notificación

```java
import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationPriority;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.NotificationResult;

// Crear el destinatario
Recipient recipient = new Recipient(
    "usuario@example.com",  // email
    "+56912345678",          // teléfono
    Map.of()                 // metadata adicional
);

// Crear la notificación
Notification notification = Notification.create(
    recipient,
    "Hola! Este es un mensaje de prueba",
    NotificationPriority.HIGH
);

// Enviar por un canal específico
NotificationResult result = notificationService.send(notification, ChannelType.EMAIL);

// O dejar que el SDK seleccione el canal automáticamente
NotificationResult result = notificationService.send(notification);
```

### 3. Procesar el Resultado

```java
if (result.success()) {
    System.out.println("✓ Notificación enviada exitosamente");
    System.out.println("  Proveedor: " + result.providerName());
    System.out.println("  Canal: " + result.channelType());
} else {
    System.err.println("✗ Error al enviar: " + result.errorMessage());
}
```

## 📦 Estructura del Proyecto

```
com.pinapp.notify
├── adapters/           # Adaptadores externos (implementaciones de providers)
│   └── mock/          # Proveedores mock para testing
├── config/            # Configuración del SDK
│   └── PinappNotifyConfig.java
├── core/              # Lógica de negocio core
│   └── NotificationServiceImpl.java
├── domain/            # Modelos del dominio
│   ├── Notification.java
│   ├── NotificationResult.java
│   ├── NotificationPriority.java
│   ├── Recipient.java
│   └── vo/           # Value Objects
│       └── ChannelType.java
├── exception/         # Excepciones del dominio
│   ├── NotificationException.java
│   ├── ValidationException.java
│   └── ProviderException.java
├── ports/             # Contratos de la arquitectura hexagonal
│   ├── in/           # Puertos de entrada (use cases)
│   │   └── NotificationService.java
│   └── out/          # Puertos de salida (SPI)
│       └── NotificationProvider.java
└── example/           # Ejemplos de uso
    └── QuickStartExample.java
```

## 🔧 Configuración Avanzada

### Registrar Proveedores por Canal Específico

```java
PinappNotifyConfig config = PinappNotifyConfig.builder()
    .addProvider(ChannelType.EMAIL, new CustomEmailProvider(apiKey))
    .addProvider(ChannelType.SMS, new TwilioSmsProvider(accountSid, authToken))
    .addProvider(ChannelType.PUSH, new FirebasePushProvider(credentials))
    .addProvider(ChannelType.SLACK, new SlackWebhookProvider(webhookUrl))
    .build();
```

### Registro Automático de Proveedores

```java
// Si un proveedor soporta múltiples canales, se registrará automáticamente
NotificationProvider multiChannelProvider = new MyCustomProvider();

PinappNotifyConfig config = PinappNotifyConfig.builder()
    .addProvider(multiChannelProvider)  // Se registra para todos los canales que soporte
    .build();
```

### Canal por Defecto Automático

El SDK selecciona automáticamente el canal basándose en la información del destinatario:

**Orden de Preferencia**: EMAIL > SMS > PUSH > SLACK

```java
// Si el destinatario tiene email, se usará EMAIL
Recipient recipient1 = new Recipient("user@example.com", null, Map.of());
service.send(notification1);  // → Usa EMAIL

// Si solo tiene teléfono, se usará SMS
Recipient recipient2 = new Recipient(null, "+56912345678", Map.of());
service.send(notification2);  // → Usa SMS

// Si tiene deviceToken en metadata, se usará PUSH
Recipient recipient3 = new Recipient(null, null, Map.of("deviceToken", "abc123"));
service.send(notification3);  // → Usa PUSH
```

## 🧪 Testing

El SDK incluye proveedores mock para facilitar el testing:

```java
import com.pinapp.notify.adapters.mock.MockNotificationProvider;

// Crear proveedores mock
MockNotificationProvider emailMock = MockNotificationProvider.forEmail();
MockNotificationProvider smsMock = MockNotificationProvider.forSms();

// Usar en tests
PinappNotifyConfig testConfig = PinappNotifyConfig.builder()
    .addProvider(ChannelType.EMAIL, emailMock)
    .addProvider(ChannelType.SMS, smsMock)
    .build();

NotificationService service = new NotificationServiceImpl(testConfig);
```

## 🎯 Ejemplo Completo

Ejecuta la clase `QuickStartExample` para ver el SDK en acción:

```bash
mvn exec:java -Dexec.mainClass="com.pinapp.notify.example.QuickStartExample"
```

## 📚 Documentación de APIs

### Notification

Record inmutable que representa una notificación:

- `UUID id`: Identificador único
- `Recipient recipient`: Destinatario
- `String message`: Mensaje a enviar
- `NotificationPriority priority`: Prioridad (LOW, NORMAL, HIGH, CRITICAL)

### Recipient

Record que representa al destinatario:

- `String email`: Correo electrónico (opcional)
- `String phone`: Número de teléfono (opcional)
- `Map<String, String> metadata`: Metadatos adicionales (ej: deviceToken, slackChannelId)

### NotificationResult

Record con el resultado del envío:

- `UUID notificationId`: ID de la notificación
- `boolean success`: Indica si fue exitoso
- `String providerName`: Nombre del proveedor utilizado
- `ChannelType channelType`: Canal utilizado
- `Instant timestamp`: Momento del procesamiento
- `String errorMessage`: Mensaje de error (si aplica)

## 🛠️ Implementar un Proveedor Personalizado

```java
public class MyEmailProvider implements NotificationProvider {
    
    @Override
    public boolean supports(ChannelType channel) {
        return ChannelType.EMAIL.equals(channel);
    }
    
    @Override
    public NotificationResult send(Notification notification) {
        try {
            // Lógica de envío real
            sendEmail(notification.recipient().email(), notification.message());
            
            return NotificationResult.success(
                notification.id(),
                getName(),
                ChannelType.EMAIL
            );
        } catch (Exception e) {
            return NotificationResult.failure(
                notification.id(),
                getName(),
                ChannelType.EMAIL,
                e.getMessage()
            );
        }
    }
    
    @Override
    public String getName() {
        return "MyEmailProvider";
    }
}
```

## 🔐 Manejo de Errores

El SDK lanza excepciones específicas para diferentes tipos de errores:

- **ValidationException**: Cuando los datos de entrada son inválidos
- **ProviderException**: Cuando el proveedor externo falla
- **NotificationException**: Error general de notificación

```java
try {
    NotificationResult result = service.send(notification, ChannelType.EMAIL);
    // Procesar resultado
} catch (ValidationException e) {
    // Datos inválidos
    log.error("Validación falló: {}", e.getMessage());
} catch (NotificationException e) {
    // Error de configuración u otro error
    log.error("Error al enviar: {}", e.getMessage());
}
```

## 📝 Licencia

Proyecto interno de PinApp.

## 👥 Autores

PinApp Team
