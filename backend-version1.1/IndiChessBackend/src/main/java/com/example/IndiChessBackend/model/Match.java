package com.example.IndiChessBackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Data
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* =========================
       PLAYERS
       ========================= */

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id")
    private User player2;

    /* =========================
       GAME STATE
       ========================= */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;   // IN_PROGRESS, PLAYER1_WON, PLAYER2_WON, DRAW, RESIGNED

    @Column(name = "current_ply")
    private Integer currentPly;   // half-move counter

    @Column(name = "fen_current", length = 200)
    private String fenCurrent;

    @Column(name = "last_move_uci", length = 10)
    private String lastMoveUci;

    /* =========================
       MOVES
       ========================= */

    @OneToMany(
            mappedBy = "match",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("ply ASC")
    private List<Move> moves = new ArrayList<>();

    /* =========================
       META
       ========================= */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @PastOrPresent
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @PastOrPresent
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PastOrPresent
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* =========================
       CONSTRUCTORS
       ========================= */

    public Match() {}

    public Match(User player1, User player2, MatchStatus status, GameType gameType) {
        this.player1 = player1;
        this.player2 = player2;
        this.status = status;
        this.gameType = gameType;
        this.currentPly = 0;
        this.createdAt = LocalDateTime.now();
        this.startedAt = LocalDateTime.now();
    }


    /* =========================
       LIFECYCLE
       ========================= */

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.startedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = MatchStatus.IN_PROGRESS;
        }
        if (this.currentPly == null) {
            this.currentPly = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
