/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.integration.remedials;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Remedial {

    private final int lineFile;
    private final String contingency;
    private final List<String> constraint;
    private final List<String> branchToOpen;
    private final List<String> branchToClose;
    private final String actions;

    public Remedial(int lineFile, String contingency, List<String> constraint, List<String> branchToOpen, List<String> branchToClose, String actions) {
        this.lineFile = lineFile;
        this.contingency = contingency;
        this.constraint = constraint;
        this.branchToOpen = branchToOpen;
        this.branchToClose = branchToClose;
        this.actions = actions;
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

    public String getActions() {
        return actions;
    }

    public final String getNameFromActions() {
        List<String> name = new ArrayList<>(); //display branchToOpen ordered then branchToClose ordered
        name.addAll(branchToOpen.stream().sorted().collect(Collectors.toList()));
        name.addAll(branchToClose.stream().map(action -> "+" + action).sorted().collect(Collectors.toList()));
        return String.join(";", name);
    }
}
