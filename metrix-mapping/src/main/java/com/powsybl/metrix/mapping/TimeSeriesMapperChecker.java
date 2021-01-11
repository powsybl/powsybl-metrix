/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.metrix.mapping.TimeSeriesMappingLogger.*;
import com.powsybl.metrix.mapping.common.iidm.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesTable;

import java.util.*;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.addActivePowerRangeExtension;

public class TimeSeriesMapperChecker extends MultipleTimeSeriesMapperObserver implements TimeSeriesConstants {

    enum ScalingDownPowerChange {
        BASE_CASE_MINP(0),
        BASE_CASE_MAXP(1),
        ZERO(2),
        MAPPED_MINP(3),
        MAPPED_MAXP(4),
        MAPPED_MINP_DISABLED(5),
        MAPPED_MAXP_DISABLED(6),
        ZERO_DISABLED(7),
        BASE_CASE_CS1TOCS2(8),
        BASE_CASE_CS2TOCS1(9),
        BASE_CASE_MINUS_MAXP(10);

        private final int change;

        ScalingDownPowerChange(int change) {
            this.change = change;
        }
    }

    enum ScalingDownLimitViolation {
        BASE_CASE_MINP_BY_TARGETP(0),
        MAPPED_MINP_BY_TARGETP(1),
        MAXP_BY_TARGETP(2),
        MAXP_BY_ACTIVEPOWER(3),
        CS1TOCS2_BY_ACTIVEPOWER(4),
        MINP_BY_TARGETP(5),
        MINP_BY_ACTIVEPOWER(6),
        CS2TOCS1_BY_ACTIVEPOWER(7);

        private final int violation;

        ScalingDownLimitViolation(int violation) {
            this.violation = violation;
        }
    }

    static final class MappedEquipments {

        private final double timeSeriesValue;

        private final Set<Identifiable> identifiables;

        private final Set<ScalingDownPowerChange> scalingDownPowerChange = new HashSet<>();

        private final Set<ScalingDownLimitViolation> scalingDownLimitViolation = new HashSet<>();

        public MappedEquipments(double timeSeriesValue, Set<Identifiable> identifiables) {
            this.timeSeriesValue = timeSeriesValue;
            this.identifiables = Objects.requireNonNull(identifiables);
        }

        public double getTimeSeriesValue() {
            return timeSeriesValue;
        }

        public Set<Identifiable> getIdentifiables() {
            return identifiables;
        }

        public Set<ScalingDownPowerChange> getScalingDownPowerChange() {
            return scalingDownPowerChange;
        }

        public Set<ScalingDownLimitViolation> getScalingDownLimitViolation() {
            return scalingDownLimitViolation;
        }
    }

    static final class MappedPower {

        private String timeSeriesNameP = null;

        private Double minP = null;

        private Double valueP = null;

        private Double maxP = null;

        private boolean ignoreLimits;

        public MappedPower() {
        }

        public MappedPower(MappedPower mappedPower) {
            this.timeSeriesNameP = mappedPower.timeSeriesNameP;
            this.minP = mappedPower.minP;
            this.valueP = mappedPower.valueP;
            this.maxP = mappedPower.maxP;
            this.ignoreLimits = mappedPower.ignoreLimits;
        }

        public String getTimeSeriesNameP() {
            return timeSeriesNameP;
        }

        public void setTimeSeriesNameP(String timeSeriesNameP) {
            this.timeSeriesNameP = timeSeriesNameP;
        }

        public Double getMinP() {
            return minP;
        }

        public void setMinP(Double minP) {
            this.minP = minP;
        }

        public Double getP() {
            return valueP;
        }

        public void setP(Double valueP) {
            this.valueP = valueP;
        }

        public Double getMaxP() {
            return maxP;
        }

        public void setMaxP(Double maxP) {
            this.maxP = maxP;
        }

        public boolean isIgnoreLimits() {
            return ignoreLimits;
        }

        public void setIgnoreLimits(boolean ignoreLimits) {
            this.ignoreLimits = ignoreLimits;
        }
    }

    static final class LimitChange {

        private final double baseCaseLimit;

        private double limit;

        private int baseCaseLimitNbOfViolation = 0;

        public LimitChange(double baseCaseLimit, double limit) {
            this.baseCaseLimit = baseCaseLimit;
            this.limit = limit;
        }

        public double getBaseCaseLimit() {
            return baseCaseLimit;
        }

        public double getLimit() {
            return limit;
        }

        public void setLimit(double limit) {
            this.limit = limit;
        }

        public int getBaseCaseLimitNbOfViolation() {
            return baseCaseLimitNbOfViolation;
        }

        public void setBaseCaseLimitNbOfViolation(int baseCaseLimitNbOfViolation) {
            this.baseCaseLimitNbOfViolation = baseCaseLimitNbOfViolation;
        }
    }

    private int version;

    private TimeSeriesIndex index;

    private final TimeSeriesMappingLogger logger;

    private final float toleranceThreshold;

    private final Map<String, Boolean> hvdcLineToActivePowerRange = new HashMap<>();

    private final Map<Identifiable, MappedPower> identifiableToConstantMappedPowers = new LinkedHashMap<>();

    private final Map<Identifiable, MappedPower> identifiableToMappedPower = new LinkedHashMap<>();

    private final Map<String, MappedEquipments> targetPTimeSeriesToEquipments = new HashMap<>();

