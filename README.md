# 🎮 LCK Match Alarm & Calendar Service (Esports Calendar)

> **LCK 경기 일정을 자동으로 수집하고, 선호하는 팀의 경기 시작 10분 전 디스코드 알림을 보내주는 서비스입니다.**

## 📖 프로젝트 소개
이 프로젝트는 **"내가 좋아하는 e스포츠 팀의 경기를 놓치지 않을 수는 없을까?"** 라는 고민에서 시작되었습니다.
네이버 스포츠 API를 통해 LCK 경기 일정을 자동으로 크롤링하여 DB에 저장하고, 사용자가 구독한 팀의 경기가 시작되기 10분 전에 등록된 Discord Webhook으로 알림을 발송합니다.

### 🌟 주요 기능
* **경기 일정 자동화 (Crawling):** `MatchScheduleCrawler`가 매일 새벽 4시, 네이버 e스포츠 API를 통해 최신 경기 일정을 수집합니다.
* **사용자 맞춤 알림 (Notification):** `TenMinAlarmScheduler`가 매분 실행되어, 경기 시작 10분 전인 경기를 감지하고 사용자가 등록한 Discord 채널로 알림을 보냅니다.
* **팀 기반 구독 시스템:** T1, Gen.G 등 특정 팀을 선택하여 해당 팀의 경기만 알림을 받을 수 있습니다.
* **회원 관리:** ID/PW 유효성 검사 및 중복 방지 로직이 포함된 회원가입/로그인 기능을 제공합니다.

---

## 🛠 기술 스택 (Tech Stack)

### Backend
* **Language:** Java 21
* **Framework:** Spring Boot 3.5.3
* **Database:** PostgreSQL
* **ORM:** Spring Data JPA
* **Build Tool:** Gradle
* **Communication:** WebClient (Naver API, Discord Webhook)
