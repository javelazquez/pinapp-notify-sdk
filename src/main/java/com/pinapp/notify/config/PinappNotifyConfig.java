package com.pinapp.notify.config;

import com.pinapp.notify.domain.vo.ChannelType;
import com.pinapp.notify.ports.out.NotificationProvider;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuración principal del SDK de notificaciones PinApp.
 * 
 * <p>Esta clase permite configurar la librería mediante código Java puro,
 * sin necesidad de archivos YAML o properties. Utiliza el patrón Builder
 * para proporcionar una API fluida y fácil de usar.</p>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * PinappNotifyConfig config = PinappNotifyConfig.builder()
 *     .addProvider(ChannelType.EMAIL, new EmailProvider(apiKey))
 *     .addProvider(ChannelType.SMS, new SmsProvider(apiKey))
 *     .build();
 * }</pre>
 * 
 * @author PinApp Team
 */
@Getter
public class PinappNotifyConfig {
    
    /**
     * Mapa de proveedores indexados por tipo de canal.
     * Utilizamos EnumMap para mejor rendimiento y type-safety.
     */
    private final Map<ChannelType, NotificationProvider> providers;
    
    /**
     * Constructor privado para forzar el uso del Builder.
     * 
     * @param providers mapa de proveedores configurados
     */
    private PinappNotifyConfig(Map<ChannelType, NotificationProvider> providers) {
        this.providers = new EnumMap<>(providers);
    }
    
    /**
     * Obtiene el proveedor configurado para un tipo de canal específico.
     * 
     * @param channelType el tipo de canal
     * @return un Optional conteniendo el proveedor si existe, o vacío si no está configurado
     */
    public Optional<NotificationProvider> getProvider(ChannelType channelType) {
        return Optional.ofNullable(providers.get(channelType));
    }
    
    /**
     * Verifica si existe un proveedor configurado para el canal especificado.
     * 
     * @param channelType el tipo de canal a verificar
     * @return true si hay un proveedor configurado, false en caso contrario
     */
    public boolean hasProvider(ChannelType channelType) {
        return providers.containsKey(channelType);
    }
    
    /**
     * Crea un nuevo Builder para construir la configuración.
     * 
     * @return una nueva instancia de Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder para construir instancias de PinappNotifyConfig de forma fluida.
     */
    public static class Builder {
        
        private final Map<ChannelType, NotificationProvider> providers;
        
        private Builder() {
            this.providers = new EnumMap<>(ChannelType.class);
        }
        
        /**
         * Agrega un proveedor para un canal específico.
         * 
         * <p>Si ya existe un proveedor configurado para ese canal,
         * será reemplazado por el nuevo.</p>
         * 
         * @param channelType el tipo de canal
         * @param provider el proveedor a registrar
         * @return esta instancia del Builder para encadenamiento fluido
         * @throws IllegalArgumentException si channelType o provider son null
         */
        public Builder addProvider(ChannelType channelType, NotificationProvider provider) {
            if (channelType == null) {
                throw new IllegalArgumentException("El tipo de canal no puede ser null");
            }
            if (provider == null) {
                throw new IllegalArgumentException("El proveedor no puede ser null");
            }
            
            // Validamos que el proveedor soporte el canal
            if (!provider.supports(channelType)) {
                throw new IllegalArgumentException(
                    String.format("El proveedor '%s' no soporta el canal %s", 
                        provider.getName(), channelType)
                );
            }
            
            this.providers.put(channelType, provider);
            return this;
        }
        
        /**
         * Agrega un proveedor detectando automáticamente los canales soportados.
         * 
         * <p>Este método itera sobre todos los tipos de canal y registra
         * el proveedor para aquellos que soporte.</p>
         * 
         * @param provider el proveedor a registrar
         * @return esta instancia del Builder para encadenamiento fluido
         * @throws IllegalArgumentException si provider es null
         */
        public Builder addProvider(NotificationProvider provider) {
            if (provider == null) {
                throw new IllegalArgumentException("El proveedor no puede ser null");
            }
            
            // Registramos el proveedor para todos los canales que soporte
            for (ChannelType channelType : ChannelType.values()) {
                if (provider.supports(channelType)) {
                    this.providers.put(channelType, provider);
                }
            }
            
            return this;
        }
        
        /**
         * Construye la instancia final de PinappNotifyConfig.
         * 
         * @return una nueva instancia de PinappNotifyConfig
         * @throws IllegalStateException si no se ha configurado ningún proveedor
         */
        public PinappNotifyConfig build() {
            if (providers.isEmpty()) {
                throw new IllegalStateException(
                    "Debe configurar al menos un proveedor antes de construir la configuración"
                );
            }
            
            return new PinappNotifyConfig(providers);
        }
    }
}
