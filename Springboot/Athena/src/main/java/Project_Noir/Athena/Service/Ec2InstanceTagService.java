package Project_Noir.Athena.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class Ec2InstanceTagService {

    private final Environment environment;
    private boolean isEc2 = false;
    private Ec2Client ec2Client;
    private String instanceId;

    @PostConstruct
    public void init() {
        try {
            String activeProfile = environment.getProperty("spring.profiles.active");
            if (!"prod".equalsIgnoreCase(activeProfile)) {
                log.info("Non-prod profile detected, skipping EC2 tag service initialization.");
                return;
            }
            instanceId = getInstanceMetadata("http://169.254.169.254/latest/meta-data/instance-id");
            String region = getInstanceMetadata("http://169.254.169.254/latest/dynamic/instance-identity/document")
                    .split("\"region\"\\s*:\\s*\"")[1].split("\"")[0];

            ec2Client = Ec2Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            isEc2 = true;
            log.info("Initialized EC2 Tag Service - Instance ID: {}, Region: {}", instanceId, region);
        } catch (Exception e) {
            log.error("Failed to initialize EC2 Tag Service", e);
        }
    }

    public void markTransactionInProgress() {
        if (!isEc2) {
            return;
        }
        createTag();
    }

    public void clearTransactionTag() {
        if (!isEc2) {
            return;
        }
        deleteTag();
    }

    private void createTag() {
        try {
            Tag tag = Tag.builder().key("tx-active").value("true").build();

            CreateTagsRequest request = CreateTagsRequest.builder()
                    .resources(instanceId)
                    .tags(tag)
                    .build();

            ec2Client.createTags(request);
            log.info("Set EC2 tag: {}={}", "tx-active", "true");
        } catch (Ec2Exception e) {
            log.error("Failed to create EC2 tag", e);
        }
    }

    private void deleteTag() {
        try {
            Tag tag = Tag.builder().key("tx-active").build();

            DeleteTagsRequest request = DeleteTagsRequest.builder()
                    .resources(instanceId)
                    .tags(tag)
                    .build();

            ec2Client.deleteTags(request);
            log.info("Deleted EC2 tag: {}", "tx-active");
        } catch (Ec2Exception e) {
            log.error("Failed to delete EC2 tag", e);
        }
    }

    private String getInstanceMetadata(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
