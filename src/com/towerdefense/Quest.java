package com.towerdefense;

public class Quest {
    public String name, desc;
    public int reward;
    public boolean completed;

    public Quest(String name, String desc, int reward) {
        this.name = name;
        this.desc = desc;
        this.reward = reward;
        this.completed = false;
    }
}
