package com.example.backend.domain.curriculum.service;

import com.example.backend.domain.curriculum.dto.CurriculumListResponse;
import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.entity.CurriculumStats;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumService {
    private final CurriculumRepository curriculumRepository;
    private final CurriculumStatsRepository curriculumStatsRepository;
    private final UserRepository userRepository;
    public List<CurriculumListResponse> getCurriculumList(Long id) {
        // 1. 유저 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        // 2. 전체 커리큘럼 조회
        List<Curriculum> allCurriculums = curriculumRepository.findAllByOrderByIdAsc();

        // 3. 내 기록 조회 (Map으로 변환하여 검색 속도 O(1)로 최적화)
        // Key: 커리큘럼 ID, Value: 기록객체
        Map<Long, CurriculumStats> myStatsMap = curriculumStatsRepository.findAllByUser(user).stream()
                .collect(Collectors.toMap(
                        stat -> stat.getCurriculum().getId(),   // key
                        stat -> stat                            // value
                ));

        // 4. 병합 (Master + Stats)
        return allCurriculums.stream()
                .map(curr -> {
                    // 이 커리큘럼에 대한 내 기록이 있나? (Map에서 조회)
                    CurriculumStats myStat = myStatsMap.get(curr.getId());
                    // DTO로 변환 (기록이 없으면 null이 넘어가서 내부적으로 0 처리)
                    return CurriculumListResponse.of(curr, myStat);
                })
                .collect(Collectors.toList());
    }
}
