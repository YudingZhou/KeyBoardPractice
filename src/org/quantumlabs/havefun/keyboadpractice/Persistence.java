package org.quantumlabs.havefun.keyboadpractice;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by quintus on 4/29/15.
 */
public class Persistence {
    final static private Logger LOG = Logger.getLogger(Persistence.class);
    final static public Persistence INSTANCE = new Persistence();
    final private static File DATASTORE = new File(System.getProperty("user.home") + "/.keyboardPractice/statistic.dat");

    final private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

    static {
        try {
            FileUtils.touch(DATASTORE);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private Persistence() {

    }

    public static void finalize(KeyBoardPracticeModel.Result result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DATE_FORMAT.format(new Date()));
        stringBuilder.append("|");
        stringBuilder.append(result.toString());
        stringBuilder.append("\n");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATASTORE, true))) {
            writer.write(stringBuilder.toString());
        } catch (IOException e) {
            LOG.error("Failed to persist result", e);
        }
    }
}
