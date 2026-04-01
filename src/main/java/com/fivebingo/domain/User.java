package com.fivebingo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(name = "daily_game_count")
    @Builder.Default
    private int dailyGameCount = 0;

    @Column(name = "last_game_date")
    private LocalDate lastGameDate;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean canPlayToday() {
        LocalDate today = LocalDate.now();
        if (lastGameDate == null || !lastGameDate.equals(today)) {
            return true; // 오늘 처음 플레이
        }
        return dailyGameCount < 5;
    }

    public void incrementDailyGameCount() {
        LocalDate today = LocalDate.now();
        if (lastGameDate == null || !lastGameDate.equals(today)) {
            dailyGameCount = 1;
            lastGameDate = today;
        } else {
            dailyGameCount++;
        }
    }
}