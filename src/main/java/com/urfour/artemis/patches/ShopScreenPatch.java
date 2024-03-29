package com.urfour.artemis.patches;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.urfour.artemis.GameStateListener;
import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.ArrayList;

public class ShopScreenPatch {

    public static boolean doHover = false;
    public static AbstractCard hoverCard;


    @SpirePatch(
            clz = ShopScreen.class,
            method = "purgeCard"
    )
    public static class PurgeCardPatch {

        public static void Postfix() {
            GameStateListener.resumeStateUpdate();  // Needed to wait for the rest of the logic to complete after card was selected.
        }

    }


    @SpirePatch(
            clz=ShopScreen.class,
            method = "update"
    )
    public static class HoverCardPatch {

        @SuppressWarnings("unchecked")
        @SpireInsertPatch(
                locator=Locator.class
        )
        public static void Insert(ShopScreen _instance) {
            if(doHover) {
                ArrayList<AbstractCard> coloredCards = (ArrayList<AbstractCard>) ReflectionHacks.getPrivate(_instance, ShopScreen.class, "coloredCards");
                ArrayList<AbstractCard> colorlessCards = (ArrayList<AbstractCard>) ReflectionHacks.getPrivate(_instance, ShopScreen.class, "colorlessCards");
                for(AbstractCard card : coloredCards) {
                    card.hb.hovered = card == hoverCard;
                }
                for(AbstractCard card : colorlessCards) {
                    card.hb.hovered = card == hoverCard;
                }
                doHover = false;
            }
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher matcher = new Matcher.MethodCallMatcher(ShopScreen.class, "updateHand");
                return LineFinder.findInOrder(ctMethodToPatch, new ArrayList<Matcher>(), matcher);
            }
        }

    }

}
