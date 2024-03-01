package com.urfour.artemis;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class SpireInfo {
    private PlayerInfo player = new PlayerInfo();
    private WorldInfo world = new WorldInfo();

    public void update() {
        player.getInfo();
        world.getInfo();
    }
    public void setBattleState(boolean isInBattle) {
        player.inBattle = isInBattle;
    }
    private static class PlayerInfo {
        private boolean inGame;
        private int health;
        private int maxHealth;
        private boolean isDead;
        private String className;
        private boolean inBattle;
        public PlayerInfo() {}
        private void getInfo() {
            try {
                if (AbstractDungeon.isPlayerInDungeon() && AbstractDungeon.getCurrRoom() != null){
                    AbstractPlayer player = AbstractDungeon.player;
                    assert player != null;
                    health = player.currentHealth;
                    inGame = AbstractDungeon.isPlayerInDungeon();
                    maxHealth = player.maxHealth;
                    isDead = player.isDead;
                    className = player.chosenClass.name();
                }
            } catch (Exception ex) {
                inGame = false;
            }
        }
    }

    private static class WorldInfo {
        private String levelName;
        public WorldInfo() {}
        private void getInfo() {
                levelName = null;
        }
    }
}