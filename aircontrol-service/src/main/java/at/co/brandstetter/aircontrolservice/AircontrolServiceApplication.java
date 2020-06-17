package at.co.brandstetter.aircontrolservice;

import at.co.brandstetter.aircontrolservice.driver.Modbus;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableCaching
public class AircontrolServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AircontrolServiceApplication.class, args);
    }

}

@Component
class MyApplicationListener
        implements ApplicationListener<ApplicationReadyEvent> {

    private final Modbus modbus;

    Logger logger = (Logger) LoggerFactory.getLogger(MyApplicationListener.class);

    MyApplicationListener(Modbus modbus) {
        this.modbus = modbus;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        modbus.open();

        logger.info("Starting communication with Drexel&Weiss device");

    }
}
