package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.metrix.mapping.DistributionKey;

import java.util.Objects;

public class MappedEquipment {

    private final Identifiable<?> identifiable;

    private final DistributionKey distributionKey;

    public MappedEquipment(Identifiable<?> identifiable, DistributionKey distributionKey) {
        this.identifiable = Objects.requireNonNull(identifiable);
        this.distributionKey = distributionKey;
    }

    public Identifiable<?> getIdentifiable() {
        return identifiable;
    }

    public String getId() {
        return identifiable.getId();
    }

    public DistributionKey getDistributionKey() {
        return distributionKey;
    }
}
