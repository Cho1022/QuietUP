# QuietUp

익명 알림으로 층간소음 문제를 전달하고 대응 이력을 관리하는 아파트 커뮤니티 서비스

QuietUp은 대면 갈등 없이 층간소음 상황을 알리고, 당사자와 관리 주체가 대응 이력을 확인할 수 있도록 만드는 것을 목표로 합니다. 현재 저장소에는 초기 Android MVP가 보존되어 있으며, 익명 소음 알림과 서버 기반 이력 관리는 향후 개편 목표입니다.

## 개발 기간

- 초기 Android MVP: 2025.10 ~ 2026.01.31
- 서비스 전면 개편: 2026.08 ~

## 기존 Android MVP

현재 소스와 빌드 결과에서 확인된 범위는 다음과 같습니다.

- Firebase Authentication 이메일 회원가입·로그인
- 사용자 추가 정보 입력 및 저장
- 게시판 게시글 작성·조회·수정·삭제
- Firebase Realtime Database 기반 채팅
- 전체 사용자 범위 소음 기록 작성·조회·수정·삭제
- Android XML 기반 UI
- Debug APK 빌드

### 기존 MVP의 한계

- Firebase에 직접 연결되어 클라이언트와 데이터 계층이 강하게 결합되어 있습니다.
- 소음 기록이 아파트·동·호 단위가 아닌 전체 사용자 범위로 관리됩니다.
- 익명 소음 알림, 수신 확인, 대응 상태, 관리 주체용 이력 관리가 완성되어 있지 않습니다.
- 현재 Firebase 프로젝트 환경은 더 이상 운영되지 않으며 원격 데이터 보존·이관 여부도 확인되지 않았습니다.
- 단위 테스트는 기본 예제 1건뿐이며, 계측 테스트 실행 환경은 확인하지 못했습니다.

## 개편 핵심 흐름

1. 입주민이 아파트·동·호 정보를 등록합니다.
2. 소음 발생 위치와 시간대를 선택해 익명 알림을 보냅니다.
3. 대상 세대는 알림을 확인하고 대응 상태를 남깁니다.
4. 반복 발생과 대응 이력은 권한에 따라 입주민 또는 관리 주체에게 제공됩니다.
5. 게시판·채팅 등 기존 커뮤니티 기능은 서버 API 전환 범위에 맞춰 단계적으로 이전합니다.

## 목표 아키텍처

```text
Android(Java)
  → HTTPS REST API
  → Spring Boot
  → Spring Data JPA
  → Amazon RDS for MySQL
```

목표 아키텍처 전체는 아직 구현되지 않았습니다. 현재는 Spring Boot와 로컬 MySQL 실행 기반, 서버 인증 API까지 구성되어 있으며 Android REST 연동과 AWS 배포는 아직 구현하지 않았습니다. 다음 원칙을 기준으로 전환합니다.

- Android 앱은 MySQL에 직접 연결하지 않고 HTTPS API만 호출합니다.
- 인증과 권한 검증은 서버에서 수행합니다.
- RDS는 공개 노출을 피하고 비공개 네트워크에서 운영합니다.
- 파일 저장이 필요할 때만 Amazon S3를 도입합니다.
- AWS 자격 증명은 Android 앱과 Git 저장소에 넣지 않습니다.

## 기술 스택

### 현재 보존된 MVP

- Android
- Java
- XML View
- Gradle 8.13
- Android Gradle Plugin 8.12.3
- Firebase Authentication
- Firebase Realtime Database
- Firebase Storage·Analytics·Firestore 의존성

### 현재 구현된 서버 기반

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC·Validation·Spring Data JPA·Spring Security
- JWT Access Token·해시 저장형 Refresh Token
- MySQL 8.4·Flyway
- Docker Compose
- Actuator Health Check
- MySQL Testcontainers 통합 테스트

### 전환 목표(미구현)

- Android Java
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- Flyway
- Docker
- Amazon EC2
- Amazon RDS
- Amazon CloudWatch
- Amazon S3(필요 시)

## 현재 구현 상태

- 기존 Android Java MVP와 Firebase 레거시 코드를 보존합니다.
- Java 21 기반 Spring Boot 서버 실행 기반을 구성했습니다.
- 로컬 MySQL 8.4 Docker Compose 환경을 구성했습니다.
- `GET /actuator/health`로 서버와 DB 연결 상태를 확인할 수 있습니다.
- 이메일 회원가입·로그인과 BCrypt 비밀번호 저장을 구현했습니다.
- JWT Access Token 인증과 현재 사용자 조회를 구현했습니다.
- Refresh Token 해시 저장·회전과 멱등 로그아웃을 구현했습니다.
- Testcontainers가 실제 MySQL 8.4에서 인증 API와 Flyway 스키마를 검증합니다.
- Android 앱은 아직 Spring Boot REST API에 연결되지 않았습니다.

