# O-IoT — 기술 요약 (Quick reference)

> **전체 제출용 통합 문서는 [README.md](README.md) 또는 [final_project_temp.md](final_project_temp.md)를 본다.**

- **Repository:** [https://github.com/csys348/26-JavaProgramming-TeamProject](https://github.com/csys348/26-JavaProgramming-TeamProject)

## 실행 순서

1. 컴파일 후 백엔드: `java -cp out oiot.backend.BackendCliServer`
2. Flutter: `cd oiot_flutter_app && flutter run`
3. Flutter는 `http://localhost:8080/api` 사용 — **백엔드 필수**

## CLI 요약

`help`, `status`, `tick [n]`, `on|off <id>`, `set <id> <value>`, `scenario <HOME|AWAY|NIGHT>`, `add <TYPE> <name>`, `log`, `quit`

## API 요약

`GET /api/health`, `GET /api/devices`, `POST /api/device/power`, `POST /api/device/option`, `POST /api/device/add`, `POST /api/scenario`, `POST /api/simulate`, `POST /api/log/save`

## 주요 경로

| 경로 | 내용 |
|------|------|
| `src/oiot/backend/BackendCliServer.java` | CLI + HTTP |
| `oiot_flutter_app/lib/main.dart` | Flutter 클라이언트 |
| `logs/` | 스냅샷 로그 |
| `readme_img/` | 스크린샷 자산 |
