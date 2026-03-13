import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomTowerDefense extends JPanel implements ActionListener, MouseListener {
    private static final int GRID_SIZE = 12;
    private static final int TILE_SIZE = 45;

    private static final int GRID_ORIGIN_X = 0;
    private static final int GRID_ORIGIN_Y = 70;
    private static final int TOP_PANEL_H = 58;
    private static final int BOTTOM_PANEL_Y = 620;
    private static final int PANEL_W = GRID_SIZE * TILE_SIZE;
    private static final int PANEL_H = 760;

    private static final int TOWER_COST = 10;
    private static final int UPGRADE_COST = 20;
    private static final int HIDDEN_UPGRADE_COST = 50;
    private static final int MAX_FIELD_MONSTERS = 100;
    private static final int TOWER_SPRITE_SIZE = 33;
    private static final double MONSTER_SCALE_NORMAL = 1.6;
    private static final double MONSTER_SCALE_BOSS = 1.4;

    private static final String[] ELEMENTS = {"Fire", "Water", "Nature"};
    private static final String[] ELEMENT_LABELS = {
            "\uD654\uC5FC", // 화염
            "\uBB3C",     // 물
            "\uC790\uC5F0"  // 자연
    };
    private static final Color[] ELEMENT_COLORS = {
            new Color(255, 114, 90),
            new Color(91, 160, 255),
            new Color(98, 216, 121)
    };

    private static final String[] HIDDEN_ELEMENTS = {"Arcane", "Shadow", "Chaos"};
    private static final Color[] HIDDEN_COLORS = {
            new Color(255, 232, 130),
            new Color(115, 95, 210),
            new Color(255, 94, 193)
    };

    private final int[][] mapGrid = {
            {0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1},
            {1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0}
    };

    private final boolean[][] tileBuildable = new boolean[GRID_SIZE][GRID_SIZE];
    private final boolean[][] tileHasTower = new boolean[GRID_SIZE][GRID_SIZE];
    private final boolean[][] tileSelected = new boolean[GRID_SIZE][GRID_SIZE];
    private final String[][] tileTowerType = new String[GRID_SIZE][GRID_SIZE];
    private final Color[][] tileColor = new Color[GRID_SIZE][GRID_SIZE];
    private final int[][] tileTier = new int[GRID_SIZE][GRID_SIZE];
    private final int[][] tileTypeIndex = new int[GRID_SIZE][GRID_SIZE];
    private final int[][] tileCooldown = new int[GRID_SIZE][GRID_SIZE];

    private final List<Object[]> monsters = new ArrayList<>();
    private final List<Object[]> lasers = new ArrayList<>();
    private final Random random = new Random();

    private final int[] upgradeLevels = {0, 0, 0};
    private int hiddenUpgradeLevel = 0;

    private int life = 20;
    // ★ 극악 난이도: 초기 골드 60원 유지!
    private int gold = 60;
    private int wave = 1;
    private int waveTimer = 600;
    private int spawnCooldown = 20;
    private int spawnedThisWave = 0;
    private int currentSpeed = 1;
    private int tick = 0;

    private int selectedTileX = -1;
    private int selectedTileY = -1;
    private Object[] selectedMonster = null;

    private final int[][] gridWaypoints = {
            {0, 0}, {0, 4}, {11, 4}, {11, 0}, {6, 0}, {6, 11},
            {11, 11}, {11, 7}, {0, 7}, {0, 11}, {3, 11}, {3, 0}
    };
    private final int[][] waypoints = new int[12][2];

    private final Rectangle speedButton = new Rectangle(448, 12, 78, 30);
    private final Rectangle sellButton = new Rectangle(172, 656, 70, 34);
    // ★ 다시 시작 버튼 구역 추가
    private final Rectangle restartButton = new Rectangle(PANEL_W / 2 - 80, PANEL_H / 2 + 60, 160, 45);
    private final Rectangle[] upgradeButtons = {
            new Rectangle(264, 664, 60, 40),
            new Rectangle(330, 664, 60, 40),
            new Rectangle(396, 664, 60, 40),
            new Rectangle(462, 664, 66, 40)
    };

    private BufferedImage bgTexture;
    private BufferedImage buildTileTexture;
    private BufferedImage pathStraightTexture;
    private BufferedImage pathCurveTexture;
    private BufferedImage pathCrossTexture;
    private final BufferedImage[] towerSprites = new BufferedImage[6];
    private final BufferedImage[] monsterSprites = new BufferedImage[4];

    private Timer gameLoop;

    private static final int M_X = 0;
    private static final int M_Y = 1;
    private static final int M_TARGET = 2;
    private static final int M_MAX_HP = 3;
    private static final int M_HP = 4;
    private static final int M_ELEMENT = 5;
    private static final int M_COLOR = 6;
    private static final int M_BOSS = 7;
    private static final int M_RADIUS = 8;

    private static final int L_START_X = 0;
    private static final int L_START_Y = 1;
    private static final int L_END_X = 2;
    private static final int L_END_Y = 3;
    private static final int L_FRAMES = 4;
    private static final int L_COLOR = 5;
    private static final int L_HEAVY = 6;

    public RandomTowerDefense() {
        setPreferredSize(new Dimension(PANEL_W, PANEL_H));
        setBackground(new Color(13, 18, 28));
        setDoubleBuffered(true);

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                tileBuildable[y][x] = mapGrid[y][x] == 1;
                tileTowerType[y][x] = "";
            }
        }

        for (int i = 0; i < waypoints.length; i++) {
            waypoints[i][0] = gridWaypoints[i][0] * TILE_SIZE + (TILE_SIZE / 2) + GRID_ORIGIN_X;
            waypoints[i][1] = gridWaypoints[i][1] * TILE_SIZE + (TILE_SIZE / 2) + GRID_ORIGIN_Y;
        }

        loadAssets();
        addMouseListener(this);

        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    // ★ 게임 완전 초기화 메서드
    private void resetGame() {
        life = 20;
        gold = 60;
        wave = 1;
        waveTimer = 600;
        spawnCooldown = 20;
        spawnedThisWave = 0;
        currentSpeed = 1;
        tick = 0;
        gameLoop.setDelay(50);

        for (int i=0; i<3; i++) upgradeLevels[i] = 0;
        hiddenUpgradeLevel = 0;

        monsters.clear();
        lasers.clear();
        clearSelectedTile();
        selectedMonster = null;

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                clearTile(x, y);
            }
        }
    }

    private void loadAssets() {
        bgTexture = loadImage("assets/ui/background.png");
        buildTileTexture = loadImage("assets/ui/build_tile.png");
        pathStraightTexture = loadImage("assets/ui/path_tile/path_tile.png");
        pathCurveTexture = loadImage("assets/ui/path_tile/curve_path_tile.png");
        pathCrossTexture = loadImage("assets/ui/path_tile/cross_path_tile.png");

        if (pathStraightTexture == null) pathStraightTexture = loadImage("assets/ui/path_tile.png");

        towerSprites[0] = loadImage("assets/towers/fire.png");
        towerSprites[1] = loadImage("assets/towers/water.png");
        towerSprites[2] = loadImage("assets/towers/nature.png");
        towerSprites[3] = loadImage("assets/towers/arcane.png");
        towerSprites[4] = loadImage("assets/towers/shadow.png");
        towerSprites[5] = loadImage("assets/towers/chaos.png");

        monsterSprites[0] = loadImage("assets/monsters/fire.png");
        monsterSprites[1] = loadImage("assets/monsters/water.png");
        monsterSprites[2] = loadImage("assets/monsters/nature.png");
        monsterSprites[3] = loadImage("assets/monsters/boss.png");

        if (buildTileTexture == null) buildTileTexture = makeTileTexture(new Color(78, 112, 136), new Color(96, 132, 160));
        if (pathStraightTexture == null) pathStraightTexture = makeTileTexture(new Color(38, 49, 64), new Color(31, 40, 54));
        if (pathCurveTexture == null) pathCurveTexture = pathStraightTexture;
        if (pathCrossTexture == null) pathCrossTexture = pathStraightTexture;

        buildTileTexture = trimTransparentBounds(buildTileTexture);
        pathStraightTexture = trimTransparentBounds(pathStraightTexture);
        pathCurveTexture = trimTransparentBounds(pathCurveTexture);
        pathCrossTexture = trimTransparentBounds(pathCrossTexture);

        buildTileTexture = fitToTileSize(buildTileTexture);
        pathStraightTexture = fitToTileSize(pathStraightTexture);
        pathCurveTexture = fitToTileSize(pathCurveTexture);
        pathCrossTexture = fitToTileSize(pathCrossTexture);
    }

    private BufferedImage loadImage(String relativePath) {
        File file = resolveAssetPath(relativePath);
        if (file == null) return null;
        try {
            return ImageIO.read(file);
        } catch (IOException ignored) {
            return null;
        }
    }

    private File resolveAssetPath(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) return direct;

        File cwd = new File(System.getProperty("user.dir"));
        File current = cwd;
        for (int i = 0; i < 6 && current != null; i++) {
            File candidate = new File(current, relativePath);
            if (candidate.exists()) return candidate;
            current = current.getParentFile();
        }
        return null;
    }

    private BufferedImage fitToTileSize(BufferedImage src) {
        if (src == null) return null;
        if (src.getWidth() == TILE_SIZE && src.getHeight() == TILE_SIZE) return src;

        BufferedImage scaled = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = Math.max(TILE_SIZE / (double) src.getWidth(), TILE_SIZE / (double) src.getHeight());
        int drawW = (int) Math.ceil(src.getWidth() * scale);
        int drawH = (int) Math.ceil(src.getHeight() * scale);
        int drawX = (TILE_SIZE - drawW) / 2;
        int drawY = (TILE_SIZE - drawH) / 2;
        g.drawImage(src, drawX, drawY, drawW, drawH, null);
        g.dispose();
        return scaled;
    }

    private BufferedImage trimTransparentBounds(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (src.getRGB(x, y) >>> 24) & 0xFF;
                if (a > 8) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) return src;
        if (minX == 0 && minY == 0 && maxX == w - 1 && maxY == h - 1) return src;
        return src.getSubimage(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
    }

    private BufferedImage makeTileTexture(Color c1, Color c2) {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, c1, TILE_SIZE, TILE_SIZE, c2));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(255, 255, 255, 16));
        for (int i = 0; i < 4; i++) {
            g.drawLine(0, i * 11 + 4, TILE_SIZE, i * 11 + 4);
        }
        g.dispose();
        return img;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();

        // ★ 게임 오버 상태일 때: 다시 시작 버튼만 누를 수 있게 차단
        if (life <= 0) {
            if (restartButton.contains(mouseX, mouseY)) {
                resetGame();
                repaint();
            }
            return;
        }

        if (speedButton.contains(mouseX, mouseY)) {
            currentSpeed++;
            if (currentSpeed > 3) currentSpeed = 1;
            if (currentSpeed == 1) gameLoop.setDelay(50);
            else if (currentSpeed == 2) gameLoop.setDelay(25);
            else gameLoop.setDelay(16);
            repaint();
            return;
        }

        if (mouseY >= BOTTOM_PANEL_Y) {
            if (hasSelectedTile() && tileHasTower[selectedTileY][selectedTileX] && sellButton.contains(mouseX, mouseY)) {
                gold += tileTier[selectedTileY][selectedTileX] * 5;
                clearTile(selectedTileX, selectedTileY);
                clearSelectedTile();
                repaint();
                return;
            }

            for (int i = 0; i < upgradeButtons.length; i++) {
                if (upgradeButtons[i].contains(mouseX, mouseY)) {
                    if (i == 3) doHiddenUpgrade();
                    else doUpgrade(i);
                    repaint();
                    return;
                }
            }
            return;
        }

        boolean clickedMonster = false;
        for (int i = monsters.size() - 1; i >= 0; i--) {
            Object[] m = monsters.get(i);
            double mx = (double) m[M_X];
            double my = (double) m[M_Y];
            double radius = (double) m[M_RADIUS];
            if (Math.hypot(mx - mouseX, my - mouseY) <= radius + 6) {
                selectedMonster = m;
                clearSelectedTile();
                clickedMonster = true;
                break;
            }
        }

        if (!clickedMonster && mouseY >= GRID_ORIGIN_Y && mouseY < GRID_ORIGIN_Y + GRID_SIZE * TILE_SIZE) {
            int gridX = (mouseX - GRID_ORIGIN_X) / TILE_SIZE;
            int gridY = (mouseY - GRID_ORIGIN_Y) / TILE_SIZE;
            selectedMonster = null;
            if (gridX >= 0 && gridX < GRID_SIZE && gridY >= 0 && gridY < GRID_SIZE) {
                handleTileClick(gridX, gridY);
            } else {
                clearSelectedTile();
            }
        }
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }

    private boolean hasSelectedTile() {
        return selectedTileX >= 0 && selectedTileY >= 0;
    }

    private void clearSelectedTile() {
        if (hasSelectedTile()) tileSelected[selectedTileY][selectedTileX] = false;
        selectedTileX = -1;
        selectedTileY = -1;
    }

    private void selectTile(int x, int y) {
        clearSelectedTile();
        selectedTileX = x;
        selectedTileY = y;
        tileSelected[y][x] = true;
    }

    private void setTower(int x, int y, String towerType, Color color, int tier, int typeIndex) {
        tileHasTower[y][x] = true;
        tileTowerType[y][x] = towerType;
        tileColor[y][x] = color;
        tileTier[y][x] = tier;
        tileTypeIndex[y][x] = typeIndex;
        tileCooldown[y][x] = 0;
    }

    private void clearTile(int x, int y) {
        tileHasTower[y][x] = false;
        tileTowerType[y][x] = "";
        tileColor[y][x] = null;
        tileTier[y][x] = 0;
        tileTypeIndex[y][x] = 0;
        tileCooldown[y][x] = 0;
        tileSelected[y][x] = false;
    }

    private void doUpgrade(int typeIndex) {
        if (gold >= UPGRADE_COST) {
            gold -= UPGRADE_COST;
            upgradeLevels[typeIndex]++;
        }
    }

    private void doHiddenUpgrade() {
        if (gold >= HIDDEN_UPGRADE_COST) {
            gold -= HIDDEN_UPGRADE_COST;
            hiddenUpgradeLevel++;
        }
    }

    private void handleTileClick(int x, int y) {
        if (!tileBuildable[y][x]) {
            clearSelectedTile();
            return;
        }

        if (!tileHasTower[y][x]) {
            if (hasSelectedTile()) {
                clearSelectedTile();
            } else if (gold >= TOWER_COST) {
                gold -= TOWER_COST;
                int typeIndex = random.nextInt(ELEMENTS.length);
                setTower(x, y, ELEMENTS[typeIndex], ELEMENT_COLORS[typeIndex], 1, typeIndex);
            }
            return;
        }

        if (!hasSelectedTile()) {
            selectTile(x, y);
            return;
        }

        if (selectedTileX == x && selectedTileY == y) {
            clearSelectedTile();
            return;
        }

        int sx = selectedTileX;
        int sy = selectedTileY;
        if (tileTier[sy][sx] == tileTier[y][x]) {
            if (tileTier[sy][sx] < 3 && tileTowerType[sy][sx].equals(tileTowerType[y][x])) {
                int newTier = tileTier[y][x] + 1;
                int rIndex = random.nextInt(ELEMENTS.length);
                setTower(x, y, ELEMENTS[rIndex], ELEMENT_COLORS[rIndex], newTier, rIndex);
                clearTile(sx, sy);
            } else if (tileTier[sy][sx] == 3) {
                // ★ 20% 확률로 히든 강림, 실패시 파괴 (가챠 유지)
                if (random.nextInt(100) < 20) {
                    int rIndex = random.nextInt(HIDDEN_ELEMENTS.length);
                    setTower(x, y, HIDDEN_ELEMENTS[rIndex], HIDDEN_COLORS[rIndex], 4, -1);
                } else {
                    clearTile(x, y); // 실패시 타겟 파괴
                }
                clearTile(sx, sy); // 재료 파괴
            }
        }

        if(tileHasTower[y][x]) selectTile(x, y);
        else clearSelectedTile();
    }

    private Object[] createMonster(String element, Color color, int hp, boolean isBoss) {
        Object[] monster = new Object[9];
        monster[M_ELEMENT] = element;
        monster[M_COLOR] = color;
        monster[M_MAX_HP] = hp;
        monster[M_HP] = hp;
        monster[M_BOSS] = isBoss;
        monster[M_RADIUS] = isBoss ? 18.0 : 10.0;
        monster[M_TARGET] = 1;
        monster[M_X] = (double) waypoints[0][0];
        monster[M_Y] = (double) waypoints[0][1];
        return monster;
    }

    private void spawnMonster() {
        int idx = (wave - 1) % ELEMENTS.length; // 단일 속성 고정 로직 유지
        int maxHp = 200 + ((wave - 1) * 150);
        monsters.add(createMonster(ELEMENTS[idx], ELEMENT_COLORS[idx], maxHp, false));
    }

    private void spawnBoss() {
        int maxHp = (200 + ((wave - 1) * 150)) * 20;
        monsters.add(createMonster("Boss", new Color(225, 90, 255), maxHp, true));
    }

    private boolean moveMonster(Object[] m) {
        int targetWaypoint = (int) m[M_TARGET];
        if (targetWaypoint >= waypoints.length) return false;

        int targetX = waypoints[targetWaypoint][0];
        int targetY = waypoints[targetWaypoint][1];
        boolean isBoss = (boolean) m[M_BOSS];
        double speed = isBoss ? 2.5 : 4.0;

        double centerX = (double) m[M_X];
        double centerY = (double) m[M_Y];

        double dx = targetX - centerX;
        double dy = targetY - centerY;
        double distance = Math.hypot(dx, dy);

        if (distance <= speed) {
            m[M_TARGET] = targetWaypoint + 1;
        } else {
            m[M_X] = centerX + (dx / distance) * speed;
            m[M_Y] = centerY + (dy / distance) * speed;
        }
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (life <= 0) return;
        tick++;
        waveTimer--;

        if (waveTimer <= 0) {
            wave++;
            waveTimer = 600;
            spawnedThisWave = 0;
            spawnCooldown = 20;
        }

        boolean isBossWave = (wave % 10 == 0);
        if (waveTimer > 200) {
            spawnCooldown--;
            if (spawnCooldown <= 0) {
                if (isBossWave) {
                    if (spawnedThisWave == 0) {
                        spawnBoss();
                        spawnedThisWave++;
                    }
                } else {
                    spawnMonster();
                    spawnedThisWave++;
                    spawnCooldown = 20;
                }
            }
        }

        if (monsters.size() >= MAX_FIELD_MONSTERS) life = 0;

        Iterator<Object[]> mobIter = monsters.iterator();
        while (mobIter.hasNext()) {
            Object[] m = mobIter.next();
            int hp = (int) m[M_HP];
            boolean isBoss = (boolean) m[M_BOSS];
            if (hp <= 0) {
                // ★ 몹 보상 너프 유지: 일반 1원, 보스 10원
                gold += isBoss ? 10 : 1;
                if (selectedMonster == m) selectedMonster = null;
                mobIter.remove();
            } else if (!moveMonster(m)) {
                if (selectedMonster == m) selectedMonster = null;
                mobIter.remove();
                life -= isBoss ? 5 : 1;
            }
        }

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (!tileHasTower[y][x]) continue;
                if (tileCooldown[y][x] > 0) {
                    tileCooldown[y][x]--;
                    continue;
                }

                Object[] target = findClosestMonster(x, y);
                if (target == null) continue;

                int tier = tileTier[y][x];
                int damage;
                double multiplier = 1.0;

                if (tier == 4) {
                    damage = 500 + (hiddenUpgradeLevel * 100); // ★ 히든 타워 너프 유지
                    multiplier = 1.5;
                } else {
                    int tIndex = tileTypeIndex[y][x];
                    String targetElement = (String) target[M_ELEMENT];
                    damage = (tier * 20) + (upgradeLevels[tIndex] * 30 * tier);
                    multiplier = calcElementMultiplier(tileTowerType[y][x], targetElement);
                }

                target[M_HP] = (int) target[M_HP] - (int) (damage * multiplier);
                tileCooldown[y][x] = 20;

                Object[] laser = new Object[7];
                laser[L_START_X] = x * TILE_SIZE + GRID_ORIGIN_X + 22;
                laser[L_START_Y] = y * TILE_SIZE + GRID_ORIGIN_Y + 22;
                laser[L_END_X] = (int) (double) target[M_X];
                laser[L_END_Y] = (int) (double) target[M_Y];
                laser[L_FRAMES] = 3;
                laser[L_COLOR] = tileColor[y][x];
                laser[L_HEAVY] = tier == 4;
                lasers.add(laser);
            }
        }

        lasers.removeIf(l -> {
            int frames = (int) l[L_FRAMES] - 1;
            l[L_FRAMES] = frames;
            return frames <= 0;
        });
        repaint();
    }

    private double calcElementMultiplier(String towerElement, String targetElement) {
        if ("Boss".equals(targetElement)) return 1.0;
        if ("Fire".equals(towerElement) && "Nature".equals(targetElement)) return 1.5;
        if ("Fire".equals(towerElement) && "Water".equals(targetElement)) return 0.5;
        if ("Water".equals(towerElement) && "Fire".equals(targetElement)) return 1.5;
        if ("Water".equals(towerElement) && "Nature".equals(targetElement)) return 0.5;
        if ("Nature".equals(towerElement) && "Water".equals(targetElement)) return 1.5;
        if ("Nature".equals(towerElement) && "Fire".equals(targetElement)) return 0.5;
        return 1.0;
    }

    private Object[] findClosestMonster(int tileX, int tileY) {
        int towerCenterX = tileX * TILE_SIZE + GRID_ORIGIN_X + 22;
        int towerCenterY = tileY * TILE_SIZE + GRID_ORIGIN_Y + 22;

        int tier = tileTier[tileY][tileX];
        double range;
        if (tier == 4) range = 200.0 + (hiddenUpgradeLevel * 20.0);
        else range = 120.0 + (upgradeLevels[tileTypeIndex[tileY][tileX]] * 15.0);

        for (Object[] m : monsters) {
            double dx = (double) m[M_X] - towerCenterX;
            double dy = (double) m[M_Y] - towerCenterY;
            if (Math.hypot(dx, dy) <= range) return m;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintBackground(g2);
        paintTopHud(g2);
        paintPath(g2);
        paintGrid(g2);
        paintMonsters(g2);
        paintLasers(g2);
        paintBottomPanel(g2);

        // ★ 게임 오버 시 UI 처리
        if (life <= 0) {
            g2.setColor(new Color(8, 10, 15, 210));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 105, 105));
            g2.setFont(new Font("Malgun Gothic", Font.BOLD, 42));
            drawCentered(g2, "\uAC8C\uC784 \uC624\uBC84", getWidth() / 2, getHeight() / 2 - 20); // 게임 오버

            g2.setColor(new Color(235, 240, 250));
            g2.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
            drawCentered(g2, "\uB3C4\uB2EC \uC6E8\uC774\uBE0C: " + wave, getWidth() / 2, getHeight() / 2 + 20); // 도달 웨이브

            // ★ 다시 시작 버튼 렌더링
            g2.setColor(new Color(78, 112, 136));
            g2.fillRoundRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height, 12, 12);
            g2.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
            drawCentered(g2, "\uB2E4\uC2DC \uC2DC\uC791", restartButton.x + restartButton.width / 2, restartButton.y + restartButton.height / 2 + 6); // 다시 시작
        }

        g2.dispose();
    }

    private void paintBackground(Graphics2D g2) {
        g2.setPaint(new GradientPaint(0, 0, new Color(18, 25, 39), 0, getHeight(), new Color(10, 14, 23)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (bgTexture != null) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            g2.drawImage(bgTexture, 0, 0, getWidth(), getHeight(), null);
            g2.setComposite(AlphaComposite.SrcOver);
        }
        g2.setColor(new Color(255, 255, 255, 20));
        for (int i = 0; i < 36; i++) {
            int x = (i * 71 + 19) % PANEL_W;
            int y = (i * 29 + tick * 2) % PANEL_H;
            g2.fillOval(x, y, 2, 2);
        }
    }

    private void paintTopHud(Graphics2D g2) {
        paintCard(g2, 8, 8, 524, TOP_PANEL_H, new Color(18, 26, 38), new Color(78, 98, 126));

        boolean isBossWave = wave % 10 == 0;
        String status;
        if (waveTimer > 200) {
            status = isBossWave
                    ? "\uBCF4\uC2A4 \uCD9C\uD604 \uC784\uBC15"
                    : String.format("\uC2A4\uD3F0 \uAC04\uACA9 %.1f\uCD08", spawnCooldown / 20.0);
        } else {
            status = "\uBC30\uCE58 \uC2DC\uAC04";
        }

        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        g2.setColor(new Color(236, 241, 250));
        g2.drawString(
                "\uC6E8\uC774\uBE0C " + wave + " | \uC2DC\uAC04 " + (waveTimer / 20) + "\uCD08 | " + status,
                18, 28
        );

        g2.setColor(monsters.size() >= 80 ? new Color(255, 115, 115) : new Color(165, 207, 255));
        g2.drawString(
                "\uD544\uB4DC " + monsters.size() + "/" + MAX_FIELD_MONSTERS
                        + " | \uC0DD\uBA85 " + life + " | \uACE8\uB4DC " + gold,
                18, 45
        );

        paintRoundedButton(
                g2,
                speedButton,
                currentSpeed == 1 ? new Color(70, 92, 118) : new Color(228, 84, 84),
                "\uC18D\uB3C4 x" + currentSpeed
        );

        float waveProgress = Math.max(0f, Math.min(1f, (600 - waveTimer) / 600f));
        g2.setColor(new Color(22, 31, 47));
        g2.fillRoundRect(8, TOP_PANEL_H + 14, 524, 12, 8, 8);
        g2.setColor(new Color(84, 107, 137));
        g2.drawRoundRect(8, TOP_PANEL_H + 14, 524, 12, 8, 8);
        g2.setPaint(new GradientPaint(8, TOP_PANEL_H + 14, new Color(80, 184, 255), 532, TOP_PANEL_H + 26, new Color(110, 246, 178)));
        g2.fillRoundRect(8, TOP_PANEL_H + 14, (int) (524 * waveProgress), 12, 8, 8);
    }

    private void paintPath(Graphics2D g2) {
        boolean isBossWave = wave % 10 == 0;
        float pulse = (float) (0.5 + 0.5 * Math.sin(tick * 0.1));
        int alpha = isBossWave ? (int) (130 + pulse * 60) : 120;
        g2.setColor(isBossWave ? new Color(255, 130, 230, alpha) : new Color(94, 118, 148, alpha));
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < waypoints.length - 1; i++) {
            g2.drawLine(waypoints[i][0], waypoints[i][1], waypoints[i + 1][0], waypoints[i + 1][1]);
        }
    }

    private void paintGrid(Graphics2D g2) {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int drawX = GRID_ORIGIN_X + x * TILE_SIZE;
                int drawY = GRID_ORIGIN_Y + y * TILE_SIZE;
                if (tileBuildable[y][x]) {
                    g2.drawImage(buildTileTexture, drawX - 1, drawY - 1, TILE_SIZE + 2, TILE_SIZE + 2, null);
                } else {
                    drawPathTile(g2, x, y, drawX, drawY);
                }

                if (tileHasTower[y][x]) {
                    drawTower(g2, x, y, drawX, drawY);
                }

                if (tileSelected[y][x]) {
                    g2.setColor(new Color(131, 252, 176));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(drawX + 1, drawY + 1, TILE_SIZE - 2, TILE_SIZE - 2, 8, 8);
                    int range = tileTier[y][x] == 4 ? 200 + hiddenUpgradeLevel * 20 : 120 + upgradeLevels[tileTypeIndex[y][x]] * 15;
                    g2.setColor(new Color(170, 240, 210, 45));
                    g2.fillOval(drawX + 22 - range, drawY + 22 - range, range * 2, range * 2);
                }
            }
        }
    }

    private void drawPathTile(Graphics2D g2, int gridX, int gridY, int drawX, int drawY) {
        boolean up = isPathTile(gridX, gridY - 1);
        boolean right = isPathTile(gridX + 1, gridY);
        boolean down = isPathTile(gridX, gridY + 1);
        boolean left = isPathTile(gridX - 1, gridY);
        int connected = (up ? 1 : 0) + (right ? 1 : 0) + (down ? 1 : 0) + (left ? 1 : 0);

        BufferedImage texture = pathStraightTexture;
        double rotation = 0.0;

        if (connected >= 3) {
            texture = pathCrossTexture;
        } else if (connected == 2) {
            if ((up && down) || (left && right)) {
                texture = pathStraightTexture;
                rotation = (left && right) ? Math.PI / 2.0 : 0.0;
            } else {
                texture = pathCurveTexture;
                rotation = getCurveRotation(up, right, down, left);
            }
        } else if (connected == 1) {
            texture = pathStraightTexture;
            rotation = (left || right) ? Math.PI / 2.0 : 0.0;
        }

        drawRotatedTile(g2, texture, drawX, drawY, rotation);
    }

    private boolean isPathTile(int x, int y) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE && !tileBuildable[y][x];
    }

    private double getCurveRotation(boolean up, boolean right, boolean down, boolean left) {
        if (up && right) return 0.0;
        if (right && down) return Math.PI / 2.0;
        if (down && left) return Math.PI;
        if (left && up) return -Math.PI / 2.0;
        return 0.0;
    }

    private void drawRotatedTile(Graphics2D g2, BufferedImage texture, int drawX, int drawY, double rotation) {
        if (texture == null) return;
        if (Math.abs(rotation) < 0.0001) {
            g2.drawImage(texture, drawX - 1, drawY - 1, TILE_SIZE + 2, TILE_SIZE + 2, null);
            return;
        }

        AffineTransform original = g2.getTransform();
        AffineTransform transform = new AffineTransform();
        transform.translate(drawX + TILE_SIZE / 2.0, drawY + TILE_SIZE / 2.0);
        transform.rotate(rotation);
        double bleedScale = (TILE_SIZE + 2.0) / TILE_SIZE;
        transform.scale(bleedScale, bleedScale);
        transform.translate(-TILE_SIZE / 2.0, -TILE_SIZE / 2.0);
        g2.drawImage(texture, transform, null);
        g2.setTransform(original);
    }

    private void drawTower(Graphics2D g2, int gridX, int gridY, int drawX, int drawY) {
        int tier = tileTier[gridY][gridX];
        String type = tileTowerType[gridY][gridX];
        Color color = tileColor[gridY][gridX];
        int spriteIndex = getTowerSpriteIndex(type);

        g2.setColor(new Color(18, 26, 38));
        g2.fillRoundRect(drawX + 6, drawY + 6, 33, 33, 10, 10);
        g2.setColor(new Color(116, 136, 164));
        g2.drawRoundRect(drawX + 6, drawY + 6, 33, 33, 10, 10);

        BufferedImage sprite = towerSprites[spriteIndex];
        int spriteOffset = (TILE_SIZE - TOWER_SPRITE_SIZE) / 2;
        if (sprite != null) {
            g2.drawImage(sprite, drawX + spriteOffset, drawY + spriteOffset, TOWER_SPRITE_SIZE, TOWER_SPRITE_SIZE, null);
        } else {
            drawFallbackTowerGlyph(g2, drawX + spriteOffset, drawY + spriteOffset, type, color);
        }

        if (tier >= 3) {
            g2.setColor(new Color(255, 235, 150, 70));
            g2.fillOval(drawX + 1, drawY + 1, 43, 43);
        }

        g2.setColor(new Color(239, 244, 254));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("T" + tier, drawX + 4, drawY + 40);
    }

    private int getTowerSpriteIndex(String type) {
        if ("Fire".equals(type)) return 0;
        if ("Water".equals(type)) return 1;
        if ("Nature".equals(type)) return 2;
        if ("Arcane".equals(type)) return 3;
        if ("Shadow".equals(type)) return 4;
        if ("Chaos".equals(type)) return 5;
        return 0;
    }

    private void drawFallbackTowerGlyph(Graphics2D g2, int x, int y, String type, Color color) {
        if ("Fire".equals(type)) {
            g2.setColor(new Color(255, 140, 66));
            int[] fx = {x + 14, x + 9, x + 12, x + 11, x + 19, x + 17, x + 21};
            int[] fy = {y + 1, y + 11, y + 11, y + 18, y + 13, y + 22, y + 12};
            g2.fillPolygon(fx, fy, fx.length);
        } else if ("Water".equals(type)) {
            g2.setColor(new Color(123, 203, 255));
            int[] dx = {x + 14, x + 9, x + 11, x + 14, x + 17, x + 19};
            int[] dy = {y + 2, y + 10, y + 18, y + 23, y + 18, y + 10};
            g2.fillPolygon(dx, dy, dx.length);
        } else if ("Nature".equals(type)) {
            g2.setColor(new Color(98, 216, 121));
            g2.fillOval(x + 5, y + 8, 9, 14);
            g2.fillOval(x + 13, y + 7, 9, 14);
            g2.setColor(new Color(52, 156, 77));
            g2.drawLine(x + 14, y + 23, x + 14, y + 6);
        } else if ("Arcane".equals(type)) {
            g2.setColor(new Color(255, 232, 130));
            int[] sx = {x + 14, x + 18, x + 24, x + 19, x + 20, x + 14, x + 8, x + 9, x + 4, x + 10};
            int[] sy = {y + 1, y + 8, y + 8, y + 13, y + 20, y + 15, y + 20, y + 13, y + 8, y + 8};
            g2.fillPolygon(sx, sy, sx.length);
        } else if ("Shadow".equals(type)) {
            g2.setColor(new Color(130, 103, 232));
            g2.fillOval(x + 6, y + 4, 15, 18);
            g2.setColor(new Color(25, 32, 48));
            g2.fillOval(x + 10, y + 4, 11, 18);
        } else if ("Chaos".equals(type)) {
            g2.setColor(new Color(255, 94, 193));
            g2.fillOval(x + 5, y + 7, 18, 14);
            g2.setColor(new Color(29, 35, 50));
            g2.fillOval(x + 10, y + 10, 9, 7);
            g2.setColor(Color.WHITE);
            g2.fillOval(x + 12, y + 12, 4, 4);
        } else {
            g2.setColor(color == null ? new Color(180, 190, 205) : color);
            g2.fillOval(x + 6, y + 6, 15, 15);
        }
    }

    private void paintMonsters(Graphics2D g2) {
        for (Object[] m : monsters) {
            boolean isBoss = (boolean) m[M_BOSS];
            double baseSize = (double) m[M_RADIUS] * 2.0;
            double scale = isBoss ? MONSTER_SCALE_BOSS : MONSTER_SCALE_NORMAL;
            int size = (int) Math.round(baseSize * scale);
            int drawX = (int) Math.round((double) m[M_X] - (size / 2.0));
            int drawY = (int) Math.round((double) m[M_Y] - (size / 2.0));

            BufferedImage sprite = getMonsterSprite((String) m[M_ELEMENT], isBoss);
            if (sprite != null) {
                g2.drawImage(sprite, drawX, drawY, size, size, null);
            } else {
                drawFallbackMonster(g2, m, drawX, drawY, size);
            }

            if (m == selectedMonster) {
                g2.setColor(new Color(255, 238, 141));
                g2.drawRoundRect(drawX - 2, drawY - 2, size + 4, size + 4, 8, 8);
            } else {
                g2.setColor(new Color(250, 250, 255, 180));
                if (isBoss) g2.drawRoundRect(drawX, drawY, size, size, 7, 7);
                else g2.drawOval(drawX, drawY, size, size);
            }

            g2.setColor(new Color(196, 48, 58));
            g2.fillRoundRect(drawX, drawY - 10, size, 5, 3, 3);
            g2.setColor(new Color(98, 224, 123));
            int hpWidth = (int) (((int) m[M_HP] / (double) (int) m[M_MAX_HP]) * size);
            if (hpWidth < 0) hpWidth = 0;
            g2.fillRoundRect(drawX, drawY - 10, hpWidth, 5, 3, 3);
        }
    }

    private BufferedImage getMonsterSprite(String element, boolean isBoss) {
        if (isBoss) return monsterSprites[3];
        if ("Fire".equals(element)) return monsterSprites[0];
        if ("Water".equals(element)) return monsterSprites[1];
        if ("Nature".equals(element)) return monsterSprites[2];
        return null;
    }

    private void drawFallbackMonster(Graphics2D g2, Object[] m, int drawX, int drawY, int size) {
        boolean isBoss = (boolean) m[M_BOSS];
        String element = (String) m[M_ELEMENT];
        Color color = (Color) m[M_COLOR];
        g2.setColor(color);
        if (isBoss) g2.fillRoundRect(drawX, drawY, size, size, 9, 9);
        else g2.fillOval(drawX, drawY, size, size);

        g2.setColor(new Color(255, 255, 255, 45));
        g2.fillOval(drawX + 4, drawY + 4, Math.max(5, size / 3), Math.max(5, size / 3));
        g2.setColor(new Color(20, 24, 30));
        int eyeY = drawY + size / 3;
        g2.fillOval(drawX + size / 4, eyeY, 4, 4);
        g2.fillOval(drawX + (size * 2 / 3) - 2, eyeY, 4, 4);
        g2.drawArc(drawX + size / 3, drawY + size / 2, size / 3, size / 4, 200, 140);

        if ("Fire".equals(element)) {
            g2.setColor(new Color(255, 190, 120));
            g2.drawLine(drawX + size / 2, drawY - 2, drawX + size / 2 + 3, drawY + 4);
        } else if ("Water".equals(element)) {
            g2.setColor(new Color(188, 232, 255));
            g2.fillOval(drawX + size / 2 - 2, drawY - 2, 4, 6);
        } else if ("Nature".equals(element)) {
            g2.setColor(new Color(146, 228, 140));
            g2.drawLine(drawX + size / 2, drawY - 2, drawX + size / 2, drawY + 4);
            g2.drawLine(drawX + size / 2, drawY + 1, drawX + size / 2 + 3, drawY + 3);
        }
    }

    private void paintLasers(Graphics2D g2) {
        for (Object[] l : lasers) {
            g2.setColor((Color) l[L_COLOR]);
            g2.setStroke(new BasicStroke((boolean) l[L_HEAVY] ? 7f : 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) l[L_START_X], (int) l[L_START_Y], (int) l[L_END_X], (int) l[L_END_Y]);
        }
    }

    private void paintBottomPanel(Graphics2D g2) {
        paintCard(g2, 6, BOTTOM_PANEL_Y - 2, 528, 132, new Color(16, 22, 34, 235), new Color(87, 103, 132));
        paintCard(g2, 10, BOTTOM_PANEL_Y + 6, 240, 112, new Color(27, 36, 52), new Color(102, 122, 155));
        paintCard(g2, 258, BOTTOM_PANEL_Y + 6, 272, 112, new Color(27, 36, 52), new Color(102, 122, 155));

        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        g2.setColor(new Color(236, 241, 250));
        g2.drawString("\uD0C0\uC6CC \uBE44\uC6A9: 10 \uACE8\uB4DC", 20, BOTTOM_PANEL_Y + 24);
        g2.setColor(new Color(169, 181, 201));
        g2.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        g2.drawString(
                "\uAC19\uC740 \uD2F0\uC5B4+\uD0C0\uC785 \uD0C0\uC6CC \uD569\uC131 \uC2DC \uAC15\uD654\uB429\uB2C8\uB2E4.",
                20, BOTTOM_PANEL_Y + 42
        );
        g2.drawString(
                "\uD2F0\uC5B43 \uB07C\uB9AC \uD569\uC131\uD558\uBA74 \uC2A4\uD398\uC15C \uD0C0\uC6CC\uAC00 \uB098\uC635\uB2C8\uB2E4.",
                20, BOTTOM_PANEL_Y + 58
        );

        if (hasSelectedTile() && tileHasTower[selectedTileY][selectedTileX]) {
            drawTowerInfo(g2);
            paintRoundedButton(
                    g2, sellButton, new Color(215, 74, 74),
                    "\uD310\uB9E4 +" + tileTier[selectedTileY][selectedTileX] * 5
            );
        } else if (selectedMonster != null) {
            g2.setColor(new Color(235, 240, 250));
            String prefix = (boolean) selectedMonster[M_BOSS] ? "[\uBCF4\uC2A4] " : "";
            g2.drawString("\uC120\uD0DD: " + prefix + toKoreanLabel((String) selectedMonster[M_ELEMENT]), 20, BOTTOM_PANEL_Y + 80);
            g2.drawString("HP " + selectedMonster[M_HP] + " / " + selectedMonster[M_MAX_HP], 20, BOTTOM_PANEL_Y + 100);
        } else {
            g2.setColor(new Color(167, 178, 196));
            g2.drawString("\uD0C0\uC6CC \uB610\uB294 \uBAAC\uC2A4\uD130\uB97C \uD074\uB9AD\uD558\uC138\uC694.", 20, BOTTOM_PANEL_Y + 90);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        g2.drawString("\uC5C5\uADF8\uB808\uC774\uB4DC (\uC77C\uBC18 20 / \uC2A4\uD398\uC15C 50)", 266, BOTTOM_PANEL_Y + 24);

        for (int i = 0; i < 3; i++) {
            paintRoundedButton(g2, upgradeButtons[i], ELEMENT_COLORS[i], ELEMENT_LABELS[i] + " Lv." + upgradeLevels[i]);
        }
        paintRoundedButton(g2, upgradeButtons[3], new Color(148, 72, 210), "\uC2A4\uD398\uC15C Lv." + hiddenUpgradeLevel);
    }

    private void drawTowerInfo(Graphics2D g2) {
        String towerType = toKoreanLabel(tileTowerType[selectedTileY][selectedTileX]);
        int tier = tileTier[selectedTileY][selectedTileX];
        g2.setColor(new Color(235, 240, 250));
        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 12));

        if (tier == 4) {
            int atk = 500 + (hiddenUpgradeLevel * 100);
            int range = 200 + (hiddenUpgradeLevel * 20);
            g2.setColor(new Color(255, 232, 135));
            g2.drawString("\uC120\uD0DD: " + towerType + " \uC2A4\uD398\uC15C \uD0C0\uC6CC", 20, BOTTOM_PANEL_Y + 76);
            g2.setColor(Color.WHITE);
            g2.drawString("\uACF5\uACA9\uB825 " + atk + " | \uC0AC\uAC70\uB9AC " + range, 20, BOTTOM_PANEL_Y + 96);
        } else {
            int typeIdx = tileTypeIndex[selectedTileY][selectedTileX];
            int atk = (tier * 20) + (upgradeLevels[typeIdx] * 30 * tier);
            int range = 120 + (upgradeLevels[typeIdx] * 15);
            g2.drawString(
                    "\uC120\uD0DD: " + towerType + " \uD0C0\uC6CC (\uD2F0\uC5B4 " + tier + ")",
                    20, BOTTOM_PANEL_Y + 76
            );
            g2.drawString("\uACF5\uACA9\uB825 " + atk + " | \uC0AC\uAC70\uB9AC " + range, 20, BOTTOM_PANEL_Y + 96);
        }
    }

    private void paintCard(Graphics2D g2, int x, int y, int w, int h, Color fill, Color border) {
        g2.setColor(fill);
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setColor(border);
        g2.drawRoundRect(x, y, w, h, 14, 14);
    }

    private void paintRoundedButton(Graphics2D g2, Rectangle rect, Color fill, String label) {
        g2.setColor(fill);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g2.setColor(new Color(235, 240, 250));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
        drawCentered(g2, label, rect.x + rect.width / 2, rect.y + rect.height / 2 + 4);
    }

    private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }

    private String toKoreanLabel(String element) {
        if ("Fire".equals(element)) return "\uD654\uC5FC";
        if ("Water".equals(element)) return "\uBB3C";
        if ("Nature".equals(element)) return "\uC790\uC5F0";
        if ("Arcane".equals(element)) return "\uBE44\uC804";
        if ("Shadow".equals(element)) return "\uADF8\uB9BC\uC790";
        if ("Chaos".equals(element)) return "\uD63C\uB3C8";
        if ("Boss".equals(element)) return "\uBCF4\uC2A4";
        return element;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("\uB79C\uB364 \uD0C0\uC6CC \uB514\uD39C\uC2A4 - \uB2E4\uC2DC\uC2DC\uC791 \uC801\uC6A9");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new RandomTowerDefense());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}