package com.fivebingo.repository;

import com.fivebingo.domain.Player;
import com.fivebingo.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByRoom(Room room);
    Optional<Player> findByRoomAndSessionId(Room room, String sessionId);
    Optional<Player> findByRoomAndUserId(Room room, Long userId);
    long countByRoom(Room room);
}
