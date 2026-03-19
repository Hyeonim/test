package com.towerdefense;

import java.awt.Color;

public enum Job {
    NOBLE(0, "귀족", "웨이브 클리어 골드 +35%", "assets/ui/jobs/noble.png", new Color(198, 156, 78)),
    KNIGHT_COMMANDER(1, "기사단장", "타워 강화비용 -20%", "assets/ui/jobs/knight_commander.png", new Color(113, 154, 197)),
    MAGITECH_ENGINEER(2, "마도공학자", "타워 공격력 +20%", "assets/ui/jobs/magitech_engineer.png", new Color(170, 118, 230));

    public final int index;
    public final String label;
    public final String summary;
    public final String imagePath;
    public final Color accentColor;

    Job(int index, String label, String summary, String imagePath, Color accentColor) {
        this.index = index;
        this.label = label;
        this.summary = summary;
        this.imagePath = imagePath;
        this.accentColor = accentColor;
    }
}
