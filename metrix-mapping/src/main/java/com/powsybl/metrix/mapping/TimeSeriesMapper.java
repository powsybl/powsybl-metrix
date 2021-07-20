/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRangeAdder;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.metrix.mapping.timeseries.MappedEquipment;
import com.powsybl.metrix.mapping.timeseries.EquipmentTimeSerieMap;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesTable;
import com.powsybl.metrix.mapping.TimeSeriesMappingLogger.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TimeSeriesMapper implements TimeSeriesConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMapper.class);

    public static final double EPSILON_COMPARISON = 1e-5;

    private static final double EPSILON_ZERO_STD_DEV = 1e-6;

    public static final int CONSTANT_VARIANT_ID = -1;

    public static final int SWITCH_OPEN = 0; // 0 means switch is open

    public static final int DISCONNECTED_VALUE = 0; // 0 means equipment is disconnected

    public static final int CONNECTED_VALUE = 1;

    private final TimeSeriesMappingConfig config;

    private final Network network;

    private final TimeSeriesMappingLogger logger;

    private static class MapperContext {
        private EquipmentTimeSerieMap timeSeriesToLoadsMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToGeneratorsMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToDanglingLinesMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToHvdcLinesMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToPhaseTapChangersMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToBreakersMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToTransformersMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToRatioTapChangersMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToLccConverterStationsMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToVscConverterStationsMapping = new EquipmentTimeSerieMap();
        private EquipmentTimeSerieMap timeSeriesToLinesMapping = new EquipmentTimeSerieMap();
        private Map<IndexedName, Set<MappingKey>> equipmentTimeSeries;
    }

    public TimeSeriesMapper(TimeSeriesMappingConfig config, Network network, TimeSeriesMappingLogger logger) {
        this.config = Objects.requireNonNull(config);
        this.network = Objects.requireNonNull(network);
        this.logger = Objects.requireNonNull(logger);
    }

    public static void setMax(Identifiable<?> identifiable, double max) {
        if (identifiable instanceof Generator) {
            ((Generator) identifiable).setMaxP(max);
        } else if (identifiable instanceof HvdcLine) {
            HvdcLine hvdcLine = (HvdcLine) identifiable;
            HvdcOperatorActivePowerRange activePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
            if (activePowerRange != null) {
                hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS1toCS2((float) Math.abs(max));
            }
            hvdcLine.setMaxP(Math.max(hvdcLine.getMaxP(), Math.abs(max)));
        }
    }

    public static void setMin(Identifiable<?> identifiable, double min) {
        if (identifiable instanceof Generator) {
            ((Generator) identifiable).setMinP(min);
        } else if (identifiable instanceof HvdcLine) {
            HvdcLine hvdcLine = (HvdcLine) identifiable;
            HvdcOperatorActivePowerRange activePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
            if (activePowerRange != null) {
                hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS2toCS1((float) Math.abs(min));
            }
            hvdcLine.setMaxP(Math.max(hvdcLine.getMaxP(), Math.abs(min)));
        }
    }

    public static float getMax(Identifiable identifiable) {
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

    private static void setHvdcLineSetPoint(HvdcLine hvdcLine, double setPoint) {
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

    private void mapToNetwork(TimeSeriesTable table, int version, int variantId, int point,
                              IndexedMappingKey mappingKey, List<MappedEquipment> mappedEquipments,
                              TimeSeriesMapperChecker observer, TimeSeriesMappingLogger logger, boolean ignoreLimits,
                              boolean ignoreEmptyFilter) {
        String timeSeriesName = mappingKey.getKey().getId();
        MappingVariable variable = mappingKey.getKey().getMappingVariable();

        // compute distribution key associated to equipment list
        double[] distributionKeys = new double[mappedEquipments.size()];
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

        double[] equipmentValues = new double[mappedEquipments.size()];
        Arrays.fill(equipmentValues, 0);

        double timeSeriesValue = table.getDoubleValue(version, mappingKey.getNum(), point);
        if (Double.isNaN(timeSeriesValue) || Double.isInfinite(timeSeriesValue)) {
            throw new TimeSeriesMappingException("Impossible to scale down " + timeSeriesValue + " of ts " + timeSeriesName + " at time index '" + table.getTableIndex().getInstantAt(point) + "' and version " + version);
        }
        if (Math.abs(timeSeriesValue) > 0) {
            // check equipment list is not empty
            if (mappedEquipments.isEmpty()) {
                TimeSeriesMappingLogger.Log log = new TimeSeriesMappingLogger.EmptyFilterWarning(timeSeriesName, timeSeriesValue, table.getTableIndex(), version, point);
                if (ignoreEmptyFilter) {
                    logger.addLog(log);
                } else {
                    throw new TimeSeriesMappingException(log.getMessage());
                }
            } else {
                if (distributionKeySum == 0) {
                    double distributionKey = NumberDistributionKey.ONE.getValue();
                    for (int i = 0; i < mappedEquipments.size(); i++) {
                        distributionKeys[i] = distributionKey;
                        distributionKeySum += distributionKeys[i];
                    }
                    logger.addLog(new ZeroDistributionKeyInfo(timeSeriesName, timeSeriesValue, table.getTableIndex(), version, variantId,
                            mappedEquipments.stream().map(MappedEquipment::getId).collect(Collectors.toList())));
                }

                if (mappedEquipments.get(0).getIdentifiable() instanceof HvdcLine) {
                    if (variable == EquipmentVariable.maxP && timeSeriesValue < 0) {
                        logger.addLog(new LimitMaxSign(table.getTableIndex(), version, variantId, timeSeriesName, EquipmentVariable.maxP.getVariableName(), timeSeriesValue));
                        return;
                    } else if (variable == EquipmentVariable.minP && timeSeriesValue > 0) {
                        logger.addLog(new LimitMinSign(table.getTableIndex(), version, variantId, timeSeriesName, EquipmentVariable.minP.getVariableName(), timeSeriesValue));
                        return;
                    }
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

    private void mapToNetwork(TimeSeriesTable table, int version, int variantId, int point,
                              EquipmentTimeSerieMap timeSeriesToEquipmentsMapping,
                              TimeSeriesMapperChecker observer, TimeSeriesMappingLogger logger, boolean ignoreLimits,
                              boolean ignoreEmptyFilter) {
        timeSeriesToEquipmentsMapping.getEquipmentTimeSeries().forEach((mappingKey, mappedEquipments) ->
            mapToNetwork(table, version, variantId, point, mappingKey, mappedEquipments, observer, logger, ignoreLimits, ignoreEmptyFilter));
    }

    public static IndexedMappingKey indexMappingKey(TimeSeriesTable timeSeriesTable, MappingKey key) {
        return new IndexedMappingKey(key, timeSeriesTable.getDoubleTimeSeriesIndex(key.getId()));
    }

    private void identifyConstantTimeSeries(boolean forceNoConstantTimeSeries, TimeSeriesTable table, int version,
                                            EquipmentTimeSerieMap sourceTimeSeries,
                                            EquipmentTimeSerieMap constantTimeSeries,
                                            EquipmentTimeSerieMap variableTimeSeries) {
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
                        LOGGER.debug("Mapping time-series '" + indexedMappingKey.getKey().getId() + "' is constant");
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
                                                EquipmentTimeSerieMap constantTimeSeries,
                                                EquipmentTimeSerieMap variableTimeSeries) {

        if (forceNoConstantTimeSeries) {
            variableTimeSeries.init(context.timeSeriesToLoadsMapping);
            return;
        }

        EquipmentTimeSerieMap possiblyConstantLoadDetailsMapping = new EquipmentTimeSerieMap();
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
                                                  EquipmentTimeSerieMap constantTimeSeries,
                                                  EquipmentTimeSerieMap variableTimeSeries,
                                                  EquipmentTimeSerieMap possiblyConstantLoadDetailsMapping,
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
                                          int version, boolean ignoreLimits, TimeSeriesIndex index, TimeSeriesMappingLogger logger) {

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
                .setAll(generator, logger);

    }

    private void correctUnmappedHvdcLine(boolean isUnmappedMinP, boolean isUnmappedMaxP, HvdcLine hvdcLine, int version,
                                         boolean ignoreLimits, TimeSeriesIndex index, TimeSeriesMappingLogger logger) {

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

        // maxP inconstancy with CS1toCS2/CS2toCS1
        if (isActivePowerRange && (maxP > hvdcLineMaxP || -minP > hvdcLineMaxP)) {
            if (ignoreLimits) {
                if (((maxP > hvdcLineMaxP && -minP > hvdcLineMaxP) && maxP > -minP) || maxP > hvdcLineMaxP && -minP <= hvdcLineMaxP) {
                    logger.addLog(new BaseCaseRangeInfoWithVariableChange(version, index, CONSTANT_VARIANT_ID, maxPVariableName, EquipmentVariable.maxP.getVariableName(), maxPVariableName, id, 0, hvdcLineMaxP, maxP, maxP));
                } else if (((maxP > hvdcLineMaxP && -minP < hvdcLineMaxP) && -minP > maxP) || (-minP > hvdcLineMaxP && maxP <= hvdcLineMaxP)) {
                    logger.addLog(new BaseCaseRangeInfoWithVariableChange(version, index, CONSTANT_VARIANT_ID, minPVariableName, MINUS_MAXP, minPVariableName, id, -hvdcLineMaxP, 0, minP, minP));
                }
                hvdcLine.setMaxP(Math.max(maxP, -minP));
            } else {
                if (maxP > hvdcLineMaxP) {
                    logger.addLog(new BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, maxPVariableName, maxPVariableName, EquipmentVariable.maxP.getVariableName(), id, 0, hvdcLineMaxP, maxP, hvdcLineMaxP));
                    hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS1toCS2((float) hvdcLineMaxP);
                    correctedMaxP = hvdcLineMaxP;
                }
                if (minP < -hvdcLineMaxP) {
                    logger.addLog(new BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, minPVariableName, minPVariableName, MINUS_MAXP, id, -hvdcLineMaxP, 0, minP, -hvdcLineMaxP));
                    hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS2toCS1((float) hvdcLineMaxP);
                    correctedMinP = -hvdcLineMaxP;
                }
            }
        }

        // setpoint inconstancy with maxP/CS1toCS2/CS2toCS1
        if (setpoint > correctedMaxP && isUnmappedMaxP) {
            if (ignoreLimits) {
                logger.addLog(new BaseCaseRangeInfoWithVariableChange(version, index, CONSTANT_VARIANT_ID, setPointVariableName, maxPVariableName, setPointVariableName, id, minP, maxP, setpoint, setpoint));
                TimeSeriesMapper.setMax(hvdcLine, setpoint);
            } else {
                logger.addLog(new BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, setPointVariableName, setPointVariableName, maxPVariableName, id, minP, maxP, setpoint, maxP));
                TimeSeriesMapper.setHvdcLineSetPoint(hvdcLine, maxP);
            }
        } else if (setpoint < correctedMinP && isUnmappedMinP) {
            if (ignoreLimits) {
                logger.addLog(new BaseCaseRangeInfoWithVariableChange(version, index, CONSTANT_VARIANT_ID, setPointVariableName, minPVariableName, setPointVariableName, id, minP, maxP, setpoint, setpoint));
                TimeSeriesMapper.setMin(hvdcLine, setpoint);
            } else {
                logger.addLog(new BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, setPointVariableName, setPointVariableName, minPVariableName, id, minP, maxP, setpoint, minP));
                TimeSeriesMapper.setHvdcLineSetPoint(hvdcLine, minP);
            }
        }
    }

    private void mapToNetwork(MapperContext context, TimeSeriesTable table, TimeSeriesMapperParameters parameters,
                              int version, TimeSeriesMapperChecker observer, TimeSeriesMappingLogger logger) {

        int firstPoint = parameters.getPointRange() != null ? parameters.getPointRange().lowerEndpoint() : 0;
        int lastPoint = parameters.getPointRange() != null ? parameters.getPointRange().upperEndpoint() : (table.getTableIndex().getPointCount() - 1);
        boolean forceNoConstantTimeSeries = !parameters.isIdentifyConstantTimeSeries();

        // Check if some load mappings are constant
        EquipmentTimeSerieMap timeSeriesToLoadsMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToLoadsMapping = new EquipmentTimeSerieMap();
        identifyConstantLoadTimeSeries(forceNoConstantTimeSeries, table, version, context, constantTimeSeriesToLoadsMapping, timeSeriesToLoadsMapping);

        // Check if some generator mappings are constant
        EquipmentTimeSerieMap timeSeriesToGeneratorsMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToGeneratorsMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToGeneratorsMapping, constantTimeSeriesToGeneratorsMapping, timeSeriesToGeneratorsMapping);

        // Check if some dangling lines mappings are constant
        EquipmentTimeSerieMap timeSeriesToDanglingLinesMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToDanglingLinesMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToDanglingLinesMapping, constantTimeSeriesToDanglingLinesMapping, timeSeriesToDanglingLinesMapping);

        // Check if some hvdc lines mappings are constant
        EquipmentTimeSerieMap timeSeriesToHvdcLinesMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToHvdcLinesMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToHvdcLinesMapping, constantTimeSeriesToHvdcLinesMapping, timeSeriesToHvdcLinesMapping);

        // Check if some phase tap changers mappings are constant
        EquipmentTimeSerieMap timeSeriesToPhaseTapChangersMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToPhaseTapChangersMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToPhaseTapChangersMapping, constantTimeSeriesToPhaseTapChangersMapping, timeSeriesToPhaseTapChangersMapping);

        // Check if some breaker mappings are constant
        EquipmentTimeSerieMap timeSeriesToBreakersMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToBreakersMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToBreakersMapping, constantTimeSeriesToBreakersMapping, timeSeriesToBreakersMapping);

        // Check if some transformers mappings are constant
        EquipmentTimeSerieMap timeSeriesToTransformersMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToTransformersMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToTransformersMapping, constantTimeSeriesToTransformersMapping, timeSeriesToTransformersMapping);

        // Check if some tap changers mappings are constant
        EquipmentTimeSerieMap timeSeriesToRatioTapChangersMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToRatioTapChangersMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToRatioTapChangersMapping, constantTimeSeriesToRatioTapChangersMapping, timeSeriesToRatioTapChangersMapping);

        // Check if some lcc converters mappings are constant
        EquipmentTimeSerieMap timeSeriesToLccConverterStationsMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToLccConverterStationsMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToLccConverterStationsMapping, constantTimeSeriesToLccConverterStationsMapping, timeSeriesToLccConverterStationsMapping);

        // Check if some vsc converters mappings are constant
        EquipmentTimeSerieMap timeSeriesToVscConverterStationsMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToVscConverterStationsMapping = new EquipmentTimeSerieMap();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToVscConverterStationsMapping, constantTimeSeriesToVscConverterStationsMapping, timeSeriesToVscConverterStationsMapping);

        // Check if some lines mappings are constant
        EquipmentTimeSerieMap timeSeriesToLinesMapping = new EquipmentTimeSerieMap();
        EquipmentTimeSerieMap constantTimeSeriesToLinesMapping = new EquipmentTimeSerieMap();
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
                    correctUnmappedGenerator(isMinPUnmapped, isMaxPUnmapped, g, version, parameters.isIgnoreLimits(), table.getTableIndex(), logger);
                });
        network.getHvdcLineStream()
                .filter(l -> config.getUnmappedHvdcLines().contains(l.getId()))
                .forEach(l -> {
                    final boolean isMinPUnmapped = config.getUnmappedMinPHvdcLines().contains(l.getId());
                    final boolean isMaxPUnmapped = config.getUnmappedMaxPHvdcLines().contains(l.getId());
                    correctUnmappedHvdcLine(isMinPUnmapped, isMaxPUnmapped, l, version, parameters.isIgnoreLimits(), table.getTableIndex(), logger);
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

            mapSinglePoint(table, parameters, version, CONSTANT_VARIANT_ID, firstPoint,
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
                    observer, logger);
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

            mapSinglePoint(table, parameters, version, point, point,
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
                observer, logger);

            if (observer != null) {
                observer.timeSeriesMappingEnd(point, table.getTableIndex(), 0);
            }
        }
    }

    private void mapSinglePoint(TimeSeriesTable table, TimeSeriesMapperParameters parameters,
                                int version, int variantId, int point,
                                EquipmentTimeSerieMap timeSeriesToLoadsMapping,
                                EquipmentTimeSerieMap timeSeriesToGeneratorsMapping,
                                EquipmentTimeSerieMap timeSeriesToDanglingLinesMapping,
                                EquipmentTimeSerieMap timeSeriesToHvdcLinesMapping,
                                EquipmentTimeSerieMap timeSeriesToPhaseTapChangersMapping,
                                EquipmentTimeSerieMap timeSeriesToBreakersMapping,
                                EquipmentTimeSerieMap timeSeriesToTransformersMapping,
                                EquipmentTimeSerieMap timeSeriesToRatioTapChangersMapping,
                                EquipmentTimeSerieMap timeSeriesToLccConverterStationsMapping,
                                EquipmentTimeSerieMap timeSeriesToVscConverterStationsMapping,
                                EquipmentTimeSerieMap timeSeriesToLinesMapping,
                                Map<IndexedName, Set<MappingKey>> equipmentTimeSeries,
                                TimeSeriesMapperChecker observer, TimeSeriesMappingLogger logger) {

        // process time series for mapping
        mapToNetwork(table, version, variantId, point, timeSeriesToLoadsMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToGeneratorsMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToDanglingLinesMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToHvdcLinesMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToPhaseTapChangersMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToBreakersMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToTransformersMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToRatioTapChangersMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToLccConverterStationsMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToVscConverterStationsMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());
        mapToNetwork(table, version, variantId, point, timeSeriesToLinesMapping, observer, logger, parameters.isIgnoreLimits(), parameters.isIgnoreEmptyFilter());

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

    public TimeSeriesMappingLogger mapToNetwork(ReadOnlyTimeSeriesStore store, TimeSeriesMapperParameters parameters,
                                                List<TimeSeriesMapperObserver> observers) {

        TimeSeriesMapperChecker checker = new TimeSeriesMapperChecker(observers, logger, parameters);

        checker.start();

        // create context
        MapperContext context = null;
        for (int version : parameters.getVersions()) {

            checker.versionStart(version);

            try {
                // load time series involved in the config in a table
                Set<String> usedTimeSeriesNames = StreamSupport.stream(config.findUsedTimeSeriesNames().spliterator(), false).collect(Collectors.toSet());
                usedTimeSeriesNames.addAll(parameters.getRequiredTimeseries());
                ReadOnlyTimeSeriesStore storeWithPlannedOutages = TimeSeriesMappingConfig.buildPlannedOutagesTimeSeriesStore(store, version, config.getTimeSeriesToPlannedOutagesMapping());
                TimeSeriesTable table = config.loadToTable(new TreeSet<>(ImmutableSet.of(version)), storeWithPlannedOutages, parameters.getPointRange(), usedTimeSeriesNames);

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

                mapToNetwork(context, table, parameters, version, checker, logger);
            } finally {
                checker.versionEnd(version);
            }
        }

        checker.end();

        logger.printLogSynthesis();

        return logger;
    }
}
