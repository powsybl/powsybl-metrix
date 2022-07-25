/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.tools;

import com.powsybl.tools.AbstractToolTest;
import com.powsybl.tools.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class MetrixToolTest extends AbstractToolTest {

    private MetrixTool tool;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        tool = new MetrixTool();
        super.setUp();
    }

    @Override
    protected Iterable<Tool> getTools() {
        return Collections.singletonList(tool);
    }

    @Override
    public void assertCommand() {
        assertCommand(tool.getCommand(), "metrix", 15, 4);
        assertOption(tool.getCommand().getOptions(), "case-file", true, true);
    }

    @Test
    void run() {

    }
}
