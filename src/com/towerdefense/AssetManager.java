package com.towerdefense;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;

import static com.towerdefense.GameConstants.*;

public class AssetManager {
    public BufferedImage bgTexture;
    public BufferedImage buildTileTexture;
    public BufferedImage pathStraightTexture;
    public BufferedImage pathCurveTexture;
    public BufferedImage pathCrossTexture;
    public final BufferedImage[] towerSprites = new BufferedImage[6];
    public final BufferedImage[] jobImages = new BufferedImage[3];
    public final BufferedImage[] jobImagesGray = new BufferedImage[3];
    public final BufferedImage[][] monsterDirectionalSprites = new BufferedImage[4][3];

    public void loadAssets() {
        bgTexture = loadImage("assets/ui/background.png");
        buildTileTexture = loadImage("assets/ui/build_tile.png");
        pathStraightTexture = loadImage("assets/ui/path_tile/path_tile.png");
        pathCrossTexture = loadImage("assets/ui/path_tile/cross_path_tile.png");

        if (pathStraightTexture == null) pathStraightTexture = loadImage("assets/ui/path_tile.png");

        towerSprites[0] = loadImage("assets/towers/fire.png");
        towerSprites[1] = loadImage("assets/towers/water.png");
        towerSprites[2] = loadImage("assets/towers/nature.png");
        towerSprites[3] = loadImage("assets/towers/arcane.png");
        towerSprites[4] = loadImage("assets/towers/shadow.png");
        towerSprites[5] = loadImage("assets/towers/chaos.png");

        for (Job job : Job.values()) {
            jobImages[job.index] = loadImage(job.imagePath);
            jobImagesGray[job.index] = toGray(jobImages[job.index]);
        }

        loadMonsterSpriteSet(0, "fire", "assets/monsters/org/fire.png");
        loadMonsterSpriteSet(1, "water", "assets/monsters/org/water.png");
        loadMonsterSpriteSet(2, "nature", "assets/monsters/org/nature.png");
        loadMonsterSpriteSet(3, "boss", "assets/monsters/org/boss.png");

        if (buildTileTexture == null) buildTileTexture = makeTileTexture(new Color(78, 112, 136), new Color(96, 132, 160));
        if (pathStraightTexture == null) pathStraightTexture = makeTileTexture(new Color(38, 49, 64), new Color(31, 40, 54));
        if (pathCrossTexture == null) pathCrossTexture = pathStraightTexture;

        buildTileTexture = fitToTileSize(trimTransparentBounds(buildTileTexture));
        pathStraightTexture = fitToTileSize(trimTransparentBounds(pathStraightTexture));
        pathCrossTexture = fitToTileSize(trimTransparentBounds(pathCrossTexture));
        pathCurveTexture = createCurvePathTexture(pathCrossTexture);
        if (pathCurveTexture == null) pathCurveTexture = pathStraightTexture;
    }

