package com.example.backend.domain.curriculum.repository;

import com.example.backend.domain.curriculum.entity.Curriculum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
    // ID 순서대로 정렬해서 전체 조회 (화면에 순서대로 뿌리기 위함)
    List<Curriculum> findAllByOrderByIdAsc();
}
