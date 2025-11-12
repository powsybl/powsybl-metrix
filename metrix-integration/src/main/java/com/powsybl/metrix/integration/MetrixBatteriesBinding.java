/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MetrixBatteriesBinding {

    public enum ReferenceVariable {

        PMAX(0), // default value
        PMIN(1),
        POBJ(2),
        PMAX_MINUS_POBJ(3);

        private final int type;

        ReferenceVariable(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

    }

    private final String name;
    private final Set<String> batteriesIds = new HashSet<>();
    private final ReferenceVariable reference;

    MetrixBatteriesBinding() {
        this.name = null;
        this.reference = null;
    }

    MetrixBatteriesBinding(String name, Collection<String> ids) {
        this(name, ids, ReferenceVariable.PMAX);
    }

    MetrixBatteriesBinding(String name, Collection<String> ids, ReferenceVariable reference) {
        this.name = name;
        this.batteriesIds.addAll(ids);
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public Set<String> getBatteriesIds() {
        return Collections.unmodifiableSet(batteriesIds);
    }

    public ReferenceVariable getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return "Batteries binding '" + name + "' (ref. variable : " + reference + ") [ " + String.join(", ", batteriesIds) + " ]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MetrixBatteriesBinding other)) {
            return false;
        }
        return name.equals(other.name) && reference == other.reference && batteriesIds.equals(other.batteriesIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, reference, batteriesIds);
    }

}
