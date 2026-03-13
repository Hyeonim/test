# 이미지 적용 가이드 (Random Tower Defense)

이 문서는 사용자가 가져온 이미지를 프로젝트에 맞게 넣는 방법을 설명합니다.

## 1) 기본 원칙
- 파일 형식: PNG 권장 (투명 배경 지원)
- 경로/파일명은 코드에서 고정으로 읽으므로, 이름을 맞추는 것이 가장 빠릅니다.
- 이미지를 교체하면 코드 수정 없이 즉시 반영됩니다.

## 2) 폴더 구조
프로젝트 루트 기준:

- `assets/ui`
- `assets/towers`
- `assets/monsters`

## 3) 파일 매핑
### UI
- `assets/ui/background.png`
  - 전체 배경 텍스처
  - 권장 크기: `540x760` 이상

- `assets/ui/build_tile.png`
  - 설치 가능 타일 텍스처
  - 권장 크기: `45x45`

- `assets/ui/path_tile.png`
  - 길 타일 텍스처
  - 권장 크기: `45x45`

### 타워
- `assets/towers/fire.png`
- `assets/towers/water.png`
- `assets/towers/nature.png`
- `assets/towers/arcane.png`
- `assets/towers/shadow.png`
- `assets/towers/chaos.png`

권장 크기: `27x27` (또는 더 큰 정사각 PNG)

### 몬스터
- `assets/monsters/fire.png`
- `assets/monsters/water.png`
- `assets/monsters/nature.png`
- `assets/monsters/boss.png`

권장 크기:
- 일반 몬스터: `36x36`
- 보스: `40x40` 이상

## 4) 실제 적용 절차
1. 사용자가 준비한 PNG를 위 파일명에 맞춰 저장합니다.
2. 게임 실행 중이면 재시작합니다.
3. 반영 확인:
   - 타워/몬스터가 커스텀 이미지로 보이면 성공
   - 이미지가 없거나 로드 실패 시 자동 폴백(코드 드로잉)으로 표시됨

## 5) 코드에서 로드하는 위치
아래 파일에서 이미지 경로를 읽습니다:
- `src/RandomTowerDefense.java`
- 메서드: `loadAssets()`

필요하면 이 메서드에서 파일명을 변경할 수 있습니다.

## 6) 자주 발생하는 문제
- 파일명 오타: 대소문자/철자 확인
- 경로 오류: 반드시 프로젝트 루트 기준 `assets/...`
- PNG 아님: jpg/webp는 현재 미권장
- 배경이 검게 보임: 알파 채널 없는 파일일 수 있음

## 7) 한글 텍스트가 깨질 때
현재 코드는 UI 문자열을 유니코드 이스케이프로 넣어 인코딩 영향을 최소화했습니다.
그래도 깨진다면 실행/컴파일 인코딩을 UTF-8로 맞춰주세요.

예시 컴파일:
- `javac -encoding UTF-8 src\RandomTowerDefense.java`

예시 실행:
- `java -cp src RandomTowerDefense`
