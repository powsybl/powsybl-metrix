/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Identifiable
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore
import com.powsybl.timeseries.StringTimeSeries
import groovy.transform.CompileStatic
import org.apache.commons.lang3.StringUtils

import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigTableLoader.plannedOutagesEquipmentTsName

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class PlannedOutagesData {

    @CompileStatic
    protected static void mapPlannedOutages(Binding binding, ReadOnlyTimeSeriesStore store, TimeSeriesMappingConfigLoader configLoader, Closure closure,
                                            Iterable<FilteringContext> transformersFilteringContext, Iterable<FilteringContext> linesFilteringContext, Iterable<FilteringContext> generatorsFilteringContext, Set<Integer> versions) {
        Object value = closure.call()
        if (!value instanceof String) {
            throw new TimeSeriesMappingException("Closure plannedOutages must return a time series name")
        }

        String timeSeriesName = String.valueOf(value)
        configLoader.timeSeriesExists(timeSeriesName)

        Set<String> disconnectedIds = new HashSet<>()
        for (int version : versions) {
            StringTimeSeries plannedOutagesTimeSeries = store.getStringTimeSeries(timeSeriesName, version).orElseThrow({ new TimeSeriesMappingException("Invalid planned outages time series name " + timeSeriesName) })
            String[] array = plannedOutagesTimeSeries.toArray()
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    continue
                }
                String[] ids = array[i].split(",")
                disconnectedIds.addAll(ids)
            }
        }
        disconnectedIds.remove(StringUtils.EMPTY)
        binding.setVariable("disconnectedIds", disconnectedIds)

        // add time series to the config
        configLoader.addPlannedOutages(timeSeriesName, disconnectedIds)

        // evaluate equipment filters
        Collection<Identifiable> filteredTransformers = Filter.evaluate(binding, transformersFilteringContext, MappableEquipmentType.TRANSFORMER.scriptVariable,
                { e -> return disconnectedIds.contains(((Identifiable) binding.getVariable(MappableEquipmentType.TRANSFORMER.getScriptVariable())).id) })
        Collection<Identifiable> filteredLines = Filter.evaluate(binding, linesFilteringContext, MappableEquipmentType.LINE.scriptVariable,
                { e -> return disconnectedIds.contains(((Identifiable) binding.getVariable(MappableEquipmentType.LINE.getScriptVariable())).id) })
        Collection<Identifiable> filteredGenerators = Filter.evaluate(binding, generatorsFilteringContext, MappableEquipmentType.GENERATOR.scriptVariable,
                { e -> return disconnectedIds.contains(((Identifiable) binding.getVariable(MappableEquipmentType.GENERATOR.getScriptVariable())).id) })

        // for each filtered equipment, add it to the config
        for (Identifiable identifiable in filteredTransformers) {
            configLoader.addEquipmentMapping(MappableEquipmentType.TRANSFORMER, plannedOutagesEquipmentTsName(timeSeriesName, identifiable.id), identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.disconnected)
        }
        for (Identifiable identifiable in filteredLines) {
            configLoader.addEquipmentMapping(MappableEquipmentType.LINE, plannedOutagesEquipmentTsName(timeSeriesName, identifiable.id), identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.disconnected)
        }
        for (Identifiable identifiable in filteredGenerators) {
            configLoader.addEquipmentMapping(MappableEquipmentType.GENERATOR, plannedOutagesEquipmentTsName(timeSeriesName, identifiable.id), identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.disconnected)
        }
    }
}
