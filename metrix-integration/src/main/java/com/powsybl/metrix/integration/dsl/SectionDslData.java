package com.powsybl.metrix.integration.dsl;

import com.powsybl.metrix.integration.MetrixSection;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public class SectionDslData extends AbstractDslData {

    // Section monitoring
    private final Set<MetrixSection> sectionList;

    public SectionDslData() {
        this(new HashSet<>());
    }

    public SectionDslData(Set<MetrixSection> sectionList) {
        this.sectionList = sectionList;
    }

    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("sectionList", sectionList);
        return map;
    }

    // Getter
    public Set<MetrixSection> getSectionList() {
        return sectionList;
    }

    // Section monitoring
    public void addSection(MetrixSection section) {
        Objects.requireNonNull(section);
        sectionList.add(section);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SectionDslData other) {
            return sectionList.equals(other.sectionList);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sectionList);
    }
}
