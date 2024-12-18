package com.urfour.artemis;

import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowRoom;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.VictoryRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameStateListener {
    private static AbstractDungeon.CurrentScreen previousScreen = null;
    private static boolean previousScreenUp = false;
    private static AbstractRoom.RoomPhase previousPhase = null;
    private static boolean previousGridSelectConfirmUp = false;
    private static int previousGold = 99;
    protected static boolean externalChange = false;
    private static boolean myTurn = false;
    private static boolean blocked = false;
    private static boolean waitingForCommand = false;
    private static boolean hasPresentedOutOfGameState = false;
    private static boolean waitOneUpdate = false;
    private static int timeout = 0;
    private static AbstractCard previousCardPlayed = null;
    private static AbstractCard cardPlayed = null;
    private static Logger logger = LogManager.getLogger(GameStateListener.class);

    /**
     * Used to indicate that something (in game logic, not external command) has been done that will change the game state,
     * and hasStateChanged() should indicate a state change when the state next becomes stable.
     */
    public static void registerStateChange() {
        externalChange = true;
        waitingForCommand = false;
    }

    /**
     * Used to tell hasStateChanged() to indicate a state change after a specified number of frames.
     * @param newTimeout The number of frames to wait
     */
    public static void setTimeout(int newTimeout) {
        timeout = newTimeout;
    }

    /**
     * Used to indicate that an external command has been executed
     */
    public static void registerCommandExecution() {
        waitingForCommand = false;
    }

    /**
     * Prevents hasStateChanged() from indicating a state change until resumeStateUpdate() is called.
     */
    public static void blockStateUpdate() {
        blocked = true;
    }

    /**
     * Removes the block instantiated by blockStateChanged()
     */
    public static void resumeStateUpdate() {
        blocked = false;
    }

    /**
     * Used by a patch in the game to signal the start of your turn. We do not care about state changes
     * when it is not our turn in combat, as we cannot take action until then.
     */
    public static void signalTurnStart() {
        myTurn = true;
    }

    /**
     * Used by patches in the game to signal the end of your turn (or the end of combat).
     */
    public static void signalTurnEnd() {
        myTurn = false;
    }

    /**
     * Resets all state detection variables for the start of a new run.
     */
    public static void resetStateVariables() {
        previousScreen = null;
        previousScreenUp = false;
        previousPhase = null;
        previousGridSelectConfirmUp = false;
        previousGold = 99;
        externalChange = false;
        myTurn = false;
        blocked = false;
        waitingForCommand = false;
        waitOneUpdate = false;
    }

    public static boolean isPlayerTurn() {
        return myTurn;
    }

    /**
     * Detects whether the game state is stable and we are ready to receive a command from the user.
     *
     * @return whether the state is stable
     */
    private static boolean hasDungeonStateChanged() {
        if (blocked) {
            return false;
        }
        hasPresentedOutOfGameState = false;
        AbstractDungeon.CurrentScreen newScreen = AbstractDungeon.screen;
        boolean newScreenUp = AbstractDungeon.isScreenUp;
        AbstractRoom.RoomPhase newPhase = AbstractDungeon.getCurrRoom().phase;
        boolean inCombat = (newPhase == AbstractRoom.RoomPhase.COMBAT);
        // Lots of stuff can happen while the dungeon is fading out, but nothing that requires input from the user.
        if (AbstractDungeon.isFadingOut || AbstractDungeon.isFadingIn) {
            return false;
        }
        // This check happens before the rest since dying can happen in combat and messes with the other cases.
        if (newScreen == AbstractDungeon.CurrentScreen.DEATH && newScreen != previousScreen) {
            return true;
        }
        // These screens have no interaction available.
        if (newScreen == AbstractDungeon.CurrentScreen.DOOR_UNLOCK || newScreen == AbstractDungeon.CurrentScreen.NO_INTERACT) {
            return false;
        }

        // In event rooms, we need to wait for the event wait timer to reach 0 before we can accurately assess its state.
        AbstractRoom currentRoom = AbstractDungeon.getCurrRoom();
        if ((currentRoom instanceof EventRoom
                || currentRoom instanceof NeowRoom
                || (currentRoom instanceof VictoryRoom && ((VictoryRoom) currentRoom).eType == VictoryRoom.EventType.HEART))
                && AbstractDungeon.getCurrRoom().event.waitTimer != 0.0F) {
            return false;
        }
        // The state has always changed in some way when one of these variables is different.
        // However, the state may not be finished changing, so we need to do some additional checks.
        if (newScreen != previousScreen || newScreenUp != previousScreenUp || newPhase != previousPhase) {
            if (inCombat) {
                return true;
            // Out of combat, we want to wait one update cycle, as some screen transitions trigger further updates.
            } else {
                waitOneUpdate = true;
                previousScreenUp = newScreenUp;
                previousScreen = newScreen;
                previousPhase = newPhase;
                return false;
            }
        } else if (waitOneUpdate) {
            waitOneUpdate = false;
            return true;
        }

        // If some other code registered a state change through registerStateChange(), or if we notice a state
        // change through the gold amount changing, we still need to wait until all actions are finished
        // resolving to claim a stable state and ask for a new command.
        if ((externalChange || previousGold != AbstractDungeon.player.gold)
                && AbstractDungeon.actionManager.phase.equals(GameActionManager.Phase.WAITING_ON_USER)
                && AbstractDungeon.actionManager.preTurnActions.isEmpty()
                && AbstractDungeon.actionManager.actions.isEmpty()
                && AbstractDungeon.actionManager.cardQueue.isEmpty()) {
            return true;
        }
        // In a grid select screen, if a confirm screen comes up or goes away, it doesn't change any other state.
        if (newScreen == AbstractDungeon.CurrentScreen.GRID) {
            boolean newGridSelectConfirmUp = AbstractDungeon.gridSelectScreen.confirmScreenUp;
            if (previousScreen == AbstractDungeon.CurrentScreen.GRID && newGridSelectConfirmUp != previousGridSelectConfirmUp) {
                return true;
            }
        }
        // Sometimes, we need to register an external change in combat while an action is resolving which brings
        // the screen up. Because the screen did not change, this is not covered by other cases.
        if (externalChange && inCombat && newScreenUp) {
            return true;
        }
        if (timeout > 0) {
            timeout -= 1;
            if(timeout == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detects whether the state of the game menu has changed. Right now, this only occurs when you first enter the
     * menu, either after starting Slay the Spire for the first time, or after ending a game and returning to the menu.
     *
     * @return Whether the main menu has just been entered.
     */
    public static boolean checkForMenuStateChange() {
        boolean stateChange = false;
        if (!hasPresentedOutOfGameState && CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT && CardCrawlGame.mainMenuScreen != null) {
            stateChange = true;
            hasPresentedOutOfGameState = true;
        }
        if (stateChange) {
            externalChange = false;
            waitingForCommand = true;
        }
        return stateChange;
    }

    /**
     * Detects a state change in AbstractDungeon, and updates all of the local variables used to detect
     * changes in the dungeon state. Sets waitingForCommand = true if a state change was registered since
     * the last command was sent.
     *
     * @return Whether a dungeon state change was detected
     */
    public static boolean checkForDungeonStateChange() {
        boolean stateChange = false;
        if (CommandExecutor.isInDungeon()) {
            stateChange = hasDungeonStateChanged();
            if (stateChange) {
                externalChange = false;
                waitingForCommand = true;
                previousPhase = AbstractDungeon.getCurrRoom().phase;
                previousScreen = AbstractDungeon.screen;
                previousScreenUp = AbstractDungeon.isScreenUp;
                previousGold = AbstractDungeon.player.gold;
                previousGridSelectConfirmUp = AbstractDungeon.gridSelectScreen.confirmScreenUp;
                timeout = 0;
            }
        } else {
            myTurn = false;
        }
        return stateChange;
    }

    public static boolean isWaitingForCommand() {
        return waitingForCommand;
    }
    public static boolean isMyTurn() { return myTurn; }

    public static void notifyCardPlayed(AbstractCard card) {
        cardPlayed = card;
        logger.info("Card played: " + card.name);
    }

    public static AbstractCard getCardPlayed() {
        if (cardPlayed != null && (previousCardPlayed == null || cardPlayed.uuid != previousCardPlayed.uuid)) {
            previousCardPlayed = cardPlayed;
            return cardPlayed;
        }
        return null;
    }


}