    private final Map<String, MappedEquipments> setpointTimeSeriesToEquipments = new HashMap<>();

    private final Map<String, Set<ScalingDownPowerChange>> targetPTimeSeriesToScalingDownPowerChangeSynthesis = new HashMap<>();

    private final Map<String, Set<ScalingDownPowerChange>> setpointTimeSeriesToScalingDownPowerChangeSynthesis = new HashMap<>();

    private final Map<String, Set<ScalingDownLimitViolation>> targetPTimeSeriesToScalingDownLimitViolationSynthesis = new HashMap<>();

    private final Map<String, Set<ScalingDownLimitViolation>> setpointTimeSeriesToScalingDownLimitViolationSynthesis = new HashMap<>();

    private final Map<Identifiable, LimitChange> generatorToMaxValues = new HashMap<>();

    private final Map<Identifiable, LimitChange> generatorToMinValues = new HashMap<>();

    private final Map<Identifiable, LimitChange> hvdcLineToMaxValues = new HashMap<>();

    private final Map<Identifiable, LimitChange> hvdcLineToMinValues = new HashMap<>();

    private final Map<Identifiable, LimitChange> hvdcLineToCS1toCS2Values = new HashMap<>();

    private final Map<Identifiable, LimitChange> hvdcLineToCS2toCS1Values = new HashMap<>();

    @Override
    public void versionStart(int version) {
        this.version = version;
        super.versionStart(version);
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        // With ignore limits option, restore previous extended limits (potentially overwritten by NetworkPointWriter)
        generatorToMaxValues.entrySet().stream().filter(e -> !Double.isNaN(e.getValue().getLimit())).forEach(e -> ((Generator) (e.getKey())).setMaxP(e.getValue().getLimit()));
        generatorToMinValues.entrySet().stream().filter(e -> !Double.isNaN(e.getValue().getLimit())).forEach(e -> ((Generator) (e.getKey())).setMinP(e.getValue().getLimit()));
        hvdcLineToMaxValues.entrySet().stream().filter(e -> !Double.isNaN(e.getValue().getLimit())).forEach(e -> ((HvdcLine) (e.getKey())).setMaxP(e.getValue().getLimit()));
        hvdcLineToMinValues.entrySet().stream().filter(e -> !Double.isNaN(e.getValue().getLimit())).forEach(e -> ((HvdcLine) (e.getKey())).setMaxP(Math.abs(e.getValue().getLimit())));
        hvdcLineToCS1toCS2Values.entrySet().stream().filter(e -> !Double.isNaN(e.getValue().getLimit())).forEach(e -> ((HvdcLine) (e.getKey())).getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS1toCS2((float) e.getValue().getLimit()));
        hvdcLineToCS2toCS1Values.entrySet().stream().filter(e -> !Double.isNaN(e.getValue().getLimit())).forEach(e -> ((HvdcLine) (e.getKey())).getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS2toCS1((float) Math.abs(e.getValue().getLimit())));

        super.timeSeriesMappingStart(point, index);
        this.index = index;
        if (point != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            identifiableToConstantMappedPowers.entrySet().forEach(e -> identifiableToMappedPower.put(e.getKey(), new MappedPower(e.getValue())));
        }
    }

    @Override
    public void map(int version, int point, TimeSeriesTable table) {
        super.map(version, point, table);
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        // Correct each mapped power value if necessary and notify observers
        identifiableToMappedPower.entrySet().forEach(e -> correctAndNotifyMappedPowers(index, version, point, e.getKey(), e.getValue()));

        // Add scaling down logs to logger
        targetPTimeSeriesToEquipments.entrySet().forEach(e -> addScalingDownLogs(index, version, point, e.getKey(), e.getValue(),
                EquipmentVariable.targetP,
                targetPTimeSeriesToScalingDownPowerChangeSynthesis,
                targetPTimeSeriesToScalingDownLimitViolationSynthesis));
        setpointTimeSeriesToEquipments.entrySet().forEach(e -> addScalingDownLogs(index, version, point, e.getKey(), e.getValue(),
                EquipmentVariable.activePowerSetpoint,
                setpointTimeSeriesToScalingDownPowerChangeSynthesis,
                setpointTimeSeriesToScalingDownLimitViolationSynthesis));

        identifiableToMappedPower.clear();
        targetPTimeSeriesToEquipments.clear();
        setpointTimeSeriesToEquipments.clear();

        super.timeSeriesMappingEnd(point, index, balance);
    }

