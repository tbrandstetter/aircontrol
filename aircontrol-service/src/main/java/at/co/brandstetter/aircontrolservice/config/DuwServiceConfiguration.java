package at.co.brandstetter.aircontrolservice.config;

import at.co.brandstetter.aircontrolservice.driver.Modbus;
import at.co.brandstetter.aircontrolservice.model.RegisterRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DuwServiceConfiguration {

    @Bean
    public Modbus modbus (RegisterRepository registerRepository, RegisterConfiguration registerConfiguration) {
        return new Modbus(registerRepository, registerConfiguration);
    }
}
