package com.pinapp.notify.domain;

/**
 * Interfaz que abstrae el contenido de un mensaje de notificación.
 * Permite soportar diferentes tipos de contenido como texto plano y templates.
 * 
 * <p>Esta interfaz sigue el principio de Open/Closed, permitiendo extender
 * los tipos de contenido sin modificar el código existente.</p>
 * 
 * @author PinApp Team
 */
public sealed interface MessageContent 
        permits MessageContent.PlainText, MessageContent.Template {
    
    /**
     * Obtiene el contenido del mensaje como String.
     * 
     * @return el contenido del mensaje
     */
    String getContent();
    
    /**
     * Representa un mensaje de texto plano.
     * 
     * @param content el contenido del mensaje
     */
    record PlainText(String content) implements MessageContent {
        @Override
        public String getContent() {
            return content;
        }
    }
    
    /**
     * Representa un mensaje basado en template.
     * 
     * @param templateId el identificador del template
     * @param variables las variables para rellenar el template
     */
    record Template(String templateId, java.util.Map<String, Object> variables) 
            implements MessageContent {
        
        @Override
        public String getContent() {
            // La implementación de renderizado se hará en los adaptadores
            return templateId;
        }
    }
}
