# TeamOpenSource_Springboot

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
