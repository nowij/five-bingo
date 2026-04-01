package com.fivebingo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // 로그인 사용자의 경우
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 비로그인 게스트의 경우
    @Column(nullable = false)
    private String nickname;

    // 세션 ID (게스트 식별용)
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "is_host")
    @Builder.Default
    private boolean isHost = false;

    @Column(name = "is_ready")
    @Builder.Default
    private boolean isReady = false;

    @Column(name = "bingo_count")
    @Builder.Default
    private int bingoCount = 0;

    @Column(name = "turn_order")
    private Integer turnOrder;

    // 25칸 빙고판 (JSON 문자열로 저장)
    @Column(name = "bingo_board", columnDefinition = "TEXT")
    private String bingoBoard; // JSON: ["단어1","단어2",...]

    // 체크된 칸 (JSON 문자열로 저장)
    @Column(name = "checked_cells", columnDefinition = "TEXT")
    @Builder.Default
    private String checkedCells = "[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false]";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlayerStatus status = PlayerStatus.JOINED;

    public enum PlayerStatus {
        JOINED,     // 참가
        WORD_DONE,  // 단어 입력 완료
        READY       // 준비 완료
    }
}