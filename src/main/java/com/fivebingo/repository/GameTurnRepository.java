package com.fivebingo.repository;

import com.fivebingo.domain.GameTurn;
import com.fivebingo.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameTurnRepository extends JpaRepository<GameTurn, Long> {
    List<GameTurn> findByRoomOrderByTurnNumberAsc(Room room);
    long countByRoom(Room room);
}
