package net.javaguides.productsservice.products.interceptors;

/**
 * ProductInterceptor
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 28/02/2026 - 06:29
 * @since 1.17
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ProductInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1. Obtener el valor y verificar si es nulo o está vacío
        String requestId = ThreadContext.get("requestId");

        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = "no-request-id"; // Valor por defecto
        }

        ThreadContext.put("requestId", request.getHeader("requestId"));

        return true; // Devuelve true para continuar con la cadena de manejo
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        ThreadContext.clearAll(); // Limpia el contexto después de la solicitud
    }
}
