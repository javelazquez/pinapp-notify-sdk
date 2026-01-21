package com.pinapp.notify.exception;

/**
 * Excepción lanzada cuando ocurre un error en los adaptadores (proveedores)
 * de notificación.
 * 
 * <p>Esta excepción debe ser lanzada cuando un proveedor específico
 * (Email, SMS, Push, etc.) falla al intentar enviar una notificación.</p>
 * 
 * @author PinApp Team
 */
public class ProviderException extends NotificationException {
    
    private final String providerName;
    
    public ProviderException(String providerName, String message) {
        super(String.format("Error en el proveedor '%s': %s", providerName, message));
        this.providerName = providerName;
    }
    
    public ProviderException(String providerName, String message, Throwable cause) {
        super(String.format("Error en el proveedor '%s': %s", providerName, message), cause);
        this.providerName = providerName;
    }
    
    public String getProviderName() {
        return providerName;
    }
}
