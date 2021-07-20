package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Generator;
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
        if (!isOkMaxP && isUnmappedMaxP) {
            logger.addLog(new TimeSeriesMappingLogger.BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName,
                    targetPVariableName, maxPVariableName, id, minP, maxP, targetP, maxP));
            generator.setTargetP(maxP);
        } else if (!isOkMinP && isUnmappedMinP && minP <= 0) {
            logger.addLog(new TimeSeriesMappingLogger.BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName,
                    targetPVariableName, minPVariableName, id, minP, maxP, targetP, minP));
            generator.setTargetP(minP);
        } else if (!isOkMinP && isUnmappedMinP && targetP < 0) {
            logger.addLog(new TimeSeriesMappingLogger.BaseCaseRangeWarningWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName,
                    targetPVariableName, "", id, minP, maxP, targetP, 0));
            generator.setTargetP(0);
        } else if (!isOkMinP && isUnmappedMinP) {
            logger.addLog(new TimeSeriesMappingLogger.BaseCaseRangeInfoWithBaseCaseMinPViolatedByBaseCaseTargetP(version, index, CONSTANT_VARIANT_ID, targetPVariableName,
                    targetPVariableName, id, minP, maxP, targetP));
        }
    }

    private void setWithoutLimits(Generator generator, TimeSeriesMappingLogger logger) {
        if (!isOkMaxP && isUnmappedMaxP) {
            logger.addLog(new TimeSeriesMappingLogger.BaseCaseRangeInfoWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName,
                    maxPVariableName, targetPVariableName, id, minP, maxP, targetP, targetP));
            generator.setMaxP(targetP);
        } else if (!isOkMinP && isUnmappedMinP && minP <= 0) {
            logger.addLog(new TimeSeriesMappingLogger.BaseCaseRangeInfoWithVariableChange(version, index, CONSTANT_VARIANT_ID, targetPVariableName,
                    minPVariableName, targetPVariableName, id, minP, maxP, targetP, targetP));
            generator.setMinP(targetP);
        } else if (!isOkMinP && isUnmappedMinP && targetP < 0) {
            logger.addLog(new TimeSeriesMappingLogger.BaseCaseRangeWarningWithVariableChangeDisabled(version, index, CONSTANT_VARIANT_ID, targetPVariableName,
                    targetPVariableName, "", id, minP, maxP, targetP, 0));
            generator.setTargetP(0);
        } else if (!isOkMinP && isUnmappedMinP) {
            logger.addLog(new TimeSeriesMappingLogger.BaseCaseRangeInfoWithBaseCaseMinPViolatedByBaseCaseTargetP(version, index, CONSTANT_VARIANT_ID, targetPVariableName,
                    targetPVariableName, id, minP, maxP, targetP));
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
