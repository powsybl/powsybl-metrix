package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.timeseries.ast.DoubleNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;

public class ContingencyPreventiveContext implements ContingencyContext {
    @Override
    public String postfix() {
        return "";
    }

    @Override
    public NodeCalc probability() {
        return DoubleNodeCalc.ONE;
    }
}
