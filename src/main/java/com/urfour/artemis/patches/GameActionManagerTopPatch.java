package com.urfour.artemis.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.urfour.artemis.GameStateListener;

@SpirePatch(
        clz= GameActionManager.class,
        method="addToTop"
)
public class GameActionManagerTopPatch {
    public static void Postfix(GameActionManager _instance, AbstractGameAction _arg) {
        GameStateListener.registerStateChange();
    }
}
