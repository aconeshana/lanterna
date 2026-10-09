/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 */
package com.googlecode.lanterna.terminal;

import java.io.IOException;

/**
 * Implemented by terminals that track whether they currently hold the alternate screen buffer
 * and can put themselves back into it.
 * <p>
 * {@link Terminal#enterPrivateMode()} is a one-shot transition: it refuses to run a second time,
 * because calling it twice normally means the caller lost track of its own state. That leaves no
 * way to recover from the case where the terminal left private mode without the application
 * asking, which does happen in practice — a shutdown hook that runs while the process keeps
 * going, a job-control stop, or another program that took the terminal and handed back an
 * alternate screen that is no longer active. An application that keeps painting absolute row
 * positions into the main buffer scrolls it, and every scroll pushes a row of the current frame
 * into the terminal's scrollback.
 * <p>
 * This interface is deliberately not part of {@link Terminal}: it describes an optional
 * capability, and terminals with no notion of private mode have nothing to report. Decorators
 * that wrap a terminal should implement it by delegating, otherwise the capability is invisible
 * to anything holding the decorator.
 *
 * @see Terminal#enterPrivateMode()
 */
public interface PrivateModeTerminal {

    /**
     * Returns whether the terminal believes it currently holds the alternate screen buffer.
     * <p>
     * This is the library's own bookkeeping, not a query of the physical terminal: it answers
     * "did a call to {@code enterPrivateMode} happen without a matching {@code exitPrivateMode}".
     *
     * @return True if private mode was entered and not since left
     */
    boolean isInPrivateMode();

    /**
     * Re-asserts the alternate screen buffer whether or not this terminal thinks it already has
     * it, and restores the mouse-capture mode that went with it.
     * <p>
     * Unlike {@link Terminal#enterPrivateMode()} this is idempotent and never throws on a
     * terminal that is already in private mode, which is what makes it usable as a repair. The
     * alternate screen it switches to has undefined contents, so the caller is expected to
     * follow up with a complete repaint.
     *
     * @throws IOException If there was an underlying I/O error
     */
    void reassertPrivateMode() throws IOException;
}
