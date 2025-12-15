# TeamOpenSource\_Springboot

[최봉규의 실습 레포지토리](https://github.com/grapebongbongs/bong_silsup)

[김성락의 실습 레포지토리](https://github.com/Rakjiori/Rak_silsup)



🧠 AI-Powered Study Automation Platform

An intelligent web service that automatically generates study questions from uploaded documents and delivers daily learning tasks through push notifications.
Built with Spring Boot, React, Azure, and OpenAI GPT.

🚀 Overview

This project helps users maintain consistent learning habits by automatically analyzing their uploaded PDFs or documents, creating AI-generated quiz sets, and delivering personalized review tasks every day.
Users can join groups, share problem sets, and track their learning progress together.

🏗️ System Flow

Upload PDF → stored in Azure Blob Storage

AI Analysis (GPT) → extracts text, summarizes content, and generates quiz questions

Question Set Storage → quiz JSON saved in Blob; link + metadata stored in DB

Push Notifications → scheduled via Azure Application Service for daily reminders

User Interaction → solve quizzes, record answers, and review wrong questions

Group Sharing → share question sets, compare results, and discuss answers

⚙️ Tech Stack

Frontend: React + TypeScript
Backend: Spring Boot (Java, JPA, Gradle)
Database: Azure MySQL / Oracle Cloud (optional)
Cloud Services: Azure Blob Storage, Application Service, Notification Hub
AI Integration: OpenAI GPT API for text extraction, summarization, and question generation



Youtube Practice\_Video Link: https://youtu.be/cnvCwzp-E4c



<프로젝트를 진행하면서 느낀 점>

* 김성락: 이번 프로젝트를 통해 혼자서는 힘들었을 문제도 협업을 통해 단계적으로 해결할 수 있다는 점을 배웠습니다. 또한 막막하기만 했던 웹개발을 세분화하고 하나씩 공부해보니 개발 과정을 전반적으로 알게 되었습니다. 또한 교재를 기반으로 스프링부트 프로젝트를 직접 구현하며 MVC 구조, 서비스 계층 분리, JPA를 활용한 데이터 처리 흐름을 실제로 적용해 볼 수 있었고, 자신감을 얻게 되었습니다. 



* 최봉규: 프로젝트를 통해 단순한 기능 구현을 넘어, 기획–설계–구현–배포까지 이어지는 웹 서비스 개발의 전체 흐름을 직접 경험할 수 있었습니다. 특히 PDF 업로드, 텍스트 추출, AI 기반 문제 생성, 퀴즈 풀이 및 통계·알림 기능이 서로 독립적이면서도 유기적으로 연결되도록 설계하는 과정에서 백엔드 구조 설계의 중요성을 깊이 이해하게 되었습니다. Spring Boot 기반의 Controller–Service–Repository 계층 분리를 통해 웹페이지에서의 백엔드 구성을 이해할 수 있었고, Gemini API와 같은 외부 AI 서비스 연동 경험을 통해 다가오는 AI시대에서 구현할 웹페이지에 AI를 활용하여 AI 서비스를 제공할 수 있다는 자신감을 얻을 수 있었습니다. 



<향후 발전 방향>

* AI 문제 생성 품질 향상: 현재는 PDF 전체 텍스트를 기반으로 문제를 생성하지만, 향후에는 문단 단위 중요도 분석, 난이도 분류, 사용자 학습 이력 기반 맞춤 문제 생성 기능을 추가하여 개인화된 학습 시스템을 제공할 것이다.



* PDF 분석 기능 향상: 현재는 PDF의 텍스트 추출만을 가지고 문제를 생성하는 시스템을 취하고 있다. 향후에는 OCR 기반 이미지 분석 기법을 활용하여 이미지 파일이 존재하는 PDF 파일에서도 문제를 적절하게 생성할 수 있도록 할 것이다. 



* 모바일 환경 지원: 현재 구현한 웹페이지에서의 알림은 브라우저를 기반으로 하여 푸시 알림을 제공하고 있다. 그러나 현재 대부분의 사용자는 모바일 환경을 통해 다양한 서비스를 이용하고 있다. 그렇기에 향후에는 모바일 환경도 지원하는 반응형 웹을 구현하여 모바일 환경에서 서비스를 편하게 이용할 수 있도록 할 것이다.



* 협업 기능 확장: 현재의 시스템은 친구추가를 하는 기능을 통해 그룹 초대를 가능하게 함으로써 오프라인에서 아는 사람들과의 스터디 환경만 지원하고 있다. 그러나 향후에는 공부하고 있는 주제 태그를 추가하여 같은 공부주제 태그를 가진 사람들끼리 매칭을 하여 스터디 그룹을 형성할 수 있는 시스템을 구현할 것이다.
