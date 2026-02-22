package net.javaguides.productsservice.config;

import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.spring.aop.BaseAbstractXRayInterceptor;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * XRayInspector
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 20/02/2026 - 20:47
 * @since 1.17
 */
@Aspect
@Component
public class XRayInspector extends BaseAbstractXRayInterceptor {

    @Override
    protected Map<String, Map<String, Object>> generateMetadata(
            ProceedingJoinPoint joinPoint, Subsegment subsegment
    ) {
        return super.generateMetadata(joinPoint, subsegment);
    }

    @Override
    @Pointcut("@within(com.amazonaws.xray.spring.aop.XRayEnabled)")
    protected void xrayEnabledClasses() {}
}
