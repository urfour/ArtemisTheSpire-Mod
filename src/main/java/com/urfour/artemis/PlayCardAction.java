package com.urfour.artemis;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;

public class PlayCardAction extends AbstractGameAction {
    private AbstractCard card;

    public PlayCardAction(AbstractCard card) {
        this.card = card;
    }

    @Override
    public void update() {
        GameStateListener.notifyCardPlayed(card);
        this.isDone = true;
    }
}