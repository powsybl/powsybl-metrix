# Mapping configuration

## Introduction

A mapping configuration is made of 3 objects: 
- an IIDM network
- a time series store (in csv format)
- a mapping groovy script using the [mapping DSL](#mapping-dsl) described below

The mapping process is mainly used through Metrix simulator, but it may be executed separately in order to check its intermediate results.

With only required options, the mapping does not produce any output. In adding option `--mapping-synthesis-dir`, it will output a [synthesis](#synthesis) of the mapping, allowing you to check the consistency of the produced multi cases. If you want to go deeper in the mapping process, the option `--check-equipment-time-series` will produce the full time series mapping. Lastly, the option `--network-output-dir` will output the network case with mapped values for the steps requested.

## Mapping DSL

The purpose of the mapping is to map time series values on network elements. As it is a groovy script, it can create composite time series or modify the network.

### General parameters

The general parameters are used to configure globally the mapping.

```groovy
parameters {
    toleranceThreshold // Tolerance threshold when comparing the mapped power to the limit defined by the network element (default : 0.0001) 
}
``` 

### Time series mapping to network

There is one mapping function for each type of network elements:

```groovy
mapToGenerators {...}
mapToLoads {...}
mapToHvdcLines {...}
mapToBoundaryLines {...}
mapToBreakers {...}
mapToTransformers {...}
mapToPhaseTapChangers {...}
mapToRatioTapChangers {...}
mapToLccConverterStations {...}
mapToVscConverterStations {...}
```

the general syntax is: 

```groovy
mapToXXX {
 variable variableName //optional, the name of the element variable to map the time series with. Each element have a default mapped variable (defined below)
 timeSeriesName 'timeseriesName' // name of the time series to map
 filter {...} // a filter predicate allowing to select which item of the element type we wish to map
 distributionKey {...} // optional, a repartition key that will define how the time serie values will be distributed for each selected items (default is equipartition : val / N)
}   
```
The element of the general syntax are described below. 

Note that for the `mapToBreakers` function, the `variable` and `distributionKey` are always ignored.

#### Time series

With the `timeSeriesName` variable you can map any time series by referring to its name. The available time series 
must exist in the input data set or be created within the groovy script. For more details about time series management, 
refer to the [time series description](inv:core:std:doc#data/timeseries).

Note that if the same time series (referred by its name) is used in multiple `mapToXXX` of same type, the mapping 
behavior will be as if it was applied once on the group of elements selected by these `mapToXXX` instructions. 
For instance, if we map a constant time series of value 100, to a generator A and in another mapTo, to a generator B, 
then, with the default distributionKey, it will map the value 50 to each generator.

#### Variable

The `variable` allows to specify which network element attribute will be overwritten by its mapped time series value.
It is an optional variable (except for transformers) which values depends on the element type (default is in bold) :

- `mapToGenerators` **targetP**, minP, maxP, targetQ
- `mapToLoads` **p0**, fixedActivePower, variableActivePower, q0, fixedReactivePower, variableReactivePower
- `mapToHvdcLines` **activePowerSetpoint**, minP, maxP
- `mapToBoundaryLines` **p0**
- `mapToBreakers` **open** (with 1 corresponding to a closed switch and 0 to open)
- `mapToTransformers` ratedU1, ratedU2
- `mapToPhaseTapChangers` **phaseTapPosition**, regulationMode
- `mapToRatioTapChangers` **ratioTapPosition**, loadTapChangingCapabilities, regulating, targetV
- `mapToLccConverterStations` **powerFactor**
- `mapToVscConverterStations` **voltageSetpoint**, voltageRegulatorOn, reactivePowerSetpoint

For the loads, it is forbidden to map `p0` and in the same time `fixedActivePower` or `variableActivePower`, as theses variables are linked (`p0 = Pfixed + Pvar`). It is restricted to prevent incoherent mapping. If only `fixedActivePower` or `variableActivePower` is mapped, then the value of the other unmapped one will be set to 0 by default. 

#### Filter

The `filter` variable is evaluated for every network item (of the requested type) and only those which match will be mapped.

The filter must be a groovy statement returning `true` or `false`. It has access to the same variables of the main script, with three more variables
related to the current object filtered.
- generator OR hvdcLine OR load OR boundaryLine OR pst OR breaker, depending on the `mapToXXX` type
- voltageLevel
- substation

Be careful of the syntax of the groovy statement as an assignation `load.id = 'conso'` instead of the comparison `load.id == 'conso'` would return something truth equal to true and then allowing all equipments to be mapped.

Please learn more with the following examples:

To filter the generators of the country 'FR' of energy source type 'THERMAL', we can define the following filter:
```groovy
filter {generator.terminal.voltageLevel.substation.country == FR && generator.energySource == EnergySource.THERMAL}
```
or 
```groovy
filter {substation.country == FR && generator.energySource == EnergySource.THERMAL}
```

To filter the generators connected to a list of substations:
```groovy
filter {['D.BURP7', 'D.BUXP7'].contains(generator.terminal.voltageLevel.id)}
```

To filter the loads of a particular region (assuming the IIDM model has been extended with a custom property `region`):  
```groovy
filter {    ['05', '08', '09', '13', '14', '17', '24', '25', '28'].contains(load.voltageLevel.substation.region)}
```

To filter generators that belong to the main connected component: 
```groovy
filter {generator.terminal.busView.bus?.inMainConnectedComponent}
```

#### Distribution key

The distribution key `distributionKey` allows to set a distribution weight for each selected item. Its content is a groovy statement returning either a time series name or an integer. The value is then normalized so that the sum for the current mapToXXX filter is equal to 1. The additional variable accessible from the groovy statement is the same as the one found in the [filter](#filter): the equipment variable (which name depends on the mapTo type).

Note that the `mapToBreakers` function does not support a distribution key as the mapped value will be either 1 or 0 (close/open) and will be applied to all filtered breakers.

Please look at the following examples:

To create a distribution key relative to the Pmax of each group:
```groovy
mapToGenerators {
    ...
    distributionKey {generator.maxP}
}
```

To create a distribution key relative to the power target:
```groovy
distributionKey {generator.targetP}
```

To create a distribution key defined by time series (assuming we have or created the time series SO_G1_key and SO_G2_key):
```groovy
mapToGenerators {
    timeSeriesName 'SO_G' 
    variable targetP 
    filter { generator.id == 'SO_G1' || generator.id == 'SO_G2' } 
    distributionKey { generator.id + '_key' } 
}
```

To create a distribution key relative to the base load:
```groovy
mapToLoads {
    ...
    distributionKey {load.p0}    
}
``` 

### Unmapping

We use that if we want to `unmap` items in order to keep their original static value. The main purpose (aside unmapping previously mapped items due to too broad filter maybe) is to prevent the selected items to appear in the `not mapped` section in the mapping synthesis that we will see later.
```groovy
unmappedXXX { // unmappedGenerators, unmappedLoads, …
   filter { … } // same usage as a normale mapToXXX
}
```

### Generators and HVDC limits

When mapping a time series to a generator or a HVDC line, a time series value may break a threshold defined in the network case 
(a max power or min power for instance). To handle this case we can choose to ignore the limit with either:
- a global parameter : use `--ignore-limits` in the command line interface parameter
- a per time series configuration : add `ignoreLimits { "timeSeriesName" }` for each wanted time series

If no option is set to ignore the limits, then if it happens, the program will exit with an exception. Otherwise it will log a warning message.

## Examples

Time series mapping on a generator :
```groovy
mapToGenerators {
    timeSeriesName 'myTimeSeries'
    filter {generator.id=='GROUP_1'}
}
```

Time series mapping on the variable active power of a load :
```groovy
mapToLoads {
    variable variableActivePower
    timeSeriesName 'myTimeSeries'
    filter {load.id=='CONSO_1'}
}
```

Mapping on the load of a whole region (with custom IIDM properties `region`):
```groovy
mapToLoads {
    timeSeriesName '15_fr_LOAD'
    filter {substation.region=='15_fr'}
    distributionKey { load.p0}
}
```

Mapping on breakers:
```groovy
mapToBreakers {
  timeSeriesName 'ARD_CT2018_PRATCP6'
  filter {breaker.id == 'PRATCP6_PRATC   6COUPL    DJ'}
}
```

Mapping on Phase Tap Shifters:
```groovy
mapToPhaseTapChangers {
    timeSeriesName 'N1_TD'
    filter {'TD_1'} 
}
```

With added properties in IIDM we can, using the groovy language, map multiple configurations:
```groovy
for (area in ['05', '08', '09', '13', '14', '17', '24', '25', '28']) {
    // on définit des couples "AA" / "BB" pour mapper pour chaque zone, les chroniques contenant "AA" sur les générateurs de la zone de type "BB"
    for (prod_type in ['SOLAR', 'WIND', 'HYDRO', 'PSP', 'MISC', 'COAL', 'NUCLEAR', 'GAS']) {
        //on sélectionne la chronique de nom "nomDelaZone_AA"
        mapToGenerators {
            timeSeriesName area + '_fr_' + prod_type
            filter {
                // on mappe chaque chronique sélectionnée sur les générateurs qui sont dans la bonne région DI et de type "BB"
                substation.region == area && generator.genre == prod_type
            }
            distributionKey {
                generator.maxP
            }
        }  
    }
}
```

Mapping on multiple items (HVDC)
```groovy
mapToHvdcLines {
    timeSeriesName 'HVDC.DE32DE34'
    filter {hvdcLine.id == 'BRUN_GROS' || hvdcLine.id == 'WILS_GRAFEN'}
    distributionKey {1}
}
```

## Outputs

### Synthesis

The use of the option `--mapping-synthesis-dir` produces csv synthesis files.

The file `mappingSynthesis.csv/txt` contains a csv/text formatted (also displayed in the command line output) global synthesis indicating how the different types of network elements or variables were processed:
- mapped: elements that were mapped with time series
- unmapped: elements that weren't mapped at all
- ignored unmapped: elements that weren't mapped but specifically indicated so
- multi-mapped: elements that were mapped multiple times (often when we map all elements of one type once then we apply another with a specific filter), only the last mapping will be applied
- disconnected: number of disconnected elements (as mapping do not apply to them)
- out of main cc: number of elements that are out of the main connected component (as mapping do not apply to them)

Component type represents the network element (generators, loads, breakers, etc.):
- Files with template name `<componentType>ToTimeSeriesMapping.csv` show the effective mapping between time series and the element.
- Files with template name `disconnected<componentType>.csv` list the disconnected elements with additional information. Note that the time series indicated as "AppliedTimeSeries" isn't effectively applied.
- Same or similar information can be found for elements that are out of main connected components, unmapped and ignored unmapped, in files with respective template name `outOfMainCc<componentType>.csv`, `unmapped<componentType>.csv` and `ignoredUnmapped<componentType>.csv`.
- Files with template name `timeSeriesTo<componentType>Mapping` show for each time series how it was used in the mapping, with the original network value and variable type. The file `timeSeries.csv` show the same mapping for all component types.

When combining with the option `--check-equipment-time-series` (and the required associated option `--check-versions`), two new files are produced:
- `balanceSummary.csv` which contains the overall power balance at each time step
- `mapping-logs.csv` which contains information and warnings about the mapping process (for instance Pmin/Pmax violations)   

### Mapping status file

The use of the option `--mapping-status-file` produces a file which indicates for each available time series if it was used for the mapping.

### Time series mapping generation

The use of the option `--equipment-time-series-dir` (with required associated options `--check-equipment-time-series` and `--check-versions`) produces a csv with template name `version_<version_number>.csv` containing for each time step the associated value for each component/variable mapped couple. 

### Network variant generation

The use of the option `--network-output-dir`  (with required associated options `--check-equipment-time-series` and `--check-versions`) produces as many IIDM network files as time steps we have in the input time series (with filtering by `--check-versions`, `--first-variant` and `--max-variant-count` options).
Each le m will have the template name `<network_id>_<version>_<YYYYMMDD>_<HHmm>.iidm` and is the network generated with values mapped at indicated time step.