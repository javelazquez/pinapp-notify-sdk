package com.pinapp.notify.adapters.email;

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
 * Adaptador de salida (Outbound Adapter) para envío de notificaciones por
 * email.
 * 
 * <p>
 * Este adaptador implementa el puerto {@link NotificationProvider} y es
 * responsable
 * de la comunicación con servicios de correo electrónico. En esta versión
 * simula
 * el envío sin realizar conexiones HTTP reales.
 * </p>
 * 
 * <p>
 * En una arquitectura hexagonal, este adaptador pertenece a la capa de
 * infraestructura y puede ser reemplazado por otra implementación sin afectar
 * el núcleo de la aplicación.
 * </p>
 * 
 * <p>
 * Características:
 * </p>
 * <ul>
 * <li>Validación de email del destinatario</li>
 * <li>Validación de subject en metadatos</li>
 * <li>Logging estructurado del envío simulado</li>
 * <li>Generación de messageId único (UUID)</li>
 * </ul>
 * 
 * @author PinApp Team
 */
@Slf4j
public class EmailNotificationProvider implements NotificationProvider {

    private static final String PROVIDER_NAME = "EmailProvider";
    private static final String METADATA_SUBJECT_KEY = "subject";

    private final String apiKey;

    /**
     * Constructor que acepta una API Key para configuración.
     * 
     * @param apiKey la clave de API simulada para el servicio de email
     */
    public EmailNotificationProvider(String apiKey) {
        this.apiKey = apiKey;
        log.debug("[EMAIL PROVIDER] Inicializado con API Key: {}***",
                apiKey != null && apiKey.length() > 4 ? apiKey.substring(0, 4) : "****");
    }

    /**
     * Constructor por defecto sin API Key.
     */
    public EmailNotificationProvider() {
        this(null);
    }

    @Override
    public boolean supports(ChannelType channel) {
        return ChannelType.EMAIL.equals(channel);
    }

    @Override
    public NotificationResult send(Notification notification) {
        log.info("[EMAIL PROVIDER] Procesando notificación [id={}]", notification.id());

        // Validar que el destinatario tenga email
        Recipient recipient = notification.recipient();
        if (recipient.email() == null || recipient.email().isBlank()) {
            String errorMsg = "El destinatario no tiene una dirección de email válida";
            log.error("[EMAIL PROVIDER] Error: {}", errorMsg);
            throw new ProviderException(PROVIDER_NAME, errorMsg);
        }

        // Validar que exista el subject en los metadatos
        String subject = recipient.metadata().get(METADATA_SUBJECT_KEY);
        if (subject == null || subject.isBlank()) {
            String errorMsg = "La notificación debe tener un 'subject' en los metadatos del destinatario";
            log.error("[EMAIL PROVIDER] Error: {}", errorMsg);
            throw new ProviderException(PROVIDER_NAME, errorMsg);
        }

        // Simular el envío del email
        String email = recipient.email();
        String body = notification.message();
        String messageId = UUID.randomUUID().toString();

        log.info("[EMAIL PROVIDER] Sending to: {} | Subject: {} | Body: {} | MessageId: {}",
                email, subject, truncateMessage(body), messageId);

        log.debug("[EMAIL PROVIDER] Priority: {} | API Key configured: {}",
                notification.priority(), apiKey != null);

        log.info("[EMAIL PROVIDER] ✓ Email enviado exitosamente [messageId={}]", messageId);

        return NotificationResult.success(
                notification.id(),
                PROVIDER_NAME,
                ChannelType.EMAIL);
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
