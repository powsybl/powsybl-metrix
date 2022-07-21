/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.integration.remedials;

import java.util.List;

public class Remedial {

    private final int lineFile;
    private final String contingency;
    private final List<String> constraint;
    private final List<String> branchToOpen;
    private final List<String> branchToClose;

    public Remedial(int lineFile, String contingency, List<String> constraint, List<String> branchToOpen, List<String> branchToClose) {
        this.lineFile = lineFile;
        this.contingency = contingency;
        this.constraint = constraint;
        this.branchToOpen = branchToOpen;
        this.branchToClose = branchToClose;
    }

    public int getLineFile() {
        return lineFile;
    }

    public String getContingency() {
        return contingency;
    }

    public List<String> getConstraint() {
        return constraint;
    }

    public List<String> getBranchToOpen() {
        return branchToOpen;
    }

    public List<String> getBranchToClose() {
        return branchToClose;
    }

}
