package at.co.brandstetter.aircontrol;

import at.co.brandstetter.aircontrol.driver.Modbus;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import javax.annotation.Nonnull;

@SpringBootApplication
@EnableCaching
public class AircontrolApplication {

    public static void main(String[] args) {
        SpringApplication.run(AircontrolApplication.class, args);
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
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {

        modbus.open();

        logger.info("Starting communication with Drexel & Weiss device");

    }
}
