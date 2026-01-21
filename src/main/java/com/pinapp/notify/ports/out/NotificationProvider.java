package com.pinapp.notify.ports.out;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ProviderException;

/**
 * 🔌 Outbound Port: Contract for external notification providers.
 * 
 * <p>
 * This Service Provider Interface (SPI) must be implemented by outbound
 * adapters (Email, SMS, Push, etc.). These adapters are responsible for
 * technical communication with external infrastructure services.
 * </p>
 * 
 * <p>
 * This decoupling ensures the core domain remains isolated from
 * infrastructure details like SMTP protocols, vendor APIs, or specific
 * transport formats.
 * </p>
 * 
 * @author PinApp Team
 */
public interface NotificationProvider {

    /**
     * Verifica si este proveedor soporta el tipo de canal especificado.
     * 
     * <p>
     * Este método permite que el sistema seleccione automáticamente
     * el proveedor adecuado para cada tipo de canal.
     * </p>
     * 
     * @param channel el tipo de canal a verificar
     * @return true si este proveedor puede manejar el canal, false en caso
     *         contrario
     */
    boolean supports(ChannelType channel);

    /**
     * Envía una notificación a través de este proveedor.
     * 
     * <p>
     * La implementación debe transformar la notificación del dominio
     * al formato requerido por el servicio externo y manejar cualquier
     * error que pueda ocurrir durante el envío.
     * </p>
     * 
     * @param notification la notificación a enviar
     * @return el resultado del envío con información sobre el éxito o fracaso
     * @throws ProviderException si ocurre un error durante el envío
     */
    NotificationResult send(Notification notification);

    /**
     * Obtiene el nombre identificador de este proveedor.
     * 
     * <p>
     * Este nombre se utiliza para logging y para identificar qué
     * proveedor procesó una notificación en el NotificationResult.
     * </p>
     * 
     * @return el nombre del proveedor (ej. "EmailProvider", "SmsProvider")
     */
    String getName();
}
