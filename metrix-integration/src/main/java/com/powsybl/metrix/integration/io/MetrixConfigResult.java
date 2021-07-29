package com.powsybl.metrix.integration.io;

import com.powsybl.timeseries.ast.NodeCalc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MetrixConfigResult {

    private final Map<String, NodeCalc> mappingTimeSeriesNodes = new HashMap<>();

    private final Map<String, NodeCalc> metrixTimeSeriesNodes = new HashMap<>();

    public Map<String, NodeCalc> getMappingTimeSeriesNodes() {
        return mappingTimeSeriesNodes;
    }

    public Map<String, NodeCalc> getMetrixTimeSeriesNodes() {
        return metrixTimeSeriesNodes;
    }

    public MetrixConfigResult(Map<String, NodeCalc> timeSeriesNodesAfterMapping, Map<String, NodeCalc> timeSeriesNodesAfterMetrix) {
        Objects.requireNonNull(timeSeriesNodesAfterMapping);
        Objects.requireNonNull(timeSeriesNodesAfterMetrix);
        List<String> overloadedTimeSeriesNames = timeSeriesNodesAfterMapping.keySet().stream()
                .filter(timeSeriesNodesAfterMetrix::containsKey)
                .filter(e -> timeSeriesNodesAfterMapping.get(e) != timeSeriesNodesAfterMetrix.get(e))
                .collect(Collectors.toList());
        // Remove from mapping nodes time series defined in mapping script and in metrix configuration script (same name but different formula)
        // -> metrix one overloads mapping one
        timeSeriesNodesAfterMapping.keySet().removeAll(overloadedTimeSeriesNames);
        this.mappingTimeSeriesNodes.putAll(timeSeriesNodesAfterMapping);
        // Remove from metrix nodes all mapping nodes
        timeSeriesNodesAfterMetrix.keySet().removeAll(mappingTimeSeriesNodes.keySet());
        this.metrixTimeSeriesNodes.putAll(timeSeriesNodesAfterMetrix);
    }
}
