package com.fivebingo.controller;

import com.fivebingo.domain.Player;
import com.fivebingo.dto.GameDto;
import com.fivebingo.repository.PlayerRepository;
import com.fivebingo.repository.RoomRepository;
import com.fivebingo.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;

    @GetMapping("/board/{playerId}")
    public ResponseEntity<?> getMyBoard(@PathVariable Long playerId) {
        try {
            GameDto.MyBoardState state = gameService.getMyBoardState(playerId);
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            log.error("getMyBoard error - playerId: {}, msg: {}", playerId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/room/{roomId}/current-turn")
    public ResponseEntity<?> getCurrentTurn(@PathVariable Long roomId) {
        try {
            Player player = gameService.getCurrentTurnPlayer(roomId);
            // ★ Map.of()는 null 값 허용 안 함 → HashMap 사용
            Map<String, Object> result = new HashMap<>();
            result.put("playerId",  player.getId());
            result.put("nickname",  player.getNickname());
            result.put("turnOrder", player.getTurnOrder() != null ? player.getTurnOrder() : 0);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("getCurrentTurn error - roomId: {}, msg: {}", roomId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<?> getPlayer(@PathVariable Long playerId) {
        try {
            return playerRepository.findById(playerId)
                    .map(p -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("playerId",  p.getId());
                        result.put("nickname",  p.getNickname());
                        result.put("isHost",    p.isHost());
                        result.put("bingoCount",p.getBingoCount());
                        result.put("isReady",   p.isReady());
                        result.put("status",    p.getStatus().name());
                        return ResponseEntity.ok(result);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("getPlayer error - playerId: {}, msg: {}", playerId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}