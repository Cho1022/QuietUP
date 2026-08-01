# 인증 API

QuietUp 서버의 이메일 회원가입, 로그인, 토큰 재발급, 로그아웃, 현재 사용자 조회 계약을 설명합니다. 모든 API의 기본 경로는 `/api/v1`입니다. Android 앱은 아직 이 API에 연결되지 않았으며 기존 Firebase 코드는 그대로 보존되어 있습니다.

## 토큰 역할

| 구분 | Access Token | Refresh Token |
| --- | --- | --- |
| 형식 | 서버가 HS256으로 서명한 JWT | SecureRandom으로 만든 불투명한 URL-safe 문자열 |
| 기본 만료 | 15분 | 14일 |
| 용도 | 보호 API 인증 | Access Token과 Refresh Token 재발급 |
| 서버 저장 | 저장하지 않음 | SHA-256 해시만 저장 |
| 폐기 | 짧은 만료 시간으로 관리 | 재발급 또는 로그아웃 시 폐기 |

Refresh Token 원문이 DB에 없으면 DB 유출만으로 토큰을 바로 사용할 수 없습니다. 서버는 요청으로 받은 원문을 SHA-256으로 해시한 뒤 저장된 해시를 조회합니다.

재발급은 하나의 트랜잭션에서 기존 행을 쓰기 잠금으로 조회하고, 유효성 확인과 폐기, 새 토큰 저장을 순서대로 처리합니다. 같은 토큰으로 동시에 재발급해도 한 요청만 성공합니다.

로그아웃은 멱등합니다. 정상 토큰은 폐기하고, 이미 폐기됐거나 존재하지 않는 토큰도 HTTP 204를 반환해 토큰 존재 여부를 노출하지 않습니다.

## JWT 최소 Claim 원칙

Access Token에는 다음 세 claim만 포함합니다.

```json
{
  "sub": "1",
  "iat": 0,
  "exp": 0
}
```

`sub`는 서버 내부 사용자 ID입니다. 이메일, 닉네임, 거주 정보, Firebase UID, 익명 별칭은 JWT에 넣지 않습니다. 향후 거주 정보와 권한은 JWT claim이 아니라 요청 시점의 DB 상태로 검증합니다.

## API

### 회원가입

`POST /api/v1/auth/signup`

```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "조용한이웃"
}
```

성공 시 HTTP 201을 반환합니다.

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "조용한이웃"
}
```

이메일은 앞뒤 공백을 제거하고 소문자로 저장합니다. 중복 이메일은 HTTP 409 `DUPLICATE_EMAIL`, 잘못된 입력은 HTTP 400 `VALIDATION_ERROR`입니다. 비밀번호는 영문과 숫자를 포함한 8~64자이며 BCrypt 해시만 저장합니다.

### 로그인

`POST /api/v1/auth/login`

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

성공 시 HTTP 200을 반환합니다.

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt-access-token>",
  "accessTokenExpiresIn": 900,
  "refreshToken": "<opaque-refresh-token>"
}
```

존재하지 않는 이메일과 잘못된 비밀번호는 모두 HTTP 401 `INVALID_CREDENTIALS`로 응답합니다.

### 토큰 재발급

`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "<opaque-refresh-token>"
}
```

성공 시 기존 Refresh Token을 폐기하고 HTTP 200과 새 Access Token·Refresh Token을 반환합니다. 만료, 폐기, 미존재 토큰은 모두 HTTP 401 `INVALID_REFRESH_TOKEN`입니다.

### 로그아웃

`POST /api/v1/auth/logout`

```json
{
  "refreshToken": "<opaque-refresh-token>"
}
```

정상, 폐기, 미존재 토큰 모두 HTTP 204를 반환합니다.

### 현재 사용자

`GET /api/v1/users/me`

```http
Authorization: Bearer <jwt-access-token>
```

성공 시 HTTP 200을 반환합니다.

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "조용한이웃"
}
```

인증 헤더가 없거나 Access Token이 잘못된 경우 HTTP 401 `INVALID_ACCESS_TOKEN`입니다.

## 오류 응답

일반 오류는 `code`와 `message`만 반환합니다.

```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다."
}
```

입력 검증 오류에는 `fieldErrors`가 추가됩니다. 응답에는 스택 트레이스, SQL 메시지, JWT 파싱 상세, 비밀번호, 토큰 원문을 포함하지 않습니다.

## 환경변수

| 변수 | 설명 | 로컬 기본 예시 |
| --- | --- | --- |
| `JWT_SECRET_BASE64` | HS256 서명 키의 Base64 값 | 실제 임의 키를 로컬 `.env`에만 설정 |
| `JWT_ACCESS_EXPIRATION_SECONDS` | Access Token 만료 초 | `900` |
| `JWT_REFRESH_EXPIRATION_DAYS` | Refresh Token 만료 일 | `14` |

`JWT_SECRET_BASE64`를 디코딩할 수 없거나 결과가 32바이트보다 짧으면 애플리케이션 시작이 실패합니다. 실제 키는 Git 추적 파일이나 로그에 남기지 않습니다.

## 익명성 보호 원칙

서버는 인증과 신고·차단·도배 방지를 위해 사용자를 내부적으로 식별하지만, 향후 익명 알림·채팅·게시판 응답에서는 다른 사용자의 `users.id`, 이메일, Firebase UID, 거주 ID, 실제 동·호수를 노출하지 않습니다. `/users/me`만 본인 정보이므로 본인의 ID, 이메일, 닉네임을 반환합니다.

향후 발신자는 클라이언트가 ID나 거주 정보를 지정하는 방식이 아니라 Access Token의 subject와 DB 상태를 바탕으로 서버가 결정합니다. 거주 테이블, 익명 별칭, 소음 알림은 현재 인증 범위에 포함되지 않습니다.
