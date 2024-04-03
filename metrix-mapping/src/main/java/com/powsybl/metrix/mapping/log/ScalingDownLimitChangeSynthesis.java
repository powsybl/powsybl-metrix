/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.mapping.log;

public class ScalingDownLimitChangeSynthesis implements LogDescriptionBuilder {

    private String timeSeriesName;
    private String violatedVariable;
    private String variable;
    private String evolution;
    private String type = "";

    public ScalingDownLimitChangeSynthesis violatedVariable(String violatedVariable) {
        this.violatedVariable = violatedVariable;
        return this;
    }

    public ScalingDownLimitChangeSynthesis variable(String variable) {
        this.variable = variable;
        return this;
    }

    public ScalingDownLimitChangeSynthesis timeSeriesName(String timeSeriesName) {
        this.timeSeriesName = timeSeriesName;
        return this;
    }

    public ScalingDownLimitChangeSynthesis min() {
        this.evolution = " decreased";
        return this;
    }

    public ScalingDownLimitChangeSynthesis max() {
        this.evolution = " increased";
        return this;
    }

    public ScalingDownLimitChangeSynthesis mapped() {
        this.type = "mapped";
        return this;
    }

    public ScalingDownLimitChangeSynthesis baseCase() {
        this.type = "base case";
        return this;
    }

    public LogContent buildLimitChange() {
        LogContent log = new LogContent();
        log.label = String.format("%sat least one %s%s%s", SCALING_DOWN_PROBLEM, violatedVariable, evolution, TS_SYNTHESIS);
        log.message = String.format("%s violated by %s in scaling down of at least one value of ts %s, %s has been%s for equipments",
                violatedVariable, variable, timeSeriesName, violatedVariable, evolution);
        return log;
    }

    public LogContent buildNotModified() {
        LogContent log = new LogContent();
        log.label = String.format("%s%s minP violated by mapped targetP%s", SCALING_DOWN_PROBLEM, type, TS_SYNTHESIS);
        log.message = String.format("Impossible to scale down at least one value of ts %s, but aimed targetP of equipments have been applied",
                timeSeriesName);
        return log;
    }
}
