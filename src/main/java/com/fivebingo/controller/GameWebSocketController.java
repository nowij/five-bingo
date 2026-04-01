package com.fivebingo.controller;

import com.fivebingo.domain.Player;
import com.fivebingo.domain.Room;
import com.fivebingo.dto.GameDto;
import com.fivebingo.repository.PlayerRepository;
import com.fivebingo.repository.RoomRepository;
import com.fivebingo.service.GameService;
import com.fivebingo.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;
    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;

    /**
     * 단어 선택 (게임 진행 중 턴인 플레이어가 단어 호출)
     * 클라이언트: stompClient.publish({ destination: '/app/room/{roomId}/call-word', body: JSON.stringify({...}) })
     * 브로드캐스트: /topic/room/{roomId}/game
     */
    @MessageMapping("/room/{roomId}/call-word")
    public void callWord(
            @DestinationVariable Long roomId,
            @Payload GameDto.CallWordRequest request) {
        try {
            GameDto.GameEvent event = gameService.callWord(roomId, request.getPlayerId(), request.getWord());
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game", event);
        } catch (RuntimeException e) {
            log.error("callWord error: {}", e.getMessage());
            // 에러를 요청자에게만 전송
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/error",
                    GameDto.GameEvent.builder()
                            .type(GameDto.GameEventType.WORD_CALLED)
                            .message("오류: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 방 입장 알림 (WebSocket 연결 후 호출)
     * 브로드캐스트: /topic/room/{roomId}/players
     */
    @MessageMapping("/room/{roomId}/enter")
    public void enterRoom(
            @DestinationVariable Long roomId,
            @Payload GameDto.CallWordRequest request) {
        broadcastRoomState(roomId, GameDto.GameEventType.PLAYER_JOINED,
                "플레이어가 입장하였습니다.");
    }

    /**
     * 방 퇴장 알림
     */
    @MessageMapping("/room/{roomId}/leave")
    public void leaveRoom(
            @DestinationVariable Long roomId,
            @Payload GameDto.CallWordRequest request) {
        broadcastRoomState(roomId, GameDto.GameEventType.PLAYER_LEFT,
                "플레이어가 퇴장하였습니다.");
    }

    /**
     * 빙고판 제출 알림
     * 브로드캐스트: /topic/room/{roomId}/players
     */
    @MessageMapping("/room/{roomId}/board-submitted")
    public void boardSubmitted(
            @DestinationVariable Long roomId,
            @Payload GameDto.CallWordRequest request) {
        broadcastRoomState(roomId, GameDto.GameEventType.BOARD_SUBMITTED,
                "플레이어가 빙고판을 제출하였습니다.");
    }

    /**
     * 준비 완료 알림
     * 브로드캐스트: /topic/room/{roomId}/players
     */
    @MessageMapping("/room/{roomId}/player-ready")
    public void playerReady(
            @DestinationVariable Long roomId,
            @Payload GameDto.CallWordRequest request) {
        broadcastRoomState(roomId, GameDto.GameEventType.PLAYER_READY,
                "플레이어가 준비 완료하였습니다.");
    }

    /**
     * 방 상태 변경 알림 (게임 시작, 단어 입력 단계 전환 등)
     * REST API 성공 후 서버에서 직접 호출 가능하도록 public
     */
    public void broadcastRoomStatusChanged(Long roomId, String message) {
        broadcastRoomState(roomId, GameDto.GameEventType.ROOM_STATUS_CHANGED, message);
    }

    // ==================== Private Helpers ====================

    private void broadcastRoomState(Long roomId, GameDto.GameEventType eventType, String message) {
        try {
            Room room = roomRepository.findById(roomId).orElseThrow();
            List<Player> players = playerRepository.findByRoom(room);

            // 현재 턴 플레이어 닉네임
            String currentTurnNickname = null;
            if (room.getStatus() == Room.RoomStatus.IN_PROGRESS) {
                try {
                    Player currentPlayer = gameService.getCurrentTurnPlayer(roomId);
                    currentTurnNickname = currentPlayer.getNickname();
                } catch (Exception ignored) {}
            }

            final String finalCurrentTurnNickname = currentTurnNickname;
            List<GameDto.PlayerBingoState> playerStates = players.stream()
                    .map(p -> GameDto.PlayerBingoState.builder()
                            .playerId(p.getId())
                            .nickname(p.getNickname())
                            .bingoCount(p.getBingoCount())
                            .isReady(p.isReady())
                            .isCurrentTurn(finalCurrentTurnNickname != null
                                    && p.getNickname().equals(finalCurrentTurnNickname))
                            .build())
                    .collect(Collectors.toList());

            GameDto.GameEvent event = GameDto.GameEvent.builder()
                    .type(eventType)
                    .playerStates(playerStates)
                    .nextTurnNickname(currentTurnNickname)
                    .message(message)
                    .build();

            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/players", event);
        } catch (Exception e) {
            log.error("broadcastRoomState error: {}", e.getMessage());
        }
    }
}