### 아직 구현되지 않은 항목

- 단지 및 거주 인증
- 익명 소음 알림
- 신고 및 차단
- Android REST 연결
- Firebase 제거
- AWS 배포

인증 API 계약과 보안 원칙은 [인증 API 문서](docs/api/authentication.md)에서 확인할 수 있습니다.

## 로컬 빌드 기준선

### 확인된 환경

- JDK: 21.0.11
- Gradle Wrapper: 8.13
- Android Gradle Plugin: 8.12.3
- compileSdk / targetSdk / minSdk: 34 / 34 / 33
- Android SDK Platform: 34
- Android SDK Build Tools: 35.0.0

로컬 `android/local.properties`에 실제 Android SDK 경로를 설정하고 `android/` 디렉터리에서 실행합니다.

```powershell
cd android
.\gradlew.bat :app:clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

기준선 확인 결과:

- Debug APK 생성 성공
- JVM 단위 테스트 1건 통과
- lint: 오류 0건, 경고 185건
- Android 기기 또는 에뮬레이터가 없어 설치·초기 실행은 확인하지 못함

## 저장소 구조

```text
QuietUP/
├─ android/             # 기존 Android Java MVP
├─ server/              # Spring Boot 서버 실행 기반
├─ docs/
├─ docker-compose.yml   # 로컬 MySQL 8.4
├─ .env.example
└─ README.md
```

## 서버 실행 방법

Windows PowerShell 기준입니다.

1. 저장소 루트에서 환경변수 예시를 로컬 파일로 복사합니다.

```powershell
Copy-Item .env.example .env
```

2. `.env`의 DB 비밀번호를 로컬 개발용 값으로 변경하고, `JWT_SECRET_BASE64`에는 Base64로 인코딩한 32바이트 이상의 임의 키를 설정합니다. `.env`는 Git에서 제외됩니다.
3. 로컬 MySQL을 시작합니다.

```powershell
docker compose --env-file .env up -d --wait mysql
```

4. `.env`의 DB 연결값을 현재 PowerShell 프로세스에 주입하고 서버를 실행합니다.

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^(DB_URL|DB_USERNAME|DB_PASSWORD|JWT_SECRET_BASE64|JWT_ACCESS_EXPIRATION_SECONDS|JWT_REFRESH_EXPIRATION_DAYS)=(.*)$') {
        Set-Item "Env:$($matches[1])" $matches[2]
    }
}

cd server
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

5. 다른 PowerShell 창에서 Health Check를 확인합니다.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

정상 응답은 HTTP 200과 `{"status":"UP"}`입니다. 종료 후 MySQL 컨테이너만 내릴 때는 저장소 루트에서 `docker compose --env-file .env down`을 실행합니다. named volume은 삭제하지 않습니다.

## 서버 테스트

Testcontainers 통합 테스트는 로컬 Docker Engine이 실행 중이어야 합니다. 로컬 Compose MySQL은 실행하지 않아도 됩니다.

```powershell
cd server
.\gradlew.bat clean test
.\gradlew.bat bootJar
```

## 전환 로드맵

1. 기존 Android 빌드 기준선과 Git 이력 보존
2. Android와 서버 디렉터리 경계 정의
3. Spring Boot 기본 프로젝트와 로컬 실행 환경 구성
4. MySQL 스키마와 Flyway 마이그레이션 설계
5. 서버 인증·인가와 사용자 도메인 구현
6. 아파트·세대 등록 및 권한 모델 구현
7. 익명 소음 알림과 대응 이력 API 구현
8. Android의 Firebase 직접 접근을 REST API 호출로 교체
9. EC2·RDS·CloudWatch 기반 배포 및 운영 설정
10. Firebase 의존성 제거와 회귀 검증

## 보안 고려사항

- `local.properties`, `google-services.json`, 키스토어, 환경 변수 파일, AWS 자격 증명은 Git에서 제외합니다.
- Android 앱에 데이터베이스 비밀번호나 AWS 액세스 키를 포함하지 않습니다.
- 서버 비밀값은 배포 환경의 비밀 관리 수단으로 주입합니다.
- 인증·권한·입력 검증은 서버 경계에서 적용합니다.
- 개인정보와 대응 이력의 조회 범위를 최소 권한 원칙으로 제한합니다.

## Firebase 레거시 처리 원칙

기존 코드는 Firebase Authentication과 Realtime Database를 사용합니다. 해당 Firebase 환경은 더 이상 운영되지 않으며, 원격 데이터가 보존되거나 새 시스템으로 이관되었다고 가정하지 않습니다. 서버 전환 중에는 현재 코드를 동작·데이터 흐름 파악용으로만 보존하고, 마이그레이션이 끝난 뒤 Firebase Authentication과 Database 의존성을 제거합니다.

자세한 배경과 결정 내용은 [프로젝트 이력](docs/project-history.md)에서 확인할 수 있습니다.
