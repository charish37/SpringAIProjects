package com.program.ai.functions.services;

import com.program.ai.functions.tools.OrderTools;
import com.program.ai.functions.tools.PolicyTools;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {
    private final OrderTools orderTools;
    private  final PolicyTools policyTools;

    private final Pattern orderIdPattern = Pattern.compile("\\b(?:order|order id|order#|order#?)\\s*(?:#|id)?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern latestOrderForUserPattern = Pattern.compile("\\b(?:latest order for user|latest order user|user)\\s*(?:id)?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern policyPattern = Pattern.compile("\\bpolicy(?: about)?\\s+(\\w+)\\b", Pattern.CASE_INSENSITIVE);

    public ChatService(OrderTools orderTools, PolicyTools policyTools){
        this.orderTools = orderTools;
        this.policyTools = policyTools;
    }

    public String handleUserMessage(String userId, String message){
        if(message == null || message.isBlank()) {
            return "Please provide a valid message.";
        }

        // check for explicit order id
        Matcher m = orderIdPattern.matcher(message);
        if (m.find()) {
            String idStr = m.group(1);
            try {
                Integer orderId = Integer.valueOf(idStr);
                return orderTools.getOrderById(orderId);
            } catch (NumberFormatException ignored) {
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
            }
        }

        // check for policy requests
        m = policyPattern.matcher(message);
        if (m.find()) {
            String policyKey = m.group(1).toLowerCase();
            return policyTools.getPolicyByName(policyKey);
        }


        // fallback: simple echo/prototype
        if (message.toLowerCase().contains("order")) {
            return "I detected a question about an order. Please include an order id like 'order 123'.";
        }
        if (message.toLowerCase().contains("policy")) {
            return "I can fetch policies. Ask for e.g. 'policy refund' or 'policy shipping'.";
        }

        return "This is a prototype response. For order queries include an order id, or ask for 'policy <key>'.";
    }



}
