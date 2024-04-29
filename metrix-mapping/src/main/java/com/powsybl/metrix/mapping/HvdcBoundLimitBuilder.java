/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.metrix.mapping.log.*;
import com.powsybl.timeseries.TimeSeriesIndex;

import static com.powsybl.metrix.mapping.TimeSeriesConstants.*;
import static com.powsybl.metrix.mapping.TimeSeriesConstants.MINUS_MAXP;
import static com.powsybl.metrix.mapping.TimeSeriesMapper.addActivePowerRangeExtension;

public class HvdcBoundLimitBuilder {
    private boolean isActivePowerRange;
    private double minP;
    private double maxP;
    private double hvdcLineMaxP;
    private double setPoint;
    private boolean ignoreLimits;
    private boolean isUnmappedMinP;
    private boolean isUnmappedMaxP;
    private int version;
    private TimeSeriesIndex index;

    final String setPointVariableName = EquipmentVariable.activePowerSetpoint.getVariableName();
    public static final int CONSTANT_VARIANT_ID = -1;
    private String id;
    private double correctedMaxP;
    private double correctedMinP;
    private String maxPVariableName;
    private String minPVariableName;

    public HvdcBoundLimitBuilder() {
        //nothing to do
    }

    public HvdcBoundLimitBuilder isActivePowerRange(boolean isActivePowerRange) {
        this.isActivePowerRange = isActivePowerRange;
        return this;
    }

    public HvdcBoundLimitBuilder minP(double minP) {
        this.minP = minP;
        return this;
    }

    public HvdcBoundLimitBuilder maxP(double maxP) {
        this.maxP = maxP;
        return this;
    }

    public HvdcBoundLimitBuilder hvdcLineMaxP(double hvdcLineMaxP) {
        this.hvdcLineMaxP = hvdcLineMaxP;
        return this;
    }

    public HvdcBoundLimitBuilder setPoint(double setPoint) {
        this.setPoint = setPoint;
        return this;
    }

    public HvdcBoundLimitBuilder ignoreLimits(boolean ignoreLimits) {
        this.ignoreLimits = ignoreLimits;
        return this;
    }

    public HvdcBoundLimitBuilder isUnmappedMinP(boolean isUnmappedMinP) {
        this.isUnmappedMinP = isUnmappedMinP;
        return this;
    }

    public HvdcBoundLimitBuilder isUnmappedMaxP(boolean isUnmappedMaxP) {
        this.isUnmappedMaxP = isUnmappedMaxP;
        return this;
    }

    public HvdcBoundLimitBuilder version(int version) {
        this.version = version;
        return this;
    }

    public HvdcBoundLimitBuilder index(TimeSeriesIndex index) {
        this.index = index;
        return this;
    }

    public void setAll(HvdcLine hvdcLine, TimeSeriesMappingLogger logger) {
        verifyEntries(hvdcLine);

        maxPVariableName = isActivePowerRange ? CS12 : EquipmentVariable.maxP.getVariableName();
        minPVariableName = isActivePowerRange ? MINUS_CS21 : "-" + EquipmentVariable.maxP.getVariableName();

        correctedMaxP = maxP;
        correctedMinP = minP;

        // Add activePowerRangeExtension
        addActivePowerRangeExtension(hvdcLine);

        // maxP inconstancy with CS1toCS2/CS2toCS1
        if (isActivePowerRange && (maxP > hvdcLineMaxP || -minP > hvdcLineMaxP)) {
            setMaxPLimits(hvdcLine, logger);
        }

        boolean isMin = setPoint < correctedMinP && isUnmappedMinP;
        boolean isMax = setPoint > correctedMaxP && isUnmappedMaxP;
        if (!isMin && !isMax) {
            return;
        }

        // setPoint inconstancy with maxP/CS1toCS2/CS2toCS1
        setPointLimits(hvdcLine, logger);
    }

    private void setMaxPLimits(HvdcLine hvdcLine, TimeSeriesMappingLogger logger) {
        if (ignoreLimits) {
            setMaxPWithoutLimits(hvdcLine, logger);
        } else {
            setMaxPWithLimits(hvdcLine, logger);
        }
    }

    private void setMaxPWithoutLimits(HvdcLine hvdcLine, TimeSeriesMappingLogger logger) {
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged().id(id).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().version(version).index(index).point(CONSTANT_VARIANT_ID).level(System.Logger.Level.WARNING);
        logBuilder.level(System.Logger.Level.INFO);
        if (maxP > hvdcLineMaxP && maxP > -minP) {
            LogContent logContent = rangeLogWithVariableChanged.notIncludedVariable(maxPVariableName).minValue(0).maxValue(hvdcLineMaxP)
                    .value(maxP).oldValue(EquipmentVariable.maxP.getVariableName()).toVariable(maxPVariableName)
                    .newValue(maxP).build();
            Log log = logBuilder.logDescription(logContent).build();
            logger.addLog(log);
        } else if (maxP < -minP && -minP < hvdcLineMaxP
                || -minP > hvdcLineMaxP && maxP <= hvdcLineMaxP) {
            LogContent logContent = rangeLogWithVariableChanged.notIncludedVariable(minPVariableName).minValue(-hvdcLineMaxP).maxValue(0)
                    .value(minP).oldValue(MINUS_MAXP).toVariable(minPVariableName).newValue(minP).build();
            Log log = logBuilder.logDescription(logContent).build();
            logger.addLog(log);
        }
        hvdcLine.setMaxP(Math.max(maxP, -minP));
    }

