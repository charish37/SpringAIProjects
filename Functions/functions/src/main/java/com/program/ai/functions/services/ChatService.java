package com.program.ai.functions.services;

import com.program.ai.functions.tools.OrderTools;
import com.program.ai.functions.tools.PolicyTools;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {
    private final OrderTools orderTools;
    private  final PolicyTools policyTools;

    private final Pattern orderIdPattern = Pattern.compile("\\b(?:order|order id|order#|order#?)\\s*(?:#|id)?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern latestOrderForUserPattern = Pattern.compile("\\b(?:latest order for user|latest order user|user)\\s*(?:id)?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern policyPattern = Pattern.compile("\\bpolicy(?: about)?\\s+(\\w+)\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern anyDigitsPattern = Pattern.compile("(\\d+)");

    public ChatService(OrderTools orderTools, PolicyTools policyTools){
        this.orderTools = orderTools;
        this.policyTools = policyTools;
    }

    // New: direct helper to fetch order by id when controller receives orderId explicitly
    public Mono<String> getOrderByIdDirect(Integer orderId) {
        return orderTools.getOrderById(orderId);
    }

    public Mono<String> handleUserMessage(String userId, String message){
        if(message == null || message.isBlank()) {
            return Mono.just("Please provide a valid message.");
        }

        // check for explicit order id
        Matcher m = orderIdPattern.matcher(message);
        if (m.find()) {
            String idStr = m.group(1);
            try {
                Integer orderId = Integer.valueOf(idStr);
                return orderTools.getOrderById(orderId);
            } catch (NumberFormatException ignored) {
                return Mono.just("Invalid order id format");
            }
        }

        // check for latest order for user
        m = latestOrderForUserPattern.matcher(message);
        if (m.find()) {
            String idStr = m.group(1);
            try {
                Integer uid = Integer.valueOf(idStr);
                return orderTools.getLatestOrderForUser(uid);
            } catch (NumberFormatException ignored) {
                return Mono.just("Invalid user id format");
            }
        }

        // check for policy requests
        m = policyPattern.matcher(message);
        if (m.find()) {
            String policyKey = m.group(1).toLowerCase();
            return policyTools.getPolicyByName(policyKey);
        }

        // fallback: if message mentions 'order' and contains any digits, try extracting digits
        if (message.toLowerCase().contains("order")) {
            Matcher digits = anyDigitsPattern.matcher(message);
            if (digits.find()) {
                String idStr = digits.group(1);
                try {
                    Integer orderId = Integer.valueOf(idStr);
                    return orderTools.getOrderById(orderId);
                } catch (NumberFormatException ignored) {
                    // fall through to helpful prompt
                }
            }

            return Mono.just("I detected a question about an order. Please include an order id like 'order 123'.");
        }
        if (message.toLowerCase().contains("policy")) {
            return Mono.just("I can fetch policies. Ask for e.g. 'policy refund' or 'policy shipping'.");
        }

        return Mono.just("This is a prototype response. For order queries include an order id, or ask for 'policy <key>'.");
    }
}
