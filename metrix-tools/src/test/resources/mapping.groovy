/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

mapped_generators = ['SO_G1', 'SO_G2']

for ( gen  in  mapped_generators ) {
   mapToGenerators {
      timeSeriesName gen
      filter { generator.id == gen }
   }
   println('generator ' + gen + ' was mapped')
}

mapToLoads {
   timeSeriesName 'SE_L1'
   filter { load.id == 'SE_L1' }
} 

timeSeries['nul'] = 0

mapToGenerators {
   timeSeriesName 'nul'
   filter { generator.id in ['N_G', 'SE_G'] }
}

mapToLoads {
   timeSeriesName 'nul'
   filter { load.id in ['SE_L2', 'SO_L'] }
}

mapToPhaseTapChangers {
    timeSeriesName 'nul'
    filter { twoWindingsTransformer.id in ['NE_NO_1'] }
}

mapToHvdcLines {
   timeSeriesName 'nul'
   filter {hvdcLine.id in ['HVDC1', 'HVDC2'] }
}
