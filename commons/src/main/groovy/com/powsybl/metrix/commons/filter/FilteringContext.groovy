package com.powsybl.metrix.commons.filter

import com.powsybl.iidm.network.*
/**
 * @author Nicolas Lhuillier <nicolas.lhuillier@rte-france.com>
 */
class FilteringContext {

    Identifiable identifiable
    VoltageLevel voltageLevel
    Substation substation

    FilteringContext(Identifiable identifiable) {
        this.identifiable = identifiable
        if (identifiable instanceof Injection) {
            voltageLevel = ((Injection) identifiable).terminal.voltageLevel
            substation = voltageLevel.substation
        } else if (identifiable instanceof Switch) {
            voltageLevel = ((Switch) identifiable).voltageLevel
            substation = voltageLevel.substation
        }
    }
}

