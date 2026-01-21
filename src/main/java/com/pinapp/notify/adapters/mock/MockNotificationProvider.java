package com.pinapp.notify.adapters.mock;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.out.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación mock de un proveedor de notificaciones.
 * 
 * <p>Esta clase se utiliza únicamente para pruebas y desarrollo.
 * Simula el envío de notificaciones sin realizar llamadas reales
 * a servicios externos.</p>
 * 
 * <p>Útil para:</p>
 * <ul>
 *   <li>Testing sin dependencias externas</li>
 *   <li>Desarrollo local sin APIs keys reales</li>
 *   <li>Validación de la lógica de orquestación</li>
 * </ul>
 * 
 * @author PinApp Team
 */
public class MockNotificationProvider implements NotificationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(MockNotificationProvider.class);
    
    private final ChannelType supportedChannel;
    private final String providerName;
    private final boolean shouldSucceed;
    
    /**
     * Constructor que crea un proveedor mock para un canal específico.
     * 
     * @param supportedChannel el canal que este proveedor soporta
     * @param providerName el nombre del proveedor
     * @param shouldSucceed si el envío debe tener éxito (true) o fallar (false)
     */
    public MockNotificationProvider(ChannelType supportedChannel, String providerName, boolean shouldSucceed) {
        this.supportedChannel = supportedChannel;
        this.providerName = providerName;
        this.shouldSucceed = shouldSucceed;
    }
    
    /**
     * Constructor que crea un proveedor mock exitoso.
     * 
     * @param supportedChannel el canal que este proveedor soporta
     * @param providerName el nombre del proveedor
     */
    public MockNotificationProvider(ChannelType supportedChannel, String providerName) {
        this(supportedChannel, providerName, true);
    }
    
    /**
     * Constructor que crea un proveedor mock con nombre por defecto.
     * 
     * @param supportedChannel el canal que este proveedor soporta
     */
    public MockNotificationProvider(ChannelType supportedChannel) {
        this(supportedChannel, "Mock" + supportedChannel.name() + "Provider", true);
    }
    
    @Override
    public boolean supports(ChannelType channel) {
        return this.supportedChannel.equals(channel);
    }
    
    @Override
    public NotificationResult send(Notification notification) {
        logger.info("[MOCK] Simulando envío de notificación [id={}] vía {} usando '{}'", 
            notification.id(), supportedChannel, providerName);
        
        logger.debug("[MOCK] Destinatario: {}", notification.recipient());
        logger.debug("[MOCK] Mensaje: {}", notification.message());
        logger.debug("[MOCK] Prioridad: {}", notification.priority());
        
        if (shouldSucceed) {
            logger.info("[MOCK] ✓ Envío simulado exitoso para notificación [id={}]", notification.id());
            return NotificationResult.success(
                notification.id(),
                providerName,
                supportedChannel
            );
        } else {
            String errorMsg = String.format(
                "Error simulado en el envío por el proveedor mock '%s'", 
                providerName
            );
            logger.warn("[MOCK] ✗ Envío simulado fallido para notificación [id={}]: {}", 
                notification.id(), errorMsg);
            return NotificationResult.failure(
                notification.id(),
                providerName,
                supportedChannel,
                errorMsg
            );
        }
    }
    
    @Override
    public String getName() {
        return providerName;
    }
    
    /**
     * Crea un proveedor mock para EMAIL.
     * 
     * @return un MockNotificationProvider para EMAIL
     */
    public static MockNotificationProvider forEmail() {
        return new MockNotificationProvider(ChannelType.EMAIL);
    }
    
    /**
     * Crea un proveedor mock para SMS.
     * 
     * @return un MockNotificationProvider para SMS
     */
    public static MockNotificationProvider forSms() {
        return new MockNotificationProvider(ChannelType.SMS);
    }
    
    /**
     * Crea un proveedor mock para PUSH.
     * 
     * @return un MockNotificationProvider para PUSH
     */
    public static MockNotificationProvider forPush() {
        return new MockNotificationProvider(ChannelType.PUSH);
    }
    
    /**
     * Crea un proveedor mock para SLACK.
     * 
     * @return un MockNotificationProvider para SLACK
     */
    public static MockNotificationProvider forSlack() {
        return new MockNotificationProvider(ChannelType.SLACK);
    }
}
