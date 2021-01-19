/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

mapToGenerators{
timeSeriesName 'SO_G1'
filter {generator.id=='SO_G1'}
}

mapToGenerators{
timeSeriesName 'SO_G1'
filter {generator.id=='SO_G2'}
}



generateurs=[ 'SO_G1','SO_G2' ]

for ( gen  in  generateurs        ){
mapToGenerators{
timeSeriesName gen
filter {  generator.id==gen  }
}
println('generator '+gen+' was mapped')
}




mapToLoads{
timeSeriesName 'SE_L1'
filter {load.id=='SE_L1'}
} 

timeSeries['nul']=0
timeSeries['test'] = new com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc('SE_L1') + 2
ts['test2'] = ts['test'] + 3

mapToGenerators{
timeSeriesName 'nul'
filter {generator.id in ['N_G','SE_G'] }
}


mapToLoads{
timeSeriesName 'nul'
filter {load.id in ['SE_L2','SO_L']}
}


network.getTwoWindingsTransformer('NE_NO_1').phaseTapChanger.tapPosition=17

mapToHvdcLines{
timeSeriesName 'nul'
filter {hvdcLine.id in ['HVDC1','HVDC2'] }
}

//mapToBreakers {
//timeSeriesName 'ouverture_S_SO_2'
//filter {breaker.id=="SOO1_Disj SS1 SOO1 2" || breaker.id=="SS1_Disj SS1 SOO1 2"}
//}

//mapToBreakers {
//timeSeriesName 'passage_2_noeuds_S '
//filter {breaker.id=="SOO1_SOO1_DJ_OMN"}
//}

