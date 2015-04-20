package org.quantumlabs.havefun.keyboadpractice;

/**
 * Created by quintus on 4/17/15.
 */
public interface IInputDelegater extends ISystemHandler {

    public void sendInputKey(String input, IDisplayView displayView);
}
