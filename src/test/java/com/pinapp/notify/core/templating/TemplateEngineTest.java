package com.pinapp.notify.core.templating;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para TemplateEngine.
 * Verifica el procesamiento de plantillas con variables.
 */
@DisplayName("TemplateEngine")
class TemplateEngineTest {
    
    private TemplateEngine templateEngine;
    
    @BeforeEach
    void setUp() {
        templateEngine = new TemplateEngine();
    }
    
    @Nested
    @DisplayName("Procesamiento de Templates")
    class TemplateProcessing {
        
        @Test
        @DisplayName("Debe reemplazar una sola variable")
        void shouldReplaceSingleVariable() {
            // Given
            String template = "Hola {{nombre}}";
            Map<String, String> variables = Map.of("nombre", "Juan");
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Hola Juan", result);
        }
        
        @Test
        @DisplayName("Debe reemplazar múltiples variables")
        void shouldReplaceMultipleVariables() {
            // Given
            String template = "Hola {{nombre}}, tu código es {{codigo}}";
            Map<String, String> variables = Map.of(
                "nombre", "Juan",
                "codigo", "1234"
            );
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Hola Juan, tu código es 1234", result);
        }
        
        @Test
        @DisplayName("Debe reemplazar la misma variable múltiples veces")
        void shouldReplaceSameVariableMultipleTimes() {
            // Given
            String template = "{{nombre}} tiene {{edad}} años. Feliz cumpleaños {{nombre}}!";
            Map<String, String> variables = Map.of(
                "nombre", "María",
                "edad", "25"
            );
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("María tiene 25 años. Feliz cumpleaños María!", result);
        }
        
        @Test
        @DisplayName("Debe manejar variables con espacios")
        void shouldHandleVariablesWithSpaces() {
            // Given
            String template = "Valor: {{ variable }}";
            Map<String, String> variables = Map.of("variable", "123");
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Valor: 123", result);
        }
        
        @Test
        @DisplayName("Debe reemplazar variables por cadena vacía si no existen")
        void shouldReplaceWithEmptyStringIfVariableNotFound() {
            // Given
            String template = "Hola {{nombre}}, tu código es {{codigo}}";
            Map<String, String> variables = Map.of("nombre", "Juan");
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Hola Juan, tu código es ", result);
        }
        
        @Test
        @DisplayName("Debe retornar template sin cambios si no hay variables")
        void shouldReturnUnchangedIfNoVariablesInTemplate() {
            // Given
            String template = "Este es un mensaje sin variables";
            Map<String, String> variables = Map.of("nombre", "Juan");
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals(template, result);
        }
        
        @Test
        @DisplayName("Debe retornar template sin cambios si mapa de variables vacío")
        void shouldReturnUnchangedIfEmptyVariablesMap() {
            // Given
            String template = "Hola {{nombre}}";
            Map<String, String> variables = Map.of();
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Hola ", result);
        }
        
        @Test
        @DisplayName("Debe retornar template sin cambios si variables es null")
        void shouldReturnUnchangedIfVariablesIsNull() {
            // Given
            String template = "Hola {{nombre}}";
            
            // When
            String result = templateEngine.process(template, null);
            
            // Then
            assertEquals(template, result);
        }
        
        @Test
        @DisplayName("Debe manejar caracteres especiales en valores")
        void shouldHandleSpecialCharactersInValues() {
            // Given
            String template = "Precio: {{precio}}";
            Map<String, String> variables = Map.of("precio", "$100.50");
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Precio: $100.50", result);
        }
        
        @Test
        @DisplayName("Debe manejar saltos de línea en template")
        void shouldHandleNewlinesInTemplate() {
            // Given
            String template = "Estimado {{nombre}},\n\nTu código de verificación es: {{codigo}}\n\nSaludos.";
            Map<String, String> variables = Map.of(
                "nombre", "Carlos",
                "codigo", "5678"
            );
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertTrue(result.contains("Estimado Carlos,"));
            assertTrue(result.contains("Tu código de verificación es: 5678"));
        }
    }
    
    @Nested
    @DisplayName("Procesamiento con Object Values")
    class ObjectValuesProcessing {
        
        @Test
        @DisplayName("Debe procesar mapa con valores Object")
        void shouldProcessMapWithObjectValues() {
            // Given
            String template = "Usuario: {{nombre}}, Edad: {{edad}}, Activo: {{activo}}";
            Map<String, Object> variables = Map.of(
                "nombre", "Ana",
                "edad", 30,
                "activo", true
            );
            
            // When
            String result = templateEngine.process(template, variables, true);
            
            // Then
            assertEquals("Usuario: Ana, Edad: 30, Activo: true", result);
        }
        
        @Test
        @DisplayName("Debe manejar valores null en Object map")
        void shouldHandleNullValuesInObjectMap() {
            // Given
            String template = "Valor: {{valor}}";
            Map<String, Object> variables = Map.of();
            
            // When
            String result = templateEngine.process(template, variables, true);
            
            // Then
            assertEquals("Valor: ", result);
        }
    }
    
    @Nested
    @DisplayName("Detección de Variables")
    class VariableDetection {
        
        @Test
        @DisplayName("Debe detectar que template tiene variables")
        void shouldDetectTemplateHasVariables() {
            // Given
            String template = "Hola {{nombre}}";
            
            // When
            boolean hasVariables = templateEngine.hasVariables(template);
            
            // Then
            assertTrue(hasVariables);
        }
        
        @Test
        @DisplayName("Debe detectar que template no tiene variables")
        void shouldDetectTemplateHasNoVariables() {
            // Given
            String template = "Este es un mensaje simple";
            
            // When
            boolean hasVariables = templateEngine.hasVariables(template);
            
            // Then
            assertFalse(hasVariables);
        }
        
