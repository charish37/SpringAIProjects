package com.program.ai.functions.controller;

import com.program.ai.functions.services.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> payload) {
        String userId = payload.getOrDefault("userId", "1");
        String message = payload.get("message");
        String orderId = payload.get("orderId");

        if ((message == null || message.isBlank()) && orderId != null && !orderId.isBlank()) {
            message = "order " + orderId;
        } else if (orderId != null && !orderId.isBlank() && (message == null || !message.toLowerCase().contains("order"))) {
            message = (message == null ? "" : message.trim()) + " order " + orderId;
        }

        String reply = chatService.handleUserMessage(userId, message);
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
