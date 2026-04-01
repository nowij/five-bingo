package com.fivebingo.controller;

import com.fivebingo.dto.RoomDto;
import com.fivebingo.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 방 생성 (로그인 필요)
    @PostMapping
    public ResponseEntity<?> createRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RoomDto.CreateRoomRequest request) {
        try {
            RoomDto.RoomResponse response = roomService.createRoom(userDetails.getUsername(), request.getTopic());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 방 정보 조회 (초대 코드로)
    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<?> getRoomByInviteCode(@PathVariable String inviteCode) {
        try {
            return ResponseEntity.ok(roomService.getRoomByInviteCode(inviteCode));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 방 정보 조회 (ID로)
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable Long roomId) {
        try {
            return ResponseEntity.ok(roomService.getRoom(roomId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 게스트 입장 (비로그인)
    @PostMapping("/join/{inviteCode}/guest")
    public ResponseEntity<?> joinAsGuest(
            @PathVariable String inviteCode,
            @RequestBody RoomDto.JoinRoomRequest request) {
        try {
            RoomDto.RoomResponse response = roomService.joinRoomAsGuest(
                    inviteCode, request.getNickname(), request.getSessionId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 로그인 유저 입장
    @PostMapping("/join/{inviteCode}/user")
    public ResponseEntity<?> joinAsUser(
            @PathVariable String inviteCode,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            RoomDto.RoomResponse response = roomService.joinRoomAsUser(inviteCode, userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 게임 시작 (단어 입력 단계로 전환) - 방장만
    @PostMapping("/{roomId}/start")
    public ResponseEntity<?> startWordInput(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            roomService.startWordInput(roomId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "단어 입력 단계 시작"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 빙고판 단어 제출
    @PostMapping("/{roomId}/board")
    public ResponseEntity<?> submitBoard(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> body) {
        try {
            Long playerId = Long.valueOf(body.get("playerId").toString());
            @SuppressWarnings("unchecked")
            List<String> words = (List<String>) body.get("words");
            roomService.submitBingoBoard(roomId, playerId, words);
            return ResponseEntity.ok(Map.of("message", "빙고판 저장 완료"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 준비 완료
    @PostMapping("/{roomId}/ready")
    public ResponseEntity<?> setReady(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> body) {
        try {
            Long playerId = Long.valueOf(body.get("playerId").toString());
            roomService.setReady(roomId, playerId);
            return ResponseEntity.ok(Map.of("message", "준비 완료"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

