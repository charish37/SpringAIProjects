package com.program.dad_joke.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.program.dad_joke.dto.JokeResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JokeController {
    private final ChatClient chatClient;

    // Fix malformed @Value (close the placeholder) and provide a safe default using StringTemplate placeholders
    public JokeController(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }

    @GetMapping("/joke")
    public JokeResponse getJoke(@RequestParam String subject) {
        BeanOutputConverter<JokeResponse> converter = new BeanOutputConverter<>(JokeResponse.class);
        String format = converter.getFormat();

        String userMsg = """
                Tell me a dad joke about {subject} and return in the format of {format}.
                """;
        PromptTemplate template = new PromptTemplate(userMsg);
        Prompt prompt = template.create(Map.of("subject", subject, "format", format ));

        ChatResponse res = chatClient.prompt(prompt).call().chatResponse();

       return converter.convert(res.getResult().getOutput().getText());
    }
}
