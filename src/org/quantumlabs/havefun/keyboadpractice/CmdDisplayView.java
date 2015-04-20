package org.quantumlabs.havefun.keyboadpractice;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by quintus on 4/17/15.
 */
public class CmdDisplayView implements IDisplayView {

    private final PrintWriter writer = new PrintWriter(System.out) {
        @Override
        public void write(String s, int off, int len) {
            System.out.println(s);
        }

        @Override
        public void println() {
            System.out.println();
        }
    };

    private final Reader lock = new InputStreamReader(System.in);
    private final BufferedReader reader = new BufferedReader(lock);
    private IControlDelegater controlDelegator;
    private IInputDelegater inputDelegator;

    private static final Logger LOG = Logger.getLogger(CmdDisplayView.class);

    @Override
    public void showExam(String key) {
        writer.println(String.format("The exam is : %s", key));
    }

    @Override
    public void showMainView(String greetingMessage) {
        writer.println(String.format("%s", greetingMessage));
    }

    @Override
    public KeyBoardPracticeModel.ExamConfiguration showConfigurationView(String message) {
        writer.println(message);
        writer.println("Please input the size you want to practice IT MUST BE A POSITIVE NUMBER");
        try {
            Optional<String> systemInputOptl = getInput();
            String inputConfig;
            if (systemInputOptl.isPresent()) {
                inputConfig = systemInputOptl.get();
                int size = Integer.valueOf(inputConfig);
                if (size <= 0) {
                    writer.println("The size must larger than 0");
                    //If there is any error, re-showConfigurationView
                    return showConfigurationView(message);
                } else {
                    return new KeyBoardPracticeModel.ExamConfiguration(size);
                }
            } else {
                return showConfigurationView(message);
            }
        } catch (NumberFormatException e) {
            writer.println();
            //If there is any error, re-showConfigurationView
            return showConfigurationView(message);
        }
    }

    private Optional<String> getInput() {
        try {
            input = reader.readLine();
            return StringUtils.isEmpty(input) ? Optional.empty() : Optional.of(input);
        } catch (IOException e) {
            LOG.error("getInput(): cant get system input ", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            input = null;
        }
    }

    private volatile String input = null;


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        reader.close();
        writer.close();
    }

    private ExecutorService uiExec = Executors.newCachedThreadPool();


    private boolean needInputDelegating;

    private Thread uiListener;

    @Override
    public void bindInputDelegetor(final IInputDelegater theDelegator) {
        this.inputDelegator = theDelegator;
        needInputDelegating = true;
        uiListener = new Thread(() -> {
            LOG.info("bindInputDelegetor(): creating UI input thread");
            while (inputDelegator.isStarted()) {
                LOG.info("bindInputDelegetor() waiting for system input");
                try {
                    Optional<String> inputOptl = getInput();
                    if (inputOptl.isPresent()) {
                        if (needInputDelegating) {
                            LOG.debug("bindInputDelegetor(): submit system input");
                            inputDelegator.sendInputKey(inputOptl.get(), this);
                        } else {

                        }
                    } else {
                        LOG.warn("System input is not presented");
                    }
                } catch (Exception e) {
                    LOG.error(e);
                }
            }
            LOG.info("bindInputDelegetor(): UI input thread exiting");
        }, "UI Delegetor");
        uiListener.start();
    }

    @Override
    public void bindControlDelegetor(IControlDelegater controlDelegater) {
        this.controlDelegator = controlDelegater;
    }

    @Override
    public void showResult(String result) {
        writer.println("The result is : " + result);
    }

    @Override
    public void showTerminating(String message) {
        writer.println("System is terminating : " + message);
    }

    @Override
    public void showOnCorrect(String key) {
        writer.println("Correct KEY-> " + key + "\n**************");
    }

    @Override
    public void showOnIncorrect(String key, String input) {
        writer.println(String.format("Incorrect KEY->%s    Input->%s\n**************", key, input));
    }

    @Override
    public void showOnTimeout(String key) {
        writer.println(String.format("Timeout KEY->%s \n**************", key));
    }

    @Override
    public void unbindInputDelegetor(IInputDelegater inputDelegator) {
        needInputDelegating = false;
    }

    @Override
    public void showPreStart() {

    }
}
