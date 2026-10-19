/*
 * Copyright (c) 2021 RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License v. 2.0. If a copy of the MPL was not distributed with this
 * file You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

parameters {
    toleranceThreshold 0.0001f
}
timeSeries['zero'] = 0
timeSeries['constant_ts3'] = 500
mapToGenerators {
    timeSeriesName 'zero'
}
mapToGenerators {
    timeSeriesName 'constant_ts1'
    filter {
        generator.id == 'FSSV.O11_G'
    }
}
mapToGenerators {
    timeSeriesName 'variable_ts1'
    filter {
        generator.id == 'FSSV.O12_G'
    }
}
mapToLoads {
    timeSeriesName 'constant_ts2'
    filter {
        load.id == 'FSSV.O11_L'
    }
}
mapToLoads {
    timeSeriesName 'variable_ts2'
    filter {
        load.id == 'FVALDI11_L'
    }
}
mapToLoads {
    timeSeriesName 'constant_ts4'
    filter {
        load.id == 'FVALDI11_L2'
    }
    variable fixedActivePower
}
mapToLoads {
    timeSeriesName 'variable_ts2'
    filter {
        load.id == 'FVALDI11_L2'
    }
    variable variableActivePower
}
mapToLoads {
    timeSeriesName 'constant_ts1'
    filter {
        load.id == 'FVERGE11_L'
    }
    variable fixedActivePower
}
mapToLoads {
    timeSeriesName 'constant_ts2'
    filter {
        load.id == 'FVERGE11_L'
    }
    variable variableActivePower
}