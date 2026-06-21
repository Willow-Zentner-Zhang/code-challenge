package interview.willow.codechallenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeChallengeApplication {

	static void main(final String[] args) {
		SpringApplication.run(CodeChallengeApplication.class, args);
	}

}
