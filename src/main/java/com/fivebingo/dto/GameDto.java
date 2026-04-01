package com.fivebingo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public class GameDto {

    // 단어 선택 요청 (클라이언트 → 서버)
    @Data
    public static class CallWordRequest {
        private Long playerId;
        private String word;
        private String sessionId; // 게스트 식별용
    }

    // 게임 이벤트 (서버 → 클라이언트 브로드캐스트)
    @Data
    @Builder
    public static class GameEvent {
        private GameEventType type;
        private String callerNickname;
        private String calledWord;
        private List<PlayerBingoState> playerStates;
        private Long winnerId;
        private String winnerNickname;
        private List<PlayerBoardReveal> revealedBoards; // 게임 종료 시
        private int turnNumber;
        private String nextTurnNickname;
        private String message;
    }

    public enum GameEventType {
        WORD_CALLED,       // 단어 선택됨
        BINGO_ACHIEVED,    // 빙고 달성
        GAME_FINISHED,     // 게임 종료
        TURN_CHANGED,      // 턴 변경
        ROOM_STATUS_CHANGED, // 방 상태 변경 (단어 입력, 준비 등)
        PLAYER_JOINED,     // 플레이어 입장
        PLAYER_LEFT,       // 플레이어 퇴장
        PLAYER_READY,      // 플레이어 준비 완료
        BOARD_SUBMITTED,   // 빙고판 제출 완료
    }

    @Data
    @Builder
    public static class PlayerBingoState {
        private Long playerId;
        private String nickname;
        private int bingoCount;
        private boolean isReady;
        private boolean isCurrentTurn;
    }

    @Data
    @Builder
    public static class PlayerBoardReveal {
        private Long playerId;
        private String nickname;
        private List<String> words;
        private List<Boolean> checkedCells;
        private int bingoCount;
        private boolean isWinner;
    }

    // 내 빙고판 상태 응답 (서버 → 해당 클라이언트만)
    @Data
    @Builder
    public static class MyBoardState {
        private List<String> words;
        private List<Boolean> checkedCells;
        private int bingoCount;
        private List<Integer> bingoCells; // 빙고 완성된 칸 인덱스
    }
}