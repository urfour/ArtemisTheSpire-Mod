package com.urfour.artemis.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.common.EnableEndTurnButtonAction;
import com.urfour.artemis.GameStateListener;

@SpirePatch(
        clz = EnableEndTurnButtonAction.class,
        method = "update"
)
public class EnableEndTurnPatch {
    public static void Postfix(EnableEndTurnButtonAction _instance) {
        GameStateListener.signalTurnStart();
    }
}
