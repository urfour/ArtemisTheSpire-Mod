package com.urfour.artemis.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.urfour.artemis.PlayCardAction;

public class AbstractPlayerUseCardPatch {

    @SpirePatch(
            clz= AbstractPlayer.class,
            method="useCard"
    )
    public static class UseCardPatch {
        public static void Postfix(AbstractPlayer _instance) {
            AbstractDungeon.actionManager.addToBottom(new PlayCardAction(_instance.cardInUse));
        }
    }
}