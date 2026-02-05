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

    private String type;   // VOWEL, CONSONANT (Enum으로 해도 됨)
}
