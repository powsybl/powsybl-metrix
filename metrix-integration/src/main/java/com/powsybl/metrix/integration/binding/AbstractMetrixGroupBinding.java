/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.binding;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
public abstract class AbstractMetrixGroupBinding {

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
    private final Set<String> ids = new HashSet<>();
    private final ReferenceVariable reference;

    AbstractMetrixGroupBinding() {
        this.name = null;
        this.reference = null;
    }

    protected AbstractMetrixGroupBinding(String name, Collection<String> ids) {
        this(name, ids, ReferenceVariable.PMAX);
    }

    protected AbstractMetrixGroupBinding(String name, Collection<String> ids, ReferenceVariable reference) {
        this.name = name;
        this.ids.addAll(ids);
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(ids);
    }

    public ReferenceVariable getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return getGroupName() + " binding '" + getName() + "' (ref. variable : " + getReference() + ") [ " + String.join(", ", getIds()) + " ]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractMetrixGroupBinding other)) {
            return false;
        }
        return name.equals(other.name) && reference == other.reference && ids.equals(other.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, reference, ids);
    }

    @JsonIgnore
    protected abstract String getGroupName();

    @JsonIgnore
    public abstract String getGroupNameSingular();

}
