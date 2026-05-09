package com.jaxon.back_end.chat.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.jaxon.back_end.chat.dto.AgentReplyRequest;
import com.jaxon.back_end.chat.dto.AgentReplyResponse;

@Service
public class PythonAgentService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${agent.base-url:http://127.0.0.1:8000}")
    private String agentBaseUrl;

    public String generateReply(Long sessionId, List<com.jaxon.back_end.chat.dto.AgentChatMessage> messages) {
        AgentReplyRequest request = new AgentReplyRequest();
        request.setSessionId(sessionId);
        request.setMessages(messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AgentReplyRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AgentReplyResponse> response = restTemplate.postForEntity(
                    agentBaseUrl + "/chat/reply",
                    entity,
                    AgentReplyResponse.class);

            AgentReplyResponse body = response.getBody();
            if (body == null || body.getContent() == null || body.getContent().isBlank()) {
                throw new IllegalStateException("Python agent returned empty content");
            }

            return body.getContent().trim();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to call Python agent service", ex);
        }
    }
}
