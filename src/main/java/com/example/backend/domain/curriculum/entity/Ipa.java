package com.example.backend.domain.curriculum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "ipa")
public class Ipa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol; // 발음 기호 (예: æ, ŋ)

    private String type;   // vowel, plosive, fricative 등

    // ✅ 데이터 보정을 위한 명시적 메서드
    public void updateType(String type) {
        this.type = type;
    }
}
