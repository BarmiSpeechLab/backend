package com.example.backend.domain.curriculum.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CurriculumType {
    IPA("발음기호"),
    WORD("단어"),
    SENTENCE("문장");

    private final String description;
}
