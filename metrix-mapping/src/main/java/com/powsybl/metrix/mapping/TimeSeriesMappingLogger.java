/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class TimeSeriesMappingLogger implements TimeSeriesConstants {

    private static final int N = 1;

    private static final DecimalFormat FORMATTER = new DecimalFormat("0." + Strings.repeat("#", N), new DecimalFormatSymbols(Locale.US));

    private static String formatDouble(double value) {
        return FORMATTER.format(value);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMapper.class);

    private static final String LABEL_SEPARATOR = " / ";

    private static final String BC_RANGE_PROBLEM = "base case range problem" + LABEL_SEPARATOR;

    private static final String MAPPING_RANGE_PROBLEM = "mapping range problem" + LABEL_SEPARATOR;

    private static final String MAPPING_SIGN_PROBLEM = "mapping sign problem" + LABEL_SEPARATOR;

    private static final String SCALING_DOWN_PROBLEM = "scaling down" + LABEL_SEPARATOR;

    private static final String LIMIT_CHANGE = "limit change" + LABEL_SEPARATOR;

    private static final String IGNORE_LIMITS_DISABLED = LABEL_SEPARATOR + "IL disabled";

    private static final String TS_SYNTHESIS = LABEL_SEPARATOR + "TS synthesis";

    enum LogType {
        WARNING,
        INFO
    }

    interface Log {

        TimeSeriesIndex getIndex();

        int getVersion();

        int getPoint();

        LogType getType();

        String getLabel();

        String getMessage();
    }

    private static class TimeSeriesLoggerConfig {

        final char separator;

        final DateTimeFormatter dateTimeFormatter;

        public TimeSeriesLoggerConfig(char separator, DateTimeFormatter dateTimeFormatter) {
            this.separator = separator;
            this.dateTimeFormatter = dateTimeFormatter;
        }
    }

    public abstract static class AbstractMappingLog implements Log {

        private final TimeSeriesIndex index;

        private final int version;

        private final int point;

        public AbstractMappingLog(TimeSeriesIndex index, int version, int point) {
            this.index = Objects.requireNonNull(index);
            this.version = version;
            this.point = point;
        }

        public TimeSeriesIndex getIndex() {
            return index;
        }

        public int getVersion() {
            return version;
        }

        public int getPoint() {
            return point;
        }
    }

    /**
     * ABSTRACT RANGE LOGS
     */

    public abstract static class AbstractRangeLog extends AbstractMappingLog {

        private final String notIncludedVariable;

        private final double value;

        private final String id;

        private final double minValue;

        private final double maxValue;

        private AbstractRangeLog(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String id, double minValue, double maxValue, double value) {
            super(index, version, point);
            this.notIncludedVariable = Objects.requireNonNull(notIncludedVariable);
            this.value = value;
            this.id = Objects.requireNonNull(id);
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        public String getMessage() {
            return notIncludedVariable + " " + formatDouble(value) + " of " + id + " not included in " + formatDouble(minValue) + " to " + formatDouble(maxValue);
        }
    }

    public abstract static class AbstractRangeLogWithVariableChanged extends AbstractRangeLog {

        private final String changedVariable;

        private final String toVariable;

        private final double newValue;

        public AbstractRangeLogWithVariableChanged(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String toVariable, String id, double minValue, double maxValue, double value, double newValue) {
            super(version, index, point, notIncludedVariable, id, minValue, maxValue, value);
            this.changedVariable = Objects.requireNonNull(changedVariable);
            this.toVariable = Objects.requireNonNull(toVariable);
            this.newValue = newValue;
        }

        public String getLabel(String problemDescription, String actionDescription) {
            return problemDescription + changedVariable + " changed to " + (toVariable.isEmpty() ? "0" : actionDescription) + toVariable;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", " + changedVariable + " changed to " + formatDouble(newValue);
        }
    }

    /**
     * RANGE LOGS
     */

    public static class BaseCaseRangeInfoWithVariableChange extends AbstractRangeLogWithVariableChanged {

        public BaseCaseRangeInfoWithVariableChange(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String toVariable, String id, double minValue, double maxValue, double value, double newValue) {
            super(version, index, point, notIncludedVariable, changedVariable, toVariable, id, minValue, maxValue, value, newValue);
        }

        @Override
        public String getLabel() {
            return super.getLabel(BC_RANGE_PROBLEM, "base case ");
        }

        @Override
        public LogType getType() {
            return LogType.INFO;
        }
    }

    public static class BaseCaseRangeWarningWithVariableChange extends AbstractRangeLogWithVariableChanged {

        public BaseCaseRangeWarningWithVariableChange(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String toVariable, String id, double minValue, double maxValue, double value, double newValue) {
            super(version, index, point, notIncludedVariable, changedVariable, toVariable, id, minValue, maxValue, value, newValue);
        }

        @Override
        public String getLabel() {
            return super.getLabel(BC_RANGE_PROBLEM, "base case ");
        }

        @Override
        public LogType getType() {
            return LogType.WARNING;
        }
    }

    public static class BaseCaseRangeWarningWithVariableChangeDisabled extends BaseCaseRangeWarningWithVariableChange {

        public BaseCaseRangeWarningWithVariableChangeDisabled(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String toVariable, String id, double minValue, double maxValue, double value, double newValue) {
            super(version, index, point, notIncludedVariable, changedVariable, toVariable, id, minValue, maxValue, value, newValue);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + IGNORE_LIMITS_DISABLED;
        }
    }

    public static class MappingRangeWarningWithVariableChange extends AbstractRangeLogWithVariableChanged {

        public MappingRangeWarningWithVariableChange(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String toVariable, String id, double minValue, double maxValue, double value, double newValue) {
            super(version, index, point, notIncludedVariable, changedVariable, toVariable, id, minValue, maxValue, value, newValue);
        }

        @Override
        public String getLabel() {
            return super.getLabel(MAPPING_RANGE_PROBLEM, "mapped ");
        }

        @Override
        public LogType getType() {
            return LogType.WARNING;
        }
    }

    public static class MappingRangeWarningWithVariableChangeDisabled extends MappingRangeWarningWithVariableChange {

        public MappingRangeWarningWithVariableChangeDisabled(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String toVariable, String id, double minValue, double maxValue, double value, double newValue) {
            super(version, index, point, notIncludedVariable, changedVariable, toVariable, id, minValue, maxValue, value, newValue);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + IGNORE_LIMITS_DISABLED;
        }

        @Override
        public LogType getType() {
            return LogType.WARNING;
        }
    }

    public static class BaseCaseRangeInfoWithBaseCaseMinPViolatedByBaseCaseTargetP extends AbstractRangeLog {

        public BaseCaseRangeInfoWithBaseCaseMinPViolatedByBaseCaseTargetP(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String id, double minValue, double maxValue, double value) {
            super(version, index, point, notIncludedVariable, id, minValue, maxValue, value);
        }

        @Override
        public String getLabel() {
            return BC_RANGE_PROBLEM + "base case minP violated by base case targetP";
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", but " + super.notIncludedVariable + " has not been changed";
        }

        public LogType getType() {
            return LogType.INFO;
        }
    }

    public static class MappingRangeInfoWithMappedMinPViolatedByTargetP extends AbstractRangeLog {

        public MappingRangeInfoWithMappedMinPViolatedByTargetP(int version, TimeSeriesIndex index, int point, String variable, String id, double minValue, double maxValue, double value) {
            super(version, index, point, variable, id, minValue, maxValue, value);
        }

        @Override
        public String getLabel() {
            return MAPPING_RANGE_PROBLEM + "mapped minP violated by targetP";
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", but " + super.notIncludedVariable + " has not been changed";
        }

        @Override
        public LogType getType() {
            return LogType.INFO;
        }
    }

    /**
     * ABSTRACT SCALING DOWN LOGS
     */

    public abstract static class AbstractScalingDownWarning extends AbstractMappingLog {

        private final String changedVariable;

        private final String timeSeriesName;

        private final double timeSeriesValue;

        private final double sum;

        public AbstractScalingDownWarning(String changedVariable, String timeSeriesName, double timeSeriesValue, double sum, TimeSeriesIndex index, int version, int point) {
            super(index, version, point);
            this.changedVariable = Objects.requireNonNull(changedVariable);
            this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
            this.timeSeriesValue = timeSeriesValue;
            this.sum = sum;
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "at least one " + changedVariable + " changed to ";
        }

        @Override
        public String getMessage() {
            return "Impossible to scale down " + formatDouble(timeSeriesValue) + " of ts " + timeSeriesName +
                    ", " + changedVariable + " " + formatDouble(sum) + " has been applied";
        }

        @Override
        public LogType getType() {
            return LogType.WARNING;
        }
    }

    /**
     * SCALING DOWN LOGS
     */

    public static class ScalingDownWarningChangeToBaseCaseVariable extends AbstractScalingDownWarning {

        private final String toVariable;

        public ScalingDownWarningChangeToBaseCaseVariable(String changedVariable, String toVariable, String timeSeriesName, double timeSeriesValue, double sum, TimeSeriesIndex index, int version, int point) {
            super(changedVariable, timeSeriesName, timeSeriesValue, sum, index, version, point);
            this.toVariable = Objects.requireNonNull(toVariable);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + "base case " + toVariable;
        }
    }

    public static class ScalingDownWarningChangeToMappedVariable extends AbstractScalingDownWarning {

        private final String toVariable;

        public ScalingDownWarningChangeToMappedVariable(String changedVariable, String toVariable, String timeSeriesName, double timeSeriesValue, double sum, TimeSeriesIndex index, int version, int point) {
            super(changedVariable, timeSeriesName, timeSeriesValue, sum, index, version, point);
            this.toVariable = Objects.requireNonNull(toVariable);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + "mapped " + toVariable;
        }
    }

    public static class ScalingDownWarningChangeToZero extends AbstractScalingDownWarning {

        public ScalingDownWarningChangeToZero(String changedVariable, String timeSeriesName, double timeSeriesValue, double sum, TimeSeriesIndex index, int version, int point) {
            super(changedVariable, timeSeriesName, timeSeriesValue, sum, index, version, point);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + 0;
        }
    }

    public static class ScalingDownWarningChangeToMappedVariableDisabled extends ScalingDownWarningChangeToMappedVariable {

        public ScalingDownWarningChangeToMappedVariableDisabled(String changedVariable, String toVariable, String timeSeriesName, double timeSeriesValue, double sum, TimeSeriesIndex index, int version, int point) {
            super(changedVariable, toVariable, timeSeriesName, timeSeriesValue, sum, index, version, point);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + IGNORE_LIMITS_DISABLED;
        }
    }

    public static class ScalingDownWarningChangeToZeroDisabled extends ScalingDownWarningChangeToZero {

        public ScalingDownWarningChangeToZeroDisabled(String changedVariable, String timeSeriesName, double timeSeriesValue, double sum, TimeSeriesIndex index, int version, int point) {
            super(changedVariable, timeSeriesName, timeSeriesValue, sum, index, version, point);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + IGNORE_LIMITS_DISABLED;
        }
    }

    public static class ScalingDownBaseCaseMinPViolatedByMappedTargetP extends AbstractRangeLog {

        public ScalingDownBaseCaseMinPViolatedByMappedTargetP(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String id, double minValue, double maxValue, double value) {
            super(version, index, point, notIncludedVariable, id, minValue, maxValue, value);
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "base case minP violated by mapped targetP";
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", but " + super.notIncludedVariable + " has not been changed";
        }

        public LogType getType() {
            return LogType.INFO;
        }
    }

    public static class ScalingDownMappedMinPViolatedByMappedTargetP extends AbstractRangeLog {

        public ScalingDownMappedMinPViolatedByMappedTargetP(int version, TimeSeriesIndex index, int point, String notIncludedVariable, String changedVariable, String id, double minValue, double maxValue, double value) {
            super(version, index, point, notIncludedVariable, id, minValue, maxValue, value);
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "mapped minP violated by mapped targetP";
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", but " + super.notIncludedVariable + " has not been changed";
        }

        public LogType getType() {
            return LogType.INFO;
        }
    }

    /**
     * SYNTHESIS LOGS
     */

    public abstract static class AbstractScalingDownModifiedSynthesis extends AbstractMappingLog {

        private final String timeSeriesName;

        protected final String changedVariable;

        public AbstractScalingDownModifiedSynthesis(String changedVariable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(index, version, Integer.MAX_VALUE);
            this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
            this.changedVariable = Objects.requireNonNull(changedVariable);
        }

        @Override
        public LogType getType() {
            return LogType.WARNING;
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "at least one " + changedVariable + " changed to ";
        }

        @Override
        public String getMessage() {
            return "Impossible to scale down at least one value of ts " + timeSeriesName +
                    ", modified " + changedVariable + " have been applied";
        }
    }

    public abstract static class AbstractScalingDownNotModifiedSynthesis extends AbstractMappingLog {

        private final String timeSeriesName;

        public AbstractScalingDownNotModifiedSynthesis(String timeSeriesName, TimeSeriesIndex index, int version) {
            super(index, version, Integer.MAX_VALUE);
            this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
        }

        @Override
        public LogType getType() {
            return LogType.INFO;
        }

        @Override
        public String getMessage() {
            return "Impossible to scale down at least one value of ts " + timeSeriesName +
                    ", but aimed targetP of equipments have been applied";
        }
    }

    public abstract static class AbstractScalingDownLimitChangeSynthesis extends AbstractMappingLog {

        private final String timeSeriesName;

        protected final String violatedVariable;

        protected final String variable;

        public AbstractScalingDownLimitChangeSynthesis(String violatedVariable, String variable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(index, version, Integer.MAX_VALUE);
            this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
            this.violatedVariable = Objects.requireNonNull(violatedVariable);
            this.variable = Objects.requireNonNull(variable);
        }

        @Override
        public LogType getType() {
            return LogType.INFO;
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "at least one " + violatedVariable;
        }

        @Override
        public String getMessage() {
            return violatedVariable + " violated by " + variable + " in scaling down of at least one value of ts " + timeSeriesName +
                    ", " + violatedVariable;
        }
    }

    public static class ScalingDownMaxPChangeSynthesis extends AbstractScalingDownLimitChangeSynthesis {

        public ScalingDownMaxPChangeSynthesis(String violatedVariable, String variable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(violatedVariable, variable, timeSeriesName, index, version);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + " increased" + TS_SYNTHESIS;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + " has been increased for equipments";
        }
    }

    public static class ScalingDownMinPChangeSynthesis extends AbstractScalingDownLimitChangeSynthesis {

        public ScalingDownMinPChangeSynthesis(String violatedVariable, String variable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(violatedVariable, variable, timeSeriesName, index, version);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + " decreased" + TS_SYNTHESIS;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + " has been decreased for equipments";
        }
    }

    public static class ScalingDownWarningChangeToBaseCaseVariableSynthesis extends AbstractScalingDownModifiedSynthesis {

        private final String toVariable;

        public ScalingDownWarningChangeToBaseCaseVariableSynthesis(String changedVariable, String toVariable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(changedVariable, timeSeriesName, index, version);
            this.toVariable = Objects.requireNonNull(toVariable);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + "base case " + toVariable + TS_SYNTHESIS;
        }
    }

    public static class ScalingDownWarningChangeToMappedVariableSynthesis extends AbstractScalingDownModifiedSynthesis {

        private final String toVariable;

        public ScalingDownWarningChangeToMappedVariableSynthesis(String changedVariable, String toVariable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(changedVariable, timeSeriesName, index, version);
            this.toVariable = Objects.requireNonNull(toVariable);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + "mapped " + toVariable + TS_SYNTHESIS;
        }
    }

    public static class ScalingDownWarningChangeToZeroSynthesis extends AbstractScalingDownModifiedSynthesis {

        public ScalingDownWarningChangeToZeroSynthesis(String changedVariable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(changedVariable, timeSeriesName, index, version);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + "0" + TS_SYNTHESIS;
        }
    }

    public static class ScalingDownWarningChangeToMappedVariableDisabledSynthesis extends ScalingDownWarningChangeToMappedVariableSynthesis {

        public ScalingDownWarningChangeToMappedVariableDisabledSynthesis(String changedVariable, String toVariable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(changedVariable, toVariable, timeSeriesName, index, version);
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "at least one " + super.changedVariable + " changed to mapped " + super.toVariable + IGNORE_LIMITS_DISABLED + TS_SYNTHESIS;
        }
    }

    public static class ScalingDownWarningChangeToZeroDisabledSynthesis extends ScalingDownWarningChangeToZeroSynthesis {

        public ScalingDownWarningChangeToZeroDisabledSynthesis(String changedVariable, String timeSeriesName, TimeSeriesIndex index, int version) {
            super(changedVariable, timeSeriesName, index, version);
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "at least one " + super.changedVariable + " changed to " + "0" + IGNORE_LIMITS_DISABLED + TS_SYNTHESIS;
        }
    }

    public static class ScalingDownBaseCaseMinPViolatedByMappedTargetPSynthesis extends AbstractScalingDownNotModifiedSynthesis {

        public ScalingDownBaseCaseMinPViolatedByMappedTargetPSynthesis(String timeSeriesName, TimeSeriesIndex index, int version) {
            super(timeSeriesName, index, version);
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "base case minP violated by mapped targetP" + TS_SYNTHESIS;
        }
    }

    public static class ScalingDownMappedMinPViolatedByMappedTargetPSynthesis extends AbstractScalingDownNotModifiedSynthesis {

        public ScalingDownMappedMinPViolatedByMappedTargetPSynthesis(String timeSeriesName, TimeSeriesIndex index, int version) {
            super(timeSeriesName, index, version);
        }

        @Override
        public String getLabel() {
            return SCALING_DOWN_PROBLEM + "mapped minP violated by mapped targetP" + TS_SYNTHESIS;
        }
    }

    /**
     * OTHER LOGS
     */

    public static class EmptyFilterWarning extends AbstractMappingLog {

        private final String timeSeriesName;

        private final double timeSeriesValue;

        public EmptyFilterWarning(String timeSeriesName, double timeSeriesValue, TimeSeriesIndex index, int version, int point) {
            super(index, version, point);
            this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
            this.timeSeriesValue = timeSeriesValue;
        }

        @Override
        public LogType getType() {
            return LogType.WARNING;
        }

        @Override
        public String getLabel() {
            return "empty filter error";
        }

        @Override
        public String getMessage() {
            return "Impossible to scale down " + formatDouble(timeSeriesValue) + " of ts '" + timeSeriesName +
                    " to empty equipment list";
        }
    }

    public static class ZeroDistributionKeyInfo extends AbstractMappingLog {

        private final String timeSeriesName;

        private final double timeSeriesValue;

        private final List<String> equipmentIds;

        public ZeroDistributionKeyInfo(String timeSeriesName, double timeSeriesValue, TimeSeriesIndex index, int version, int point,
                                          List<String> equipmentIds) {
            super(index, version, point);
            this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
            this.timeSeriesValue = timeSeriesValue;
            this.equipmentIds = Objects.requireNonNull(equipmentIds);
        }

        @Override
        public LogType getType() {
            return LogType.INFO;
        }

        @Override
        public String getLabel() {
            return "zero distribution key warning";
        }

        @Override
        public String getMessage() {
            return "Distribution key are all equal to zero in scaling down " + formatDouble(timeSeriesValue) + " of ts '" + timeSeriesName +
                    " on equipments " + equipmentIds + " -> uniform distribution";
        }
    }

    public abstract static class AbstractLimitInfo extends AbstractMappingLog {

        private final String id;

        private final String variableToChange;

        private final String variable;

        private final int nbViolation;

        private final double oldValue;

        private final double newValue;

        public AbstractLimitInfo(TimeSeriesIndex index, int version, int point, String id, String variableToChange, String variable, int nbViolation, double oldValue, double newValue) {
            super(index, version, point);
            this.id = Objects.requireNonNull(id);
            this.variableToChange = Objects.requireNonNull(variableToChange);
            this.variable = Objects.requireNonNull(variable);
            this.nbViolation = nbViolation;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public LogType getType() {
            return LogType.INFO;
        }

        @Override
        public String getLabel() {
            return LIMIT_CHANGE + variableToChange;
        }

        public String getMessage(String problemDescription, String actionDescription) {
            return variableToChange + " of " + id + " " + problemDescription + " " + variable + " for " + nbViolation +
                    " variants, " + variableToChange + " " + actionDescription + " " + formatDouble(oldValue) + " to " + formatDouble(newValue);
        }
    }

    public static class LimitMinInfo extends AbstractLimitInfo {

        public LimitMinInfo(TimeSeriesIndex index, int version, int point, String id, String variableToChange, String variable, int nbViolation, double oldValue, double newValue) {
            super(index, version, point, id, variableToChange, variable, nbViolation, oldValue, newValue);
        }

        @Override
        public String getMessage() {
            return super.getMessage("higher than",  "decreased from");
        }
    }

    public static class LimitMaxInfo extends AbstractLimitInfo {

        public LimitMaxInfo(TimeSeriesIndex index, int version, int point, String id, String variableToChange, String variable, int nbViolation, double oldValue, double newValue) {
            super(index, version, point, id, variableToChange, variable, nbViolation, oldValue, newValue);
        }

        @Override
        public String getMessage() {
            return super.getMessage("lower than",  "increased from");
        }
    }

    public abstract static class AbstractLimitSign extends AbstractMappingLog {

        private final String timeSeriesName;

        private final String variable;

        private final double timeSeriesValue;

        public AbstractLimitSign(TimeSeriesIndex index, int version, int point, String timeSeriesName, String variable, double timeSeriesValue) {
            super(index, version, point);
            this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
            this.variable = Objects.requireNonNull(variable);
            this.timeSeriesValue = timeSeriesValue;
        }

        @Override
        public LogType getType() {
            return LogType.WARNING;
        }

        @Override
        public String getLabel() {
            return MAPPING_SIGN_PROBLEM;
        }

        @Override
        public String getMessage() {
            return "Impossible to map " + variable + " " + formatDouble(timeSeriesValue) + " of ts " + timeSeriesName;
        }
    }

    public static class LimitMaxSign extends AbstractLimitSign {

        public LimitMaxSign(TimeSeriesIndex index, int version, int point, String timeSeriesName, String variable, double timeSeriesValue) {
            super(index, version, point, timeSeriesName, variable, timeSeriesValue);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + "negative " + super.variable + " value";
        }
    }

    public static class LimitMinSign extends AbstractLimitSign {

        public LimitMinSign(TimeSeriesIndex index, int version, int point, String timeSeriesName, String variable, double timeSeriesValue) {
            super(index, version, point, timeSeriesName, variable, timeSeriesValue);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + "positive " + super.variable + " value";
        }
    }

    private final List<Log> logs = new ArrayList<>();

    public TimeSeriesMappingLogger() {
    }

    public void addLog(Log log) {
        logs.add(log);
    }

    public void printLogSynthesis() {
        Map<String, AtomicInteger> labelCount = new HashMap<>();
        for (Log log : logs) {
            AtomicInteger count = labelCount.computeIfAbsent(log.getLabel(), k -> new AtomicInteger(0));
            count.incrementAndGet();
        }
        labelCount.forEach((label, count) -> LOGGER.error("{} {}", count, label));
    }

    public void writeJson(Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeJson(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeJson(Writer writer) {
        ObjectMapper mapper = JsonUtil.createObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, logs);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeCsv(Path file) {
        writeCsv(file, CSV_SEPARATOR);
    }

    private void writeCsv(Path file, char separator) {
        writeCsv(file, separator, ZoneId.systemDefault());
    }

    public void writeCsv(Path file, ZoneId zoneId) {
        writeCsv(file, CSV_SEPARATOR, zoneId);
    }

    private void writeCsv(Path file, char separator, ZoneId zoneId) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeCsv(writer, separator, zoneId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeCsv(BufferedWriter writer) {
        writeCsv(writer, CSV_SEPARATOR);
    }

    private void writeCsv(BufferedWriter writer, char separator) {
        writeCsv(writer, separator, ZoneId.systemDefault());
    }

    public void writeCsv(BufferedWriter writer, ZoneId zoneId) {
        writeCsv(writer, CSV_SEPARATOR, zoneId);
    }

    private void writeCsv(BufferedWriter writer, char separator, ZoneId zoneId) {
        try {
            TimeSeriesLoggerConfig config = new TimeSeriesLoggerConfig(separator, DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId));
            writer.write("Type");
            writer.write(config.separator);
            writer.write("Label");
            writer.write(config.separator);
            writer.write("Time");
            writer.write(config.separator);
            writer.write("Variant");
            writer.write(config.separator);
            writer.write("Version");
            writer.write(config.separator);
            writer.write("Message");
            writer.newLine();
            for (Log log : logs) {
                int point = log.getPoint();
                String pointLabel = "";
                String dateLabel = "";
                if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
                    pointLabel = "all";
                } else if (point != Integer.MAX_VALUE) {
                    pointLabel = Integer.toString(point + 1);
                    ZonedDateTime dateTime = ZonedDateTime.ofInstant(log.getIndex().getInstantAt(point), zoneId);
                    dateLabel = dateTime.format(config.dateTimeFormatter);
                }
                writer.write(log.getType().name());
                writer.write(config.separator);
                writer.write(log.getLabel());
                writer.write(config.separator);
                writer.write(dateLabel);
                writer.write(config.separator);
                writer.write(pointLabel);
                writer.write(config.separator);
                writer.write(Integer.toString(log.getVersion()));
                writer.write(config.separator);
                writer.write(log.getMessage());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
