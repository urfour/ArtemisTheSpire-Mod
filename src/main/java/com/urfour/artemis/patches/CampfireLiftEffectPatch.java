package com.urfour.artemis.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.vfx.campfire.CampfireLiftEffect;
import com.urfour.artemis.GameStateListener;
import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.ArrayList;

@SpirePatch(
        clz= CampfireLiftEffect.class,
        method="update"
)
public class CampfireLiftEffectPatch {

    @SpireInsertPatch(
            locator=Locator.class
    )
    public static void Insert(CampfireLiftEffect _instance) {
        GameStateListener.resumeStateUpdate();
    }

    private static class Locator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher matcher = new Matcher.FieldAccessMatcher(CampfireLiftEffect.class, "isDone");
            return LineFinder.findInOrder(ctMethodToPatch, new ArrayList<Matcher>(), matcher);
        }
    }

}
