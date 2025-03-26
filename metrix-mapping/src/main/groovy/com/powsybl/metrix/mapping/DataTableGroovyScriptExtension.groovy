package com.powsybl.metrix.mapping

import com.google.auto.service.AutoService
import com.powsybl.metrix.mapping.exception.DataTableException
import com.powsybl.scripting.groovy.GroovyScriptExtension

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
@AutoService(GroovyScriptExtension.class)
class DataTableGroovyScriptExtension implements GroovyScriptExtension {

    DataTableGroovyScriptExtension() {}

    static class DataTableGroovyObject {

        private final DataTableStore store

        private DataTableGroovyObject(DataTableStore store) {
            this.store = Objects.requireNonNull(store)
        }

        DataTable getAt(String name) {
            Objects.requireNonNull(name)
            if (exists(name)) {
                return store.get(name)
            }
            throw new DataTableException("Data table '" + name + "' not found")
        }

        Set<String> names() {
            return store.names()
        }

        boolean exists(String dataTableName) {
            return store.exists(dataTableName)
        }
    }

    @Override
    void load(Binding binding, Map<Class<?>, Object> contextObjects) {
        if (contextObjects.keySet().contains(DataTableStore.class)) {
            DataTableStore dataTableStore = contextObjects.get(DataTableStore.class) as DataTableStore
            def dt = new DataTableGroovyObject(dataTableStore)
            binding.dataTable = dt
            binding.dt = dt
            binding.toDataTable = { List<String> columnNames, List<List<String>> values -> DataTable::toDataTable(columnNames, values)
            }
        }
    }

    @Override
    void unload() {}
}
