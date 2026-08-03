package com.quietup.chat.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.chat.dto.ChatMessageHistoryItemResponse;
import com.quietup.chat.dto.ChatMessageHistoryResponse;
import com.quietup.chat.dto.ChatMessageRequest;
import com.quietup.chat.dto.ChatMessageResponse;
import com.quietup.chat.dto.ChatMessageSenderRole;
import com.quietup.chat.dto.ChatRoomCreationResult;
import com.quietup.chat.dto.ChatRoomDetailResponse;
import com.quietup.chat.dto.ChatRoomResponse;
import com.quietup.chat.dto.ChatRoomSummaryResponse;
import com.quietup.chat.entity.ChatMessage;
import com.quietup.chat.entity.ChatRoom;
import com.quietup.chat.repository.ChatMessageRepository;
import com.quietup.chat.repository.ChatRoomRepository;
import com.quietup.global.error.ChatMessageInvalidException;
import com.quietup.global.error.ChatRequestRequiredException;
import com.quietup.global.error.ChatRoomClosedException;
import com.quietup.global.error.ChatRoomNotFoundException;
import com.quietup.global.error.ResidenceRequiredException;
import com.quietup.noise.entity.NoiseAlert;
import com.quietup.noise.entity.NoiseAlertResponse;
import com.quietup.noise.entity.ResponseType;
import com.quietup.noise.repository.NoiseAlertRepository;
import com.quietup.noise.repository.NoiseAlertResponseRepository;
import com.quietup.residence.entity.Residence;
import com.quietup.residence.repository.ResidenceRepository;

@Service
public class RestrictedChatService {

    private static final String ALERT_SENDER_LABEL = "알림을 보낸 이웃";
    private static final String ALERT_RECIPIENT_LABEL = "알림을 받은 이웃";

