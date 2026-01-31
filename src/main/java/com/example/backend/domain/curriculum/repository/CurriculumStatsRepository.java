package com.example.backend.domain.curriculum.repository;

import com.example.backend.domain.curriculum.entity.CurriculumStats;
import com.example.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurriculumStatsRepository extends JpaRepository<CurriculumStats, Long> {
    // 특정 유저의 모든 학습 기록 조회
    List<CurriculumStats> findAllByUser(User user);

    // 특정 유저의 특정 커리큘럼 기록 조회 (나중에 상세 조회나 업데이트 때 필요)
    Optional<CurriculumStats> findByUserAndCurriculumId(User user, Long curriculumId);
}
