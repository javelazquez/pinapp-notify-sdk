package com.pinapp.notify.domain;

/**
 * Record que representa la política de reintentos para el envío de notificaciones.
 * 
 * <p>Define la estrategia de reintentos cuando una notificación falla,
 * incluyendo el número máximo de intentos y el tiempo de espera entre cada intento.</p>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * RetryPolicy policy = RetryPolicy.of(3, 1000); // 3 intentos, 1 segundo entre reintentos
 * RetryPolicy noRetry = RetryPolicy.noRetry(); // Sin reintentos
 * }</pre>
 * 
 * @param maxAttempts número máximo de intentos (debe ser >= 1)
 * @param delayMillis tiempo de espera en milisegundos entre reintentos (debe ser >= 0)
 * 
 * @author PinApp Team
 */
public record RetryPolicy(
        int maxAttempts,
        long delayMillis
) {
    /**
     * Constructor compacto que valida los parámetros.
     */
    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                "El número máximo de intentos debe ser al menos 1, recibido: " + maxAttempts
            );
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException(
                "El delay en milisegundos no puede ser negativo, recibido: " + delayMillis
            );
        }
    }
    
    /**
     * Crea una política de reintentos con los parámetros especificados.
     * 
     * @param maxAttempts número máximo de intentos
     * @param delayMillis tiempo de espera en milisegundos entre reintentos
     * @return una nueva instancia de RetryPolicy
     */
    public static RetryPolicy of(int maxAttempts, long delayMillis) {
        return new RetryPolicy(maxAttempts, delayMillis);
    }
    
    /**
     * Crea una política sin reintentos (solo un intento).
     * 
     * @return una RetryPolicy con maxAttempts = 1
     */
    public static RetryPolicy noRetry() {
        return new RetryPolicy(1, 0);
    }
    
    /**
     * Crea una política de reintentos por defecto.
     * 
     * <p>Configuración por defecto: 3 intentos con 1 segundo (1000ms) de espera.</p>
     * 
     * @return una RetryPolicy con configuración por defecto
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, 1000);
    }
    
    /**
     * Verifica si se deben realizar reintentos.
     * 
     * @return true si maxAttempts > 1, false en caso contrario
     */
    public boolean shouldRetry() {
        return maxAttempts > 1;
    }
    
    /**
     * Obtiene el número de reintentos (maxAttempts - 1).
     * 
     * @return el número de reintentos posibles
     */
    public int getRetryCount() {
        return maxAttempts - 1;
    }
    
    /**
     * Obtiene el delay para un intento específico.
     * 
     * <p>En la política básica, todos los reintentos tienen el mismo delay.</p>
     * 
     * @param attempt el número de intento (empezando en 1)
     * @return el delay en milisegundos para este intento
     */
    public long getDelayForAttempt(int attempt) {
        return attempt > 1 ? delayMillis : 0;
    }
}
