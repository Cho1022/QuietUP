# 익명 소음 알림의 공동체 경계와 비식별 원칙

## 문서 목적

이 문서는 현재 구현된 익명 소음 알림의 대상 세대 계산, 접근 권한, 정형 응답, 해결 상태와 비식별 경계를 설명합니다. 현재 범위는 인증된 거주자가 같은 주거 공동체 안의 바로 위층 또는 아래층 세대에 알림을 보내고 대응 이력을 확인하는 서버 API입니다.

`REQUEST_CHAT`에서 파생되는 제한형 REST 채팅은 구현되어 있습니다. Android REST 연결, FCM 알림 전송과 WebSocket·SSE 실시간 연결은 아직 구현하지 않았습니다.

## 공동체 경계

서버는 Access Token의 사용자와 인증된 거주 관계를 기준으로 공동체를 결정합니다.

```text
Access Token의 사용자
  → Residence
  → ApartmentUnit
  → ApartmentBuilding
  → ApartmentComplex
```

클라이언트는 `senderUserId`, `senderResidenceId`, `apartmentComplexId`, `targetUserId`, `targetResidenceId`, `targetUnitId`를 지정하지 않습니다. 서버가 현재 사용자의 `Residence`에서 발신 세대와 단지를 구하고, 같은 동의 층·라인 기준으로 대상 세대를 계산합니다.

이 경계는 여러 동을 가진 아파트와 한 동으로 표현되는 빌라 모두에 동일하게 적용됩니다. 현재 단계에서는 별도의 `communityId`나 주거 유형 필드를 추가하지 않습니다.

## 대상 세대 계산

현재 지원하는 방향은 `UP`, `DOWN`뿐입니다.

| 방향 | 동 | 층 | 라인 |
|---|---|---|---|
| `UP` | 현재 세대와 동일 | 현재 층 + 1 | 현재 세대와 동일 |
| `DOWN` | 현재 세대와 동일 | 현재 층 - 1 | 현재 세대와 동일 |

호수 문자열을 계산에 사용하지 않고 `building_id`, `floor_number`, `line_number`를 사용합니다. 계산된 세대가 없거나 그 세대에 현재 인증된 거주자가 없으면 두 경우를 구분하지 않고 HTTP 409 `NOISE_ALERT_TARGET_UNAVAILABLE`을 반환합니다.

## 알림 유형과 상태

소음 유형은 다음 여섯 가지 고정 값만 받습니다.

- `FOOTSTEPS`
- `FURNITURE`
- `MUSIC`
- `CONSTRUCTION`
- `PET`
- `OTHER`

자유 입력 문구는 받지 않습니다. 서버는 방향과 유형에 맞는 중립적인 `displayMessage`를 생성합니다.

알림 상태는 다음 순서로 관리합니다.

```text
SENT ──정형 응답──> RESPONDED
  └──────── 발신자 해결 ────────> RESOLVED
RESPONDED ──발신자 해결──> RESOLVED
```

발신자와 대상 세대는 각각 보낸 이력과 받은 이력을 조회할 수 있습니다. 해결은 발신자만 수행하며, 이미 해결된 알림에 대한 반복 요청은 기존 해결 시각을 유지하고 HTTP 204를 반환합니다.

## 정형 응답

대상 세대의 현재 인증 거주자는 다음 중 하나로 최초 1회만 응답할 수 있습니다.

- `ACKNOWLEDGED`
- `WILL_TAKE_ACTION`
- `NOT_OUR_HOME`
- `REQUEST_CHAT`

같은 세대에 여러 인증 거주자가 있어도 알림별 응답은 한 건만 저장됩니다. 서비스는 `NoiseAlert` 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 기존 응답을 확인하고, 응답 저장과 `RESPONDED` 상태 변경을 하나의 트랜잭션에서 처리합니다. `noise_alert_responses.noise_alert_id`의 unique 제약이 최종 방어선입니다.

`REQUEST_CHAT`은 제한형 익명 채팅을 요청하는 의사 표시입니다. 알림 발신자가 이를 수락해 채팅방 생성 API를 호출하면 해당 알림의 발신자와 최초 응답자만 참여할 수 있습니다. 각 채팅 요청은 사건 참여 권한을 다시 검증하며 WebSocket 연결은 제공하지 않습니다. 세부 정책은 [제한형 익명 채팅 문서](restricted-anonymous-chat.md)에서 설명합니다.

## API와 접근 권한

모든 API에는 Access Token 인증과 현재 사용자의 거주 인증이 필요합니다.

| API | 허용 범위 |
|---|---|
| `POST /api/v1/noise-alerts` | 인증된 현재 거주자 |
| `GET /api/v1/noise-alerts/received` | 현재 거주 세대가 대상인 알림 |
| `GET /api/v1/noise-alerts/sent` | 현재 거주 관계가 발신자인 알림 |
| `GET /api/v1/noise-alerts/{noiseAlertId}` | 발신자 또는 대상 세대의 현재 거주자 |
| `POST /api/v1/noise-alerts/{noiseAlertId}/responses` | 대상 세대의 현재 거주자 |
| `POST /api/v1/noise-alerts/{noiseAlertId}/resolve` | 발신자 |

사건과 무관한 사용자는 알림의 존재를 추론하지 못하도록 HTTP 404로 차단합니다. 사건 참여자이지만 역할이 맞지 않는 발신자의 응답과 수신자의 해결 요청은 HTTP 403으로 차단합니다.

`noiseAlertId`는 사건 API에 접근하기 위한 불투명 식별자로 반환하지만, ID를 알고 있다는 사실만으로 권한을 부여하지 않습니다. 상세 조회, 응답과 해결 요청마다 서버가 현재 `Residence`와 사건 참여 관계를 다시 확인합니다.

## 사용자 간 비식별성

서버 내부에는 권한 검증과 이력 관리를 위해 발신 `Residence`, 대상 `ApartmentUnit`, 실제 응답 `Residence`와 `ApartmentComplex`를 저장합니다. 이는 서버 운영자로부터도 신원을 숨기는 완전한 익명성이 아니라 사용자 간 비식별성입니다.

외부 알림 DTO에는 다른 사용자나 세대의 다음 정보를 반환하지 않습니다.

- 사용자 ID, 이메일과 닉네임
- 발신·응답 거주 관계 ID
- 대상 세대 ID와 단지 ID
- 실제 동·호수
- 같은 세대의 다른 거주자 정보

발신자에게 대상은 `위층 이웃` 또는 `아래층 이웃`으로, 수신자에게 발신자는 `알림을 보낸 이웃`으로 표시합니다. 정형 응답 이력에는 응답 유형만 노출하며 실제 응답자의 신원은 노출하지 않습니다.

## 현재 미구현 범위

- Android 앱의 서버 API 전환
- FCM 푸시 알림
- WebSocket·SSE 실시간 연결
- 자유 입력 메시지와 정화 필터
- 신고·차단과 Rate Limit
- 관리자 이력 조회
- AWS 배포
