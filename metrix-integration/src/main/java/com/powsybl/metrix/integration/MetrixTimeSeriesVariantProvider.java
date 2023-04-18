/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.contingency.Probability;
import com.powsybl.metrix.integration.metrix.MetrixChunkParam;
import com.powsybl.metrix.integration.timeseries.InitOptimizedTimeSeriesWriter;
import com.powsybl.metrix.mapping.*;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesTable;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.timeseries.InitOptimizedTimeSeriesWriter.INPUT_OPTIMIZED_FILE_NAME;

public class MetrixTimeSeriesVariantProvider implements MetrixVariantProvider {

    private static final Set<EquipmentVariable> METRIX_EQUIPMENT_VARIABLES = EnumSet.of(EquipmentVariable.targetP,
            EquipmentVariable.minP,
            EquipmentVariable.maxP,
            EquipmentVariable.activePowerSetpoint,
            EquipmentVariable.p0,
            EquipmentVariable.fixedActivePower,
            EquipmentVariable.variableActivePower,
            EquipmentVariable.phaseTapPosition,
            EquipmentVariable.open,
            EquipmentVariable.disconnected);

    public static boolean isMetrixVariable(MappingVariable variable) {
        if (variable instanceof MetrixVariable) {
            return true;
        } else if (variable instanceof EquipmentVariable) {
            return METRIX_EQUIPMENT_VARIABLES.contains(variable);
        }
        return false;
    }

    private final Network network;

    private final ReadOnlyTimeSeriesStore store;

    private final MappingParameters mappingParameters;

    private final ContingenciesProvider contingenciesProvider;

    private final TimeSeriesMappingConfig config;

    private final MetrixDslData metrixDslData;

    private final int version;

    private final Range<Integer> variantRange;

    private final boolean ignoreLimits;

    private final boolean ignoreEmptyFilter;

    private final boolean isNetworkPointComputation;

    private final PrintStream err;

    private final TimeSeriesMapper mapper;

    public MetrixTimeSeriesVariantProvider(Network network, ReadOnlyTimeSeriesStore store, MappingParameters mappingParameters,
                                           TimeSeriesMappingConfig config, MetrixDslData metrixDslData, MetrixChunkParam metrixChunkParam,
                                           Range<Integer> variantRange, PrintStream err) {

        this.network = Objects.requireNonNull(network);
        this.store = Objects.requireNonNull(store);
        this.mappingParameters = Objects.requireNonNull(mappingParameters);
        this.config = Objects.requireNonNull(config);
        this.metrixDslData = metrixDslData;
        this.version = metrixChunkParam.version;
        this.variantRange = variantRange;
        this.ignoreLimits = metrixChunkParam.ignoreLimits;
        this.ignoreEmptyFilter = metrixChunkParam.ignoreEmptyFilter;
        this.isNetworkPointComputation = metrixChunkParam.networkPointFile != null;
        this.contingenciesProvider = metrixChunkParam.contingenciesProvider;
        this.err = Objects.requireNonNull(err);
        mapper = new TimeSeriesMapper(config, network, new TimeSeriesMappingLogger());
    }

    @Override
    public Range<Integer> getVariantRange() {
        return variantRange;
    }

    @Override
    public TimeSeriesIndex getIndex() {
        return new TimeSeriesMappingConfigTableLoader(config, store).checkIndexUnicity();
    }

    @Override
    public Set<String> getMappedBreakers() {
        return config.getBreakerToTimeSeriesMapping().keySet()
                .stream()
                .map(MappingKey::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader, Path workingDir) {
        Objects.requireNonNull(variantReadRange);
        if (!variantRange.encloses(variantReadRange)) {
            throw new IllegalArgumentException("Variant range " + variantRange + " do not enclose read range " + variantReadRange);
        }
        Objects.requireNonNull(reader);

        List<TimeSeriesMapperObserver> observers = new ArrayList<>(1);
        observers.add(createBalanceSummary(reader));
        if (isNetworkPointComputation) {
            observers.add(createNetworkPointWriter(workingDir));
        }
        if (metrixDslData != null && (!metrixDslData.getHvdcFlowResults().isEmpty() || !metrixDslData.getPstAngleTapResults().isEmpty())) {
            observers.add(createInitOptimizedTimeSeriesWriter(workingDir, variantReadRange));
        }
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(version)), variantReadRange, ignoreLimits, ignoreEmptyFilter, !isNetworkPointComputation, getContingenciesProbabilitiesTs(), mappingParameters.getToleranceThreshold());
        mapper.mapToNetwork(store, parameters, observers);
    }

    private Set<String> getContingenciesProbabilitiesTs() {
        return contingenciesProvider.getContingencies(network)
                .stream()
                .filter(contingency -> contingency.getExtension(Probability.class) != null && contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef() != null)
                .map(contingency -> contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef())
                .collect(Collectors.toSet());
    }

    private TimeSeriesMapperObserver createBalanceSummary(MetrixVariantReader reader) {
        return new BalanceSummary(err) {
            @Override
            public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
                super.timeSeriesMappingStart(point, index);
                reader.onVariantStart(point);
            }

            @Override
            public void map(int version, int point, TimeSeriesTable table) {
                reader.onVariant(version, point, table);
            }

            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
                super.timeSeriesMappedToEquipment(point, timeSeriesName, identifiable, variable, equipmentValue);
                if (isMetrixVariable(variable) && !Double.isNaN(equipmentValue)) {
                    reader.onEquipmentVariant(identifiable, variable, equipmentValue);
                }
            }

            @Override
            public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
                super.timeSeriesMappingEnd(point, index, balance);
                reader.onVariantEnd(point);
            }
        };
    }

    protected TimeSeriesMapperObserver createNetworkPointWriter(Path workingDir) {
        Objects.requireNonNull(workingDir);
        DataSource dataSource = DataSourceUtil.createDataSource(workingDir, network.getId(), null);
        return new NetworkPointWriter(network, dataSource);
    }

    private TimeSeriesMapperObserver createInitOptimizedTimeSeriesWriter(Path workingDir, Range<Integer> pointRange) {
        Objects.requireNonNull(workingDir);
        Objects.requireNonNull(pointRange);
        try {
            BufferedWriter writer = Files.newBufferedWriter(workingDir.resolve(INPUT_OPTIMIZED_FILE_NAME), StandardCharsets.UTF_8);
            return new InitOptimizedTimeSeriesWriter(network, metrixDslData, pointRange, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
