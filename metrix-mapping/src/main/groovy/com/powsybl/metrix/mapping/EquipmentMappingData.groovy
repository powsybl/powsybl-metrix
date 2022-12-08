/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Identifiable
import groovy.transform.CompileStatic

class EquipmentMappingData extends SimpleMappingData {

    Closure closureDistributionKey
    String timeSeriesNameDistributionKey
    Set<EquipmentVariable> variableSet = new HashSet<>()

    void distributionKey(Closure closure) {
        this.closureDistributionKey = closure
    }

    void distributionKey(String timeSeriesName) {
        this.timeSeriesNameDistributionKey = timeSeriesName
    }

    void variable(EquipmentVariable variable) {
        this.variableSet.add(variable)
    }

    void variables(EquipmentVariable[] variables) {
        this.variableSet.addAll(variables)
    }

    @CompileStatic
    protected static void mapToEquipments(Binding binding, TimeSeriesMappingConfigLoader configLoader,
                                          Closure closure, Iterable<FilteringContext> filteringContexts,
                                          MappableEquipmentType equipmentType) {
        Closure cloned = (Closure) closure.clone()
        EquipmentMappingData spec = new EquipmentMappingData()
        cloned.delegate = spec
        cloned()

        configLoader.timeSeriesExists(spec.timeSeriesName)

        // check variable
        Set<EquipmentVariable> variables = new HashSet<>()
        if (spec.variableSet.isEmpty()) {
            variables.add(EquipmentVariable.getByDefaultVariable(equipmentType))
        } else {
            variables.addAll(EquipmentVariable.check(equipmentType, spec.variableSet))
        }

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        // create at least one entry in the config even if no equipment match the filter (mandatory for ignore-empty-filter option)
        if (filteredEquipments.isEmpty()) {
            variables.forEach({ EquipmentVariable variable ->
                configLoader.addEquipmentMapping(equipmentType, spec.timeSeriesName, null, NumberDistributionKey.ONE, variable)
            })
        }

        // for each filtered equipment, compute the distribution key and add it to the config
        filteredEquipments.forEach({ Identifiable identifiable ->
            DistributionKey distributionKey
            if (spec.closureDistributionKey != null && spec.timeSeriesNameDistributionKey != null) {
                throw new TimeSeriesMappingException("Closure and time series name distribution key are exclusives")
            }
            if (spec.closureDistributionKey != null) {
                binding.setVariable(equipmentType.getScriptVariable(), identifiable)
                Object value = spec.closureDistributionKey.call()
                if (value instanceof Number) {
                    distributionKey = new NumberDistributionKey(((Number) value).doubleValue())
                } else if (value instanceof String) {
                    configLoader.timeSeriesExists(String.valueOf(value))
                    distributionKey = new TimeSeriesDistributionKey(String.valueOf(value))
                } else {
                    throw new TimeSeriesMappingException("Closure distribution key of equipment '" + identifiable.id
                            + "' must return a number or a time series name")
                }
                binding.setVariable(equipmentType.getScriptVariable(), null)
            } else if (spec.timeSeriesNameDistributionKey != null) {
                configLoader.timeSeriesExists(spec.timeSeriesNameDistributionKey)
                distributionKey = new TimeSeriesDistributionKey(spec.timeSeriesNameDistributionKey)
            } else {
                distributionKey = NumberDistributionKey.ONE
            }
            variables.forEach({ EquipmentVariable variable ->
                configLoader.addEquipmentMapping(equipmentType, spec.timeSeriesName, identifiable.id, distributionKey, variable)
            })
        })
    }
}
