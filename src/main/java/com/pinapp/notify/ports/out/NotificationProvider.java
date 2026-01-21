package com.pinapp.notify.ports.out;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ProviderException;
/**
 * Puerto de salida (Outbound Port) que define el contrato para los
 * proveedores de notificación.
 * 
 * <p>Esta interfaz representa un Service Provider Interface (SPI) que debe
 * ser implementado por los adaptadores externos (Email, SMS, Push, Slack, etc.).
 * Los adaptadores son responsables de la comunicación con servicios externos
 * y transforman las notificaciones del dominio a los formatos específicos
 * de cada proveedor.</p>
 * 
 * <p>Siguiendo Arquitectura Hexagonal, esta interfaz pertenece al hexágono
 * exterior y permite que la aplicación sea independiente de los detalles
 * de implementación de los servicios externos.</p>
 * 
 * <p>Los proveedores deben registrarse como servicios usando el mecanismo
 * de ServiceLoader de Java o mediante inyección de dependencias manual.</p>
 * 
 * @author PinApp Team
 */
public interface NotificationProvider {
    
    /**
     * Verifica si este proveedor soporta el tipo de canal especificado.
     * 
     * <p>Este método permite que el sistema seleccione automáticamente
     * el proveedor adecuado para cada tipo de canal.</p>
     * 
     * @param channel el tipo de canal a verificar
     * @return true si este proveedor puede manejar el canal, false en caso contrario
     */
    boolean supports(ChannelType channel);
    
    /**
     * Envía una notificación a través de este proveedor.
     * 
     * <p>La implementación debe transformar la notificación del dominio
     * al formato requerido por el servicio externo y manejar cualquier
     * error que pueda ocurrir durante el envío.</p>
     * 
     * @param notification la notificación a enviar
     * @return el resultado del envío con información sobre el éxito o fracaso
     * @throws ProviderException si ocurre un error durante el envío
     */
    NotificationResult send(Notification notification);
    
    /**
     * Obtiene el nombre identificador de este proveedor.
     * 
     * <p>Este nombre se utiliza para logging y para identificar qué
     * proveedor procesó una notificación en el NotificationResult.</p>
     * 
     * @return el nombre del proveedor (ej. "EmailProvider", "SmsProvider")
     */
    String getName();
}
