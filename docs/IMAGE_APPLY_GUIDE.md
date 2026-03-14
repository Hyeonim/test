# 이미지 적용 가이드 (Random Tower Defense)

이 문서는 현재 프로젝트의 실제 로딩 규칙 기준으로, 사용자가 직접 준비한 이미지를 어디에 어떤 이름으로 넣어야 하는지 정리한 문서입니다.

## 1) 적용 원칙
- 파일 형식은 `PNG` 권장
- 배경을 제외한 대부분 이미지는 투명 배경 사용 권장
- 경로와 파일명은 코드에서 직접 읽으므로, 아래 이름을 그대로 맞추는 것이 가장 빠릅니다
- 게임 실행 중 이미지를 교체했다면 재실행 후 확인합니다

## 2) 폴더 구조
프로젝트 루트 기준:

- `assets/ui`
- `assets/ui/path_tile`
- `assets/towers`
- `assets/monsters`

## 3) UI 이미지
### 배경 / 설치 타일
- `assets/ui/background.png`
  - 전체 배경
  - 권장 크기: `540x760` 이상

- `assets/ui/build_tile.png`
  - 설치 가능 타일
  - 권장 크기: `45x45` 이상

### 길 타일
아래 3장은 모두 코드에서 자동 회전하여 사용합니다.

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
- 즉, 자연스러운 커브 표현은 `cross_path_tile.png` 품질에 직접 영향을 받습니다
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
몬스터는 이제 이동 방향에 따라 `앞 / 뒤 / 옆` 이미지를 따로 읽을 수 있습니다.

### 권장 파일명
일반 몬스터:

- `assets/monsters/fire_front.png`
- `assets/monsters/fire_back.png`
- `assets/monsters/fire_side.png`
- `assets/monsters/water_front.png`
- `assets/monsters/water_back.png`
- `assets/monsters/water_side.png`
- `assets/monsters/nature_front.png`
- `assets/monsters/nature_back.png`
- `assets/monsters/nature_side.png`

보스:

- `assets/monsters/boss_front.png`
- `assets/monsters/boss_back.png`
- `assets/monsters/boss_side.png`

### 방향 기준
- `front`: 아래쪽으로 이동할 때 보이는 모습
- `back`: 위쪽으로 이동할 때 보이는 모습
- `side`: 좌/우 이동 시 보이는 옆모습

### 옆모습 준비 규칙
- `side` 이미지는 한 장만 준비하면 됩니다
- 코드에서 좌측 이동 시 자동 좌우 반전해서 사용합니다
- 따라서 원본은 `오른쪽을 바라보는 옆모습` 기준으로 준비하는 것을 권장합니다

### 권장 크기
- 일반 몬스터: `36x36` 이상 정사각 PNG
- 보스: `48x48` 이상 정사각 PNG

## 6) 하위 호환 규칙
기존 단일 이미지도 계속 동작합니다.

- `assets/monsters/fire.png`
- `assets/monsters/water.png`
- `assets/monsters/nature.png`
- `assets/monsters/boss.png`

동작 방식:
- `front/back/side` 중 일부가 없으면 기존 단일 이미지로 대체됩니다
- 즉, 이미지를 한 번에 다 준비하지 않아도 단계적으로 교체할 수 있습니다

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
  - `createCurvePathTexture(...)`
  - `drawPathTile(...)`
  - `loadMonsterSpriteSet(...)`
  - `updateMonsterFacing(...)`

## 9) 자주 발생하는 문제
- 파일명 오타
- `assets/ui/path_tile` 폴더가 아닌 다른 위치에 저장함
- 배경이 불투명하거나 가장자리에 여백이 너무 많음
- `side` 이미지를 좌우 각각 따로 만들었는데 코드가 한 장만 반전 사용하도록 되어 있음

## 10) 컴파일 / 실행 예시
컴파일:

```powershell
javac -encoding UTF-8 src\RandomTowerDefense.java
```

실행:

```powershell
java -cp src RandomTowerDefense
```
