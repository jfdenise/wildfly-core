/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.cli.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import org.aesh.readline.Prompt;
import org.aesh.readline.Readline;
import org.aesh.readline.ReadlineFlag;
import org.aesh.readline.action.ActionDecoder;
import org.aesh.readline.alias.AliasCompletion;
import org.aesh.readline.alias.AliasManager;
import org.aesh.readline.alias.AliasPreProcessor;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.history.FileHistory;
import org.aesh.terminal.Attributes;
import org.aesh.utils.ANSI;
import org.aesh.utils.Config;
import org.aesh.util.FileAccessPermission;
import org.aesh.util.Parser;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.CompletionHandler;
import org.aesh.readline.editing.EditModeBuilder;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Signal;
import org.jboss.as.cli.CommandHistory;
import org.jboss.logging.Logger;

/**
 * Integration point with aesh-readline. There are multiple paths when the CLI
 * exits.
 * <ul>
 * <li>quit command: Command ctx is terminated, console is closed, terminal
 * connection reading thread is interrupted, Main thread exit, jvm exit handlers
 * are called</li>
 * <li>Ctrl-C: can be received as an OS signal or parsed by aesh. In both cases
 * interruptHandler is called, the connection is closed, terminal connection
 * reading thread is interrupted, Main thread exit, jvm exit handlers are
 * called. If a prompt is in progress, the reading thread will be interrupted
 * too.</li>
 * <li>Ctrl-D (without characters typed): Only parsed by aesh, no native signal
 * raised. In both cases, the connection is closed by aesh, terminal connection
 * reading thread is interrupted, Main thread exit, jvm exit handlers are
 * called. If a prompt is in progress, the reading thread will be interrupted
 * too. Because Ctrl-D is not a native signal, it can't be handled during the
 * execution of a command. Only Ctrl-C can be used to interrupt the CLI.</li>
 * </ul>
 *
 * @author jdenise@redhat.com
 */
public abstract class ReadlineConsole {

    protected static final Logger LOG = Logger.getLogger(ReadlineConsole.class.getName());
    protected static final boolean isTraceEnabled = LOG.isTraceEnabled();

    public interface Settings {

        /**
         * @return the inStream
         */
        InputStream getInStream();

        /**
         * @return the outStream
         */
        OutputStream getOutStream();

        /**
         * @return the disableHistory
         */
        boolean isDisableHistory();

        /**
         * @return the outputRedefined
         */
        boolean isOutputRedefined();

        /**
         * @return the historyFile
         */
        File getHistoryFile();

        /**
         * @return the historySize
         */
        int getHistorySize();

        /**
         * @return the permission
         */
        FileAccessPermission getPermission();

        /**
         * @return the interrupt
         */
        Runnable getInterrupt();
    }

    private static class SettingsImpl implements Settings {

        private final InputStream inStream;
        private final OutputStream outStream;
        private final boolean disableHistory;
        private final File historyFile;
        private final int historySize;
        private final FileAccessPermission permission;
        private final Runnable interrupt;
        private final boolean outputRedefined;

        private SettingsImpl(InputStream inStream,
                OutputStream outStream,
                boolean outputRedefined,
                boolean disableHistory,
                File historyFile,
                int historySize,
                FileAccessPermission permission,
                Runnable interrupt) {
            this.inStream = inStream;
            this.outStream = outStream;
            this.outputRedefined = outputRedefined;
            this.disableHistory = disableHistory;
            this.historyFile = historyFile;
            this.historySize = historySize;
            this.permission = permission;
            this.interrupt = interrupt;
        }

        /**
         * @return the inStream
         */
        @Override
        public InputStream getInStream() {
            return inStream;
        }

        /**
         * @return the outStream
         */
        @Override
        public OutputStream getOutStream() {
            return outStream;
        }

        /**
         * @return the disableHistory
         */
        @Override
        public boolean isDisableHistory() {
            return disableHistory;
        }

        /**
         * @return the outputRedefined
         */
        @Override
        public boolean isOutputRedefined() {
            return outputRedefined;
        }

        /**
         * @return the historyFile
         */
        @Override
        public File getHistoryFile() {
            return historyFile;
        }

        /**
         * @return the historySize
         */
        @Override
        public int getHistorySize() {
            return historySize;
        }

        /**
         * @return the permission
         */
        @Override
        public FileAccessPermission getPermission() {
            return permission;
        }

        /**
         * @return the interrupt
         */
        @Override
        public Runnable getInterrupt() {
            return interrupt;
        }
    }

    public static class SettingsBuilder {

