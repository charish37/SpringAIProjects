package com.program.ai.functions.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PolicyTools {
    private final WebClient webClient;

    public PolicyTools(WebClient.Builder builder){
        // using a placeholder baseRaw URL; adjust to your actual raw file location if available
        this.webClient = builder.baseUrl("https://raw.githubusercontent.com/charish37/SpringAIProjects/master/Functions").build();
    }

    @Tool(description="Retrieve policy by short name (returns the policy text).")
    public Mono<String> getPolicyByName(@ToolParam(description = "policy key e.g., 'refund', 'shipping'") String policyKey){
        String path = "/" + policyKey + ".md";
        return webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("{\"error\": \"failed to fetch policy: " + e.getMessage() + "\"}"));
    }
}
