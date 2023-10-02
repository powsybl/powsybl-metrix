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
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.metrix.mapping.log.*;
import com.powsybl.metrix.mapping.timeseries.*;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.util.*;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.addActivePowerRangeExtension;

public class TimeSeriesMapperChecker extends MultipleTimeSeriesMapperObserver {

    private static final String UNHANDLED_SCALING_OPERATION_ERROR = "Unhandled scaling operation %s";

    private int version;

    private TimeSeriesIndex index;

    private final TimeSeriesMappingLogger timeSeriesMappingLogger;

    private final float toleranceThreshold;

    private final Map<String, Boolean> hvdcLineToActivePowerRange = new HashMap<>();
    private final Map<Identifiable<?>, MappedPower> identifiableToConstantMappedPowers = new LinkedHashMap<>();
    private final Map<Identifiable<?>, MappedPower> identifiableToMappedPower = new LinkedHashMap<>();
    private final Map<String, MappedEquipments> targetPTimeSeriesToEquipments = new HashMap<>();
    private final Map<String, MappedEquipments> setpointTimeSeriesToEquipments = new HashMap<>();
    private final Map<String, Set<ScalingDownPowerChange>> targetPTimeSeriesToScalingDownPowerChangeSynthesis = new HashMap<>();
    private final Map<String, Set<ScalingDownPowerChange>> setpointTimeSeriesToScalingDownPowerChangeSynthesis = new HashMap<>();
    private final Map<String, Set<ScalingDownLimitViolation>> targetPTimeSeriesToScalingDownLimitViolationSynthesis = new HashMap<>();
    private final Map<String, Set<ScalingDownLimitViolation>> setpointTimeSeriesToScalingDownLimitViolationSynthesis = new HashMap<>();
    private final Map<Identifiable<?>, LimitChange> generatorToMaxValues = new HashMap<>();
    private final Map<Identifiable<?>, LimitChange> generatorToMinValues = new HashMap<>();
    private final Map<Identifiable<?>, LimitChange> hvdcLineToMaxValues = new HashMap<>();
    private final Map<Identifiable<?>, LimitChange> hvdcLineToMinValues = new HashMap<>();
    private final Map<Identifiable<?>, LimitChange> hvdcLineToCS1toCS2Values = new HashMap<>();
    private final Map<Identifiable<?>, LimitChange> hvdcLineToCS2toCS1Values = new HashMap<>();

    private static final String MAX_P_VARIABLE_NAME = EquipmentVariable.maxP.getVariableName();
    private static final String MIN_P_VARIABLE_NAME = EquipmentVariable.minP.getVariableName();
    private static final String TARGET_P_VARIABLE_NAME = EquipmentVariable.targetP.getVariableName();
    private static final String SET_POINT_VARIABLE_NAME = EquipmentVariable.activePowerSetpoint.getVariableName();

    private String timeSeriesName;
    private String id;
    private boolean isMappedMinP;
    private boolean isMappedMaxP;
    private double minP;
    private double maxP;
    private boolean isOkMinP;
    private boolean isOkMaxP;

