package com.jaxon.back_end.chat.service;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.jaxon.back_end.chat.dto.ChatMessageDTO;
import com.jaxon.back_end.chat.dto.ChatSessionDTO;
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

    public List<ChatMessageDTO> findBySessionId(Long sessionId){
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        LoginUser currentUser = getCurrentLoginUser();
        ChatSession chatSession = chatSessionMapper.selectById(sessionId);
        if (chatSession == null) {
            throw new IllegalArgumentException("Chat session does not exist");
        }
        if (!chatSession.getUserId().equals(currentUser.getUserId())
                || !chatSession.getUserType().equals(currentUser.getUserType())) {
            throw new AccessDeniedException("You do not have permission to access this session");
        }

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



}
