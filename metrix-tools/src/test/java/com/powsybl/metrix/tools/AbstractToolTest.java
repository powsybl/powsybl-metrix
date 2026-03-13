/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.tools;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.CompletableFutureTask;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultExecutionReport;
import com.powsybl.computation.ExecutionEnvironment;
import com.powsybl.computation.ExecutionHandler;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.tools.Command;
import com.powsybl.tools.CommandLineTools;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolInitializationContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public abstract class AbstractToolTest {

    protected FileSystem fileSystem;

    protected InMemoryPlatformConfig platformConfig;

    private CommandLineTools tools;

    @BeforeEach
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        tools = new CommandLineTools(getTools());
    }

    @AfterEach
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    protected abstract Iterable<Tool> getTools();

    private void assertMatches(String expected, String actual) {
        //The empty string is matched exactly, other strings as regexs
        if (!actual.equals(expected) && ("".equals(expected) || !Pattern.compile(expected).matcher(actual).find())) {
            assertEquals(expected, actual);
        }
    }

    protected void assertCommand(String[] args, int expectedStatus, String expectedOut, String expectedErr) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream berr = new ByteArrayOutputStream();
        int status;
        try (PrintStream out = new PrintStream(bout);
            PrintStream err = new PrintStream(berr)) {
            LocalComputationConfig localComputationConfig = LocalComputationConfig.load(platformConfig, fileSystem);
            LocalCommandExecutor commandExecutor = new LocalCommandExecutor() {
                @Override
                public int execute(String s, List<String> list, Path path, Path path1, Path path2, Map<String, String> map) {
                    return CommandLineTools.COMMAND_OK_STATUS;
                }

                @Override
                public void stop(Path path) {
                    // Nothing to do here
                }

                @Override
                public void stopForcibly(Path path) {
                    // Nothing to do here
                }
            };
            ComputationManager computationManager = new LocalComputationManager(localComputationConfig, commandExecutor, Executors.newSingleThreadExecutor()) {
                @Override
                public <R> CompletableFuture<R> execute(ExecutionEnvironment executionEnvironment, ExecutionHandler<R> executionHandler) {
                    return CompletableFutureTask.runAsync(() -> {
                        try {
                            Path path = fileSystem.getPath("/working-dir");
                            Files.createDirectory(path);
                            return executionHandler.after(path, new DefaultExecutionReport(path));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }, ForkJoinPool.commonPool());

                }
            };
            ToolInitializationContext toolInitializationContext = new ToolInitializationContext() {
                @Override
                public PrintStream getOutputStream() {
                    return out;
                }

                @Override
                public PrintStream getErrorStream() {
                    return err;
                }

                @Override
                public Options getAdditionalOptions() {
                    return new Options();
                }

                @Override
                public FileSystem getFileSystem() {
                    return fileSystem;
                }

                @Override
                public ComputationManager createShortTimeExecutionComputationManager(CommandLine commandLine) {
                    return computationManager;
                }

                @Override
                public ComputationManager createLongTimeExecutionComputationManager(CommandLine commandLine) {
                    return computationManager;
                }
            };
            status = tools.run(args, toolInitializationContext);
        }
        if (expectedErr != null) {
            assertMatches(expectedErr, berr.toString(StandardCharsets.UTF_8));
        }
        assertEquals(expectedStatus, status);
        if (expectedOut != null) {
            assertMatches(expectedOut, bout.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    public abstract void assertCommand();

    protected void assertCommand(Command command, String commandName, int optionCount, int requiredOptionCount) {
        assertEquals(commandName, command.getName());
        assertEquals(optionCount, command.getOptions().getOptions().size());
        assertEquals(requiredOptionCount, command.getOptions().getRequiredOptions().size());
    }

    protected void assertOption(Options options, String optionName, boolean isRequired, boolean hasArgument) {
        Option option = options.getOption(optionName);
        assertNotNull(option);
        assertEquals(isRequired, option.isRequired());
        assertEquals(hasArgument, option.hasArg());
    }
}
