/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2024 Martin Berglund
 */
package com.googlecode.lanterna.terminal.ansi;

import com.googlecode.lanterna.TerminalSize;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * UnixLikeTerminal extends from ANSITerminal and defines functionality that is common to
 *  {@code UnixTerminal} and {@code CygwinTerminal}, like setting tty modes; echo, cbreak
 *  and minimum characters for reading as well as a shutdown hook to set the tty back to
 *  original state at the end.
 * <p>
 *  If requested, it handles Control-C input to terminate the program, and hooks
 *  into Unix WINCH signal to detect when the user has resized the terminal,
 *  if supported by the JVM.
 *
 * @author Andreas
 * @author Martin
 */
public abstract class UnixLikeTTYTerminal extends UnixLikeTerminal {

    private final File ttyDev;
    private String sttyStatusToRestore;

    /**
     * Creates a UnixTerminal using a specified input stream, output stream and character set, with a custom size
     * querier instead of using the default one. This way you can override size detection (if you want to force the
     * terminal to a fixed size, for example). You also choose how you want ctrl+c key strokes to be handled.
     *
     * @param ttyDev TTY device file that is representing this terminal session, will be used when calling stty to make
     *               it operate on this session
     * @param terminalInput Input stream to read terminal input from
     * @param terminalOutput Output stream to write terminal output to
     * @param terminalCharset Character set to use when converting characters to bytes
     * @param terminalCtrlCBehaviour Special settings on how the terminal will behave, see {@code UnixTerminalMode} for
     *                               more details
     * @throws IOException If there was an I/O error while setting up the terminal
     */
    protected UnixLikeTTYTerminal(
            File ttyDev,
            InputStream terminalInput,
            OutputStream terminalOutput,
            Charset terminalCharset,
            CtrlCBehaviour terminalCtrlCBehaviour) throws IOException {

        super(terminalInput,
                terminalOutput,
                terminalCharset,
                terminalCtrlCBehaviour);

        this.ttyDev = ttyDev;

        // Take ownership of the terminal
        realAcquire();
    }

    @Override
    protected void acquire() throws IOException {
        // Hack!
    }

    private void realAcquire() throws IOException {
        super.acquire();
    }

