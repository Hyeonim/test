import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomTowerDefense extends JPanel implements ActionListener, MouseListener {
    private static final int GRID_SIZE = 12;
    private static final int TILE_SIZE = 45;

    private int life = 20;
    private int gold = 150;

    private int wave = 1;
    private int waveTimer = 600;
    private int spawnCooldown = 20;
    private int spawnedThisWave = 0;
    private final int MAX_FIELD_MONSTERS = 100;

    private int currentSpeed = 1;

    private final int[] upgradeLevels = {0, 0, 0};
    private final int UPGRADE_COST = 20;

    private int hiddenUpgradeLevel = 0;
    private final int HIDDEN_UPGRADE_COST = 50;

    private final int[][] mapGrid = {
            { 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0 },
            { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0 },
            { 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0 }
    };

    private final String[] types = {"불", "물", "풀"};
    private final Color[] typeColors = {new Color(255, 100, 100), new Color(100, 150, 255), new Color(100, 255, 100)};

    private final String[] hiddenTypes = {"빛(히든)", "어둠(히든)", "혼돈(히든)"};
    private final Color[] hiddenColors = {Color.WHITE, new Color(80, 80, 80), Color.MAGENTA};

    private final boolean[][] tileBuildable = new boolean[GRID_SIZE][GRID_SIZE];
    private final boolean[][] tileHasTower = new boolean[GRID_SIZE][GRID_SIZE];
    private final boolean[][] tileSelected = new boolean[GRID_SIZE][GRID_SIZE];
    private final String[][] tileTowerType = new String[GRID_SIZE][GRID_SIZE];
    private final Color[][] tileColor = new Color[GRID_SIZE][GRID_SIZE];
    private final int[][] tileTier = new int[GRID_SIZE][GRID_SIZE];
    private final int[][] tileTypeIndex = new int[GRID_SIZE][GRID_SIZE];
    private final int[][] tileCooldown = new int[GRID_SIZE][GRID_SIZE];

    private int selectedTileX = -1;
    private int selectedTileY = -1;
    private Object[] selectedMonster = null;

    private final List<Object[]> monsters = new ArrayList<>();
    private final List<Object[]> lasers = new ArrayList<>();

    private Timer gameLoop;
    private final Random random = new Random();

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

    private final int[][] gridWaypoints = {
            {0, 0}, {0, 4}, {11, 4}, {11, 0}, {6, 0}, {6, 11},
            {11, 11}, {11, 7}, {0, 7}, {0, 11}, {3, 11}, {3, 0}
    };
    private final int[][] waypoints = new int[12][2];

    public RandomTowerDefense() {
        setPreferredSize(new Dimension(540, 720));
        setBackground(Color.DARK_GRAY);

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                tileBuildable[y][x] = mapGrid[y][x] == 1;
                tileTowerType[y][x] = "";
            }
        }

        for (int i = 0; i < 12; i++) {
            waypoints[i][0] = gridWaypoints[i][0] * TILE_SIZE + (TILE_SIZE / 2);
            waypoints[i][1] = gridWaypoints[i][1] * TILE_SIZE + (TILE_SIZE / 2) + 50;
        }

        addMouseListener(this);

        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();

        if (mouseY >= 10 && mouseY <= 40 && mouseX >= 450 && mouseX <= 520) {
            currentSpeed++;
            if (currentSpeed > 3) currentSpeed = 1;

            if (currentSpeed == 1) gameLoop.setDelay(50);
            else if (currentSpeed == 2) gameLoop.setDelay(25);
            else gameLoop.setDelay(16);

            repaint();
            return;
        }

        if (mouseY >= 590) {
            if (hasSelectedTile() && tileHasTower[selectedTileY][selectedTileX]) {
                if (mouseX >= 180 && mouseX <= 240 && mouseY >= 640 && mouseY <= 670) {
                    gold += tileTier[selectedTileY][selectedTileX] * 5;
                    clearTile(selectedTileX, selectedTileY);
                    clearSelectedTile();
                    repaint();
                    return;
                }
            }

            if (mouseY >= 630 && mouseY <= 670) {
                if (mouseX >= 265 && mouseX <= 325) doUpgrade(0);
                else if (mouseX >= 330 && mouseX <= 390) doUpgrade(1);
                else if (mouseX >= 395 && mouseX <= 455) doUpgrade(2);
                else if (mouseX >= 460 && mouseX <= 520) doHiddenUpgrade();
            }
            return;
        }

        boolean clickedMonster = false;
        for (int i = monsters.size() - 1; i >= 0; i--) {
            Object[] m = monsters.get(i);
            double mx = (double) m[M_X];
            double my = (double) m[M_Y];
            double radius = (double) m[M_RADIUS];
            if (Math.hypot(mx - mouseX, my - mouseY) <= radius + 5) {
                selectedMonster = m;
                clearSelectedTile();
                clickedMonster = true;
                break;
            }
        }

        if (!clickedMonster && mouseY >= 50 && mouseY < 590) {
            int gridX = mouseX / TILE_SIZE;
            int gridY = (mouseY - 50) / TILE_SIZE;

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
        if (hasSelectedTile()) {
            tileSelected[selectedTileY][selectedTileX] = false;
        }
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
        repaint();
    }

    private void doHiddenUpgrade() {
        if (gold >= HIDDEN_UPGRADE_COST) {
            gold -= HIDDEN_UPGRADE_COST;
            hiddenUpgradeLevel++;
        }
        repaint();
    }

    private void handleTileClick(int x, int y) {
        if (!tileBuildable[y][x]) {
            clearSelectedTile();
            return;
        }

        if (!tileHasTower[y][x]) {
            if (hasSelectedTile()) {
                clearSelectedTile();
            } else if (gold >= 10) {
                gold -= 10;
                int typeIndex = random.nextInt(types.length);
                setTower(x, y, types[typeIndex], typeColors[typeIndex], 1, typeIndex);
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
                int rIndex = random.nextInt(types.length);
                setTower(x, y, types[rIndex], typeColors[rIndex], newTier, rIndex);
                clearTile(sx, sy);
            } else if (tileTier[sy][sx] == 3) {
                int rIndex = random.nextInt(hiddenTypes.length);
                setTower(x, y, hiddenTypes[rIndex], hiddenColors[rIndex], 4, -1);
                clearTile(sx, sy);
            }
        }

        selectTile(x, y);
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
        int typeIndex = random.nextInt(types.length);
        int maxHp = 100 + ((wave - 1) * 80);
        monsters.add(createMonster(types[typeIndex], typeColors[typeIndex], maxHp, false));
    }

    private void spawnBoss() {
        int maxHp = (100 + ((wave - 1) * 80)) * 25;
        monsters.add(createMonster("무속성", Color.MAGENTA, maxHp, true));
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

        waveTimer--;

        if (waveTimer <= 0) {
            wave++;
            waveTimer = 600;
            spawnedThisWave = 0;
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
                gold += isBoss ? 50 : 5;
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

                int baseDamage;
                double multiplier = 1.0;
                int tier = tileTier[y][x];

                if (tier == 4) {
                    baseDamage = 800 + (hiddenUpgradeLevel * 200);
                    multiplier = 1.5;
                } else {
                    int tIndex = tileTypeIndex[y][x];
                    String targetElement = (String) target[M_ELEMENT];
                    baseDamage = (tier * 20) + (upgradeLevels[tIndex] * 30 * tier);

                    if (!targetElement.equals("무속성")) {
                        String towerElement = tileTowerType[y][x];
                        if (towerElement.equals("불") && targetElement.equals("풀")) multiplier = 1.5;
                        else if (towerElement.equals("불") && targetElement.equals("물")) multiplier = 0.5;
                        else if (towerElement.equals("물") && targetElement.equals("불")) multiplier = 1.5;
                        else if (towerElement.equals("물") && targetElement.equals("풀")) multiplier = 0.5;
                        else if (towerElement.equals("풀") && targetElement.equals("물")) multiplier = 1.5;
                        else if (towerElement.equals("풀") && targetElement.equals("불")) multiplier = 0.5;
                    }
                }

                int targetHp = (int) target[M_HP];
                target[M_HP] = targetHp - (int) (baseDamage * multiplier);
                tileCooldown[y][x] = 20;

                Object[] laser = new Object[7];
                laser[L_START_X] = x * TILE_SIZE + 22;
                laser[L_START_Y] = y * TILE_SIZE + 72;
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

    private Object[] findClosestMonster(int tileX, int tileY) {
        int towerCenterX = tileX * TILE_SIZE + 22;
        int towerCenterY = tileY * TILE_SIZE + 72;

        int tier = tileTier[tileY][tileX];
        double currentRange;
        if (tier == 4) {
            currentRange = 200.0 + (hiddenUpgradeLevel * 20.0);
        } else {
            currentRange = 120.0 + (upgradeLevels[tileTypeIndex[tileY][tileX]] * 15.0);
        }

        for (Object[] m : monsters) {
            double dx = (double) m[M_X] - towerCenterX;
            double dy = (double) m[M_Y] - towerCenterY;
            if (Math.hypot(dx, dy) <= currentRange) return m;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean isBossWave = (wave % 10 == 0);

        String spawnText = (waveTimer > 200)
                ? (isBossWave ? "보스 출현!" : String.format("스폰 간격: %.1f초", spawnCooldown / 20.0))
                : "[정비 시간] 타워를 배치하세요!";

        g2.setPaint(new GradientPaint(0, 0, new Color(22, 28, 40), 0, getHeight(), new Color(11, 15, 24)));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(18, 24, 36));
        g2.fillRoundRect(8, 8, 524, 38, 16, 16);
        g2.setColor(new Color(80, 100, 135));
        g2.drawRoundRect(8, 8, 524, 38, 16, 16);

        g2.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        if (life > 0) {
            if (isBossWave && waveTimer > 200) g2.setColor(new Color(255, 120, 250));
            else if (waveTimer <= 200) g2.setColor(new Color(125, 255, 170));
            else g2.setColor(new Color(230, 235, 245));

            g2.drawString("Wave: " + wave + " | 남은 시간: " + (waveTimer / 20) + "초 | " + spawnText, 16, 24);

            if (monsters.size() >= 80) g2.setColor(new Color(255, 115, 115));
            else g2.setColor(new Color(168, 206, 255));
            g2.drawString("필드 몹: " + monsters.size() + " / " + MAX_FIELD_MONSTERS + " | 라이프: " + life + " | 골드: " + gold, 16, 42);
        } else {
            g2.setColor(new Color(255, 90, 90));
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            g2.drawString("GAME OVER", 200, 35);
        }

        g2.setColor(currentSpeed == 1 ? new Color(77, 93, 112) : new Color(228, 79, 79));
        g2.fillRoundRect(450, 10, 70, 30, 12, 12);
        g2.setColor(new Color(235, 240, 250));
        g2.drawRoundRect(450, 10, 70, 30, 12, 12);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        g2.drawString("▶ x" + currentSpeed, 466, 31);

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int drawY = y * TILE_SIZE + 50;
                g2.setColor(tileBuildable[y][x] ? new Color(76, 92, 108) : new Color(31, 38, 48));
                g2.fillRoundRect(x * TILE_SIZE + 1, drawY + 1, TILE_SIZE - 2, TILE_SIZE - 2, 8, 8);
                g2.setColor(new Color(15, 20, 30));
                g2.drawRoundRect(x * TILE_SIZE + 1, drawY + 1, TILE_SIZE - 2, TILE_SIZE - 2, 8, 8);

                if (tileHasTower[y][x]) {
                    g2.setColor(tileColor[y][x]);
                    g2.fillRoundRect(x * TILE_SIZE + 6, y * TILE_SIZE + 56, 33, 33, 10, 10);
                    g2.setColor(new Color(12, 12, 12));
                    g2.drawRoundRect(x * TILE_SIZE + 6, y * TILE_SIZE + 56, 33, 33, 10, 10);

                    if (tileTier[y][x] == 4) {
                        g2.setColor(new Color(255, 229, 123));
                        g2.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                        g2.drawString(tileTowerType[y][x].replace("(히든)", ""), x * TILE_SIZE + 8, y * TILE_SIZE + 70);
                        g2.drawString("HIDDEN", x * TILE_SIZE + 6, y * TILE_SIZE + 84);
                    } else {
                        g2.setColor(new Color(15, 15, 15));
                        g2.setFont(new Font("맑은 고딕", Font.BOLD, 11));
                        g2.drawString(tileTowerType[y][x] + tileTier[y][x], x * TILE_SIZE + 10, y * TILE_SIZE + 78);
                    }
                }

                if (tileSelected[y][x]) {
                    g2.setColor(new Color(126, 250, 171));
                    g2.drawRoundRect(x * TILE_SIZE, drawY, TILE_SIZE, TILE_SIZE, 8, 8);
                    g2.drawRoundRect(x * TILE_SIZE + 1, drawY + 1, TILE_SIZE - 2, TILE_SIZE - 2, 7, 7);

                    int currentRange = (tileTier[y][x] == 4)
                            ? 200 + (hiddenUpgradeLevel * 20)
                            : 120 + (upgradeLevels[tileTypeIndex[y][x]] * 15);

                    g2.setColor(new Color(180, 240, 210, 55));
                    g2.fillOval(x * TILE_SIZE + 22 - currentRange, y * TILE_SIZE + 72 - currentRange, currentRange * 2, currentRange * 2);
                }
            }
        }

        for (Object[] m : monsters) {
            g2.setColor((Color) m[M_COLOR]);
            int drawX = (int) ((double) m[M_X] - (double) m[M_RADIUS]);
            int drawY = (int) ((double) m[M_Y] - (double) m[M_RADIUS]);
            int size = (int) ((double) m[M_RADIUS] * 2);
            boolean isBoss = (boolean) m[M_BOSS];

            if (isBoss) {
                g2.fillRoundRect(drawX, drawY, size, size, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                g2.drawString("BOSS", drawX + 3, drawY + 23);
            } else {
                g2.fillOval(drawX, drawY, size, size);
            }

            if (m == selectedMonster) {
                g2.setColor(new Color(255, 240, 140));
                g2.drawRoundRect(drawX - 2, drawY - 2, size + 4, size + 4, 6, 6);
            } else {
                g2.setColor(Color.WHITE);
                if (isBoss) g2.drawRoundRect(drawX, drawY, size, size, 6, 6);
                else g2.drawOval(drawX, drawY, size, size);
            }

            g2.setColor(new Color(200, 45, 45));
            g2.fillRoundRect(drawX, drawY - 10, size, 5, 3, 3);
            g2.setColor(new Color(88, 220, 110));
            int hpWidth = (int) (((int) m[M_HP] / (double) (int) m[M_MAX_HP]) * size);
            if (hpWidth < 0) hpWidth = 0;
            g2.fillRoundRect(drawX, drawY - 10, hpWidth, 5, 3, 3);
        }

        for (Object[] l : lasers) {
            g2.setColor((Color) l[L_COLOR]);
            g2.setStroke(new BasicStroke((boolean) l[L_HEAVY] ? 6 : 3));
            g2.drawLine((int) l[L_START_X], (int) l[L_START_Y], (int) l[L_END_X], (int) l[L_END_Y]);
        }

        g2.setColor(new Color(16, 20, 32, 240));
        g2.fillRoundRect(6, 592, 528, 124, 12, 12);
        g2.setColor(new Color(87, 103, 132));
        g2.drawRoundRect(6, 592, 528, 124, 12, 12);

        g2.setColor(new Color(28, 36, 50));
        g2.fillRoundRect(10, 600, 240, 100, 12, 12);
        g2.setColor(new Color(102, 122, 155));
        g2.drawRoundRect(10, 600, 240, 100, 12, 12);
        g2.setColor(new Color(235, 240, 250));
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        g2.drawString("설치 비용: 10 골드", 20, 615);

        if (hasSelectedTile() && tileHasTower[selectedTileY][selectedTileX]) {
            if (tileTier[selectedTileY][selectedTileX] == 4) {
                int currentAtk = 800 + (hiddenUpgradeLevel * 200);
                int currentRange = 200 + (hiddenUpgradeLevel * 20);
                g2.setColor(new Color(255, 232, 135));
                g2.drawString("선택됨: " + tileTowerType[selectedTileY][selectedTileX], 20, 635);
                g2.setColor(Color.WHITE);
                g2.drawString("공격력: " + currentAtk + " | 사거리: " + currentRange, 20, 658);
                g2.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                g2.drawString("(+히든 보너스: " + (hiddenUpgradeLevel * 200) + ")", 20, 680);
            } else {
                int typeIdx = tileTypeIndex[selectedTileY][selectedTileX];
                int tier = tileTier[selectedTileY][selectedTileX];
                int currentAtk = (tier * 20) + (upgradeLevels[typeIdx] * 30 * tier);
                int currentRange = 120 + (upgradeLevels[typeIdx] * 15);
                g2.drawString("선택됨: " + tileTowerType[selectedTileY][selectedTileX] + " 타워 (Lv." + tier + ")", 20, 635);
                g2.drawString("공격력: " + currentAtk + " | 사거리: " + currentRange, 20, 658);
                g2.setColor(new Color(255, 229, 140));
                g2.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                g2.drawString("(+업글 보너스: 뎀 " + (upgradeLevels[typeIdx] * 30 * tier) + ")", 20, 680);
            }

            g2.setColor(new Color(215, 74, 74));
            g2.fillRoundRect(180, 640, 60, 30, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(180, 640, 60, 30, 10, 10);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            g2.drawString("판매(" + (tileTier[selectedTileY][selectedTileX] * 5) + ")", 185, 660);

        } else if (selectedMonster != null) {
            String prefix = (boolean) selectedMonster[M_BOSS] ? "[보스] " : "";
            g2.setColor(new Color(235, 240, 250));
            g2.drawString("선택됨: " + prefix + (String) selectedMonster[M_ELEMENT], 20, 640);
            g2.drawString("체력: " + (int) selectedMonster[M_HP] + " / " + (int) selectedMonster[M_MAX_HP], 20, 665);
        } else {
            g2.setColor(new Color(167, 178, 196));
            g2.drawString("타워나 몬스터를 클릭하세요.", 20, 650);
        }

        g2.setColor(new Color(28, 36, 50));
        g2.fillRoundRect(260, 600, 270, 100, 12, 12);
        g2.setColor(new Color(102, 122, 155));
        g2.drawRoundRect(260, 600, 270, 100, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        g2.drawString("업그레이드 (일반 20골드 / 히든 50골드)", 265, 620);

        String[] btnNames = {"불", "물", "풀"};
        for (int i = 0; i < 3; i++) {
            int bx = 265 + (i * 65);
            g2.setColor(typeColors[i]);
            g2.fillRoundRect(bx, 630, 60, 40, 10, 10);
            g2.setColor(new Color(10, 10, 10));
            g2.drawRoundRect(bx, 630, 60, 40, 10, 10);

            g2.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            g2.drawString(btnNames[i] + " Lv." + upgradeLevels[i], bx + 8, 655);
        }

        int hbx = 265 + (3 * 65);
        g2.setColor(new Color(148, 72, 210));
        g2.fillRoundRect(hbx, 630, 60, 40, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(hbx, 630, 60, 40, 10, 10);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        g2.drawString("히든Lv." + hiddenUpgradeLevel, hbx + 5, 655);

        g2.dispose();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("스타 랜타디 - 히든 전용 업그레이드");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new RandomTowerDefense());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
