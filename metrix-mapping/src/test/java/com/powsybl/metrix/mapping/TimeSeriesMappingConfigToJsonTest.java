/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.powsybl.timeseries.ast.IntegerNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class TimeSeriesMappingConfigToJsonTest {

    private TimeSeriesMappingConfig config = new TimeSeriesMappingConfig();

    private Map<MappingKey, List<String>> timeSeriesToGenerators = ImmutableMap.of(new MappingKey(EquipmentVariable.targetP, "tsG"), ImmutableList.of("g1"));
    private Map<MappingKey, List<String>> timeSeriesToLoads = ImmutableMap.of(new MappingKey(EquipmentVariable.variableActivePower, "tsL"), ImmutableList.of("l1", "l2"));
    private Map<MappingKey, List<String>> timeSeriesToDanglingLines = ImmutableMap.of(new MappingKey(EquipmentVariable.p0, "tsDL"), ImmutableList.of("dl1"));
    private Map<MappingKey, List<String>> timeSeriesToHvdcLines = ImmutableMap.of(new MappingKey(EquipmentVariable.activePowerSetpoint, "tsH"), ImmutableList.of("h1", "h2"));
    private Map<MappingKey, List<String>> timeSeriesToPhaseTapChangers = ImmutableMap.of(new MappingKey(EquipmentVariable.phaseTapPosition, "tsP"), ImmutableList.of("p1", "p2", "p3"));
    private Map<MappingKey, List<String>> timeSeriesToBreakers = ImmutableMap.of(new MappingKey(EquipmentVariable.open, "tsB"), ImmutableList.of("b1"));
    private Map<MappingKey, List<String>> timeSeriesToTransformers = ImmutableMap.of(new MappingKey(EquipmentVariable.ratedU1, "tsT"), ImmutableList.of("t1", "t2"));
    private Map<MappingKey, List<String>> timeSeriesToRatioTapChangers = ImmutableMap.of(new MappingKey(EquipmentVariable.ratioTapPosition, "tsTC"), ImmutableList.of("tc1", "tc2"));
    private Map<MappingKey, List<String>> timeSeriesToLccConverterStations = ImmutableMap.of(new MappingKey(EquipmentVariable.powerFactor, "tsLcc"), ImmutableList.of("lcc1", "lcc2"));
    private Map<MappingKey, List<String>> timeSeriesToVscConverterStations = ImmutableMap.of(new MappingKey(EquipmentVariable.reactivePowerSetpoint, "tsVsc"), ImmutableList.of("vsc1", "vsc2"));

    private Map<MappingKey, List<String>> generatorToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.targetP, "g1"), ImmutableList.of("tsG"));
    private Map<MappingKey, List<String>> loadToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.fixedActivePower, "l1"), ImmutableList.of("tsL1", "tsL2"));
    private Map<MappingKey, List<String>> danglingLineToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.p0, "dl1"), ImmutableList.of("tsDL"));
    private Map<MappingKey, List<String>> hvdcLineToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.activePowerSetpoint, "h1"), ImmutableList.of("tsH"));
    private Map<MappingKey, List<String>> phaseTapChangerToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.phaseTapPosition, "p1"), ImmutableList.of("tsP"));
    private Map<MappingKey, List<String>> breakerToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.open, "b1"), ImmutableList.of("tsB"));
    private Map<MappingKey, List<String>> transformerToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.ratedU1, "h1"), ImmutableList.of("tsT"));
    private Map<MappingKey, List<String>> ratioTapChangerToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.ratioTapPosition, "tc1"), ImmutableList.of("tsTC"));
    private Map<MappingKey, List<String>> lccConverterStationToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.powerFactor, "lcc1"), ImmutableList.of("tsLcc"));
    private Map<MappingKey, List<String>> vscConverterStationToTimeSeries = ImmutableMap.of(new MappingKey(EquipmentVariable.reactivePowerSetpoint, "vsc1"), ImmutableList.of("tsVsc"));

    private Set<MappingKey> generatorTs = ImmutableSet.of(new MappingKey(EquipmentVariable.targetP, "g1"), new MappingKey(EquipmentVariable.minP, "g2"));
    private Set<MappingKey> loadTs = ImmutableSet.of(new MappingKey(EquipmentVariable.fixedActivePower, "l1"), new MappingKey(EquipmentVariable.variableActivePower, "l2"));
    private Set<MappingKey> danglingLineTs = ImmutableSet.of(new MappingKey(EquipmentVariable.p0, "dl"));
    private Set<MappingKey> hvdcLineTs = ImmutableSet.of(new MappingKey(EquipmentVariable.minP, "h1"), new MappingKey(EquipmentVariable.maxP, "h2"));
    private Set<MappingKey> phaseTapChangerTs = ImmutableSet.of(new MappingKey(EquipmentVariable.phaseTapPosition, "p"));
    private Set<MappingKey> breakerTs = ImmutableSet.of(new MappingKey(EquipmentVariable.open, "b"));
    private Set<MappingKey> transformerTs = ImmutableSet.of(new MappingKey(EquipmentVariable.ratedU1, "t1"), new MappingKey(EquipmentVariable.ratedU2, "t2"));
    private Set<MappingKey> ratioTapChangerTs = ImmutableSet.of(new MappingKey(EquipmentVariable.ratioTapPosition, "tc1"), new MappingKey(EquipmentVariable.loadTapChangingCapabilities, "tc2"));
    private Set<MappingKey> lccConverterStationTs = ImmutableSet.of(new MappingKey(EquipmentVariable.powerFactor, "lcc1"));
    private Set<MappingKey> vscConverterStationTs = ImmutableSet.of(new MappingKey(EquipmentVariable.voltageRegulatorOn, "vsc1"), new MappingKey(EquipmentVariable.voltageSetpoint, "vsc2"));

    private Set<String> unmappedGenerators = ImmutableSet.of("ug1");
    private Set<String> unmappedLoads = ImmutableSet.of("ul1", "ul2");
    private Set<String> unmappedFixedActivePowerLoads = ImmutableSet.of("uf1");
    private Set<String> unmappedVariableActivePowerLoads = ImmutableSet.of("uv1", "uv2");
    private Set<String> unmappedDanglingLines = ImmutableSet.of("udl1");
    private Set<String> unmappedHvdcLines = ImmutableSet.of("uh1");
    private Set<String> unmappedPhaseTapChangers = ImmutableSet.of("upst1");

    private Set<String> unmappedMinPGenerators = ImmutableSet.of("ugmin");
    private Set<String> unmappedMaxPGenerators = ImmutableSet.of("ugmax");
    private Set<String> unmappedMinPHvdcLines = ImmutableSet.of("uhmin");
    private Set<String> unmappedMaxPHvdcLines = ImmutableSet.of("uhmax");

    private Set<String> ignoredUnmappedGenerators = ImmutableSet.of("iug1");
    private Set<String> ignoredUnmappedLoads = ImmutableSet.of("iul1", "iul2");
    private Set<String> ignoredUnmappedDanglingLines = ImmutableSet.of("iudl1");
    private Set<String> ignoredUnmappedHvdcLines = ImmutableSet.of("iuh1");
    private Set<String> ignoredUnmappedPhaseTapChangers = ImmutableSet.of("iup1");

    private Set<String> disconnectedGenerators = ImmutableSet.of("dg1");
    private Set<String> disconnectedLoads = ImmutableSet.of("dl1", "dl2");
    private Set<String> disconnectedDanglingLines = ImmutableSet.of("ddl1", "ddl2");

    private Set<String> outOfMainCcGenerators = ImmutableSet.of("og1");
    private Set<String> outOfMainCcLoads = ImmutableSet.of("ol1", "ol2");
    private Set<String> outOfMainCcDanglingLines = ImmutableSet.of("odl1");

    private Map<String, NodeCalc> timeSeriesNodes = ImmutableMap.of("ts", new IntegerNodeCalc(10));
    private Map<MappingKey, DistributionKey> distributionKeys = ImmutableMap.of(new MappingKey(EquipmentVariable.targetP, "id"), new NumberDistributionKey(2.0));
    private Set<String> mappedTimeSeriesNames = ImmutableSet.of("ts1", "ts2");
    private Set<String> ignoreLimitsTimeSeriesNames = ImmutableSet.of("tsIL1", "tsIL2");

    @Before
    public void setUp() throws Exception {
        config.setTimeSeriesToGeneratorsMapping(timeSeriesToGenerators);
        config.setTimeSeriesToLoadsMapping(timeSeriesToLoads);
        config.setTimeSeriesToDanglingLinesMapping(timeSeriesToDanglingLines);
        config.setTimeSeriesToHvdcLinesMapping(timeSeriesToHvdcLines);
        config.setTimeSeriesToPhaseTapChangersMapping(timeSeriesToPhaseTapChangers);
        config.setTimeSeriesToBreakersMapping(timeSeriesToBreakers);
        config.setTimeSeriesToTransformersMapping(timeSeriesToTransformers);
        config.setTimeSeriesToRatioTapChangersMapping(timeSeriesToRatioTapChangers);
        config.setTimeSeriesToLccConverterStationsMapping(timeSeriesToLccConverterStations);
        config.setTimeSeriesToVscConverterStationsMapping(timeSeriesToVscConverterStations);

        config.setGeneratorToTimeSeriesMapping(generatorToTimeSeries);
        config.setLoadToTimeSeriesMapping(loadToTimeSeries);
        config.setDanglingLineToTimeSeriesMapping(danglingLineToTimeSeries);
        config.setHvdcLineToTimeSeriesMapping(hvdcLineToTimeSeries);
        config.setPhaseTapChangerToTimeSeriesMapping(phaseTapChangerToTimeSeries);
        config.setBreakerToTimeSeriesMapping(breakerToTimeSeries);
        config.setTransformerToTimeSeriesMapping(transformerToTimeSeries);
        config.setRatioTapChangerToTimeSeriesMapping(ratioTapChangerToTimeSeries);
        config.setLccConverterStationToTimeSeriesMapping(lccConverterStationToTimeSeries);
        config.setVscConverterStationToTimeSeriesMapping(vscConverterStationToTimeSeries);

        config.setGeneratorTimeSeries(generatorTs);
        config.setLoadTimeSeries(loadTs);
        config.setDanglingLineTimeSeries(danglingLineTs);
        config.setHvdcLineTimeSeries(hvdcLineTs);
        config.setPhaseTapChangerTimeSeries(phaseTapChangerTs);
        config.setBreakerTimeSeries(breakerTs);
        config.setTransformerTimeSeries(transformerTs);
        config.setRatioTapChangerTimeSeries(ratioTapChangerTs);
        config.setLccConverterStationTimeSeries(lccConverterStationTs);
        config.setVscConverterStationTimeSeries(vscConverterStationTs);

        config.setUnmappedGenerators(unmappedGenerators);
        config.setUnmappedLoads(unmappedLoads);
        config.setUnmappedFixedActivePowerLoads(unmappedFixedActivePowerLoads);
        config.setUnmappedVariableActivePowerLoads(unmappedVariableActivePowerLoads);
        config.setUnmappedDanglingLines(unmappedDanglingLines);
        config.setUnmappedHvdcLines(unmappedHvdcLines);
        config.setUnmappedPhaseTapChangers(unmappedPhaseTapChangers);

        config.setUnmappedMinPGenerators(unmappedMinPGenerators);
        config.setUnmappedMaxPGenerators(unmappedMaxPGenerators);
        config.setUnmappedMinPHvdcLines(unmappedMinPHvdcLines);
        config.setUnmappedMaxPHvdcLines(unmappedMaxPHvdcLines);

        config.setIgnoredUnmappedGenerators(ignoredUnmappedGenerators);
        config.setIgnoredUnmappedLoads(ignoredUnmappedLoads);
        config.setIgnoredUnmappedDanglingLines(ignoredUnmappedDanglingLines);
        config.setIgnoredUnmappedHvdcLines(ignoredUnmappedHvdcLines);
        config.setIgnoredUnmappedPhaseTapChangers(ignoredUnmappedPhaseTapChangers);

        config.setDisconnectedGenerators(disconnectedGenerators);
        config.setDisconnectedLoads(disconnectedLoads);
        config.setDisconnectedDanglingLines(disconnectedDanglingLines);

        config.setOutOfMainCcGenerators(outOfMainCcGenerators);
        config.setOutOfMainCcLoads(outOfMainCcLoads);
        config.setOutOfMainCcDanglingLines(outOfMainCcDanglingLines);

        config.setTimeSeriesNodes(timeSeriesNodes);
        config.setDistributionKeys(distributionKeys);
        config.setMappedTimeSeriesNames(mappedTimeSeriesNames);
        config.setIgnoreLimitsTimeSeriesNames(ignoreLimitsTimeSeriesNames);
    }

    @Test
    public void testGet() {
        assertEquals(timeSeriesToGenerators, config.getTimeSeriesToGeneratorsMapping());
        assertEquals(timeSeriesToLoads, config.getTimeSeriesToLoadsMapping());
        assertEquals(timeSeriesToDanglingLines, config.getTimeSeriesToDanglingLinesMapping());
        assertEquals(timeSeriesToHvdcLines, config.getTimeSeriesToHvdcLinesMapping());
        assertEquals(timeSeriesToPhaseTapChangers, config.getTimeSeriesToPhaseTapChangersMapping());
        assertEquals(timeSeriesToBreakers, config.getTimeSeriesToBreakersMapping());
        assertEquals(timeSeriesToTransformers, config.getTimeSeriesToTransformersMapping());
        assertEquals(timeSeriesToRatioTapChangers, config.getTimeSeriesToRatioTapChangersMapping());
        assertEquals(timeSeriesToLccConverterStations, config.getTimeSeriesToLccConverterStationsMapping());
        assertEquals(timeSeriesToVscConverterStations, config.getTimeSeriesToVscConverterStationsMapping());

        assertEquals(generatorToTimeSeries, config.getGeneratorToTimeSeriesMapping());
        assertEquals(loadToTimeSeries, config.getLoadToTimeSeriesMapping());
        assertEquals(danglingLineToTimeSeries, config.getDanglingLineToTimeSeriesMapping());
        assertEquals(hvdcLineToTimeSeries, config.getHvdcLineToTimeSeriesMapping());
        assertEquals(phaseTapChangerToTimeSeries, config.getPhaseTapChangerToTimeSeriesMapping());
        assertEquals(breakerToTimeSeries, config.getBreakerToTimeSeriesMapping());
        assertEquals(transformerToTimeSeries, config.getTransformerToTimeSeriesMapping());
        assertEquals(ratioTapChangerToTimeSeries, config.getRatioTapChangerToTimeSeriesMapping());
        assertEquals(lccConverterStationToTimeSeries, config.getLccConverterStationToTimeSeriesMapping());
        assertEquals(vscConverterStationToTimeSeries, config.getVscConverterStationToTimeSeriesMapping());

        assertEquals(generatorTs, config.getGeneratorTimeSeries());
        assertEquals(loadTs, config.getLoadTimeSeries());
        assertEquals(danglingLineTs, config.getDanglingLineTimeSeries());
        assertEquals(hvdcLineTs, config.getHvdcLineTimeSeries());
        assertEquals(phaseTapChangerTs, config.getPhaseTapChangerTimeSeries());
        assertEquals(breakerTs, config.getBreakerTimeSeries());
        assertEquals(transformerTs, config.getTransformerTimeSeries());
        assertEquals(ratioTapChangerTs, config.getRatioTapChangerTimeSeries());
        assertEquals(lccConverterStationTs, config.getLccConverterStationTimeSeries());
        assertEquals(vscConverterStationTs, config.getVscConverterStationTimeSeries());

        assertEquals(unmappedGenerators, config.getUnmappedGenerators());
        assertEquals(unmappedLoads, config.getUnmappedLoads());
        assertEquals(unmappedFixedActivePowerLoads, config.getUnmappedFixedActivePowerLoads());
        assertEquals(unmappedVariableActivePowerLoads, config.getUnmappedVariableActivePowerLoads());
        assertEquals(unmappedDanglingLines, config.getUnmappedDanglingLines());
        assertEquals(unmappedHvdcLines, config.getUnmappedHvdcLines());
        assertEquals(unmappedPhaseTapChangers, config.getUnmappedPhaseTapChangers());

        assertEquals(unmappedMinPGenerators, config.getUnmappedMinPGenerators());
        assertEquals(unmappedMaxPGenerators, config.getUnmappedMaxPGenerators());
        assertEquals(unmappedMinPHvdcLines, config.getUnmappedMinPHvdcLines());
        assertEquals(unmappedMaxPHvdcLines, config.getUnmappedMaxPHvdcLines());

        assertEquals(ignoredUnmappedGenerators, config.getIgnoredUnmappedGenerators());
        assertEquals(ignoredUnmappedLoads, config.getIgnoredUnmappedLoads());
        assertEquals(ignoredUnmappedDanglingLines, config.getIgnoredUnmappedDanglingLines());
        assertEquals(ignoredUnmappedHvdcLines, config.getIgnoredUnmappedHvdcLines());
        assertEquals(ignoredUnmappedPhaseTapChangers, config.getIgnoredUnmappedPhaseTapChangers());

        assertEquals(disconnectedGenerators, config.getDisconnectedGenerators());
        assertEquals(disconnectedLoads, config.getDisconnectedLoads());
        assertEquals(disconnectedDanglingLines, config.getDisconnectedDanglingLines());

        assertEquals(outOfMainCcGenerators, config.getOutOfMainCcGenerators());
        assertEquals(outOfMainCcLoads, config.getOutOfMainCcLoads());
        assertEquals(outOfMainCcDanglingLines, config.getOutOfMainCcDanglingLines());

        assertEquals(timeSeriesNodes, config.getTimeSeriesNodes());
        assertEquals(distributionKeys, config.getDistributionKeys());
        assertEquals(mappedTimeSeriesNames, config.getMappedTimeSeriesNames());
        assertEquals(ignoreLimitsTimeSeriesNames, config.getIgnoreLimitsTimeSeriesNames());
    }

    @Test
    public void testJson() {
        String json = TimeSeriesMappingConfig.toJson(config);
        TimeSeriesMappingConfig config2 = TimeSeriesMappingConfig.parseJson(json);
        assertEquals(config, config2);
    }
}
