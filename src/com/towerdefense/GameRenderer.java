package com.towerdefense;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static com.towerdefense.GameConstants.*;

public class GameRenderer {
    private final GameContext ctx;
    private final AssetManager assetManager;
    private final TowerManager towerManager;

    public GameRenderer(GameContext ctx, AssetManager assetManager, TowerManager towerManager) {
        this.ctx = ctx;
        this.assetManager = assetManager;
        this.towerManager = towerManager;
    }

    public void paint(Graphics2D g2, int panelW, int panelH, boolean showQuestUI, boolean showJobSelectUI, Job hoveredJob, Rectangle[] jobButtons, Rectangle closeQuestBtn, Rectangle sellButton, Rectangle restartButton, Rectangle[] upgradeButtons) {
        paintBackground(g2, panelW, panelH);
        paintTopHud(g2);
        paintPath(g2);
        paintGrid(g2);
        paintMonsters(g2);
        paintLasers(g2);
        paintBottomPanel(g2, sellButton, upgradeButtons);

        if (ctx.toastTimer > 0) {
            drawToast(g2, ctx.toastMsg, panelW);
        }

        if (showQuestUI) {
            paintQuestOverlay(g2, panelW, panelH, closeQuestBtn);
        }

        if (ctx.life <= 0 || ctx.gameWon) {
            paintGameOverOverlay(g2, panelW, panelH, restartButton);
        }

        if (showJobSelectUI) {
            paintJobSelectionOverlay(g2, panelW, panelH, jobButtons, hoveredJob);
        }
    }

