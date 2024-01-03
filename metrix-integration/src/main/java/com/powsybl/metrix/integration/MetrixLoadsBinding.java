/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import java.util.*;

public class MetrixLoadsBinding {

    private final String name;
    private final Set<String> loadsIds = new HashSet<>();

    MetrixLoadsBinding() {
        this.name = null;
    }

    MetrixLoadsBinding(String name, Collection<String> ids) {
        this.name = name;
        this.loadsIds.addAll(ids);
    }

    public String getName() {
        return name;
    }

    public Set<String> getLoadsIds() {
        return Collections.unmodifiableSet(loadsIds);
    }

    @Override
    public String toString() {
        return "Loads group '" + name + "' [ " + String.join(", ", loadsIds) + " ]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MetrixLoadsBinding other)) {
            return false;
        }
        return name.equals(other.name) && loadsIds.equals(other.loadsIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, loadsIds);
    }
}
