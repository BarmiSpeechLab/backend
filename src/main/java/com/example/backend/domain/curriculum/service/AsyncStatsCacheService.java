package com.example.backend.domain.curriculum.service;

import com.example.backend.domain.curriculum.entity.CurriculumStats;
import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncStatsCacheService {

    private final CurriculumStatsRepository curriculumStatsRepository;
    private final UserRepository userRepository;
    private final org.springframework.cache.CacheManager cacheManager; // CacheManager 주입

    /** 비동기 캐시 갱신 (Cache Refresh)
     * - 저장 트랜잭션 완료 후 호출됨
     * - 별도 스레드에서 DB 조회 후 '수동으로' 캐시 갱신
     * - @Async 메서드는 반드시 void 또는 Future를 반환해야 함 (Map 반환 시 에러 발생)
     */
    @Async
    @Transactional(readOnly = true)
    // @CachePut 제거 -> 수동 갱신으로 변경
    public void refreshUserStats(Long userId) {
        log.info("[Async Cache Refresh] 시작 - userId={}", userId);
        long start = System.currentTimeMillis();

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            log.warn("[Async Cache Refresh] 유저를 찾을 수 없음 - userId={}", userId);
            return;
        }

        // DB에서 최신 통계 전체 조회
        Map<Long, CurriculumStats> statsMap = curriculumStatsRepository.findAllByUser(user).stream()
                .collect(Collectors.toMap(
                        stat -> stat.getCurriculum().getId(),
                        stat -> stat
                ));

        // 수동으로 캐시 저장
        org.springframework.cache.Cache cache = cacheManager.getCache("curriculum_stats");
        if (cache != null) {
            cache.put(userId, statsMap);
            log.info("[Async Cache Refresh] 캐시 수동 업데이트 완료 - userId={}", userId);
        } else {
            log.warn("[Async Cache Refresh] 'curriculum_stats' 캐시를 찾을 수 없음");
        }

        long end = System.currentTimeMillis();
        log.info("[Async Cache Refresh] 완료 - 항목수={}, 소요시간={}ms", statsMap.size(), (end - start));
    }
}
