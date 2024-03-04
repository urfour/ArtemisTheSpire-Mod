package com.urfour.artemis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

public class DataWriter implements Runnable {
    private static String getArtemisFolder() {
        if (System.getProperty("os.name").contains("Windows")) {
            return System.getenv("ProgramData");
        }
        else {
            return System.getProperty("user.home") + "/.local/share";
        }
    }
    private final BlockingQueue<String> queue;
    private boolean verbose;
    private static final Logger logger = LogManager.getLogger(DataWriter.class.getName());
    private static final String WEB_SERVER_FILE = getArtemisFolder() + "/Artemis/webserver.txt";
    private final String IP;

    public DataWriter(BlockingQueue<String> queue, boolean verbose) {
        this.queue = queue;
        this.verbose = verbose;

        try {
            IP = new String(Files.readAllBytes(Paths.get(WEB_SERVER_FILE)))+"plugins/97BEA404-5C66-4B35-8B3D-96163E8F895A/SlayTheSpire";
            logger.info("Using IP " + IP + " for connection.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        String message = "";
        while (!Thread.currentThread().isInterrupted()) {
            try {
                URL url = new URL(IP);
                String line;
                StringBuffer sb = new StringBuffer();
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.addRequestProperty("Content-Type", "application/json");
                urlConnection.setDoOutput(true);

                BufferedOutputStream stream = new BufferedOutputStream(urlConnection.getOutputStream());
                message = this.queue.take();
                if (verbose) {
                    logger.info("Sending message: " + message);
                }
                stream.write(message.getBytes());
                stream.write('\n');
                stream.flush();
                stream.close();

                InputStream iStream = urlConnection.getInputStream();
                InputStreamReader iStreamReader = new InputStreamReader(iStream);
                BufferedReader br = new BufferedReader(iStreamReader);

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                if (verbose) {
                    logger.info("Received message: " + sb.toString());
                }
            } catch (InterruptedException e) {
                logger.info("Communications writing thread interrupted.");
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                logger.error("Message could not be sent to server: " + message);
                e.printStackTrace();
            }
        }
    }
}
