import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomTowerDefense extends JPanel implements ActionListener {
    private int life = 10;
    private int gold = 50;

    // 웨이브 및 자동 스폰 관련 변수
    private int wave = 1;
    private int waveTimer = 600; // 1라운드 = 30초 (50ms * 600틱)
    private int spawnCooldown = 20; // 스폰 간격 = 1초 (50ms * 20틱)
    private final int MAX_FIELD_MONSTERS = 50; // 랜타디 패배 조건 (필드 몹 제한)

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

    private List<Monster> monsters = new ArrayList<>();
    private List<Laser> lasers = new ArrayList<>();
    private Timer gameLoop;
    private Random random = new Random();

    public RandomTowerDefense() {
        setPreferredSize(new Dimension(500, 550));
        setBackground(Color.DARK_GRAY);

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                boolean isBuildable = (mapGrid[y][x] == 1);
                tiles[y][x] = new Tile(x * TILE_SIZE, y * TILE_SIZE, isBuildable);
            }
        }

        // 수동 몹 소환 버튼 삭제, 타워 건설/조합만 남김
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int gridX = e.getX() / TILE_SIZE;
                int gridY = (e.getY() - 50) / TILE_SIZE; // 상단 UI 여백 50 제외

                if (gridX >= 0 && gridX < 5 && gridY >= 0 && gridY < 5) {
                    handleTileClick(tiles[gridY][gridX]);
                }
            }
        });

        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    private void handleTileClick(Tile tile) {
        if (!tile.isBuildable) return;

        if (!tile.hasTower) {
            if (selectedTile == null && gold >= 10) {
                gold -= 10;
                int typeIndex = random.nextInt(types.length);
                tile.setTower(types[typeIndex], typeColors[typeIndex], 1);
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
                selectedTile = null;
            }
        }
        repaint();
    }

    private void spawnMonster() {
        int typeIndex = random.nextInt(types.length);
        String mobElement = types[typeIndex];
        Color mobColor = typeColors[typeIndex];

        // 웨이브마다 몹의 체력이 기하급수적으로 증가
        int maxHp = 100 + ((wave - 1) * 80);
        monsters.add(new Monster(20, 70, mobElement, mobColor, maxHp));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (life <= 0) return; // 게임 오버 시 멈춤

        // 1. 자동 웨이브 및 스폰 타이머 관리
        waveTimer--;
        if (waveTimer <= 0) {
            wave++;
            waveTimer = 600; // 30초 리셋
        }

        // 라운드 30초 중 앞의 20초(400틱) 동안만 몹을 소환하고, 뒤의 10초는 다음 웨이브 대기
        if (waveTimer > 200) {
            spawnCooldown--;
            if (spawnCooldown <= 0) {
                spawnMonster();
                spawnCooldown = 20; // 1초(20틱) 후 다시 소환
            }
        }

        // 2. 패배 조건 체크 (필드 몹 초과)
        if (monsters.size() >= MAX_FIELD_MONSTERS) {
            life = 0;
            System.out.println("필드 몹 마리 수 초과! 게임 오버!");
        }

        // 3. 몹 이동 및 사망 처리
        Iterator<Monster> mobIter = monsters.iterator();
        while (mobIter.hasNext()) {
            Monster m = mobIter.next();
            if (m.hp <= 0) {
                gold += 5;
                mobIter.remove();
            } else if (!m.move()) {
                mobIter.remove();
                life--;
            }
        }

        // 4. 타워 공격 로직 및 상성 계산
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                Tile t = tiles[y][x];
                if (t.hasTower) {
                    if (t.cooldownTimer > 0) {
                        t.cooldownTimer--;
                    } else {
                        Monster target = findClosestMonster(t);
                        if (target != null) {
                            double multiplier = 1.0;
                            String atk = t.towerType;
                            String def = target.element;

                            if (atk.equals("불") && def.equals("풀")) multiplier = 1.5;
                            else if (atk.equals("불") && def.equals("물")) multiplier = 0.5;
                            else if (atk.equals("물") && def.equals("불")) multiplier = 1.5;
                            else if (atk.equals("물") && def.equals("풀")) multiplier = 0.5;
                            else if (atk.equals("풀") && def.equals("물")) multiplier = 1.5;
                            else if (atk.equals("풀") && def.equals("불")) multiplier = 0.5;

                            int baseDamage = t.tier * 20;
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
            int mobCenterX = (int)m.x + 10;
            int mobCenterY = (int)m.y + 10;
            double dx = mobCenterX - towerCenterX;
            double dy = mobCenterY - towerCenterY;
            if (Math.sqrt(dx * dx + dy * dy) <= range) return m;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 상단 UI 업데이트 (두 줄로 나누어 더 많은 정보 표시)
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        // 스폰 남은 시간 계산 (대기 시간 중이면 "대기중" 출력)
        String spawnText = (waveTimer > 200) ? String.format("%.1f초", spawnCooldown / 20.0) : "휴식중";

        if (life > 0) {
            g.drawString("Wave: " + wave + " | 라운드 남은 시간: " + (waveTimer / 20) + "초 | 몹 스폰: " + spawnText, 10, 20);

            // 필드 몹 마리수가 위험해지면 빨간색으로 경고
            if (monsters.size() >= 40) g.setColor(Color.RED);
            else g.setColor(new Color(200, 200, 255)); // 옅은 파란색
            g.drawString("필드 몹: " + monsters.size() + " / " + MAX_FIELD_MONSTERS + " 마리 | 라이프: " + life + " | 미네랄: " + gold, 10, 42);
        } else {
            g.setColor(Color.RED);
            g.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            g.drawString("GAME OVER", 180, 35);
        }

        // 맵 및 타워 그리기 (이하 동일)
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

        for (Monster m : monsters) {
            g.setColor(m.color);
            g.fillOval((int)m.x, (int)m.y, 20, 20);
            g.setColor(Color.WHITE);
            g.drawOval((int)m.x, (int)m.y, 20, 20);
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
    }

    class Tile {
        int x, y;
        boolean isBuildable, hasTower = false, isSelected = false;
        String towerType = "";
        Color color = null;
        int tier = 0, cooldownTimer = 0;
        public Tile(int x, int y, boolean isBuildable) { this.x = x; this.y = y; this.isBuildable = isBuildable; }
        public void setTower(String type, Color color, int tier) { this.hasTower = true; this.towerType = type; this.color = color; this.tier = tier; this.cooldownTimer = 0; }
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
            double distance = Math.sqrt(dx * dx + dy * dy);
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
        JFrame frame = new JFrame("스타 랜타디 - 자동 웨이브 시스템");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new RandomTowerDefense());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}