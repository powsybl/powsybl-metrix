/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Battery;
import com.powsybl.metrix.mapping.log.LogBuilder;
import com.powsybl.metrix.mapping.log.LogContent;
import com.powsybl.metrix.mapping.log.RangeLogWithVariableChanged;
import com.powsybl.metrix.mapping.log.RangeWithMinPViolatedByTargetP;
import com.powsybl.timeseries.TimeSeriesIndex;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public class BatteryBoundLimitBuilder {
    private double minP;
    private double maxP;
    private double targetP;
    private boolean ignoreLimits;
    private boolean isOkMinP;
    private boolean isOkMaxP;
    private boolean isUnmappedMinP;
    private boolean isUnmappedMaxP;
    private int version;
    private TimeSeriesIndex index;
    final String targetPVariableName = EquipmentVariable.TARGET_P.getVariableName();
    final String maxPVariableName = EquipmentVariable.MAX_P.getVariableName();
    final String minPVariableName = EquipmentVariable.MIN_P.getVariableName();
    public static final int CONSTANT_VARIANT_ID = -1;
    private String id;

    public BatteryBoundLimitBuilder() {
        //nothing to do
    }

    public BatteryBoundLimitBuilder minP(double minP) {
        this.minP = minP;
        return this;
    }

    public BatteryBoundLimitBuilder maxP(double maxP) {
        this.maxP = maxP;
        return this;
    }

    public BatteryBoundLimitBuilder targetP(double targetP) {
        this.targetP = targetP;
        return this;
    }

    public BatteryBoundLimitBuilder ignoreLimits(boolean ignoreLimits) {
        this.ignoreLimits = ignoreLimits;
        return this;
    }

    public BatteryBoundLimitBuilder isUnmappedMinP(boolean isUnmappedMinP) {
        this.isUnmappedMinP = isUnmappedMinP;
        return this;
    }

    public BatteryBoundLimitBuilder isUnmappedMaxP(boolean isUnmappedMaxP) {
        this.isUnmappedMaxP = isUnmappedMaxP;
        return this;
    }

    public BatteryBoundLimitBuilder version(int version) {
        this.version = version;
        return this;
    }

    public BatteryBoundLimitBuilder index(TimeSeriesIndex index) {
        this.index = index;
        return this;
    }

    public void setAll(Battery battery, TimeSeriesMappingLogger logger) {
        verifyEntries(battery);
        if (ignoreLimits) {
            setWithoutLimits(battery, logger);
            return;
        }
        setWithLimits(battery, logger);
    }

    private void setWithLimits(Battery battery, TimeSeriesMappingLogger logger) {
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged()
                .notIncludedVariable(targetPVariableName).id(id).minValue(minP).maxValue(maxP).value(targetP).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().level(System.Logger.Level.WARNING).version(version).index(index).point(CONSTANT_VARIANT_ID);
        if (!isOkMaxP && isUnmappedMaxP) {
            LogContent logContent = rangeLogWithVariableChanged.toVariable(maxPVariableName).newValue(maxP).oldValue(targetPVariableName).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            battery.setTargetP(maxP);
        } else if (!isOkMinP && isUnmappedMinP && minP <= 0) {
            LogContent logContent = rangeLogWithVariableChanged.toVariable(minPVariableName).oldValue(targetPVariableName).newValue(minP).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            battery.setTargetP(minP);
        } else if (!isOkMinP && isUnmappedMinP && targetP < 0) {
            LogContent logContent = rangeLogWithVariableChanged.toVariable("").oldValue(targetPVariableName).newValue(0).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            battery.setTargetP(0);
        } else if (!isOkMinP && isUnmappedMinP) {
            LogContent logContent = new RangeWithMinPViolatedByTargetP()
                    .notIncludedVariable(targetPVariableName).id(id).minValue(minP).maxValue(maxP).value(targetP).baseCase().build();
            logger.addLog(logBuilder.level(System.Logger.Level.INFO).logDescription(logContent).build());
        }
    }

    private void setWithoutLimits(Battery battery, TimeSeriesMappingLogger logger) {
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged()
                .notIncludedVariable(targetPVariableName).id(id).minValue(minP).maxValue(maxP).value(targetP).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().level(System.Logger.Level.INFO).version(version).index(index).point(CONSTANT_VARIANT_ID);
        if (!isOkMaxP && isUnmappedMaxP) {
            LogContent logContent = rangeLogWithVariableChanged.oldValue(maxPVariableName).toVariable(targetPVariableName).newValue(targetP).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            battery.setMaxP(targetP);
        } else if (!isOkMinP && isUnmappedMinP && minP <= 0) {
            LogContent logContent = rangeLogWithVariableChanged.oldValue(minPVariableName).toVariable(targetPVariableName).newValue(targetP).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            battery.setMinP(targetP);
        } else if (!isOkMinP && isUnmappedMinP && targetP < 0) {
            LogContent logContent = rangeLogWithVariableChanged.oldValue(targetPVariableName).toVariable("").newValue(0).disabled(true).build();
            logger.addLog(logBuilder.level(System.Logger.Level.WARNING).logDescription(logContent).build());
            battery.setTargetP(0);
        } else if (!isOkMinP && isUnmappedMinP) {
            LogContent logContent = new RangeWithMinPViolatedByTargetP()
                    .notIncludedVariable(targetPVariableName).id(id).minValue(minP)
                    .maxValue(maxP).value(targetP).baseCase().build();
            logger.addLog(logBuilder.logDescription(logContent).build());
        }
    }

    private void verifyEntries(Battery battery) {
        if (minP > maxP) {
            throw new AssertionError("Equipment '" + battery.getId() + "' : invalid active limits [" + minP + ", " + maxP + "] in base case");
        }
        this.isOkMinP = targetP >= minP;
        this.isOkMaxP = targetP <= maxP;
        this.id = battery.getId();
    }
}
