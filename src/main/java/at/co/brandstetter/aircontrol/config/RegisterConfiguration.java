package at.co.brandstetter.aircontrol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Data
@Configuration
@PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:register.yml")
@ConfigurationProperties(prefix = "duw")
public class RegisterConfiguration {

    private List<Register> register;

    @Data
    @Cacheable("register")
    public static class Register {
        private Integer id;
        private String description;
        private int min;
        private int max;
        private int divisor;
        private String access;
        private List<String> devices;
    }
}
