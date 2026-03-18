# 이미지 적용 가이드 (Random Tower Defense)

이 문서는 현재 프로젝트의 실제 로딩 규칙 기준으로, 준비한 이미지를 어디에 어떤 이름으로 넣어야 하는지 정리한 문서입니다.

## 1) 공통 규칙
- 파일 형식은 `PNG` 권장
- 배경이 필요한 경우를 제외하면 `투명 배경` 권장
- 경로와 파일명은 코드에서 직접 로드하므로 아래 이름을 그대로 맞춰야 함
- 게임 실행 중 이미지를 교체했다면 재실행으로 반영 확인

## 2) 폴더 구조
- `assets/ui`
- `assets/ui/path_tile`
- `assets/ui/jobs`
- `assets/towers`
- `assets/monsters/motion`
- `assets/monsters/org`

## 3) UI 이미지
### 배경 / 설치 타일
- `assets/ui/background.png`
  - 전체 배경
  - 권장 크기: `540x760` 이상
- `assets/ui/build_tile.png`
  - 타워 설치 가능 타일
  - 권장 크기: `45x45` 이상

### 길 타일
- `assets/ui/path_tile/path_tile.png`
  - 직선 길 타일
- `assets/ui/path_tile/cross_path_tile.png`
  - 교차로 타일
- 권장 크기: `45x45` 이상 정사각 PNG

## 4) JOB 선택 이미지 (신규)
게임 시작 전 직업 선택 창에서 각 직업 카드를 표시할 때 사용합니다.

### 파일 경로/이름
- `assets/ui/jobs/noble.png` (귀족)
- `assets/ui/jobs/knight_commander.png` (기사단장)
- `assets/ui/jobs/magitech_engineer.png` (마도공학자)

### 권장 규격
- 카드 내부 이미지 권장 비율: `4:5` 근사
- 최소 권장 크기: `272x340` 이상
- 더 큰 해상도 사용 가능 (`544x680` 등)

### UI 표시 방식
- 직업 선택창은 게임 시작 전에 오버레이로 표시됨
- 각 카드 구성:
  - 상단: 직업 이미지
  - 이미지 아래: 직업명
  - 직업명 아래: 짧은 능력 요약 텍스트
- 선택 방식: `버튼 없이 이미지 클릭`으로 직업 선택
- 호버 효과:
  - 마우스 포인터를 올린 직업 이미지는 컬러 강조
  - 나머지 직업 이미지는 흑백 + 반투명으로 표시

### 직업 능력 요약 문구 (기본)
- 귀족: `웨이브 클리어 골드 +35%`
- 기사단장: `타워 강화비용 -20%`
- 마도공학자: `타워 공격력 +20%`

## 5) 타워 이미지
- `assets/towers/fire.png`
- `assets/towers/water.png`
- `assets/towers/nature.png`
- `assets/towers/arcane.png`
- `assets/towers/shadow.png`
- `assets/towers/chaos.png`
- 권장 크기: `27x27` 이상 정사각 PNG

## 6) 몬스터 이미지
몬스터는 이동 방향에 따라 `front / side / back` 3분할 스프라이트 시트를 사용합니다.

### 기본 파일명
일반 몬스터:
- `assets/monsters/motion/fire-front-side-back.png`
- `assets/monsters/motion/water-front-side-back.png`
- `assets/monsters/motion/nature-front-side-back.png`

보스:
- `assets/monsters/motion/boss-front-side-back.png`

### 로드 우선순위
1. `assets/monsters/motion/*-front-side-back.png`
2. `*_front.png`, `*_side.png`, `*_back.png`
3. `assets/monsters/org/*.png`
4. `assets/monsters/*.png`

## 7) 적용 확인 체크리스트
- 게임 시작 시 직업 선택 오버레이가 먼저 표시되는지
- 각 직업 카드에서 이미지가 정상 표시되는지
- 이미지 아래 능력 요약 문구가 보이는지
- 직업 선택 후 게임이 진행되는지
- 선택 직업 효과가 실제 수치에 반영되는지
  - 귀족: 웨이브 종료 골드 증가
  - 기사단장: 강화 버튼 비용 감소
  - 마도공학자: 타워 공격력 증가

## 8) 관련 코드 위치
- `src/RandomTowerDefense.java`
  - `loadAssets()`
  - `paintJobSelectionOverlay(...)`
  - `paintJobCard(...)`
  - `getUpgCost(...)`
  - `getHiddenUpgCost(...)`
  - `actionPerformed(...)`

## 9) 빌드/실행
```powershell
javac -encoding UTF-8 src\RandomTowerDefense.java
java -cp src RandomTowerDefense
```
