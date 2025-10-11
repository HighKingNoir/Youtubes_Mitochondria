package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()
public class RateLimit {
    @Id
    private String id;
    private int tokens;
    private int capacity;
    private long lastRefill;
    private long refillIntervalMs;
    private long lastUpdated;
}
