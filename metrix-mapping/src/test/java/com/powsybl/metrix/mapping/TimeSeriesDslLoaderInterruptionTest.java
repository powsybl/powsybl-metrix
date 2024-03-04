package com.powsybl.metrix.mapping;

import com.powsybl.computation.CompletableFutureTask;
import com.powsybl.iidm.network.Network;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimeSeriesDslLoaderInterruptionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesDslLoaderInterruptionTest.class);

    private final MappingParameters parameters = MappingParameters.load();
    private final Network network = MappingTestNetwork.create();
    private final ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();

    // Counters
    CountDownLatch waitForStart;
    CountDownLatch waitForFinish;
    CountDownLatch waitForInterruption;

    // Booleans
    AtomicBoolean config;
    AtomicBoolean interrupted;

    @BeforeEach
    void setup() {
        // Counters
        waitForStart = new CountDownLatch(1);
        waitForFinish = new CountDownLatch(1);
        waitForInterruption = new CountDownLatch(1);

        // Booleans
        config = new AtomicBoolean(false);
        interrupted = new AtomicBoolean(false);
    }

    private void assertions(CompletableFuture<Object> task) throws InterruptedException {

        // This line is used to check that the task has already started
        waitForStart.await();

        // The task should not be done at that point
        assertFalse(task.isDone());

        // Cancel the task
        boolean cancelled = task.cancel(true);

        // Check that the task is cancelled
        assertTrue(cancelled);
        assertTrue(task.isCancelled());

        // Boolean stays at false if the task is cancelled
        assertFalse(config.get());

        // This should throw an exception since the task is cancelled
        assertThrows(CancellationException.class, () -> {
            task.get();
            fail("Should not happen: task has been cancelled");
        });

        // This line should return immediately since the task has been cancelled
        waitForInterruption.await();

        // This boolean is true if the task has been interrupted
        assertTrue(interrupted.get());

        // Second call to cancel should return false
        cancelled = task.cancel(true);
        assertFalse(cancelled);
    }

    @ParameterizedTest
    @Timeout(2)
    @Order(1)
    @ValueSource(booleans= {false, true})
    void testCancelJava(boolean isDelayed) throws Exception {
        CompletableFuture<Object> task = CompletableFutureTask.runAsync(() -> {
            waitForStart.countDown();
            try {
                Thread.sleep(5000);
                config.set(true);
                waitForFinish.countDown();
            } catch (Exception e) { // Thread interrupted => good
                interrupted.set(true);
                waitForInterruption.countDown();
            }
            return null;
        }, Executors.newSingleThreadExecutor());

        // Is asked, wait a bit to simulate interruption by a user
        if (isDelayed) {
            Thread.sleep(800);
        }

        assertions(task);
    }

    @ParameterizedTest
    @Timeout(2)
    @Order(2)
    @ValueSource(booleans= {false, true})
    void testCancelGroovyLongScript(boolean isDelayed) throws Exception {
        String script = """
                            for (int i = 0; i < 10; i++) {
                                sleep(500)
                            }
                        """;
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        CompletableFuture<Object> task = CompletableFutureTask.runAsync(() -> {
            waitForStart.countDown();
            try {
                dsl.load(network, parameters, store, new DataTableStore(), null);
                config.set(true);
                waitForFinish.countDown();
            } catch (Exception e) { // Thread interrupted => good
                interrupted.set(true);
                waitForInterruption.countDown();
            }
            return null;
        }, Executors.newSingleThreadExecutor());

        // Is asked, wait a bit to simulate interruption by a user
        if (isDelayed) {
            Thread.sleep(800);
        }

        assertions(task);
    }

    @ParameterizedTest
    @Timeout(3)
    @ValueSource(booleans= {false, true})
    @Order(3)
    void testCancelGroovyShortScript(boolean isDelayed) throws Exception {
        String script = "writeLog(\"LOG_TYPE\", \"LOG_SECTION\", \"LOG_MESSAGE\")";
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        CompletableFuture<Object> task = CompletableFutureTask.runAsync(() -> {
            waitForStart.countDown();
            try {
                dsl.load(network, parameters, store, new DataTableStore(), null);
                config.set(true);
                waitForFinish.countDown();
            } catch (Exception e) { // Thread interrupted => good
                interrupted.set(true);
                waitForInterruption.countDown();
            }
            return null;
        }, Executors.newFixedThreadPool(3));

        // Is asked, wait a bit to simulate interruption by a user
        if (isDelayed) {
            Thread.sleep(800);

            // This line is used to check that the task has already started
            waitForStart.await();

            // the script was to short to be interrupted before its end so the task is done
            assertTrue(task.isDone());

            // Cancel the task
            boolean cancelled = task.cancel(true);
            assertFalse(cancelled);
        } else {
            // If it's not delayed, the script didn't have enough time to finish yet
            assertions(task);
        }
    }
}
