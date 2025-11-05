package com.program.ai.functions.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OrderTools {
    private final WebClient webClient;

    public OrderTools(WebClient.Builder webClientBuilder){
        this.webClient = webClientBuilder.baseUrl("https://fakestoreapi.com").build();
    }

    @Tool(description="Get order details by order id from the mock store. Returns JSON with status and items.")
    public Mono<String> getOrderById(@ToolParam(description = "Numeric order ID") Integer orderId) {
        return webClient.get()
                .uri("/carts/{id}", orderId)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("{\"error\": \"failed to fetch order: " + ex.getMessage() + "\"}"));
    }

    @Tool(description = "Search latest order for a user id (fake store). Returns Order summary.")
    public Mono<String> getLatestOrderForUser(@ToolParam(description = "Numeric user ID") Integer userId){
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/carts/user/{userId}").build(userId))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("{\"error\": \"failed to fetch latest order for user: " + ex.getMessage() + "\"}"));
    }
}
