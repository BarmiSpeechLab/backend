package com.example.backend.domain.report.service;

import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.entity.CurriculumStats;
import com.example.backend.domain.curriculum.entity.Ipa;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.curriculum.repository.IpaRepository;
import com.example.backend.domain.report.entity.DailyStudyLog;
import com.example.backend.domain.report.entity.UserIpaStats;
import com.example.backend.domain.report.repository.DailyStudyLogRepository;
import com.example.backend.domain.report.repository.UserIpaStatsRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Disabled("더미 데이터 생성을 위한 도구로, 필요 시에만 수동으로 실행합니다.")
@SpringBootTest
@Transactional
@Rollback(false) // DB에 실제로 데이터가 들어가도록 함
class DataSeedingTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private CurriculumStatsRepository curriculumStatsRepository;

    @Autowired
    private DailyStudyLogRepository dailyStudyLogRepository;

    @Autowired
    private UserIpaStatsRepository userIpaStatsRepository;

    @Autowired
    private IpaRepository ipaRepository;

    @Test
    @DisplayName("소원, 사랑 유저 더미 데이터 생성")
    void seedData() {
        List<String> targetNicknames = Arrays.asList("sowon", "sarang");
        Random random = new Random();

        for (String nickname : targetNicknames) {
            Optional<User> userOpt = userRepository.findByNickname(nickname);
            if (userOpt.isEmpty()) {
                System.out.println("User not found: " + nickname);
                continue;
            }
            User user = userOpt.get();
            System.out.println("Seeding data for user: " + nickname);

            // 1. DailyStudyLog (최근 30일간 랜덤 학습 기록)
            for (int i = 0; i < 30; i++) {
                LocalDate date = LocalDate.now().minusDays(i);
                // 70% 확률로 공부함
                if (random.nextDouble() > 0.3) {
                    int feedbackCount = random.nextInt(20) + 1; // 1~20회
                    
                    // 기존 데이터가 있으면 가져오고, 없으면 새로 생성
                    DailyStudyLog log = dailyStudyLogRepository.findByUserAndDate(user, date)
                            .orElseGet(() -> dailyStudyLogRepository.save(new DailyStudyLog(user, date)));

                    for(int k=0; k<feedbackCount; k++) {
                        log.increaseFeedbackCount();
                    }
                    dailyStudyLogRepository.save(log);
                }
            }

            // 2. CurriculumStats (커리큘럼 랜덤 완료 처리)
            List<Curriculum> allCurriculums = curriculumRepository.findAll();
            for (Curriculum curr : allCurriculums) {
                // 30% 확률로 학습 기록 생성/업데이트 (너무 많으면 느리므로 확률 낮춤)
                if (random.nextDouble() > 0.7) {
                    int score = random.nextInt(41) + 60; // 60~100점
                    int additionalTry = random.nextInt(5) + 1; // 1~5회 추가

                    // 기존 데이터가 있는지 확인
                    CurriculumStats stats = curriculumStatsRepository.findByUserAndCurriculumId(user, curr.getId())
                            .orElseGet(() -> CurriculumStats.builder()
                                    .user(user)
                                    .curriculum(curr)
                                    .score(0)
                                    .tryCount(0)
                                    .build());

                    stats.updateScore(score);
                    for(int t=0; t<additionalTry; t++) {
                        stats.increaseTryCount();
                    }
                    stats.updateErrorMetrics(100 - score > 0 ? (100-score)/100.0 : 0.0, 
                                           score > 90 ? 0 : (score > 80 ? 1 : 2));
                    
                    curriculumStatsRepository.save(stats);
                }
            }

            // 3. UserIpaStats (IPA 통계 - 레이더 차트용)
            List<Ipa> allIpas = ipaRepository.findAll();
            
            // IPA 기호별 카테고리 매핑 (레이더 차트 8개 카테고리에 맞춤)
            Map<String, String> ipaToCategory = new HashMap<>();
            // Vowel (모음)
            for(String s : new String[]{"æ", "ɑ", "ʌ", "i", "e", "u", "o", "ɔ", "ə", "ɪ", "ʊ"}) ipaToCategory.put(s, "vowel");
            // Plosive (파열음)
            for(String s : new String[]{"p", "b", "t", "d", "k", "g", "ɡ"}) ipaToCategory.put(s, "plosive");
            // Fricative (마찰음)
            for(String s : new String[]{"f", "v", "θ", "ð", "s", "z", "ʃ", "ʒ", "h"}) ipaToCategory.put(s, "fricative");
            // Affricate (파찰음)
            for(String s : new String[]{"tʃ", "dʒ"}) ipaToCategory.put(s, "affricate");
            // Nasal (비음)
            for(String s : new String[]{"m", "n", "ŋ"}) ipaToCategory.put(s, "nasal");
            // Liquid (유음)
            for(String s : new String[]{"l", "r"}) ipaToCategory.put(s, "liquid");
            // Semivowel (반모음)
            for(String s : new String[]{"w", "j"}) ipaToCategory.put(s, "semivowel");
            // Aspirate (기식음 - h가 중복되지만 여기선 따로 분류)
            ipaToCategory.put("h", "aspirate");

            for (Ipa ipa : allIpas) {
                // IPA 타입 보정 (CONSONANT 등 generic한 타입을 구체적 타입으로 변경)
                String correctType = ipaToCategory.getOrDefault(ipa.getSymbol(), "vowel");
                if (!correctType.equals(ipa.getType())) {
                    ipa.updateType(correctType);
                    ipaRepository.save(ipa); // DB 업데이트
                }

                // 70% 확률로 데이터 생성/업데이트
                if (random.nextDouble() > 0.3) {
                    int additionalTotal = random.nextInt(30) + 10; // 10~40회 추가
                    int additionalSuccess = (int) (additionalTotal * (random.nextDouble() * 0.4 + 0.5)); // 50~90% 성공률

                    UserIpaStats ipaStats = userIpaStatsRepository.findByUserAndIpa(user, ipa)
                            .orElseGet(() -> UserIpaStats.builder()
                                    .user(user)
                                    .ipa(ipa)
                                    .totalTryCount(0)
                                    .successCount(0)
                                    .build());

                    for(int s=0; s<additionalSuccess; s++) ipaStats.incrementSuccess();
                    for(int f=0; f<(additionalTotal-additionalSuccess); f++) ipaStats.recordFailure();

                    userIpaStatsRepository.save(ipaStats);
                }
            }
        }
    }
}
