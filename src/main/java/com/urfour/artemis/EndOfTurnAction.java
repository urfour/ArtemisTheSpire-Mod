package com.urfour.artemis;

import com.megacrit.cardcrawl.actions.AbstractGameAction;

public class EndOfTurnAction extends AbstractGameAction {
    public void update() {
        GameStateListener.signalTurnEnd();
        this.isDone = true;
    }
}
