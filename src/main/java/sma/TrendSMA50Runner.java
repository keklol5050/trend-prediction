package sma;

import data_utils.utils.BinanceDataUtil;
import fundamental.stock.FundamentalDataUtil;
import core.updater.DataSetUpdater;
import core.vo.DataObject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.Coin;
import utils.PropertiesUtil;
import utils.TimeFrame;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static core.data_utils.select.StaticUtils.python_interpreter_path;

public class TrendSMA50Runner {
    private static final Logger logger = LoggerFactory.getLogger(TrendSMA50Runner.class);

    private static final String p_path = new File(TrendSMA50Runner.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParent()
            + PropertiesUtil.getProperty("python_path") + "trend/SMA50/";
    private static final String data_temp_path = p_path + "d_temp/";
    private static final String img_temp_path = p_path + "i_temp/";
    private static final String models_path = p_path + "models/";

    private final Coin coin;
    private final TimeFrame tf;
    private final FundamentalDataUtil fdUtil;

    static  {
        logger.info("Trend prediction (SMA50) script temp path: {}", data_temp_path);
    }

    public TrendSMA50Runner(Coin coin, TimeFrame tf, FundamentalDataUtil fdUtil) {
        this.coin = coin;
        this.tf = tf;
        this.fdUtil = fdUtil;
    }

    public String run() {
        logger.info("Running trend prediction (SMA50) script wrapper for coin {} and interval {}", coin, tf);
        createTempDirectories();

        ArrayList<DataObject> latest = BinanceDataUtil.getLatestInstances(coin, tf, 200, fdUtil);

        String path = String.format("%s%s-%s.csv", data_temp_path, coin, tf);
        DataSetUpdater.writeData(Path.of(path), latest);
        logger.info("Wrote input data at {}", path);

        String pathOut = String.format("%s%s-%s.png", img_temp_path, coin, tf);

        String modelPath = String.format("%sSMA50-%s-%s.np", models_path, coin, tf.getTimeFrame());
        logger.info("Starting model from {}", modelPath);

        ProcessBuilder pb = new ProcessBuilder(python_interpreter_path, p_path + "SMA50-accessor.py",
                path, modelPath, String.valueOf(tf.getMinuteCount()), pathOut, coin.toString(), tf.getTimeFrame());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return pathOut;
    }

    public static void main(String[] args) {
        new TrendSMA50Runner(Coin.ETHUSDT, TimeFrame.ONE_HOUR, new FundamentalDataUtil()).run();
    }

    public static void createTempDirectories() {
        try {
            Files.createDirectories(Paths.get(data_temp_path));
            Files.createDirectories(Paths.get(img_temp_path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void deleteTempDirectories() {
        try {
            FileUtils.deleteDirectory(new File(data_temp_path));
            FileUtils.deleteDirectory(new File(img_temp_path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

