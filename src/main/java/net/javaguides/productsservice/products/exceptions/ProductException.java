package net.javaguides.productsservice.products.exceptions;


import net.javaguides.productsservice.products.enums.ProductErrors;
import org.springframework.lang.Nullable;

/**
 * ProductException
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 28/02/2026 - 12:04
 * @since 1.17
 */
public class ProductException extends Exception {

    private final ProductErrors productErrors;

    @Nullable
    private final String productId;


    public ProductException(ProductErrors productErrors, @Nullable String productId) {
        this.productErrors = productErrors;
        this.productId = productId;
    }

    public ProductErrors getProductErrors() {
        return productErrors;
    }

    @Nullable
    public String getProductId() {
        return productId;
    }
}
