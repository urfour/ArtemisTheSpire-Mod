package com.urfour.artemis.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.urfour.artemis.GameStateListener;
import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.ArrayList;

@SpirePatch(
        clz= GenericEventDialog.class,
        method="update"
)

public class GenericEventDialogPatch {

    @SpireInsertPatch(
            locator=Locator.class
    )
    public static void Insert(GenericEventDialog _instance) {
        GameStateListener.registerStateChange();
    }

    private static class Locator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.FieldAccessMatcher(GenericEventDialog.class, "selectedOption");
            return LineFinder.findInOrder(ctMethodToPatch, new ArrayList<Matcher>(), matcher);
        }
    }

}
