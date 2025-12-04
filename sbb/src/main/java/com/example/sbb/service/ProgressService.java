package com.example.sbb.service;

import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.StudySession;
import com.example.sbb.repository.QuizQuestionRepository;
import com.example.sbb.domain.user.UserRepository;
import com.example.sbb.repository.StudySessionRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserRepository userRepository;
    private final StudySessionRepository studySessionRepository;

    @Getter
    public static class FolderStat {
        private final String folderName;
        private final long solved;
        private final long correct;
        private final double accuracy;

        public FolderStat(String folderName, long solved, long correct) {
            this.folderName = folderName;
            this.solved = solved;
            this.correct = correct;
            this.accuracy = solved == 0 ? 0 : Math.round((correct * 1000.0 / solved)) / 10.0;
        }
    }

    public record BadgeStatus(String id, String name, String desc, boolean unlocked, boolean equipped, String requirement) {}

    public record Stats(long totalSolved,
                        long totalCorrect,
                        long todaySolved,
                        int maxCorrectStreak,
                        int currentStreak,
                        long todaySeconds,
                        List<FolderStat> folderStats,
                        Optional<FolderStat> weakest) {}

    public Stats computeStats(SiteUser user) {
        List<QuizQuestion> solved = quizQuestionRepository.findByUserAndSolvedTrueOrderByCreatedAtAsc(user);
        long totalSolved = solved.size();
        long totalCorrect = solved.stream().filter(q -> Boolean.TRUE.equals(q.getCorrect())).count();

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        long todaySolved = solved.stream().filter(q -> q.getCreatedAt() != null
                && !q.getCreatedAt().isBefore(start)
                && !q.getCreatedAt().isAfter(end)).count();
        long todaySeconds = studySessionRepository.findByUserAndDate(user, LocalDate.now()).stream()
                .mapToLong(StudySession::getSeconds).sum();

        Set<String> excluded = Set.of("맞은 문제", "틀린 문제");
        Map<String, List<QuizQuestion>> byFolder = solved.stream()
                .filter(q -> q.getFolder() != null && !excluded.contains(q.getFolder().getName()))
                .collect(Collectors.groupingBy(q -> q.getFolder().getName()));
        List<FolderStat> folderStats = byFolder.entrySet().stream()
                .map(e -> {
                    long solvedCnt = e.getValue().size();
                    long correctCnt = e.getValue().stream().filter(q -> Boolean.TRUE.equals(q.getCorrect())).count();
                    return new FolderStat(e.getKey(), solvedCnt, correctCnt);
                })
                .sorted(Comparator.comparing(FolderStat::getFolderName))
                .toList();

        Optional<FolderStat> weakest = folderStats.stream()
                .filter(f -> f.getSolved() >= 5)
                .min(Comparator.comparing(FolderStat::getAccuracy));

        int maxCorrectStreak = calcMaxCorrectStreak(solved);

        return new Stats(totalSolved, totalCorrect, todaySolved, maxCorrectStreak, user.getStreak(), todaySeconds, folderStats, weakest);
    }

    private int calcMaxCorrectStreak(List<QuizQuestion> solved) {
        int max = 0;
        int cur = 0;
        for (QuizQuestion q : solved) {
            if (Boolean.TRUE.equals(q.getCorrect())) {
                cur++;
                max = Math.max(max, cur);
            } else {
                cur = 0;
            }
        }
        return max;
    }

    public List<BadgeStatus> badges(SiteUser user, Stats stats) {
        Set<String> owned = parse(user.getPurchasedBadges());
        Set<String> unlocked = new HashSet<>(owned);

        boolean conceptMaster = stats.folderStats().stream().anyMatch(f -> f.getSolved() >= 30);
        boolean diligent = stats.currentStreak() >= 7;
        boolean proAccurate = stats.maxCorrectStreak() >= 5;
        boolean marathon = stats.totalSolved() >= 100;
        boolean starter = stats.totalSolved() >= 10;

        if (conceptMaster) unlocked.add("concept_master");
        if (diligent) unlocked.add("diligent_master");
        if (proAccurate) unlocked.add("pro_sharpshooter");
        if (marathon) unlocked.add("marathon_runner");
        if (starter) unlocked.add("warmup_starter");

        List<BadgeStatus> list = new ArrayList<>();
        list.add(build("warmup_starter", "워밍업", "총 10문제 이상 풀기", stats.totalSolved() >= 10, user.getActiveBadge(), "10문제 해결"));
        list.add(build("concept_master", "개념마스터", "한 과목 30문제 이상 해결", conceptMaster, user.getActiveBadge(), "한 과목 30문제 해결"));
        list.add(build("diligent_master", "성실함의 달인", "연속 7일 이상 문제 풀기", diligent, user.getActiveBadge(), "연속 7일 풀이"));
        list.add(build("pro_sharpshooter", "프로 꼼꼼러", "연속 5문제 정답", proAccurate, user.getActiveBadge(), "연속 5문제 정답"));
        list.add(build("marathon_runner", "마라톤러", "총 100문제 이상 해결", marathon, user.getActiveBadge(), "100문제 해결"));

        // persist unlocked
        if (!unlocked.equals(owned)) {
            Set<String> merged = new HashSet<>(owned);
            merged.addAll(unlocked);
            user.setPurchasedBadges(String.join(",", merged));
            userRepository.save(user);
        }
        return list;
    }

    private BadgeStatus build(String id, String name, String desc, boolean unlocked, String activeBadge, String requirement) {
        return new BadgeStatus(id, name, desc, unlocked, id.equals(activeBadge), requirement);
    }

    private Set<String> parse(String raw) {
        if (raw == null || raw.isBlank()) return new HashSet<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }
}