        @Test
        @DisplayName("Debe retornar false para template null")
        void shouldReturnFalseForNullTemplate() {
            // When
            boolean hasVariables = templateEngine.hasVariables(null);
            
            // Then
            assertFalse(hasVariables);
        }
        
        @Test
        @DisplayName("Debe retornar false para template vacío")
        void shouldReturnFalseForEmptyTemplate() {
            // When
            boolean hasVariables = templateEngine.hasVariables("");
            
            // Then
            assertFalse(hasVariables);
        }
    }
    
    @Nested
    @DisplayName("Extracción de Variables")
    class VariableExtraction {
        
        @Test
        @DisplayName("Debe extraer una sola variable")
        void shouldExtractSingleVariable() {
            // Given
            String template = "Hola {{nombre}}";
            
            // When
            Set<String> variables = templateEngine.extractVariables(template);
            
            // Then
            assertEquals(1, variables.size());
            assertTrue(variables.contains("nombre"));
        }
        
        @Test
        @DisplayName("Debe extraer múltiples variables")
        void shouldExtractMultipleVariables() {
            // Given
            String template = "Hola {{nombre}}, tu código es {{codigo}}";
            
            // When
            Set<String> variables = templateEngine.extractVariables(template);
            
            // Then
            assertEquals(2, variables.size());
            assertTrue(variables.contains("nombre"));
            assertTrue(variables.contains("codigo"));
        }
        
        @Test
        @DisplayName("Debe extraer variables únicas aunque se repitan")
        void shouldExtractUniqueVariablesEvenIfRepeated() {
            // Given
            String template = "{{nombre}} tiene {{edad}} años. Feliz cumpleaños {{nombre}}!";
            
            // When
            Set<String> variables = templateEngine.extractVariables(template);
            
            // Then
            assertEquals(2, variables.size());
            assertTrue(variables.contains("nombre"));
            assertTrue(variables.contains("edad"));
        }
        
        @Test
        @DisplayName("Debe retornar conjunto vacío si no hay variables")
        void shouldReturnEmptySetIfNoVariables() {
            // Given
            String template = "Mensaje sin variables";
            
            // When
            Set<String> variables = templateEngine.extractVariables(template);
            
            // Then
            assertTrue(variables.isEmpty());
        }
        
        @Test
        @DisplayName("Debe retornar conjunto vacío para template null")
        void shouldReturnEmptySetForNullTemplate() {
            // When
            Set<String> variables = templateEngine.extractVariables(null);
            
            // Then
            assertTrue(variables.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Casos Edge")
    class EdgeCases {
        
        @Test
        @DisplayName("Debe fallar con template null")
        void shouldThrowExceptionForNullTemplate() {
            // Given
            Map<String, String> variables = Map.of("nombre", "Juan");
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                templateEngine.process(null, variables)
            );
        }
        
        @Test
        @DisplayName("Debe manejar template vacío")
        void shouldHandleEmptyTemplate() {
            // Given
            String template = "";
            Map<String, String> variables = Map.of("nombre", "Juan");
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("Debe manejar llaves desbalanceadas")
        void shouldHandleUnbalancedBraces() {
            // Given
            String template = "Texto con {{variable incompleta";
            Map<String, String> variables = Map.of("variable", "valor");
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Texto con {{variable incompleta", result);
        }
        
        @Test
        @DisplayName("Debe ignorar llaves simples")
        void shouldIgnoreSingleBraces() {
            // Given
            String template = "Código {variable} no válido";
            Map<String, String> variables = Map.of("variable", "valor");
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Código {variable} no válido", result);
        }
    }
    
    @Nested
    @DisplayName("Casos Reales de Uso")
    class RealWorldUseCases {
        
        @Test
        @DisplayName("Debe procesar template de verificación de email")
        void shouldProcessEmailVerificationTemplate() {
            // Given
            String template = """
                Estimado {{nombre}},
                
                Gracias por registrarte en nuestra plataforma.
                Tu código de verificación es: {{codigo}}
                
                Este código expira en {{minutos}} minutos.
                
                Saludos,
                El equipo de PinApp
                """;
            
            Map<String, String> variables = Map.of(
                "nombre", "María García",
                "codigo", "ABC-123-XYZ",
                "minutos", "15"
            );
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertTrue(result.contains("Estimado María García,"));
            assertTrue(result.contains("Tu código de verificación es: ABC-123-XYZ"));
            assertTrue(result.contains("Este código expira en 15 minutos."));
        }
        
        @Test
        @DisplayName("Debe procesar template de notificación SMS")
        void shouldProcessSmsNotificationTemplate() {
            // Given
            String template = "Tu codigo de acceso es {{codigo}}. Valido por {{minutos}}min. No lo compartas.";
            Map<String, String> variables = Map.of(
                "codigo", "8723",
                "minutos", "5"
            );
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("Tu codigo de acceso es 8723. Valido por 5min. No lo compartas.", result);
        }
        
        @Test
        @DisplayName("Debe procesar template de alerta")
        void shouldProcessAlertTemplate() {
            // Given
            String template = "ALERTA: Transaccion de {{monto}} {{moneda}} detectada en tu cuenta {{cuenta}}. Si no fuiste tu, contactanos.";
            Map<String, String> variables = Map.of(
                "monto", "500.00",
                "moneda", "MXN",
                "cuenta", "****1234"
            );
            
            // When
            String result = templateEngine.process(template, variables);
            
            // Then
            assertEquals("ALERTA: Transaccion de 500.00 MXN detectada en tu cuenta ****1234. Si no fuiste tu, contactanos.", result);
        }
    }
}
