package com.urfour.artemis;

import basemod.*;
import basemod.interfaces.PostDungeonUpdateSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import basemod.interfaces.PreUpdateSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.urfour.artemis.patches.InputActionPatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SpireInitializer
public class Artemis implements PostUpdateSubscriber, PostDungeonUpdateSubscriber, PreUpdateSubscriber, OnStateChangeSubscriber {

    private static Process listener;
    private static StringBuilder inputBuffer = new StringBuilder();
    public static boolean messageReceived = false;
    private static final Logger logger = LogManager.getLogger(Artemis.class.getName());
    private static Thread writeThread;
    private static BlockingQueue<String> writeQueue;
    private static BlockingQueue<String> readQueue;
    public static boolean mustSendGameState = false;
    private static ArrayList<OnStateChangeSubscriber> onStateChangeSubscribers;

    private static SpireConfig communicationConfig;
    private static final String COMMAND_OPTION = "command";
    private static final String GAME_START_OPTION = "runAtGameStart";
    private static final String VERBOSE_OPTION = "verbose";
    private static final String INITIALIZATION_TIMEOUT_OPTION = "maxInitializationTimeout";
    private static final String DEFAULT_COMMAND = "";
    private static final long DEFAULT_TIMEOUT = 10L;
    private static final boolean DEFAULT_VERBOSITY = true;

