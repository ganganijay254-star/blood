package com.bloodcare.bloodcare.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bloodcare.bloodcare.service.ChatbotService;
import com.bloodcare.bloodcare.entity.ChatbotMessage;
import jakarta.servlet.http.HttpSession;
import com.bloodcare.bloodcare.entity.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    /**
     * Send a message to the chatbot
     */
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        try {
            User user = (User) session.getAttribute("user");
            
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(new HashMap<String, String>() {{
                        put("error", "Please login first");
                    }});
            }

            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new HashMap<String, String>() {{
                        put("error", "Message cannot be empty");
                    }});
            }

            // Process the message
            String response = chatbotService.processChatMessage(user.getEmail(), message);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", message);
            result.put("response", response);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get chat history for the logged-in user
     */
    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(new HashMap<String, String>() {{
                        put("error", "Please login first");
                    }});
            }

            List<ChatbotMessage> history = chatbotService.getChatHistory(user.getEmail());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("history", history);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Quick query endpoint for instant responses
     */
    @PostMapping("/quick")
    public ResponseEntity<?> quickQuery(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        try {
            User user = (User) session.getAttribute("user");
            
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(new HashMap<String, String>() {{
                        put("error", "Please login first");
                    }});
            }

            String query = request.get("query");
            String response = chatbotService.processChatMessage(user.getEmail(), query);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("response", response);
            }});

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new HashMap<String, String>() {{
                    put("error", e.getMessage());
                }});
        }
    }
}
