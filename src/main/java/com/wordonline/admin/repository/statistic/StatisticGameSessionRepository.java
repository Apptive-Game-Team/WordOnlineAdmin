package com.wordonline.admin.repository.statistic;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.statistic.GameSessionStatus;
import com.wordonline.admin.entity.statistic.StatisticGameSession;

public interface StatisticGameSessionRepository extends JpaRepository<StatisticGameSession, Long> {

    List<StatisticGameSession> findByStartedAtAfterOrderByStartedAtDesc(Instant from);

    List<StatisticGameSession> findByStatusAndStartedAtAfterOrderByStartedAtDesc(
            GameSessionStatus status, Instant from);
}