    @Override
    public void versionEnd(int version) {
        // Add limit change warnings to logger
        addLimitChangeLog(generatorToMinValues, MappingLimitType.MIN, version, EquipmentVariable.minP.getVariableName(), EquipmentVariable.targetP.getVariableName());
        addLimitChangeLog(generatorToMaxValues, MappingLimitType.MAX, version, EquipmentVariable.maxP.getVariableName(), EquipmentVariable.targetP.getVariableName());
        addLimitChangeLog(hvdcLineToMinValues, MappingLimitType.MIN, version, MINUS_MAXP, EquipmentVariable.activePowerSetpoint.getVariableName());
        addLimitChangeLog(hvdcLineToMaxValues, MappingLimitType.MAX, version, EquipmentVariable.maxP.getVariableName(), EquipmentVariable.activePowerSetpoint.getVariableName());
        addLimitChangeLog(hvdcLineToCS2toCS1Values, MappingLimitType.MIN, version, MINUS_CS21, EquipmentVariable.activePowerSetpoint.getVariableName());
        addLimitChangeLog(hvdcLineToCS1toCS2Values, MappingLimitType.MAX, version, CS12, EquipmentVariable.activePowerSetpoint.getVariableName());

        // Add scaling down logs synthesis to logger
        targetPTimeSeriesToScalingDownPowerChangeSynthesis.entrySet().forEach(ts -> ts.getValue().forEach(change -> addScalingDownLogSynthesis(EquipmentVariable.targetP.getVariableName(), change, version, ts.getKey())));
        targetPTimeSeriesToScalingDownLimitViolationSynthesis.entrySet().forEach(ts -> ts.getValue().forEach(change -> addScalingDownLimitViolationLogSynthesis(change, version, ts.getKey())));
        setpointTimeSeriesToScalingDownPowerChangeSynthesis.entrySet().forEach(ts -> ts.getValue().forEach(change -> addScalingDownLogSynthesis(EquipmentVariable.activePowerSetpoint.getVariableName(), change, version, ts.getKey())));
        setpointTimeSeriesToScalingDownLimitViolationSynthesis.entrySet().forEach(ts -> ts.getValue().forEach(change -> addScalingDownLimitViolationLogSynthesis(change, version, ts.getKey())));

        identifiableToConstantMappedPowers.clear();
        generatorToMinValues.clear();
        generatorToMaxValues.clear();
        hvdcLineToMinValues.clear();
        hvdcLineToMaxValues.clear();
        hvdcLineToCS1toCS2Values.clear();
        hvdcLineToCS2toCS1Values.clear();
        targetPTimeSeriesToScalingDownPowerChangeSynthesis.clear();
        targetPTimeSeriesToScalingDownLimitViolationSynthesis.clear();
        setpointTimeSeriesToScalingDownPowerChangeSynthesis.clear();
        setpointTimeSeriesToScalingDownLimitViolationSynthesis.clear();

        super.versionEnd(version);
    }

    public TimeSeriesMapperChecker(List<TimeSeriesMapperObserver> observers, TimeSeriesMappingLogger logger, TimeSeriesMapperParameters parameters) {
        super(observers);
        this.logger = Objects.requireNonNull(logger);
        this.toleranceThreshold = parameters.getToleranceThreshold();
    }

    public void timeSeriesMappedToEquipments(int point, String timeSeriesName, double timeSeriesValue, List<Identifiable> identifiables, MappingVariable variable, double[] equipmentValues, boolean ignoreLimits) {
        if (variable == EquipmentVariable.targetP) {
            targetPTimeSeriesToEquipments.put(timeSeriesName, new MappedEquipments(timeSeriesValue, new HashSet<>(identifiables)));
        } else if (variable == EquipmentVariable.activePowerSetpoint) {
            setpointTimeSeriesToEquipments.put(timeSeriesName, new MappedEquipments(timeSeriesValue, new HashSet<>(identifiables)));
        }
        for (int i = 0; i < identifiables.size(); i++) {
            Identifiable identifiable = identifiables.get(i);
            double equipmentValue = equipmentValues[i];
            if (TimeSeriesMapper.isPowerOrLimitVariable(variable)) {
                // Store mapped power values and limits in order to correct power values not included in limits
                addTimeSeriesMappedToEquipments(point, timeSeriesName, identifiable, variable, equipmentValue, ignoreLimits);
            }
            if (identifiable instanceof HvdcLine) {
                hvdcLineToActivePowerRange.computeIfAbsent(identifiable.getId(), e -> ((HvdcLine) identifiable).getExtension(HvdcOperatorActivePowerRange.class) != null);
            }
            if (!TimeSeriesMapper.isPowerVariable(variable)) {
                // For power values, observers will be notified later after correction
                // For other values, observers are notified immediately
                super.timeSeriesMappedToEquipment(point, timeSeriesName, identifiable, variable, equipmentValue);
            }
        }
    }

    private void addTimeSeriesMappedToEquipments(int point, String timeSeriesName, Identifiable identifiable, MappingVariable variable, double equipmentValue, boolean ignoreLimits) {
        MappedPower mappedPower;
        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            identifiableToConstantMappedPowers.computeIfAbsent(identifiable, e -> new MappedPower());
            mappedPower = identifiableToConstantMappedPowers.get(identifiable);
        } else {
            identifiableToMappedPower.computeIfAbsent(identifiable, e -> new MappedPower());
            mappedPower = identifiableToMappedPower.get(identifiable);
        }

