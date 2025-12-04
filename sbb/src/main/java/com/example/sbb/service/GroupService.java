package com.example.sbb.service;

import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.GroupSharedQuestion;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.group.GroupInvite;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.GroupMemberRepository;
import com.example.sbb.repository.GroupSharedQuestionRepository;
import com.example.sbb.repository.StudyGroupRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import com.example.sbb.repository.GroupInviteRepository;
import com.example.sbb.repository.PendingNotificationRepository;
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
    private final GroupInviteRepository groupInviteRepository;
    private final PendingNotificationRepository pendingNotificationRepository;
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

    public List<GroupMember> membersOf(StudyGroup group) {
        if (group == null) return List.of();
        List<GroupMember> byGroup = groupMemberRepository.findByGroupOrderByJoinedAtAsc(group);
        if (byGroup == null || byGroup.isEmpty()) {
            return groupMemberRepository.findByGroup(group);
        }
        return byGroup;
    }

    public boolean isOwner(StudyGroup group, SiteUser user) {
        if (group == null || user == null) return false;
        return group.getOwner() != null && group.getOwner().getId().equals(user.getId());
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

    @Transactional
    public boolean deleteShared(Long sharedId, SiteUser user) {
        if (sharedId == null || user == null) return false;
        GroupSharedQuestion shared = sharedQuestionRepository.findById(sharedId).orElse(null);
        if (shared == null) return false;
        if (shared.getGroup() == null || shared.getSharedBy() == null) return false;
        boolean ownerOrSharer = shared.getGroup().getOwner().getId().equals(user.getId())
                || shared.getSharedBy().getId().equals(user.getId());
        if (!ownerOrSharer) return false;
        sharedQuestionRepository.delete(shared);
        return true;
    }

    @Transactional
    public int inviteFriends(Long groupId, SiteUser owner, List<SiteUser> targets) {
        StudyGroup group = studyGroupRepository.findById(groupId).orElse(null);
        if (group == null || !isOwner(group, owner) || targets == null || targets.isEmpty()) {
            return 0;
        }
        int created = 0;
        for (SiteUser t : targets) {
            if (t == null) continue;
            if (groupMemberRepository.findByGroupAndUser(group, t).isPresent()) continue;
            if (groupInviteRepository.existsByGroupAndToUserAndStatus(group, t, GroupInvite.Status.PENDING)) continue;
            GroupInvite invite = new GroupInvite();
            invite.setGroup(group);
            invite.setFromUser(owner);
            invite.setToUser(t);
            invite.setStatus(GroupInvite.Status.PENDING);
            groupInviteRepository.save(invite);
            created++;
        }
        return created;
    }

    @Transactional
    public boolean removeMember(Long groupId, Long memberId, SiteUser owner) {
        StudyGroup group = studyGroupRepository.findById(groupId).orElse(null);
        if (group == null || !isOwner(group, owner)) return false;
        if (memberId == null) return false;
        GroupMember gm = groupMemberRepository.findById(memberId).orElse(null);
        if (gm == null || gm.getGroup() == null || !gm.getGroup().getId().equals(groupId)) return false;
        if (gm.getUser() != null && group.getOwner().getId().equals(gm.getUser().getId())) return false;
        groupMemberRepository.delete(gm);
        return true;
    }

    public List<GroupInvite> inboxInvites(SiteUser user) {
        return groupInviteRepository.findByToUserAndStatus(user, GroupInvite.Status.PENDING);
    }

    public List<GroupInvite> sentInvites(SiteUser user) {
        return groupInviteRepository.findByFromUserAndStatus(user, GroupInvite.Status.PENDING);
    }

    @Transactional
    public boolean acceptInvite(Long inviteId, SiteUser user) {
        var invOpt = groupInviteRepository.findByIdAndToUser(inviteId, user);
        if (invOpt.isEmpty()) return false;
        GroupInvite invite = invOpt.get();
        invite.setStatus(GroupInvite.Status.ACCEPTED);
        groupInviteRepository.save(invite);
        StudyGroup group = invite.getGroup();
        if (group != null && groupMemberRepository.findByGroupAndUser(group, user).isEmpty()) {
            GroupMember gm = new GroupMember();
            gm.setGroup(group);
            gm.setUser(user);
            gm.setRole("MEMBER");
            groupMemberRepository.save(gm);
        }
        return true;
    }

    @Transactional
    public boolean rejectInvite(Long inviteId, SiteUser user) {
        var invOpt = groupInviteRepository.findByIdAndToUser(inviteId, user);
        if (invOpt.isEmpty()) return false;
        GroupInvite invite = invOpt.get();
        invite.setStatus(GroupInvite.Status.REJECTED);
        groupInviteRepository.save(invite);
        return true;
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

    public GroupSharedQuestion getShared(Long id) {
        if (id == null) return null;
        return sharedQuestionRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean deleteGroup(StudyGroup group) {
        if (group == null) return false;
        // 1) 공유된 문제 제거
        sharedQuestionRepository.deleteAllByGroup(group);
        // 2) 그룹 초대 제거
        groupInviteRepository.deleteAll(groupInviteRepository.findAll().stream()
                .filter(inv -> inv.getGroup() != null && inv.getGroup().getId().equals(group.getId()))
                .toList());
        // 3) 멤버 제거
        groupMemberRepository.deleteAll(groupMemberRepository.findByGroup(group));
        // 4) 그룹 엔티티 제거
        studyGroupRepository.delete(group);
        return true;
    }

    @Transactional
    public boolean leaveGroup(StudyGroup group, SiteUser user) {
        if (group == null || user == null) return false;
        List<GroupMember> members = groupMemberRepository.findByGroupOrderByJoinedAtAsc(group);
        Optional<GroupMember> mine = members.stream()
                .filter(m -> m.getUser() != null && m.getUser().getId().equals(user.getId()))
                .findFirst();
        if (mine.isEmpty()) return false;
        // 오너가 나갈 경우: 나를 삭제하고 다음 가입자에게 owner 위임
        boolean isOwner = group.getOwner() != null && group.getOwner().getId().equals(user.getId());
        groupMemberRepository.delete(mine.get());
        if (isOwner) {
            // 남은 멤버 중 가장 먼저 가입한 사람에게 오너 위임
            List<GroupMember> remaining = groupMemberRepository.findByGroupOrderByJoinedAtAsc(group);
            if (!remaining.isEmpty()) {
                GroupMember nextOwner = remaining.get(0);
                group.setOwner(nextOwner.getUser());
                nextOwner.setRole("OWNER");
                groupMemberRepository.save(nextOwner);
                studyGroupRepository.save(group);
            } else {
                // 남은 멤버가 없으면 그룹 삭제
                deleteGroup(group);
            }
        }
        return true;
    }
}
