package com.example.sbb.domain.question;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {
  private String title;
  private String answer;
}
