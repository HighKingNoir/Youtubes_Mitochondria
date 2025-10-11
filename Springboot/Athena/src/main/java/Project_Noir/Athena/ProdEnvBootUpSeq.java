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

    }
}
