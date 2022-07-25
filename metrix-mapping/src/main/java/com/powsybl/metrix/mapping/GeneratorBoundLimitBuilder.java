/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Generator;
import com.powsybl.metrix.mapping.log.*;
import com.powsybl.timeseries.TimeSeriesIndex;

public class GeneratorBoundLimitBuilder {
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
    final String targetPVariableName = EquipmentVariable.targetP.getVariableName();
    final String maxPVariableName = EquipmentVariable.maxP.getVariableName();
    final String minPVariableName = EquipmentVariable.minP.getVariableName();
    public static final int CONSTANT_VARIANT_ID = -1;
    private String id;

    public GeneratorBoundLimitBuilder() {
        //nothing to do
    }

    public GeneratorBoundLimitBuilder minP(double minP) {
        this.minP = minP;
        return this;
    }

    public GeneratorBoundLimitBuilder maxP(double maxP) {
        this.maxP = maxP;
        return this;
    }

    public GeneratorBoundLimitBuilder targetP(double targetP) {
        this.targetP = targetP;
        return this;
    }

    public GeneratorBoundLimitBuilder ignoreLimits(boolean ignoreLimits) {
        this.ignoreLimits = ignoreLimits;
        return this;
    }

    public GeneratorBoundLimitBuilder isUnmappedMinP(boolean isUnmappedMinP) {
        this.isUnmappedMinP = isUnmappedMinP;
        return this;
    }

    public GeneratorBoundLimitBuilder isUnmappedMaxP(boolean isUnmappedMaxP) {
        this.isUnmappedMaxP = isUnmappedMaxP;
        return this;
    }

    public GeneratorBoundLimitBuilder version(int version) {
        this.version = version;
        return this;
    }

    public GeneratorBoundLimitBuilder index(TimeSeriesIndex index) {
        this.index = index;
        return this;
    }

    public void setAll(Generator generator, TimeSeriesMappingLogger logger) {
        verifyEntries(generator);
        if (ignoreLimits) {
            setWithoutLimits(generator, logger);
            return;
        }
        setWithLimits(generator, logger);
    }

    private void setWithLimits(Generator generator, TimeSeriesMappingLogger logger) {
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged()
                .notIncludedVariable(targetPVariableName).id(id).minValue(minP).maxValue(maxP).value(targetP).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().level(System.Logger.Level.WARNING).version(version).index(index).point(CONSTANT_VARIANT_ID);
        if (!isOkMaxP && isUnmappedMaxP) {
            LogContent logContent = rangeLogWithVariableChanged.toVariable(maxPVariableName).newValue(maxP).oldValue(targetPVariableName).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            generator.setTargetP(maxP);
        } else if (!isOkMinP && isUnmappedMinP && minP <= 0) {
            LogContent logContent = rangeLogWithVariableChanged.toVariable(minPVariableName).oldValue(targetPVariableName).newValue(minP).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            generator.setTargetP(minP);
        } else if (!isOkMinP && isUnmappedMinP && targetP < 0) {
            LogContent logContent = rangeLogWithVariableChanged.toVariable("").oldValue(targetPVariableName).newValue(0).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            generator.setTargetP(0);
        } else if (!isOkMinP && isUnmappedMinP) {
            LogContent logContent = new RangeWithMinPViolatedByTargetP()
                    .notIncludedVariable(targetPVariableName).id(id).minValue(minP).maxValue(maxP).value(targetP).baseCase().build();
            logger.addLog(logBuilder.level(System.Logger.Level.INFO).logDescription(logContent).build());
        }
    }

    private void setWithoutLimits(Generator generator, TimeSeriesMappingLogger logger) {
        RangeLogWithVariableChanged rangeLogWithVariableChanged = new RangeLogWithVariableChanged()
                .notIncludedVariable(targetPVariableName).id(id).minValue(minP).maxValue(maxP).value(targetP).isBaseCase();
        LogBuilder logBuilder = new LogBuilder().level(System.Logger.Level.INFO).version(version).index(index).point(CONSTANT_VARIANT_ID);
        if (!isOkMaxP && isUnmappedMaxP) {
            LogContent logContent = rangeLogWithVariableChanged.oldValue(maxPVariableName).toVariable(targetPVariableName).newValue(targetP).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            generator.setMaxP(targetP);
        } else if (!isOkMinP && isUnmappedMinP && minP <= 0) {
            LogContent logContent = rangeLogWithVariableChanged.oldValue(minPVariableName).toVariable(targetPVariableName).newValue(targetP).build();
            logger.addLog(logBuilder.logDescription(logContent).build());
            generator.setMinP(targetP);
        } else if (!isOkMinP && isUnmappedMinP && targetP < 0) {
            LogContent logContent = rangeLogWithVariableChanged.oldValue(targetPVariableName).toVariable("").newValue(0).disabled(true).build();
            logger.addLog(logBuilder.level(System.Logger.Level.WARNING).logDescription(logContent).build());
            generator.setTargetP(0);
        } else if (!isOkMinP && isUnmappedMinP) {
            LogContent logContent =  new RangeWithMinPViolatedByTargetP()
                    .notIncludedVariable(targetPVariableName).id(id).minValue(minP)
                    .maxValue(maxP).value(targetP).baseCase().build();
            logger.addLog(logBuilder.logDescription(logContent).build());
        }
    }

    private void verifyEntries(Generator generator) {
        if (minP > maxP) {
            throw new AssertionError("Equipment '" + generator.getId() + "' : invalid active limits [" + minP + ", " + maxP + "] in base case");
        }
        this.isOkMinP = targetP >= minP;
        this.isOkMaxP = targetP <= maxP;
        this.id = generator.getId();
    }
}
