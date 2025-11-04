package com.program.ai.functions.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PolicyTools {
    private final WebClient webClient;

    public PolicyTools(WebClient.Builder builder){
        this.webClient = builder.baseUrl("https://github.com/charish37/SpringAIProjects/blob/master/Functions/policy.json").build();
    }

    @Tool(description="Retrieve policy by short name (returns the policy text).")
    public String getPolicyByName(@ToolParam(description = "policy key e.g., 'refund', 'shipping'") String policyKey){
        try {
            String path = "/" + policyKey + ".md";
            return webClient.get().uri(path).retrieve().bodyToMono(String.class).block();
        } catch (Exception e){
            return "{\"error\":\"failed to fetch policy: " + e.getMessage() + "\"}";
        }
    }
}
