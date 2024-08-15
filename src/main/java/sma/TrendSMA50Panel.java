package sma;

import fundamental.stock.FundamentalDataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.Coin;
import utils.TimeFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TrendSMA50Panel extends JPanel {
    private JTabbedPane chartTabbedPane = new JTabbedPane();
    private static final Logger logger = LoggerFactory.getLogger(TrendSMA50Panel.class);

    private final FundamentalDataUtil fdUtil;

    public TrendSMA50Panel(FundamentalDataUtil fdUtil) {
        this.fdUtil = fdUtil;
        init();
    }

    public void refreshChart() {
        logger.info("Updating SMA50 trend panel with start date");
        chartTabbedPane.removeAll();
        for (Coin coin : Coin.values()) {
            JTabbedPane upperTabbedPane = new JTabbedPane();

            for (TimeFrame tf : TimeFrame.values()) {
                if (tf == TimeFrame.FIVE_MINUTES)
                    continue;
                TrendSMA50Runner runner = new TrendSMA50Runner(coin, tf, fdUtil);
                String output = runner.run();
                upperTabbedPane.add("Timeframe " + tf.getTimeFrame(), getChartPanel(output));
            }
            chartTabbedPane.add(coin.toString(), upperTabbedPane);
        }
        chartTabbedPane.revalidate();
        chartTabbedPane.repaint();
    }

    private JPanel getChartPanel(String imagePath) {
        JPanel panel = new JPanel();
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            JLabel label = new JLabel(new ImageIcon(image));
            panel.add(label);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return panel;
    }

    private void init() {
        logger.info("Initializing SMA50 trend panel");
        this.setLayout(new BorderLayout());
        add(chartTabbedPane, BorderLayout.CENTER);
        initGridPanel();
    }

    private void initGridPanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));

        Font newButtonFont = new Font("Arial", Font.BOLD, 22);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(newButtonFont);
        refreshButton.setBackground(Color.LIGHT_GRAY);
        refreshButton.addActionListener(e -> {
            new Thread(this::refreshChart).start();
        });

        JPanel gridPanel = new JPanel(new GridLayout(1, 1));
        gridPanel.add(refreshButton);

        sidePanel.add(gridPanel);
        sidePanel.setBackground(Color.LIGHT_GRAY);

        add(sidePanel, BorderLayout.EAST);
    }

    public void clearTempDirectories() {
        TrendSMA50Runner.deleteTempDirectories();
    }
}