        private InputStream inStream;
        private OutputStream outStream;
        private boolean disableHistory;
        private File historyFile;
        private int historySize;
        private FileAccessPermission permission;
        private Runnable interrupt;
        private boolean outputRedefined;

        public SettingsBuilder inputStream(InputStream inStream) {
            this.inStream = inStream;
            return this;
        }

        public SettingsBuilder outputStream(OutputStream outStream) {
            this.outStream = outStream;
            return this;
        }

        public SettingsBuilder disableHistory(boolean disableHistory) {
            this.disableHistory = disableHistory;
            return this;
        }

        public SettingsBuilder historyFile(File historyFile) {
            this.historyFile = historyFile;
            return this;
        }

        public SettingsBuilder historySize(int historySize) {
            this.historySize = historySize;
            return this;
        }

        public SettingsBuilder historyFilePermission(FileAccessPermission permission) {
            this.permission = permission;
            return this;
        }

        public SettingsBuilder interruptHook(Runnable interrupt) {
            this.interrupt = interrupt;
            return this;
        }

        public SettingsBuilder outputRedefined(boolean outputRedefined) {
            this.outputRedefined = outputRedefined;
            return this;
        }

        public Settings create() {
            return new SettingsImpl(inStream, outStream, outputRedefined,
                    disableHistory, historyFile, historySize, permission, interrupt);
        }
    }

    class HistoryImpl implements CommandHistory {

        @Override
        public List<String> asList() {
            List<String> lst = new ArrayList<>();
            for (int[] l : readlineHistory.getAll()) {
                lst.add(Parser.stripAwayAnsiCodes(Parser.fromCodePoints(l)));
            }
            return lst;
        }

        @Override
        public boolean isUseHistory() {
            return readlineHistory.isEnabled();
        }

        @Override
        public void setUseHistory(boolean useHistory) {
            if (useHistory) {
                readlineHistory.enable();
            } else {
                readlineHistory.disable();
            }

        }

        @Override
        public void clear() {
            readlineHistory.clear();
        }

        @Override
        public int getMaxSize() {
            return readlineHistory.size();
        }
    }

    private final List<Completion> completions = new ArrayList<>();
    private Readline readline;
    private final CommandHistory history = new HistoryImpl();
    private final FileHistory readlineHistory;
    private Prompt prompt;
    protected final Settings settings;
    protected volatile boolean started;
    protected volatile boolean closed;
    protected Thread readingThread;
    private Consumer<String> callback;

    private final ExecutorService executor = Executors.newFixedThreadPool(1,
            (r) -> new Thread(r, "CLI command"));
    private StringBuilder outputCollector;

    private final AliasManager aliasManager;
    private final List<Function<String, Optional<String>>> preProcessors = new ArrayList<>();

    protected static final EnumMap<ReadlineFlag, Integer> READLINE_FLAGS = new EnumMap<>(ReadlineFlag.class);

    static {
        READLINE_FLAGS.put(ReadlineFlag.NO_PROMPT_REDRAW_ON_INTR, Integer.MAX_VALUE);
    }

    protected Consumer<Signal> interruptHandler;

    protected ReadlineConsole(Settings settings) throws IOException {
        this.settings = settings;
        readlineHistory = new FileHistory(settings.getHistoryFile(),
                settings.getHistorySize(), settings.getPermission(), false);
        if (settings.isDisableHistory()) {
            readlineHistory.disable();
        } else {
            readlineHistory.enable();
        }
        if (isTraceEnabled) {
            LOG.tracef("History is enabled? %s", !settings.isDisableHistory());
        }
        aliasManager = new AliasManager(new File(Config.getHomeDir()
                + Config.getPathSeparator() + ".aesh_aliases"), true);
        AliasPreProcessor aliasPreProcessor = new AliasPreProcessor(aliasManager);
        preProcessors.add(aliasPreProcessor);
        completions.add(new AliasCompletion(aliasManager));
        readline = new Readline();
    }

    public void setActionCallback(Consumer<String> callback) {
        this.callback = callback;
    }

    private boolean isSystemTerminal() {
        return getConnection() != null && getConnection().supportsAnsi();
    }
    /**
     * This has the side effect to create the internal readline instance.
     *
     * @param ch The Completion Handler.
     */
    public void setCompletionHandler(CompletionHandler<? extends CompleteOperation> ch) {
        readline = new Readline(EditModeBuilder.builder().create(), null, ch);
    }

    protected Readline getReadLine() {
        if (readline == null) {
            readline = new Readline();
        }
        return readline;
    }

    public void addCompleter(Completion<? extends CompleteOperation> completer) {
        completions.add(completer);
    }

    public CommandHistory getHistory() {
        return history;
    }

    public abstract Connection getConnection();

