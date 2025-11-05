package com.program.ai.functions;

import com.program.ai.functions.services.WaitTimeService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Function;

@SpringBootApplication
public class FunctionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FunctionsApplication.class, args);
	}

	@Bean
	@Description("Get the wait time for a DisneyLand attraction in minutes")
	public Function<WaitTimeService.Request,WaitTimeService.Response> getWaitTime(){
		return new WaitTimeService();
	}

	@Bean
	ApplicationRunner go(ChatClient.Builder chatClientBuilder) {
		return args -> {
			UserMessage userMessage = new UserMessage("What's the wait time for Jungle Cruise?");

			// build the ChatClient from the injected builder, then call
			ChatClient chatClient = chatClientBuilder.build();

			Prompt prompt = new Prompt(List.of(userMessage));
			// This call runs at startup on the main thread; it's acceptable to call blocking here if desired
			ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

			System.out.println(response);
		};
	};

	@Bean
	RouterFunction<ServerResponse> routes(ChatClient.Builder builder){
		ChatClient chatClient = builder.build();

		return RouterFunctions.route()
				.GET("/waitTime", req -> {
					String ride = req.queryParam("ride").orElse("Space Mountain");

					UserMessage userMessage =
							new UserMessage("What's the wait time for " + ride + "?");

					Prompt prompt = new Prompt(List.of(userMessage), OpenAiChatOptions.builder()
							.toolNames("getWaitTime")
							.build());

					// Wrap the blocking ChatClient call in a Mono and run it off the event-loop
					Mono<ChatResponse> respMono = Mono.fromCallable(() -> chatClient.prompt(prompt).call().chatResponse())
							.subscribeOn(Schedulers.boundedElastic());

					return respMono.flatMap(response ->
							ServerResponse.ok().bodyValue(response.getResult().getOutput().getText())
					);
				}).build();
	}

}