    private final ResidenceRepository residenceRepository;
    private final NoiseAlertRepository noiseAlertRepository;
    private final NoiseAlertResponseRepository noiseAlertResponseRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public RestrictedChatService(
            ResidenceRepository residenceRepository,
            NoiseAlertRepository noiseAlertRepository,
            NoiseAlertResponseRepository noiseAlertResponseRepository,
            ChatRoomRepository chatRoomRepository,
            ChatMessageRepository chatMessageRepository) {
        this.residenceRepository = residenceRepository;
        this.noiseAlertRepository = noiseAlertRepository;
        this.noiseAlertResponseRepository = noiseAlertResponseRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatRoomCreationResult createRoom(String subject, Long noiseAlertId) {
        Residence currentResidence = findCurrentResidence(subject);
        NoiseAlert noiseAlert = noiseAlertRepository.findByIdForUpdate(noiseAlertId)
                .orElseThrow(ChatRoomNotFoundException::new);

        if (!noiseAlert.getSenderResidence().getId().equals(currentResidence.getId())) {
            throw new ChatRoomNotFoundException();
        }
        if (noiseAlert.isResolved()) {
            throw new ChatRequestRequiredException();
        }

        NoiseAlertResponse request = noiseAlertResponseRepository.findByNoiseAlertId(noiseAlertId)
                .filter(response -> response.getResponseType() == ResponseType.REQUEST_CHAT)
                .orElseThrow(ChatRequestRequiredException::new);

        return chatRoomRepository.findByNoiseAlertIdForUpdate(noiseAlertId)
                .map(room -> new ChatRoomCreationResult(
                        toRoomResponse(room, currentResidence.getId()),
                        false))
                .orElseGet(() -> {
                    ChatRoom room = chatRoomRepository.saveAndFlush(new ChatRoom(
                            noiseAlert,
                            noiseAlert.getSenderResidence(),
                            request.getResponderResidence()));
                    return new ChatRoomCreationResult(
                            toRoomResponse(room, currentResidence.getId()),
                            true);
                });
    }

    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getRooms(String subject) {
        Residence currentResidence = findCurrentResidence(subject);
        return chatRoomRepository.findAllForParticipant(currentResidence.getId()).stream()
                .map(room -> toRoomSummary(room, currentResidence.getId()))
                .sorted(Comparator.comparing(this::activityAt).reversed()
                        .thenComparing(ChatRoomSummaryResponse::chatRoomId, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getRoom(String subject, Long chatRoomId) {
        Residence currentResidence = findCurrentResidence(subject);
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .filter(candidate -> candidate.isParticipant(currentResidence.getId()))
                .orElseThrow(ChatRoomNotFoundException::new);

        return new ChatRoomDetailResponse(
                room.getId(),
                room.getNoiseAlert().getId(),
                room.getStatus(),
                counterpartLabel(room, currentResidence.getId()),
                room.getNoiseAlert().getNoiseType(),
                room.getOpenedAt(),
                room.getClosedAt());
    }

    @Transactional
    public ChatMessageResponse sendMessage(
            String subject,
            Long chatRoomId,
            ChatMessageRequest request) {
        Residence currentResidence = findCurrentResidence(subject);
        ChatRoom room = findParticipantRoomForUpdate(chatRoomId, currentResidence.getId());
        if (!room.isOpen()) {
            throw new ChatRoomClosedException();
        }

        String content = normalizeContent(request.content());
        ChatMessage message = chatMessageRepository.saveAndFlush(new ChatMessage(
                room,
                currentResidence,
                content,
                LocalDateTime.now(ZoneOffset.UTC)));

        return new ChatMessageResponse(
                message.getId(),
                room.getId(),
                ChatMessageSenderRole.ME,
                message.getContent(),
                message.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ChatMessageHistoryResponse getMessages(
            String subject,
            Long chatRoomId,
            Long afterMessageId,
            int size) {
        if (size < 1 || size > 100 || (afterMessageId != null && afterMessageId < 1)) {
            throw new ChatMessageInvalidException();
        }

        Residence currentResidence = findCurrentResidence(subject);
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .filter(candidate -> candidate.isParticipant(currentResidence.getId()))
                .orElseThrow(ChatRoomNotFoundException::new);
        PageRequest page = PageRequest.of(0, size);
        List<ChatMessage> messages = afterMessageId == null
                ? chatMessageRepository.findByChatRoomIdOrderByIdAsc(chatRoomId, page)
                : chatMessageRepository.findByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        chatRoomId,
                        afterMessageId,
                        page);
        List<ChatMessageHistoryItemResponse> responseMessages = messages.stream()
                .map(message -> new ChatMessageHistoryItemResponse(
                        message.getId(),
                        senderRole(room, currentResidence.getId(), message.getSenderResidence().getId()),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
        Long nextCursor = messages.isEmpty() ? null : messages.getLast().getId();
        return new ChatMessageHistoryResponse(responseMessages, nextCursor);
    }

    @Transactional
    public void closeRoom(String subject, Long chatRoomId) {
        Residence currentResidence = findCurrentResidence(subject);
        ChatRoom room = findParticipantRoomForUpdate(chatRoomId, currentResidence.getId());
        room.close(LocalDateTime.now(ZoneOffset.UTC));
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, Long residenceId) {
        return new ChatRoomResponse(
                room.getId(),
                room.getNoiseAlert().getId(),
                room.getStatus(),
                counterpartLabel(room, residenceId),
                room.getOpenedAt());
    }

    private ChatRoomSummaryResponse toRoomSummary(ChatRoom room, Long residenceId) {
        ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(room.getId())
                .orElse(null);
        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getNoiseAlert().getId(),
                room.getStatus(),
                counterpartLabel(room, residenceId),
                lastMessage == null ? null : lastMessage.getContent(),
                lastMessage == null ? null : lastMessage.getCreatedAt(),
                room.getOpenedAt());
    }

    private LocalDateTime activityAt(ChatRoomSummaryResponse room) {
        return room.lastMessageAt() == null ? room.openedAt() : room.lastMessageAt();
    }

    private ChatRoom findParticipantRoomForUpdate(Long chatRoomId, Long residenceId) {
        return chatRoomRepository.findByIdForUpdate(chatRoomId)
                .filter(room -> room.isParticipant(residenceId))
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    private ChatMessageSenderRole senderRole(
            ChatRoom room,
            Long currentResidenceId,
            Long senderResidenceId) {
        if (currentResidenceId.equals(senderResidenceId)) {
            return ChatMessageSenderRole.ME;
        }
        return room.isAlertSender(senderResidenceId)
                ? ChatMessageSenderRole.ALERT_SENDER
                : ChatMessageSenderRole.ALERT_RECIPIENT;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            throw new ChatMessageInvalidException();
        }
        String normalized = content.trim();
        int length = normalized.codePointCount(0, normalized.length());
        if (length < 1 || length > 500) {
            throw new ChatMessageInvalidException();
        }
        return normalized;
    }

    private String counterpartLabel(ChatRoom room, Long residenceId) {
        return room.isAlertSender(residenceId) ? ALERT_RECIPIENT_LABEL : ALERT_SENDER_LABEL;
    }

    private Residence findCurrentResidence(String subject) {
        Long userId;
        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new ResidenceRequiredException();
        }
        return residenceRepository.findByUserId(userId)
                .orElseThrow(ResidenceRequiredException::new);
    }
}
