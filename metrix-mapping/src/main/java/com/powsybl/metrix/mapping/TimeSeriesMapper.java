/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
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
import com.powsybl.timeseries.TimeSeriesIndex;
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

    private final TimeSeriesMappingConfig config;
    private final Network network;
    private final TimeSeriesMappingLogger timeSeriesMappingLogger;

    private TimeSeriesTable table;

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

    public TimeSeriesMapper(TimeSeriesMappingConfig config, Network network, TimeSeriesMappingLogger timeSeriesMappingLogger) {
        this.config = Objects.requireNonNull(config);
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
        if (identifiable instanceof Generator) {
            return (float) ((Generator) identifiable).getMaxP();
        } else if (identifiable instanceof HvdcLine) {
            HvdcLine hvdcLine = (HvdcLine) identifiable;
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
        if (identifiable instanceof Generator) {
            return (float) ((Generator) identifiable).getMinP();
        } else if (identifiable instanceof HvdcLine) {
            HvdcLine hvdcLine = (HvdcLine) identifiable;
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
        if (identifiable instanceof Generator) {
            return (float) ((Generator) identifiable).getTargetP();
        } else if (identifiable instanceof HvdcLine) {
            return getHvdcLineSetPoint((HvdcLine) identifiable);
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
                              IndexedMappingKey mappingKey, List<MappedEquipment> mappedEquipments,
                              TimeSeriesMapperChecker observer, boolean ignoreLimits,
                              boolean ignoreEmptyFilter) {
        String timeSeriesName = mappingKey.getKey().getId();
        MappingVariable variable = mappingKey.getKey().getMappingVariable();

        // compute distribution key associated to equipment list
        double[] distributionKeys = new double[mappedEquipments.size()];
        double distributionKeySum = calculateDistributionKeySum(table, version, point, mappedEquipments, distributionKeys);

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
                if (ignoreEmptyFilter) {
                    timeSeriesMappingLogger.addLog(log);
                } else {
                    throw new TimeSeriesMappingException(log.getMessage());
                }
            } else {
                distributionKeySum = logDistributionKeySumNull(mappedEquipments, timeSeriesName, distributionKeys, distributionKeySum, timeSeriesValue, logBuilder);

                if (logHvdcLimitSign(mappedEquipments, timeSeriesName, variable, timeSeriesValue, logBuilder)) {
                    return;
                }

                // scaling down time series value to mapped equipments
                for (int i = 0; i < mappedEquipments.size(); i++) {
                    assert distributionKeySum != 0;
                    double distributionFactor = distributionKeys[i] / distributionKeySum;
                    double equipmentValue = timeSeriesValue * distributionFactor;
                    equipmentValues[i] = equipmentValue;
                }
            }
        }

        if (observer != null) {
            List<Identifiable<?>> identifiables = mappedEquipments.stream().map(MappedEquipment::getIdentifiable).collect(Collectors.toList());
            boolean ignoreLimitsForTimeSeries = ignoreLimits || TimeSeriesMapper.isPowerVariable(variable) && config.getIgnoreLimitsTimeSeriesNames().contains(timeSeriesName);
            observer.timeSeriesMappedToEquipments(variantId, timeSeriesName, timeSeriesValue, identifiables, variable, equipmentValues, ignoreLimitsForTimeSeries);
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
            double distributionKey = NumberDistributionKey.ONE.getValue();
            for (int i = 0; i < mappedEquipments.size(); i++) {
                distributionKeys[i] = distributionKey;
                resultDistributionKeySum += distributionKeys[i];
            }
            LogContent logContent = new ZeroDistributionKeyInfo(timeSeriesName, timeSeriesValue,
                    mappedEquipments.stream().map(MappedEquipment::getId).collect(Collectors.toList())).build();
            Log log = logBuilder.level(System.Logger.Level.INFO).logDescription(logContent).build();
            timeSeriesMappingLogger.addLog(log);
        }
        return resultDistributionKeySum;
    }

    private double calculateDistributionKeySum(TimeSeriesTable table, int version, int point, List<MappedEquipment> mappedEquipments, double[] distributionKeys) {
        double distributionKeySum = 0;
        for (int i = 0; i < mappedEquipments.size(); i++) {
            MappedEquipment mappedEquipment = mappedEquipments.get(i);
            DistributionKey distributionKey = mappedEquipment.getDistributionKey();
            if (distributionKey instanceof NumberDistributionKey) {
                distributionKeys[i] = ((NumberDistributionKey) distributionKey).getValue();
            } else if (distributionKey instanceof TimeSeriesDistributionKey) {
                int timeSeriesNum = ((TimeSeriesDistributionKey) distributionKey).getTimeSeriesNum();
                if (timeSeriesNum == -1) {
                    timeSeriesNum = table.getDoubleTimeSeriesIndex(((TimeSeriesDistributionKey) distributionKey).getTimeSeriesName());
                    ((TimeSeriesDistributionKey) distributionKey).setTimeSeriesNum(timeSeriesNum);
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
                              EquipmentTimeSeriesMap timeSeriesToEquipmentsMapping,
                              TimeSeriesMapperChecker observer, boolean ignoreLimits,
                              boolean ignoreEmptyFilter) {
        timeSeriesToEquipmentsMapping.getEquipmentTimeSeries().forEach((mappingKey, mappedEquipments) ->
            mapToNetwork(version, variantId, point, mappingKey, mappedEquipments, observer, ignoreLimits, ignoreEmptyFilter));
    }

    public static IndexedMappingKey indexMappingKey(TimeSeriesTable timeSeriesTable, MappingKey key) {
        return new IndexedMappingKey(key, timeSeriesTable.getDoubleTimeSeriesIndex(key.getId()));
    }

    private void identifyConstantTimeSeries(boolean forceNoConstantTimeSeries, TimeSeriesTable table, int version,
                                            EquipmentTimeSeriesMap sourceTimeSeries,
                                            EquipmentTimeSeriesMap constantTimeSeries,
                                            EquipmentTimeSeriesMap variableTimeSeries) {
        if (forceNoConstantTimeSeries) {
            variableTimeSeries.init(sourceTimeSeries);
        } else {
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
    }

    private void identifyConstantLoadTimeSeries(boolean forceNoConstantTimeSeries, TimeSeriesTable table, int version,
                                                MapperContext context,
                                                EquipmentTimeSeriesMap constantTimeSeries,
                                                EquipmentTimeSeriesMap variableTimeSeries) {

        if (forceNoConstantTimeSeries) {
            variableTimeSeries.init(context.timeSeriesToLoadsMapping);
            return;
        }

        EquipmentTimeSeriesMap possiblyConstantLoadDetailsMapping = new EquipmentTimeSeriesMap();
        Set<String> variableLoadDetailsIds = new HashSet<>();

        context.timeSeriesToLoadsMapping.getEquipmentTimeSeries().forEach((indexedMappingKey, mappedEquipments) ->
            identifyConstantLoadOneTimeSerie(table, version, constantTimeSeries, variableTimeSeries,
                    possiblyConstantLoadDetailsMapping, variableLoadDetailsIds, indexedMappingKey, mappedEquipments));

        possiblyConstantLoadDetailsMapping.getEquipmentTimeSeries().forEach((indexedMappingKey, mappedEquipments) ->
            mappedEquipments.forEach(mappedEquipment -> {
                if (variableLoadDetailsIds.contains(mappedEquipment.getIdentifiable().getId())) {
                    variableTimeSeries.computeIfAbsent(indexedMappingKey, mappedEquipment);
                } else {
                    constantTimeSeries.computeIfAbsent(indexedMappingKey, mappedEquipment);
                }
            })
        );
    }

    private void identifyConstantLoadOneTimeSerie(TimeSeriesTable table, int version,
                                                  EquipmentTimeSeriesMap constantTimeSeries,
                                                  EquipmentTimeSeriesMap variableTimeSeries,
                                                  EquipmentTimeSeriesMap possiblyConstantLoadDetailsMapping,
                                                  Set<String> variableLoadDetailsIds, IndexedMappingKey indexedMappingKey,
                                                  List<MappedEquipment> mappedEquipments) {
        int timeSeriesNum = indexedMappingKey.getNum();
        MappingVariable variable = indexedMappingKey.getKey().getMappingVariable();

        if (variable != EquipmentVariable.p0 &&
                variable != EquipmentVariable.fixedActivePower &&
                variable != EquipmentVariable.variableActivePower) {
            // Only test if active load power mapping is constant
            variableTimeSeries.addMappedEquipmentTimeSeries(indexedMappingKey, mappedEquipments);
            return;
        }

        if (table.getStdDev(version, timeSeriesNum) < EPSILON_ZERO_STD_DEV) { // std dev == 0 means time-series is constant
            LOGGER.debug("Mapping time-series '{}' is constant", indexedMappingKey.getKey().getId());
            if (variable == EquipmentVariable.p0) {
                constantTimeSeries.addMappedEquipmentTimeSeries(indexedMappingKey, mappedEquipments);
            } else {
                possiblyConstantLoadDetailsMapping.addMappedEquipmentTimeSeries(indexedMappingKey, mappedEquipments);
            }
        } else {
            variableTimeSeries.addMappedEquipmentTimeSeries(indexedMappingKey, mappedEquipments);
            if (variable != EquipmentVariable.p0) {
                variableLoadDetailsIds.addAll(mappedEquipments.stream()
                        .map(mappedEquipment -> mappedEquipment.getIdentifiable().getId())
                        .collect(Collectors.toList()));
            }
        }
    }

    private void correctUnmappedGenerator(boolean isUnmappedMinP, boolean isUnmappedMaxP, Generator generator,
                                          int version, boolean ignoreLimits, TimeSeriesIndex index) {

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
                .index(index)
                .ignoreLimits(ignoreLimits)
                .setAll(generator, timeSeriesMappingLogger);

    }

    private void correctUnmappedHvdcLine(boolean isUnmappedMinP, boolean isUnmappedMaxP, HvdcLine hvdcLine, int version,
                                         boolean ignoreLimits, TimeSeriesIndex index) {

        final boolean isActivePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class) != null;
        final double minP = TimeSeriesMapper.getMin(hvdcLine);
        final double maxP = TimeSeriesMapper.getMax(hvdcLine);
        final double hvdcLineMaxP = hvdcLine.getMaxP();
        final double setpoint = TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine);

        final String id = hvdcLine.getId();

        final String setPointVariableName = EquipmentVariable.activePowerSetpoint.getVariableName();
        final String maxPVariableName = isActivePowerRange ? CS12 : EquipmentVariable.maxP.getVariableName();
        final String minPVariableName = isActivePowerRange ? MINUS_CS21 : "-" + EquipmentVariable.maxP.getVariableName();

        if (hvdcLineMaxP < 0) {
            throw new AssertionError("Equipment '" + hvdcLine.getId() + "' : invalid active limit maxP " + hvdcLineMaxP + " in base case");
        } else if (isActivePowerRange && (minP > 0 || maxP < 0)) {
            throw new AssertionError("Equipment '" + hvdcLine.getId() + "' : invalid active limits [" + minP + ", " + maxP + "] in base case");
        }

        double correctedMaxP = maxP;
        double correctedMinP = minP;

        // Add activePowerRangeExtension
        addActivePowerRangeExtension(hvdcLine);

        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged().id(id).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().version(version).index(index).point(CONSTANT_VARIANT_ID).level(System.Logger.Level.WARNING);
        if (ignoreLimits) {
            logBuilder.level(System.Logger.Level.INFO);
        }
        // maxP inconstancy with CS1toCS2/CS2toCS1
        if (isActivePowerRange && (maxP > hvdcLineMaxP || -minP > hvdcLineMaxP)) {
            if (ignoreLimits) {
                if (((maxP > hvdcLineMaxP && -minP > hvdcLineMaxP) && maxP > -minP) || maxP > hvdcLineMaxP && -minP <= hvdcLineMaxP) {
                    LogContent logContent = rangeLogWithVariableChanged.notIncludedVariable(maxPVariableName).minValue(0).maxValue(hvdcLineMaxP)
                            .value(maxP).oldValue(EquipmentVariable.maxP.getVariableName()).toVariable(maxPVariableName)
                            .newValue(maxP).build();
                    Log log = logBuilder.logDescription(logContent).build();
                    timeSeriesMappingLogger.addLog(log);
                } else if (((maxP > hvdcLineMaxP && -minP < hvdcLineMaxP) && -minP > maxP) || (-minP > hvdcLineMaxP && maxP <= hvdcLineMaxP)) {
                    LogContent logContent = rangeLogWithVariableChanged.notIncludedVariable(minPVariableName).minValue(-hvdcLineMaxP).maxValue(0)
                            .value(minP).oldValue(MINUS_MAXP).toVariable(minPVariableName).newValue(minP).build();
                    Log log = logBuilder.logDescription(logContent).build();
                    timeSeriesMappingLogger.addLog(log);
                }
                hvdcLine.setMaxP(Math.max(maxP, -minP));
            } else {
                if (maxP > hvdcLineMaxP) {
                    LogContent logContent = rangeLogWithVariableChanged.notIncludedVariable(maxPVariableName).minValue(0).maxValue(hvdcLineMaxP).value(maxP)
                            .oldValue(maxPVariableName).toVariable(EquipmentVariable.maxP.getVariableName()).newValue(hvdcLineMaxP).build();
                    Log log = logBuilder.logDescription(logContent).build();
                    timeSeriesMappingLogger.addLog(log);
                    hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS1toCS2((float) hvdcLineMaxP);
                    correctedMaxP = hvdcLineMaxP;
                }
                if (minP < -hvdcLineMaxP) {
                    LogContent logContent = rangeLogWithVariableChanged.notIncludedVariable(minPVariableName).minValue(-hvdcLineMaxP).maxValue(0)
                            .value(minP).oldValue(minPVariableName).toVariable(MINUS_MAXP).newValue(-hvdcLineMaxP).build();
                    Log log = logBuilder.logDescription(logContent).build();
                    timeSeriesMappingLogger.addLog(log);
                    hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS2toCS1((float) hvdcLineMaxP);
                    correctedMinP = -hvdcLineMaxP;
                }
            }
        }

        rangeLogWithVariableChanged.notIncludedVariable(setPointVariableName).minValue(minP).maxValue(maxP).value(setpoint);
        boolean isMin = setpoint < correctedMinP && isUnmappedMinP;
        boolean isMax = setpoint > correctedMaxP && isUnmappedMaxP;
        if (!isMin && !isMax) {
            return;
        }
        String variableName = isMax ? maxPVariableName : minPVariableName;
        LogContent logContent;
        // setpoint inconstancy with maxP/CS1toCS2/CS2toCS1
        if (ignoreLimits) {
            logContent = rangeLogWithVariableChanged.oldValue(variableName).toVariable(setPointVariableName).newValue(setpoint).build();
            if (isMax) {
                TimeSeriesMapper.setHvdcMax(hvdcLine, setpoint);
            } else {
                TimeSeriesMapper.setHvdcMin(hvdcLine, setpoint);
            }
        } else {
            double newValue = isMax ? maxP : minP;
            logContent = rangeLogWithVariableChanged.oldValue(setPointVariableName).toVariable(variableName).newValue(newValue).build();
            TimeSeriesMapper.setHvdcLineSetPoint(hvdcLine, newValue);
        }
        Log log = logBuilder.logDescription(logContent).build();
        timeSeriesMappingLogger.addLog(log);
    }

    private void mapToNetwork(MapperContext context, TimeSeriesMapperParameters parameters,
                              int version, TimeSeriesMapperChecker observer) {

        int firstPoint = parameters.getPointRange() != null ? parameters.getPointRange().lowerEndpoint() : 0;
        int lastPoint = parameters.getPointRange() != null ? parameters.getPointRange().upperEndpoint() : (table.getTableIndex().getPointCount() - 1);
        boolean forceNoConstantTimeSeries = !parameters.isIdentifyConstantTimeSeries();

        // Check if some load mappings are constant
        EquipmentTimeSeriesMap timeSeriesToLoadsMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToLoadsMapping = new EquipmentTimeSeriesMap();
        identifyConstantLoadTimeSeries(forceNoConstantTimeSeries, table, version, context, constantTimeSeriesToLoadsMapping, timeSeriesToLoadsMapping);

        // Check if some generator mappings are constant
        EquipmentTimeSeriesMap timeSeriesToGeneratorsMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToGeneratorsMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToGeneratorsMapping, constantTimeSeriesToGeneratorsMapping, timeSeriesToGeneratorsMapping);

        // Check if some dangling lines mappings are constant
        EquipmentTimeSeriesMap timeSeriesToDanglingLinesMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToDanglingLinesMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToDanglingLinesMapping, constantTimeSeriesToDanglingLinesMapping, timeSeriesToDanglingLinesMapping);

        // Check if some hvdc lines mappings are constant
        EquipmentTimeSeriesMap timeSeriesToHvdcLinesMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToHvdcLinesMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToHvdcLinesMapping, constantTimeSeriesToHvdcLinesMapping, timeSeriesToHvdcLinesMapping);

        // Check if some phase tap changers mappings are constant
        EquipmentTimeSeriesMap timeSeriesToPhaseTapChangersMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToPhaseTapChangersMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToPhaseTapChangersMapping, constantTimeSeriesToPhaseTapChangersMapping, timeSeriesToPhaseTapChangersMapping);

        // Check if some breaker mappings are constant
        EquipmentTimeSeriesMap timeSeriesToBreakersMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToBreakersMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToBreakersMapping, constantTimeSeriesToBreakersMapping, timeSeriesToBreakersMapping);

        // Check if some transformers mappings are constant
        EquipmentTimeSeriesMap timeSeriesToTransformersMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToTransformersMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToTransformersMapping, constantTimeSeriesToTransformersMapping, timeSeriesToTransformersMapping);

        // Check if some tap changers mappings are constant
        EquipmentTimeSeriesMap timeSeriesToRatioTapChangersMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToRatioTapChangersMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToRatioTapChangersMapping, constantTimeSeriesToRatioTapChangersMapping, timeSeriesToRatioTapChangersMapping);

        // Check if some lcc converters mappings are constant
        EquipmentTimeSeriesMap timeSeriesToLccConverterStationsMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToLccConverterStationsMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToLccConverterStationsMapping, constantTimeSeriesToLccConverterStationsMapping, timeSeriesToLccConverterStationsMapping);

        // Check if some vsc converters mappings are constant
        EquipmentTimeSeriesMap timeSeriesToVscConverterStationsMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToVscConverterStationsMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToVscConverterStationsMapping, constantTimeSeriesToVscConverterStationsMapping, timeSeriesToVscConverterStationsMapping);

        // Check if some lines mappings are constant
        EquipmentTimeSeriesMap timeSeriesToLinesMapping = new EquipmentTimeSeriesMap();
        EquipmentTimeSeriesMap constantTimeSeriesToLinesMapping = new EquipmentTimeSeriesMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToLinesMapping, constantTimeSeriesToLinesMapping, timeSeriesToLinesMapping);

        // Check if some equipement mappings are constant
        Map<IndexedName, Set<MappingKey>> equipmentTimeSeries = new HashMap<>();
        Map<IndexedName, Set<MappingKey>> constantEquipmentTimeSeries = new HashMap<>();
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
                    correctUnmappedGenerator(isMinPUnmapped, isMaxPUnmapped, g, version, parameters.isIgnoreLimits(), table.getTableIndex());
                });
        network.getHvdcLineStream()
                .filter(l -> config.getUnmappedHvdcLines().contains(l.getId()))
                .forEach(l -> {
                    final boolean isMinPUnmapped = config.getUnmappedMinPHvdcLines().contains(l.getId());
                    final boolean isMaxPUnmapped = config.getUnmappedMaxPHvdcLines().contains(l.getId());
                    correctUnmappedHvdcLine(isMinPUnmapped, isMaxPUnmapped, l, version, parameters.isIgnoreLimits(), table.getTableIndex());
                });

        // process constant time series
        if (observer != null) {
            observer.timeSeriesMappingStart(CONSTANT_VARIANT_ID, table.getTableIndex());
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

            mapSinglePoint(parameters, version, CONSTANT_VARIANT_ID, firstPoint,
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
                    constantEquipmentTimeSeries,
                    observer);
        }

        // for unmapped equipments, keep base case value which is constant
        double constantBalance = 0;
        Set<String> unmappedGenerators = new HashSet<>(config.getUnmappedGenerators());
        unmappedGenerators.retainAll(config.getUnmappedMinPGenerators());
        unmappedGenerators.retainAll(config.getUnmappedMaxPGenerators());
        for (String id : unmappedGenerators) {
            Generator generator = network.getGenerator(id);
            if (generator == null) {
                throw new TimeSeriesMappingException("Generator '" + id + "' not found");
            }
            constantBalance += generator.getTargetP();
        }
        Set<String> unmappedLoads = config.getUnmappedLoads();
        for (String id : unmappedLoads) {
            Load load = network.getLoad(id);
            if (load == null) {
                throw new TimeSeriesMappingException("Load '" + id + "' not found");
            }
            constantBalance += -load.getP0();
        }
        for (String id : config.getUnmappedFixedActivePowerLoads()) {
            if (!unmappedLoads.contains(id)) {
                Load load = network.getLoad(id);
                if (load == null) {
                    throw new TimeSeriesMappingException("Load '" + id + "' not found");
                }
                LoadDetail loadDetail = load.getExtension(LoadDetail.class);
                if (loadDetail == null) {
                    throw new TimeSeriesMappingException("LoadDetail '" + id + "' not found");
                }
                constantBalance += -loadDetail.getFixedActivePower();
            }
        }
        for (String id : config.getUnmappedVariableActivePowerLoads()) {
            if (!unmappedLoads.contains(id)) {
                Load load = network.getLoad(id);
                if (load == null) {
                    throw new TimeSeriesMappingException("Load '" + id + "' not found");
                }
                LoadDetail loadDetail = load.getExtension(LoadDetail.class);
                if (loadDetail == null) {
                    throw new TimeSeriesMappingException("LoadDetail '" + id + "' not found");
                }
                constantBalance += -loadDetail.getVariableActivePower();
            }
        }
        for (String id : config.getUnmappedDanglingLines()) {
            DanglingLine danglingLine = network.getDanglingLine(id);
            if (danglingLine == null) {
                throw new TimeSeriesMappingException("Dangling line '" + id + "' not found");
            }
            constantBalance += -danglingLine.getP0();
        }

        if (observer != null) {
            observer.timeSeriesMappingEnd(CONSTANT_VARIANT_ID, table.getTableIndex(), constantBalance);
        }

        // process each time point
        for (int point = firstPoint; point <= lastPoint; point++) {

            if (observer != null) {
                observer.timeSeriesMappingStart(point, table.getTableIndex());
            }

            mapSinglePoint(parameters, version, point, point,
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
                equipmentTimeSeries,
                observer);

            if (observer != null) {
                observer.timeSeriesMappingEnd(point, table.getTableIndex(), 0);
            }
        }
    }

    private void mapSinglePoint(TimeSeriesMapperParameters parameters,
                                int version, int variantId, int point,
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
                                Map<IndexedName, Set<MappingKey>> equipmentTimeSeries,
                                TimeSeriesMapperChecker observer) {

        // process time series for mapping
        mapToNetwork(version, variantId, point, timeSeriesToLoadsMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToGeneratorsMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToDanglingLinesMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToHvdcLinesMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToPhaseTapChangersMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToBreakersMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToTransformersMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToRatioTapChangersMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToLccConverterStationsMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToVscConverterStationsMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(version, variantId, point, timeSeriesToLinesMapping, observer, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());

        if (observer != null) {
            // generic mapping
            if (CONSTANT_VARIANT_ID != variantId) {
                observer.map(version, point, table);
            }

            // process equipment time series
            for (Map.Entry<IndexedName, Set<MappingKey>> e : equipmentTimeSeries.entrySet()) {
                String timeSeriesName = e.getKey().getName();
                int timeSeriesNum = e.getKey().getNum();
                double timeSeriesValue = table.getDoubleValue(version, timeSeriesNum, point);
                for (MappingKey equipment : e.getValue()) {
                    observer.timeSeriesMappedToEquipment(point, timeSeriesName, network.getIdentifiable(equipment.getId()), equipment.getMappingVariable(), timeSeriesValue);
                }
            }
        }
    }

    public void mapToNetwork(ReadOnlyTimeSeriesStore store, TimeSeriesMapperParameters parameters,
                                                List<TimeSeriesMapperObserver> observers) {

        TimeSeriesMapperChecker checker = new TimeSeriesMapperChecker(observers, timeSeriesMappingLogger, parameters);

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
                            .collect(Collectors.toMap(e -> new IndexedName(e.getKey(), table.getDoubleTimeSeriesIndex(e.getKey())), Map.Entry::getValue));
                }

                mapToNetwork(context, parameters, version, checker);
            } finally {
                checker.versionEnd(version);
            }
        }

        checker.end();

        timeSeriesMappingLogger.printLogSynthesis();
    }
}
