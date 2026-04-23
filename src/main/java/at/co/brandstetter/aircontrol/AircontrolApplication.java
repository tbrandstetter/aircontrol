package at.co.brandstetter.aircontrol;

import at.co.brandstetter.aircontrol.driver.ConnectionSupervisor;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableScheduling
public class AircontrolApplication {

    public static void main(String[] args) {
        SpringApplication.run(AircontrolApplication.class, args);
    }

}

@Component
class MyApplicationListener
        implements ApplicationListener<ApplicationReadyEvent> {

    private final ConnectionSupervisor connectionSupervisor;

    Logger logger = (Logger) LoggerFactory.getLogger(MyApplicationListener.class);

    MyApplicationListener(ConnectionSupervisor connectionSupervisor) {
        this.connectionSupervisor = connectionSupervisor;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        connectionSupervisor.start();

        logger.info("Starting communication with Drexel & Weiss device");

    }
}
