/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.binding;

import java.util.*;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixGeneratorsBinding {

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
    private final Set<String> generatorsIds = new HashSet<>();
    private final ReferenceVariable reference;

    MetrixGeneratorsBinding() {
        this.name = null;
        this.reference = null;
    }

    public MetrixGeneratorsBinding(String name, Collection<String> ids) {
        this(name, ids, ReferenceVariable.PMAX);
    }

    public MetrixGeneratorsBinding(String name, Collection<String> ids, ReferenceVariable reference) {
        this.name = name;
        this.generatorsIds.addAll(ids);
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public Set<String> getGeneratorsIds() {
        return Collections.unmodifiableSet(generatorsIds);
    }

    public ReferenceVariable getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return "Generators binding '" + name + "' (ref. variable : " + reference + ") [ " + String.join(", ", generatorsIds) + " ]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MetrixGeneratorsBinding other)) {
            return false;
        }
        return name.equals(other.name) && reference == other.reference && generatorsIds.equals(other.generatorsIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, reference, generatorsIds);
    }

}
