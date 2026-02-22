package net.javaguides.productsservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.strategy.sampling.CentralizedSamplingStrategy;
import java.io.FileNotFoundException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import jakarta.servlet.Filter;

/**
 * XRayConfig
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 20/02/2026 - 18:00
 * @since 1.17
 */
@Configuration
public class XRayConfig {

    private static final Logger LOG = LoggerFactory.getLogger(XRayConfig.class);

        public XRayConfig() {
            try {
                URL ruleFile = ResourceUtils.getURL("classpath:xray/xray-sampling-rules.json");

                AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                        .withDefaultPlugins()
                        .withSamplingStrategy(new CentralizedSamplingStrategy(ruleFile))
                        .build();

                AWSXRay.setGlobalRecorder(recorder);

                LOG.info("✅ - AWS X-Ray SDK for Java is available in the classpath");
            } catch (FileNotFoundException e) {
                LOG.warn("⚠️ - X-Ray sampling rules file not found, using default sampling rules");
            }
        }

        @Bean
        public Filter TracingFilter() {
            return new AWSXRayServletFilter("productsservice");
        }
}
