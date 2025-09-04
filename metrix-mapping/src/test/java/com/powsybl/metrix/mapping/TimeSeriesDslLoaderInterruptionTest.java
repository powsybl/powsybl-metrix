package com.powsybl.metrix.mapping;

import com.powsybl.computation.AbstractTaskInterruptionTest;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.mapping.util.MappingTestNetwork;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimeSeriesDslLoaderInterruptionTest extends AbstractTaskInterruptionTest {

    private final MappingParameters parameters = MappingParameters.load();
    private final Network network = MappingTestNetwork.create();
    private final ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();

    @ParameterizedTest
    @Timeout(10)
    @Order(1)
    @ValueSource(booleans = {false, true})
    void testCancelTaskJava(boolean isDelayed) throws Exception {
        testCancelLongTask(isDelayed, () -> {
            try {
                Thread.sleep(5000L);
                return 0;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ParameterizedTest
    @Timeout(10)
    @Order(2)
    @ValueSource(booleans = {false, true})
    void testCancelTaskGroovyLong(boolean isDelayed) throws Exception {
        String script = """
                for (int i = 0; i < 10; i++) {
                    sleep(500)
                }
            """;
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        testCancelLongTask(isDelayed, () -> dsl.load(network, parameters, store, new DataTableStore(), null));
    }

    @ParameterizedTest
    @Timeout(10)
    @Order(3)
    @ValueSource(booleans = {false, true})
    void testCancelTaskGroovyShort(boolean isDelayed) throws Exception {
        String script = "writeLog(\"LOG_TYPE\", \"LOG_SECTION\", \"LOG_MESSAGE\")";
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        testCancelShortTask(isDelayed, () -> dsl.load(network, parameters, store, new DataTableStore(), null));
    }
}
