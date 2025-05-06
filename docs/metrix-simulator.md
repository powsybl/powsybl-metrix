(simulator)=
# Metrix simulator

## Introduction

*Metrix simulator* is an independent C++ executable. This module is capable of performing Power Load Flow calculations as 
well as Optimal Power Load Flow for networks with multiple variants.

## Installation

*Metrix simulator* must be installed before using *powsybl-metrix*. It has its own toolchain and requirements.
You can either download the already built and released executable on GitHub (for example 
[here](https://github.com/powsybl/powsybl-metrix/releases/download/v2.7.0/metrix-simulator-ubuntu.zip) for the last 
version built for Ubuntu at the time of writing), or build the executable by yourself.

Detailed build instructions are available [on this page](simulator/build.md).

## Running the simulator
The following environment variables must be defined to run metrix properly:
* `METRIX_ETC`: location of the `.dic` files for a language region (some of these dictionaries are exported in `etc` directory in installation directory)

All options are detailed in the helper
```
$> ./metrix-simulator --help
Usage:
 metrix-simulator <errorFilepath> <variantFilepath> <resultsFilepath> <firstVariantIndex> <numberVariants> <paradesFilepath> 
Such as:
 - <errorFilepath>: a string representing the log file path
 - <variantFilepath>: a string representing the variants file path 
 - <resultsFilepath>: a string representing the prefix path for the results files
 - <firstVariantIndex>: an integer representing the index of the first variant (described in the <variantFilepath> file) to be considered
 - <numberVariants>: an integer representing the number of variants (described in the <variantFilepath> file) to be considered from the <firstVariantIndex>
 - <paradesFilepath>: a string representing the parades file path (= "parades.csv" by default)

[options] 
Metrix options:
  -h [ --help ]                 Display help message
  --log-level arg               Logger level (allowed values are critical, 
                                error, warning, info, debug, trace): default is
                                info
  -p [ --print-log ]            Print developer log in standard output
  --verbose-config              Activate debug/trace logs relative to 
                                configuration
  --verbose-constraints         Activate debug/trace logs relative to 
                                constraint detection
  --write-constraints           Write the constraints in a dedicated file
  --print-constraints           Trace in logs the constraints matrix (time 
                                consuming even if trace logs are not active), 
                                log level at trace is required
  --write-PTDF                  Write the power transfer distribution factors 
                                matrix in a dedicated file
  --write-LODF                  Write the line outage distribution factors 
                                matrix report in a dedicated file
  --check-constraints-level arg Check adding constraints:
                                0: no check (default)
                                1: When adding a constraint, perform a load 
                                flow to check transit (more time consuming)
                                2: When adding a constraint, run every incident
                                to check that we didn't forget a constraint 
                                (even more time consuming
  --compare-reports             Compare load flow reports after application of 
                                report factors to check trigger of coupling
  --no-incident-group           Ignore incident if a group of N-K is not 
                                available
  --all-outputs                 Display all values in results files
  --mps-file                    Export MPS file
```

## Inputs and outputs

The inputs and outputs available for Metrix simulator are detailed [on the specific page](simulator/io_doc.md).

## Functional and mathematical descriptions

### Configuration and scenarios

OPF, Load Flow only, OPF w/o redispatching (with gap variables), and OPF_WITH_OVERLOAD.
PTDFs, LODF
Gestion des crashs
Parade topologique en N

[Config](simulator/config.md)

### Algorithm description

Steps, micro-iterations, variants, network modeling.
[Algo](simulator/algo.md)

### Mathematical model

Optimization problem formulation
[Math](simulator/math.md)


```{toctree}
---
maxdepth: 2
hidden: true
---

simulator/build.md
simulator/io_doc.md
simulator/config.md
simulator/algo.md
simulator/math.md
```