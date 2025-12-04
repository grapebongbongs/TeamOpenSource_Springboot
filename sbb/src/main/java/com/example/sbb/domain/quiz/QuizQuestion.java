package com.example.sbb.domain.quiz;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.Folder;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.group.GroupSharedQuestion;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 문제를 받은 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    private SiteUser user;

    // 어떤 PDF 세트에서 만들어졌는지 (필수는 아님)
    @ManyToOne(fetch = FetchType.LAZY)
    private DocumentFile document;    // 필요 없다면 나중에 null로 둬도 됨

    // 어떤 과목(폴더)에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    private Folder folder;

    // [1], [2] 같은 문제 번호 (문제 생성 시의 번호)
    private Integer numberTag;

    @Column(columnDefinition = "TEXT")
    private String questionText;      // 문제 내용 전체

    @Column(columnDefinition = "TEXT")
    private String choices;           // 보기 문자열 (객관식일 때만 사용)

    private boolean multipleChoice;   // 객관식 여부

    private String answer;            // 정답

    @Column(columnDefinition = "TEXT")
    private String explanation;       // 해설

    // 상태 관리
    private boolean solved = false;   // 풀었는지
    private Boolean correct;          // 맞았는지(null: 아직 안품)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<GroupSharedQuestion> sharedQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<QuestionDiscussion> discussions = new ArrayList<>();

    // ===== getter / setter =====
    public Long getId() {
        return id;
    }

    public SiteUser getUser() {
        return user;
    }

    public void setUser(SiteUser user) {
        this.user = user;
    }

    public DocumentFile getDocument() {
        return document;
    }

    public void setDocument(DocumentFile document) {
        this.document = document;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public Integer getNumberTag() {
        return numberTag;
    }

    public void setNumberTag(Integer numberTag) {
        this.numberTag = numberTag;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getChoices() {
        return choices;
    }

    public void setChoices(String choices) {
        this.choices = choices;
    }

    public boolean isMultipleChoice() {
        return multipleChoice;
    }

    public void setMultipleChoice(boolean multipleChoice) {
        this.multipleChoice = multipleChoice;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @Transient
    public String getCreatedAtFormatted() {
        if (this.createdAt == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        return this.createdAt.format(formatter);
    }
}
