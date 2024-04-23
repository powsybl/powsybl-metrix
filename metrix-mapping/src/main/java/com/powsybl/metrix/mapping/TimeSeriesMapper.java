/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRangeAdder;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.metrix.mapping.log.*;
import com.powsybl.metrix.mapping.timeseries.MappedEquipment;
import com.powsybl.metrix.mapping.timeseries.EquipmentTimeSeriesMap;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.metrix.mapping.TimeSeriesConstants.*;

public class TimeSeriesMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMapper.class);
    public static final double EPSILON_COMPARISON = 1e-5;
    private static final double EPSILON_ZERO_STD_DEV = 1e-6;
    public static final int CONSTANT_VARIANT_ID = -1;
    public static final int SWITCH_OPEN = 0; // 0 means switch is open
    public static final int DISCONNECTED_VALUE = 0; // 0 means equipment is disconnected
    public static final int CONNECTED_VALUE = 1;
    private static final String LOAD_NOT_FOUND_MESSAGE = "Load '%s' not found";

    private final TimeSeriesMappingConfig config;
    private final TimeSeriesMapperParameters parameters;
    private final Network network;
    private final TimeSeriesMappingLogger timeSeriesMappingLogger;

    private TimeSeriesTable table;
    private TimeSeriesMapperChecker checker;

    private static class MapperContext {
        private final EquipmentTimeSeriesMap timeSeriesToLoadsMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToGeneratorsMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToDanglingLinesMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToHvdcLinesMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToPhaseTapChangersMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToBreakersMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToTransformersMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToRatioTapChangersMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToLccConverterStationsMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToVscConverterStationsMapping = new EquipmentTimeSeriesMap();
        private final EquipmentTimeSeriesMap timeSeriesToLinesMapping = new EquipmentTimeSeriesMap();
        private Map<IndexedName, Set<MappingKey>> equipmentTimeSeries;
    }

    public TimeSeriesMapper(TimeSeriesMappingConfig config, TimeSeriesMapperParameters parameters, Network network, TimeSeriesMappingLogger timeSeriesMappingLogger) {
        this.config = Objects.requireNonNull(config);
        this.parameters = Objects.requireNonNull(parameters);
        this.network = Objects.requireNonNull(network);
        this.timeSeriesMappingLogger = Objects.requireNonNull(timeSeriesMappingLogger);
    }

    public static void setHvdcMax(HvdcLine hvdcLine, double max) {
        HvdcOperatorActivePowerRange activePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
        if (activePowerRange != null) {
            activePowerRange.setOprFromCS1toCS2((float) Math.abs(max));
        }
        hvdcLine.setMaxP(Math.max(hvdcLine.getMaxP(), Math.abs(max)));
    }

    public static void setHvdcMin(HvdcLine hvdcLine, double min) {
        HvdcOperatorActivePowerRange activePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
        if (activePowerRange != null) {
            hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS2toCS1((float) Math.abs(min));
        }
        hvdcLine.setMaxP(Math.max(hvdcLine.getMaxP(), Math.abs(min)));
    }

    public static float getMax(Identifiable<?> identifiable) {
        if (identifiable instanceof Generator generator) {
            return (float) generator.getMaxP();
        } else if (identifiable instanceof HvdcLine hvdcLine) {
            HvdcOperatorActivePowerRange activePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
            if (activePowerRange != null) {
                return activePowerRange.getOprFromCS1toCS2();
            } else {
                return (float) hvdcLine.getMaxP();
            }
        } else {
            return Float.MAX_VALUE;
        }
    }

    public static float getMin(Identifiable<?> identifiable) {
        if (identifiable instanceof Generator generator) {
            return (float) generator.getMinP();
        } else if (identifiable instanceof HvdcLine hvdcLine) {
            HvdcOperatorActivePowerRange activePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
            if (activePowerRange != null) {
                return -activePowerRange.getOprFromCS2toCS1();
            } else {
                return (float) -hvdcLine.getMaxP();
            }
        } else {
            return Float.MIN_VALUE;
        }
    }

    public static HvdcAngleDroopActivePowerControl getActivePowerControl(HvdcLine hvdcLine) {
        HvdcAngleDroopActivePowerControl activePowerControl = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
        if (activePowerControl != null &&
                activePowerControl.isEnabled() &&
                activePowerControl.getDroop() > 0) {
            return activePowerControl;
        }
        return null;
    }

    public static void setHvdcLineSetPoint(HvdcLine hvdcLine, double setPoint) {
        HvdcAngleDroopActivePowerControl activePowerControl = getActivePowerControl(hvdcLine);
        if (activePowerControl != null) {
            activePowerControl.setP0((float) setPoint);
        } else {
            if (setPoint >= 0) {
                hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
                hvdcLine.setActivePowerSetpoint((float) setPoint);
            } else {
                hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
                hvdcLine.setActivePowerSetpoint((float) -setPoint);
            }
        }
    }

    public static float getHvdcLineSetPoint(HvdcLine hvdcLine) {
        HvdcAngleDroopActivePowerControl activePowerControl = getActivePowerControl(hvdcLine);
        if (activePowerControl != null) {
            return activePowerControl.getP0();
        } else {
            if (hvdcLine.getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER) {
                return (float) -hvdcLine.getActivePowerSetpoint();
            } else {
                return (float) hvdcLine.getActivePowerSetpoint();
            }
        }
    }

    public static float getP(Identifiable<?> identifiable) {
        if (identifiable instanceof Generator generator) {
            return (float) generator.getTargetP();
        } else if (identifiable instanceof HvdcLine hvdcLine) {
            return getHvdcLineSetPoint(hvdcLine);
        } else {
            return Float.MIN_VALUE;
        }
    }

    public static HvdcOperatorActivePowerRange addActivePowerRangeExtension(HvdcLine hvdcLine) {
        HvdcOperatorActivePowerRange hvdcRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
        if (hvdcRange == null) {
            hvdcLine
                    .newExtension(HvdcOperatorActivePowerRangeAdder.class)
                    .withOprFromCS1toCS2((float) Math.abs(hvdcLine.getMaxP()))
                    .withOprFromCS2toCS1((float) Math.abs(hvdcLine.getMaxP()))
                    .add();
            hvdcRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
        }
        return hvdcRange;
    }

    public static boolean isPowerOrLimitVariable(MappingVariable variable) {
        return variable == EquipmentVariable.targetP ||
                variable == EquipmentVariable.activePowerSetpoint ||
                variable == EquipmentVariable.minP ||
                variable == EquipmentVariable.maxP;
    }

    public static boolean isPowerVariable(MappingVariable variable) {
        return variable == EquipmentVariable.targetP ||
                variable == EquipmentVariable.activePowerSetpoint;
    }

    public static MappingVariable getPowerVariable(Identifiable<?> identifiable) {
        if (identifiable instanceof Generator) {
            return EquipmentVariable.targetP;
        } else if (identifiable instanceof HvdcLine) {
            return EquipmentVariable.activePowerSetpoint;
        } else {
            throw new AssertionError("Unsupported equipment type for id " + identifiable.getId());
        }
    }

    private void mapToNetwork(int version, int variantId, int point,
                              IndexedMappingKey mappingKey, List<MappedEquipment> mappedEquipments) {
        String timeSeriesName = mappingKey.getKey().getId();
        MappingVariable variable = mappingKey.getKey().getMappingVariable();

        // compute distribution key associated to equipment list
        double[] distributionKeys = new double[mappedEquipments.size()];
        double distributionKeySum = calculateDistributionKeySum(version, point, mappedEquipments, distributionKeys);

        double[] equipmentValues = new double[mappedEquipments.size()];
        Arrays.fill(equipmentValues, 0);

        double timeSeriesValue = table.getDoubleValue(version, mappingKey.getNum(), point);
        if (Double.isNaN(timeSeriesValue) || Double.isInfinite(timeSeriesValue)) {
            throw new TimeSeriesMappingException("Impossible to scale down " + timeSeriesValue + " of ts " + timeSeriesName + " at time index '" + table.getTableIndex().getInstantAt(point) + "' and version " + version);
        }

        LogBuilder logBuilder = new LogBuilder().level(System.Logger.Level.WARNING).version(version).point(variantId).index(table.getTableIndex());
        if (Math.abs(timeSeriesValue) > 0) {
            // check equipment list is not empty
            if (mappedEquipments.isEmpty()) {
                LogContent emptyFilter = new EmptyFilter().timeSeriesName(timeSeriesName).timeSeriesValue(timeSeriesValue).build();
                Log log = logBuilder.point(point).logDescription(emptyFilter).build();
                if (parameters.isIgnoreEmptyFilter()) {
                    timeSeriesMappingLogger.addLog(log);
                } else {
                    throw new TimeSeriesMappingException(log.getMessage());
                }
            } else {
                distributionKeySum = logDistributionKeySumNull(mappedEquipments, timeSeriesName, distributionKeys, distributionKeySum, timeSeriesValue, logBuilder);

                if (logHvdcLimitSign(mappedEquipments, timeSeriesName, variable, timeSeriesValue, logBuilder)) {
                    return;
                }

                // scaling downtime series value to mapped equipments
                for (int i = 0; i < mappedEquipments.size(); i++) {
                    assert distributionKeySum != 0;
                    double distributionFactor = distributionKeys[i] / distributionKeySum;
                    double equipmentValue = timeSeriesValue * distributionFactor;
                    equipmentValues[i] = equipmentValue;
                }
            }
        }

        if (checker != null) {
            List<Identifiable<?>> ids = mappedEquipments.stream().map(MappedEquipment::getIdentifiable).collect(Collectors.toList());
            boolean ignoreLimitsForTimeSeries = parameters.isIgnoreLimits() || TimeSeriesMapper.isPowerVariable(variable) && config.getIgnoreLimitsTimeSeriesNames().contains(timeSeriesName);
            checker.timeSeriesMappedToEquipments(variantId, timeSeriesName, timeSeriesValue, ids, variable, equipmentValues, ignoreLimitsForTimeSeries);
        }
    }

    private boolean logHvdcLimitSign(List<MappedEquipment> mappedEquipments, String timeSeriesName, MappingVariable variable, double timeSeriesValue, LogBuilder logBuilder) {
        if (mappedEquipments.get(0).getIdentifiable() instanceof HvdcLine) {
            LimitSignBuilder limitSignBuilder = new LimitSignBuilder()
                    .timeSeriesValue(timeSeriesValue)
                    .timeSeriesName(timeSeriesName)
                    .variable(EquipmentVariable.maxP.getVariableName());
            if (variable == EquipmentVariable.maxP && timeSeriesValue < 0) {
                limitSignBuilder.max();
                timeSeriesMappingLogger.addLog(logBuilder.logDescription(limitSignBuilder.build()).build());
                return true;
            } else if (variable == EquipmentVariable.minP && timeSeriesValue > 0) {
                limitSignBuilder.min();
                timeSeriesMappingLogger.addLog(logBuilder.logDescription(limitSignBuilder.build()).build());
                return true;
            }
        }
        return false;
    }

    private double logDistributionKeySumNull(List<MappedEquipment> mappedEquipments, String timeSeriesName, double[] distributionKeys, double distributionKeySum, double timeSeriesValue, LogBuilder logBuilder) {
        double resultDistributionKeySum = distributionKeySum;
        if (resultDistributionKeySum == 0) {
            double distributionKey = NumberDistributionKey.ONE.value();
            for (int i = 0; i < mappedEquipments.size(); i++) {
                distributionKeys[i] = distributionKey;
                resultDistributionKeySum += distributionKeys[i];
            }
            LogContent logContent = new ZeroDistributionKeyInfo(timeSeriesName, timeSeriesValue,
                    mappedEquipments.stream().map(MappedEquipment::getId).toList()).build();
            Log log = logBuilder.level(System.Logger.Level.INFO).logDescription(logContent).build();
            timeSeriesMappingLogger.addLog(log);
        }
        return resultDistributionKeySum;
    }

    private double calculateDistributionKeySum(int version, int point, List<MappedEquipment> mappedEquipments, double[] distributionKeys) {
        double distributionKeySum = 0;
        for (int i = 0; i < mappedEquipments.size(); i++) {
            MappedEquipment mappedEquipment = mappedEquipments.get(i);
            DistributionKey distributionKey = mappedEquipment.getDistributionKey();
            if (distributionKey instanceof NumberDistributionKey numberDistributionKey) {
                distributionKeys[i] = numberDistributionKey.value();
            } else if (distributionKey instanceof TimeSeriesDistributionKey timeSeriesDistributionKey) {
                int timeSeriesNum = timeSeriesDistributionKey.getTimeSeriesNum();
                if (timeSeriesNum == -1) {
                    timeSeriesNum = table.getDoubleTimeSeriesIndex(timeSeriesDistributionKey.getTimeSeriesName());
                    timeSeriesDistributionKey.setTimeSeriesNum(timeSeriesNum);
                }
                distributionKeys[i] = Math.abs(table.getDoubleValue(version, timeSeriesNum, point));
            } else {
                throw new AssertionError();
            }

            distributionKeySum += distributionKeys[i];
        }
        return distributionKeySum;
    }

    private void mapToNetwork(int version, int variantId, int point,
                              EquipmentTimeSeriesMap timeSeriesToEquipmentsMapping) {
        timeSeriesToEquipmentsMapping.getEquipmentTimeSeries().forEach((mappingKey, mappedEquipments) ->
            mapToNetwork(version, variantId, point, mappingKey, mappedEquipments));
    }

    public static IndexedMappingKey indexMappingKey(TimeSeriesTable timeSeriesTable, MappingKey key) {
        return new IndexedMappingKey(key, timeSeriesTable.getDoubleTimeSeriesIndex(key.getId()));
    }

    private void identifyConstantTimeSeries(int version,
                                            EquipmentTimeSeriesMap sourceTimeSeries,
                                            EquipmentTimeSeriesMap constantTimeSeries,
                                            EquipmentTimeSeriesMap variableTimeSeries) {
        if (!parameters.isIdentifyConstantTimeSeries()) {
            variableTimeSeries.init(sourceTimeSeries);
            return;
        }
        sourceTimeSeries.getEquipmentTimeSeries().forEach((indexedMappingKey, mappedEquipments) -> {
            int timeSeriesNum = indexedMappingKey.getNum();
            MappingVariable variable = indexedMappingKey.getKey().getMappingVariable();
            if (variable == EquipmentVariable.targetP ||
                    variable == EquipmentVariable.activePowerSetpoint) {
                // Active power mapping is not tested in order to allow later correction of values not included in [minP, maxP]
                variableTimeSeries.addMappedEquipmentTimeSeries(indexedMappingKey, mappedEquipments);
            } else {
                if (table.getStdDev(version, timeSeriesNum) < EPSILON_ZERO_STD_DEV) { // std dev == 0 means time-series is constant
                    LOGGER.debug("Mapping time-series '{}' is constant", indexedMappingKey.getKey().getId());
                    constantTimeSeries.addMappedEquipmentTimeSeries(indexedMappingKey, mappedEquipments);
                } else {
                    variableTimeSeries.addMappedEquipmentTimeSeries(indexedMappingKey, mappedEquipments);
                }
            }
        });
    }

    private void correctUnmappedGenerator(boolean isUnmappedMinP, boolean isUnmappedMaxP, Generator generator,
                                          int version) {

        final double minP = generator.getMinP();
        final double maxP = generator.getMaxP();
        final double targetP = generator.getTargetP();

        new GeneratorBoundLimitBuilder()
                .minP(minP)
                .maxP(maxP)
                .targetP(targetP)
                .isUnmappedMinP(isUnmappedMinP)
                .isUnmappedMaxP(isUnmappedMaxP)
                .version(version)
                .index(table.getTableIndex())
                .ignoreLimits(parameters.isIgnoreLimits())
                .setAll(generator, timeSeriesMappingLogger);

    }

    private void correctUnmappedHvdcLine(boolean isUnmappedMinP, boolean isUnmappedMaxP, HvdcLine hvdcLine, int version) {

        final boolean isActivePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class) != null;
        final double minP = TimeSeriesMapper.getMin(hvdcLine);
        final double maxP = TimeSeriesMapper.getMax(hvdcLine);
        final double hvdcLineMaxP = hvdcLine.getMaxP();
        final double setPoint = TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine);

        new HvdcBoundLimitBuilder()
                .isActivePowerRange(isActivePowerRange)
                .minP(minP)
                .maxP(maxP)
                .hvdcLineMaxP(hvdcLineMaxP)
                .setPoint(setPoint)
                .isUnmappedMinP(isUnmappedMinP)
                .isUnmappedMaxP(isUnmappedMaxP)
                .version(version)
                .index(table.getTableIndex())
                .ignoreLimits(parameters.isIgnoreLimits())
                .setAll(hvdcLine, timeSeriesMappingLogger);
    }

    private void mapToNetwork(MapperContext context, int version) {

        int firstPoint = parameters.getPointRange() != null ? parameters.getPointRange().lowerEndpoint() : 0;
        int lastPoint = parameters.getPointRange() != null ? parameters.getPointRange().upperEndpoint() : (table.getTableIndex().getPointCount() - 1);

        // Load mappings are always variable time series to ensure data consistency (p0/LoadDetail)
        EquipmentTimeSeriesMap timeSeriesToLoadsMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToLoadsMapping = new EquipmentTimeSeriesMap();
        timeSeriesToLoadsMapping.init(context.timeSeriesToLoadsMapping);

        // Check if some generator mappings are constant
        EquipmentTimeSeriesMap timeSeriesToGeneratorsMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToGeneratorsMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToGeneratorsMapping, constantTimeSeriesToGeneratorsMapping, timeSeriesToGeneratorsMapping);

        // Check if some dangling lines mappings are constant
        EquipmentTimeSeriesMap timeSeriesToDanglingLinesMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToDanglingLinesMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToDanglingLinesMapping, constantTimeSeriesToDanglingLinesMapping, timeSeriesToDanglingLinesMapping);

        // Check if some hvdc lines mappings are constant
        EquipmentTimeSeriesMap timeSeriesToHvdcLinesMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToHvdcLinesMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToHvdcLinesMapping, constantTimeSeriesToHvdcLinesMapping, timeSeriesToHvdcLinesMapping);

        // Check if some phase tap changers mappings are constant
        EquipmentTimeSeriesMap timeSeriesToPhaseTapChangersMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToPhaseTapChangersMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToPhaseTapChangersMapping, constantTimeSeriesToPhaseTapChangersMapping, timeSeriesToPhaseTapChangersMapping);

        // Check if some breaker mappings are constant
        EquipmentTimeSeriesMap timeSeriesToBreakersMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToBreakersMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToBreakersMapping, constantTimeSeriesToBreakersMapping, timeSeriesToBreakersMapping);

        // Check if some transformers mappings are constant
        EquipmentTimeSeriesMap timeSeriesToTransformersMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToTransformersMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToTransformersMapping, constantTimeSeriesToTransformersMapping, timeSeriesToTransformersMapping);

        // Check if some tap changers mappings are constant
        EquipmentTimeSeriesMap timeSeriesToRatioTapChangersMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToRatioTapChangersMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToRatioTapChangersMapping, constantTimeSeriesToRatioTapChangersMapping, timeSeriesToRatioTapChangersMapping);

        // Check if some lcc converters mappings are constant
        EquipmentTimeSeriesMap timeSeriesToLccConverterStationsMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToLccConverterStationsMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToLccConverterStationsMapping, constantTimeSeriesToLccConverterStationsMapping, timeSeriesToLccConverterStationsMapping);

        // Check if some vsc converters mappings are constant
        EquipmentTimeSeriesMap timeSeriesToVscConverterStationsMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToVscConverterStationsMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToVscConverterStationsMapping, constantTimeSeriesToVscConverterStationsMapping, timeSeriesToVscConverterStationsMapping);

        // Check if some lines mappings are constant
        EquipmentTimeSeriesMap timeSeriesToLinesMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToLinesMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(version, context.timeSeriesToLinesMapping, constantTimeSeriesToLinesMapping, timeSeriesToLinesMapping);

        // Check if some equipment mappings are constant
        Map<IndexedName, Set<MappingKey>> equipmentTimeSeries = new LinkedHashMap<>();
        Map<IndexedName, Set<MappingKey>> constantEquipmentTimeSeries = new LinkedHashMap<>();
        context.equipmentTimeSeries.forEach((indexedName, mappingKeys) -> {
            int timeSeriesNum = indexedName.getNum();
            if (table.getStdDev(version, timeSeriesNum) < EPSILON_COMPARISON) { // std dev == 0 means time-series is constant
                LOGGER.debug("Equipment time-series '{}' is constant", indexedName.getName());
                constantEquipmentTimeSeries.put(indexedName, mappingKeys);
            } else {
                equipmentTimeSeries.put(indexedName, mappingKeys);
            }
        });

        // Correct base case values
        network.getGeneratorStream()
                .filter(g -> config.getUnmappedGenerators().contains(g.getId()))
                .forEach(g -> {
                    final boolean isMinPUnmapped = config.getUnmappedMinPGenerators().contains(g.getId());
                    final boolean isMaxPUnmapped = config.getUnmappedMaxPGenerators().contains(g.getId());
                    correctUnmappedGenerator(isMinPUnmapped, isMaxPUnmapped, g, version);
                });
        network.getHvdcLineStream()
                .filter(l -> config.getUnmappedHvdcLines().contains(l.getId()))
                .forEach(l -> {
                    final boolean isMinPUnmapped = config.getUnmappedMinPHvdcLines().contains(l.getId());
                    final boolean isMaxPUnmapped = config.getUnmappedMaxPHvdcLines().contains(l.getId());
                    correctUnmappedHvdcLine(isMinPUnmapped, isMaxPUnmapped, l, version);
                });

        // process constant time series
        if (checker != null) {
            checker.timeSeriesMappingStart(CONSTANT_VARIANT_ID, table.getTableIndex());
        }

        if (!constantTimeSeriesToLoadsMapping.isEmpty() ||
                !constantTimeSeriesToGeneratorsMapping.isEmpty() ||
                !constantTimeSeriesToDanglingLinesMapping.isEmpty() ||
                !constantTimeSeriesToHvdcLinesMapping.isEmpty() ||
                !constantTimeSeriesToPhaseTapChangersMapping.isEmpty() ||
                !constantTimeSeriesToBreakersMapping.isEmpty() ||
                !constantTimeSeriesToTransformersMapping.isEmpty() ||
                !constantTimeSeriesToRatioTapChangersMapping.isEmpty() ||
                !constantTimeSeriesToLccConverterStationsMapping.isEmpty() ||
                !constantTimeSeriesToVscConverterStationsMapping.isEmpty() ||
                !constantTimeSeriesToLinesMapping.isEmpty() ||
                !constantEquipmentTimeSeries.isEmpty()) {

            mapSinglePoint(version, CONSTANT_VARIANT_ID, firstPoint,
                    constantTimeSeriesToLoadsMapping,
                    constantTimeSeriesToGeneratorsMapping,
                    constantTimeSeriesToDanglingLinesMapping,
                    constantTimeSeriesToHvdcLinesMapping,
                    constantTimeSeriesToPhaseTapChangersMapping,
                    constantTimeSeriesToBreakersMapping,
                    constantTimeSeriesToTransformersMapping,
                    constantTimeSeriesToRatioTapChangersMapping,
                    constantTimeSeriesToLccConverterStationsMapping,
                    constantTimeSeriesToVscConverterStationsMapping,
                    constantTimeSeriesToLinesMapping,
                    constantEquipmentTimeSeries);
        }

        // for unmapped equipments, keep base case value which is constant
        double constantBalance = calculateConstantBalance();

        if (checker != null) {
            checker.timeSeriesMappingEnd(CONSTANT_VARIANT_ID, table.getTableIndex(), constantBalance);
        }

        // process each time point
        for (int point = firstPoint; point <= lastPoint; point++) {

            if (checker != null) {
                checker.timeSeriesMappingStart(point, table.getTableIndex());
            }

            mapSinglePoint(version, point, point,
                timeSeriesToLoadsMapping,
                timeSeriesToGeneratorsMapping,
                timeSeriesToDanglingLinesMapping,
                timeSeriesToHvdcLinesMapping,
                timeSeriesToPhaseTapChangersMapping,
                timeSeriesToBreakersMapping,
                timeSeriesToTransformersMapping,
                timeSeriesToRatioTapChangersMapping,
                timeSeriesToLccConverterStationsMapping,
                timeSeriesToVscConverterStationsMapping,
                timeSeriesToLinesMapping,
                equipmentTimeSeries);

            if (checker != null) {
                checker.timeSeriesMappingEnd(point, table.getTableIndex(), 0);
            }
        }
    }

    private double calculateConstantBalance() {
        double constantBalance = 0;
        Set<String> unmappedGenerators = new HashSet<>(config.getUnmappedGenerators());
        unmappedGenerators.retainAll(config.getUnmappedMinPGenerators());
        unmappedGenerators.retainAll(config.getUnmappedMaxPGenerators());
        for (String id : unmappedGenerators) {
            Generator generator = network.getGenerator(id);
            if (generator == null) {
                throw new TimeSeriesMappingException(String.format("Generator '%s' not found", id));
            }
            constantBalance += generator.getTargetP();
        }
        Set<String> unmappedLoads = config.getUnmappedLoads();
        for (String id : unmappedLoads) {
            Load load = network.getLoad(id);
            if (load == null) {
                throw new TimeSeriesMappingException(String.format(LOAD_NOT_FOUND_MESSAGE, id));
            }
            constantBalance -= load.getP0();
        }
        for (String id : config.getUnmappedFixedActivePowerLoads()) {
            if (!unmappedLoads.contains(id)) {
                Load load = network.getLoad(id);
                if (load == null) {
                    throw new TimeSeriesMappingException(String.format(LOAD_NOT_FOUND_MESSAGE, id));
                }
                LoadDetail loadDetail = load.getExtension(LoadDetail.class);
                if (loadDetail == null) {
                    throw new TimeSeriesMappingException(String.format("LoadDetail '%s' not found", id));
                }
                constantBalance -= loadDetail.getFixedActivePower();
            }
        }
        for (String id : config.getUnmappedVariableActivePowerLoads()) {
            if (!unmappedLoads.contains(id)) {
                Load load = network.getLoad(id);
                if (load == null) {
                    throw new TimeSeriesMappingException(String.format(LOAD_NOT_FOUND_MESSAGE, id));
                }
                LoadDetail loadDetail = load.getExtension(LoadDetail.class);
                if (loadDetail == null) {
                    throw new TimeSeriesMappingException(String.format("LoadDetail '%s' not found", id));
                }
                constantBalance -= loadDetail.getVariableActivePower();
            }
        }
        for (String id : config.getUnmappedDanglingLines()) {
            DanglingLine danglingLine = network.getDanglingLine(id);
            if (danglingLine == null) {
                throw new TimeSeriesMappingException(String.format("Dangling line '%s' not found", id));
            }
            constantBalance -= danglingLine.getP0();
        }
        return constantBalance;
    }

    private void mapSinglePoint(int version, int variantId, int point,
                                EquipmentTimeSeriesMap timeSeriesToLoadsMapping,
                                EquipmentTimeSeriesMap timeSeriesToGeneratorsMapping,
                                EquipmentTimeSeriesMap timeSeriesToDanglingLinesMapping,
                                EquipmentTimeSeriesMap timeSeriesToHvdcLinesMapping,
                                EquipmentTimeSeriesMap timeSeriesToPhaseTapChangersMapping,
                                EquipmentTimeSeriesMap timeSeriesToBreakersMapping,
                                EquipmentTimeSeriesMap timeSeriesToTransformersMapping,
                                EquipmentTimeSeriesMap timeSeriesToRatioTapChangersMapping,
                                EquipmentTimeSeriesMap timeSeriesToLccConverterStationsMapping,
                                EquipmentTimeSeriesMap timeSeriesToVscConverterStationsMapping,
                                EquipmentTimeSeriesMap timeSeriesToLinesMapping,
                                Map<IndexedName, Set<MappingKey>> equipmentTimeSeries) {

        // process time series for mapping
        mapToNetwork(version, variantId, point, timeSeriesToLoadsMapping);
        mapToNetwork(version, variantId, point, timeSeriesToGeneratorsMapping);
        mapToNetwork(version, variantId, point, timeSeriesToDanglingLinesMapping);
        mapToNetwork(version, variantId, point, timeSeriesToHvdcLinesMapping);
        mapToNetwork(version, variantId, point, timeSeriesToPhaseTapChangersMapping);
        mapToNetwork(version, variantId, point, timeSeriesToBreakersMapping);
        mapToNetwork(version, variantId, point, timeSeriesToTransformersMapping);
        mapToNetwork(version, variantId, point, timeSeriesToRatioTapChangersMapping);
        mapToNetwork(version, variantId, point, timeSeriesToLccConverterStationsMapping);
        mapToNetwork(version, variantId, point, timeSeriesToVscConverterStationsMapping);
        mapToNetwork(version, variantId, point, timeSeriesToLinesMapping);

        if (checker != null) {
            // generic mapping
            if (CONSTANT_VARIANT_ID != variantId) {
                checker.map(version, point, table);
            }

            // process equipment time series
            for (Map.Entry<IndexedName, Set<MappingKey>> e : equipmentTimeSeries.entrySet()) {
                String timeSeriesName = e.getKey().getName();
                int timeSeriesNum = e.getKey().getNum();
                double timeSeriesValue = table.getDoubleValue(version, timeSeriesNum, point);
                for (MappingKey equipment : e.getValue()) {
                    checker.timeSeriesMappedToEquipment(point, timeSeriesName, network.getIdentifiable(equipment.getId()), equipment.getMappingVariable(), timeSeriesValue);
                }
            }
        }
    }

    public void mapToNetwork(ReadOnlyTimeSeriesStore store, List<TimeSeriesMapperObserver> observers) {

        checker = new TimeSeriesMapperChecker(observers, timeSeriesMappingLogger, parameters);

        checker.start();

        // create context
        MapperContext context = null;
        for (int version : parameters.getVersions()) {

            checker.versionStart(version);

            try {
                // load time series involved in the config in a table
                table = new TimeSeriesMappingConfigTableLoader(config, store).load(version, parameters.getRequiredTimeseries(), parameters.getPointRange());

                if (context == null) {
                    context = new MapperContext();
                    context.timeSeriesToLoadsMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToLoadsMapping(), table, network, config);
                    context.timeSeriesToGeneratorsMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToGeneratorsMapping(), table, network, config);
                    context.timeSeriesToDanglingLinesMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToDanglingLinesMapping(), table, network, config);
                    context.timeSeriesToHvdcLinesMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToHvdcLinesMapping(), table, network, config);
                    context.timeSeriesToPhaseTapChangersMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToPhaseTapChangersMapping(), table, network, config);
                    context.timeSeriesToBreakersMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToBreakersMapping(), table, network, config);
                    context.timeSeriesToTransformersMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToTransformersMapping(), table, network, config);
                    context.timeSeriesToRatioTapChangersMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToRatioTapChangersMapping(), table, network, config);
                    context.timeSeriesToLccConverterStationsMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToLccConverterStationsMapping(), table, network, config);
                    context.timeSeriesToVscConverterStationsMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToVscConverterStationsMapping(), table, network, config);
                    context.timeSeriesToLinesMapping.convertToEquipmentTimeSeriesMap(config.getTimeSeriesToLinesMapping(), table, network, config);
                    context.equipmentTimeSeries = config.getTimeSeriesToEquipment().entrySet().stream()
                            .collect(Collectors.toMap(e -> new IndexedName(e.getKey(), table.getDoubleTimeSeriesIndex(e.getKey())), Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
                }

                mapToNetwork(context, version);
            } finally {
                checker.versionEnd(version);
            }
        }

        checker.end();

        timeSeriesMappingLogger.printLogSynthesis();
    }
}
