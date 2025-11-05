package com.program.ai.functions.controller;

import com.program.ai.functions.services.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final ChatService chatService;
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final Pattern ORDER_WORD = Pattern.compile("\\border\\b", Pattern.CASE_INSENSITIVE);

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public Mono<ResponseEntity<Map<String, String>>> chat(@RequestBody Map<String, Object> payload) {
        // Coerce incoming payload values to string to handle numbers and strings
        String userId = payload.getOrDefault("userId", "1") == null ? "1" : payload.get("userId").toString();
        Object messageObj = payload.get("message");
        Object orderIdObj = payload.get("orderId");

        String message = messageObj == null ? null : messageObj.toString();
        String orderId = orderIdObj == null ? null : orderIdObj.toString();

        log.info("/api/chat received payload userId={}, orderId={}, message={}", userId, orderId, message);

        // If orderId is supplied, short-circuit and call the service directly using that id
        if (orderId != null && !orderId.isBlank()) {
            try {
                Integer oid = Integer.valueOf(orderId.trim());
                String finalMessageIfNull = (message == null || message.isBlank()) ? "order " + orderId : message;
                return chatService.getOrderByIdDirect(oid)
                        .map(reply -> ResponseEntity.ok(Map.of(
                                "reply", reply,
                                "finalMessage", finalMessageIfNull
                        )));
            } catch (NumberFormatException nfe) {
                // fall through to normal message parsing below
                log.warn("Invalid orderId provided: {}", orderId);
            }
        }

        // If no short-circuit, try to ensure message contains the id if supplied earlier
        if (orderId != null && !orderId.isBlank()) {
            if (message == null || message.isBlank()) {
                message = "order " + orderId;
            } else if (!message.contains(orderId)) {
                // Insert the id immediately after the first occurrence of the word "order" (case-insensitive)
                Matcher orderMatcher = ORDER_WORD.matcher(message);
                if (orderMatcher.find()) {
                    int insertPos = orderMatcher.end();
                    StringBuilder sb = new StringBuilder(message);
                    sb.insert(insertPos, " " + orderId);
                    message = sb.toString();
                } else {
                    message = message.trim() + " order " + orderId;
                }
            }
        }

        String finalMessage = message; // capture for closure
        log.info("/api/chat final message passed to ChatService: {}", finalMessage);

        return chatService.handleUserMessage(userId, finalMessage)
                .map(reply -> ResponseEntity.ok(Map.of(
                        "reply", reply,
                        "finalMessage", finalMessage == null ? "" : finalMessage
                )));
    }
}
