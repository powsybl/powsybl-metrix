/*
 * Copyright (c) 2026, RTE (https://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

mapToBatteries {
    timeSeriesName 'constant_ts1'
    filter {battery.id == 'BATTERY_1'}
    variable targetP
}
mapToBatteries {
    timeSeriesName 'variable_ts1'
    filter {battery.id == 'BATTERY_1'}
    variable targetQ
}
mapToBatteries {
    timeSeriesName 'switch_ts'
    filter {battery.id == 'BATTERY_1'}
    variable disconnected
}
mapToBatteries {
    timeSeriesName 'constant_ts2'
    filter {battery.id == 'BATTERY_1'}
    variable maxP
}
mapToBatteries {
    timeSeriesName 'constant_ts1'
    filter {battery.id == 'BATTERY_1'}
    variable minP
}
mapToGenerators {
    timeSeriesName 'constant_ts1'
    filter {generator.id == 'FSSV.O11_G'}
    variable maxP
}
mapToGenerators {
    timeSeriesName 'constant_ts2'
    filter {generator.id == 'FSSV.O12_G'}
}
mapToGenerators {
    timeSeriesName 'variable_ts1'
    filter {generator.id == 'FSSV.O11_G'}
    variable minP
}
mapToGenerators {
    timeSeriesName 'ts1'
    filter {
        generator.id=="FVALDI11_G"
    }
    variable voltageRegulatorOn
}
mapToGenerators {
    timeSeriesName 'ts1'
    filter {
        generator.id=="FSSV.O11_G"
    }
    variable targetQ
}
mapToGenerators {
    timeSeriesName 'ts1'
    filter {
        generator.id=="FSSV.O11_G"
    }
    variable targetV
}
mapToGenerators {
    timeSeriesName 'ts1'
    filter {
        generator.id=="FSSV.O11_G"
    }
    variable voltageRegulatorOn
}
mapToGenerators {
    timeSeriesName 'ts1'
    filter {
        generator.id=="FSSV.O11_G"
    }
    variable disconnected
}
mapToLoads {
    timeSeriesName 'ts1'
    filter {
        load.id=="FSSV.O11_L"
    }
    variable p0
}
mapToLoads {
    timeSeriesName 'ts1'
    filter {
        load.id=="FSSV.O11_L"
    }
    variable q0
}
mapToLoads {
    timeSeriesName 'ts1'
    filter {
        load.id=="FVALDI11_L"
    }
    variable fixedActivePower
}
mapToLoads {
    timeSeriesName 'ts1'
    filter {
        load.id=="FVALDI11_L2"
    }
    variable variableActivePower
}
mapToLoads {
    timeSeriesName 'ts1'
    filter {
        load.id=="FVALDI11_L"
    }
    variable fixedReactivePower
}
mapToLoads {
    timeSeriesName 'ts1'
    filter {
        load.id=="FVALDI11_L2"
    }
    variable variableReactivePower
}
mapToHvdcLines {
    timeSeriesName 'ts1'
    filter {
        hvdcLine.id=="HVDC1"
    }
    variable activePowerSetpoint
}
mapToHvdcLines {
    timeSeriesName 'ts2'
    filter {
        hvdcLine.id=="HVDC2"
    }
    variable minP
}
mapToHvdcLines {
    timeSeriesName 'ts1'
    filter {
        hvdcLine.id=="HVDC2"
    }
    variable maxP
}
mapToHvdcLines {
    timeSeriesName 'ts1'
    filter {
        hvdcLine.id=="HVDC1"
    }
    variable nominalV
}
mapToBreakers {
    timeSeriesName 'switch_ts'
    filter {breaker.id == 'FP.AND1_FP.AND1_DJ_OMN'}
}
mapToPhaseTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable phaseTapPosition
}
mapToPhaseTapChangers {
    timeSeriesName 'regulation_mode_ts'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable regulationMode
}
mapToPhaseTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable phaseRegulating
}
mapToPhaseTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable targetDeadband
}
mapToTransformers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable ratedU1
}
mapToTransformers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable ratedU2
}
mapToTransformers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable disconnected
}
mapToRatioTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable targetV
}
mapToRatioTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable ratioTapPosition
}
mapToRatioTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable loadTapChangingCapabilities
}
mapToLccConverterStations {
    timeSeriesName 'power_factor_ts'
    filter {
        lccConverterStation.id=="FVALDI1_FVALDI1_HVDC1"
    }
    variable powerFactor
}
mapToVscConverterStations {
    timeSeriesName 'ts1'
    filter {
        vscConverterStation.id=="FSSV.O1_FSSV.O1_HVDC1"
    }
    variable voltageRegulatorOn
}
mapToVscConverterStations {
    timeSeriesName 'ts1'
    filter {
        vscConverterStation.id=="FSSV.O1_FSSV.O1_HVDC1"
    }
    variable voltageSetpoint
}
mapToVscConverterStations {
    timeSeriesName 'ts1'
    filter {
        vscConverterStation.id=="FSSV.O1_FSSV.O1_HVDC1"
    }
    variable reactivePowerSetpoint
}
mapToVscConverterStations {
    timeSeriesName 'ts1'
    filter {
        vscConverterStation.id=="FSSV.O1_FSSV.O1_HVDC1"
    }
    variable voltageRegulatorOn
}
mapToLines {
    timeSeriesName 'ts1'
    filter {
        line.id=="FP.AND1  FVERGE1  1"
    }
    variable disconnected
}
