package com.example.backend.domain.curriculum.service;

import com.example.backend.domain.curriculum.dto.CurriculumDto;
import com.example.backend.domain.curriculum.dto.CurriculumResponse;
import com.example.backend.domain.curriculum.dto.CurriculumStatsDto;
import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.entity.CurriculumStats;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumService {
    private final CurriculumCache curriculumCache;
    private final CurriculumStatsRepository curriculumStatsRepository;
    private final UserRepository userRepository;

    // 1. 커리큘럼 리스트 조회 메서드
    public List<CurriculumResponse> getCurriculumList(Long id, String type, String theme) {
        // 1. 유저 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        // 2. 전체 커리큘럼 조회 (DB가 아닌 메모리 조회)
        List<Curriculum> allCurriculums = curriculumCache.getCurriculumsByCondition(type, theme);

        // 3. 내 기록 조회 (@Cacheable로 캐싱 - 동일 유저의 반복 조회 최적화)
        Map<Long, CurriculumStats> myStatsMap = getUserStatsMap(user);

        // 4. 병합
        return allCurriculums.stream()
                .map(curr -> {
                    // 이 커리큘럼에 대한 내 기록이 있나? (Map에서 조회)
                    CurriculumStats myStat = myStatsMap.get(curr.getId());
                    // DTO로 변환 (기록이 없으면 null이 넘어가서 내부적으로 0 처리)
                    return CurriculumResponse.of(curr, myStat);
                })
                .collect(Collectors.toList());
    }

    // 2. 커리큘럼 상세 조회 메서드
    public CurriculumResponse getCurriculumDetail(Long userId, Long curriculumId) {
        // 1. 커리큘럼 조회(메모리)
        Curriculum curriculum = curriculumCache.getCurriculum(curriculumId);
        if (curriculum == null) {
            throw new CustomException(ErrorCode.EXPRESSION_NOT_FOUND);
        }
        // 2. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 3. 내 기록 조회 (없을 수도 있음 -> Optional 처리)
        CurriculumStats stats = curriculumStatsRepository
                .findByUserAndCurriculumId(user, curriculumId)
                .orElse(null); // 없으면 null 넘김

        // 4. DTO 변환
        return CurriculumResponse.of(curriculum, stats);
    }

    // =================================================================
    // 학습 통계 조회 (캐싱 없음)
    // =================================================================

    /**
     * 사용자별 학습 통계를 Map으로 반환 (캐싱 제거)
     * - UserStats는 자주 변경되므로 매번 최신 데이터 조회
     * - key: userId, value: Map<커리큘럼ID, 통계>
     */
    /**
     *  사용자별 학습 통계를 Map으로 반환 (캐싱 적용)
     * - 기본적으로 캐시에서 조회 (@Cacheable)
     * - 학습 완료 시 AsyncStatsCacheService가 갱신해줌 (@CachePut)
     */
    @Cacheable(value = "curriculum_stats", key = "#user.id")
    public Map<Long, CurriculumStats> getUserStatsMap(User user) {
        return curriculumStatsRepository.findAllByUser(user).stream()
                .collect(Collectors.toMap(
                        stat -> stat.getCurriculum().getId(),   // key
                        stat -> stat                            // value
                ));
    }

    // =================================================================
    // API 분리: Curriculum (정적) vs Stats (동적)
    // =================================================================

    /**
     * 커리큘럼 목록 조회 (Stats 없음, 정적 데이터만)
     * - 프론트엔드에서 장기 캐싱 가능
     */
    public List<CurriculumDto> getCurriculumListWithoutStats(String type, String theme) {
        return curriculumCache.getCurriculumsByCondition(type, theme).stream()
                .map(CurriculumDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자별 통계만 조회 (동적 데이터)
     * - 매번 최신 데이터 조회
     */
    public List<CurriculumStatsDto> getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        
        // 캐시된 Map을 사용하여 DB 조회 방지
        return getUserStatsMap(user).values().stream()
                .map(CurriculumStatsDto::from)
                .collect(Collectors.toList());
    }
}

