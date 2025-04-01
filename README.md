# AdminActionLogger

마인크래프트 서버에서 관리자 권한을 가진 플레이어의 활동(특히 크리에이티브 모드 사용)을 감지하고 로그를 기록하는 Spigot/Paper 플러그인입니다. 서버 운영자가 관리자들의 활동을 투명하게 모니터링하는 데 도움을 줍니다.

## ✨ 주요 기능

* **크리에이티브 모드 활동 감지**
  * 마우스 휠 클릭으로 아이템 복사
  * 아이템 생성 및 배치
  * 아이템 드롭
  
* **관리자 명령어 사용 감시**
  * 게임모드 변경 감지
  * 민감한 명령어 사용 로깅
  * OP 또는 gamemode 권한이 있는 플레이어 추적
  
* **상세한 로그 기록**
  * 로그 파일: `plugins/AdminActionLogger/admin-actions.log`
  * 디스코드 및 게임 내 알림
  * 날짜, 시간, 플레이어, 아이템, 위치 등 상세 정보

* **인벤토리 변화 감지**
  * 관리자의 인벤토리 변화 감지
  * 서바이벌 모드에서 새 아이템 획득 로깅
  
* **접속/퇴장 모니터링**
  * 관리자 접속 IP 및 위치 기록
  * 퇴장 시 마지막 위치 기록

## 💾 설치 방법

1. **[Releases 페이지](https://github.com/sudapeople/AdminActionLogger/releases)** 에서 최신 버전의 `.jar` 파일을 다운로드합니다.
2. 다운로드한 `.jar` 파일을 사용 중인 Spigot/Paper 서버의 `plugins` 폴더에 넣습니다.
3. 서버를 재시작하거나, 플러그인 관리 플러그인(예: PlugManX)을 사용하여 로드합니다.

## ⚙️ 설정 (`config.yml`)

플러그인을 처음 실행하면 `plugins/AdminActionLogger/config.yml` 파일이 생성됩니다. 이 파일을 통해 다양한 설정을 변경할 수 있습니다.

```yaml
# 로깅 기능 활성화 여부
enable-logging: true

# 디버그 모드 - 문제 해결 시 활성화
debug-mode: false

# 모니터링할 플레이어 목록 (비워두면 OP와 gamemode 권한이 있는 모든 플레이어 감시)
monitored-players:
  - "ServerAdmin1" 
  - "ModeratorUser"

# 로그 발생 시 실행할 명령어 목록
commands-to-execute:
  - "broadcast &c[알림] &f%player%님이 크리에이티브 모드에서 &e%item% x%amount%&f을(를) %action%했습니다."
  - "discord broadcast `%player%`가 크리에이티브 모드에서 **%item% x%amount%**을(를) %action%했습니다."
```

## 🛠️ 명령어

플러그인은 다음 명령어를 제공합니다:

* `/clog toggle` - 로깅 기능 활성화/비활성화
* `/clog reload` - 설정 파일 리로드
* `/clog status` - 현재 상태 확인
* `/clog debug` - 디버그 모드 활성화/비활성화
* `/clog logs view` - 최근 로그 요약 보기
* `/clog logs clear` - 로그 파일 초기화
* `/clog monitor <add|remove> <플레이어>` - 모니터링 대상 추가/제거

## 📋 권한

* `creativeitemlogger.admin` - 모든 명령어 사용 권한

## 🔧 개발자 정보

이 플러그인은 서버 관리자들의 활동을 투명하게 관리하기 위해 개발되었습니다.
기존의 CreativeItemLogger를 확장하여 더 많은 관리자 행동을 감시할 수 있도록 개선되었습니다.