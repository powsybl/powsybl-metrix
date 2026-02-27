package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.timeseries.ast.NodeCalc;

public interface ContingencyContext {
    String postfix();

    NodeCalc probability();
}
