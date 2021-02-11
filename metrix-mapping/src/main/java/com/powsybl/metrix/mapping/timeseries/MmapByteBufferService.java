/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.powsybl.computation.local.LocalComputationConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Memory mapped file buffer allocation service.
 * Main purpose of this utility class is to overcome a Java issue on Windows with memory mapped file.
 * Once Random access file is closed, file cannot be delete until buffer has been garbage collected.
 * The workaround used here is to start a "cleaner" thread that try to delete the file every minute.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MmapByteBufferService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MmapByteBufferService.class);

    public static final MmapByteBufferService INSTANCE = new MmapByteBufferService();

    private static final class BufferContext {

        private File file;

        private RandomAccessFile raf;

        private BufferContext(File file, RandomAccessFile raf) {
            this.file = file;
            this.raf = raf;
        }
    }

    private final Map<String, BufferContext> contexts = new HashMap<>();

    private final Lock lock = new ReentrantLock();

    public MmapByteBufferService() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("MMAP_CLEANER-%d")
                .build());
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                for (Iterator<Map.Entry<String, BufferContext>> it = contexts.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, BufferContext> e = it.next();
                    BufferContext context = e.getValue();
                    // only try to delete file already closed
                    if (context.raf == null && tryToDelete(context)) {
                        it.remove();
                    }
                }
            } catch (Throwable t) {
                LOGGER.error(t.toString(), t);
            } finally {
                lock.unlock();
            }
        }, 0L, 1L, TimeUnit.MINUTES);
    }

    public ByteBuffer create(String fileName, int size) {
        Objects.requireNonNull(fileName);
        lock.lock();
        try {
            if (contexts.containsKey(fileName)) {
                throw new IllegalArgumentException("File '" + fileName + "' already opened");
            }
            Path localDir = LocalComputationConfig.load().getLocalDir();
            File file = localDir.resolve(fileName).toFile();
            file.deleteOnExit();
            try {
                BufferContext context = new BufferContext(file, new RandomAccessFile(file, "rw"));

                MappedByteBuffer buffer = context.raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, size);

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Buffer {} allocated ({})", FileUtils.byteCountToDisplaySize(size), file);
                }

                contexts.put(fileName, context);
                return buffer;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean tryToDelete(BufferContext context) {
        try {
            if (context.file.delete()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Buffer {} deleted", context.file);
                }
                context.file = null;
            }
        } catch (Exception e) {
            LOGGER.trace(e.toString(), e);
        }
        if (context.file != null) {
            LOGGER.info("Fail to delete buffer {}, retry later on", context.file);
        }
        return context.file != null;
    }

    public void closeAndTryToDelete(String fileName) {
        Objects.requireNonNull(fileName);
        lock.lock();
        try {
            BufferContext context = contexts.get(fileName);
            if (context == null) {
                throw new IllegalArgumentException("File '" + fileName + "' not found");
            }
            try {
                context.raf.close();
                context.raf = null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Buffer {} closed", context.file);
            }
            if (tryToDelete(context)) {
                contexts.remove(fileName);
            }
        } finally {
            lock.unlock();
        }
    }
}
