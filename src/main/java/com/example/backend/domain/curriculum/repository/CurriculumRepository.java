package com.example.backend.domain.curriculum.repository;

import com.example.backend.domain.curriculum.entity.Curriculum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurriculumRepository extends JpaRepository<Long, Curriculum> {
    // ID 순서대로 정렬해서 전체 조회 (화면에 순서대로 뿌리기 위함)
    List<Curriculum> findAllByOrderByIdAsc();
}
