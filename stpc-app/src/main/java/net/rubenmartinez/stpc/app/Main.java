package net.rubenmartinez.stpc.app;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * 
 * net.rubenmartinez.stpc.app.controller
 * net.rubenmartinez.stpc.app.websocket
 * 
 * @author Rubén Martínez {@literal <ruben.martinez.olivares@gmail.com>}
 */
@SpringBootApplication
public class Main implements ApplicationListener<ApplicationReadyEvent> {
	
	@Value("${server.port}")
	private String serverPort;

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@PostConstruct
	public void init() {
		LOGGER.info("Initialization in progress. Please wait...");
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		logBanner();
	}
	
	private void logBanner() {
		final String lineSep = System.getProperty("line.separator"); 
		
		LOGGER.info("{}{}{}=================================================================================="
				   + "{}>>>>>>>>>>>>>>>>>>>> Server ready at http://127.0.0.1:{}/ <<<<<<<<<<<<<<<<<<<<"
				   + "{}=================================================================================={}{}", lineSep, lineSep, lineSep, lineSep, serverPort, lineSep, lineSep, lineSep);
	}
}