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
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesTable;
import com.powsybl.metrix.mapping.TimeSeriesMappingLogger.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class TimeSeriesMapper implements TimeSeriesConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMapper.class);

    private static final double EPSILON_COMPARISON = 1e-5;

    private static final double EPSILON_ZERO_STD_DEV = 1e-6;

    public static final int CONSTANT_VARIANT_ID = -1;

    public static final int SWITCH_OPEN = 0; // 0 means switch is open

    private final TimeSeriesMappingConfig config;

    private final Network network;

    private final TimeSeriesMappingLogger logger;

    private static class MapperContext {

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToLoadsMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToGeneratorsMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToDanglingLinesMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToHvdcLinesMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToPhaseTapChangersMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToBreakersMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToTransformersMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToRatioTapChangersMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToLccConverterStationsMapping;

        private Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToVscConverterStationsMapping;

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

    static class MappedEquipment {

        private final Identifiable<?> identifiable;

        private final DistributionKey distributionKey;

        public MappedEquipment(Identifiable<?> identifiable, DistributionKey distributionKey) {
            this.identifiable = Objects.requireNonNull(identifiable);
            this.distributionKey = distributionKey;
        }

        public Identifiable<?> getIdentifiable() {
            return identifiable;
        }

        public String getId() {
            return identifiable.getId();
        }

        public DistributionKey getDistributionKey() {
            return distributionKey;
        }
    }

    private static Identifiable<?> getIdentifiable(Network network, String equipmentId) {
        Identifiable<?> identifiable = network.getIdentifiable(equipmentId);
        // check equipment exists
        if (identifiable == null) {
            throw new TimeSeriesMappingException("'" + equipmentId + "' not found");
        }
        return identifiable;
    }

    private List<MappedEquipment> mapEquipments(MappingKey key, List<String> equipmentIds) {
        return equipmentIds.stream().map(equipmentId -> {
            Identifiable<?> identifiable = getIdentifiable(network, equipmentId);
            DistributionKey distributionKey = config.getDistributionKey(new MappingKey(key.getMappingVariable(), equipmentId));
            return new MappedEquipment(identifiable, distributionKey);
        }).collect(Collectors.toList());
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
                              Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToEquipmentsMapping,
                              TimeSeriesMapperChecker observer, TimeSeriesMappingLogger logger, boolean ignoreLimits,
                              boolean ignoreEmptyFilter) {
        timeSeriesToEquipmentsMapping.forEach((mappingKey, mappedEquipments) ->
            mapToNetwork(table, version, variantId, point, mappingKey, mappedEquipments, observer, logger, ignoreLimits, ignoreEmptyFilter));
    }

    private static IndexedMappingKey indexMappingKey(TimeSeriesTable timeSeriesTable, MappingKey key) {
        return new IndexedMappingKey(key, timeSeriesTable.getDoubleTimeSeriesIndex(key.getId()));
    }

    private void identifyConstantTimeSeries(boolean forceNoConstantTimeSeries, TimeSeriesTable table, int version,
                                            Map<IndexedMappingKey, List<MappedEquipment>> sourceTimeSeries,
                                            Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeries,
                                            Map<IndexedMappingKey, List<MappedEquipment>> variableTimeSeries) {
        if (forceNoConstantTimeSeries) {
            variableTimeSeries.putAll(sourceTimeSeries);
        } else {
            sourceTimeSeries.forEach((indexedMappingKey, mappedEquipments) -> {
                int timeSeriesNum = indexedMappingKey.getNum();
                MappingVariable variable = indexedMappingKey.getKey().getMappingVariable();
                if (variable == EquipmentVariable.targetP ||
                        variable == EquipmentVariable.activePowerSetpoint) {
                    // Active power mapping is not tested in order to allow later correction of values not included in [minP, maxP]
                    variableTimeSeries.put(indexedMappingKey, mappedEquipments);
                } else {
                    if (table.getStdDev(version, timeSeriesNum) < EPSILON_ZERO_STD_DEV) { // std dev == 0 means time-series is constant
                        LOGGER.debug("Mapping time-series '" + indexedMappingKey.getKey().getId() + "' is constant");
                        constantTimeSeries.put(indexedMappingKey, mappedEquipments);
                    } else {
                        variableTimeSeries.put(indexedMappingKey, mappedEquipments);
                    }
                }
            });
        }
    }

    private void identifyConstantLoadTimeSeries(boolean forceNoConstantTimeSeries, TimeSeriesTable table, int version,
                                                MapperContext context,
                                                Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeries,
                                                Map<IndexedMappingKey, List<MappedEquipment>> variableTimeSeries) {

        if (forceNoConstantTimeSeries) {
            variableTimeSeries.putAll(context.timeSeriesToLoadsMapping);
        } else {
            Map<IndexedMappingKey, List<MappedEquipment>> possiblyConstantLoadDetailsMapping = new HashMap<>();
            Set<String> variableLoadDetailsIds = new HashSet<>();

            context.timeSeriesToLoadsMapping.forEach((indexedMappingKey, mappedEquipments) -> {
                int timeSeriesNum = indexedMappingKey.getNum();
                MappingVariable variable = indexedMappingKey.getKey().getMappingVariable();

                if (variable != EquipmentVariable.p0 &&
                        variable != EquipmentVariable.fixedActivePower &&
                        variable != EquipmentVariable.variableActivePower) {
                    // Only test if active load power mapping is constant
                    variableTimeSeries.put(indexedMappingKey, mappedEquipments);
                } else {
                    if (table.getStdDev(version, timeSeriesNum) < EPSILON_ZERO_STD_DEV) { // std dev == 0 means time-series is constant
                        LOGGER.debug("Mapping time-series '" + indexedMappingKey.getKey().getId() + "' is constant");
                        if (variable == EquipmentVariable.p0) {
                            constantTimeSeries.put(indexedMappingKey, mappedEquipments);
                        } else {
                            possiblyConstantLoadDetailsMapping.put(indexedMappingKey, mappedEquipments);
                        }
                    } else {
                        variableTimeSeries.put(indexedMappingKey, mappedEquipments);
                        if (variable != EquipmentVariable.p0) {
                            variableLoadDetailsIds.addAll(mappedEquipments.stream()
                                    .map(mappedEquipment -> mappedEquipment.getIdentifiable().getId())
                                    .collect(Collectors.toList()));
                        }
                    }
                }
            });
            possiblyConstantLoadDetailsMapping.forEach((indexedMappingKey, mappedEquipments) -> {
                mappedEquipments.forEach(mappedEquipment -> {
                    if (variableLoadDetailsIds.contains(mappedEquipment.getIdentifiable().getId())) {
                        variableTimeSeries.computeIfAbsent(indexedMappingKey, i -> new ArrayList<>()).add(mappedEquipment);
                    } else {
                        constantTimeSeries.computeIfAbsent(indexedMappingKey, i -> new ArrayList<>()).add(mappedEquipment);
                    }
                });
            });
        }
    }

    private void correctUnmappedGenerator(boolean isUnmappedMinP, boolean isUnmappedMaxP, Generator generator, int version, boolean ignoreLimits, TimeSeriesIndex index, TimeSeriesMappingLogger logger) {

        final double minP = generator.getMinP();
        final double maxP = generator.getMaxP();
        final double targetP = generator.getTargetP();
        final String targetPVariableName = EquipmentVariable.targetP.getVariableName();

        final String id = generator.getId();

        final boolean isOkMinP = targetP >= minP;
        final boolean isOkMaxP = targetP <= maxP;

        if (minP > maxP) {
            throw new AssertionError("Equipment '" + generator.getId() + "' : invalid active limits [" + minP + ", " + maxP + "] in base case");
        }

        if (!isOkMaxP && isUnmappedMaxP) {
            if (ignoreLimits) {
                logger.addLog(new BaseCaseRangeInfoWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName, EquipmentVariable.maxP.getVariableName(), targetPVariableName, id, minP, maxP, targetP, targetP));
                generator.setMaxP(targetP);
            } else {
                logger.addLog(new BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName, targetPVariableName, EquipmentVariable.maxP.getVariableName(), id, minP, maxP, targetP, maxP));
                generator.setTargetP(maxP);
            }
        } else if (!isOkMinP && isUnmappedMinP) {
            if (minP <= 0) {
                if (ignoreLimits) {
                    logger.addLog(new BaseCaseRangeInfoWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName, EquipmentVariable.minP.getVariableName(), targetPVariableName, id, minP, maxP, targetP, targetP));
                    generator.setMinP(targetP);
                } else {
                    logger.addLog(new BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName, targetPVariableName, EquipmentVariable.minP.getVariableName(), id, minP, maxP, targetP, minP));
                    generator.setTargetP(minP);
                }
            } else if (targetP < 0) {
                if (ignoreLimits) {
                    logger.addLog(new BaseCaseRangeWarningWithVariableChangeDisabled(version, index, CONSTANT_VARIANT_ID, targetPVariableName, targetPVariableName, "", id, minP, maxP, targetP, 0));
                } else {
                    logger.addLog(new BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName, targetPVariableName, "", id, minP, maxP, targetP, 0));
                }
                generator.setTargetP(0);
            } else {
                logger.addLog(new BaseCaseRangeInfoWithBaseCaseMinPViolatedByBaseCaseTargetP(version, index, CONSTANT_VARIANT_ID, targetPVariableName, targetPVariableName, id, minP, maxP, targetP));
            }
        }
    }

    private void correctUnmappedHvdcLine(boolean isUnmappedMinP, boolean isUnmappedMaxP, HvdcLine hvdcLine, int version, boolean ignoreLimits, TimeSeriesIndex index, TimeSeriesMappingLogger logger) {

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
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToLoadsMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToLoadsMapping = new LinkedHashMap<>();
        identifyConstantLoadTimeSeries(forceNoConstantTimeSeries, table, version, context, constantTimeSeriesToLoadsMapping, timeSeriesToLoadsMapping);

        // Check if some generator mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToGeneratorsMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToGeneratorsMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToGeneratorsMapping, constantTimeSeriesToGeneratorsMapping, timeSeriesToGeneratorsMapping);

        // Check if some dangling lines mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToDanglingLinesMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToDanglingLinesMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToDanglingLinesMapping, constantTimeSeriesToDanglingLinesMapping, timeSeriesToDanglingLinesMapping);

        // Check if some hvdc lines mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToHvdcLinesMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToHvdcLinesMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToHvdcLinesMapping, constantTimeSeriesToHvdcLinesMapping, timeSeriesToHvdcLinesMapping);

        // Check if some phase tap changers mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToPhaseTapChangersMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToPhaseTapChangersMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToPhaseTapChangersMapping, constantTimeSeriesToPhaseTapChangersMapping, timeSeriesToPhaseTapChangersMapping);

        // Check if some breaker mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToBreakersMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToBreakersMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToBreakersMapping, constantTimeSeriesToBreakersMapping, timeSeriesToBreakersMapping);

        // Check if some transformers mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToTransformersMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToTransformersMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToTransformersMapping, constantTimeSeriesToTransformersMapping, timeSeriesToTransformersMapping);

        // Check if some tap changers mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToRatioTapChangersMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToRatioTapChangersMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToRatioTapChangersMapping, constantTimeSeriesToRatioTapChangersMapping, timeSeriesToRatioTapChangersMapping);

        // Check if some lcc converters mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToLccConverterStationsMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToLccConverterStationsMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToLccConverterStationsMapping, constantTimeSeriesToLccConverterStationsMapping, timeSeriesToLccConverterStationsMapping);

        // Check if some vsc converters mappings are constant
        Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToVscConverterStationsMapping = new LinkedHashMap<>();
        Map<IndexedMappingKey, List<MappedEquipment>> constantTimeSeriesToVscConverterStationsMapping = new LinkedHashMap<>();
        identifyConstantTimeSeries(forceNoConstantTimeSeries, table, version, context.timeSeriesToVscConverterStationsMapping, constantTimeSeriesToVscConverterStationsMapping, timeSeriesToVscConverterStationsMapping);

        // Check if some equipement mappings are constant
        Map<IndexedName, Set<MappingKey>> equipmentTimeSeries = new HashMap<>();
        Map<IndexedName, Set<MappingKey>> constantEquipmentTimeSeries = new HashMap<>();
        context.equipmentTimeSeries.forEach((indexedName, mappingKeys) -> {
            int timeSeriesNum = indexedName.getNum();
            if (table.getStdDev(version, timeSeriesNum) < EPSILON_COMPARISON) { // std dev == 0 means time-series is constant
                LOGGER.debug("Equipment time-series '" + indexedName.getName() + "' is constant");
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
                equipmentTimeSeries,
                observer, logger);

            if (observer != null) {
                observer.timeSeriesMappingEnd(point, table.getTableIndex(), 0);
            }
        }
    }

    private void mapSinglePoint(TimeSeriesTable table, TimeSeriesMapperParameters parameters,
                                int version, int variantId, int point,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToLoadsMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToGeneratorsMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToDanglingLinesMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToHvdcLinesMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToPhaseTapChangersMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToBreakersMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToTransformersMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToRatioTapChangersMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToLccConverterStationsMapping,
                                Map<IndexedMappingKey, List<MappedEquipment>> timeSeriesToVscConverterStationsMapping,
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
                TimeSeriesTable table = config.loadToTable(new TreeSet<>(ImmutableSet.of(version)), store, parameters.getPointRange(), usedTimeSeriesNames);

                if (context == null) {
                    context = new MapperContext();
                    context.timeSeriesToLoadsMapping = config.getTimeSeriesToLoadsMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToGeneratorsMapping = config.getTimeSeriesToGeneratorsMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToDanglingLinesMapping = config.getTimeSeriesToDanglingLinesMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToHvdcLinesMapping = config.getTimeSeriesToHvdcLinesMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToPhaseTapChangersMapping = config.getTimeSeriesToPhaseTapChangersMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToBreakersMapping = config.getTimeSeriesToBreakersMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToTransformersMapping = config.getTimeSeriesToTransformersMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToRatioTapChangersMapping = config.getTimeSeriesToRatioTapChangersMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToLccConverterStationsMapping = config.getTimeSeriesToLccConverterStationsMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
                    context.timeSeriesToVscConverterStationsMapping = config.getTimeSeriesToVscConverterStationsMapping().entrySet().stream()
                            .collect(Collectors.toMap(e -> indexMappingKey(table, e.getKey()), e -> mapEquipments(e.getKey(), e.getValue()), (x, y) -> y, LinkedHashMap::new));
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