    public void clearScreen() {
        if (getConnection() != null) {
            getConnection().stdoutHandler().accept(ANSI.CLEAR_SCREEN);
        }
    }

    public String formatColumns(Collection<String> list) {
        String[] newList = new String[list.size()];
        list.toArray(newList);
        return Parser.formatDisplayList(newList,
                getHeight(),
                getWidth());
    }

    public void print(String line, boolean collect) {
        LOG.tracef("Print %s", line);
        if (collect && outputCollector != null) {
            outputCollector.append(line);
        } else if (getConnection() == null) {
            OutputStream out = settings.getOutStream() == null ? System.out : settings.getOutStream();
            try {
                out.write(line.getBytes());
            } catch (IOException ex) {
                LOG.tracef("Print exception %s", ex);
            }
        } else {
            getConnection().write(line);
        }
    }

    public Key readKey() throws InterruptedException, IOException {
        return Key.findStartKey(read());
    }

    // handle "a la" 'more' scrolling
    // Doesn't take into account wrapped lines (lines that are longer than the
    // terminal width. This could make a page to skip some lines.
    private void printCollectedOutput() {
        if (outputCollector == null) {
            return;
        }
        try {
            String line = outputCollector.toString();
            if (line.isEmpty()) {
                return;
            }
            // '\R' will match any line break.
            // -1 to keep empty lines at the end of content.
            String[] lines = line.split("\\R", -1);
            int max = getConnection().size().getHeight();
            int currentLines = 0;
            int allLines = 0;
            while (allLines < lines.length) {
                if (currentLines > max - 2) {
                    try {
                        getConnection().write(ANSI.CURSOR_SAVE);
                        int percentage = (allLines * 100) / lines.length;
                        getConnection().write("--More(" + percentage + "%)--");
                        Key k = readKey();
                        getConnection().write(ANSI.CURSOR_RESTORE);
                        getConnection().stdoutHandler().accept(ANSI.ERASE_LINE_FROM_CURSOR);
                        if (k == null) { // interrupted, exit.
                            allLines = lines.length;
                        } else {
                            switch (k) {
                                case SPACE: {
                                    currentLines = 0;
                                    break;
                                }
                                case ENTER:
                                case CTRL_M: { // On Mac, CTRL_M...
                                    currentLines -= 1;
                                    break;
                                }
                                case q: {
                                    allLines = lines.length;
                                    break;
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    String l = lines[allLines];
                    currentLines += 1;
                    allLines += 1;
                    // Do not add an extra \n
                    // The \n has been added by the previous line.
                    if (allLines == lines.length) {
                        if (l.isEmpty()) {
                            continue;
                        }
                    }
                    getConnection().write(l + Config.getLineSeparator());
                }
            }
        } finally {
            outputCollector = null;
        }
    }

    protected abstract void initializeConnection() throws IOException;

    public int[] read() throws InterruptedException, IOException {
        initializeConnection();
        ActionDecoder decoder = new ActionDecoder();
        final int[][] key = {null};
        // Keep a reference on the caller thread in case Ctrl-C is pressed
        // and thread needs to be interrupted.
        readingThread = Thread.currentThread();
        // We need to set the interrupt SignalHandler to interrupt the CLI.
        Consumer<Signal> prevHandler = getConnection().getSignalHandler();
        getConnection().setSignalHandler(interruptHandler);
        CountDownLatch latch = new CountDownLatch(1);
        Attributes attributes = getConnection().enterRawMode();
        getConnection().setStdinHandler(keys -> {
            decoder.add(keys);
            if (decoder.hasNext()) {
                key[0] = decoder.next().buffer().array();
                latch.countDown();
            }
        });
        try {
            // Wait until interrupted
            latch.await();
        } finally {
            getConnection().setStdinHandler(null);
            getConnection().setSignalHandler(prevHandler);
            readingThread = null;
        }
        return key[0];
    }

    public void printNewLine(boolean collect) {
        print(Config.getLineSeparator(), collect);
    }

    public String readLine(String prompt) throws IOException, InterruptedException {
        return readLine(prompt, null);
    }

    /**
     * Prompt a user. The complexity of this method is implied by the Ctrl-C
     * handling. When Ctrl-C occurs, the Exit hook will call this.close that
     * interrupts the thread calling this operation.<p>
     * We have 2 cases.
     * <p>
     * 1) prompting prior to have started the console: - Must start a new
     * connection. - Make it non blocking. - Wait on latch to resync and to
     * catch Thread.interrupt.
     * <p>
     * 2) prompting after to have started the console: - No need to open the
     * connection. - Wait on latch to resync and to catch Thread.interrupt.
     *
     * @param prompt
     * @param mask
     * @return
     * @throws IOException
     */
    public String readLine(String prompt, Character mask) throws InterruptedException, IOException {
        logPromptMask(prompt, mask);
        return readLine(new Prompt(prompt, mask));
    }

    protected abstract String doReadLine(Prompt prompt) throws InterruptedException, IOException;

    public String readLine(Prompt prompt) throws InterruptedException, IOException {
        // If there are some output collected, flush it.
        printCollectedOutput();
        // New collector
        outputCollector = createCollector();

        // Keep a reference on the caller thread in case Ctrl-C is pressed
        // and thread needs to be interrupted.
        readingThread = Thread.currentThread();
        try {
            return doReadLine(prompt);
        } finally {
            readingThread = null;
        }
    }

    private StringBuilder createCollector() {
        if (!isSystemTerminal()) {
            return null;
        }
        return new StringBuilder();
    }


    private void logPromptMask(String prompt, Character mask) {
        LOG.tracef("Prompt %s mask %s", prompt, mask);
    }

    public int getTerminalWidth() {
        return getWidth();
    }

    public int getTerminalHeight() {
        return getHeight();
    }

    private int getHeight() {
        if (getConnection() == null) {
            return 40;
        }
        return getConnection().size().getHeight();
    }

    private int getWidth() {
        if (getConnection() == null) {
            return 80;
        }
        return getConnection().size().getWidth();
    }

    public void start() throws IOException {
        if (closed) {
            throw new IllegalStateException("Console has already been closed");
        }
        doStart();
    }

    protected abstract void doStart() throws IOException;

    protected void loop() {
        try {
            if (isTraceEnabled) {
                LOG.tracef("Set a readline callback with prompt %s", prompt);
            }
            // Console could have been closed during a command execution.
            if (!closed) {
                getReadLine().readline(getConnection(), prompt, line -> {
                    // All commands can lead to prompting the user. So require
                    // to be executed in a dedicated thread.
                    // This can happen if a security configuration occurs
                    // on the remote server.
                    LOG.tracef("Executing command %s in a new thread.", line);
                    if (line == null || line.trim().length() == 0 || handleAlias(line)) {
                        loop();
                        return;
                    }
                    executor.submit(() -> {
                        Consumer<Signal> handler = getConnection().getSignalHandler();
                        Thread callingThread = Thread.currentThread();
                        getConnection().setSignalHandler((signal) -> {
                            // Interrupting the current command thread.
                            switch (signal) {
                                case INT: {
                                    LOG.tracef("Interrupting command: %s", line);
                                    callingThread.interrupt();
                                }
                            }
                        });
                        try {
                            outputCollector = createCollector();
                            callback.accept(line);
                        } catch (Throwable thr) {
                            getConnection().write("Unexpected exception");
                            thr.printStackTrace();
                        } finally {
                            printCollectedOutput();
                            // The current thread could have been interrupted.
                            // Clear the flag to safely interact with aesh-readline
                            Thread.interrupted();
                            getConnection().setSignalHandler(handler);
                            LOG.tracef("Done Executing command %s", line);
                            loop();
                        }
                    });
                }, completions, preProcessors, readlineHistory, null, READLINE_FLAGS);
            }
        } catch (Exception ex) {
            getConnection().write("Unexpected exception");
            ex.printStackTrace();
        }
    }

    private boolean handleAlias(String line) {
        if (line.startsWith("alias ") || line.equals("alias")) {
            String out = aliasManager.parseAlias(line.trim());
            if (out != null) {
                print(out, false);
            }
            return true;
        } else if (line.startsWith("unalias ") || line.equals("unalias")) {
            String out = aliasManager.removeAlias(line.trim());
            if (out != null) {
                print(out, false);
            }
            return true;
        }
        return false;
    }

    public void stop() {
        if (!closed) {
            LOG.trace("Stopping.");

            closed = true;
            if (readingThread != null) {
                LOG.trace("Interrupting reading thread");
                readingThread.interrupt();
            }
            if (started) {
                readlineHistory.stop();
                aliasManager.persist();
            }
            executor.shutdown();
            if (getConnection() != null) {
                getConnection().close();
            }
        }
    }

    public boolean running() {
        return started;
    }

    public void setPrompt(String prompt) {
        this.prompt = new Prompt(prompt);
    }

    public void setPrompt(Prompt prompt) {
        this.prompt = prompt;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public String handleBuiltins(String line) {
        if (handleAlias(line)) {
            return null;
        }
        return parse(line);
    }

    private String parse(String line) {
        Optional<String> out = aliasManager.getAliasName(line);
        if (out.isPresent()) {
            line = out.get();
        }
        return line;
    }
}
