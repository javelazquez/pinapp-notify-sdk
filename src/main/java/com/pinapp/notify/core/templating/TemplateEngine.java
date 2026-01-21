package com.pinapp.notify.core.templating;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de plantillas simple para procesamiento de mensajes con variables.
 * 
 * <p>Este motor reemplaza ocurrencias de {@code {{key}}} con los valores
 * correspondientes del mapa de variables proporcionado.</p>
 * 
 * <p>Características:</p>
 * <ul>
 *   <li>Sintaxis simple: {@code {{variable}}}</li>
 *   <li>Soporte para variables anidadas en el texto</li>
 *   <li>Manejo seguro de variables faltantes</li>
 *   <li>Performance optimizada con regex compilado</li>
 * </ul>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * TemplateEngine engine = new TemplateEngine();
 * String template = "Hola {{nombre}}, tu código es {{codigo}}";
 * Map<String, String> vars = Map.of("nombre", "Juan", "codigo", "1234");
 * String result = engine.process(template, vars);
 * // result: "Hola Juan, tu código es 1234"
 * }</pre>
 * 
 * @author PinApp Team
 */
public class TemplateEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(TemplateEngine.class);
    
    /**
     * Pattern para detectar variables en formato {{key}}.
     * Captura el nombre de la variable en el grupo 1.
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");
    
    /**
     * Placeholder para variables no encontradas.
     */
    private static final String MISSING_VARIABLE_PLACEHOLDER = "";
    
    /**
     * Procesa una plantilla reemplazando las variables por sus valores.
     * 
     * <p>Si una variable no se encuentra en el mapa, se reemplaza por una cadena vacía
     * y se registra una advertencia en el log.</p>
     * 
     * @param template la plantilla con variables en formato {{key}}
     * @param variables el mapa de variables con sus valores
     * @return el texto procesado con todas las variables reemplazadas
     * @throws IllegalArgumentException si template es null
     */
    public String process(String template, Map<String, String> variables) {
        if (template == null) {
            throw new IllegalArgumentException("La plantilla no puede ser null");
        }
        
        // Si la plantilla está vacía, retornar directamente
        if (template.isBlank()) {
            logger.debug("Plantilla vacía, retornando sin procesar");
            return template;
        }
        
        // Si variables es null (no solo vacío), retornar template sin procesar
        // null significa "no hay contexto de variables"
        if (variables == null) {
            logger.debug("No hay contexto de variables (null), retornando template sin procesar");
            return template;
        }
        
        // Si llegamos aquí, variables no es null (puede estar vacío, pero existe un contexto)
        logger.debug("Procesando plantilla con {} variable(s)", variables.size());
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        int replacementCount = 0;
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variables.getOrDefault(
                variableName, 
                handleMissingVariable(variableName)
            );
            
            // Escapar caracteres especiales de regex en el replacement
            String safeReplacement = Matcher.quoteReplacement(replacement);
            matcher.appendReplacement(result, safeReplacement);
            replacementCount++;
            
            logger.trace("Variable '{}' reemplazada por '{}'", variableName, replacement);
        }
        
        matcher.appendTail(result);
        
        logger.debug("Plantilla procesada: {} variable(s) reemplazada(s)", replacementCount);
        return result.toString();
    }
    
    /**
     * Sobrecarga del método process que acepta Map con valores Object.
     * 
     * <p>Convierte automáticamente los valores Object a String usando toString().</p>
     * 
     * @param template la plantilla con variables en formato {{key}}
     * @param variables el mapa de variables con valores de cualquier tipo
     * @return el texto procesado
     * @throws IllegalArgumentException si template es null
     */
    public String process(String template, Map<String, Object> variables, boolean objectValues) {
        if (template == null) {
            throw new IllegalArgumentException("La plantilla no puede ser null");
        }
        
        // Si variables es null (no solo vacío), retornar template sin procesar
        if (variables == null) {
            return template;
        }
        
        // Convertir Map<String, Object> a Map<String, String>
        // Si el mapa está vacío, esto creará un mapa vacío de Strings
        Map<String, String> stringVariables = variables.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() != null ? entry.getValue().toString() : ""
            ));
        
        // Llamar al método principal que manejará el mapa vacío correctamente
        return process(template, stringVariables);
    }
    
    /**
     * Maneja el caso de una variable no encontrada en el mapa.
     * 
     * <p>Por defecto, retorna una cadena vacía y registra una advertencia.
     * Esto previene que el mensaje falle completamente por una variable faltante.</p>
     * 
     * @param variableName el nombre de la variable no encontrada
     * @return el valor a usar como reemplazo
     */
    private String handleMissingVariable(String variableName) {
        logger.warn("Variable '{}' no encontrada en el mapa de variables. " +
            "Se reemplazará por cadena vacía", variableName);
        return MISSING_VARIABLE_PLACEHOLDER;
    }
    
    /**
     * Verifica si una plantilla contiene variables.
     * 
     * @param template la plantilla a verificar
     * @return true si contiene al menos una variable en formato {{key}}
     */
    public boolean hasVariables(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        
        return VARIABLE_PATTERN.matcher(template).find();
    }
    
    /**
     * Extrae todas las variables encontradas en una plantilla.
     * 
     * @param template la plantilla a analizar
     * @return un conjunto con los nombres de todas las variables encontradas
     */
    public java.util.Set<String> extractVariables(String template) {
        if (template == null || template.isBlank()) {
            return java.util.Set.of();
        }
        
        java.util.Set<String> variables = new java.util.HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        
        logger.debug("Extraídas {} variable(s) de la plantilla", variables.size());
        return variables;
    }
}