    private void paintBackground(Graphics2D g2, int w, int h) {
        g2.setPaint(new GradientPaint(0, 0, new Color(18, 25, 39), 0, h, new Color(10, 14, 23)));
        g2.fillRect(0, 0, w, h);
        if (assetManager.bgTexture != null) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            g2.drawImage(assetManager.bgTexture, 0, 0, w, h, null);
            g2.setComposite(AlphaComposite.SrcOver);
        }
        g2.setColor(new Color(255, 255, 255, 20));
        for (int i = 0; i < 36; i++) {
            int x = (i * 71 + 19) % w;
            int y = (i * 29 + ctx.tick * 2) % h;
            g2.fillOval(x, y, 2, 2);
        }
    }

    private void paintTopHud(Graphics2D g2) {
        paintCard(g2, 8, 8, 524, TOP_PANEL_H, new Color(18, 26, 38), new Color(78, 98, 126));
        boolean isBossWave = ctx.wave % 10 == 0;
        String status = ctx.waveTimer > 200 ? (isBossWave ? "보스 출현 임박" : String.format("스폰 간격 %.1f초", ctx.spawnCooldown / 20.0)) : "배치 시간";

        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        g2.setColor(new Color(236, 241, 250));
        g2.drawString("웨이브 " + ctx.wave + " / " + MAX_WAVE + " | 시간 " + (ctx.waveTimer / 20) + "초 | " + status, 18, 28);
        g2.setColor(ctx.monsters.size() >= 80 ? new Color(255, 115, 115) : new Color(165, 207, 255));
        g2.drawString("필드 " + ctx.monsters.size() + "/" + MAX_FIELD_MONSTERS + " | 생명 " + ctx.life + " | 골드 " + ctx.gold + " | 직업 " + (ctx.selectedJob == null ? "미선택" : ctx.selectedJob.label), 18, 45);

        paintRoundedButton(g2, new Rectangle(360, 12, 78, 30), new Color(100, 150, 100), "퀘스트");
        paintRoundedButton(g2, new Rectangle(448, 12, 78, 30), ctx.currentSpeed == 1 ? new Color(70, 92, 118) : new Color(228, 84, 84), "속도 x" + ctx.currentSpeed);

        float waveProgress = Math.max(0f, Math.min(1f, (600 - ctx.waveTimer) / 600f));
        g2.setColor(new Color(22, 31, 47));
        g2.fillRoundRect(8, TOP_PANEL_H + 14, 524, 12, 8, 8);
        g2.setPaint(new GradientPaint(8, TOP_PANEL_H + 14, new Color(80, 184, 255), 532, TOP_PANEL_H + 26, new Color(110, 246, 178)));
        g2.fillRoundRect(8, TOP_PANEL_H + 14, (int) (524 * waveProgress), 12, 8, 8);
    }

    private void paintPath(Graphics2D g2) {
        boolean isBossWave = ctx.wave % 10 == 0;
        int alpha = isBossWave ? (int) (130 + (0.5 + 0.5 * Math.sin(ctx.tick * 0.1)) * 60) : 120;
        g2.setColor(isBossWave ? new Color(255, 130, 230, alpha) : new Color(94, 118, 148, alpha));
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < ctx.waypoints.length - 1; i++) {
            g2.drawLine(ctx.waypoints[i][0], ctx.waypoints[i][1], ctx.waypoints[i + 1][0], ctx.waypoints[i + 1][1]);
        }
    }

    private void paintGrid(Graphics2D g2) {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int drawX = GRID_ORIGIN_X + x * TILE_SIZE;
                int drawY = GRID_ORIGIN_Y + y * TILE_SIZE;
                Tile tile = ctx.tiles[y][x];
                if (tile.buildable) g2.drawImage(assetManager.buildTileTexture, drawX - 1, drawY - 1, TILE_SIZE + 2, TILE_SIZE + 2, null);
                else drawPathTile(g2, x, y, drawX, drawY);

                if (tile.hasTower()) drawTower(g2, x, y, drawX, drawY);
                if (tile.selected || (ctx.selectedTileX == x && ctx.selectedTileY == y)) {
                    g2.setColor(new Color(131, 252, 176));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(drawX + 1, drawY + 1, TILE_SIZE - 2, TILE_SIZE - 2, 8, 8);
                    
                    if (tile.hasTower()) {
                        int range = (int) (tile.tower.tier == 4 ? 200 + ctx.hiddenUpgradeLevel * 20 : 120 + ctx.upgradeLevels[tile.tower.typeIndex] * 15);
                        g2.setColor(new Color(170, 240, 210, 45));
                        g2.fillOval(drawX + 22 - range, drawY + 22 - range, range * 2, range * 2);
                    }
                }
            }
        }
    }

    private void drawPathTile(Graphics2D g2, int gridX, int gridY, int drawX, int drawY) {
        boolean up = isPathTile(gridX, gridY - 1), right = isPathTile(gridX + 1, gridY), down = isPathTile(gridX, gridY + 1), left = isPathTile(gridX - 1, gridY);
        int connected = (up ? 1 : 0) + (right ? 1 : 0) + (down ? 1 : 0) + (left ? 1 : 0);
        BufferedImage tex = assetManager.pathStraightTexture;
        double rot = 0.0;
        if (connected >= 3) tex = assetManager.pathCrossTexture;
        else if (connected == 2) {
            if ((up && down) || (left && right)) { tex = assetManager.pathStraightTexture; rot = (left && right) ? Math.PI / 2.0 : 0.0; }
            else { tex = assetManager.pathCurveTexture; rot = getCurveRotation(up, right, down, left); }
        } else if (connected == 1) { tex = assetManager.pathStraightTexture; rot = (left || right) ? Math.PI / 2.0 : 0.0; }
        drawRotatedTile(g2, tex, drawX, drawY, rot);
    }

    private boolean isPathTile(int x, int y) { return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE && !ctx.tiles[y][x].buildable; }
    private double getCurveRotation(boolean u, boolean r, boolean d, boolean l) { if (d && l) return 0.0; if (l && u) return Math.PI / 2.0; if (u && r) return Math.PI; if (r && d) return -Math.PI / 2.0; return 0.0; }

    private void drawRotatedTile(Graphics2D g2, BufferedImage texture, int drawX, int drawY, double rotation) {
        if (texture == null) return;
        AffineTransform old = g2.getTransform();
        AffineTransform tx = new AffineTransform();
        tx.translate(drawX + TILE_SIZE / 2.0, drawY + TILE_SIZE / 2.0);
        tx.rotate(rotation);
        double s = (TILE_SIZE + 2.0) / TILE_SIZE;
        tx.scale(s, s);
        tx.translate(-TILE_SIZE / 2.0, -TILE_SIZE / 2.0);
        g2.drawImage(texture, tx, null);
        g2.setTransform(old);
    }

    private void drawTower(Graphics2D g2, int gx, int gy, int dx, int dy) {
        Tower t = ctx.tiles[gy][gx].tower;
        int idx = getTowerSpriteIndex(t.type);
        g2.setColor(new Color(18, 26, 38)); g2.fillRoundRect(dx + 6, dy + 6, 33, 33, 10, 10);
        BufferedImage sprite = assetManager.towerSprites[idx];
        if (sprite != null) g2.drawImage(sprite, dx + 6, dy + 6, 33, 33, null);
        if (t.tier >= 3) { g2.setColor(new Color(255, 235, 150, 70)); g2.fillOval(dx + 1, dy + 1, 43, 43); }
        drawTowerTierBadge(g2, dx, dy, t.tier, t.color);
    }

    private void drawTowerTierBadge(Graphics2D g2, int dx, int dy, int tier, Color col) {
        int sz = 16, bx = dx + TILE_SIZE - sz - 2, by = dy + 2;
        g2.setColor(col != null ? col : new Color(60, 76, 98)); g2.fillOval(bx, by, sz, sz);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        drawCentered(g2, String.valueOf(tier), bx + sz / 2, by + 12);
    }

    private int getTowerSpriteIndex(String type) {
        if ("Fire".equals(type)) return 0; if ("Water".equals(type)) return 1; if ("Nature".equals(type)) return 2;
        if ("Arcane".equals(type)) return 3; if ("Shadow".equals(type)) return 4; if ("Chaos".equals(type)) return 5;
        return 0;
    }

    private void paintMonsters(Graphics2D g2) {
        for (Monster m : ctx.monsters) {
            int sz = (int) Math.round(m.radius * 2.0 * (m.isBoss ? MONSTER_SCALE_BOSS : MONSTER_SCALE_NORMAL));
            int dx = (int) Math.round(m.x - sz / 2.0), dy = (int) Math.round(m.y - sz / 2.0);
            BufferedImage s = assetManager.monsterDirectionalSprites[m.isBoss ? 3 : ("Fire".equals(m.element) ? 0 : ("Water".equals(m.element) ? 1 : 2))][m.facing];
            if (s != null) { if (m.mirror) g2.drawImage(s, dx + sz, dy, -sz, sz, null); else g2.drawImage(s, dx, dy, sz, sz, null); }
            else { g2.setColor(m.color); if (m.isBoss) g2.fillRoundRect(dx, dy, sz, sz, 9, 9); else g2.fillOval(dx, dy, sz, sz); }
            if (m == ctx.selectedMonster) { g2.setColor(Color.YELLOW); g2.drawRoundRect(dx - 2, dy - 2, sz + 4, sz + 4, 8, 8); }
            g2.setColor(Color.RED); g2.fillRect(dx, dy - 10, sz, 5);
            g2.setColor(Color.GREEN); g2.fillRect(dx, dy - 10, (int)(sz * (m.hp / (double)m.maxHp)), 5);
        }
    }

    private void paintLasers(Graphics2D g2) {
        for (Laser l : ctx.lasers) { g2.setColor(l.color); g2.setStroke(new BasicStroke(l.heavy ? 7f : 3f)); g2.drawLine(l.startX, l.startY, l.endX, l.endY); }
    }

    private void paintBottomPanel(Graphics2D g2, Rectangle sellBtn, Rectangle[] upgBtns) {
        paintCard(g2, 6, BOTTOM_PANEL_Y - 2, 528, 132, new Color(16, 22, 34, 235), new Color(87, 103, 132));
        paintCard(g2, 10, BOTTOM_PANEL_Y + 6, 240, 112, new Color(27, 36, 52), new Color(102, 122, 155));
        paintCard(g2, 258, BOTTOM_PANEL_Y + 6, 272, 112, new Color(27, 36, 52), new Color(102, 122, 155));

        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        g2.setColor(new Color(236, 241, 250));
        g2.drawString("타워 비용: 10 골드", 20, BOTTOM_PANEL_Y + 24);

        if (ctx.selectedTileX >= 0 && ctx.tiles[ctx.selectedTileY][ctx.selectedTileX].hasTower()) {
            Tower t = ctx.tiles[ctx.selectedTileY][ctx.selectedTileX].tower;
            drawTowerInfo(g2, t);
            paintRoundedButton(g2, sellBtn, new Color(215, 74, 74), "판매 +" + t.tier * 5);
        } else if (ctx.selectedMonster != null) {
            g2.setColor(new Color(235, 240, 250));
            g2.drawString("선택: [보스] " + toKoreanLabel(ctx.selectedMonster.element), 20, BOTTOM_PANEL_Y + 80);
            g2.drawString("HP " + ctx.selectedMonster.hp + " / " + ctx.selectedMonster.maxHp, 20, BOTTOM_PANEL_Y + 100);
        }

        for (int i = 0; i < 3; i++) {
            paintTwoLineButton(g2, upgBtns[i], ELEMENT_COLORS[i], ELEMENT_LABELS[i] + " Lv." + ctx.upgradeLevels[i], "(" + towerManager.getUpgCost(i) + "G)");
        }
        paintTwoLineButton(g2, upgBtns[3], new Color(148, 72, 210), "스페셜 Lv." + ctx.hiddenUpgradeLevel, "(" + towerManager.getHiddenUpgCost() + "G)");
    }

    private void drawTowerInfo(Graphics2D g2, Tower tower) {
        String towerLabel = toKoreanLabel(tower.type);
        g2.setColor(new Color(235, 240, 250));
        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 12));

        if (tower.tier == 4) {
            double atkMultiplier = 1.0 + (ctx.hiddenUpgradeLevel * 0.4);
            if (ctx.selectedJob == Job.MAGITECH_ENGINEER) atkMultiplier *= (1.0 + ENGINEER_DAMAGE_BONUS_RATE);
            int atk = (int) (800 * atkMultiplier);
            int range = 200 + (ctx.hiddenUpgradeLevel * 20);
            g2.setColor(new Color(255, 232, 135));
            g2.drawString("선택: " + towerLabel + " 스페셜 타워", 20, BOTTOM_PANEL_Y + 76);
            g2.setColor(Color.WHITE);
            g2.drawString("공격력 " + atk + " | 사거리 " + range, 20, BOTTOM_PANEL_Y + 96);
        } else {
            int baseAtk = tower.tier == 1 ? 25 : (tower.tier == 2 ? 60 : 150);
            double atkMultiplier = 1.0 + (ctx.upgradeLevels[tower.typeIndex] * 0.3);
            if (ctx.selectedJob == Job.MAGITECH_ENGINEER) atkMultiplier *= (1.0 + ENGINEER_DAMAGE_BONUS_RATE);
            int atk = (int) (baseAtk * atkMultiplier);
            int range = 120 + (ctx.upgradeLevels[tower.typeIndex] * 15);
            g2.drawString("선택: " + towerLabel + " 타워 (티어 " + tower.tier + ")", 20, BOTTOM_PANEL_Y + 76);
            g2.drawString("공격력 " + atk + " | 사거리 " + range, 20, BOTTOM_PANEL_Y + 96);
        }
    }

    private void drawToast(Graphics2D g2, String msg, int w) {
        g2.setColor(new Color(0, 0, 0, 180)); g2.fillRoundRect(w / 2 - 140, 80, 280, 40, 20, 20);
        g2.setColor(Color.YELLOW); drawCentered(g2, msg, w / 2, 105);
    }

    private void paintQuestOverlay(Graphics2D g2, int w, int h, Rectangle closeBtn) {
        g2.setColor(new Color(10, 14, 23, 200)); g2.fillRect(0, 0, w, h);
        paintCard(g2, (w - 340) / 2, (h - 420) / 2, 340, 420, new Color(28, 36, 50), new Color(102, 122, 155));
        int ly = (h - 420) / 2 + 80;
        for (Quest q : ctx.questList) {
            g2.setColor(q.completed ? Color.GREEN : Color.LIGHT_GRAY);
            g2.drawString((q.completed ? "[완료] " : "[진행] ") + q.name, (w - 340) / 2 + 20, ly);
            ly += 50;
        }
        paintRoundedButton(g2, closeBtn, Color.RED, "닫기");
    }

    private void paintJobSelectionOverlay(Graphics2D g2, int w, int h, Rectangle[] jobBtns, Job hovered) {
        g2.setColor(new Color(8, 10, 15, 210)); g2.fillRect(0, 0, w, h);
        paintCard(g2, (w - 534) / 2, 120, 534, 470, new Color(23, 31, 46), new Color(95, 112, 140));
        g2.setColor(Color.WHITE); g2.setFont(new Font("Malgun Gothic", Font.BOLD, 22)); drawCentered(g2, "직업 선택", w / 2, 164);
        g2.setFont(new Font("Malgun Gothic", Font.PLAIN, 12)); g2.setColor(new Color(177, 191, 213)); drawCentered(g2, "이미지를 클릭하면 직업이 선택됩니다.", w / 2, 188);

        for (Job j : Job.values()) {
            Rectangle r = jobBtns[j.index]; Rectangle ir = getJobImageRect(r);
            boolean d = hovered != null && hovered != j;
            g2.setColor(new Color(18, 25, 38)); g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            g2.setColor(d ? Color.DARK_GRAY : j.accentColor); g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            BufferedImage img = d ? assetManager.jobImagesGray[j.index] : assetManager.jobImages[j.index];
            if (img != null) g2.drawImage(img, ir.x, ir.y, ir.width, ir.height, null);
            else { g2.setColor(Color.GRAY); g2.fillRect(ir.x, ir.y, ir.width, ir.height); }
            g2.setColor(Color.WHITE); g2.setFont(new Font("Malgun Gothic", Font.BOLD, 18)); drawCentered(g2, j.label, r.x + r.width / 2, r.y + 216);
            g2.setColor(d ? Color.GRAY : new Color(190, 202, 223)); g2.setFont(new Font("Malgun Gothic", Font.PLAIN, 12)); drawCentered(g2, j.summary, r.x + r.width / 2, r.y + 238);
        }
    }

    private void paintGameOverOverlay(Graphics2D g2, int w, int h, Rectangle restartBtn) {
        g2.setColor(new Color(8, 10, 15, 210)); g2.fillRect(0, 0, w, h);
        g2.setColor(ctx.gameWon ? Color.YELLOW : Color.RED); g2.setFont(new Font("Malgun Gothic", Font.BOLD, 42));
        drawCentered(g2, ctx.gameWon ? "VICTORY" : "GAME OVER", w / 2, h / 2 - 20);
        paintRoundedButton(g2, restartBtn, new Color(78, 112, 136), "다시 시작");
    }

    private Rectangle getJobImageRect(Rectangle r) { return new Rectangle(r.x + 12, r.y + 12, r.width - 24, 180); }
    private void paintCard(Graphics2D g2, int x, int y, int w, int h, Color f, Color b) { g2.setColor(f); g2.fillRoundRect(x, y, w, h, 14, 14); g2.setColor(b); g2.drawRoundRect(x, y, w, h, 14, 14); }
    private void paintRoundedButton(Graphics2D g2, Rectangle r, Color f, String l) { g2.setColor(f); g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10); g2.setColor(Color.WHITE); g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10); drawCentered(g2, l, r.x + r.width / 2, r.y + r.height / 2 + 5); }
    private void paintTwoLineButton(Graphics2D g2, Rectangle r, Color f, String l1, String l2) { g2.setColor(f); g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10); g2.setColor(Color.WHITE); g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10); drawCentered(g2, l1, r.x + r.width / 2, r.y + r.height / 2 - 2); drawCentered(g2, l2, r.x + r.width / 2, r.y + r.height / 2 + 10); }
    private void drawCentered(Graphics2D g2, String t, int x, int y) { FontMetrics fm = g2.getFontMetrics(); g2.drawString(t, x - fm.stringWidth(t) / 2, y); }
    private String toKoreanLabel(String e) { if ("Fire".equals(e)) return "화염"; if ("Water".equals(e)) return "물"; if ("Nature".equals(e)) return "자연"; if ("Arcane".equals(e)) return "비전"; if ("Shadow".equals(e)) return "그림자"; if ("Chaos".equals(e)) return "혼돈"; if ("Boss".equals(e)) return "보스"; return e; }
}
