package no.idporten.eudiw.demo.verifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.security.Security;

@ConfigurationPropertiesScan
@SpringBootApplication
public class VerifierDemoApplication {

	public static void main(String[] args) {
		addBouncyCastleProvider();
		SpringApplication.run(VerifierDemoApplication.class, args);
	}

	/**
	 * Bootstrap Bouncy Castle.
	 */
	private static void addBouncyCastleProvider() {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

}
