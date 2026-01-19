package com.example.IndiChessBackend.repo;

import com.example.IndiChessBackend.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoveRepo extends JpaRepository<Move, Long> {

    List<Move> findByMatchIdOrderByPlyAsc(Long matchId);

    Optional<Move> findTopByMatchIdOrderByPlyDesc(Long matchId);

    long countByMatchId(Long matchId);
}
