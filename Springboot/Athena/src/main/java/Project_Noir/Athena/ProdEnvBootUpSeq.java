package Project_Noir.Athena;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.HashMap;
import java.util.Map;

public class ProdEnvBootUpSeq implements EnvironmentPostProcessor {

    private static final String SECRET_ID = "prod/Sivantis";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String activeProfile = environment.getProperty("spring.profiles.active");

        // Only load secrets for the prod profile
        if (!"prod".equalsIgnoreCase(activeProfile)) {
            return;
        }

        try {
            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(Region.of("us-east-1")) // Set your AWS region
                    .build();

            String secretJson = client.getSecretValue(GetSecretValueRequest.builder()
                            .secretId(SECRET_ID)
                            .build())
                    .secretString();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> secretMap = mapper.readValue(secretJson, Map.class);

            // Dynamically create Mongo URI from secrets
            String mongoPassword = secretMap.get("mongo.password");
            if (mongoPassword != null) {
                String mongoUri = String.format(
                        "mongodb+srv://AdminNoir:%s@sivantis.cqja1.mongodb.net/?retryWrites=true&w=majority&appName=Sivantis",
                        mongoPassword
                );
                secretMap.put("spring.data.mongodb.uri", mongoUri);
            }

            Map<String, Object> resolvedSecrets = new HashMap<>(secretMap);

            // Precedence: add first to override application.properties
            environment.getPropertySources().addFirst(
                    new MapPropertySource("aws-secrets", resolvedSecrets)
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load secrets from AWS Secrets Manager", e);
        }
    }
}
