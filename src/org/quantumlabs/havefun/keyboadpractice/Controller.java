package org.quantumlabs.havefun.keyboadpractice;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by quintus on 4/16/15.
 */
public class Controller implements IInputDelegater, IControlDelegater, KeyBoardPracticeModel.ExamStateListener {

    private static final Logger LOG = Logger.getLogger(Controller.class);
    private final IDisplayView display;
    private KeyBoardPracticeModel examEngine;
    private boolean started;
    private List<Function<ISystemHandler, Void>> startingListeners = new ArrayList<>();

    public Controller(IDisplayView view) {
        this.display = view;
    }

    public static void main(IDisplayView displayView) {
        //IDisplayView cmdDisplayView = new CmdDisplayView();
        Controller controller = new Controller(displayView);
        displayView.bindControlDelegetor(controller);
        controller.start();
    }

    public void createExamEngine(KeyBoardPracticeModel.ExamConfiguration configuration) {
        LOG.info("createExamEngine():config:" + configuration);
        examEngine = new KeyBoardPracticeModel(this);
        examEngine.init(configuration);
        registerExamEventListener();
    }

    private void registerExamEventListener() {
        examEngine.registerListener((exam) -> {
            display.showOnCorrect(exam.key);
            return null;
        }, KeyBoardPracticeModel.ExamEvent.CORRECT);

        examEngine.registerListener((exam) -> {
            display.showOnIncorrect(exam.key, exam.getInput());
            return null;
        }, KeyBoardPracticeModel.ExamEvent.FAILURE);

        examEngine.registerListener((exam) -> {
            display.showOnTimeout(exam.key);
            return null;
        }, KeyBoardPracticeModel.ExamEvent.TIMEOUT);
    }

    public void showTheResult(KeyBoardPracticeModel.Result result) {
        LOG.info("showTheResult(): engine state:" + examEngine);
        display.showResult(String.format("\nCorrection:%s\nError:%s\nTimeout:%s\nDuration:%s''\n", result.correction, result.failure, result.timeout, result.durationInSeconds));
        LOG.info("showTheResult():" + result);
    }

    public void start() {
        display.showMainView("Welcome to keyboard practice");
        started = true;
        notifyStart();
        //Main thread controls exam displaying
        while (started) {
            LOG.info("start(): next round of exam");
            KeyBoardPracticeModel.ExamConfiguration configuration = display.showConfigurationView("Please configure the practice");
            createExamEngine(configuration);
            startExam();
            LOG.info("start(): end current round of exam");
        }
    }

    private void notifyStart() {
        LOG.info("notifyStart(): notifying system start");
        for (Function<ISystemHandler, Void> listener : startingListeners.toArray(new Function[startingListeners.size()])) {
            listener.apply(this);
        }
    }

    private void startExam() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("enter:startExam()" + examEngine);
        }
        examEngine.start();
        display.showPreStart();
        while (examEngine.hasNext()) {
            KeyBoardPracticeModel.Exam exam = examEngine.next();
            if (LOG.isDebugEnabled()) {
                LOG.debug("current exam" + exam);
            }
            display.showExam(exam.key);
            exam.waitForInput();
        }
        KeyBoardPracticeModel.Result result = examEngine.finish();
        showTheResult(result);
        if (LOG.isDebugEnabled()) {
            LOG.debug("exit:start()" + examEngine);
        }
    }

    /**
     * Send input key will be called in UI thread
     */
    @Override
    public void sendInputKey(String input, IDisplayView displayView) {
        if (isExamStarted()) {
            dispatchInputKey(input, displayView);
        } else {
            LOG.debug(String.format("No exam started, ignore input %s", input));
        }
    }


    private boolean isExamStarted() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("isExamStarted():" + examEngine.isStarted());
        }
        return examEngine.isStarted();
    }

    private void dispatchInputKey(String input, IDisplayView displayView) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("dispatchInputKey():" + input);
        }
        ensureExamIsStarted();
        final KeyBoardPracticeModel.Exam exam = examEngine.getCurrentExam();
        if (LOG.isDebugEnabled()) {
            LOG.debug("dispatchInputKey():exam:" + exam);
        }
        exam.exam(input);
    }

    private void ensureExamIsStarted() {
        Validate.notNull(examEngine);
        Validate.notNull(examEngine.getCurrentExam());
    }

    @Override
    public void terminate() {
        LOG.info("terminate():system existing");
        started = false;
        display.showTerminating("Bye byte");
        try {
            Thread.currentThread().sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
            //ignore
        }
    }

    /**
     * InputDelegator would be the delegator for processing input for exam while exam existing.
     * While exam started, input source would be notified; after a whole round of exam finished,
     * there is <strong>no notification</strong> but the {@link org.quantumlabs.havefun.keyboadpractice.Controller#isStarted()}
     * would return false.
     */
    @Override
    public boolean isStarted() {
        return isExamStarted();
    }

    @Override
    public void registerSystemListener(Function<ISystemHandler, Void> listener) {
        Validate.notNull(listener);
        startingListeners.add(listener);
    }

    @Override
    public void onStartExam(KeyBoardPracticeModel keyBoardPracticeModel) {
        display.bindInputDelegetor(this);
    }

    @Override
    public void onFinishExam(KeyBoardPracticeModel keyBoardPracticeModel) {
        display.unbindInputDelegetor(this);
    }
}
