package com.fivebingo.service;

import com.fivebingo.domain.Player;
import com.fivebingo.domain.Room;
import com.fivebingo.domain.User;
import com.fivebingo.dto.RoomDto;
import com.fivebingo.repository.PlayerRepository;
import com.fivebingo.repository.RoomRepository;
import com.fivebingo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;

    public RoomDto.RoomResponse createRoom(String username, String topic) {
        User host = userRepository.findByUsername(username).orElseThrow();

        if (!host.canPlayToday()) {
            throw new RuntimeException("오늘 게임 횟수를 초과하였습니다. (하루 최대 5회)");
        }

        Room room = Room.builder()
                .host(host)
                .topic(topic)
                .build();
        room = roomRepository.save(room);

        Player hostPlayer = Player.builder()
                .room(room)
                .user(host)
                .nickname(host.getNickname())
                .isHost(true)
                .build();
        playerRepository.save(hostPlayer);

        return toRoomResponse(room);
    }

    public RoomDto.RoomResponse joinRoomAsGuest(String inviteCode, String nickname, String sessionId) {
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        if (room.isFull()) {
            throw new RuntimeException("방이 꽉 찼습니다.");
        }
        if (room.getStatus() != Room.RoomStatus.WAITING) {
            throw new RuntimeException("이미 게임이 시작되었습니다.");
        }

        // 중복 세션 확인
        if (playerRepository.findByRoomAndSessionId(room, sessionId).isPresent()) {
            return toRoomResponse(room); // 이미 입장
        }

        Player player = Player.builder()
                .room(room)
                .nickname(nickname)
                .sessionId(sessionId)
                .isHost(false)
                .build();
        playerRepository.save(player);

        return toRoomResponse(room);
    }

    public RoomDto.RoomResponse joinRoomAsUser(String inviteCode, String username) {
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow();

        if (playerRepository.findByRoomAndUserId(room, user.getId()).isPresent()) {
            return toRoomResponse(room);
        }
        if (room.isFull()) throw new RuntimeException("방이 꽉 찼습니다.");
        if (room.getStatus() != Room.RoomStatus.WAITING) throw new RuntimeException("이미 게임이 시작됨");

        Player player = Player.builder()
                .room(room)
                .user(user)
                .nickname(user.getNickname())
                .isHost(false)
                .build();
        playerRepository.save(player);

        return toRoomResponse(room);
    }

    public RoomDto.RoomResponse getRoomByInviteCode(String inviteCode) {
        Room room = roomRepository.findByInviteCode(inviteCode).orElseThrow();
        return toRoomResponse(room);
    }

    public void startWordInput(Long roomId, String username) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        validateHost(room, username);
        room.setStatus(Room.RoomStatus.WORD_INPUT);
        roomRepository.save(room);
    }

    public void submitBingoBoard(Long roomId, Long playerId, List<String> words) {
        if (words.size() != 25) throw new RuntimeException("25개의 단어를 입력해주세요.");

        Player player = playerRepository.findById(playerId).orElseThrow();
        player.setBingoBoard(wordsToJson(words));
        player.setStatus(Player.PlayerStatus.WORD_DONE);
        playerRepository.save(player);
    }

    public void setReady(Long roomId, Long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.setReady(true);
        player.setStatus(Player.PlayerStatus.READY);
        playerRepository.save(player);

        Room room = roomRepository.findById(roomId).orElseThrow();
        List<Player> players = playerRepository.findByRoom(room);
        boolean allReady = players.stream().allMatch(Player::isReady);

        if (allReady && players.size() > 1) {
            startGame(room, players);
        }
    }

    private void startGame(Room room, List<Player> players) {
        // 랜덤 순서 배정
        List<Integer> orders = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) orders.add(i + 1);
        Collections.shuffle(orders);

        for (int i = 0; i < players.size(); i++) {
            players.get(i).setTurnOrder(orders.get(i));
            playerRepository.save(players.get(i));
        }

        room.setStatus(Room.RoomStatus.IN_PROGRESS);
        roomRepository.save(room);

        // 방장의 게임 횟수 증가
        User host = room.getHost();
        host.incrementDailyGameCount();
        userRepository.save(host);
    }

    public RoomDto.RoomResponse getRoom(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        return toRoomResponse(room);
    }

    private void validateHost(Room room, String username) {
        if (!room.getHost().getUsername().equals(username)) {
            throw new RuntimeException("방장만 가능합니다.");
        }
    }

    private String wordsToJson(List<String> words) {
        return "[" + words.stream().map(w -> "\"" + w + "\"").collect(Collectors.joining(",")) + "]";
    }

    public RoomDto.RoomResponse toRoomResponse(Room room) {
        List<Player> players = playerRepository.findByRoom(room);
        List<RoomDto.PlayerInfo> playerInfos = players.stream()
                .map(p -> RoomDto.PlayerInfo.builder()
                        .playerId(p.getId())
                        .nickname(p.getNickname())
                        .isHost(p.isHost())
                        .isReady(p.isReady())
                        .bingoCount(p.getBingoCount())
                        .status(p.getStatus())
                        .turnOrder(p.getTurnOrder())
                        .build())
                .collect(Collectors.toList());

        return RoomDto.RoomResponse.builder()
                .roomId(room.getId())
                .inviteCode(room.getInviteCode())
                .topic(room.getTopic())
                .hostNickname(room.getHost().getNickname())
                .status(room.getStatus())
                .players(playerInfos)
                .maxPlayers(room.getMaxPlayers())
                .build();
    }
}