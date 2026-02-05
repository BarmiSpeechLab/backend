package com.example.backend.domain.curriculum.repository;

import com.example.backend.domain.curriculum.entity.CurriculumStats;
import com.example.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CurriculumStatsRepository extends JpaRepository<CurriculumStats, Long> {
    // 1. 특정 유저의 모든 학습 기록 조회
    // ✅ N+1 문제 해결: curriculum을 함께 fetch (JOIN FETCH)
    @EntityGraph(attributePaths = {"curriculum"})
    List<CurriculumStats> findAllByUser(User user);

    // 2. 특정 유저의 특정 커리큘럼 기록 조회 (상세 조회/업데이트 때 필요)
    Optional<CurriculumStats> findByUserAndCurriculumId(User user, Long curriculumId);

    // 3. 나의 전체 학습 평균 점수 조회 (Report쪽 기능)
    // 기록이 없으면 0.0 반환
    @Query("SELECT COALESCE(AVG(s.score), 0) FROM CurriculumStats s WHERE s.user = :user")
    Double getAverageScoreByUser(@Param("user") User user);
}
