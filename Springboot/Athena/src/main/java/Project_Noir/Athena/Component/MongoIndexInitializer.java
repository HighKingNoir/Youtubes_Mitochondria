package Project_Noir.Athena.Component;

import Project_Noir.Athena.Model.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoIndexInitializer implements ApplicationListener<ApplicationReadyEvent> {
    private final MongoTemplate mongoTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Expire after 2 hours (7200 seconds)
        Index index = new Index()
                .on("lastUpdated", Sort.Direction.ASC)
                .expire(7200);

        mongoTemplate.indexOps(RateLimit.class).ensureIndex(index);
    }
}
