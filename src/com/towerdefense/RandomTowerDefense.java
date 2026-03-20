package com.towerdefense;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import static com.towerdefense.GameConstants.*;

public class RandomTowerDefense extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    private final GameContext ctx = new GameContext();
    private final AssetManager assetManager = new AssetManager();
    
    private final QuestManager questManager = new QuestManager(ctx);
    private final MonsterManager monsterManager = new MonsterManager(ctx);
    private final TowerManager towerManager = new TowerManager(ctx, questManager);
    private final WaveManager waveManager = new WaveManager(ctx, monsterManager);
    
    private final GameRenderer renderer = new GameRenderer(ctx, assetManager, towerManager);
    private final Timer gameLoop;

    private boolean showQuestUI = false;
    private boolean showJobSelectUI = true;
    private Job hoveredJob = null;

    private final Rectangle speedButton = new Rectangle(448, 12, 78, 30);
    private final Rectangle questButton = new Rectangle(360, 12, 78, 30);
    private final Rectangle closeQuestBtn = new Rectangle(PANEL_W / 2 - 60, PANEL_H - 120, 120, 40);
    private final Rectangle sellButton = new Rectangle(172, 656, 70, 34);
    private final Rectangle restartButton = new Rectangle(PANEL_W / 2 - 80, PANEL_H / 2 + 60, 160, 45);
    private final Rectangle[] jobButtons = {
            new Rectangle(15, 215, 160, 290),
            new Rectangle(190, 215, 160, 290),
            new Rectangle(365, 215, 160, 290)
    };
    private final Rectangle[] upgradeButtons = {
            new Rectangle(260, 664, 62, 40),
            new Rectangle(326, 664, 62, 40),
            new Rectangle(392, 664, 62, 40),
            new Rectangle(458, 664, 66, 40)
    };

    public RandomTowerDefense() {
        setPreferredSize(new Dimension(PANEL_W, PANEL_H));
        setBackground(new Color(13, 18, 28));
        setDoubleBuffered(true);

        resetGame();
        assetManager.loadAssets();
        
        addMouseListener(this);
        addMouseMotionListener(this);

        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    private void resetGame() {
        ctx.reset();
        questManager.initQuests();
        showQuestUI = false;
        showJobSelectUI = true;
        
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                ctx.tiles[y][x] = new Tile(ctx.mapGrid[y][x] == 1);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (showJobSelectUI) {
            ctx.updatePresentationTimers();
            repaint();
            return;
        }
        if (ctx.life <= 0 || ctx.gameWon) return;

        ctx.tick++;
        ctx.updatePresentationTimers();

        waveManager.update();
        monsterManager.update();
        updateTowerAttacks();

        ctx.lasers.removeIf(l -> --l.frames <= 0);
        repaint();
    }

    private void updateTowerAttacks() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                Tile tile = ctx.tiles[y][x];
                if (!tile.hasTower()) continue;
                
                Tower tower = tile.tower;
                if (tower.cooldown > 0) {
                    tower.cooldown--;
                    continue;
                }

                Monster target = monsterManager.findClosestMonster(x, y);
                if (target == null) continue;

                double damage = towerManager.calculateDamage(tower, target);
                target.hp -= (int) damage;
                tower.cooldown = 20;
                
                ctx.lasers.add(new Laser(
                    x * TILE_SIZE + GRID_ORIGIN_X + 22, 
                    y * TILE_SIZE + GRID_ORIGIN_Y + 22, 
                    (int)target.x, (int)target.y, 
                    3, tower.color, tower.tier == 4
                ));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.paint((Graphics2D) g, getWidth(), getHeight(), showQuestUI, showJobSelectUI, hoveredJob, jobButtons, closeQuestBtn, sellButton, restartButton, upgradeButtons);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        if (showJobSelectUI) {
            for (Job job : Job.values()) {
                if (getJobImageRect(job).contains(mx, my)) {
                    ctx.selectedJob = job;
                    showJobSelectUI = false;
                    ctx.showToast("직업 선택: " + job.label + " (" + job.summary + ")", 70);
                    return;
                }
            }
            return;
        }

        if (ctx.life <= 0 || ctx.gameWon) {
            if (restartButton.contains(mx, my)) resetGame();
            return;
        }

        if (showQuestUI) {
            if (closeQuestBtn.contains(mx, my)) showQuestUI = false;
            return;
        }

        if (questButton.contains(mx, my)) { showQuestUI = true; repaint(); return; }
        
        if (speedButton.contains(mx, my)) {
            ctx.currentSpeed = (ctx.currentSpeed % 3) + 1;
            gameLoop.setDelay(ctx.currentSpeed == 1 ? 50 : (ctx.currentSpeed == 2 ? 25 : 16));
            repaint();
            return;
        }

        if (my >= BOTTOM_PANEL_Y) {
            handleBottomPanelClick(mx, my);
        } else {
            handleMapClick(mx, my);
        }
        repaint();
    }

    private void handleBottomPanelClick(int mx, int my) {
        if (ctx.selectedTileX >= 0 && sellButton.contains(mx, my)) {
            towerManager.sellTower(ctx.selectedTileX, ctx.selectedTileY);
            ctx.selectedTileX = -1;
        }
        for (int i = 0; i < 3; i++) if (upgradeButtons[i].contains(mx, my)) towerManager.upgradeNormal(i);
        if (upgradeButtons[3].contains(mx, my)) towerManager.upgradeHidden();
    }

    private void handleMapClick(int mx, int my) {
        boolean clickedMonster = false;
        for (int i = ctx.monsters.size() - 1; i >= 0; i--) {
            Monster m = ctx.monsters.get(i);
            if (Math.hypot(m.x - mx, m.y - my) <= m.radius + 6) {
                ctx.selectedMonster = m;
                ctx.selectedTileX = -1;
                clickedMonster = true;
                break;
            }
        }

        if (!clickedMonster) {
            int gx = (mx - GRID_ORIGIN_X) / TILE_SIZE;
            int gy = (my - GRID_ORIGIN_Y) / TILE_SIZE;
            if (gx >= 0 && gx < GRID_SIZE && gy >= 0 && gy < GRID_SIZE) {
                ctx.selectedMonster = null;
                Tile tile = ctx.tiles[gy][gx];
                if (!tile.buildable) { ctx.selectedTileX = -1; return; }

                if (!tile.hasTower()) {
                    if (ctx.selectedTileX != -1) { ctx.selectedTileX = -1; }
                    else towerManager.buildTower(gx, gy);
                } else {
                    if (ctx.selectedTileX == -1) {
                        ctx.selectedTileX = gx; ctx.selectedTileY = gy;
                    } else if (ctx.selectedTileX == gx && ctx.selectedTileY == gy) {
                        ctx.selectedTileX = -1;
                    } else {
                        towerManager.mergeTowers(ctx.selectedTileX, ctx.selectedTileY, gx, gy);
                        ctx.selectedTileX = -1;
                    }
                }
            } else {
                ctx.selectedTileX = -1;
            }
        }
    }

    private Rectangle getJobImageRect(Job job) {
        Rectangle card = jobButtons[job.index];
        return new Rectangle(card.x + 12, card.y + 12, card.width - 24, 180);
    }

    @Override public void mouseMoved(MouseEvent e) {
        if (showJobSelectUI) {
            hoveredJob = null;
            for (Job job : Job.values()) {
                if (getJobImageRect(job).contains(e.getX(), e.getY())) { hoveredJob = job; break; }
            }
            repaint();
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("랜덤 타워 디펜스 - 리팩토링 버전");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new RandomTowerDefense());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
