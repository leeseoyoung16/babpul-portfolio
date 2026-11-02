# 3star
캡스톤(1) 3성전자팀 프로젝트입니다.

## Firebase(FCM) 키 설정

Firebase 서비스 계정 키(JSON)는 저장소에 포함하지 않습니다. `user-service` 실행 시
`FIREBASE_CREDENTIALS_PATH` 환경변수로 키 파일의 실제 경로를 주입하세요.

```env
FIREBASE_CREDENTIALS_PATH=/path/to/firebaseKey.json
```

이 값을 비워두면 애플리케이션은 정상 기동하되 FCM 푸시 알림 기능만 비활성화됩니다.
