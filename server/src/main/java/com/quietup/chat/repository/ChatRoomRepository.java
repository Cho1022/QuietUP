package com.quietup.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quietup.chat.entity.ChatRoom;

import jakarta.persistence.LockModeType;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from ChatRoom room where room.noiseAlert.id = :noiseAlertId")
    Optional<ChatRoom> findByNoiseAlertIdForUpdate(@Param("noiseAlertId") Long noiseAlertId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from ChatRoom room where room.id = :chatRoomId")
    Optional<ChatRoom> findByIdForUpdate(@Param("chatRoomId") Long chatRoomId);

    @Query("""
            select room
            from ChatRoom room
            where room.alertSenderResidence.id = :residenceId
               or room.alertResponderResidence.id = :residenceId
            order by room.openedAt desc, room.id desc
            """)
    List<ChatRoom> findAllForParticipant(@Param("residenceId") Long residenceId);
}
