package org.quantumlabs.havefun.keyboadpractice;

/**
 * Created by quintus on 4/17/15.
 * <p>
 * IDisplay view is responsible for maintenance UI display stuff, UI input dispatching and UI system event dispatching.
 */
public interface IDisplayView {
    void showExam(String key);

    void showMainView(String greetingMessage);

    KeyBoardPracticeModel.ExamConfiguration showConfigurationView(String message);

    void bindInputDelegetor(IInputDelegater delegator);

    void bindControlDelegetor(IControlDelegater controlDelegater);

    void showResult(String result);

    void showTerminating(String message);

    void showOnCorrect(String key);

    void showOnIncorrect(String key, String input);

    void showOnTimeout(String key);

    void unbindInputDelegetor(IInputDelegater inputDelegator);

    void showPreStart();
}
