# log4shell Proof of Concept
This is a very simple PoC of the log4shell attack. All components required are written in plain old Java, 
only the LDAP server uses a simple 3rd party library.

Be advised that some antivirus scanners are already blocking the Log4j execution, so don't be alarmed if your scanner
goes off. This code is safe - trust me. Or don't, you can easily check yourself.

To execute, just run the _de.apwolf.log4shell.Main_ class in your IDE or use Maven:
1. mvn clean package
2. java -jar target/log4shell-poc-0.0.1-SNAPSHOT-jar-with-dependencies.jar

Thanks to [mbechler](https://github.com/mbechler) for his support in debugging the LDAP configuration.

## But... why the 1000th implementation of this security issue?
I wanted to try it myself from start to finish, and every setup I found uses some library or tools or other programming
languages to show the attack.

This PoC is self-contained, uses *Java only* and reduces the code to understand the attack to a minimum.

Honestly, the only tricky part is getting the Java Classloader to accept the class file from a remote system. This is
pretty much not documented at all. Check _AttackerLdapServer#AttackInterceptor#sendResult_ for the LDAP configuration,
make sure you compiled your payload class properly and include it in a Jar (beware of Java packages), and you're done.

## This setup provides:
1. A victim web server which uses Log4j to log incoming http requests (see _VictimWebService.java_)
2. An HTTP request calling the victim web service and requesting to load a class from an LDAP server (see _Main.java#attack()_)
3. An LDAP server providing a link to the attackers' web server (see _AttackerLdapServer.java_)
4. A web server providing a Jar containing the actual attack payload (see _AttackerWebService.java_)
5. A class containing an example attack (see _Attack.java_)

## The attack goes like this (please be advised there are a hundred better explanations out there):
1. Victim web server is up and running
2. Attacker web server and attacker LDAP server are up and running
3. Attacker calls victim web service
4. Victim wants to log the request and stumbles upon the JNDI lookup in the request
5. Victim web service calls up attacker LDAP server to ask for the Jar containing the class requested by the attacker
6. Attacker LDAP server returns download link pointing to attacker web server
7. Victim web service downloads Jar from attacker web server
8. Victim automatically initializes the class requested by the attacker thanks to the static code block in the attack class
9. The actual attack happens (whatever is written in the static code block)
10. Victim finally writes the log entry it wanted to write in step 4 - if the JVM is still running after the attack

## Developer infos
* If you want to see debug output for log4j, use this VM parameter: -Dlog4j.debug
* If you want to see debug output for the JNDI class loading mechanism, use these VM parameters:
    * -Dsun.misc.URLClassPath.debug=true 
    * -Dsun.misc.URLClassPath.debugLookupCache=true
* I omitted using Java packages for the Attack class, if you want to use them the lookup code gets a bit more complex
* I used jdk1.8.0_151 for Windows and it is vulnerable - if the attack doesn't work, check your JDK version. 
Maybe one day log4j might even backport the fix to older versions like they did before and this attack won't work at all anymore.

## Enough, I want to attack stuff myself!
Easy: 
1. Just write whatever you want in the static code block of the _Attack.java_ class 
2. Recompile it: _javac Attack.java_
3. Add it to the _Attack.jar_ library
4. Profit!
