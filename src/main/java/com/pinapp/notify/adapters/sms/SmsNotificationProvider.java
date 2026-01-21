package com.pinapp.notify.adapters.sms;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.Recipient;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ProviderException;
import com.pinapp.notify.ports.out.NotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Adaptador de salida (Outbound Adapter) para envío de notificaciones por SMS.
 * 
 * <p>
 * Este adaptador implementa el puerto {@link NotificationProvider} y es
 * responsable
 * de la comunicación con servicios de mensajería SMS. En esta versión simula
 * el envío sin realizar conexiones HTTP reales.
 * </p>
 * 
 * <p>
 * En una arquitectura hexagonal, este adaptador pertenece a la capa de
 * infraestructura y puede ser reemplazado por otra implementación (ej. Twilio,
 * AWS SNS) sin afectar el núcleo de la aplicación.
 * </p>
 * 
 * <p>
 * Características:
 * </p>
 * <ul>
 * <li>Validación de número de teléfono del destinatario</li>
 * <li>Logging estructurado del envío simulado</li>
 * <li>Generación de messageId único (UUID)</li>
 * <li>Soporte para configuración de API Key</li>
 * </ul>
 * 
 * @author PinApp Team
 */
@Slf4j
public class SmsNotificationProvider implements NotificationProvider {

    private static final String PROVIDER_NAME = "SmsProvider";

    private final String apiKey;
    private final String senderId;

    /**
     * Constructor completo que acepta configuración del proveedor.
     * 
     * @param apiKey   la clave de API simulada para el servicio de SMS
     * @param senderId el identificador del remitente (ej. nombre de la app)
     */
    public SmsNotificationProvider(String apiKey, String senderId) {
        this.apiKey = apiKey;
        this.senderId = senderId != null ? senderId : "PinApp";
        log.debug("[SMS PROVIDER] Inicializado con API Key: {}*** | SenderId: {}",
                apiKey != null && apiKey.length() > 4 ? apiKey.substring(0, 4) : "****",
                this.senderId);
    }

    /**
     * Constructor que acepta solo API Key.
     * 
     * @param apiKey la clave de API simulada para el servicio de SMS
     */
    public SmsNotificationProvider(String apiKey) {
        this(apiKey, null);
    }

    /**
     * Constructor por defecto sin configuración.
     */
    public SmsNotificationProvider() {
        this(null, null);
    }

    @Override
    public boolean supports(ChannelType channel) {
        return ChannelType.SMS.equals(channel);
    }

    @Override
    public NotificationResult send(Notification notification) {
        log.info("[SMS PROVIDER] Procesando notificación [id={}]", notification.id());

        // Validar que el destinatario tenga número de teléfono
        Recipient recipient = notification.recipient();
        if (recipient.phone() == null || recipient.phone().isBlank()) {
            String errorMsg = "El destinatario no tiene un número de teléfono válido";
            log.error("[SMS PROVIDER] Error: {}", errorMsg);
            throw new ProviderException(PROVIDER_NAME, errorMsg);
        }

        // Simular el envío del SMS
        String phone = recipient.phone();
        String message = notification.message();
        String messageId = UUID.randomUUID().toString();

        log.info("[SMS PROVIDER] Sending to: {} | From: {} | Message: {} | MessageId: {}",
                phone, senderId, truncateMessage(message), messageId);

        log.debug("[SMS PROVIDER] Priority: {} | Message length: {} chars | API Key configured: {}",
                notification.priority(), message.length(), apiKey != null);

        log.info("[SMS PROVIDER] ✓ SMS enviado exitosamente [messageId={}]", messageId);

        return NotificationResult.success(
                notification.id(),
                PROVIDER_NAME,
                ChannelType.SMS);
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    /**
     * Trunca el mensaje para logging si es muy largo.
     * 
     * @param message el mensaje a truncar
     * @return el mensaje truncado
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 100 ? message.substring(0, 100) + "..." : message;
    }
}
