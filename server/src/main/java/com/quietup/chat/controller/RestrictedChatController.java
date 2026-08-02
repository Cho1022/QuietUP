package com.quietup.chat.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quietup.chat.dto.ChatMessageHistoryResponse;
import com.quietup.chat.dto.ChatMessageRequest;
import com.quietup.chat.dto.ChatMessageResponse;
import com.quietup.chat.dto.ChatRoomCreationResult;
import com.quietup.chat.dto.ChatRoomDetailResponse;
import com.quietup.chat.dto.ChatRoomResponse;
import com.quietup.chat.dto.ChatRoomSummaryResponse;
import com.quietup.chat.service.RestrictedChatService;

@RestController
@RequestMapping("/api/v1")
public class RestrictedChatController {

    private final RestrictedChatService restrictedChatService;

    public RestrictedChatController(RestrictedChatService restrictedChatService) {
        this.restrictedChatService = restrictedChatService;
    }

    @PostMapping("/noise-alerts/{noiseAlertId}/chat-room")
    public ResponseEntity<ChatRoomResponse> createRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long noiseAlertId) {
        ChatRoomCreationResult result = restrictedChatService.createRoom(jwt.getSubject(), noiseAlertId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.chatRoom());
    }

    @GetMapping("/chat-rooms")
    public List<ChatRoomSummaryResponse> rooms(@AuthenticationPrincipal Jwt jwt) {
        return restrictedChatService.getRooms(jwt.getSubject());
    }

    @GetMapping("/chat-rooms/{chatRoomId}")
    public ChatRoomDetailResponse room(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long chatRoomId) {
        return restrictedChatService.getRoom(jwt.getSubject(), chatRoomId);
    }

    @PostMapping("/chat-rooms/{chatRoomId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long chatRoomId,
            @RequestBody ChatMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restrictedChatService.sendMessage(jwt.getSubject(), chatRoomId, request));
    }

    @GetMapping("/chat-rooms/{chatRoomId}/messages")
    public ChatMessageHistoryResponse messages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long afterMessageId,
            @RequestParam(defaultValue = "50") int size) {
        return restrictedChatService.getMessages(jwt.getSubject(), chatRoomId, afterMessageId, size);
    }

    @PostMapping("/chat-rooms/{chatRoomId}/close")
    public ResponseEntity<Void> closeRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long chatRoomId) {
        restrictedChatService.closeRoom(jwt.getSubject(), chatRoomId);
        return ResponseEntity.noContent().build();
    }
}
