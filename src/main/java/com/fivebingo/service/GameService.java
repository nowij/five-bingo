package com.fivebingo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivebingo.domain.GameTurn;
import com.fivebingo.domain.Player;
import com.fivebingo.domain.Room;
import com.fivebingo.dto.GameDto;
import com.fivebingo.repository.GameTurnRepository;
import com.fivebingo.repository.PlayerRepository;
import com.fivebingo.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final GameTurnRepository gameTurnRepository;
    private final ObjectMapper objectMapper;

    /**
     * 단어 선택 처리 - 모든 플레이어 빙고판에서 해당 단어 체크
     * @return GameEvent (브로드캐스트용)
     */
    public GameDto.GameEvent callWord(Long roomId, Long playerId, String word) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        Player caller = playerRepository.findById(playerId).orElseThrow();

        if (room.getStatus() != Room.RoomStatus.IN_PROGRESS) {
            throw new RuntimeException("게임이 진행 중이 아닙니다.");
        }

        // 현재 턴인지 확인
        validateTurn(room, caller);

        // 턴 기록
        long turnCount = gameTurnRepository.countByRoom(room);
        GameTurn gameTurn = GameTurn.builder()
                .room(room)
                .player(caller)
                .calledWord(word)
                .turnNumber((int) turnCount + 1)
                .build();
        gameTurnRepository.save(gameTurn);

        // 모든 플레이어 빙고판에서 단어 체크 및 빙고 계산
        List<Player> players = playerRepository.findByRoom(room);
        List<GameDto.PlayerBingoState> playerStates = new ArrayList<>();
        boolean gameFinished = false;
        Player winner = null;

        for (Player player : players) {
            if (player.getBingoBoard() == null) continue;

            List<String> words = parseWords(player.getBingoBoard());
            List<Boolean> checked = parseChecked(player.getCheckedCells());

            // 단어 체크
            for (int i = 0; i < words.size(); i++) {
                if (words.get(i).trim().equalsIgnoreCase(word.trim())) {
                    checked.set(i, true);
                }
            }
            player.setCheckedCells(boolListToJson(checked));

            // 빙고 계산
            int newBingoCount = calculateBingo(checked);
            player.setBingoCount(newBingoCount);
            playerRepository.save(player);

            // 5빙고 달성 확인
            if (newBingoCount >= 5 && !gameFinished) {
                gameFinished = true;
                winner = player;
            }

            playerStates.add(GameDto.PlayerBingoState.builder()
                    .playerId(player.getId())
                    .nickname(player.getNickname())
                    .bingoCount(newBingoCount)
                    .isReady(player.isReady())
                    .build());
        }

        // 게임 종료 처리
        if (gameFinished) {
            room.setStatus(Room.RoomStatus.FINISHED);
            roomRepository.save(room);

            List<GameDto.PlayerBoardReveal> reveals = buildRevealedBoards(players, winner);
            return GameDto.GameEvent.builder()
                    .type(GameDto.GameEventType.GAME_FINISHED)
                    .callerNickname(caller.getNickname())
                    .calledWord(word)
                    .playerStates(playerStates)
                    .winnerId(winner.getId())
                    .winnerNickname(winner.getNickname())
                    .revealedBoards(reveals)
                    .turnNumber((int) turnCount + 1)
                    .message(winner.getNickname() + "님이 5빙고로 승리하였습니다!")
                    .build();
        }

        // 다음 턴 플레이어 계산
        String nextTurnNickname = getNextTurnPlayer(players, caller);

        return GameDto.GameEvent.builder()
                .type(GameDto.GameEventType.WORD_CALLED)
                .callerNickname(caller.getNickname())
                .calledWord(word)
                .playerStates(playerStates)
                .turnNumber((int) turnCount + 1)
                .nextTurnNickname(nextTurnNickname)
                .message(caller.getNickname() + "님이 '" + word + "' 선택")
                .build();
    }

    /**
     * 내 빙고판 상태 조회
     */
    @Transactional(readOnly = true)
    public GameDto.MyBoardState getMyBoardState(Long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.getBingoBoard() == null) {
            return GameDto.MyBoardState.builder()
                    .words(new ArrayList<>())
                    .checkedCells(new ArrayList<>())
                    .bingoCount(0)
                    .bingoCells(new ArrayList<>())
                    .build();
        }

        List<String> words = parseWords(player.getBingoBoard());
        List<Boolean> checked = parseChecked(player.getCheckedCells());
        List<Integer> bingoCells = getBingoCellIndices(checked);

        return GameDto.MyBoardState.builder()
                .words(words)
                .checkedCells(checked)
                .bingoCount(player.getBingoCount())
                .bingoCells(bingoCells)
                .build();
    }

    /**
     * 현재 턴 플레이어 조회
     */
    @Transactional(readOnly = true)
    public Player getCurrentTurnPlayer(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        List<Player> players = playerRepository.findByRoom(room);
        long turnCount = gameTurnRepository.countByRoom(room);

        int playerCount = players.size();
        // 순서 기반으로 현재 턴 결정
        int currentOrder = (int) (turnCount % playerCount) + 1;

        return players.stream()
                .filter(p -> p.getTurnOrder() != null && p.getTurnOrder() == currentOrder)
                .findFirst()
                .orElse(players.get(0));
    }

    // ==================== Private Helpers ====================

    private void validateTurn(Room room, Player caller) {
        Player currentTurnPlayer = getCurrentTurnPlayer(room.getId());
        if (!currentTurnPlayer.getId().equals(caller.getId())) {
            throw new RuntimeException("현재 당신의 차례가 아닙니다.");
        }
    }

    private String getNextTurnPlayer(List<Player> players, Player currentPlayer) {
        List<Player> sorted = players.stream()
                .filter(p -> p.getTurnOrder() != null)
                .sorted((a, b) -> a.getTurnOrder() - b.getTurnOrder())
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(currentPlayer.getId())) {
                int nextIdx = (i + 1) % sorted.size();
                return sorted.get(nextIdx).getNickname();
            }
        }
        return sorted.isEmpty() ? "" : sorted.get(0).getNickname();
    }

    /**
     * 빙고 계산 (5x5)
     * 가로 5줄 + 세로 5줄 + 대각선 2줄 = 12개 라인
     */
    private int calculateBingo(List<Boolean> checked) {
        int count = 0;

        // 가로 5줄
        for (int row = 0; row < 5; row++) {
            boolean bingo = true;
            for (int col = 0; col < 5; col++) {
                if (!checked.get(row * 5 + col)) { bingo = false; break; }
            }
            if (bingo) count++;
        }

        // 세로 5줄
        for (int col = 0; col < 5; col++) {
            boolean bingo = true;
            for (int row = 0; row < 5; row++) {
                if (!checked.get(row * 5 + col)) { bingo = false; break; }
            }
            if (bingo) count++;
        }

        // 대각선 (좌상→우하)
        boolean diag1 = true;
        for (int i = 0; i < 5; i++) {
            if (!checked.get(i * 5 + i)) { diag1 = false; break; }
        }
        if (diag1) count++;

        // 대각선 (우상→좌하)
        boolean diag2 = true;
        for (int i = 0; i < 5; i++) {
            if (!checked.get(i * 5 + (4 - i))) { diag2 = false; break; }
        }
        if (diag2) count++;

        return count;
    }

    /**
     * 빙고 완성된 칸 인덱스 반환
     */
    private List<Integer> getBingoCellIndices(List<Boolean> checked) {
        List<Integer> bingoIndices = new ArrayList<>();

        // 가로
        for (int row = 0; row < 5; row++) {
            boolean bingo = true;
            for (int col = 0; col < 5; col++) {
                if (!checked.get(row * 5 + col)) { bingo = false; break; }
            }
            if (bingo) {
                for (int col = 0; col < 5; col++) bingoIndices.add(row * 5 + col);
            }
        }

        // 세로
        for (int col = 0; col < 5; col++) {
            boolean bingo = true;
            for (int row = 0; row < 5; row++) {
                if (!checked.get(row * 5 + col)) { bingo = false; break; }
            }
            if (bingo) {
                for (int row = 0; row < 5; row++) bingoIndices.add(row * 5 + col);
            }
        }

        // 대각선1
        boolean diag1 = true;
        for (int i = 0; i < 5; i++) {
            if (!checked.get(i * 5 + i)) { diag1 = false; break; }
        }
        if (diag1) { for (int i = 0; i < 5; i++) bingoIndices.add(i * 5 + i); }

        // 대각선2
        boolean diag2 = true;
        for (int i = 0; i < 5; i++) {
            if (!checked.get(i * 5 + (4 - i))) { diag2 = false; break; }
        }
        if (diag2) { for (int i = 0; i < 5; i++) bingoIndices.add(i * 5 + (4 - i)); }

        return bingoIndices.stream().distinct().sorted().collect(Collectors.toList());
    }

    private List<GameDto.PlayerBoardReveal> buildRevealedBoards(List<Player> players, Player winner) {
        return players.stream().map(p -> {
            List<String> words = p.getBingoBoard() != null ? parseWords(p.getBingoBoard()) : new ArrayList<>();
            List<Boolean> checked = parseChecked(p.getCheckedCells());
            return GameDto.PlayerBoardReveal.builder()
                    .playerId(p.getId())
                    .nickname(p.getNickname())
                    .words(words)
                    .checkedCells(checked)
                    .bingoCount(p.getBingoCount())
                    .isWinner(p.getId().equals(winner.getId()))
                    .build();
        }).collect(Collectors.toList());
    }

    private List<String> parseWords(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse words JSON", e);
            return new ArrayList<>();
        }
    }

    private List<Boolean> parseChecked(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Boolean>>() {});
        } catch (Exception e) {
            List<Boolean> defaults = new ArrayList<>();
            for (int i = 0; i < 25; i++) defaults.add(false);
            return defaults;
        }
    }

    private String boolListToJson(List<Boolean> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false]";
        }
    }
}

