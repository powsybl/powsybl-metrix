# Mapping configuration

A mapping configuration is made of 3 objects : 
- an iidm situation case
- a timeserie store (csv)
- a mapping groovy script using the [mapping dsl](#mapping-dsl) described below

The mapping process is mainly to be used within the metrix tool but it may be executed separatly
in order to check this temporary result.

With only required options, the mapping does not produce any output.
Option `--mapping-synthesis-dir`, it will output a [synthesis](#synthesis) of the mapping, allowing you
to check the consistency of the produced multi case.\
If you want in depth mapping detail, the option `--check-equipment-time-series` will produce the full timeseries mapping.\
Lastly, the option `--network-output-dir` will output the network case with value mapped for the steps asked.


## Mapping dsl

The purpose of the mapping is to map time serie values to network elements. As it is a groovy
script, it can also be used to create composite time series or modify the network.

### General parameters

The general parameters can be used to configure globally the mapping.

```groovy
parameters {
    toleranceThreshold // Tolerance threshold when comparing the mapped power to the limit defined by the network element (default : 0.0001) 
}
``` 

### Mapping timeseries to network

There is one mapping function for each type of network elements

```groovy
mapToGenerators {...}
mapToLoads {...}
mapToHvdcLines {...}
mapToBoundaryLines {...}
mapToBreakers {...}
mapToPsts {...}
```

the general syntax is 

```groovy
mapToXXX {
 variable variableName //optional, the name of the element variable to map the timeseries with. Each element have a default mapped variable (defined below)
 timeSeriesName 'timeseriesName' // name of the timeseries to map
 filter {...} // a filter predicate allowing to select which item of the element type we wish to map
 distributionKey {...} // optional, a repartition key that will define how the timeserie values will be distributed for each selected items (default is equipartition : val / N)
}   
```

For the `mapToBreakers` function, the `variable` and `distributionKey` are always ignored.


#### Time series

In the `timeSeriesName` variable you can map any time series by referring to its name.\
Time series available must exist within the input data or created within the script. For more details about time series manipulation, refer to the [Time series section](https://www.powsybl.org/pages/documentation/data/timeseries).

Note that if the same time series (refered by its name) is used in multiple mapToXXX of same type, the
mapping behavior will be as if it was applied once on all of the elements selected by theses mapToXXX instructions.\
For instance, if we map a constant time series of value 100, to a group A and in another mapTo, to a group B,
then, with the default distributionKey, it will map the value 50 to each group. 


#### Variable

The `variable` allow to specify which attribute of the network element will be overriden by its mapped time series value.
It is an optional variable which values depends on the element type (default is in bold) :

- mapToGenerators : **targetP**, minP, maxP, targetQ
- mapToLoads : **p0**, fixedActivePower, variableActivePower, q0, fixedReactivePower, variableReactivePower
- mapToHvdcLines : **activePowerSetpoint**, minP, maxP
- mapToBoundaryLines : **p0**
- mapToBreakers : **open** (with 1 corresponding to a closed switch and 0 to open)
- mapToPsts: **currentTap**

For the loads, it is forbidden to map `p0` and either `fixedActivePower` or `variableActivePower`, as theses variables are linked (`p0 = Pfixed + Pvar`).
It is restricted to prevent incoherent mapping.\
If only one of `fixedActivePower` or `variableActivePower` is mapped, then the value of the other unmapped one will default to 0. 

### Filter

The `filter` variable is evaluated for every network item (of the requested type) and only those which
match will be mapped.

The filter must a groovy statement returning `true` or `false`. It has access to the same variables that of the main script plus 3 variables
related to the current object filtered. One of the three depends on the `mapToXXX` type:
- generator OR hvdcLine OR load OR boundaryLine OR pst OR breaker
- voltageLevel
- substation

Be careful of the syntax of the groovy statement as an assignation `load.id = 'conso'` instead of the comparison `load.id == 'conso'` would return something truth equal to true and then allowing all equipments to be mapped.

#### Examples

To filter the generators of the country 'FR' of energy source type 'THERMAL', we can define the following filter:
```groovy
filter {generator.terminal.voltageLevel.substation.country == FR && generator.energySource == EnergySource.THERMAL}
```
or 
```groovy
filter {substation.country == FR && generator.energySource == EnergySource.THERMAL}
```

To filter the generators connected to a list of substations :
```groovy
filter {['D.BURP7', 'D.BUXP7'].contains(generator.terminal.voltageLevel.id)}
```

To filter the loads of a particular region (assuming the iidm model has been extended with a custom property `region`):  
```groovy
filter {    ['05', '08', '09', '13', '14', '17', '24', '25', '28'].contains(load.voltageLevel.substation.region)}
```

To filter generators that belong to the main connected subnetwork : 
```groovy
filter {generator.terminal.busView.bus?.inMainConnectedComponent}
```


### Distribution key

The distribution key `distributionKey` allows to set a distribution weight for each item selected. Its content is a
groovy statement returning either a time series name or a integer. The value is then normalized so that the total sum for
the current mapToXXX filter is equal to 1.\
The additionnal variable accessible from the groovy statement is the same as the one found in the [filter](#filter) : the equipment variable (which name depends on the mapTo type).

Note that the `mapToBreakers` function does not support a distribution key as the mapped value will be either 1 or 0 (close/open) and will be applied to all filtered breakers.

#### Examples

To create a distribution key relative to the Pmax of each group :
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

To create a distribution key defined by time series (assuming we have or created the time series SO_G1_key and SO_G2_key) :
```groovy
mapToGenerators {
    timeSeriesName 'SO_G' 
    variable targetP 
    filter { generator.id == 'SO_G1' || generator.id == 'SO_G2' } 
    distributionKey { generator.id + '_key' } 
}
```

To create a distribution key relative to the base load :
```groovy
mapToLoads {
    ...
    distributionKey {load.p0}    
}
``` 

### Unmapping

If we want to "unmap" items so that their value original static value. The main purpose (aside unmapping previously mapped items due to too broad filter maybe) is to prevent the selected items to appear in the "not mapped" section in the mapping synthesis that we will see later.
```groovy
unmappedXXX { // unmappedGenerators, unmappedLoads, …
   filter { … } // same usage as a normale mapToXXX
}
```

### Generators and HVDC limits

When mapping a time series to a generator or a HVDC, a time series value may break a threshold defined in the network case 
(a max or min power for instance). To handle this case we can choose to ignore the limit with either :
- a global parameter : use `--ignore-limits` in the CLI parameter
- a per time series configuration : add `ignoreLimits { "timeSeriesName" }` for each timeseries wanted

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

Mapping on the load of a whole region (with custom iidm properties `region`):
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
mapToPsts {
    timeSeriesName 'N1_TD'
    filter {'TD_1'} 
}
```

With added properties in iidm we can, using the groovy language, map multiple configurations :
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

When using the option `--mapping-synthesis-dir` it will produce csv synthesis files.
The file `mappingSynthesis.csv/txt` will contain a csv/text formatted (also displayed in the command line output) global synthesis
indicating how the different types of network elements or variable were processed :
- mapped : elements that were mapped with one time series
- unmapped : elements that weren't mapped at all
- ignored unammped : elements that weren't mapped but specifically indicated so
- multi-mapped : elements that were mapped multiple times (often when we map all of one type once then we apply another with a specific filter), only the last mapping will be applied
- disconnected : number of disconnected elements (as mapping do not apply to them)
- out of main cc : number of elements that are out of the main connected network (as mapping do not apply to them)

Files with template name `<componentType>ToTimeSeriesMapping.csv` show the effective mapping between timeseries and component.

Files with template name `disconnected<componentType>.csv` list the disconnected component with additionnal information. Note that the time series indicated as "AppliedTimeSeries" isn't effectively applied.\
Same or similar information can be found for out of main connex components, unmapped and ignored unmapped component in files with respective template name `outOfMainCc<componentType>.csv`, `unmapped<componentType>.csv` and `ignoredUnmapped<componentType>.csv`

Files with template name `timeSeriesTo<componentType>Mapping` show for each timeseries how it was used in the mapping, with the original network value and variable type.\
The file `timeSeries.csv` show the same mapping for all component types.

When combining with the option `--check-equipment-time-series` (and the required associated option `--check-versions`), two new files are produced :
- `balanceSummary.csv` which contains the overall power balance at each time step
- `mapping-logs.csv` which contains information and warning about the mapping process (for instance Pmin/Pmax violations)   

### Mapping status file

When using the option `--mapping-status-file`, it will produce a file which contains for each available time series if it was used for the mapping.

### Time series mapping generation

When using the option `--equipment-time-series-dir` (with required associated options `--check-equipment-time-series` and `--check-versions`),
it will produce a csv with template name `version_<version_number>.csv` containing for each time step the associated value for each component/variable mapped couple. 

### Network variant generation

When using the option `--network-output-dir`  (with required associated options `--check-equipment-time-series` and `--check-versions`),
it will produce as many iidm network files as time steps in the input time series (with filtering by `--check-versions`, `--first-variant` and `--max-variant-count` options).
Each iidm will have the template name `<network_id>_<version>_<YYYYMMDD>_<HHmm>.iidm` and is the network generated with values mapped at indicated time step.