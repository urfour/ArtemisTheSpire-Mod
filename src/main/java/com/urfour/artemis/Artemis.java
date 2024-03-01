package com.urfour.artemis;

import basemod.BaseMod;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostPlayerUpdateSubscriber;
import basemod.interfaces.StartGameSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpireInitializer
public class Artemis implements StartGameSubscriber, PostPlayerUpdateSubscriber, OnStartBattleSubscriber, PostBattleSubscriber {
    private static String getArtemisFolder() {
        if (System.getProperty("os.name").contains("Windows")) {
            return System.getenv("ProgramData");
        }
        else {
            return System.getProperty("user.home") + "/.local/share";
        }
    }
    private final SpireInfo info = new SpireInfo();
    private static final String WEB_SERVER_FILE = getArtemisFolder() + "/Artemis/webserver.txt";
    private final URL IP;
    private final Gson gson =  new Gson();
    public Artemis() {
        BaseMod.subscribe(this);
        try {
            String address = new String(Files.readAllBytes(Paths.get(WEB_SERVER_FILE)));
            IP = new URL("http", address, "/plugins/97BEA404-5C66-4B35-8B3D-96163E8F895A/SlayTheSpire");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static final Logger LOGGER = LogManager.getLogger(Artemis.class.getName());
    public static void initialize() {
        new Artemis();
    }
    @Override
    public void receiveStartGame() {
        info.update();
        try {
            LOGGER.info(gson.toJson(info));
            HttpURLConnection urlConn;
            urlConn = (HttpURLConnection) IP.openConnection();
            urlConn.setDoOutput(true);
            urlConn.addRequestProperty("Content-Type", "application/json");
            urlConn.getOutputStream().write(gson.toJson(info).getBytes(StandardCharsets.UTF_8));
            int response = urlConn.getResponseCode();
            assert response != 400;
            LOGGER.debug(response);
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }
    @Override
    public void receivePostPlayerUpdate() {
        info.update();
        try {
            HttpURLConnection urlConn;
            urlConn = (HttpURLConnection) IP.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setConnectTimeout(200);
            urlConn.setRequestProperty("Content-Type", "application/json");
            urlConn.getOutputStream().write(gson.toJson(info).getBytes(StandardCharsets.UTF_8));
            String response = urlConn.getResponseMessage();
            //assert response != 400;
            LOGGER.debug(response);
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }
    public void receiveOnBattleStart(AbstractRoom room) {
        if (room.isBattleOver) {
            LOGGER.info("Battle started");
            info.setBattleState(true);
        }
    }
    public void receivePostBattle(AbstractRoom room) {
        if (room.isBattleOver) {
            LOGGER.info("Battle finished");
            info.setBattleState(false);
        }
    }
}