    @Override
    protected void registerTerminalResizeListener(final Runnable onResize) throws IOException {
        try {
            Class<?> signalClass = Class.forName("sun.misc.Signal");
            for(Method m : signalClass.getDeclaredMethods()) {
                if("handle".equals(m.getName())) {
                    Object windowResizeHandler = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Class.forName("sun.misc.SignalHandler")}, (proxy, method, args) -> {
                        if("handle".equals(method.getName())) {
                            onResize.run();
                        }
                        return null;
                    });
                    m.invoke(null, signalClass.getConstructor(String.class).newInstance("WINCH"), windowResizeHandler);
                }
            }
        }
        catch(Throwable ignore) {
            // We're probably running on a non-Sun JVM and there's no way to catch signals without resorting to native
            // code integration
        }
    }

    /**
     * Local Unix terminals already expose their dimensions through {@code stty size}; using that
     * channel avoids the ANSI cursor-position request used by {@link ANSITerminal}, which can wait
     * for several seconds when a terminal, multiplexer, or test PTY does not answer DSR queries.
     * The ANSI path remains the compatibility fallback for unusual {@code stty} implementations.
     */
    @Override
    protected TerminalSize findTerminalSize() throws IOException {
        try {
            TerminalSize size = parseSTTYSize(runSTTYCommand("size"));
            if (size != null) {
                return size;
            }
        }
        catch (IOException ignored) {
            // Fall through to the ANSI cursor-report query for compatibility.
        }
        return super.findTerminalSize();
    }

    static TerminalSize parseSTTYSize(String output) {
        if (output == null) {
            return null;
        }
        String trimmed = output.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length != 2) {
            return null;
        }
        try {
            int rows = Integer.parseInt(parts[0]);
            int columns = Integer.parseInt(parts[1]);
            if (rows <= 0 || columns <= 0) {
                return null;
            }
            return new TerminalSize(columns, rows);
        }
        catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    protected void saveTerminalSettings() throws IOException {
        sttyStatusToRestore = runSTTYCommand("-g").trim();
    }

    @Override
    protected void restoreTerminalSettings() throws IOException {
        if(sttyStatusToRestore != null) {
            runSTTYCommand(sttyStatusToRestore);
        }
    }

    @Override
    protected void keyEchoEnabled(boolean enabled) throws IOException {
        runSTTYCommand(enabled ? "echo" : "-echo");
    }

    @Override
    protected void canonicalMode(boolean enabled) throws IOException {
        runSTTYCommand(enabled ? "icanon" : "-icanon");
        if(!enabled) {
            runSTTYCommand("min", "1");
            // Also disable icrnl so kernel does not translate CR -> LF on
            // input. Without this, plain Enter (which the terminal sends as
            // \r) and Shift+Enter (bound to Send Text \n) both arrive as LF
            // and become indistinguishable at the KeyDecodingProfile layer.
            runSTTYCommand("-icrnl");
            // Also disable XON/XOFF flow control so Ctrl+S / Ctrl+Q reach the
            // application as key strokes instead of freezing terminal output.
            // Node's setRawMode (cfmakeraw) clears IXON the same way; without
            // this an app-level Ctrl+S binding (e.g. "copy screenshot") can
            // never fire. The "stty -g" snapshot taken in
            // saveTerminalSettings restores the user's flow-control state on
            // exit.
            runSTTYCommand("-ixon");
            // The remaining cfmakeraw input flags. IEXTEN is the one that bites:
            // it keeps VLNEXT alive, so the tty swallows Ctrl+V and passes the
            // following byte through literally. An application binding for
            // Ctrl+V (paste an image, preview a session) then sees a bare 'v'
            // with no ctrl modifier and can never fire. INLCR and BRKINT are
            // cleared for the same reason as ICRNL: the kernel must not rewrite
            // or synthesise input the key decoder is trying to classify.
            runSTTYCommand("-iexten");
            runSTTYCommand("-inlcr");
            runSTTYCommand("-brkint");
        }
    }

    @Override
    protected void keyStrokeSignalsEnabled(boolean enabled) throws IOException {
        // Toggle ISIG rather than blanking intr/susp/dsusp one by one. Undefining
        // the characters reaches the same end for the three we know about, but it
        // edits the user's terminal instead of switching off the feature that
        // consumes them, and it silently leaves behind anything not on the list.
        // cfmakeraw - which is what every other full-screen terminal application
        // applies here - clears the flag. Doing the same keeps the characters at
        // their own values, so they come back by themselves when ISIG returns.
        runSTTYCommand(enabled ? "isig" : "-isig");
    }

    protected String runSTTYCommand(String... parameters) throws IOException {
        List<String> commandLine = new ArrayList<>(Arrays.asList(
                getSTTYCommand()));
        commandLine.addAll(Arrays.asList(parameters));
        return exec(commandLine.toArray(new String[0]));
    }

    protected String exec(String... cmd) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (ttyDev != null) {
            pb.redirectInput(ProcessBuilder.Redirect.from(ttyDev));
        }
        Process process = pb.start();
        ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
        InputStream stdout = process.getInputStream();
        int readByte = stdout.read();
        while(readByte >= 0) {
            stdoutBuffer.write(readByte);
            readByte = stdout.read();
        }
        ByteArrayInputStream stdoutBufferInputStream = new ByteArrayInputStream(stdoutBuffer.toByteArray());
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdoutBufferInputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    protected String[] getSTTYCommand() {
        String sttyOverride = System.getProperty("com.googlecode.lanterna.terminal.UnixTerminal.sttyCommand");
        if (sttyOverride != null) {
            return new String[] { sttyOverride };
        }
        else {
            // Issue #519: this will hopefully be more portable across linux distributions
            // Previously we hard-coded "/bin/stty" here
            return new String[] {
                    "/usr/bin/env",
                    "stty"
            };
        }
    }
}
