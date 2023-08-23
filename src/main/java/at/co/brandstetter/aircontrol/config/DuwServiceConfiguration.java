package at.co.brandstetter.aircontrol.config;

import at.co.brandstetter.aircontrol.driver.Modbus;
import at.co.brandstetter.aircontrol.model.RegisterRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DuwServiceConfiguration {

    @Bean
    public Modbus modbus (RegisterRepository registerRepository, RegisterConfiguration registerConfiguration) {
        return new Modbus(registerRepository, registerConfiguration);
    }
}
