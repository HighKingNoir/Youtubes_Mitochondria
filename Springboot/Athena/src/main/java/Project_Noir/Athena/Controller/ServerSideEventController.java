package Project_Noir.Athena.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/sse")
public class ServerSideEventController {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Timer timer = new Timer();
    private final ObjectMapper objectMapper = new ObjectMapper();
    public Double latestValue;
    @Value("${spring.profiles.active}")
    private String environment;

    @GetMapping("/manaPrice")
    public SseEmitter getManaPrice(@RequestParam String sessionId) {
        SseEmitter emitter = new SseEmitter(7_200_000L);

        // Set up lifecycle handlers
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onError((e) -> emitters.remove(sessionId));
        emitter.onTimeout(() -> {
            emitters.remove(sessionId);
            emitter.complete();
        });
        emitters.put(sessionId, emitter);

        // Send latest value right away
        if (latestValue != null) {
            try {
                emitter.send(SseEmitter.event().data(latestValue));
            } catch (IOException e) {
                emitters.remove(sessionId); // Failsafe
            }
        }

        return emitter;
    }



    @Scheduled(fixedRate = 10000)
    public void UpdateManaPrice() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url("https://api.coinbase.com/v2/prices/MANA-USD/spot")
                .method("GET", null)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            JsonNode jsonResponse = objectMapper.readTree(response.body().string());
            String amount = jsonResponse.path("data").path("amount").asText();
            latestValue = Double.valueOf(amount);
            if ("dev".equals(environment)) {
                latestValue = 0.45887321;
            }

            List<String> deadEmitters = new ArrayList<>();
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                try {
                    entry.getValue().send(SseEmitter.event().data(latestValue));
                } catch (IOException e) {
                    deadEmitters.add(entry.getKey());
                }
            }

            deadEmitters.forEach(emitters::remove);
        }
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.badRequest().build(); // or just .ok()
        }
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
        }
        return ResponseEntity.ok().build();
    }

    @PreDestroy
    public void cleanup() {
        timer.cancel();
        for (SseEmitter emitter : emitters.values()) {
            emitter.complete();
        }
        emitters.clear();
    }
}
