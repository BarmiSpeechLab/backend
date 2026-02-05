package com.example.backend.domain.curriculum.service;

import com.example.backend.domain.curriculum.entity.Ipa;
import com.example.backend.domain.curriculum.repository.IpaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ IPA 캐시 서비스
 * - 애플리케이션 시작 시 모든 IPA 심볼을 메모리에 로드
 * - symbol → Ipa 매핑으로 빠른 조회 제공
 * - DB 조회 없이 IPA 엔티티 반환
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IpaCacheService {
    
    private final IpaRepository ipaRepository;
    
    // ✅ symbol → Ipa 매핑 캐시 (thread-safe)
    private final Map<String, Ipa> ipaCache = new ConcurrentHashMap<>();
    
    /**
     * ✅ 애플리케이션 시작 시 모든 IPA를 캐시에 로드
     */
    @PostConstruct
    public void init() {
        log.info("[IPA 캐시 초기화 시작]");
        List<Ipa> allIpas = ipaRepository.findAll();
        
        for (Ipa ipa : allIpas) {
            ipaCache.put(ipa.getSymbol(), ipa);
        }
        
        log.info("[IPA 캐시 초기화 완료] 총 {}개 IPA 로드", ipaCache.size());
    }
    
    /**
     * ✅ symbol로 IPA 조회 (캐시 사용, DB 조회 없음)
     * @param symbol IPA 심볼 (예: "ə", "s")
     * @return IPA 엔티티 (없으면 null)
     */
    public Ipa getBySymbol(String symbol) {
        Ipa ipa = ipaCache.get(symbol);
        
        if (ipa == null) {
            log.warn("[IPA 캐시 미스] symbol={} (IPA 테이블에 없음)", symbol);
        }
        
        return ipa;
    }
    
    /**
     * ✅ 새로운 IPA 추가 (캐시도 함께 업데이트)
     * @param ipa 새로 저장된 IPA 엔티티
     */
    public void addToCache(Ipa ipa) {
        ipaCache.put(ipa.getSymbol(), ipa);
        log.info("[IPA 캐시 추가] symbol={}, id={}", ipa.getSymbol(), ipa.getId());
    }
    
    /**
     * ✅ 캐시 새로고침 (필요 시)
     */
    public void refresh() {
        ipaCache.clear();
        init();
    }
    
    /**
     * ✅ 캐시 크기 확인
     */
    public int getCacheSize() {
        return ipaCache.size();
    }
}
