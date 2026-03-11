import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomTowerDefense extends JPanel implements ActionListener {
    private int life = 20;
    private int gold = 150;

    private int wave = 1;
    private int waveTimer = 600;
    private int spawnCooldown = 20;
    private int spawnedThisWave = 0;
    private final int MAX_FIELD_MONSTERS = 100;

    private int currentSpeed = 1;

    // ★ 업그레이드 변수 분리
    private int[] upgradeLevels = {0, 0, 0};
    private final int UPGRADE_COST = 20;

    private int hiddenUpgradeLevel = 0;
    private final int HIDDEN_UPGRADE_COST = 50; // 히든 업그레이드는 50원!

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
    private final int TILE_SIZE = 45;

    private final String[] types = {"불", "물", "풀"};
    private final Color[] typeColors = {new Color(255, 100, 100), new Color(100, 150, 255), new Color(100, 255, 100)};

    private final String[] hiddenTypes = {"빛(히든)", "어둠(히든)", "혼돈(히든)"};
    private final Color[] hiddenColors = {Color.WHITE, new Color(80, 80, 80), Color.MAGENTA};

    private Tile[][] tiles = new Tile[12][12];
    private Tile selectedTile = null;
    private Monster selectedMonster = null;

    private List<Monster> monsters = new ArrayList<>();
    private List<Laser> lasers = new ArrayList<>();
    private Timer gameLoop;
    private Random random = new Random();

    public RandomTowerDefense() {
        setPreferredSize(new Dimension(540, 720));
        setBackground(Color.DARK_GRAY);

        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) {
                boolean isBuildable = (mapGrid[y][x] == 1);
                tiles[y][x] = new Tile(x * TILE_SIZE, y * TILE_SIZE, isBuildable);
            }
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();

                // 1. 우측 상단 배속 버튼
                if (mouseY >= 10 && mouseY <= 40 && mouseX >= 450 && mouseX <= 520) {
                    currentSpeed++;
                    if (currentSpeed > 3) currentSpeed = 1;

                    if (currentSpeed == 1) gameLoop.setDelay(50);
                    else if (currentSpeed == 2) gameLoop.setDelay(25);
                    else if (currentSpeed == 3) gameLoop.setDelay(16);

                    repaint();
                    return;
                }

                // 2. 하단 UI 클릭
                if (mouseY >= 590) {
                    // 판매 버튼
                    if (selectedTile != null && selectedTile.hasTower) {
                        if (mouseX >= 180 && mouseX <= 240 && mouseY >= 640 && mouseY <= 670) {
                            gold += selectedTile.tier * 5;
                            selectedTile.clearTower();
                            selectedTile = null;
                            repaint();
                            return;
                        }
                    }

                    // ★ 업그레이드 버튼 4개 처리
                    if (mouseY >= 630 && mouseY <= 670) {
                        if (mouseX >= 265 && mouseX <= 325) doUpgrade(0);
                        else if (mouseX >= 330 && mouseX <= 390) doUpgrade(1);
                        else if (mouseX >= 395 && mouseX <= 455) doUpgrade(2);
                        else if (mouseX >= 460 && mouseX <= 520) doHiddenUpgrade(); // 히든 업글!
                    }
                    return;
                }

                // 3. 몬스터 클릭
                boolean clickedMonster = false;
                for (int i = monsters.size() - 1; i >= 0; i--) {
                    Monster m = monsters.get(i);
                    if (Math.hypot(m.centerX - mouseX, m.centerY - mouseY) <= m.radius + 5) {
                        selectedMonster = m;
                        if (selectedTile != null) selectedTile.isSelected = false;
                        selectedTile = null;
                        clickedMonster = true;
                        break;
                    }
                }

                // 4. 맵 타일 클릭
                if (!clickedMonster && mouseY >= 50 && mouseY < 590) {
                    int gridX = mouseX / TILE_SIZE;
                    int gridY = (mouseY - 50) / TILE_SIZE;

                    selectedMonster = null;
                    if (gridX >= 0 && gridX < 12 && gridY >= 0 && gridY < 12) {
                        handleTileClick(tiles[gridY][gridX]);
                    } else {
                        if (selectedTile != null) {
                            selectedTile.isSelected = false;
                            selectedTile = null;
                        }
                    }
                }
                repaint();
            }
        });

        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    private void doUpgrade(int typeIndex) {
        if (gold >= UPGRADE_COST) {
            gold -= UPGRADE_COST;
            upgradeLevels[typeIndex]++;
        }
        repaint();
    }

    // ★ 히든 전용 업그레이드 로직
    private void doHiddenUpgrade() {
        if (gold >= HIDDEN_UPGRADE_COST) {
            gold -= HIDDEN_UPGRADE_COST;
            hiddenUpgradeLevel++;
        }
        repaint();
    }

    private void handleTileClick(Tile tile) {
        if (!tile.isBuildable) {
            if (selectedTile != null) {
                selectedTile.isSelected = false;
                selectedTile = null;
            }
            return;
        }

        if (!tile.hasTower) {
            if (selectedTile != null) {
                selectedTile.isSelected = false;
                selectedTile = null;
            } else if (gold >= 10) {
                gold -= 10;
                int typeIndex = random.nextInt(types.length);
                tile.setTower(types[typeIndex], typeColors[typeIndex], 1, typeIndex);
            }
        } else {
            if (selectedTile == null) {
                selectedTile = tile;
                tile.isSelected = true;
            } else if (selectedTile == tile) {
                selectedTile.isSelected = false;
                selectedTile = null;
            } else {
                if (selectedTile.tier == tile.tier) {
                    if (selectedTile.tier < 3 && selectedTile.towerType.equals(tile.towerType)) {
                        int newTier = tile.tier + 1;
                        int rIndex = random.nextInt(types.length);
                        tile.setTower(types[rIndex], typeColors[rIndex], newTier, rIndex);
                        selectedTile.clearTower();
                    }
                    else if (selectedTile.tier == 3) {
                        int newTier = 4;
                        int rIndex = random.nextInt(hiddenTypes.length);
                        tile.setTower(hiddenTypes[rIndex], hiddenColors[rIndex], newTier, -1);
                        selectedTile.clearTower();
                    }
                }

                if (selectedTile != null) selectedTile.isSelected = false;
                selectedTile = tile;
                tile.isSelected = true;
            }
        }
    }

    private void spawnMonster() {
        int typeIndex = random.nextInt(types.length);
        int maxHp = 100 + ((wave - 1) * 80);
        monsters.add(new Monster(types[typeIndex], typeColors[typeIndex], maxHp, false));
    }

    private void spawnBoss() {
        int maxHp = (100 + ((wave - 1) * 80)) * 25;
        monsters.add(new Monster("무속성", Color.MAGENTA, maxHp, true));
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

        Iterator<Monster> mobIter = monsters.iterator();
        while (mobIter.hasNext()) {
            Monster m = mobIter.next();
            if (m.hp <= 0) {
                gold += m.isBoss ? 50 : 5;
                if (selectedMonster == m) selectedMonster = null;
                mobIter.remove();
            } else if (!m.move()) {
                if (selectedMonster == m) selectedMonster = null;
                mobIter.remove();
                life -= m.isBoss ? 5 : 1;
            }
        }

        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) {
                Tile t = tiles[y][x];
                if (t.hasTower) {
                    if (t.cooldownTimer > 0) t.cooldownTimer--;
                    else {
                        Monster target = findClosestMonster(t);
                        if (target != null) {
                            int baseDamage;
                            double multiplier = 1.0;

                            // ★ 히든 데미지 공식 독립
                            if (t.tier == 4) {
                                baseDamage = 800 + (hiddenUpgradeLevel * 200); // 업글당 200 증가!
                                multiplier = 1.5;
                            } else {
                                baseDamage = (t.tier * 20) + (upgradeLevels[t.typeIndex] * 30 * t.tier);
                                if (!target.element.equals("무속성")) {
                                    if (t.towerType.equals("불") && target.element.equals("풀")) multiplier = 1.5;
                                    else if (t.towerType.equals("불") && target.element.equals("물")) multiplier = 0.5;
                                    else if (t.towerType.equals("물") && target.element.equals("불")) multiplier = 1.5;
                                    else if (t.towerType.equals("물") && target.element.equals("풀")) multiplier = 0.5;
                                    else if (t.towerType.equals("풀") && target.element.equals("물")) multiplier = 1.5;
                                    else if (t.towerType.equals("풀") && target.element.equals("불")) multiplier = 0.5;
                                }
                            }

                            target.hp -= (int)(baseDamage * multiplier);
                            t.cooldownTimer = 20;

                            lasers.add(new Laser(t.x + 22, t.y + 72, (int)target.centerX, (int)target.centerY, t.color, t.tier == 4));
                        }
                    }
                }
            }
        }

        lasers.removeIf(Laser::update);
        repaint();
    }

    private Monster findClosestMonster(Tile t) {
        int towerCenterX = t.x + 22;
        int towerCenterY = t.y + 72;

        double currentRange;
        // ★ 히든 사거리 공식 독립
        if (t.tier == 4) {
            currentRange = 200.0 + (hiddenUpgradeLevel * 20.0);
        } else {
            currentRange = 120.0 + (upgradeLevels[t.typeIndex] * 15.0);
        }

        for (Monster m : monsters) {
            double dx = m.centerX - towerCenterX;
            double dy = m.centerY - towerCenterY;
            if (Math.hypot(dx, dy) <= currentRange) return m;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        boolean isBossWave = (wave % 10 == 0);

        String spawnText = (waveTimer > 200) ? (isBossWave ? "보스 출현!" : String.format("스폰 간격: %.1f초", spawnCooldown / 20.0)) : "[정비 시간] 타워를 배치하세요!";

        if (life > 0) {
            if(isBossWave && waveTimer > 200) g.setColor(Color.MAGENTA);
            else if(waveTimer <= 200) g.setColor(Color.GREEN);

            g.drawString("Wave: " + wave + " | 남은 시간: " + (waveTimer / 20) + "초 | " + spawnText, 10, 20);

            if (monsters.size() >= 80) g.setColor(Color.RED);
            else g.setColor(new Color(200, 200, 255));
            g.drawString("필드 몹: " + monsters.size() + " / " + MAX_FIELD_MONSTERS + " | 라이프: " + life + " | 미네랄: " + gold, 10, 42);
        } else {
            g.setColor(Color.RED);
            g.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            g.drawString("GAME OVER", 200, 35);
        }

        g.setColor(currentSpeed == 1 ? new Color(100, 100, 100) : new Color(200, 50, 50));
        g.fillRect(450, 10, 70, 30);
        g.setColor(Color.WHITE);
        g.drawRect(450, 10, 70, 30);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        g.drawString("▶ x" + currentSpeed, 467, 31);

        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) {
                Tile t = tiles[y][x];
                g.setColor(t.isBuildable ? new Color(85, 85, 85) : new Color(34, 34, 34));
                g.fillRect(t.x, t.y + 50, TILE_SIZE, TILE_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(t.x, t.y + 50, TILE_SIZE, TILE_SIZE);

                if (t.hasTower) {
                    g.setColor(t.color);
                    g.fillRect(t.x + 5, t.y + 55, 35, 35);
                    g.setColor(Color.BLACK);

                    if(t.tier == 4) {
                        g.setColor(Color.YELLOW);
                        g.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                        g.drawString(t.towerType.replace("(히든)", ""), t.x + 8, t.y + 70);
                        g.drawString("HIDDEN", t.x + 6, t.y + 83);
                    } else {
                        g.setFont(new Font("맑은 고딕", Font.BOLD, 11));
                        g.drawString(t.towerType+t.tier, t.x + 10, t.y + 77);
                    }
                }

                if (t.isSelected) {
                    g.setColor(Color.GREEN);
                    g.drawRect(t.x, t.y + 50, TILE_SIZE, TILE_SIZE);
                    g.drawRect(t.x + 1, t.y + 51, TILE_SIZE - 2, TILE_SIZE - 2);

                    int currentRange = (t.tier == 4)
                            ? 200 + (hiddenUpgradeLevel * 20)
                            : 120 + (upgradeLevels[t.typeIndex] * 15);

                    g.setColor(new Color(255, 255, 255, 50));
                    g.fillOval(t.x + 22 - currentRange, t.y + 72 - currentRange, currentRange * 2, currentRange * 2);
                }
            }
        }

        for (Monster m : monsters) {
            g.setColor(m.color);
            int drawX = (int)(m.centerX - m.radius);
            int drawY = (int)(m.centerY - m.radius);
            int size = (int)(m.radius * 2);

            if(m.isBoss) {
                g.fillRect(drawX, drawY, size, size);
                g.setColor(Color.WHITE);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                g.drawString("BOSS", drawX + 3, drawY + 23);
            } else {
                g.fillOval(drawX, drawY, size, size);
            }

            if (m == selectedMonster) {
                g.setColor(Color.YELLOW);
                g.drawRect(drawX - 2, drawY - 2, size + 4, size + 4);
            } else {
                g.setColor(Color.WHITE);
                if(m.isBoss) g.drawRect(drawX, drawY, size, size);
                else g.drawOval(drawX, drawY, size, size);
            }

            g.setColor(Color.RED);
            g.fillRect(drawX, drawY - 10, size, 5);
            g.setColor(Color.GREEN);
            int hpWidth = (int)((m.hp / (double)m.maxHp) * size);
            if (hpWidth < 0) hpWidth = 0;
            g.fillRect(drawX, drawY - 10, hpWidth, 5);
        }

        for (Laser l : lasers) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(l.color);
            g2.setStroke(new BasicStroke(l.isHeavy ? 6 : 3));
            g2.drawLine(l.startX, l.startY, l.endX, l.endY);
        }

        // ==========================================
        // 하단 상태창 UI
        // ==========================================
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 590, 540, 130);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 590, 540, 130);

        g.drawRect(10, 600, 240, 100);
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        if (selectedTile != null && selectedTile.hasTower) {
            if (selectedTile.tier == 4) {
                int currentAtk = 800 + (hiddenUpgradeLevel * 200);
                int currentRange = 200 + (hiddenUpgradeLevel * 20);
                g.setColor(Color.YELLOW);
                g.drawString("선택됨: " + selectedTile.towerType, 20, 625);
                g.setColor(Color.WHITE);
                g.drawString("공격력: " + currentAtk + " | 사거리: " + currentRange, 20, 650);
                g.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                g.drawString("(+히든 전용 업글 보너스: " + (hiddenUpgradeLevel * 200) + ")", 20, 675);
            } else {
                int currentAtk = (selectedTile.tier * 20) + (upgradeLevels[selectedTile.typeIndex] * 30 * selectedTile.tier);
                int currentRange = 120 + (upgradeLevels[selectedTile.typeIndex] * 15);
                g.drawString("선택됨: " + selectedTile.towerType + " 타워 (Lv." + selectedTile.tier + ")", 20, 625);
                g.drawString("공격력: " + currentAtk + " | 사거리: " + currentRange, 20, 650);
                g.setColor(Color.YELLOW);
                g.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                g.drawString("(+업글 보너스: 뎀 " + (upgradeLevels[selectedTile.typeIndex] * 30 * selectedTile.tier) + ")", 20, 675);
            }

            g.setColor(new Color(200, 50, 50));
            g.fillRect(180, 640, 60, 30);
            g.setColor(Color.WHITE);
            g.drawRect(180, 640, 60, 30);
            g.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            g.drawString("판매(" + (selectedTile.tier * 5) + ")", 185, 660);

        } else if (selectedMonster != null) {
            String prefix = selectedMonster.isBoss ? "[보스] " : "";
            g.drawString("선택됨: " + prefix + selectedMonster.element, 20, 630);
            g.drawString("체력: " + selectedMonster.hp + " / " + selectedMonster.maxHp, 20, 660);
        } else {
            g.setColor(Color.GRAY);
            g.drawString("타워나 몬스터를 클릭하세요.", 20, 650);
        }

        // ★ 업그레이드 구역 (버튼 4개로 분할)
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(260, 600, 270, 100);
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        g.drawString("업그레이드 (일반 20원 / 히든 50원)", 265, 620);

        String[] btnNames = {"불", "물", "풀"};
        for (int i = 0; i < 3; i++) {
            int bx = 265 + (i * 65);
            g.setColor(typeColors[i]);
            g.fillRect(bx, 630, 60, 40);
            g.setColor(Color.BLACK);
            g.drawRect(bx, 630, 60, 40);

            g.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            g.drawString(btnNames[i] + " Lv." + upgradeLevels[i], bx + 8, 655);
        }

        // ★ 4번째 버튼: 히든 타워 전용 업그레이드
        int hbx = 265 + (3 * 65);
        g.setColor(new Color(180, 0, 180)); // 진한 보라색 느낌
        g.fillRect(hbx, 630, 60, 40);
        g.setColor(Color.WHITE);
        g.drawRect(hbx, 630, 60, 40);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        g.drawString("히든Lv." + hiddenUpgradeLevel, hbx + 5, 655);
    }

    class Tile {
        int x, y;
        boolean isBuildable, hasTower = false, isSelected = false;
        String towerType = "";
        Color color = null;
        int tier = 0, typeIndex = 0, cooldownTimer = 0;
        public Tile(int x, int y, boolean isBuildable) { this.x = x; this.y = y; this.isBuildable = isBuildable; }
        public void setTower(String type, Color color, int tier, int typeIndex) {
            this.hasTower = true; this.towerType = type; this.color = color;
            this.tier = tier; this.typeIndex = typeIndex; this.cooldownTimer = 0;
        }
        public void clearTower() { this.hasTower = false; this.towerType = ""; this.color = null; this.tier = 0; this.isSelected = false; }
    }

    class Monster {
        double centerX, centerY;
        int targetWaypoint = 1, maxHp, hp;
        String element;
        Color color;
        boolean isBoss;
        double radius;

        int[][] gridWaypoints = {
                {0, 0}, {0, 4}, {11, 4}, {11, 0}, {6, 0}, {6, 11},
                {11, 11}, {11, 7}, {0, 7}, {0, 11}, {3, 11}, {3, 0}
        };
        int[][] waypoints = new int[12][2];

        public Monster(String element, Color color, int hp, boolean isBoss) {
            this.element = element; this.color = color; this.maxHp = hp; this.hp = hp;
            this.isBoss = isBoss;
            this.radius = isBoss ? 18.0 : 10.0;

            for(int i=0; i<12; i++) {
                waypoints[i][0] = gridWaypoints[i][0] * TILE_SIZE + (TILE_SIZE/2);
                waypoints[i][1] = gridWaypoints[i][1] * TILE_SIZE + (TILE_SIZE/2) + 50;
            }
            this.centerX = waypoints[0][0];
            this.centerY = waypoints[0][1];
        }

        public boolean move() {
            if (targetWaypoint >= waypoints.length) return false;
            int targetX = waypoints[targetWaypoint][0], targetY = waypoints[targetWaypoint][1];
            double speed = isBoss ? 2.5 : 4.0;

            double dx = targetX - centerX, dy = targetY - centerY;
            double distance = Math.hypot(dx, dy);
            if (distance <= speed) targetWaypoint++;
            else { centerX += (dx / distance) * speed; centerY += (dy / distance) * speed; }
            return true;
        }
    }

    class Laser {
        int startX, startY, endX, endY, framesLeft = 3;
        Color color;
        boolean isHeavy;
        public Laser(int startX, int startY, int endX, int endY, Color color, boolean isHeavy) {
            this.startX = startX; this.startY = startY; this.endX = endX; this.endY = endY; this.color = color; this.isHeavy = isHeavy;
        }
        public boolean update() { framesLeft--; return framesLeft <= 0; }
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