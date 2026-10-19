/*
 * Copyright (c) 2026, RTE (https://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

mapToGenerators {
    timeSeriesName 'ts1'
    filter {
        generator.id=="FSSV.O11_G"
    }
    variable targetP
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
        generator.id=="FSSV.O12_G"
    }
    variable minP
}
mapToGenerators {
    timeSeriesName 'ts1'
    filter {
        generator.id=="FSSV.O12_G"
    }
    variable maxP
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
    variable targetV
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
mapToPhaseTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable phaseTapPosition
}
mapToPhaseTapChangers {
    timeSeriesName 'ts1'
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
    variable ratioTapPosition
}
mapToRatioTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable loadTapChangingCapabilities
}
mapToRatioTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable ratioRegulating
}
mapToRatioTapChangers {
    timeSeriesName 'ts1'
    filter {
        twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
    }
    variable targetV
}
mapToLccConverterStations {
    timeSeriesName 'ts1'
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
mapToLines {
    timeSeriesName 'ts1'
    filter {
        line.id=="FP.AND1  FVERGE1  1"
    }
    variable disconnected
}
mapToBatteries {
    timeSeriesName 'ts1'
    filter {
        battery.id=="BATTERY_1"
    }
    variable targetP
}
mapToBatteries {
    timeSeriesName 'ts1'
    filter {
        battery.id=="BATTERY_1"
    }
    variable targetQ
}
mapToBatteries {
    timeSeriesName 'ts1'
    filter {
        battery.id=="BATTERY_1"
    }
    variable minP
}
mapToBatteries {
    timeSeriesName 'ts1'
    filter {
        battery.id=="BATTERY_1"
    }
    variable maxP
}
mapToBatteries {
    timeSeriesName 'ts1'
    filter {
        battery.id=="BATTERY_1"
    }
    variable disconnected
}
