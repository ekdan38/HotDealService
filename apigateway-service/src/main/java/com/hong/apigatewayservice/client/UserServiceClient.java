package com.hong.apigatewayservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class UserServiceClient {
    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://127.0.0.1:8080/user-service/user-service").build();
    }

    public Mono<Boolean> validateUser(String username){
        // URL 안전한 Base64 인코딩 적용
        String encodedUsername = Base64.getUrlEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8));
        System.out.println("Encoded Username: " + encodedUsername);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/users/validate/{username}").build(encodedUsername))
                .retrieve()
                .bodyToMono(Boolean.class)
                .doOnError(e -> System.err.println("Error during WebClient call: " + e.getMessage()));
    }
}
