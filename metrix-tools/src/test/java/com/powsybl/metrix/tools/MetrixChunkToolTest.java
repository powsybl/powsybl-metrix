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
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class MetrixChunkToolTest extends AbstractToolTest {

    private MetrixChunkTool tool;

    @Override
    @Before
    public void setUp() throws Exception {
        tool = new MetrixChunkTool();
        super.setUp();
    }

    @Override
    protected Iterable<Tool> getTools() {
        return Collections.singletonList(tool);
    }

    @Override
    public void assertCommand() {
        assertCommand(tool.getCommand(), "metrix-chunk", 17, 3);
        assertOption(tool.getCommand().getOptions(), "case-file", true, true);
    }

    @Test
    public void run() {

    }
}
