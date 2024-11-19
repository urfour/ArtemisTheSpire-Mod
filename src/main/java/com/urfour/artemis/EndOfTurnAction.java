package com.urfour.artemis;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import java.util.logging.Logger;
import java.util.logging.Level;

public class EndOfTurnAction extends AbstractGameAction {

    public void update() {
        GameStateListener.signalTurnEnd();
        this.isDone = true;
    }
}