    private void loadMonsterSpriteSet(int spriteIndex, String assetName, String legacyPath) {
        BufferedImage legacy = normalizeMonsterSprite(loadImage(legacyPath));
        if (legacy == null) {
            legacy = normalizeMonsterSprite(loadImage("assets/monsters/" + assetName + ".png"));
        }

        BufferedImage sheet = loadImage("assets/monsters/motion/" + assetName + "-front-side-back.png");
        if (sheet == null) {
            sheet = loadImage("assets/monsters/" + assetName + "-front-side-back.png");
        }
        if (sheet != null) {
            BufferedImage[] splitSprites = splitMonsterSpriteSheet(sheet);
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_FRONT] = splitSprites[0];
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_SIDE] = splitSprites[1];
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_BACK] = splitSprites[2];
        } else {
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_FRONT] = normalizeMonsterSprite(loadImage("assets/monsters/" + assetName + "_front.png"));
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_BACK] = normalizeMonsterSprite(loadImage("assets/monsters/" + assetName + "_back.png"));
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_SIDE] = normalizeMonsterSprite(loadImage("assets/monsters/" + assetName + "_side.png"));
        }

        if (monsterDirectionalSprites[spriteIndex][GameConstants.FACING_FRONT] == null) {
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_FRONT] = legacy;
        }
        if (monsterDirectionalSprites[spriteIndex][GameConstants.FACING_BACK] == null) {
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_BACK] = monsterDirectionalSprites[spriteIndex][GameConstants.FACING_FRONT];
        }
        if (monsterDirectionalSprites[spriteIndex][GameConstants.FACING_SIDE] == null) {
            monsterDirectionalSprites[spriteIndex][GameConstants.FACING_SIDE] = monsterDirectionalSprites[spriteIndex][GameConstants.FACING_FRONT];
        }
    }

    private BufferedImage[] splitMonsterSpriteSheet(BufferedImage sheet) {
        BufferedImage[] sprites = new BufferedImage[3];
        if (sheet == null) return sprites;

        int sliceWidth = sheet.getWidth() / 3;
        int remainder = sheet.getWidth() - (sliceWidth * 3);
        int startX = 0;

        for (int i = 0; i < 3; i++) {
            int currentWidth = sliceWidth;
            if (i == 2) currentWidth += remainder;
            sprites[i] = normalizeMonsterSprite(sheet.getSubimage(startX, 0, currentWidth, sheet.getHeight()));
            startX += sliceWidth;
        }
        return sprites;
    }

    private BufferedImage normalizeMonsterSprite(BufferedImage src) {
        if (src == null) return null;
        return trimTransparentBounds(src);
    }

    private BufferedImage loadImage(String relativePath) {
        File file = resolveAssetPath(relativePath);
        if (file == null) {
            System.err.println("[AssetManager] File not found: " + relativePath);
            return null;
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null && (img.getWidth() > 1024 || img.getHeight() > 1024)) {
                return scaleImage(img, 1024);
            }
            return img;
        } catch (IOException e) {
            System.err.println("[AssetManager] Failed to read image: " + relativePath + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private BufferedImage scaleImage(BufferedImage src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        double factor = Math.min((double) maxSide / w, (double) maxSide / h);
        int targetW = (int) (w * factor);
        int targetH = (int) (h * factor);
        
        BufferedImage res = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = res.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return res;
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

    private BufferedImage createCurvePathTexture(BufferedImage crossTexture) {
        if (crossTexture == null) return null;

        BufferedImage source = fitToTileSize(crossTexture);
        BufferedImage mask = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);

        float strokeWidth = TILE_SIZE * 0.70f;
        float center = TILE_SIZE / 2.0f;
        Path2D.Float curvePath = new Path2D.Float();
        curvePath.moveTo(0, center);
        curvePath.lineTo(center, center);
        curvePath.lineTo(center, TILE_SIZE);
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(curvePath);
        g.dispose();

        BufferedImage softenedMask = blurImage(blurImage(mask));
        BufferedImage result = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int srcArgb = source.getRGB(x, y);
                int srcAlpha = (srcArgb >>> 24) & 0xFF;
                int maskAlpha = (softenedMask.getRGB(x, y) >>> 24) & 0xFF;
                int outAlpha = (srcAlpha * maskAlpha) / 255;
                result.setRGB(x, y, (outAlpha << 24) | (srcArgb & 0x00FFFFFF));
            }
        }

        return result;
    }

    private BufferedImage blurImage(BufferedImage src) {
        float[] kernel = {
                1f / 16f, 2f / 16f, 1f / 16f,
                2f / 16f, 4f / 16f, 2f / 16f,
                1f / 16f, 2f / 16f, 1f / 16f
        };
        ConvolveOp blur = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        return blur.filter(src, null);
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

    private BufferedImage toGray(BufferedImage src) {
        if (src == null) return null;
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(src, gray);
        return gray;
    }
}
