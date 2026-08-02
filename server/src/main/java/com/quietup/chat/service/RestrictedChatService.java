package com.quietup.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.chat.dto.ChatRoomCreationResult;
import com.quietup.chat.dto.ChatRoomDetailResponse;
import com.quietup.chat.dto.ChatRoomResponse;
import com.quietup.chat.dto.ChatRoomSummaryResponse;
import com.quietup.chat.entity.ChatRoom;
import com.quietup.chat.repository.ChatRoomRepository;
import com.quietup.global.error.ChatRequestRequiredException;
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

    public RestrictedChatService(
            ResidenceRepository residenceRepository,
            NoiseAlertRepository noiseAlertRepository,
            NoiseAlertResponseRepository noiseAlertResponseRepository,
            ChatRoomRepository chatRoomRepository) {
        this.residenceRepository = residenceRepository;
        this.noiseAlertRepository = noiseAlertRepository;
        this.noiseAlertResponseRepository = noiseAlertResponseRepository;
        this.chatRoomRepository = chatRoomRepository;
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
                .map(room -> new ChatRoomSummaryResponse(
                        room.getId(),
                        room.getNoiseAlert().getId(),
                        room.getStatus(),
                        counterpartLabel(room, currentResidence.getId()),
                        null,
                        null,
                        room.getOpenedAt()))
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

    private ChatRoomResponse toRoomResponse(ChatRoom room, Long residenceId) {
        return new ChatRoomResponse(
                room.getId(),
                room.getNoiseAlert().getId(),
                room.getStatus(),
                counterpartLabel(room, residenceId),
                room.getOpenedAt());
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
