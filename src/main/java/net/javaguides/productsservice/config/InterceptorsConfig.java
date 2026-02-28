package net.javaguides.productsservice.config;

import lombok.RequiredArgsConstructor;
import net.javaguides.productsservice.products.interceptors.ProductInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * InterceptorsConfig
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 28/02/2026 - 06:36
 * @since 1.17
 */
@Configuration
@RequiredArgsConstructor
public class InterceptorsConfig implements WebMvcConfigurer {

    private final ProductInterceptor productInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.productInterceptor)
                .addPathPatterns("/api/products/**"); // Aplica el interceptor a todas las rutas de productos
    }
}
