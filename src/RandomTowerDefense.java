import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomTowerDefense extends JPanel implements ActionListener {
    private int life = 10;
    private int gold = 100; // 테스트를 위해 초기 미네랄 증가

    private int wave = 1;
    private int waveTimer = 600;
    private int spawnCooldown = 20;
    private final int MAX_FIELD_MONSTERS = 50;

    // 업그레이드 레벨 (0: 불, 1: 물, 2: 풀)
    private int[] upgradeLevels = {0, 0, 0};
    private final int UPGRADE_COST = 20; // 업그레이드 비용

    private final int[][] mapGrid = {
            {0, 0, 0, 0, 0},
            {0, 1, 1, 1, 0},
            {0, 1, 1, 1, 0},
            {0, 1, 1, 1, 0},
            {0, 0, 0, 0, 0}
    };
    private final int TILE_SIZE = 100;

    private final String[] types = {"불", "물", "풀"};
    private final Color[] typeColors = {new Color(255, 100, 100), new Color(100, 150, 255), new Color(100, 255, 100)};

    private Tile[][] tiles = new Tile[5][5];
    private Tile selectedTile = null;
    private Monster selectedMonster = null; // 선택된 몹 확인용

    private List<Monster> monsters = new ArrayList<>();
    private List<Laser> lasers = new ArrayList<>();
    private Timer gameLoop;
    private Random random = new Random();

    public RandomTowerDefense() {
        // 창 높이를 650으로 늘림 (하단 100px은 상태창 UI)
        setPreferredSize(new Dimension(500, 650));
        setBackground(Color.DARK_GRAY);

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                boolean isBuildable = (mapGrid[y][x] == 1);
                tiles[y][x] = new Tile(x * TILE_SIZE, y * TILE_SIZE, isBuildable);
            }
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();

                // 1. 하단 UI 영역 (업그레이드 버튼) 클릭 감지
                if (mouseY >= 550) {
                    if (mouseY >= 580 && mouseY <= 620) {
                        if (mouseX >= 280 && mouseX <= 340) doUpgrade(0); // 불 업글
                        else if (mouseX >= 350 && mouseX <= 410) doUpgrade(1); // 물 업글
                        else if (mouseX >= 420 && mouseX <= 480) doUpgrade(2); // 풀 업글
                    }
                    return;
                }

                // 2. 몬스터 클릭 감지 (타워보다 우선)
                boolean clickedMonster = false;
                for (int i = monsters.size() - 1; i >= 0; i--) {
                    Monster m = monsters.get(i);
                    // 몬스터 중심점과의 거리 계산 (클릭 판정)
                    if (Math.hypot(m.x + 10 - mouseX, m.y + 10 - mouseY) <= 15) {
                        selectedMonster = m;
                        if (selectedTile != null) selectedTile.isSelected = false;
                        selectedTile = null;
                        clickedMonster = true;
                        break;
                    }
                }

                // 3. 맵 타일 클릭 감지
                if (!clickedMonster && mouseY >= 50 && mouseY < 550) {
                    int gridX = mouseX / TILE_SIZE;
                    int gridY = (mouseY - 50) / TILE_SIZE;

                    selectedMonster = null; // 타일 클릭 시 몹 선택 해제
                    if (gridX >= 0 && gridX < 5 && gridY >= 0 && gridY < 5) {
                        handleTileClick(tiles[gridY][gridX]);
                    }
                }
                repaint();
            }
        });

        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    // 업그레이드 처리 로직
    private void doUpgrade(int typeIndex) {
        if (gold >= UPGRADE_COST) {
            gold -= UPGRADE_COST;
            upgradeLevels[typeIndex]++;
        } else {
            System.out.println("미네랄이 부족합니다!");
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

                    tile.tier++;
                    selectedTile.clearTower();
                }
                selectedTile.isSelected = false;
                selectedTile = tile; // 합치든 안 합치든 클릭한 걸 다시 선택
                tile.isSelected = true;
            }
        }
    }

    private void spawnMonster() {
        int typeIndex = random.nextInt(types.length);
        int maxHp = 100 + ((wave - 1) * 80);
        monsters.add(new Monster(20, 70, types[typeIndex], typeColors[typeIndex], maxHp));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (life <= 0) return;

        waveTimer--;
        if (waveTimer <= 0) {
            wave++;
            waveTimer = 600;
        }

        if (waveTimer > 200) {
            spawnCooldown--;
            if (spawnCooldown <= 0) {
                spawnMonster();
                spawnCooldown = 20;
            }
        }

        if (monsters.size() >= MAX_FIELD_MONSTERS) life = 0;

        Iterator<Monster> mobIter = monsters.iterator();
        while (mobIter.hasNext()) {
            Monster m = mobIter.next();
            if (m.hp <= 0) {
                gold += 5;
                if (selectedMonster == m) selectedMonster = null; // 죽으면 선택 해제
                mobIter.remove();
            } else if (!m.move()) {
                if (selectedMonster == m) selectedMonster = null;
                mobIter.remove();
                life--;
            }
        }

        // 타워 공격 로직
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                Tile t = tiles[y][x];
                if (t.hasTower) {
                    if (t.cooldownTimer > 0) t.cooldownTimer--;
                    else {
                        Monster target = findClosestMonster(t);
                        if (target != null) {
                            double multiplier = 1.0;
                            if (t.towerType.equals("불") && target.element.equals("풀")) multiplier = 1.5;
                            else if (t.towerType.equals("불") && target.element.equals("물")) multiplier = 0.5;
                            else if (t.towerType.equals("물") && target.element.equals("불")) multiplier = 1.5;
                            else if (t.towerType.equals("물") && target.element.equals("풀")) multiplier = 0.5;
                            else if (t.towerType.equals("풀") && target.element.equals("물")) multiplier = 1.5;
                            else if (t.towerType.equals("풀") && target.element.equals("불")) multiplier = 0.5;

                            // 데미지 공식: (기본 데미지 20 * 티어) + (업그레이드 수치 * 10 * 티어)
                            int baseDamage = (t.tier * 20) + (upgradeLevels[t.typeIndex] * 10 * t.tier);
                            target.hp -= (int)(baseDamage * multiplier);
                            t.cooldownTimer = 20;
                            lasers.add(new Laser(t.x + 50, t.y + 100, (int)target.x + 10, (int)target.y + 10, t.color));
                        }
                    }
                }
            }
        }

        lasers.removeIf(Laser::update);
        repaint();
    }

    private Monster findClosestMonster(Tile t) {
        int towerCenterX = t.x + 50;
        int towerCenterY = t.y + 100;
        double range = 150.0;

        for (Monster m : monsters) {
            double dx = (m.x + 10) - towerCenterX;
            double dy = (m.y + 10) - towerCenterY;
            if (Math.hypot(dx, dy) <= range) return m;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 1. 상단 UI (기존 동일)
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        String spawnText = (waveTimer > 200) ? String.format("%.1f초", spawnCooldown / 20.0) : "휴식중";

        if (life > 0) {
            g.drawString("Wave: " + wave + " | 남은 시간: " + (waveTimer / 20) + "초 | 몹 스폰: " + spawnText, 10, 20);
            if (monsters.size() >= 40) g.setColor(Color.RED);
            else g.setColor(new Color(200, 200, 255));
            g.drawString("필드 몹: " + monsters.size() + " / " + MAX_FIELD_MONSTERS + " | 라이프: " + life + " | 미네랄: " + gold, 10, 42);
        } else {
            g.setColor(Color.RED);
            g.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            g.drawString("GAME OVER", 180, 35);
        }

        // 2. 맵 및 타워
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                Tile t = tiles[y][x];
                g.setColor(t.isBuildable ? new Color(85, 85, 85) : new Color(34, 34, 34));
                g.fillRect(t.x, t.y + 50, TILE_SIZE, TILE_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(t.x, t.y + 50, TILE_SIZE, TILE_SIZE);

                if (t.hasTower) {
                    g.setColor(t.color);
                    g.fillRect(t.x + 10, t.y + 60, 80, 80);
                    g.setColor(Color.BLACK);
                    g.drawString(t.towerType + " Lv." + t.tier, t.x + 25, t.y + 105);
                }
                if (t.isSelected) {
                    g.setColor(Color.GREEN);
                    g.drawRect(t.x, t.y + 50, TILE_SIZE, TILE_SIZE);
                    g.drawRect(t.x + 1, t.y + 51, TILE_SIZE - 2, TILE_SIZE - 2);
                    g.setColor(new Color(255, 255, 255, 50));
                    g.fillOval(t.x + 50 - 150, t.y + 100 - 150, 300, 300);
                }
            }
        }

        // 3. 몬스터 (선택된 몹은 노란색 테두리로 표시)
        for (Monster m : monsters) {
            g.setColor(m.color);
            g.fillOval((int)m.x, (int)m.y, 20, 20);

            if (m == selectedMonster) {
                g.setColor(Color.YELLOW); // 선택된 몹 강조
                g.drawOval((int)m.x - 2, (int)m.y - 2, 24, 24);
                g.drawOval((int)m.x - 3, (int)m.y - 3, 26, 26);
            } else {
                g.setColor(Color.WHITE);
                g.drawOval((int)m.x, (int)m.y, 20, 20);
            }

            g.setColor(Color.RED);
            g.fillRect((int)m.x - 5, (int)m.y - 10, 30, 5);
            g.setColor(Color.GREEN);
            int hpWidth = (int)((m.hp / (double)m.maxHp) * 30);
            if (hpWidth < 0) hpWidth = 0;
            g.fillRect((int)m.x - 5, (int)m.y - 10, hpWidth, 5);
        }

        for (Laser l : lasers) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(l.color);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(l.startX, l.startY, l.endX, l.endY);
        }

        // ==========================================
        // 4. 하단 상태창 UI (Y: 550 ~ 650)
        // ==========================================
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 550, 500, 100);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 550, 500, 100); // 상태창 테두리

        // 4-1. 왼쪽: 타워/몹 스탯 정보창
        g.drawRect(10, 560, 250, 80);
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 15));

        if (selectedTile != null && selectedTile.hasTower) {
            int currentAtk = (selectedTile.tier * 20) + (upgradeLevels[selectedTile.typeIndex] * 10 * selectedTile.tier);
            g.drawString("선택됨: " + selectedTile.towerType + " 타워 (Lv." + selectedTile.tier + ")", 20, 585);
            g.drawString("공격력: " + currentAtk, 20, 610);
            g.setColor(Color.YELLOW);
            g.drawString("(+업글 보너스: " + (upgradeLevels[selectedTile.typeIndex] * 10 * selectedTile.tier) + ")", 20, 630);
        } else if (selectedMonster != null) {
            g.drawString("선택됨: " + selectedMonster.element + " 속성 몬스터", 20, 585);
            g.drawString("체력: " + selectedMonster.hp + " / " + selectedMonster.maxHp, 20, 610);
        } else {
            g.setColor(Color.GRAY);
            g.drawString("타워나 몬스터를 클릭하세요.", 20, 605);
        }

        // 4-2. 오른쪽: 글로벌 업그레이드 구역
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(270, 560, 220, 80);
        g.setColor(Color.WHITE);
        g.drawString("공격력 업그레이드 (20원)", 280, 575);

        // 버튼(모양) 그리기
        String[] btnNames = {"불", "물", "풀"};
        for (int i = 0; i < 3; i++) {
            int bx = 280 + (i * 70);
            g.setColor(typeColors[i]);
            g.fillRect(bx, 580, 60, 40); // 60x40 버튼
            g.setColor(Color.BLACK);
            g.drawRect(bx, 580, 60, 40);

            // 버튼 텍스트 (Lv 표시)
            g.setFont(new Font("맑은 고딕", Font.BOLD, 13));
            g.drawString(btnNames[i] + " Lv." + upgradeLevels[i], bx + 8, 605);
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
        double x, y;
        int targetWaypoint = 1, maxHp, hp;
        String element;
        Color color;
        int[][] waypoints = {{40, 90}, {440, 90}, {440, 490}, {40, 490}};
        public Monster(double x, double y, String element, Color color, int hp) {
            this.x = x; this.y = y; this.element = element; this.color = color; this.maxHp = hp; this.hp = hp;
        }
        public boolean move() {
            if (targetWaypoint >= waypoints.length) return false;
            int targetX = waypoints[targetWaypoint][0], targetY = waypoints[targetWaypoint][1];
            double speed = 4.0;
            double dx = targetX - x, dy = targetY - y;
            double distance = Math.hypot(dx, dy);
            if (distance <= speed) targetWaypoint++;
            else { x += (dx / distance) * speed; y += (dy / distance) * speed; }
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
        JFrame frame = new JFrame("스타 랜타디 - UI 및 업그레이드");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new RandomTowerDefense());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}