package net.javaguides.productsservice.products.exceptions;

import net.javaguides.productsservice.products.dto.ProductErrorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * ProductsExceptionHandler
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 28/02/2026 - 12:22
 * @since 1.17
 */
@RestControllerAdvice
public class ProductsExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(ProductsExceptionHandler.class);

    @ExceptionHandler(value = {ProductException.class})
    protected ResponseEntity<Object> handleProductException(ProductException productException, WebRequest request) {
        LOG.error("❌ - ProductException occurred: {}", productException.getProductErrors().getMessage());

        // 1. Obtener el valor y verificar si es nulo o está vacío
        String requestId = ThreadContext.get("requestId");

        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = "no-request-id"; // Valor por defecto
        }
        ProductErrorResponse productErrorResponse = new ProductErrorResponse(
                productException.getProductErrors().getMessage(),
                productException.getProductErrors().getHttpStatus().value(),
                ThreadContext.get("requestId"),
                productException.getProductId()
        );

        return handleExceptionInternal(
            productException,
            productErrorResponse,
                new HttpHeaders(),
                productException.getProductErrors().getHttpStatus(),
                request
        );
    }
}
