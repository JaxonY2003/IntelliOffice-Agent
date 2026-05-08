package com.jaxon.back_end.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.back_end.chat.dto.ChatMessageDTO;
import com.jaxon.back_end.chat.dto.ChatSessionDTO;
import com.jaxon.back_end.chat.service.ChatService;
import com.jaxon.back_end.common.result.Result;


import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/messages")
    public Result<List<ChatMessageDTO>> findBySessionId(@RequestParam Long sessionId){
        return Result.ok(chatService.findBySessionId(sessionId));
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionDTO>> findCurrentUserSessions(){
        return Result.ok(chatService.findCurrentUserSessions());
    }
}
