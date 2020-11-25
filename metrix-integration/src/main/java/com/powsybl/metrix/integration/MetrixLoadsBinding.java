/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import java.util.*;

/**
 * Created by Nicolas.Lhuillier@rte-france.com on 09/11/18.
 */
public class MetrixLoadsBinding {

    private String name;
    private Set<String> loadsIds = new HashSet<>();

    MetrixLoadsBinding() {
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
        if (!(obj instanceof MetrixLoadsBinding)) {
            return false;
        }
        MetrixLoadsBinding other = (MetrixLoadsBinding) obj;
        return name.equals(other.name) && loadsIds.equals(other.loadsIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, loadsIds);
    }
}
