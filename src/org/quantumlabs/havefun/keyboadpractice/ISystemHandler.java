package org.quantumlabs.havefun.keyboadpractice;

import java.util.function.Function;

/**
 * Created by quintus on 4/17/15.
 */
public interface ISystemHandler {

    boolean isStarted();

    void registerSystemListener(Function<ISystemHandler,Void> listener);
}