    private void setMaxPWithLimits(HvdcLine hvdcLine, TimeSeriesMappingLogger logger) {
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged().id(id).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().version(version).index(index).point(CONSTANT_VARIANT_ID).level(System.Logger.Level.WARNING);
        if (maxP > hvdcLineMaxP) {
            LogContent logContent = rangeLogWithVariableChanged.notIncludedVariable(maxPVariableName).minValue(0).maxValue(hvdcLineMaxP).value(maxP)
                    .oldValue(maxPVariableName).toVariable(EquipmentVariable.maxP.getVariableName()).newValue(hvdcLineMaxP).build();
            Log log = logBuilder.logDescription(logContent).build();
            logger.addLog(log);
            hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS1toCS2((float) hvdcLineMaxP);
            correctedMaxP = hvdcLineMaxP;
        }
        if (minP < -hvdcLineMaxP) {
            LogContent logContent = rangeLogWithVariableChanged.notIncludedVariable(minPVariableName).minValue(-hvdcLineMaxP).maxValue(0)
                    .value(minP).oldValue(minPVariableName).toVariable(MINUS_MAXP).newValue(-hvdcLineMaxP).build();
            Log log = logBuilder.logDescription(logContent).build();
            logger.addLog(log);
            hvdcLine.getExtension(HvdcOperatorActivePowerRange.class).setOprFromCS2toCS1((float) hvdcLineMaxP);
            correctedMinP = -hvdcLineMaxP;
        }
    }

    private void setPointLimits(HvdcLine hvdcLine, TimeSeriesMappingLogger logger) {
        if (ignoreLimits) {
            setPointWithoutLimits(hvdcLine, logger);
        } else {
            setPointWithLimits(hvdcLine, logger);
        }
    }

    private void setPointWithoutLimits(HvdcLine hvdcLine, TimeSeriesMappingLogger logger) {
        boolean isMax = setPoint > correctedMaxP && isUnmappedMaxP;
        String variableName = isMax ? maxPVariableName : minPVariableName;
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged().id(id).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().version(version).index(index).point(CONSTANT_VARIANT_ID).level(System.Logger.Level.WARNING);
        logBuilder.level(System.Logger.Level.INFO);
        rangeLogWithVariableChanged.notIncludedVariable(setPointVariableName).minValue(minP).maxValue(maxP).value(setPoint);

        LogContent logContent = rangeLogWithVariableChanged.oldValue(variableName).toVariable(setPointVariableName).newValue(setPoint).build();
        if (isMax) {
            TimeSeriesMapper.setHvdcMax(hvdcLine, setPoint);
        } else {
            TimeSeriesMapper.setHvdcMin(hvdcLine, setPoint);
        }
        Log log = logBuilder.logDescription(logContent).build();
        logger.addLog(log);
    }

    private void setPointWithLimits(HvdcLine hvdcLine, TimeSeriesMappingLogger logger) {
        boolean isMax = setPoint > correctedMaxP && isUnmappedMaxP;
        String variableName = isMax ? maxPVariableName : minPVariableName;
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged().id(id).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().version(version).index(index).point(CONSTANT_VARIANT_ID).level(System.Logger.Level.WARNING);
        rangeLogWithVariableChanged.notIncludedVariable(setPointVariableName).minValue(minP).maxValue(maxP).value(setPoint);
        double newValue = isMax ? maxP : minP;
        LogContent logContent = rangeLogWithVariableChanged.oldValue(setPointVariableName).toVariable(variableName).newValue(newValue).build();
        TimeSeriesMapper.setHvdcLineSetPoint(hvdcLine, newValue);
        Log log = logBuilder.logDescription(logContent).build();
        logger.addLog(log);
    }

    private void verifyEntries(HvdcLine hvdcLine) {
        if (hvdcLineMaxP < 0) {
            throw new AssertionError("Equipment '" + hvdcLine.getId() + "' : invalid active limit maxP " + hvdcLineMaxP + " in base case");
        } else if (isActivePowerRange && (minP > 0 || maxP < 0)) {
            throw new AssertionError("Equipment '" + hvdcLine.getId() + "' : invalid active limits [" + minP + ", " + maxP + "] in base case");
        }
        this.id = hvdcLine.getId();
    }
}
