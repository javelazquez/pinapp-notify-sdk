package com.pinapp.notify.ports.in;

import com.pinapp.notify.domain.Notification;
import com.pinapp.notify.domain.NotificationResult;
import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.exception.ValidationException;
import com.pinapp.notify.exception.ProviderException;

import java.util.concurrent.CompletableFuture;

/**
 * ⚓ Inbound Port: Primary interface for the Notification Service.
 * 
 * <p>
 * Following Hexagonal Architecture principles, this port defines the contract
 * for the application's core functionality. It is framework-agnostic and
 * serves as the entry point for primary actors (REST controllers, CLI, etc.).
 * </p>
 * 
 * <p>
 * The implementation manages the orchestration of template processing,
 * validation, and provider selection via outbound ports.
 * </p>
 * 
 * @author PinApp Team
 */
public interface NotificationService {

    /**
     * Envía una notificación de forma síncrona.
     * 
     * <p>
     * Este método procesa la notificación y devuelve el resultado
     * del envío. La implementación seleccionará automáticamente el
     * proveedor adecuado basándose en el tipo de canal requerido.
     * </p>
     * 
     * @param notification la notificación a enviar
     * @param channelType  el tipo de canal a utilizar (EMAIL, SMS, PUSH, SLACK)
     * @return el resultado del envío con información sobre el éxito o fracaso
     * @throws ValidationException si los datos de la notificación son inválidos
     * @throws ProviderException   si el proveedor falla al enviar
     */
    NotificationResult send(Notification notification, ChannelType channelType);

    /**
     * Envía una notificación utilizando el canal por defecto.
     * 
     * <p>
     * La implementación determinará el canal apropiado basándose
     * en la información disponible del destinatario.
     * </p>
     * 
     * @param notification la notificación a enviar
     * @return el resultado del envío con información sobre el éxito o fracaso
     * @throws ValidationException si los datos de la notificación son inválidos
     * @throws ProviderException   si el proveedor falla al enviar
     */
    NotificationResult send(Notification notification);

    /**
     * Envía una notificación de forma asíncrona.
     * 
     * <p>
     * Este método ejecuta el envío en un hilo separado usando CompletableFuture,
     * permitiendo que el cliente continúe su ejecución sin bloquear. El resultado
     * puede ser procesado mediante callbacks (thenApply, thenAccept, etc.) o
     * esperando la completación con get() o join().
     * </p>
     * 
     * <p>
     * Si el envío falla, el CompletableFuture se completará excepcionalmente
     * y el error puede ser manejado usando exceptionally() o handle().
     * </p>
     * 
     * <p>
     * Ejemplo de uso:
     * </p>
     * 
     * <pre>{@code
     * service.sendAsync(notification, ChannelType.EMAIL)
     *         .thenAccept(result -> {
     *             if (result.success()) {
     *                 logger.info("Enviado exitosamente");
     *             }
     *         })
     *         .exceptionally(error -> {
     *             logger.error("Error: " + error.getMessage());
     *             return null;
     *         });
     * }</pre>
     * 
     * @param notification la notificación a enviar
     * @param channelType  el tipo de canal a utilizar
     * @return un CompletableFuture que se completará con el resultado del envío
     */
    CompletableFuture<NotificationResult> sendAsync(Notification notification, ChannelType channelType);

    /**
     * Envía una notificación de forma asíncrona usando el canal por defecto.
     * 
     * <p>
     * Este método ejecuta el envío en un hilo separado, determinando
     * automáticamente el canal apropiado basándose en la información del
     * destinatario.
     * </p>
     * 
     * @param notification la notificación a enviar
     * @return un CompletableFuture que se completará con el resultado del envío
     */
    CompletableFuture<NotificationResult> sendAsync(Notification notification);
}
