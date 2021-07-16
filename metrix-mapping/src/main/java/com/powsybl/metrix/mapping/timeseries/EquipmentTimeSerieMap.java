package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.mapping.*;
import com.powsybl.timeseries.TimeSeriesTable;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.indexMappingKey;

public class EquipmentTimeSerieMap {
    private Map<IndexedMappingKey, List<MappedEquipment>> equimentTimeSeries = new LinkedHashMap<>();

    public void init(EquipmentTimeSerieMap map) {
        equimentTimeSeries.putAll(map.equimentTimeSeries);
    }

    public void convertToEquipmentTimeSeriesMap(Map<MappingKey, List<String>> timeSerieMap, TimeSeriesTable table, Network network, TimeSeriesMappingConfig config) {
        equimentTimeSeries.clear();
        timeSerieMap.entrySet().stream().forEach(timeSeries -> equimentTimeSeries
                .put(indexMappingKey(table, timeSeries.getKey()), mapEquipments(timeSeries.getKey(), timeSeries.getValue(), network, config)));
    }

    public List<MappedEquipment> mapEquipments(MappingKey key, List<String> equipmentIds, Network network, TimeSeriesMappingConfig config) {
        return equipmentIds.stream().map(equipmentId -> {
            Identifiable<?> identifiable = getIdentifiable(network, equipmentId);
            DistributionKey distributionKey = config.getDistributionKey(new MappingKey(key.getMappingVariable(), equipmentId));
            return new MappedEquipment(identifiable, distributionKey);
        }).collect(Collectors.toList());
    }

    private static Identifiable<?> getIdentifiable(Network network, String equipmentId) {
        Identifiable<?> identifiable = network.getIdentifiable(equipmentId);
        // check equipment exists
        if (identifiable == null) {
            throw new TimeSeriesMappingException("'" + equipmentId + "' not found");
        }
        return identifiable;
    }

    public void addMappedEquipmentTimeSeries(IndexedMappingKey indexedMappingKey,
                                            List<MappedEquipment> mappedEquipmentsTimeSeries) {
        equimentTimeSeries.put(indexedMappingKey, mappedEquipmentsTimeSeries);
    }

    public Map<IndexedMappingKey, List<MappedEquipment>> getEquipmentTimeSeries() {
        return equimentTimeSeries;
    }

    public boolean isEmpty() {
        return equimentTimeSeries.isEmpty();
    }

    public void computeIfAbsent(IndexedMappingKey indexedMappingKey, MappedEquipment mappedEquipment) {
        equimentTimeSeries.computeIfAbsent(indexedMappingKey, i -> new ArrayList<>()).add(mappedEquipment);
    }
}
