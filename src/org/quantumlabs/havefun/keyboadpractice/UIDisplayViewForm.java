package org.quantumlabs.havefun.keyboadpractice;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by quintus on 4/18/15.
 */
public class UIDisplayViewForm implements IDisplayView {
    public static final String iconBayMaxBlue = "baymax-blue.png";
    public static final String iconBayMaxGreen = "baymax-green.png";
    public static final String iconBayMaxPink = "baymax-pink.png";
    public static final String iconBayMaxSleep = "baymax-sleep.png";
    private final static int DISPLAY_FONT_SIZE = 64;
    private static final Logger LOG = Logger.getLogger(UIDisplayViewForm.class);
    private static final int FRAME_WIDTH = 400;
    private static final int FRAME_HEIGHT = 300;
    private final JFrame mainFrame;
    private JPanel mainPanel;
    private JLabel keyDisplayLabel;
    private JLabel resultDisplayLabel;
    private int displayFontSize;
    private InputAdapter inputAdapter;
    private boolean inputDelegaterBinded = false;
    private int doodleSize = 32;

    public UIDisplayViewForm(JFrame frame) {
        this.mainFrame = frame;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Key board practice  (～￣▽￣)～ ");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setBounds((screenSize.width - FRAME_WIDTH) / 2, (screenSize.height - FRAME_HEIGHT) / 2, FRAME_WIDTH, FRAME_HEIGHT);
        UIDisplayViewForm uiDisplayViewForm = new UIDisplayViewForm(frame);
        frame.setContentPane(uiDisplayViewForm.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        uiDisplayViewForm.setDisplayFontSize(DISPLAY_FONT_SIZE);
        uiDisplayViewForm.keyDisplayLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, uiDisplayViewForm.getDoodleSize()));
        uiDisplayViewForm.keyDisplayLabel.setText("ლ(╹◡╹ლ )");
        uiDisplayViewForm.resultDisplayLabel.setText("(๑´ڡ`๑)");
        uiDisplayViewForm.keyDisplayLabel.setVerticalTextPosition(JLabel.CENTER);
        uiDisplayViewForm.keyDisplayLabel.setHorizontalAlignment(JLabel.CENTER);
        frame.pack();
        frame.setVisible(true);
        Controller controller = new Controller(uiDisplayViewForm);
        uiDisplayViewForm.bindControlDelegetor(controller);
        controller.start();
    }

    public int getDoodleSize() {
        return doodleSize;
    }

    public int getDisplayFontSize() {
        return displayFontSize;
    }

    public void setDisplayFontSize(int size) {
        displayFontSize = size;
    }

    @Override
    public void showExam(String key) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("showExam():exam:" + key);
        }
        keyDisplayLabel.setText(key);
    }

    private URL loadImageFile(String file) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(file);
        return url;
    }

    @Override
    public void showMainView(String greetingMessage) {
        ImageIcon icon = getImageIcon(iconBayMaxPink);
        int code = JOptionPane.showConfirmDialog(mainPanel, "welcome to KeyBoardPractice", "Laila~  (〃＾∇＾)", JOptionPane.NO_OPTION, JOptionPane.INFORMATION_MESSAGE, icon);
        if (JOptionPane.CANCEL_OPTION == code || JOptionPane.NO_OPTION == code) {
            showTerminating("Byebye!");
            System.exit(0);
        }
    }

    private ImageIcon getImageIcon(String file) {
        ImageIcon icon = new ImageIcon(loadImageFile(file));
        icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_AREA_AVERAGING));
        return icon;
    }

    @Override
    public KeyBoardPracticeModel.ExamConfiguration showConfigurationView(String message) {
        Icon icon = getImageIcon(iconBayMaxGreen);
        String configure = (String) JOptionPane.showInputDialog(mainPanel, "How many characters you want to try? =≡Σ((( つ•̀ω•́)つ   ", message, JOptionPane.INFORMATION_MESSAGE, icon, null, null);
        if (null == configure) {
            showTerminating("Bye bye");
            System.exit(0);
        }
        if (!StringUtils.isEmpty(configure)) {
            try {
                int size = Integer.valueOf(configure);
                if (size <= 0) {
                    return showConfigurationView(message);
                }
                return new KeyBoardPracticeModel.ExamConfiguration(size);
            } catch (NumberFormatException e) {
                JOptionPane.showConfirmDialog(mainPanel, "Exam size must be a positive integer, retry");
                return showConfigurationView(message);
            }
        } else {
            return showConfigurationView(message);
        }
    }

    private InputAdapter retrieveKeyAdapter(IInputDelegater delegater) {
        Validate.notNull(delegater);
        if (inputAdapter == null) {
            inputAdapter = new InputAdapter(delegater);
        }
        return inputAdapter;
    }

    @Override
    public void bindInputDelegetor(IInputDelegater delegator) {
        Validate.isTrue(!inputDelegaterBinded, "Duplicate bind input delegator " + Arrays.toString(mainFrame.getKeyListeners()));
        mainFrame.addKeyListener(retrieveKeyAdapter(delegator));
        inputDelegaterBinded = true;
    }

    @Override
    public void bindControlDelegetor(IControlDelegater controlDelegater) {
        mainFrame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controlDelegater.terminate();
            }
        });
    }

    @Override
    public void showResult(String result) {
        Icon icon = getImageIcon(iconBayMaxBlue);
        JOptionPane.showMessageDialog(mainPanel, "Your score is " + result, "(｀◕‸◕´+) ", JOptionPane.INFORMATION_MESSAGE, icon);
    }

    @Override
    public void showTerminating(String message) {
        Icon icon = getImageIcon(iconBayMaxSleep);
        resultDisplayLabel.setText("(๑´ڡ`๑)");
        JOptionPane.showMessageDialog(mainPanel, "Bye bye", " (≥﹏ ≤) ", JOptionPane.INFORMATION_MESSAGE, icon);
    }

    @Override
    public void showOnCorrect(String key) {
        resultDisplayLabel.setText("Correct " + key);
    }

    @Override
    public void showOnIncorrect(String key, String input) {
        resultDisplayLabel.setText("Incorrect " + key + "   " + input);
    }

    @Override
    public void showOnTimeout(String key) {
        resultDisplayLabel.setText("Timeout " + key);
    }

    @Override
    public void unbindInputDelegetor(IInputDelegater inputDelegator) {
        Validate.isTrue(inputDelegaterBinded);
        mainFrame.removeKeyListener(retrieveKeyAdapter(inputDelegator));
        inputDelegaterBinded = false;
    }

    @Override
    public void showPreStart() {
        try {
            keyDisplayLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, getDoodleSize()));
            keyDisplayLabel.setText("3");
            Thread.currentThread().sleep(1000);
            keyDisplayLabel.setText("2");
            Thread.currentThread().sleep(1000);
            keyDisplayLabel.setText("1");
            Thread.currentThread().sleep(1000);
            keyDisplayLabel.setText("(ง •̀_•́)ง Go");
            Thread.currentThread().sleep(1500);
            keyDisplayLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, getDisplayFontSize()));
        } catch (InterruptedException e) {
            LOG.warn("showPreStart(): pre-starting interrupted");
        }
    }

    private class InputAdapter extends KeyAdapter {
        private final IInputDelegater delegater;

        InputAdapter(IInputDelegater delegater) {
            this.delegater = delegater;
        }

        @Override
        public void keyTyped(KeyEvent e) {
            LOG.info("keyTyped:" + e.getKeyChar());
            delegater.sendInputKey(String.valueOf(e.getKeyChar()), UIDisplayViewForm.this);
        }
    }
}
