package Project_Noir.Athena.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/sse")
public class ServerSideEventController {
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Timer timer = new Timer();
    private final ObjectMapper objectMapper = new ObjectMapper();
    public Double latestValue;
    @Value("${spring.profiles.active}")
    private String environment;

    @GetMapping("/manaPrice")
    public SseEmitter getManaPrice() throws IOException {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.send(SseEmitter.event().data(latestValue));
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }



    @Scheduled(fixedRate = 10000) // Emit every 30 seconds
    public void UpdateManaPrice() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url("https://api.coinbase.com/v2/prices/MANA-USD/spot")
                .method("GET",null)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        if(response.isSuccessful() && response.body() != null){
            JsonNode jsonResponse = objectMapper.readTree(response.body().string());
            String amount = jsonResponse.path("data").path("amount").asText();
            latestValue = Double.valueOf(amount);
            if (environment.equals("dev")) {
                latestValue = .45887321;
            }
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().data(latestValue));
                } catch (Exception e) {
                    // Handle error or remove emitter from the list
                    emitters.remove(emitter);
                }
            }
        }
    }


    @PreDestroy
    public void cleanup() {
        timer.cancel();
        for (SseEmitter emitter : emitters) {
            emitter.complete();
        }
    }
}