    boolean ignoreLimits;

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
            identifiableToConstantMappedPowers.forEach((key, value) -> identifiableToMappedPower.put(key, new MappedPower(value)));
        }
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        // Correct each mapped power value if necessary and notify observers
        identifiableToMappedPower.forEach((key, value) -> correctAndNotifyMappedPowers(point, key, value));

        // Add scaling down logs to logger
        targetPTimeSeriesToEquipments.forEach((key, value) -> addScalingDownLogs(index, point, key, value,
                EquipmentVariable.targetP,
                targetPTimeSeriesToScalingDownPowerChangeSynthesis,
                targetPTimeSeriesToScalingDownLimitViolationSynthesis));
        setpointTimeSeriesToEquipments.forEach((key, value) -> addScalingDownLogs(index, point, key, value,
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
        addLimitChangeLog(generatorToMinValues, MappingLimitType.MIN, version, MIN_P_VARIABLE_NAME, EquipmentVariable.targetP.getVariableName());
        addLimitChangeLog(generatorToMaxValues, MappingLimitType.MAX, version, MAX_P_VARIABLE_NAME, EquipmentVariable.targetP.getVariableName());
        addLimitChangeLog(hvdcLineToMinValues, MappingLimitType.MIN, version, TimeSeriesConstants.MINUS_MAXP, EquipmentVariable.activePowerSetpoint.getVariableName());
        addLimitChangeLog(hvdcLineToMaxValues, MappingLimitType.MAX, version, MAX_P_VARIABLE_NAME, EquipmentVariable.activePowerSetpoint.getVariableName());
        addLimitChangeLog(hvdcLineToCS2toCS1Values, MappingLimitType.MIN, version, TimeSeriesConstants.MINUS_CS21, EquipmentVariable.activePowerSetpoint.getVariableName());
        addLimitChangeLog(hvdcLineToCS1toCS2Values, MappingLimitType.MAX, version, TimeSeriesConstants.CS12, EquipmentVariable.activePowerSetpoint.getVariableName());

        // Add scaling down logs synthesis to logger
        targetPTimeSeriesToScalingDownPowerChangeSynthesis.forEach((key, value) -> value.forEach(change -> addScalingDownLogSynthesis(EquipmentVariable.targetP.getVariableName(), change, version, key)));
        targetPTimeSeriesToScalingDownLimitViolationSynthesis.forEach((key, value) -> value.forEach(change -> addScalingDownLimitViolationLogSynthesis(change, version, key)));
        setpointTimeSeriesToScalingDownPowerChangeSynthesis.forEach((key, value) -> value.forEach(change -> addScalingDownLogSynthesis(EquipmentVariable.activePowerSetpoint.getVariableName(), change, version, key)));
        setpointTimeSeriesToScalingDownLimitViolationSynthesis.forEach((key, value) -> value.forEach(change -> addScalingDownLimitViolationLogSynthesis(change, version, key)));

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

    public TimeSeriesMapperChecker(List<TimeSeriesMapperObserver> observers, TimeSeriesMappingLogger timeSeriesMappingLogger, TimeSeriesMapperParameters parameters) {
        super(observers);
        this.timeSeriesMappingLogger = Objects.requireNonNull(timeSeriesMappingLogger);
        this.toleranceThreshold = parameters.getToleranceThreshold();
    }

    public void timeSeriesMappedToEquipments(int point, String timeSeriesName, double timeSeriesValue, List<Identifiable<?>> identifiables, MappingVariable variable, double[] equipmentValues, boolean ignoreLimits) {
        if (variable == EquipmentVariable.targetP) {
            targetPTimeSeriesToEquipments.put(timeSeriesName, new MappedEquipments(timeSeriesValue, new HashSet<>(identifiables)));
        } else if (variable == EquipmentVariable.activePowerSetpoint) {
            setpointTimeSeriesToEquipments.put(timeSeriesName, new MappedEquipments(timeSeriesValue, new HashSet<>(identifiables)));
        }

        for (int i = 0; i < identifiables.size(); i++) {
            Identifiable<?> identifiable = identifiables.get(i);
            double equipmentValue = equipmentValues[i];

            if (TimeSeriesMapper.isPowerOrLimitVariable(variable)) {
                // Store mapped power values and limits in order to correct power values not included in limits
                addTimeSeriesMappedToEquipments(point, timeSeriesName, identifiable, variable, equipmentValue, ignoreLimits);
            }

            if (identifiable instanceof HvdcLine hvdcLine) {
                hvdcLineToActivePowerRange.computeIfAbsent(identifiable.getId(), e -> hvdcLine.getExtension(HvdcOperatorActivePowerRange.class) != null);
            }

            if (!TimeSeriesMapper.isPowerVariable(variable)) {
                // For power values, observers will be notified later after correction
                // For other values, observers are notified immediately
                super.timeSeriesMappedToEquipment(point, timeSeriesName, identifiable, variable, equipmentValue);
            }
        }
    }

    private void addTimeSeriesMappedToEquipments(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue, boolean ignoreLimits) {
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

    private void correctAndNotifyMappedPowers(int point, Identifiable<?> identifiable, MappedPower mappedPower) {
        double value;
        if (identifiable instanceof Generator generator) {
            value = correctMappedPowerGenerator(point, generator, mappedPower);
        } else if (identifiable instanceof HvdcLine hvdcLine) {
            value = correctMappedPowerHvdcLine(point, hvdcLine, mappedPower);
        } else {
            throw new AssertionError("Unsupported equipment type for id " + identifiable.getId());
        }
        mappedPower.setP(value);
        super.timeSeriesMappedToEquipment(point, timeSeriesName != null ? timeSeriesName : "", identifiable, TimeSeriesMapper.getPowerVariable(identifiable), value);
    }

    private double correctMappedPowerGenerator(int point, Generator generator, MappedPower mappedPower) {

        initCorrector(mappedPower);
        id = generator.getId();
        minP = isMappedMinP ? mappedPower.getMinP() : generator.getMinP();
        maxP = isMappedMaxP ? mappedPower.getMaxP() : generator.getMaxP();
        if (minP > maxP) {
            throw new AssertionError(String.format("Equipment '%s' : invalid active limits [%s, %s] at point %s", id, minP, maxP, point));
        }
        final boolean isMappedTargetP = mappedPower.getP() != null;
        double targetP = isMappedTargetP ? mappedPower.getP() : TimeSeriesMapper.getP(generator);
        isOkMinP = targetP >= minP - toleranceThreshold;
        isOkMaxP = targetP <= maxP + toleranceThreshold;
        targetP = applyToleranceThresholdOnTargetP(isMappedTargetP, targetP);

        addGeneratorLimitValue(generator, isMappedTargetP, targetP);

        if (!isMappedTargetP) {
            double result = correctMappedPowerGeneratorWhenTargetPIsNotMapped(targetP, point, TARGET_P_VARIABLE_NAME, false);
            if (!Double.isNaN(result)) {
                return result;
            }
        }

        if (ignoreLimits) {
            double result = correctMappedPowerGeneratorWithIgnoreLimits(targetP, generator);
            if (!Double.isNaN(result)) {
                return result;
            }
        }

        return correctMappedPowerGeneratorIsTargetP(targetP, isMappedTargetP);
    }

    private double correctMappedPowerGeneratorIsTargetP(double targetP, boolean isMappedTargetP) {
        double newValue = Double.NaN;
        ScalingDownPowerChange change = null;
        if (!isOkMaxP) {
            change = isMappedMaxP ? ScalingDownPowerChange.MAPPED_MAXP : ScalingDownPowerChange.BASE_CASE_MAXP;
            newValue = maxP;
        } else if (!isOkMinP && minP <= 0) {
            change = isMappedMinP ? ScalingDownPowerChange.MAPPED_MINP : ScalingDownPowerChange.BASE_CASE_MINP;
            newValue = minP;
        } else if (!isOkMinP && targetP < 0) {
            change = ScalingDownPowerChange.ZERO;
            newValue = 0;
        }
        if (!Double.isNaN(newValue)) {
            targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(change);
            return newValue;
        }
        if (!isOkMinP) {
            ScalingDownLimitViolation changes = isMappedMinP ? ScalingDownLimitViolation.MAPPED_MINP_BY_TARGETP : ScalingDownLimitViolation.BASE_CASE_MINP_BY_TARGETP;
            targetPTimeSeriesToEquipments.get(timeSeriesName).getScalingDownLimitViolation().add(changes);
            return targetP;
        }
        if (isMappedTargetP) {
            return targetP;
        }
        return newValue;
    }

    private double correctMappedPowerGeneratorWithIgnoreLimits(double targetP, Generator generator) {
        MappedEquipments mappedEquipments = targetPTimeSeriesToEquipments.get(timeSeriesName);
        if (!isOkMaxP && !isMappedMaxP) {
            generator.setMaxP(Math.round(targetP + 0.5f));
            mappedEquipments.getScalingDownLimitViolation().add(ScalingDownLimitViolation.MAXP_BY_TARGETP);
            return targetP;
        } else if (!isOkMaxP) {
            mappedEquipments.getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MAXP_DISABLED);
            return maxP;
        } else if (!isOkMinP && minP <= 0 && !isMappedMinP) {
            // targetP is mapped, minP is not mapped -> reduce base case minP to targetP
            generator.setMinP(Math.floor(targetP - 0.5f));
            mappedEquipments.getScalingDownLimitViolation().add(ScalingDownLimitViolation.MINP_BY_TARGETP);
            return targetP;
        } else if (!isOkMinP && minP <= 0) {
            mappedEquipments.getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MINP_DISABLED);
            return minP;
        } else if (!isOkMinP && targetP < 0) {
            mappedEquipments.getScalingDownPowerChange().add(ScalingDownPowerChange.ZERO_DISABLED);
            return 0;
        }
        return Double.NaN;
    }

    private double correctMappedPowerHvdcLinesWithIgnoreLimits(double setpoint, HvdcLine hvdcLine, boolean isActivePowerRange) {
        final long round = Math.round(Math.abs(setpoint) + 0.5f);
        if (!isOkMaxP && !isMappedMaxP) {
            // setpoint is mapped, maxP is not mapped -> increase base case maxP to setpoint
            addActivePowerRangeExtension(hvdcLine);
            if (!isActivePowerRange) {
                hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS1toCS2((float) Math.abs(hvdcLineToMinValues.get(hvdcLine).getBaseCaseLimit()));
            }
            TimeSeriesMapper.setHvdcMax(hvdcLine, round);
            setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownLimitViolation().add(isActivePowerRange ? ScalingDownLimitViolation.CS1TOCS2_BY_ACTIVEPOWER : ScalingDownLimitViolation.MAXP_BY_ACTIVEPOWER);
            return setpoint;
        } else if (!isOkMaxP) {
            setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MAXP_DISABLED);
            return maxP;
        } else if (!isOkMinP && !isMappedMinP) {
            addActivePowerRangeExtension(hvdcLine);
            if (!isActivePowerRange) {
                hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS2toCS1((float) Math.abs(hvdcLineToMaxValues.get(hvdcLine).getBaseCaseLimit()));
            }
            TimeSeriesMapper.setHvdcMin(hvdcLine, round);
            setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownLimitViolation().add(isActivePowerRange ? ScalingDownLimitViolation.CS2TOCS1_BY_ACTIVEPOWER : ScalingDownLimitViolation.MINP_BY_ACTIVEPOWER);
            return setpoint;
        } else if (!isOkMinP) {
            setpointTimeSeriesToEquipments.get(timeSeriesName).getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MINP_DISABLED);
            return minP;
        }
        return Double.NaN;
    }

    private double correctMappedPowerHvdcLinesWithNotIgnoreLimits(boolean isActivePowerRange) {
        MappedEquipments mappedEquipments = setpointTimeSeriesToEquipments.get(timeSeriesName);
        if (!isOkMaxP && isMappedMaxP) {
            mappedEquipments.getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MAXP);
            return maxP;
        } else if (!isOkMaxP) {
            ScalingDownPowerChange change = isActivePowerRange ? ScalingDownPowerChange.BASE_CASE_CS1TOCS2 : ScalingDownPowerChange.BASE_CASE_MAXP;
            mappedEquipments.getScalingDownPowerChange().add(change);
            return maxP;
        } else if (!isOkMinP && isMappedMinP) {
            mappedEquipments.getScalingDownPowerChange().add(ScalingDownPowerChange.MAPPED_MINP);
            return minP;
        } else if (!isOkMinP) {
            ScalingDownPowerChange change = isActivePowerRange ? ScalingDownPowerChange.BASE_CASE_CS2TOCS1 : ScalingDownPowerChange.BASE_CASE_MINUS_MAXP;
            mappedEquipments.getScalingDownPowerChange().add(change);
            return minP;
        }
        return Double.NaN;
    }

    private double correctMappedPowerGeneratorWhenTargetPIsNotMapped(double value, int point, String variableName, boolean isHvdc) {
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged()
                .notIncludedVariable(variableName).id(id).minValue(minP).maxValue(maxP)
                .value(value).oldValue(variableName).isMapping();
        LogBuilder logbuilder = new LogBuilder().level(System.Logger.Level.WARNING).version(version).index(index).point(point);

        double newValue = Double.NaN;
        if (ignoreLimits) {
            rangeLogWithVariableChanged.disabled(true);
        }

        if (!isOkMaxP) {
            newValue = maxP;
            rangeLogWithVariableChanged.toVariable(MAX_P_VARIABLE_NAME).newValue(newValue);
        } else if (!isOkMinP && minP <= 0 || isHvdc && !isOkMinP) {
            newValue = minP;
            rangeLogWithVariableChanged.toVariable(MIN_P_VARIABLE_NAME).newValue(newValue);
        } else if (!isOkMinP && value < 0) {
            newValue = 0;
            rangeLogWithVariableChanged.toVariable("").newValue(newValue);
        } else if (!isOkMinP) {
            LogContent logContent = new RangeWithMinPViolatedByTargetP()
                    .notIncludedVariable(TARGET_P_VARIABLE_NAME).id(id).minValue(minP)
                    .maxValue(maxP).value(value).mapped().build();
            Log log = logbuilder.level(System.Logger.Level.INFO).logDescription(logContent).build();
            timeSeriesMappingLogger.addLog(log);
            return value;
        }
        if (!Double.isNaN(newValue)) {
            Log log = logbuilder.logDescription(rangeLogWithVariableChanged.build()).build();
            timeSeriesMappingLogger.addLog(log);
        }
        return newValue;
    }

    private double applyToleranceThresholdOnTargetP(boolean isMappedTargetP, double targetP) {
        if (!ignoreLimits && isMappedTargetP) {
            if (isOkMaxP && targetP >= maxP - toleranceThreshold) {
                return maxP - toleranceThreshold;
            } else if (isOkMinP && targetP <= minP + toleranceThreshold) {
                return minP + toleranceThreshold;
            }
        }
        return targetP;
    }

    private void initCorrector(MappedPower mappedPower) {
        timeSeriesName = mappedPower.getTimeSeriesNameP();
        isMappedMinP = mappedPower.getMinP() != null;
        isMappedMaxP = mappedPower.getMaxP() != null;
        ignoreLimits = mappedPower.isIgnoreLimits();
    }

    private void addGeneratorLimitValue(Generator generator, boolean isMappedTargetP, double targetP) {
        if (ignoreLimits && isMappedTargetP) {
            if (!isMappedMaxP) {
                addLimitValueChange(MappingLimitType.MAX, generatorToMaxValues, generator, generator.getMaxP(), targetP);
            }
            if (!isMappedMinP && minP <= 0) {
                addLimitValueChange(MappingLimitType.MIN, generatorToMinValues, generator, generator.getMinP(), targetP);
            }
        }
    }

    private void addHvdcLineLimitValue(HvdcLine hvdcLine, boolean isMappedSetpoint, boolean isActivePowerRange, double setpoint) {
        if (ignoreLimits && isMappedSetpoint) {
            if (!isMappedMaxP) {
                addLimitValueChange(MappingLimitType.MAX, isActivePowerRange ? hvdcLineToCS1toCS2Values : hvdcLineToMaxValues, hvdcLine, TimeSeriesMapper.getMax(hvdcLine), setpoint);
            }
            if (!isMappedMinP) {
                addLimitValueChange(MappingLimitType.MIN, isActivePowerRange ? hvdcLineToCS2toCS1Values : hvdcLineToMinValues, hvdcLine, TimeSeriesMapper.getMin(hvdcLine), setpoint);
            }
        }
    }

    private double correctMappedPowerHvdcLine(int point, HvdcLine hvdcLine, MappedPower mappedPower) {

        initCorrector(mappedPower);
        id = hvdcLine.getId();
        minP = isMappedMinP ? mappedPower.getMinP() : TimeSeriesMapper.getMin(hvdcLine);
        maxP = isMappedMaxP ? mappedPower.getMaxP() : TimeSeriesMapper.getMax(hvdcLine);
        final boolean isMappedSetpoint = mappedPower.getP() != null;
        final boolean isActivePowerRange = hvdcLineToActivePowerRange.get(id);
        double setpoint = isMappedSetpoint ? mappedPower.getP() : TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine);
        isOkMinP = setpoint >= minP - toleranceThreshold;
        isOkMaxP = setpoint <= maxP + toleranceThreshold;

        setpoint = applyToleranceThresholdOnTargetP(isMappedSetpoint, setpoint);

        if (hvdcLine.getMaxP() < 0) {
            throw new AssertionError(String.format("Equipment '%s' : invalid active limit maxP %s at point %s", id, hvdcLine.getMaxP(), point));
        } else if (isActivePowerRange && (minP > 0 || maxP < 0)) {
            throw new AssertionError(String.format("Equipment '%s' : invalid active limits [%s, %s] at point %s", id, minP, maxP, point));
        }

        addHvdcLineLimitValue(hvdcLine, isMappedSetpoint, isActivePowerRange, setpoint);

        if (!isMappedSetpoint) {
            double result = correctMappedPowerGeneratorWhenTargetPIsNotMapped(setpoint, point, SET_POINT_VARIABLE_NAME, true);
            if (!Double.isNaN(result)) {
                return result;
            }
        }

        if (ignoreLimits) {
            double result = correctMappedPowerHvdcLinesWithIgnoreLimits(setpoint, hvdcLine, isActivePowerRange);
            if (!Double.isNaN(result)) {
                return result;
            }
        }

        double result = correctMappedPowerHvdcLinesWithNotIgnoreLimits(isActivePowerRange);
        if (!Double.isNaN(result)) {
            return result;
        }

        if (isMappedSetpoint) {
            return setpoint;
        }
        return Double.NaN;
    }

    private void addLimitValueChange(MappingLimitType limitType, Map<Identifiable<?>, LimitChange> equipmentToLimitValues, Identifiable<?> identifiable, double oldLimit, double newLimit) {
        if (limitType == MappingLimitType.MAX || limitType == MappingLimitType.MIN) {
            equipmentToLimitValues.computeIfAbsent(identifiable, e -> new LimitChange(oldLimit, Double.NaN));
            if (limitType == MappingLimitType.MAX && newLimit > oldLimit + toleranceThreshold
                    || limitType == MappingLimitType.MIN && newLimit < oldLimit - toleranceThreshold) {
                LimitChange limitChange = equipmentToLimitValues.get(identifiable);
                limitChange.setLimit(newLimit);
            }
        }
        equipmentToLimitValues.computeIfPresent(identifiable, (k, v) -> {
            LimitChange limitChange = equipmentToLimitValues.get(identifiable);
            if (!Double.isNaN(limitChange.getLimit()) && limitType == MappingLimitType.MAX && newLimit > limitChange.getBaseCaseLimit()
                    || limitType == MappingLimitType.MIN && newLimit < limitChange.getBaseCaseLimit()) {
                limitChange.setBaseCaseLimitNbOfViolation(limitChange.getBaseCaseLimitNbOfViolation() + 1);
            }
            return v;
        });
    }

    private void addScalingDownLogs(TimeSeriesIndex index, int point, String timeSeriesName, MappedEquipments mappedEquipments,
                                    MappingVariable variable,
                                    Map<String, Set<ScalingDownPowerChange>> timeSeriesToPowerChange,
                                    Map<String, Set<ScalingDownLimitViolation>> timeSeriesToLimitViolation) {

        if (!mappedEquipments.getScalingDownPowerChange().isEmpty()) {
            double value = mappedEquipments.getTimeSeriesValue();
            Set<Identifiable<?>> identifiables = mappedEquipments.getIdentifiables();
            double sum = identifiables.stream()
                    .mapToDouble(e -> identifiableToMappedPower.get(e).getP() != null ? identifiableToMappedPower.get(e).getP() : TimeSeriesMapper.getP(e))
                    .sum();
            mappedEquipments.getScalingDownPowerChange().forEach(e -> addScalingDownLog(variable.getVariableName(), e, index, timeSeriesName, value, sum, point));

            timeSeriesToPowerChange.computeIfAbsent(timeSeriesName, e -> new HashSet<>());
            Set<ScalingDownPowerChange> powerChange = timeSeriesToPowerChange.get(timeSeriesName);
            powerChange.addAll(mappedEquipments.getScalingDownPowerChange());
        }

        if (!mappedEquipments.getScalingDownLimitViolation().isEmpty()) {
            timeSeriesToLimitViolation.computeIfAbsent(timeSeriesName, e -> new HashSet<>());
            Set<ScalingDownLimitViolation> scalingDownLimitViolation = timeSeriesToLimitViolation.get(timeSeriesName);
            scalingDownLimitViolation.addAll(mappedEquipments.getScalingDownLimitViolation());
        }
    }

    private void addScalingDownLog(String changedVariable, ScalingDownPowerChange change, TimeSeriesIndex index, String timeSeriesName, double value, double sum, int point) {
        ScalingDownChangeToVariable logBuilder = new ScalingDownChangeToVariable().changedVariable(changedVariable)
                .timeSeriesName(timeSeriesName).timeSeriesValue(value).sum(sum);

        if (ScalingDownPowerChange.MAPPED_MAXP_DISABLED.equals(change)
                || ScalingDownPowerChange.MAPPED_MINP_DISABLED.equals(change)
                || ScalingDownPowerChange.ZERO_DISABLED.equals(change)) {
            logBuilder.disabled(true);
        }
        LogContent logContent;

        switch (change) {
            case ZERO, ZERO_DISABLED -> logContent = logBuilder.toVariable("0").build();
            case BASE_CASE_MINP -> logContent = logBuilder.toVariable(MIN_P_VARIABLE_NAME).basecase().build();
            case BASE_CASE_MAXP -> logContent = logBuilder.toVariable(MAX_P_VARIABLE_NAME).basecase().build();
            case MAPPED_MINP_DISABLED, MAPPED_MINP -> logContent = logBuilder.toVariable(MIN_P_VARIABLE_NAME).mapped().build();
            case MAPPED_MAXP_DISABLED, MAPPED_MAXP -> logContent = logBuilder.toVariable(MAX_P_VARIABLE_NAME).mapped().build();
            case BASE_CASE_CS1TOCS2 -> logContent = logBuilder.toVariable(TimeSeriesConstants.CS12).basecase().build();
            case BASE_CASE_CS2TOCS1 -> logContent = logBuilder.toVariable(TimeSeriesConstants.MINUS_CS21).basecase().build();
            case BASE_CASE_MINUS_MAXP -> logContent = logBuilder.toVariable(TimeSeriesConstants.MINUS_MAXP).basecase().build();
            default -> throw new AssertionError(String.format(UNHANDLED_SCALING_OPERATION_ERROR, change.name()));
        }
        Log log = new LogBuilder().level(System.Logger.Level.WARNING).index(index).version(version).point(point).logDescription(logContent).build();
        timeSeriesMappingLogger.addLog(log);
    }

    private void addScalingDownLogSynthesis(String changedVariable, ScalingDownPowerChange change, int version, String timeSeriesName) {
        ScalingDownChangeToVariable scalingDownChangeToVariable = new ScalingDownChangeToVariable()
                .changedVariable(changedVariable).timeSeriesName(timeSeriesName);
        LogContent logContent = switch (change) {
            case BASE_CASE_MINP ->
                scalingDownChangeToVariable.toVariable(MIN_P_VARIABLE_NAME).basecase().synthesis(true).build();
            case BASE_CASE_MAXP ->
                scalingDownChangeToVariable.toVariable(MAX_P_VARIABLE_NAME).basecase().synthesis(true).build();
            case MAPPED_MINP ->
                scalingDownChangeToVariable.toVariable(MIN_P_VARIABLE_NAME).mapped().synthesis(true).build();
            case MAPPED_MAXP ->
                scalingDownChangeToVariable.toVariable(MAX_P_VARIABLE_NAME).mapped().synthesis(true).build();
            case ZERO -> scalingDownChangeToVariable.toVariable("0").synthesis(true).build();
            case MAPPED_MINP_DISABLED ->
                scalingDownChangeToVariable.toVariable(MIN_P_VARIABLE_NAME).mapped().disabled(true).synthesis(true).build();
            case MAPPED_MAXP_DISABLED ->
                scalingDownChangeToVariable.toVariable(MAX_P_VARIABLE_NAME).mapped().disabled(true).synthesis(true).build();
            case ZERO_DISABLED -> scalingDownChangeToVariable.toVariable("0").disabled(true).synthesis(true).build();
            case BASE_CASE_CS1TOCS2 ->
                scalingDownChangeToVariable.toVariable(TimeSeriesConstants.CS12).basecase().synthesis(true).build();
            case BASE_CASE_CS2TOCS1 ->
                scalingDownChangeToVariable.toVariable(TimeSeriesConstants.MINUS_CS21).basecase().synthesis(true).build();
            case BASE_CASE_MINUS_MAXP ->
                scalingDownChangeToVariable.toVariable(TimeSeriesConstants.MINUS_MAXP).basecase().synthesis(true).build();
            default -> throw new AssertionError(String.format(UNHANDLED_SCALING_OPERATION_ERROR, change.name()));
        };
        Log log = new LogBuilder().index(index).version(version).level(System.Logger.Level.WARNING).point(Integer.MAX_VALUE).logDescription(logContent).build();
        timeSeriesMappingLogger.addLog(log);
    }

    private void addScalingDownLimitViolationLogSynthesis(ScalingDownLimitViolation change, int version, String timeSeriesName) {
        ScalingDownLimitChangeSynthesis scalingDownLimitChangeSynthesis = new ScalingDownLimitChangeSynthesis().timeSeriesName(timeSeriesName);
        LogContent logContent = switch (change) {
            case BASE_CASE_MINP_BY_TARGETP -> scalingDownLimitChangeSynthesis.baseCase().buildNotModified();
            case MAPPED_MINP_BY_TARGETP -> scalingDownLimitChangeSynthesis.mapped().buildNotModified();
            case MAXP_BY_TARGETP -> scalingDownLimitChangeSynthesis.violatedVariable(MAX_P_VARIABLE_NAME).max()
                .variable(EquipmentVariable.targetP.getVariableName()).buildLimitChange();
            case MAXP_BY_ACTIVEPOWER -> scalingDownLimitChangeSynthesis.violatedVariable(MAX_P_VARIABLE_NAME).max()
                .variable(EquipmentVariable.activePowerSetpoint.getVariableName()).buildLimitChange();
            case CS1TOCS2_BY_ACTIVEPOWER ->
                scalingDownLimitChangeSynthesis.violatedVariable(TimeSeriesConstants.CS12).max()
                    .variable(EquipmentVariable.activePowerSetpoint.getVariableName()).buildLimitChange();
            case MINP_BY_TARGETP -> scalingDownLimitChangeSynthesis.violatedVariable(MIN_P_VARIABLE_NAME).min()
                .variable(EquipmentVariable.targetP.getVariableName()).buildLimitChange();
            case MINP_BY_ACTIVEPOWER ->
                scalingDownLimitChangeSynthesis.violatedVariable(TimeSeriesConstants.MINUS_MAXP).min()
                    .variable(EquipmentVariable.activePowerSetpoint.getVariableName()).buildLimitChange();
            case CS2TOCS1_BY_ACTIVEPOWER ->
                scalingDownLimitChangeSynthesis.violatedVariable(TimeSeriesConstants.MINUS_CS21).min()
                    .variable(EquipmentVariable.activePowerSetpoint.getVariableName()).buildLimitChange();
            default -> throw new AssertionError(String.format(UNHANDLED_SCALING_OPERATION_ERROR, change.name()));
        };
        Log log = new LogBuilder().index(index).version(version).level(System.Logger.Level.INFO).point(Integer.MAX_VALUE)
                .logDescription(logContent).build();
        timeSeriesMappingLogger.addLog(log);
    }

    private void addLimitChangeLog(Map<Identifiable<?>, LimitChange> equipmentToValues, MappingLimitType limitType,
                                   int version, String variableToChange, String variable) {
        equipmentToValues.entrySet().stream()
                .filter(e -> !Double.isNaN(e.getValue().getLimit()))
                .forEach(e -> timeSeriesMappingLogger.addLog(getLimitLog(limitType, version, variableToChange, variable, e)));
    }

    private Log getLimitLog(MappingLimitType limitType, int version, String variableToChange,
                                        String variable, Map.Entry<Identifiable<?>, LimitChange> e) {
        LimitLogBuilder limitLogBuilder = new LimitLogBuilder()
                .id(e.getKey().getId())
                .variable(variable)
                .nbViolation(e.getValue().getBaseCaseLimitNbOfViolation())
                .newValue(e.getValue().getLimit())
                .oldValue(e.getValue().getBaseCaseLimit())
                .variableToChange(variableToChange);
        LogContent logContent;
        if (limitType == MappingLimitType.MIN) {
            logContent = limitLogBuilder.isMin().build();
        } else {
            logContent = limitLogBuilder.isMax().build();
        }
        return new LogBuilder().level(System.Logger.Level.INFO)
                .index(index)
                .version(version)
                .point(Integer.MAX_VALUE)
                .logDescription(logContent)
                .build();
    }
}
