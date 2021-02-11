/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.contingency.Probability;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class ContingencyTest {
    private FileSystem fileSystem;

    private Path dslFile;

    private Network network;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        dslFile = fileSystem.getPath("/test.dsl");
        network = NetworkXml.read(ContingencyTest.class.getResourceAsStream("/simpleNetwork.xml"));
    }

    @Test
    public void testProbability() {
        Probability a = new Probability(1.2d, null);
        Probability b = new Probability(1.2d, null);
        Probability c = new Probability(1.2d, "somets");
        Probability d = new Probability(1.2d, "somets");
        Probability e = new Probability(1.2d, "otherts");
        Probability f = new Probability(1.3d, "otherts");
        Probability g = new Probability(null, "otherts");
        Probability h = new Probability(null, "otherts");

        assertThat(a).isEqualTo(b);
        assertThat(c).isEqualTo(d);
        assertThat(g).isEqualTo(h);

        assertThat(a).isNotEqualTo(c);
        assertThat(c).isNotEqualTo(e);
        assertThat(e).isNotEqualTo(f);
    }
}
