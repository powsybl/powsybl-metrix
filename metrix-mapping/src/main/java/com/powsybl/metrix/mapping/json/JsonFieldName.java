/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.json;

import java.util.Arrays;
import java.util.Objects;

public enum JsonFieldName {
    MAPPING_KEY("mappingKey"),
    MAPPING_KEYS("mappingKeys"),
    DISTRIBUTION("distributionKey"),
    MAPPING_LIST("mappingList"),
    TS_TO_GENERATORS("timeSeriesToGeneratorsMapping"),
    TS_TO_LOADS("timeSeriesToLoadsMapping"),
    TS_TO_DANGLING_LINES("timeSeriesToDanglingLinesMapping"),
    TS_TO_HVDC_LINES("timeSeriesToHvdcLinesMapping"),
    TS_TO_PHASE_TAP_CHANGERS("timeSeriesToPhaseTapChangersMapping"),
    TS_TO_BREAKERS("timeSeriesToBreakersMapping"),
    TS_TO_TRANSFORMERS("timeSeriesToTransformersMapping"),
    TS_TO_LINES("timeSeriesToLinesMapping"),
    TS_TO_RATIO_TAP_CHANGERS("timeSeriesToRatioTapChangersMapping"),
    TS_TO_LCC_CONVERTER_STATIONS("timeSeriesToLccConverterStationsMapping"),
    TS_TO_VSC_CONVERTER_STATIONS("timeSeriesToVscConverterStationsMapping"),
    GENERATOR_TO_TS("generatorToTimeSeriesMapping"),
    LOAD_TO_TS("loadToTimeSeriesMapping"),
    DANGLING_LINE_TO_TS("danglingLineToTimeSeriesMapping"),
    HVDC_LINE_TO_TS("hvdcLineToTimeSeriesMapping"),
    PHASE_TAP_CHANGER_TO_TS("phaseTapChangerToTimeSeriesMapping"),
    BREAKER_TO_TS("breakerToTimeSeriesMapping"),
    TRANSFORMER_TO_TS("transformerToTimeSeriesMapping"),
    LINE_TO_TS("lineToTimeSeriesMapping"),
    RATIO_TAP_CHANGER_TO_TS("ratioTapChangerToTimeSeriesMapping"),
    LCC_CONVERTER_STATION_TO_TS("lccConverterStationToTimeSeriesMapping"),
    VSC_CONVERTER_STATION_TO_TS("vscConverterStationToTimeSeriesMapping"),
    GENERATOR_TS("generatorTimeSeries"),
    LOAD_TS("loadTimeSeries"),
    DANGLING_LINE_TS("danglingLineTimeSeries"),
    HVDC_LINE_TS("hvdcLineTimeSeries"),
    PHASE_TAP_CHANGER_TS("phaseTapChangerTimeSeries"),
    BREAKER_TS("breakerTimeSeries"),
    TRANSFORMER_TS("transformerTimeSeries"),
    LINE_TS("lineTimeSeries"),
    RATIO_TAP_CHANGER_TS("ratioTapChangerTimeSeries"),
    LCC_CONVERTER_STATION_TS("lccConverterStationTimeSeries"),
    VSC_CONVERTER_STATION_TS("vscConverterStationTimeSeries"),
    UNMAPPED_GENERATORS("unmappedGenerators"),
    UNMAPPED_LOADS("unmappedLoads"),
    UNMAPPED_FIXED_ACTIVE_POWER_LOADS("unmappedFixedActivePowerLoads"),
    UNMAPPED_VARIABLE_ACTIVE_POWER_LOADS("unmappedVariableActivePowerLoads"),
    UNMAPPED_DANGLING_LINES("unmappedDanglingLines"),
    UNMAPPED_HVDC_LINES("unmappedHvdcLines"),
    UNMAPPED_PHASE_TAP_CHANGERS("unmappedPhaseTapChangers"),
    UNMAPPED_MIN_P_GENERATORS("unmappedMinPGenerators"),
    UNMAPPED_MAX_P_GENERATORS("unmappedMaxPGenerators"),
    UNMAPPED_MIN_P_HVDC_LINES("unmappedMinPHvdcLines"),
    UNMAPPED_MAX_P_HVDC_LINES("unmappedMaxPHvdcLines"),
    DISTRIBUTION_KEYS("distributionKeys"),
    IGNORED_UNMAPPED_GENERATORS("ignoredUnmappedGenerators"),
    IGNORED_UNMAPPED_LOADS("ignoredUnmappedLoads"),
    IGNORED_UNMAPPED_DANGLING_LINES("ignoredUnmappedDanglingLines"),
    IGNORED_UNMAPPED_HVDC_LINES("ignoredUnmappedHvdcLines"),
    IGNORED_UNMAPPED_PHASE_TAP_CHANGERS("ignoredUnmappedPhaseTapChangers"),
    DISCONNECTED_GENERATORS("disconnectedGenerators"),
    DISCONNECTED_LOADS("disconnectedLoads"),
    DISCONNECTED_DANGLING_LINES("disconnectedDanglingLines"),
    OUT_OF_MAIN_CC_GENERATORS("outOfMainCcGenerators"),
    OUT_OF_MAIN_CC_LOADS("outOfMainCcLoads"),
    OUT_OF_MAIN_CC_DANGLING_LINES("outOfMainCcDanglingLines"),
    TIME_SERIES_NODES("timeSeriesNodes"),
    TIME_SERIES_NAME("timeSeriesName"),
    TS_TO_EQUIPMENT("timeSeriesToEquipment"),
    EQUIPMENT_TO_TS("equipmentToTimeSeries"),
    MAPPED_TIME_SERIES_NAMES("mappedTimeSeriesNames"),
    IGNORE_LIMITS_TIME_SERIES_NAMES("ignoreLimitsTimeSeriesNames"),
    TS_TO_PLANNED_OUTAGES("timeSeriesToPlannedOutagesMapping"),
    OUTAGES("outages"),
    GENERATORGROUPTS("generatorGroupTimeSeries"),
    LOADGROUPTS("loadGroupTimeSeries"),
    TIMESERIESNAMES("timeSeriesNames"),
    EQUIPMENTID("id");

    private final String fieldName;

    JsonFieldName(String fieldName) {
        this.fieldName = Objects.requireNonNull(fieldName);
    }

    public String getFieldName() {
        return fieldName;
    }

    public static JsonFieldName nameOf(String value) {
        return Arrays.stream(JsonFieldName.values()).filter(name -> name.fieldName.equals(value)).findFirst().orElse(null);
    }
}
