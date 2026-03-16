# 이미지 적용 가이드 (Random Tower Defense)

이 문서는 현재 프로젝트의 실제 로딩 규칙 기준으로, 사용자가 직접 준비한 이미지를 어디에 어떤 이름으로 넣어야 하는지 정리한 문서입니다.

## 1) 적용 원칙
- 파일 형식은 `PNG` 권장
- 배경을 제외한 대부분 이미지는 투명 배경 사용 권장
- 경로와 파일명은 코드에서 직접 읽으므로 아래 이름을 그대로 맞추는 것이 가장 빠릅니다
- 게임 실행 중 이미지를 교체했다면 재실행 후 확인합니다

## 2) 폴더 구조
프로젝트 루트 기준:

- `assets/ui`
- `assets/ui/path_tile`
- `assets/towers`
- `assets/monsters`
- `assets/monsters/motion`
- `assets/monsters/org`

## 3) UI 이미지
### 배경 / 설치 타일
- `assets/ui/background.png`
  - 전체 배경
  - 권장 크기: `540x760` 이상

- `assets/ui/build_tile.png`
  - 설치 가능 타일
  - 권장 크기: `45x45` 이상

### 길 타일
- `assets/ui/path_tile/path_tile.png`
  - 직선 길 타일
  - 기본 기준 방향: `상-하`로 이어지는 형태

- `assets/ui/path_tile/cross_path_tile.png`
  - 교차로 타일
  - 4방향이 모두 열려 있는 형태

권장 크기:
- `45x45` 이상 정사각 PNG

주의:
- 현재 커브 길은 `cross_path_tile.png`를 기반으로 코드에서 자동 생성합니다
- 즉 자연스러운 커브 표현은 `cross_path_tile.png` 품질에 직접 영향을 받습니다
- 커브 경계는 코드에서 블러 처리된 마스크를 사용해 둥글게 정리합니다

## 4) 타워 이미지
파일명:

- `assets/towers/fire.png`
- `assets/towers/water.png`
- `assets/towers/nature.png`
- `assets/towers/arcane.png`
- `assets/towers/shadow.png`
- `assets/towers/chaos.png`

권장 크기:
- `27x27` 이상 정사각 PNG

참고:
- 실제 게임에서는 타일 안에 맞춰 축소/확대되어 표시됩니다

## 5) 몬스터 이미지
몬스터는 이동 방향에 따라 `front / side / back` 3개 모습을 사용합니다.

### 표준 파일명
앞으로는 몬스터별로 아래 형식의 스프라이트 시트를 넣는 것을 기준으로 합니다.

일반 몬스터:
- `assets/monsters/motion/fire-front-side-back.png`
- `assets/monsters/motion/water-front-side-back.png`
- `assets/monsters/motion/nature-front-side-back.png`

보스:
- `assets/monsters/motion/boss-front-side-back.png`

원본 보관용:
- `assets/monsters/org/fire.png`
- `assets/monsters/org/water.png`
- `assets/monsters/org/nature.png`
- `assets/monsters/org/boss.png`

### 시트 배치 규칙
이미지 한 장 안에 좌에서 우 순서로 3등분되어 있어야 합니다.

- 왼쪽 1/3: `front`
- 가운데 1/3: `side`
- 오른쪽 1/3: `back`

### 방향 기준
- `front`: 아래쪽으로 이동할 때 보이는 모습
- `back`: 위쪽으로 이동할 때 보이는 모습
- `side`: 좌/우 이동 시 보이는 옆모습

### 옆모습 준비 규칙
- `side`는 한 장만 준비하면 됩니다
- 코드에서 좌측 이동 시 자동 좌우 반전해서 사용합니다
- 따라서 원본은 `오른쪽을 바라보는 옆모습` 기준으로 준비하는 것을 권장합니다

### 시트 제작 규칙
- 3개 영역은 되도록 같은 폭으로 맞춥니다
- 권장 비율은 `3:2` 또는 `3:1`이 아니라, "가로 3칸짜리 동일 폭 레이아웃"입니다
- 예: `1536x1024`이면 각 칸은 대략 `512x1024`
- 각 칸 안의 캐릭터는 너무 바깥쪽에 붙이지 말고 어느 정도 중앙에 배치하는 것이 안전합니다

### 권장 크기
- 일반 몬스터: `900x300` 이상 또는 `1200x400` 이상
- 보스: `1200x400` 이상

## 6) 하위 호환 규칙
현재 기준 폴더 역할은 아래와 같습니다.

- `motion`: 실제 게임에서 읽는 방향별 스프라이트 시트
- `org`: 원본 단일 이미지 보관 및 최종 폴백

기존 형식도 당장은 계속 동작합니다.

우선순위:
1. `assets/monsters/motion/*-front-side-back.png`
2. `*_front.png`, `*_side.png`, `*_back.png`
3. `assets/monsters/org/*.png`
4. 기존 루트 단일 이미지 `assets/monsters/*.png`

예:
- `assets/monsters/motion/fire-front-side-back.png`가 있으면 자동 컷팅 사용
- 없으면 `fire_front.png`, `fire_side.png`, `fire_back.png`를 찾음
- 그것도 없으면 `assets/monsters/org/fire.png`
- 마지막으로 루트의 `assets/monsters/fire.png` 한 장으로 대체

## 7) 실제 적용 절차
1. 준비한 PNG를 위 파일명에 맞춰 저장합니다.
2. 게임을 다시 실행합니다.
3. 아래 내용을 확인합니다.

검수 포인트:
- 몬스터가 아래로 갈 때 `front`, 위로 갈 때 `back`, 좌우 이동 시 `side`로 보이는지
- 좌측 이동 시 `side` 이미지가 반전되어 자연스럽게 보이는지
- 길 코너에서 교차로 타일 기반 커브가 자연스럽게 이어지는지
- 4방향 교차 지점에서만 `cross_path_tile.png`가 사용되는지

## 8) 코드에서 로드하는 위치
- `src/RandomTowerDefense.java`
- 주요 메서드:
  - `loadAssets()`
  - `loadMonsterSpriteSet(...)`
  - `splitMonsterSpriteSheet(...)`
  - `createCurvePathTexture(...)`
  - `drawPathTile(...)`
  - `updateMonsterFacing(...)`

## 9) 자주 발생하는 문제
- 파일명 오타
- `front-side-back` 순서가 아닌 다른 순서로 시트를 저장함
- 3등분 폭이 크게 다름
- `motion`이 아닌 `org`에 시트 파일을 넣어둠
- `side` 이미지를 이미 좌우 각각 만들어놨는데 코드가 한 장만 반전 사용하도록 되어 있음
- `assets/ui/path_tile` 폴더가 아닌 다른 위치에 저장함

## 10) 컴파일 / 실행 예시
컴파일:

```powershell
javac -encoding UTF-8 src\RandomTowerDefense.java
```

실행:

```powershell
java -cp src RandomTowerDefense
```