        mappedPower.setIgnoreLimits(ignoreLimits);
        if (variable == EquipmentVariable.minP) {
            mappedPower.setMinP(equipmentValue);
        } else if (TimeSeriesMapper.isPowerVariable(variable)) {
            mappedPower.setTimeSeriesNameP(timeSeriesName);
            mappedPower.setP(equipmentValue);
        } else if (variable == EquipmentVariable.maxP) {
            mappedPower.setMaxP(equipmentValue);
        }
    }

    private void correctAndNotifyMappedPowers(TimeSeriesIndex index, int version, int point, Identifiable identifiable, MappedPower mappedPower) {
        double value;
        if (identifiable instanceof Generator) {
            value = correctMappedPowerGenerator(index, version, point, (Generator) identifiable, mappedPower);
        } else if (identifiable instanceof HvdcLine) {
            value = correctMappedPowerHvdcLine(index, version, point, (HvdcLine) identifiable, mappedPower);
        } else {
            throw new AssertionError("Unsupported equipment type for id " + identifiable.getId());
        }

        String timeSeriesName = mappedPower.getTimeSeriesNameP() != null ? mappedPower.getTimeSeriesNameP() : "";
        mappedPower.setP(value);
        super.timeSeriesMappedToEquipment(point, timeSeriesName, identifiable, TimeSeriesMapper.getPowerVariable(identifiable), value);
    }

    private double correctMappedPowerGenerator(TimeSeriesIndex index, int version, int point, Generator generator, MappedPower mappedPower) {

        final String timeSeriesName = mappedPower.getTimeSeriesNameP();

        final String targetPVariableName = EquipmentVariable.targetP.getVariableName();
        final String maxPVariableName = EquipmentVariable.maxP.getVariableName();
        final String minPVariableName = EquipmentVariable.minP.getVariableName();

        final String id = generator.getId();

        final boolean isMappedTargetP = mappedPower.getP() != null;
        final boolean isMappedMinP = mappedPower.getMinP() != null;
        final boolean isMappedMaxP = mappedPower.getMaxP() != null;

        double targetP = isMappedTargetP ? mappedPower.getP() : TimeSeriesMapper.getP(generator);
        final double minP = isMappedMinP ? mappedPower.getMinP() : TimeSeriesMapper.getMin(generator);
        final double maxP = isMappedMaxP ? mappedPower.getMaxP() : TimeSeriesMapper.getMax(generator);

        final boolean isOkMinP = targetP >= minP - toleranceThreshold;
        final boolean isOkMaxP = targetP <= maxP + toleranceThreshold;

        if (isMappedTargetP) {
            if (isOkMaxP && targetP >= maxP - toleranceThreshold) {
                targetP = maxP - toleranceThreshold;
            } else if (isOkMinP && targetP <= minP + toleranceThreshold) {
                targetP = minP + toleranceThreshold;
            }
        }

        boolean ignoreLimits = mappedPower.isIgnoreLimits();

        if (minP > maxP) {
            throw new AssertionError("Equipment '" + generator.getId() + "' : invalid active limits [" + minP + ", " + maxP + "] at point " + point);
        }

        if (ignoreLimits && isMappedTargetP) {
            if (!isMappedMaxP) {
                addLimitValueChange(MappingLimitType.MAX, generatorToMaxValues, generator, TimeSeriesMapper.getMax(generator), targetP);
            }
            if (!isMappedMinP && minP <= 0) {
                addLimitValueChange(MappingLimitType.MIN, generatorToMinValues, generator, TimeSeriesMapper.getMin(generator), targetP);
            }
        }

        if (!isOkMaxP) {
            // targetP is greater than maxP ..
            if (!ignoreLimits) {
                // ... without ignoreLimits option -> reduce targetP to maxP
                if (isMappedTargetP) {
                    ScalingDownPowerChange change = isMappedMaxP ? ScalingDownPowerChange.MAPPED_MAXP : ScalingDownPowerChange.BASE_CASE_MAXP;
                    targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(change);
                } else {
                    logger.addLog(new MappingRangeWarningWithVariableChange(version, index, point, targetPVariableName, targetPVariableName, maxPVariableName, id, minP, maxP, targetP, maxP));
                }
                return maxP;
            } else {
                // ... with ignoreLimits option -> try to increase maxP
                if (isMappedTargetP && !isMappedMaxP) {
                    // targetP is mapped, maxP is not mapped -> increase base case maxP to targetP
                    TimeSeriesMapper.setMax(generator, targetP);
                    targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownLimitViolation().add(ScalingDownLimitViolation.MAXP_BY_TARGETP);
                    return targetP;
                } else if (!isMappedTargetP) {
                    // targetP is not mapped, maxP is mapped -> reduce targetP to maxP
                    logger.addLog(new MappingRangeWarningWithVariableChangeDisabled(version, index, point, targetPVariableName, targetPVariableName, maxPVariableName, id, minP, maxP, targetP, maxP));
                    return maxP;
                } else {
                    targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MAXP_DISABLED);
                    return maxP;
                }
            }
        } else if (!isOkMinP) {
            if (minP <= 0) {
                // targetP is smaller than minP ...
                if (!ignoreLimits) {
                    // ... without ignoreLimits option -> increase targetP to minP
                    if (isMappedTargetP) {
                        ScalingDownPowerChange change = isMappedMinP ? ScalingDownPowerChange.MAPPED_MINP : ScalingDownPowerChange.BASE_CASE_MINP;
                        targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(change);
                    } else {
                        logger.addLog(new MappingRangeWarningWithVariableChange(version, index, point, targetPVariableName, targetPVariableName, minPVariableName, id, minP, maxP, targetP, minP));
                    }
                    return minP;
                } else {
                    // ... with ignoreLimits option -> try to reduce minP
                    if (isMappedTargetP && !isMappedMinP) {
                        // targetP is mapped, minP is not mapped -> reduce base case minP to targetP
                        TimeSeriesMapper.setMin(generator, targetP);
                        targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownLimitViolation().add(ScalingDownLimitViolation.MINP_BY_TARGETP);
                        return targetP;
                    } else if (!isMappedTargetP) {
                        // targetP is not mapped, minP is mapped -> increase targetP to minP
                        logger.addLog(new MappingRangeWarningWithVariableChangeDisabled(version, index, point, targetPVariableName, targetPVariableName, minPVariableName, id, minP, maxP, targetP, minP));
                        return minP;
                    } else {
                        targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MINP_DISABLED);
                        return minP;
                    }
                }
            } else if (targetP < 0) {
                // targetP is smaller than minP -> increase targetP to 0
                if (!isMappedTargetP) {
                    if (!ignoreLimits) {
                        logger.addLog(new MappingRangeWarningWithVariableChange(version, index, point, targetPVariableName, targetPVariableName, "", id, minP, maxP, targetP, 0));
                    } else {
                        logger.addLog(new MappingRangeWarningWithVariableChangeDisabled(version, index, point, targetPVariableName, targetPVariableName, "", id, minP, maxP, targetP, 0));
                    }
                } else {
                    ScalingDownPowerChange change = ignoreLimits ? ScalingDownPowerChange.ZERO_DISABLED : ScalingDownPowerChange.ZERO;
                    targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(change);
                }
                return 0;
            } else {
                // targetP is smaller than minP -> warning
                if (isMappedTargetP) {
                    ScalingDownLimitViolation change = isMappedMinP ? ScalingDownLimitViolation.MAPPED_MINP_BY_TARGETP : ScalingDownLimitViolation.BASE_CASE_MINP_BY_TARGETP;
                    targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownLimitViolation().add(change);
                    // For the moment, this verbose messages are not added to the logger
                    // It may be interesting to add it later, when it will be possible to filter the logger and/or configure the level of detail in the logger
                    //if (change == ScalingDownLimitViolation.BASE_CASE_MINP_BY_TARGETP) {
                    //logger.addLog(new ScalingDownBaseCaseMinPViolatedByMappedTargetP(version, index, point, targetPVariableName, targetPVariableName, id, minP, maxP, targetP));
                    //} else {
                    //logger.addLog(new ScalingDownMappedMinPViolatedByMappedTargetP(version, index, point, targetPVariableName, targetPVariableName, id, minP, maxP, targetP));
                    //}
                } else {
                    logger.addLog(new MappingRangeInfoWithMappedMinPViolatedByTargetP(version, index, point, targetPVariableName, id, minP, maxP, targetP));
                }
                return targetP;
            }
        } else if (isMappedTargetP) {
            return targetP;
        }
        return Double.NaN;
    }

    private double correctMappedPowerHvdcLine(TimeSeriesIndex index, int version, int point, HvdcLine hvdcLine, MappedPower mappedPower) {

        final String timeSeriesName = mappedPower.getTimeSeriesNameP();

        final boolean isMappedSetpoint = mappedPower.getP() != null;
        final boolean isMappedMinP = mappedPower.getMinP() != null;
        final boolean isMappedMaxP = mappedPower.getMaxP() != null;

        final String id = hvdcLine.getId();

        final boolean isActivePowerRange = hvdcLineToActivePowerRange.get(id);

        final String setpointVariableName = EquipmentVariable.activePowerSetpoint.getVariableName();

        double setpoint = isMappedSetpoint ? mappedPower.getP() : TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine);
        final double minP = isMappedMinP ? mappedPower.getMinP() : TimeSeriesMapper.getMin(hvdcLine);
        final double maxP = isMappedMaxP ? mappedPower.getMaxP() : TimeSeriesMapper.getMax(hvdcLine);

        final boolean isOkMinP = setpoint >= minP - toleranceThreshold;
        final boolean isOkMaxP = setpoint <= maxP + toleranceThreshold;

        if (isMappedSetpoint) {
            if (isOkMaxP && setpoint >= maxP - toleranceThreshold) {
                setpoint = maxP - toleranceThreshold;
            } else if (isOkMinP && setpoint <= minP + toleranceThreshold) {
                setpoint = minP + toleranceThreshold;
            }
        }

        boolean ignoreLimits = mappedPower.isIgnoreLimits();

        if (hvdcLine.getMaxP() < 0) {
            throw new AssertionError("Equipment '" + hvdcLine.getId() + "' : invalid active limit maxP " + hvdcLine.getMaxP() + " at point " + point);
        } else if (isActivePowerRange && (minP > 0 || maxP < 0)) {
            throw new AssertionError("Equipment '" + hvdcLine.getId() + "' : invalid active limits [" + minP + ", " + maxP + "] at point " + point);
        }

        if (ignoreLimits && isMappedSetpoint) {
            if (!isMappedMaxP) {
                addLimitValueChange(MappingLimitType.MAX, isActivePowerRange ? hvdcLineToCS1toCS2Values : hvdcLineToMaxValues, hvdcLine, TimeSeriesMapper.getMax(hvdcLine), setpoint);
            }
            if (!isMappedMinP) {
                addLimitValueChange(MappingLimitType.MIN, isActivePowerRange ? hvdcLineToCS2toCS1Values : hvdcLineToMinValues, hvdcLine, TimeSeriesMapper.getMin(hvdcLine), setpoint);
            }
        }

        if (!isOkMaxP) {
            // setpoint is greater than maxP ..
            if (!ignoreLimits) {
                // ... without ignoreLimits option -> reduce setpoint to maxP
                if (isMappedSetpoint) {
                    ScalingDownPowerChange change = isMappedMaxP ? ScalingDownPowerChange.MAPPED_MAXP : (isActivePowerRange ? ScalingDownPowerChange.BASE_CASE_CS1TOCS2 : ScalingDownPowerChange.BASE_CASE_MAXP);
                    setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(change);
                } else {
                    logger.addLog(new MappingRangeWarningWithVariableChange(version, index, point, setpointVariableName, setpointVariableName, EquipmentVariable.maxP.getVariableName(), id, minP, maxP, setpoint, maxP));
                }
                return maxP;
            } else {
                // ... with ignoreLimits option -> try to increase maxP
                if (isMappedSetpoint && !isMappedMaxP) {
                    // setpoint is mapped, maxP is not mapped -> increase base case maxP to setpoint
                    addActivePowerRangeExtension(hvdcLine);
                    if (!isActivePowerRange) {
                        hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS1toCS2((float) Math.abs(hvdcLineToMinValues.get(hvdcLine).getBaseCaseLimit()));
                    }
                    TimeSeriesMapper.setMax(hvdcLine, Math.abs(setpoint));
                    setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownLimitViolation().add(isActivePowerRange ? ScalingDownLimitViolation.CS1TOCS2_BY_ACTIVEPOWER : ScalingDownLimitViolation.MAXP_BY_ACTIVEPOWER);
                    return setpoint;
                } else if (!isMappedSetpoint) {
                    // setpoint is not mapped, maxP is mapped -> reduce setpoint to maxP
                    logger.addLog(new MappingRangeWarningWithVariableChangeDisabled(version, index, point, setpointVariableName, setpointVariableName, EquipmentVariable.maxP.getVariableName(), id, minP, maxP, setpoint, maxP));
                    return maxP;
                } else {
                    setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MAXP_DISABLED);
                    return maxP;
                }
            }
        } else if (!isOkMinP) {
            // setpoint is smaller than minP ...
            if (!ignoreLimits) {
                // ... without ignoreLimits option -> increase setpoint to minP
                if (isMappedSetpoint) {
                    ScalingDownPowerChange change = isMappedMinP ? ScalingDownPowerChange.MAPPED_MINP : (isActivePowerRange ? ScalingDownPowerChange.BASE_CASE_CS2TOCS1 : ScalingDownPowerChange.BASE_CASE_MINUS_MAXP);
                    setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(change);
                } else {
                    logger.addLog(new MappingRangeWarningWithVariableChange(version, index, point, setpointVariableName, setpointVariableName, EquipmentVariable.minP.getVariableName(), id, minP, maxP, setpoint, minP));
                }
                return minP;
            } else {
                // ... with ignoreLimits option -> try to reduce minP
                if (isMappedSetpoint && !isMappedMinP) {
                    // setpoint is mapped, minP is not mapped -> reduce base case minP to setpoint
                    addActivePowerRangeExtension(hvdcLine);
                    if (!isActivePowerRange) {
                        hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS2toCS1((float) Math.abs(hvdcLineToMaxValues.get(hvdcLine).getBaseCaseLimit()));
                    }
                    TimeSeriesMapper.setMin(hvdcLine, Math.abs(setpoint));
                    setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownLimitViolation().add(isActivePowerRange ? ScalingDownLimitViolation.CS2TOCS1_BY_ACTIVEPOWER : ScalingDownLimitViolation.MINP_BY_ACTIVEPOWER);
                    return setpoint;
                } else if (!isMappedSetpoint) {
                    // setpoint is not mapped, minP is mapped -> increase setpoint to minP
                    logger.addLog(new MappingRangeWarningWithVariableChangeDisabled(version, index, point, setpointVariableName, setpointVariableName, EquipmentVariable.minP.getVariableName(), id, minP, maxP, setpoint, minP));
                    return minP;
                } else {
                    setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MINP_DISABLED);
                    return minP;
                }
            }
        } else if (isMappedSetpoint) {
            return setpoint;
        }
        return Double.NaN;
    }

    private void addLimitValueChange(MappingLimitType limitType, Map<Identifiable, LimitChange> equipmentToLimitValues, Identifiable identifiable, double oldLimit, double newLimit) {
        if ((limitType == MappingLimitType.MAX) || (limitType == MappingLimitType.MIN)) {
            equipmentToLimitValues.computeIfAbsent(identifiable, e -> new LimitChange(oldLimit, Double.NaN));
            if ((limitType == MappingLimitType.MAX && newLimit > oldLimit + toleranceThreshold) || (limitType == MappingLimitType.MIN && newLimit < oldLimit - toleranceThreshold)) {
                LimitChange limitChange = equipmentToLimitValues.get(identifiable);
                limitChange.setLimit(newLimit);
            }
        }
        equipmentToLimitValues.computeIfPresent(identifiable, (k, v) -> {
            LimitChange limitChange = equipmentToLimitValues.get(identifiable);
            if (!Double.isNaN(limitChange.getLimit()) && (limitType == MappingLimitType.MAX && newLimit > limitChange.getBaseCaseLimit()) || (limitType == MappingLimitType.MIN && newLimit < limitChange.getBaseCaseLimit())) {
                limitChange.setBaseCaseLimitNbOfViolation(limitChange.getBaseCaseLimitNbOfViolation() + 1);
            }
            return v;
        });
    }

    private void addScalingDownLogs(TimeSeriesIndex index, int version, int point, String timeSeriesName, MappedEquipments mappedEquipments,
                                    MappingVariable variable,
                                    Map<String, Set<ScalingDownPowerChange>> timeSeriesToPowerChange,
                                    Map<String, Set<ScalingDownLimitViolation>> timeSeriesToLimitViolation) {

        if (mappedEquipments.getScalingDownPowerChange().size() > 0) {
            double value = mappedEquipments.getTimeSeriesValue();
            Set<Identifiable> identifiables = mappedEquipments.getIdentifiables();
            double sum = identifiables.stream()
                    .mapToDouble(e -> identifiableToMappedPower.get(e).getP() != null ? identifiableToMappedPower.get(e).getP() : TimeSeriesMapper.getP(e))
                    .sum();
            mappedEquipments.getScalingDownPowerChange().forEach(e -> addScalingDownLog(variable.getVariableName(), e, index, version, timeSeriesName, value, sum, point));

            timeSeriesToPowerChange.computeIfAbsent(timeSeriesName, e -> new HashSet<>());
            Set<ScalingDownPowerChange> powerChange = timeSeriesToPowerChange.get(timeSeriesName);
            powerChange.addAll(mappedEquipments.getScalingDownPowerChange());
        }

        if (mappedEquipments.getScalingDownLimitViolation().size() > 0) {
            timeSeriesToLimitViolation.computeIfAbsent(timeSeriesName, e -> new HashSet<>());
            Set<ScalingDownLimitViolation> scalingDownLimitViolation = timeSeriesToLimitViolation.get(timeSeriesName);
            scalingDownLimitViolation.addAll(mappedEquipments.getScalingDownLimitViolation());
        }
    }

    private void addScalingDownLog(String changedVariable, ScalingDownPowerChange change, TimeSeriesIndex index, int version, String timeSeriesName, double value, double sum, int point) {
        switch (change) {
            case BASE_CASE_MINP:
                logger.addLog(new ScalingDownWarningChangeToBaseCaseVariable(changedVariable, EquipmentVariable.minP.getVariableName(), timeSeriesName, value, sum, index, version, point));
                break;
            case BASE_CASE_MAXP:
                logger.addLog(new ScalingDownWarningChangeToBaseCaseVariable(changedVariable, EquipmentVariable.maxP.getVariableName(), timeSeriesName, value, sum, index, version, point));
                break;
            case MAPPED_MINP:
                logger.addLog(new ScalingDownWarningChangeToMappedVariable(changedVariable, EquipmentVariable.minP.getVariableName(), timeSeriesName, value, sum, index, version, point));
                break;
            case MAPPED_MAXP:
                logger.addLog(new ScalingDownWarningChangeToMappedVariable(changedVariable, EquipmentVariable.maxP.getVariableName(), timeSeriesName, value, sum, index, version, point));
                break;
            case ZERO:
                logger.addLog(new ScalingDownWarningChangeToZero(changedVariable, timeSeriesName, value, sum, index, version, point));
                break;
            case MAPPED_MINP_DISABLED:
                logger.addLog(new ScalingDownWarningChangeToMappedVariableDisabled(changedVariable, EquipmentVariable.minP.getVariableName(), timeSeriesName, value, sum, index, version, point));
                break;
            case MAPPED_MAXP_DISABLED:
                logger.addLog(new ScalingDownWarningChangeToMappedVariableDisabled(changedVariable, EquipmentVariable.maxP.getVariableName(), timeSeriesName, value, sum, index, version, point));
                break;
            case ZERO_DISABLED:
                logger.addLog(new ScalingDownWarningChangeToZeroDisabled(changedVariable, timeSeriesName, value, sum, index, version, point));
                break;
            case BASE_CASE_CS1TOCS2:
                logger.addLog(new TimeSeriesMappingLogger.ScalingDownWarningChangeToBaseCaseVariable(changedVariable, CS12, timeSeriesName, value, sum, index, version, point));
                break;
            case BASE_CASE_CS2TOCS1:
                logger.addLog(new TimeSeriesMappingLogger.ScalingDownWarningChangeToBaseCaseVariable(changedVariable, MINUS_CS21, timeSeriesName, value, sum, index, version, point));
                break;
            case BASE_CASE_MINUS_MAXP:
                logger.addLog(new ScalingDownWarningChangeToBaseCaseVariable(changedVariable, MINUS_MAXP, timeSeriesName, value, sum, index, version, point));
                break;
            default:
                throw new AssertionError();
        }
    }

    private void addScalingDownLogSynthesis(String changedVariable, ScalingDownPowerChange change, int version, String timeSeriesName) {
        switch (change) {
            case BASE_CASE_MINP:
                logger.addLog(new ScalingDownWarningChangeToBaseCaseVariableSynthesis(changedVariable, EquipmentVariable.minP.getVariableName(), timeSeriesName, index, version));
                break;
            case BASE_CASE_MAXP:
                logger.addLog(new ScalingDownWarningChangeToBaseCaseVariableSynthesis(changedVariable, EquipmentVariable.maxP.getVariableName(), timeSeriesName, index, version));
                break;
            case MAPPED_MINP:
                logger.addLog(new ScalingDownWarningChangeToMappedVariableSynthesis(changedVariable, EquipmentVariable.minP.getVariableName(), timeSeriesName, index, version));
                break;
            case MAPPED_MAXP:
                logger.addLog(new ScalingDownWarningChangeToMappedVariableSynthesis(changedVariable, EquipmentVariable.maxP.getVariableName(), timeSeriesName, index, version));
                break;
            case ZERO:
                logger.addLog(new ScalingDownWarningChangeToZeroSynthesis(changedVariable, timeSeriesName, index, version));
                break;
            case MAPPED_MINP_DISABLED:
                logger.addLog(new ScalingDownWarningChangeToMappedVariableDisabledSynthesis(changedVariable, EquipmentVariable.minP.getVariableName(), timeSeriesName, index, version));
                break;
            case MAPPED_MAXP_DISABLED:
                logger.addLog(new ScalingDownWarningChangeToMappedVariableDisabledSynthesis(changedVariable, EquipmentVariable.maxP.getVariableName(), timeSeriesName, index, version));
                break;
            case ZERO_DISABLED:
                logger.addLog(new ScalingDownWarningChangeToZeroDisabledSynthesis(changedVariable, timeSeriesName, index, version));
                break;
            case BASE_CASE_CS1TOCS2:
                logger.addLog(new ScalingDownWarningChangeToBaseCaseVariableSynthesis(changedVariable, CS12, timeSeriesName, index, version));
                break;
            case BASE_CASE_CS2TOCS1:
                logger.addLog(new ScalingDownWarningChangeToBaseCaseVariableSynthesis(changedVariable, MINUS_CS21, timeSeriesName, index, version));
                break;
            case BASE_CASE_MINUS_MAXP:
                logger.addLog(new ScalingDownWarningChangeToBaseCaseVariableSynthesis(changedVariable, MINUS_MAXP, timeSeriesName, index, version));
                break;
            default:
                throw new AssertionError();
        }
    }

    private void addScalingDownLimitViolationLogSynthesis(ScalingDownLimitViolation change, int version, String timeSeriesName) {
        switch (change) {
            case BASE_CASE_MINP_BY_TARGETP:
                logger.addLog(new ScalingDownBaseCaseMinPViolatedByMappedTargetPSynthesis(timeSeriesName, index, version));
                break;
            case MAPPED_MINP_BY_TARGETP:
                logger.addLog(new ScalingDownMappedMinPViolatedByMappedTargetPSynthesis(timeSeriesName, index, version));
                break;
            case MAXP_BY_TARGETP:
                logger.addLog(new ScalingDownMaxPChangeSynthesis(EquipmentVariable.maxP.getVariableName(), EquipmentVariable.targetP.getVariableName(), timeSeriesName, index, version));
                break;
            case MAXP_BY_ACTIVEPOWER:
                logger.addLog(new ScalingDownMaxPChangeSynthesis(EquipmentVariable.maxP.getVariableName(), EquipmentVariable.activePowerSetpoint.getVariableName(), timeSeriesName, index, version));
                break;
            case CS1TOCS2_BY_ACTIVEPOWER:
                logger.addLog(new ScalingDownMaxPChangeSynthesis(CS12, EquipmentVariable.activePowerSetpoint.getVariableName(), timeSeriesName, index, version));
                break;
            case MINP_BY_TARGETP:
                logger.addLog(new ScalingDownMinPChangeSynthesis(EquipmentVariable.minP.getVariableName(), EquipmentVariable.targetP.getVariableName(), timeSeriesName, index, version));
                break;
            case MINP_BY_ACTIVEPOWER:
                logger.addLog(new ScalingDownMinPChangeSynthesis(MINUS_MAXP, EquipmentVariable.activePowerSetpoint.getVariableName(), timeSeriesName, index, version));
                break;
            case CS2TOCS1_BY_ACTIVEPOWER:
                logger.addLog(new ScalingDownMinPChangeSynthesis(MINUS_CS21, EquipmentVariable.activePowerSetpoint.getVariableName(), timeSeriesName, index, version));
                break;
            default:
                throw new AssertionError();
        }
    }

    private void addLimitChangeLog(Map<Identifiable, LimitChange> equipmentToValues, MappingLimitType limitType, int version, String variableToChange, String variable) {
        if (limitType == MappingLimitType.MIN) {
            equipmentToValues.entrySet().stream()
                    .filter(e -> !Double.isNaN(e.getValue().getLimit()))
                    .forEach(e -> logger.addLog(new LimitMinInfo(
                            index, version, Integer.MAX_VALUE,
                            e.getKey().getId(),
                            variableToChange,
                            variable,
                            e.getValue().getBaseCaseLimitNbOfViolation(),
                            e.getValue().getBaseCaseLimit(),
                            e.getValue().getLimit())));
        } else {
            equipmentToValues.entrySet().stream()
                    .filter(e -> !Double.isNaN(e.getValue().getLimit()))
                    .forEach(e -> logger.addLog(new LimitMaxInfo(
                            index, version, Integer.MAX_VALUE,
                            e.getKey().getId(),
                            variableToChange,
                            variable,
                            e.getValue().getBaseCaseLimitNbOfViolation(),
                            e.getValue().getBaseCaseLimit(),
                            e.getValue().getLimit())));
        }
    }
}
