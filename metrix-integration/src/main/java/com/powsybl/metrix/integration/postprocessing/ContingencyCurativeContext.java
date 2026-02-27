package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.timeseries.ast.NodeCalc;

public class ContingencyCurativeContext implements ContingencyContext {
    private final String contingencyId;
    private final NodeCalc probability;

    public ContingencyCurativeContext(String contingencyId, NodeCalc probability) {
        this.contingencyId = contingencyId;
        this.probability = probability;
    }

    @Override
    public String postfix() {
        return "_" + contingencyId;
    }

    @Override
    public NodeCalc probability() {
        return probability;
    }
}
