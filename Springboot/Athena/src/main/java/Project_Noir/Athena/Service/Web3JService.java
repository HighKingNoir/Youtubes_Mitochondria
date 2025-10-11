package Project_Noir.Athena.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
@Service
@Slf4j
@RequiredArgsConstructor
public class Web3JService {

    @Value("${infura.api.secret}")
    private String infuraAPISecret;

    @Value("${infura.api.key}")
    private String infuraAPIKey;

    public Web3j web3j;

    @PostConstruct
    public void init() {
        if (infuraAPIKey != null && !infuraAPIKey.isEmpty()) {
            web3j = Web3j.build(createCustomHttpService("https://polygon-mainnet.infura.io/v3/" + infuraAPIKey));
        } else {
            web3j = Web3j.build(new HttpService());
        }
    }

    private HttpService createCustomHttpService(String url) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        // Add an interceptor to add the Bearer token to each request
        clientBuilder.addInterceptor(chain -> {
            okhttp3.Request original = chain.request();
            okhttp3.Request request = original.newBuilder()
                    .header("Authorization", Credentials.basic(infuraAPIKey, infuraAPISecret))
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });
        return new HttpService(url, clientBuilder.build());
    }
}
