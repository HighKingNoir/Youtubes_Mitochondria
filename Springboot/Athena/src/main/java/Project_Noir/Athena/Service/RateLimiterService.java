package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.RateLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.query.BasicUpdate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;


@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimiterService {
    private final MongoTemplate mongoTemplate;

    public boolean allowRequest(String key, int capacity, int refillTokens, long refillIntervalMs) {
        long now = System.currentTimeMillis();
        
        List<Bson> pipeline = Arrays.asList(
                // Set defaults only if fields are null (emulates $setOnInsert for pipeline)
                new Document("$set", new Document()
                        .append("capacity", new Document("$ifNull", Arrays.asList("$capacity", capacity)))
                        .append("refillIntervalMs", new Document("$ifNull", Arrays.asList("$refillIntervalMs", refillIntervalMs)))
                        .append("lastRefill", new Document("$ifNull", Arrays.asList("$lastRefill", now)))
                        .append("tokens", new Document("$ifNull", Arrays.asList("$tokens", capacity)))
                ),

                // The rest remains unchanged
                new Document("$set", new Document()
                        .append("elapsed", new Document("$subtract", Arrays.asList(now, "$lastRefill")))
                        .append("tokens", new Document("$cond", Arrays.asList(
                                new Document("$gte", Arrays.asList("$elapsed", refillIntervalMs)),
                                new Document("$min", Arrays.asList(
                                        "$capacity",
                                        new Document("$add", Arrays.asList(
                                                "$tokens",
                                                new Document("$multiply", Arrays.asList(
                                                        refillTokens,
                                                        new Document("$floor", new Document("$divide", Arrays.asList("$elapsed", refillIntervalMs)))
                                                ))
                                        ))
                                )),
                                "$tokens"
                        )))
                        .append("lastRefill", new Document("$cond", Arrays.asList(
                                new Document("$gte", Arrays.asList("$elapsed", refillIntervalMs)),
                                now,
                                "$lastRefill"
                        )))
                ),

                new Document("$set", new Document()
                        .append("tokens", new Document("$subtract", Arrays.asList("$tokens", 1)))
                        .append("lastUpdated", now)
                ),

                new Document("$set", new Document("tokens", new Document("$max", Arrays.asList("$tokens", 0))))
        );

        // Get the underlying collection for your RateLimit documents
        MongoCollection<Document> coll = mongoTemplate.getCollection(mongoTemplate.getCollectionName(RateLimit.class));

        // Build filter by _id
        Bson filter = Filters.eq("_id", key);

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER);

        // Run the pipeline update (native driver)
        Document result = coll.findOneAndUpdate(filter, pipeline, options);

        // Convert Document -> RateLimit using the MongoConverter so mapping rules are respected
        MongoConverter converter = mongoTemplate.getConverter();
        RateLimit entry = converter.read(RateLimit.class, result);

        // Decide whether to allow: require tokens > 0 (or >=0 depending on semantics)
        return entry.getTokens() > 0;
    }
}
