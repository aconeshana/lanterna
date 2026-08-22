package com.googlecode.lanterna.gui2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SeparateTextGUIThreadTest {

    @Test
    public void inputDrivenFramesAreNotDelayedByBackgroundFrameThrottling() {
        assertEquals(0, SeparateTextGUIThread.frameDelayMillis(true, 0));
        assertEquals(12, SeparateTextGUIThread.frameDelayMillis(false, 4));
        assertEquals(0, SeparateTextGUIThread.frameDelayMillis(false, 16));
    }
}
