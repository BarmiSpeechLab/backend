package com.example.backend.domain.curriculum.repository;

import com.example.backend.domain.curriculum.entity.Ipa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IpaRepository extends JpaRepository<Ipa, Long> {
    
    // IPA 기호로 조회
    Optional<Ipa> findBySymbol(String symbol);
}
