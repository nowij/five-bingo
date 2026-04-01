package com.fivebingo.dto;

import com.fivebingo.domain.Player;
import com.fivebingo.domain.Room;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class RoomDto {

    @Data
    public static class CreateRoomRequest {
        private String topic;
    }

    @Data
    public static class JoinRoomRequest {
        private String nickname;   // 게스트용
        private String sessionId;  // 게스트 세션 식별
    }

    @Data
    @Builder
    public static class RoomResponse {
        private Long roomId;
        private String inviteCode;
        private String topic;
        private String hostNickname;
        private Room.RoomStatus status;
        private List<PlayerInfo> players;
        private int maxPlayers;
    }

    @Data
    @Builder
    public static class PlayerInfo {
        private Long playerId;
        private String nickname;
        private boolean isHost;
        private boolean isReady;
        private int bingoCount;
        private Player.PlayerStatus status;
        private Integer turnOrder;
    }
}