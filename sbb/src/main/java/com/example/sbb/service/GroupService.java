package com.example.sbb.service;

import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.GroupSharedQuestion;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.GroupMemberRepository;
import com.example.sbb.repository.GroupSharedQuestionRepository;
import com.example.sbb.repository.StudyGroupRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupSharedQuestionRepository sharedQuestionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public StudyGroup createGroup(String name, SiteUser owner) {
        StudyGroup group = new StudyGroup();
        group.setName(name);
        group.setOwner(owner);
        group.setJoinCode(generateJoinCode());
        StudyGroup saved = studyGroupRepository.save(group);

        GroupMember member = new GroupMember();
        member.setGroup(saved);
        member.setUser(owner);
        member.setRole("OWNER");
        groupMemberRepository.save(member);
        return saved;
    }

    @Transactional
    public Optional<StudyGroup> joinGroupByCode(String joinCode, SiteUser user) {
        Optional<StudyGroup> groupOpt = studyGroupRepository.findByJoinCode(joinCode);
        groupOpt.ifPresent(group -> groupMemberRepository.findByGroupAndUser(group, user)
                .orElseGet(() -> {
                    GroupMember m = new GroupMember();
                    m.setGroup(group);
                    m.setUser(user);
                    m.setRole("MEMBER");
                    return groupMemberRepository.save(m);
                }));
        return groupOpt;
    }

    public List<GroupMember> memberships(SiteUser user) {
        return groupMemberRepository.findByUser(user);
    }

    public List<GroupSharedQuestion> listShared(StudyGroup group) {
        return sharedQuestionRepository.findByGroupOrderBySharedAtDesc(group);
    }

    @Transactional
    public boolean shareQuestion(Long groupId, Long questionId, SiteUser user) {
        StudyGroup group = studyGroupRepository.findById(groupId).orElse(null);
        if (group == null) return false;
        Optional<GroupMember> membership = groupMemberRepository.findByGroupAndUser(group, user);
        if (membership.isEmpty()) return false;

        QuizQuestion question = quizQuestionRepository.findById(questionId).orElse(null);
        if (question == null || !question.getUser().getId().equals(user.getId())) {
            return false;
        }

        if (sharedQuestionRepository.existsByGroup_IdAndQuestion_Id(groupId, questionId)) {
            return true;
        }

        // 공유용 문제 복제 후 저장
        QuizQuestion copy = new QuizQuestion();
        copy.setUser(user);
        copy.setDocument(question.getDocument());
        copy.setFolder(question.getFolder());
        copy.setNumberTag(question.getNumberTag());
        copy.setQuestionText(question.getQuestionText());
        copy.setChoices(question.getChoices());
        copy.setMultipleChoice(question.isMultipleChoice());
        copy.setAnswer(question.getAnswer());
        copy.setExplanation(question.getExplanation());
        quizQuestionRepository.save(copy);

        GroupSharedQuestion shared = new GroupSharedQuestion();
        shared.setGroup(group);
        shared.setQuestion(copy);
        shared.setSharedBy(user);
        sharedQuestionRepository.save(shared);
        return true;
    }

    @Transactional
    public QuizQuestion cloneSharedForUser(Long sharedId, SiteUser user) {
        GroupSharedQuestion shared = sharedQuestionRepository.findById(sharedId).orElse(null);
        if (shared == null) return null;
        StudyGroup group = shared.getGroup();
        if (group == null) return null;
        Optional<GroupMember> membership = groupMemberRepository.findByGroupAndUser(group, user);
        if (membership.isEmpty()) return null;

        QuizQuestion original = shared.getQuestion();
        if (original == null) return null;

        QuizQuestion copy = new QuizQuestion();
        copy.setUser(user);
        copy.setDocument(original.getDocument());
        copy.setFolder(original.getFolder());
        copy.setNumberTag(original.getNumberTag());
        copy.setQuestionText(original.getQuestionText());
        copy.setChoices(original.getChoices());
        copy.setMultipleChoice(original.isMultipleChoice());
        copy.setAnswer(original.getAnswer());
        copy.setExplanation(original.getExplanation());
        copy.setSolved(false);
        copy.setCorrect(null);
        return quizQuestionRepository.save(copy);
    }

    private String generateJoinCode() {
        byte[] bytes = new byte[3];
        String code;
        do {
            random.nextBytes(bytes);
            code = HexFormat.of().withUpperCase().formatHex(bytes);
        } while (studyGroupRepository.findByJoinCode(code).isPresent());
        return code;
    }
}
