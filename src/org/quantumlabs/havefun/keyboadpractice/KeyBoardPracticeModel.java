package org.quantumlabs.havefun.keyboadpractice;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by quintus on 4/16/15.
 */
public class KeyBoardPracticeModel {

    public static final int EXAM_INTERVAL_DEFAULT_SECONDS = 2;
    private static final Logger LOG = Logger.getLogger(KeyBoardPracticeModel.class);
    private static final int NOT_STARTED = -1;
    private static final Logger LOG2 = Logger.getLogger(Exam.class);
    private final List<ExamStateListener> examStateListeners = new ArrayList<>();
    private boolean examStarted;
    private List<Function<Exam, Void>> failureCallBacks = new ArrayList<>();
    private List<Function<Exam, Void>> timeoutCallBacks = new ArrayList<>();
    private List<Function<Exam, Void>> correctCallBacks = new ArrayList<>();
    private int examSize;
    private int cursor;
    private int examInterval;
    private Exam currentExam;
    private Map<String, Statistic> keysStatistic = new HashMap<>();
    private String[] keysTable;
    private Random random;
    private long startTimeStamp;

    public KeyBoardPracticeModel(ExamStateListener... listener) {
        Validate.notNull(listener);
        examStateListeners.addAll(Arrays.asList(listener));
    }

    public boolean isStarted() {
        return examStarted;
    }

    public void start() {
        cursor = 0;
        createCurrentExam();
        startTimeStamp = System.currentTimeMillis();
    }

    public void registerExamStateListener(ExamStateListener listener) {
        Validate.notNull(listener);
        examStateListeners.add(listener);
    }

