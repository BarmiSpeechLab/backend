package com.example.backend.domain.curriculum.service;

import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurriculumCache {

    private final CurriculumRepository curriculumRepository;

    // 메모리에 저장할 저장소 (전체 리스트)
    private List<Curriculum> allCurriculums;

    // ID로 빨리 찾기 위한 Map
    private Map<Long, Curriculum> curriculumMap;

    // 1. 서버 켜질 때 딱 1번 실행됨
    @PostConstruct
    public void init() {
        refreshCache();
    }

    // 2. 데이터 로드 및 갱신 (커리큘럼 데이터 수정시 호출)
    public void refreshCache() {
        long start = System.currentTimeMillis();

        // DB에서 전체 조회 (정렬 포함)
        this.allCurriculums = curriculumRepository.findAllByOrderByIdAsc();

        // ID 기반 조회용 Map 생성
        this.curriculumMap = allCurriculums.stream()
                .collect(Collectors.toMap(Curriculum::getId, c -> c));

        long end = System.currentTimeMillis();
        log.info("📚 커리큘럼 캐시 로드 완료! 개수: {}개, 소요시간: {}ms", allCurriculums.size(), (end - start));
    }

    // 3. 필터링 메서드 (쿼리 대신 Java Stream 사용 -> 속도측면)
    public List<Curriculum> getCurriculumsByCondition(String type, String theme) {
        return allCurriculums.stream()
                .filter(c -> c.getType().equals(type))
                .filter(c -> c.getTheme().equals(theme))
                .collect(Collectors.toList());
    }

    // 4. 단건 조회
    public Curriculum getCurriculum(Long id) {
        return curriculumMap.get(id);
    }

    // 5. 전체 조회
    public List<Curriculum> getAll() {
        return allCurriculums;
    }
}
