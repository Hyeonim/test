import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomTowerDefense extends JPanel implements ActionListener {
    private int life = 20;
    private int gold = 100;

    private int wave = 1;
    private int waveTimer = 600;
    private int spawnCooldown = 20;
    private int spawnedThisWave = 0; // 보스 1마리 스폰 체크용
    private final int MAX_FIELD_MONSTERS = 100;

    private int[] upgradeLevels = {0, 0, 0};
    private final int UPGRADE_COST = 20;

    // ★ 유저님이 직접 엑셀로 찍어주신 12x12 근본 맵 원상복구!
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

    private Tile[][] tiles = new Tile[12][12];
    private Tile selectedTile = null;
    private Monster selectedMonster = null;

    private List<Monster> monsters = new ArrayList<>();
    private List<Laser> lasers = new ArrayList<>();
    private Timer gameLoop;
    private Random random = new Random();

    public RandomTowerDefense() {
        setPreferredSize(new Dimension(540, 720)); // 커스텀 맵 사이즈에 맞춘 창 크기
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

                // 1. 하단 UI 클릭 (Y: 590 ~ 720)
                if (mouseY >= 590) {
                    if (mouseY >= 630 && mouseY <= 670) {
                        if (mouseX >= 270 && mouseX <= 345) doUpgrade(0);
                        else if (mouseX >= 355 && mouseX <= 430) doUpgrade(1);
                        else if (mouseX >= 440 && mouseX <= 515) doUpgrade(2);
                    }
                    return;
                }

                // 2. 몬스터 클릭 (보스도 편하게 클릭 가능)
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

                // 3. 맵 타일 클릭 (Top UI 50px 제외)
                if (!clickedMonster && mouseY >= 50 && mouseY < 590) {
                    int gridX = mouseX / TILE_SIZE;
                    int gridY = (mouseY - 50) / TILE_SIZE;

                    selectedMonster = null;
                    if (gridX >= 0 && gridX < 12 && gridY >= 0 && gridY < 12) {
                        handleTileClick(tiles[gridY][gridX]);
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
        } else {
            System.out.println("미네랄 부족!");
        }
        repaint();
    }

    private void handleTileClick(Tile tile) {
        if (!tile.isBuildable) {
            if (selectedTile != null) selectedTile.isSelected = false;
            selectedTile = null;
            return;
        }

        if (!tile.hasTower) {
            if (selectedTile == null && gold >= 10) {
                gold -= 10;
                int typeIndex = random.nextInt(types.length);
                tile.setTower(types[typeIndex], typeColors[typeIndex], 1, typeIndex);
            }
        } else {
            if (selectedTile == null) {
                selectedTile = tile;
                tile.isSelected = true;
            } else {
                if (selectedTile != tile &&
                        selectedTile.towerType.equals(tile.towerType) &&
                        selectedTile.tier == tile.tier) {

                    int newTier = tile.tier + 1;
                    int randomTypeIndex = random.nextInt(types.length);

                    tile.setTower(types[randomTypeIndex], typeColors[randomTypeIndex], newTier, randomTypeIndex);
                    selectedTile.clearTower();
                }
                selectedTile.isSelected = false;
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

    // ★ 보스 스폰
    private void spawnBoss() {
        int maxHp = (100 + ((wave - 1) * 80)) * 25; // 일반 몹 체력의 25배
        monsters.add(new Monster("무속성", Color.MAGENTA, maxHp, true));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (life <= 0) return;

        waveTimer--;

        // 웨이브 초기화
        if (waveTimer <= 0) {
            wave++;
            waveTimer = 600;
            spawnedThisWave = 0;
        }

        boolean isBossWave = (wave % 10 == 0); // 10라운드마다 보스

        // 스폰 타이머
        if (waveTimer > 200) {
            spawnCooldown--;
            if (spawnCooldown <= 0) {
                if (isBossWave) {
                    if (spawnedThisWave == 0) { // 보스는 1마리만 스폰
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
                gold += m.isBoss ? 50 : 5; // 보스 보상은 50 미네랄!
                if (selectedMonster == m) selectedMonster = null;
                mobIter.remove();
            } else if (!m.move()) {
                if (selectedMonster == m) selectedMonster = null;
                mobIter.remove();
                life -= m.isBoss ? 5 : 1; // 보스 놓치면 라이프 5칸 깎임
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
                            double multiplier = 1.0;
                            // 무속성(보스)은 모든 속성 데미지를 1.0배로 받음
                            if (!target.element.equals("무속성")) {
                                if (t.towerType.equals("불") && target.element.equals("풀")) multiplier = 1.5;
                                else if (t.towerType.equals("불") && target.element.equals("물")) multiplier = 0.5;
                                else if (t.towerType.equals("물") && target.element.equals("불")) multiplier = 1.5;
                                else if (t.towerType.equals("물") && target.element.equals("풀")) multiplier = 0.5;
                                else if (t.towerType.equals("풀") && target.element.equals("물")) multiplier = 1.5;
                                else if (t.towerType.equals("풀") && target.element.equals("불")) multiplier = 0.5;
                            }

                            int baseDamage = (t.tier * 20) + (upgradeLevels[t.typeIndex] * 10 * t.tier);
                            target.hp -= (int)(baseDamage * multiplier);
                            t.cooldownTimer = 20;

                            lasers.add(new Laser(t.x + 22, t.y + 72, (int)target.centerX, (int)target.centerY, t.color));
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
        double range = 120.0;

        for (Monster m : monsters) {
            double dx = m.centerX - towerCenterX;
            double dy = m.centerY - towerCenterY;
            if (Math.hypot(dx, dy) <= range) return m;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        boolean isBossWave = (wave % 10 == 0);
        String spawnText = (waveTimer > 200) ? (isBossWave ? "보스 출현!" : String.format("%.1f초", spawnCooldown / 20.0)) : "휴식중";

        if (life > 0) {
            if(isBossWave) g.setColor(Color.MAGENTA);
            g.drawString("Wave: " + wave + " | 남은 시간: " + (waveTimer / 20) + "초 | " + spawnText, 10, 20);

            if (monsters.size() >= 80) g.setColor(Color.RED);
            else g.setColor(new Color(200, 200, 255));
            g.drawString("필드 몹: " + monsters.size() + " / " + MAX_FIELD_MONSTERS + " | 라이프: " + life + " | 미네랄: " + gold, 10, 42);
        } else {
            g.setColor(Color.RED);
            g.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            g.drawString("GAME OVER", 200, 35);
        }

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
                    g.setFont(new Font("맑은 고딕", Font.BOLD, 11));
                    g.drawString(t.towerType+t.tier, t.x + 10, t.y + 77);
                }
                if (t.isSelected) {
                    g.setColor(Color.GREEN);
                    g.drawRect(t.x, t.y + 50, TILE_SIZE, TILE_SIZE);
                    g.drawRect(t.x + 1, t.y + 51, TILE_SIZE - 2, TILE_SIZE - 2);
                    g.setColor(new Color(255, 255, 255, 50));
                    g.fillOval(t.x + 22 - 120, t.y + 72 - 120, 240, 240);
                }
            }
        }

        // 몹 & 보스 그리기
        for (Monster m : monsters) {
            g.setColor(m.color);
            int drawX = (int)(m.centerX - m.radius);
            int drawY = (int)(m.centerY - m.radius);
            int size = (int)(m.radius * 2);

            if(m.isBoss) {
                g.fillRect(drawX, drawY, size, size); // 보스는 네모
                g.setColor(Color.WHITE);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                g.drawString("BOSS", drawX + 3, drawY + 23);
            } else {
                g.fillOval(drawX, drawY, size, size); // 일반은 동그라미
            }

            if (m == selectedMonster) {
                g.setColor(Color.YELLOW);
                g.drawRect(drawX - 2, drawY - 2, size + 4, size + 4);
            } else {
                g.setColor(Color.WHITE);
                if(m.isBoss) g.drawRect(drawX, drawY, size, size);
                else g.drawOval(drawX, drawY, size, size);
            }

            // 체력바
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
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(l.startX, l.startY, l.endX, l.endY);
        }

        // ==========================================
        // 하단 상태창 UI (Y: 590 ~ 720)
        // ==========================================
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 590, 540, 130);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 590, 540, 130);

        g.drawRect(10, 600, 240, 100);
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        if (selectedTile != null && selectedTile.hasTower) {
            int currentAtk = (selectedTile.tier * 20) + (upgradeLevels[selectedTile.typeIndex] * 10 * selectedTile.tier);
            g.drawString("선택됨: " + selectedTile.towerType + " 타워 (Lv." + selectedTile.tier + ")", 20, 630);
            g.drawString("공격력: " + currentAtk, 20, 660);
            g.setColor(Color.YELLOW);
            g.drawString("(+업글 보너스: " + (upgradeLevels[selectedTile.typeIndex] * 10 * selectedTile.tier) + ")", 20, 685);
        } else if (selectedMonster != null) {
            String prefix = selectedMonster.isBoss ? "[보스] " : "";
            g.drawString("선택됨: " + prefix + selectedMonster.element, 20, 630);
            g.drawString("체력: " + selectedMonster.hp + " / " + selectedMonster.maxHp, 20, 660);
        } else {
            g.setColor(Color.GRAY);
            g.drawString("타워나 몬스터를 클릭하세요.", 20, 650);
        }

        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(260, 600, 270, 100);
        g.setColor(Color.WHITE);
        g.drawString("공격력 업그레이드 (20 미네랄)", 270, 620);

        String[] btnNames = {"불", "물", "풀"};
        for (int i = 0; i < 3; i++) {
            int bx = 270 + (i * 85);
            g.setColor(typeColors[i]);
            g.fillRect(bx, 630, 75, 40);
            g.setColor(Color.BLACK);
            g.drawRect(bx, 630, 75, 40);

            g.setFont(new Font("맑은 고딕", Font.BOLD, 13));
            g.drawString(btnNames[i] + " Lv." + upgradeLevels[i], bx + 18, 655);
        }
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

        // ★ 유저님이 찍어주신 오리지널 12개 웨이포인트 복구 완료!
        int[][] gridWaypoints = {
                {0, 0}, {0, 4}, {11, 4}, {11, 0}, {6, 0}, {6, 11},
                {11, 11}, {11, 7}, {0, 7}, {0, 11}, {3, 11}, {3, 0}
        };
        int[][] waypoints = new int[12][2];

        public Monster(String element, Color color, int hp, boolean isBoss) {
            this.element = element; this.color = color; this.maxHp = hp; this.hp = hp;
            this.isBoss = isBoss;
            this.radius = isBoss ? 18.0 : 10.0; // 보스는 크게 표시

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
            double speed = isBoss ? 2.5 : 4.0; // 보스는 살짝 느리게

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
        public Laser(int startX, int startY, int endX, int endY, Color color) {
            this.startX = startX; this.startY = startY; this.endX = endX; this.endY = endY; this.color = color;
        }
        public boolean update() { framesLeft--; return framesLeft <= 0; }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("스타 랜타디 - 커스텀 맵 & 보스");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new RandomTowerDefense());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}