    public Artemis(){
        BaseMod.subscribe(this);
        onStateChangeSubscribers = new ArrayList<>();
        Artemis.subscribe(this);
        readQueue = new LinkedBlockingQueue<>();
        try {
            Properties defaults = new Properties();
            defaults.put(GAME_START_OPTION, Boolean.toString(true));
            defaults.put(INITIALIZATION_TIMEOUT_OPTION, Long.toString(DEFAULT_TIMEOUT));
            defaults.put(VERBOSE_OPTION, Boolean.toString(DEFAULT_VERBOSITY));
            communicationConfig = new SpireConfig("Artemis", "config", defaults);
            String command = communicationConfig.getString(COMMAND_OPTION);
            // I want this to always be saved to the file so people can set it more easily.
            if (command == null) {
                communicationConfig.setString(COMMAND_OPTION, DEFAULT_COMMAND);
                communicationConfig.save();
            }
            communicationConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(getRunOnGameStartOption()) {
            startCommunication();
        }
    }

    public static void initialize() {
        Artemis mod = new Artemis();
    }

    public void receivePreUpdate() {
        /* if(writeThread != null && writeThread.isAlive()) {
            logger.info("Child process has died...");
            writeThread.interrupt();
        }
        */
        if(messageAvailable()) {
            try {
                boolean stateChanged = CommandExecutor.executeCommand(readMessage());
                if(stateChanged) {
                    GameStateListener.registerCommandExecution();
                }
            } catch (InvalidCommandException e) {
                HashMap<String, Object> jsonError = new HashMap<>();
                jsonError.put("error", e.getMessage());
                jsonError.put("ready_for_command", GameStateListener.isWaitingForCommand());
                Gson gson = new Gson();
                sendMessage(gson.toJson(jsonError));
            }
        }
    }

    public static void subscribe(OnStateChangeSubscriber sub) {
        onStateChangeSubscribers.add(sub);
    }

    public static void publishOnGameStateChange() {
        for(OnStateChangeSubscriber sub : onStateChangeSubscribers) {
            sub.receiveOnStateChange();
        }
    }

    public void receiveOnStateChange() {
        sendGameState();
    }

    public static void queueCommand(String command) {
        readQueue.add(command);
    }

    public void receivePostInitialize() {
        setUpOptionsMenu();
    }

    public void receivePostUpdate() {
        if(!mustSendGameState && GameStateListener.checkForMenuStateChange()) {
            mustSendGameState = true;
        }
        if(mustSendGameState) {
            publishOnGameStateChange();
            mustSendGameState = false;
        }
        InputActionPatch.doKeypress = false;
    }

    public void receivePostDungeonUpdate() {
        if (GameStateListener.checkForDungeonStateChange()) {
            mustSendGameState = true;
        }
        if(AbstractDungeon.getCurrRoom().isBattleOver) {
            GameStateListener.signalTurnEnd();
        }
    }

    private void setUpOptionsMenu() {
        ModPanel settingsPanel = new ModPanel();
        ModLabeledToggleButton gameStartOptionButton = new ModLabeledToggleButton(
                "Start external process at game launch",
                350, 550, Settings.CREAM_COLOR, FontHelper.charDescFont,
                getRunOnGameStartOption(), settingsPanel, modLabel -> {},
                modToggleButton -> {
                    if (communicationConfig != null) {
                        communicationConfig.setBool(GAME_START_OPTION, modToggleButton.enabled);
                        try {
                            communicationConfig.save();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        settingsPanel.addUIElement(gameStartOptionButton);

        ModLabel externalCommandLabel = new ModLabel(
                "", 350, 600, Settings.CREAM_COLOR, FontHelper.charDescFont,
                settingsPanel, modLabel -> {
            modLabel.text = String.format("External Process Command: %s", getSubprocessCommandString());
        });
        settingsPanel.addUIElement(externalCommandLabel);

        ModButton startProcessButton = new ModButton(
                350, 650, settingsPanel, modButton -> {
            BaseMod.modSettingsUp = false;
            startCommunication();
        });
        settingsPanel.addUIElement(startProcessButton);

        ModLabel startProcessLabel = new ModLabel(
                "(Re)start external process",
                475, 700, Settings.CREAM_COLOR, FontHelper.charDescFont,
                settingsPanel, modLabel -> {
            if(listener != null && listener.isAlive()) {
                modLabel.text = "Restart external process";
            } else {
                modLabel.text = "Start external process";
            }
        });
        settingsPanel.addUIElement(startProcessLabel);

        ModButton editProcessButton = new ModButton(
                850, 650, settingsPanel, modButton -> {});
        settingsPanel.addUIElement(editProcessButton);

        ModLabel editProcessLabel = new ModLabel(
                "Set command (not implemented)",
                975, 700, Settings.CREAM_COLOR, FontHelper.charDescFont,
                settingsPanel, modLabel -> {});
        settingsPanel.addUIElement(editProcessLabel);

        ModLabeledToggleButton verbosityOption = new ModLabeledToggleButton(
                "Suppress verbose log output",
                350, 500, Settings.CREAM_COLOR, FontHelper.charDescFont,
                getVerbosityOption(), settingsPanel, modLabel -> {},
                modToggleButton -> {
                    if (communicationConfig != null) {
                        communicationConfig.setBool(VERBOSE_OPTION, modToggleButton.enabled);
                        try {
                            communicationConfig.save();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        settingsPanel.addUIElement(verbosityOption);
        BaseMod.registerModBadge(ImageMaster.loadImage("Icon.png"),"Communication Mod", "Forgotten Arbiter", null, settingsPanel);
    }

    private void startCommunicationThreads() {
        writeQueue = new LinkedBlockingQueue<>();
        writeThread = new Thread(new DataWriter(writeQueue, getVerbosityOption()));
        writeThread.start();
    }

    private static void sendGameState() {
        String state = GameStateConverter.getCommunicationState();
        sendMessage(state);
    }

    private static void sendMessage(String message) {
        if(writeQueue != null && writeThread.isAlive()) {
            writeQueue.add(message);
        }
    }

    private static boolean messageAvailable() {
        return readQueue != null && !readQueue.isEmpty();
    }

    private static String readMessage() {
        if(messageAvailable()) {
            return readQueue.remove();
        } else {
            return null;
        }
    }

    private static String readMessageBlocking() {
        try {
            return readQueue.poll(getInitializationTimeoutOption(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to read message from subprocess.");
        }
    }

    private static String[] getSubprocessCommand() {
        if (communicationConfig == null) {
            return new String[0];
        }
        return communicationConfig.getString(COMMAND_OPTION).trim().split("\\s+");
    }

    private static String getSubprocessCommandString() {
        if (communicationConfig == null) {
            return "";
        }
        return communicationConfig.getString(COMMAND_OPTION).trim();
    }

    private static boolean getRunOnGameStartOption() {
        if (communicationConfig == null) {
            return false;
        }
        return communicationConfig.getBool(GAME_START_OPTION);
    }

    private static long getInitializationTimeoutOption() {
        if (communicationConfig == null) {
            return DEFAULT_TIMEOUT;
        }
        return (long)communicationConfig.getInt(INITIALIZATION_TIMEOUT_OPTION);
    }

    private static boolean getVerbosityOption() {
        if (communicationConfig == null) {
            return DEFAULT_VERBOSITY;
        }
        return communicationConfig.getBool(VERBOSE_OPTION);
    }

    private boolean startCommunication() {
        startCommunicationThreads();
        if (GameStateListener.isWaitingForCommand()) {
            mustSendGameState = true;
        }
        return true;
    }

}
