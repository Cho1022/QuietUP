# 제한형 익명 채팅의 참여자 권한과 비식별 원칙

## 문서 목적

이 문서는 익명 소음 알림에서 파생되는 제한형 채팅의 생성 조건, 참여자 결정, 역할 기반 표현, 메시지 이력과 종료 정책을 설명합니다. 현재 구현은 Spring Boot REST API 기반이며 WebSocket, SSE, FCM과 Android 연결은 포함하지 않습니다.

## NoiseAlert 사건에서만 생성하는 이유

QuietUp의 채팅은 임의 사용자를 검색해 시작하는 DM이 아닙니다. 하나의 `NoiseAlert`에서 소음 상황과 참여 권한이 먼저 확정된 경우에만 생성됩니다.

```text
익명 소음 알림
  → 대상 세대의 최초 정형 응답 REQUEST_CHAT
  → 알림 발신자의 채팅방 생성 요청
  → 제한형 익명 채팅 OPEN
```

클라이언트는 발신자, 응답자, 거주 관계나 세대 ID를 지정하지 않습니다. 서버는 Access Token의 현재 `Residence`, `NoiseAlert.senderResidence`와 최초 `NoiseAlertResponse.responderResidence`를 사용해 참여자를 결정합니다.

## REQUEST_CHAT과 발신자 수락

대상 세대의 인증 거주자가 최초 정형 응답으로 `REQUEST_CHAT`을 선택하면 대화 요청 의사가 기록됩니다. 이 응답만으로 채팅방이 자동 생성되지는 않습니다. 알림 발신자가 다음 API를 호출할 때 요청을 수락한 것으로 보고 방을 생성합니다.

```http
POST /api/v1/noise-alerts/{noiseAlertId}/chat-room
```

생성 조건은 다음과 같습니다.

- 현재 사용자가 알림 발신자여야 합니다.
- 최초 정형 응답이 `REQUEST_CHAT`이어야 합니다.
- 알림이 `RESOLVED` 상태가 아니어야 합니다.
- 같은 알림의 방이 없으면 HTTP 201로 생성합니다.
- 이미 방이 있으면 새로 만들지 않고 같은 방을 HTTP 200으로 반환합니다.

서버는 `NoiseAlert`와 채팅방 존재 여부를 비관적으로 잠가 동시 생성 요청을 직렬화합니다. `chat_rooms.noise_alert_id` unique 제약은 최종 방어선으로 동작합니다.

## 최초 응답자만 참여하는 이유

하나의 대상 세대에는 여러 인증 거주자가 있을 수 있지만 채팅 참여자는 `REQUEST_CHAT`을 실제로 남긴 최초 응답자 한 명으로 고정합니다. 같은 세대라는 이유만으로 가족이나 다른 거주자에게 대화 내용을 공개하면 응답자의 의사와 사건 단위 최소 권한을 벗어나기 때문입니다.

따라서 다음 두 Residence만 방에 접근할 수 있습니다.

- 알림을 생성한 `senderResidence`
- 최초 `REQUEST_CHAT`을 저장한 `responderResidence`

같은 대상 세대의 다른 거주자는 소음 알림 자체를 조회할 수 있어도 채팅방 목록·상세·메시지·종료 API에는 접근할 수 없습니다.

## 역할 기반 비식별 표현

외부 API는 참여자를 실제 신원 대신 현재 사용자 관점의 역할로 표시합니다.

| 현재 사용자 | 본인 메시지 | 상대 메시지 | 상대 표시 문구 |
|---|---|---|---|
| 알림 발신자 | `ME` | `ALERT_RECIPIENT` | 알림을 받은 이웃 |
| 최초 응답자 | `ME` | `ALERT_SENDER` | 알림을 보낸 이웃 |

외부 DTO에는 다음 정보를 반환하지 않습니다.

- 사용자 ID, 이메일과 닉네임
- 발신·응답 Residence ID
- 단지·동·세대 ID와 실제 동·호수
- JWT subject

`chatRoomId`, `messageId`, `noiseAlertId`는 사건 접근 식별자로 반환할 수 있지만, 각 요청에서 현재 Residence가 두 참여자 중 하나인지 다시 확인합니다. 무관한 사용자는 채팅방 존재 여부를 추론하기 어렵도록 HTTP 404 `CHAT_ROOM_NOT_FOUND`로 처리합니다.

## REST API

| API | 동작 |
|---|---|
| `POST /api/v1/noise-alerts/{noiseAlertId}/chat-room` | 발신자가 채팅방 생성 또는 기존 방 조회 |
| `GET /api/v1/chat-rooms` | 현재 Residence가 참여자인 방 목록 조회 |
| `GET /api/v1/chat-rooms/{chatRoomId}` | 참여자의 방 상세 조회 |
| `POST /api/v1/chat-rooms/{chatRoomId}/messages` | 참여자의 메시지 전송 |
| `GET /api/v1/chat-rooms/{chatRoomId}/messages` | 참여자의 메시지 커서 조회 |
| `POST /api/v1/chat-rooms/{chatRoomId}/close` | 참여자의 채팅방 종료 |

메시지는 앞뒤 공백을 제거한 뒤 1자 이상 500자 이하만 저장합니다. 발신 Residence는 요청 값이 아니라 Access Token의 현재 사용자에서 결정합니다. 이미지·파일, 수정과 개별 삭제는 지원하지 않습니다.

메시지 조회는 기본 50건, 최대 100건이며 `afterMessageId`보다 큰 현재 방의 메시지만 ID 오름차순으로 반환합니다. 다른 방의 메시지 ID를 커서로 사용해도 조회 조건은 현재 방에 한정되므로 다른 방의 내용은 노출되지 않습니다.

## 종료와 동시성

두 참여자 중 누구나 채팅방을 종료할 수 있습니다.

- `OPEN`에서 `CLOSED`로 변경하며 `closedAt`을 기록합니다.
- 반복 종료는 기존 종료 시각을 유지하고 HTTP 204를 반환합니다.
- 종료 후 새 메시지는 HTTP 409 `CHAT_ROOM_CLOSED`로 차단합니다.
- 종료 전의 기존 메시지 이력은 계속 조회할 수 있습니다.

메시지 전송과 종료는 모두 같은 `ChatRoom` 행을 `PESSIMISTIC_WRITE`로 조회합니다. 이 잠금으로 종료가 확정된 뒤 새 메시지가 저장되는 경쟁을 차단합니다.

## 현재 미구현 범위

- Android 앱의 REST 채팅 연결과 주기적 조회
- WebSocket, SSE와 실시간 이벤트
- FCM 푸시 알림
- 읽음 수, 타이핑과 온라인 상태
- 이미지·파일 메시지
- 메시지 수정과 개별 삭제
- 욕설 필터와 신고·차단
- 관리자 열람 화면
- Redis, Kafka와 AWS 배포
