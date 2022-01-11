package de.apwolf.log4shell.victim;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * A simple web service which just returns "ok" everytime you call it. However, it actually logs some suff it shouldn't...
 */
public class VictimWebServer {

    public static final int PORT = 8080;

    private static final Logger LOGGER = LogManager.getLogger(VictimWebServer.class);

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::handle);
        server.start();
        LOGGER.info("Started Victim HTTP Server on localhost:" + PORT);
    }

    private void handle(HttpExchange e) {
        try {
            // Ladies and gentlemen, the actual attack vector: the "User-Agent" header can be randomly set by the
            // attacker, so this is the place we will put our JNDI lookup string
            LOGGER.info("Agent " + e.getRequestHeaders().getFirst("User-Agent") + " is calling us via "
                    + e.getRequestMethod() + " " + e.getRequestURI());


            byte[] payload = "ok".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(200, payload.length);
            OutputStream os = e.getResponseBody();
            os.write(payload);
            os.close();
        } catch (Exception exception) {
            LOGGER.error(e);
            throw new IllegalStateException(exception);
        }
    }
}