    public void registerListener(Function<Exam, Void> callBack, ExamEvent eventType) {
        Validate.notNull(callBack);
        switch (eventType) {
            case FAILURE:
                failureCallBacks.add(callBack);
                break;
            case CORRECT:
                correctCallBacks.add(callBack);
                break;
            case TIMEOUT:
                timeoutCallBacks.add(callBack);
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(eventType));
        }
    }

    public void init(ExamConfiguration configuration) {
        cursor = NOT_STARTED;
        this.examSize = configuration.examSize;
        loadKeys();
        initRandomGen();
        examInterval = EXAM_INTERVAL_DEFAULT_SECONDS;
        examStarted = true;
        LOG.info("init():" + toString());
    }

    public boolean hasNext() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("hasNext():enter");
        }
        ensureState();
        boolean hasNext = cursor < examSize;
        if (LOG.isDebugEnabled()) {
            LOG.debug("hasNext()exit:" + hasNext);
        }
        return hasNext;
    }

    private void createCurrentExam() {
        if (getCurrentExam() == null) {
            currentExam = new Exam(randomKey(), examInterval);
            notifyExamCreated();
        } else {
            currentExam = new Exam(randomKey(), examInterval);
        }
    }

    public Exam getCurrentExam() {
        return currentExam;
    }

    public Exam next() {
        ensureState();
        createCurrentExam();
        increaseCursor();
        return getCurrentExam();
    }

    private void increaseCursor() {
        cursor++;
    }

    private void onExamFailed(Exam exam) {
        Validate.isTrue(keysStatistic.containsKey(exam.key));
        Statistic statistic = keysStatistic.get(exam.key);
        statistic.occurs++;
        failureCallBacks.stream().forEach((callBack) -> callBack.apply(exam));
    }

    private void onExamTimeout(Exam exam) {
        Validate.isTrue(keysStatistic.containsKey(exam.key));
        Statistic statistic = keysStatistic.get(exam.key);
        statistic.occurs++;
        statistic.timeout++;
        timeoutCallBacks.stream().forEach((callBack) -> callBack.apply(exam));
    }

    private void onExamSucceed(Exam exam) {
        Validate.isTrue(keysStatistic.containsKey(exam.key));
        Statistic statistic = keysStatistic.get(exam.key);
        statistic.occurs++;
        statistic.bingos++;
        correctCallBacks.stream().forEach((callBack) -> callBack.apply(exam));
    }

    @Override
    public String toString() {
        return "KeyBoardPracticeModel{" +
                "examStarted=" + examStarted +
                ", examSize=" + examSize +
                ", cursor=" + cursor +
                ", currentExam=" + currentExam +
                '}';
    }

    public Result finish() {
        ensureState();
        Statistic aggregatedStatistic = keysStatistic.values().stream().reduce((previousResult, currentStatistic) -> {
            Statistic accumulator = new Statistic();
            accumulator.occurs = previousResult.occurs + currentStatistic.occurs;
            accumulator.bingos = previousResult.bingos + currentStatistic.bingos;
            accumulator.timeout = previousResult.timeout + currentStatistic.timeout;
            return accumulator;
        }).get();

        long duration = System.currentTimeMillis() - startTimeStamp;
        Result result = new Result(aggregatedStatistic.occurs, aggregatedStatistic.bingos, aggregatedStatistic.timeout, duration);
        if (LOG.isDebugEnabled()) {
            LOG.debug("finish():result:" + result + "; statistic:" + keysStatistic);
        }
        reset();
        notifyExamFinished();
        Persistence.INSTANCE.finalize(result);
        return result;
    }

    private void reset() {
        keysStatistic.clear();
        keysTable = null;
        cursor = NOT_STARTED;
        examSize = 0;
        currentExam = null;
        LOG.info("exit:reset():" + toString());
    }

    private void loadKeys() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("loadKeys(): enter");
        }
        Field[] fields = Keys.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                String keyValue = (String) fields[i].get(Keys.class);
                keysStatistic.put(keyValue, new Statistic());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cant load keys", e);
            }
        }
        keysTable = keysStatistic.keySet().toArray(new String[keysStatistic.size()]);
        if (LOG.isTraceEnabled()) {
            LOG.trace("loadKeys(): exit : " + keysStatistic);
        }
    }

    private void notifyExamCreated() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("notifyExamCreated()");
        }
        for (ExamStateListener listener : examStateListeners.toArray(new ExamStateListener[examStateListeners.size()])) {
            listener.onStartExam(this);
        }
    }

    private void notifyExamFinished() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("notifyExamFinished()");
        }
        for (ExamStateListener listener : examStateListeners.toArray(new ExamStateListener[examStateListeners.size()])) {
            listener.onFinishExam(this);
        }
    }

    private void ensureState() {
        Validate.isTrue(keysTable.length == keysStatistic.size());
        Validate.isTrue(keysTable.length > 0);
        Validate.notNull(random);
        Validate.isTrue(cursor >= 0);
        Validate.isTrue(cursor <= examSize);
        Validate.isTrue(examInterval > 0);
    }

    private void initRandomGen() {
        random = new Random();
    }

    private String randomKey() {
        ensureState();
        int key = random.nextInt(keysTable.length);
        if (LOG.isTraceEnabled()) {
            LOG.trace("randomKey()" + key);
        }
        return keysTable[key];
    }

    public static enum ExamEvent {FAILURE, CORRECT, TIMEOUT}

    public static interface ExamStateListener {
        void onStartExam(KeyBoardPracticeModel keyBoardPracticeModel);

        void onFinishExam(KeyBoardPracticeModel keyBoardPracticeModel);
    }

    public static class ExamConfiguration {
        public final int examSize;

        public ExamConfiguration(int examSize) {
            this.examSize = examSize;
        }
    }

    class Exam {
        public final String key;
        private final int interval;
        private String input;
        private boolean examed;

        Exam(String key, int interval) {
            Validate.isTrue(StringUtils.isNotEmpty(key));
            this.key = key;
            this.interval = interval;
        }

        @Override
        public String toString() {
            return "Exam{" +
                    "key='" + key + '\'' +
                    ", interval=" + interval +
                    ", input='" + input + '\'' +
                    ", examed=" + examed +
                    '}';
        }

        public String getInput() {
            return input;
        }

        public void waitForInput() {
            if (LOG2.isDebugEnabled()) {
                LOG2.debug("waitForInput(): enter");
            }
            synchronized (this) {
                try {
                    wait(TimeUnit.SECONDS.toMillis(interval));
                } catch (InterruptedException e) {
                    //Ignore
                }
                if (!examed) {
                    onExamTimeout(this);
                }
            }

            synchronized (this) {
                if (!hasNext()) {
                    examStarted = false;
                }
                notifyAll();
            }
            if (LOG2.isDebugEnabled()) {
                LOG2.debug("waitForInput(): exit");
            }
        }


        public void exam(String input) {
            Validate.isTrue(!examed);
            this.input = input;
            examed = true;

            boolean bingo = key.equals(input);
            if (bingo) {
                onExamSucceed(this);
            } else {
                onExamFailed(this);
            }

            synchronized (this) {
                try {
                    notifyAll();
                    wait();
                } catch (InterruptedException e) {
                    //Ignore
                }
            }
            if (LOG2.isDebugEnabled()) {
                LOG2.debug("exam(): input: " + input + "bingo:" + bingo);
            }
        }
    }

    public class Result {
        /**
         * Score = total correction / total occurs
         */
        public final float score;
        public final float correction;
        public final float failure;
        public final float timeout;
        public final long durationInSeconds;

        private Result(float totalOccurs, float totalCorrections, float totalTimeout, long durationInMillis) {
            this.correction = totalCorrections;
            this.failure = totalOccurs - totalCorrections - totalTimeout;
            this.timeout = totalTimeout;
            this.durationInSeconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis);
            this.score = totalCorrections / totalOccurs;
            Validate.isTrue(score <= 1 && score >= 0);
        }

        @Override
        public String toString() {
            return String.format("Result{score:%s, statistic:%s}", score, keysStatistic);
        }
    }

    class Statistic {
        int occurs;
        int bingos;
        int timeout;

        @Override
        public String toString() {
            return "Statistic{" +
                    "occurs=" + occurs +
                    ", bingos=" + bingos +
                    ", timeout=" + timeout +
                    '}';
        }
    }
}
