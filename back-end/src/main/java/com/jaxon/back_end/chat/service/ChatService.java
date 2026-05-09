package com.jaxon.back_end.chat.service;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jaxon.back_end.chat.dto.AgentChatMessage;
import com.jaxon.back_end.chat.dto.ChatMessageDTO;
import com.jaxon.back_end.chat.dto.ChatSessionDTO;
import com.jaxon.back_end.chat.dto.SendMessageRequest;
import com.jaxon.back_end.chat.dto.SendMessageResponse;
import com.jaxon.back_end.chat.entity.ChatMessage;
import com.jaxon.back_end.chat.entity.ChatSession;
import com.jaxon.back_end.chat.mapper.ChatMessageMapper;
import com.jaxon.back_end.chat.mapper.ChatSessionMapper;
import com.jaxon.back_end.common.login.LoginUser;



@Service
public class ChatService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private PythonAgentService pythonAgentService;

    private static final String DEFAULT_AGENT_SYSTEM_PROMPT = "你是一个简洁、专业的企业办公助手，请始终使用中文回答。";
    private static final String DEFAULT_MESSAGE_TYPE = "TEXT";
    private static final String AGENT_UNAVAILABLE_MESSAGE = "模型服务暂时不可用，请稍后重试。";

    public List<ChatMessageDTO> findBySessionId(Long sessionId){
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        requireOwnedSession(sessionId);

        return chatMessageMapper.findBySessionId(sessionId);
    }

    public List<ChatSessionDTO> findCurrentUserSessions(){
        LoginUser currentUser = getCurrentLoginUser();
        return chatSessionMapper.findByUserIdAndUserType(currentUser.getUserId(), currentUser.getUserType());
    }

    public ChatSessionDTO insertNewSession(){
        LoginUser currentUser = getCurrentLoginUser();
        ChatSession newSession = new ChatSession();
        newSession.setUserId(currentUser.getUserId());
        newSession.setUserType(currentUser.getUserType());
        newSession.setTitle("新建会话");
        int affectedRows = chatSessionMapper.insertNewSession(newSession);
        if (affectedRows != 1 || newSession.getId() == null) {
            throw new IllegalStateException("Failed to create chat session");
        }

        ChatSessionDTO chatSessionDTO = new ChatSessionDTO();
        chatSessionDTO.setId(newSession.getId());
        chatSessionDTO.setTitle(newSession.getTitle());
        chatSessionDTO.setCreateTime(resolveCreateTime(newSession.getCreateTime()));
        return chatSessionDTO;
    }

    @Transactional
    public void deleteSession(Long sessionId){
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        requireOwnedSession(sessionId);
        chatSessionMapper.deleteSession(sessionId);
        chatMessageMapper.deleteSessionMessages(sessionId);
    }

    public void resetTitle(String newTitle, Long sessionId){
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        requireOwnedSession(sessionId);
        chatSessionMapper.resetTitle(newTitle, sessionId);
    }

    public SendMessageResponse sendMessage(SendMessageRequest request){
        validateSendMessageRequest(request);
        requireOwnedSession(request.getSessionId());

        String normalizedMessageType = normalizeMessageType(request.getMessageType());
        String normalizedContent = request.getContent().trim();

        ChatMessageDTO userMessage = saveMessage(
                request.getSessionId(),
                "USER",
                normalizedMessageType,
                normalizedContent);

        List<ChatMessageDTO> historyMessages = chatMessageMapper.findBySessionId(request.getSessionId());

        ChatMessageDTO agentMessage;
        try {
            String agentReply = pythonAgentService.generateReply(
                    request.getSessionId(),
                    buildAgentMessages(historyMessages));
            agentMessage = saveMessage(
                    request.getSessionId(),
                    "AGENT",
                    DEFAULT_MESSAGE_TYPE,
                    agentReply);
        } catch (Exception ex) {
            agentMessage = saveMessage(
                    request.getSessionId(),
                    "SYSTEM",
                    DEFAULT_MESSAGE_TYPE,
                    AGENT_UNAVAILABLE_MESSAGE);
        }

        SendMessageResponse response = new SendMessageResponse();
        response.setUserMessage(userMessage);
        response.setAgentMessage(agentMessage);
        return response;
    }

    private LoginUser getCurrentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Current user is not authenticated");
        }
        if (!(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new AccessDeniedException("Current principal is not a valid login user");
        }
        return loginUser;
    }

    private LocalDateTime resolveCreateTime(LocalDateTime createTime) {
        return createTime != null ? createTime : LocalDateTime.now();
    }

    private ChatSession requireOwnedSession(Long sessionId) {
        LoginUser currentUser = getCurrentLoginUser();
        ChatSession chatSession = chatSessionMapper.selectById(sessionId);
        if (chatSession == null) {
            throw new IllegalArgumentException("Chat session does not exist");
        }
        if (!chatSession.getUserId().equals(currentUser.getUserId())
                || !chatSession.getUserType().equals(currentUser.getUserType())) {
            throw new AccessDeniedException("You do not have permission to access this session");
        }
        return chatSession;
    }

    private void validateSendMessageRequest(SendMessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Send message request must not be null");
        }
        if (request.getSessionId() == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    private String normalizeMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return DEFAULT_MESSAGE_TYPE;
        }

        return messageType.trim().toUpperCase();
    }

    private ChatMessageDTO saveMessage(Long sessionId, String senderType, String messageType, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setSenderType(senderType);
        chatMessage.setMessageType(messageType);
        chatMessage.setContent(content);

        int affectedRows = chatMessageMapper.insertMessage(chatMessage);
        if (affectedRows != 1 || chatMessage.getId() == null) {
            throw new IllegalStateException("Failed to persist chat message");
        }

        ChatMessageDTO chatMessageDTO = new ChatMessageDTO();
        chatMessageDTO.setId(chatMessage.getId());
        chatMessageDTO.setSenderType(chatMessage.getSenderType());
        chatMessageDTO.setMessageType(chatMessage.getMessageType());
        chatMessageDTO.setContent(chatMessage.getContent());
        chatMessageDTO.setCreateTime(resolveCreateTime(chatMessage.getCreateTime()));
        return chatMessageDTO;
    }

    private List<AgentChatMessage> buildAgentMessages(List<ChatMessageDTO> historyMessages) {
        List<AgentChatMessage> agentMessages = new ArrayList<>();
        agentMessages.add(buildAgentMessage("system", DEFAULT_AGENT_SYSTEM_PROMPT));

        for (ChatMessageDTO historyMessage : historyMessages) {
            String role = mapSenderTypeToAgentRole(historyMessage.getSenderType());
            if (role == null) {
                continue;
            }

            agentMessages.add(buildAgentMessage(role, historyMessage.getContent()));
        }

        return agentMessages;
    }

    private AgentChatMessage buildAgentMessage(String role, String content) {
        AgentChatMessage message = new AgentChatMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private String mapSenderTypeToAgentRole(String senderType) {
        if (senderType == null || senderType.isBlank()) {
            return null;
        }

        String normalizedType = senderType.trim().toUpperCase();
        if ("USER".equals(normalizedType)) {
            return "user";
        }
        if ("AGENT".equals(normalizedType)) {
            return "assistant";
        }

        return null;
    }



}
