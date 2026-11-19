<style>
r { color: Red }
o { color: Orange }
g { color: Green }
y { color: yellow}
</style>

(io)=
# Inputs and outputs

## Contents
1. [Introduction](#io-intro)
2. [Input data in *json* format](#io-inputs-json)
    1. [Computation options](#io-computing-options)
    2. [Results options](#io-results-options)
    3. [Regions](#io-regions)
    4. [Nodal information](#io-nodal-information)
        1. [Vertices](#io-summits)
        2. [Loads](#io-loads)
    5. [Generators](#io-generators)
    6. [Branches](#io-quadripoles)
        1. [Elementary branches](#io-elementary-quadripoles)
        2. [Phase shifters](#io-phase-shifters)
    7. [HVDC lines](#io-dc-lines)
    8. [N-1 and N-k contingencies](#io-contingencies)
    9. [Curative](#io-curative)
    10. [Monitored sections](#io-monitored-sections)
    11. [Coupled variables](#io-coupled-variables)
    12. [Detailed marginal variations](#io-detailed-marginal-variations)
3. [Variants](#io-variants)
4. [Remedial actions](#io-remedial-actions)
5. [Output data](#io-outputs)
   1. [Results file](#io-result-file)
      1. [Tables describing the situation](#io-detailed-situation-tables)
      2. [Results tables](#io-result-table)
   2. [Log file](#io-logs)
   3. [LODF file](#io-lodf)
   4. [PTDF file](#io-ptdf)
   5. [MPS file](#io-mps)

(io-intro)=
## Introduction
This document describes the inputs and outputs of the METRIX v6 model (used in the ImaGrid platform). Following its
integration into ImaGrid, some formats initially used in the ASSESS platform have been kept; namely
CSV for the variants, the remedial actions and the output tables. The other input data have, for their part,
been put into *json* form, within which some have been added and others made optional. The data
format of METRIX v6 is therefore not compatible with previous versions of the model.

(io-inputs-json)=
## Input data in *json* format
The ImaGrid gateway for METRIX takes as input a network file in IIDM format and a configuration script in
Groovy format. It generates a *json* file: '*fort.json*'.

The data shown in **bold** in the following tables must always be present in the input files,
the others are optional.
The default value configured in METRIX is given in parentheses.
Those shown in <o>orange</o>, on the other hand, are non-mandatory data, but they cause segmentation faults
if they are not filled in. Finally, those shown in <y>yellow</y> cause errors (see
log file) if they are not filled in.

(io-types)=
N.B.: The data types are defined by a letter as follows <a id="types"></a>:
- B = *BOOLEAN*
- C = *STRING*
- I = *INTEGER*
- R = *FLOAT*

(io-computing-options)=
### Computation options
| Name         | Type | Size   | Description                                                                                                                                                                     |
|:-------------|:-----|:-------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **CGCPERTE** | R    | 1      | Initial loss coefficient.<br>= lossFactor (0)                                                                                                                                   |
| **UNOMINAL** | I    | 1      | Nominal voltage used in the per-unit conversion on the ImaGrid side.<br>= nominalU (0)                                                                                          |
| MODECALC     | I    | 1      | 0 OPF, 1 Load Flow only, 2 OPF without redispatching (with slack variables) and 3 OPF_WITH_OVERLOAD.<br>= computationType (0)                                                   |
| NBMAXMIT     | I    | 1      | Maximum number of micro-iterations per variant.<br>= nbMaxIteration (30)                                                                                                        |
| NBMAXCUR     | I    | 1      | Maximum number of curative actions per contingency. No limit if the value is negative or zero.<br>= nbMaxCurativeAction (0)                                                     |
| COUTECAR     | I    | 1      | Cost of the slack variables<br>= gapVariableCost (10)                                                                                                                           |
| RELPERTE     | I    | 1      | Maximum number of relaunch(es) for loss gap (default value: 0 = no relaunch)<br>= lossNbRelaunch (0)                                                                            |
| SEUILPER     | I    | 1      | Threshold of the gap between theoretical losses and computed losses requiring a relaunch (MW)<br>= lossThreshold (500)                                                          |
| COUTDEFA     | R    | 1      | Default cost of loss of load<br>= lossOfLoadCost (13000)                                                                                                                        |
| MAXSOLVE     | I    | 1      | Maximum time allowed for each solver resolution. 0: no limit.<br>= maxSolverTime (0)                                                                                            |
| TDPENALI     | R    | 1      | Phase-shifting cost of the PSTs in the objective function. A negative or zero value means that there is no associated cost (phase shift not penalized).<br>= pstCostPenality (10-2) |
| HVDCPENA     | R    | 1      | Flow cost of the HVDC lines in the objective function. A negative or zero value means that there is no associated cost (flow not penalized).<br>= hvdcCostPenality (10-1)      |
| PROBAINC     | R    | 1      | Probability of the contingencies<br>= contingenciesProbability(10-3)                                                                                                            |
| TESTITAM     | B    | 1      | Taking into account the post-contingency instant<br>= preCurativeThresholds (false)                                                                                             |
| INCNOCON     | B    | 1      | Taking into account the contingencies that break connectivity<br>= outagesBreakingConnexity (false)                                                                             |
| PARNOCON     | B    | 1      | Taking into account the remedial actions that break connectivity<br>= remedialActionsBreakingConnexity (false)                                                                  |
| PAREQUIV     | B    | 1      | Detection of equivalent remedial actions<br>= analogousRemedialActionDetection(false)                                                                                           |
| COUENDCU     | R    | 1      | Cost of undelivered energy for a contingency that breaks connectivity (load disconnected from the network)<br>= curativeLossOfLoadCost(26000)                                   |
| COUENECU     | R    | 1      | Cost of unevacuated energy on a contingency that breaks connectivity (generation disconnected from the network)<br>= curativeLossOfGenerationCost(100)                          |
| LIMCURGR     | I    | 1      | Curative redispatching limit<br>= curativeRedispatchingLimit(-1)                                                                                                                |
| ADEQUAOF     | I    | 1      | Offset of generator and load shedding costs in the balancing phase<br>= adequacyCostOffset(0)                                                                                   |
| REDISPOF     | I    | 1      | Offset of generator and load shedding costs in the redispatching phase<br>= redispatchingCostOffset(0)                                                                          |
| NULLCOST     | R    | 1      | Minimum cost of generators and load shedding, applied after adding the phase offset<br>= generatorMinCost(0.5)                                                                  |

(io-results-options)=
### Results options
| Name     | Type | Size | Description                                                                                      |
|:---------|:-----|:-----|:-------------------------------------------------------------------------------------------------|
| NBTHREAT | I    | 1    | Number of N-k threats in the results<br>= nbThreatResults (1)                                    |
| EQUILRES | B    | 1    | Output of the detailed results of the initial balancing<br>= withAdequacyResults (false)         |
| REDISRES | B    | 1    | Output of the detailed results of the preventive redispatching<br>= withRedispatchingResults (false) |
| VARMARES | B    | 1    | Computation of marginal variations on the branches<br>= marginalVariationsOnBranches (false)     |
| LCCVMRES | B    | 1    | Computation of marginal variations on the HVDC lines<br>= marginalVariationsOnHvdc (false)       |
| LOSSDETA | B    | 1    | Computation of losses by region/country<br>= lossDetailPerCountry (false)                        |
| OVRLDRES | B    | 1    | Output of the network elements under constraint only<br>= overloadResultsOnly (false)            |

(io-regions)=
### Regions
| Name         | Type | Size     | Description                                                                 |
|:-------------|:-----|:---------|:----------------------------------------------------------------------------|
| **CGNBREGI** | I    | 1        | Total number of regions<br>= $\sum$ country                                 |
| CGNOMREG     | C    | CGNBREGI | For each region, name of the region<br>= « paysCvg » if it exists, « country » otherwise |

(io-nodal-information)=
### Nodal information

(io-summits)=
#### Vertices
| Name         | Type | Size | Description                                                                                                       |
|:-------------|:-----|:-----|:------------------------------------------------------------------------------------------------------------------|
| **TNNBNTOT** | I    | 1    | Total number of vertices<br>= $\sum$ bus s.t. voltageLevel.busBreakerView.bus $\in$ main connected component      |

(io-loads)=
#### Loads

(table_tnneucel)=

| Name            | Type | Size     | Description                                                                                                                     |
|:----------------|:-----|:---------|:--------------------------------------------------------------------------------------------------------------------------------|
| **ECNBCONS**    | I    | 1        | Number of elementary loads<br>= $\sum$ load s.t. load.terminal.busBreakerView.bus $\in$ main connected component                |
| <o>TNNOMNOE</o> | C    | ECNBCONS | Names of the loads<br>= load.id                                                                                                 |
| <o>TNNEUCEL</o> | I    | ECNBCONS | Number of the connection vertex of the elementary load<br>= index of the bus in the TNNOMNOE table                              |
| <o>CPPOSREG</o> | I    | ECNBCONS | Vertex-region link<br>= index of the region in CGNOMREG                                                                         |
| <o>ESAFIACT</o> | R    | ECNBCONS | Value of the active load (sum of the fixed and affine parts)<br>= load.p0                                                       |
| TNVAPAL1        | I    | ECNBCONS | Percentage of the first load shedding tier.<br>(100 if no node is configured)                                                  |
| TNVACOU1        | R    | ECNBCONS | Load shedding cost (value of COUTDEFA by default)                                                                               |

(io-generators)=
### Generators

(table_trnomgth)=

| Name            | Type | Size     | Description                                                                                                                                                                                                             |
|:----------------|:-----|:---------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **TRNBGROU**    | I    | 1        | Number of connected generators<br>= $\sum$ generator s.t. generator.terminal.busBreakerView.bus $\in$  main connected component                                                                                         |
| <o>TRNOMGTH</o> | C    | TRNBGROU | Name of the generator<br>= generator.id                                                                                                                                                                                 |
| <o>TNNEURGT</o> | I    | TRNBGROU | Connection vertices of the generator<br>= index of the bus in the TNNOMNOE table                                                                                                                                        |
| <o>SPPACTGT</o> | R    | TRNBGROU | Setpoint power $P_{obj} \in [\underline{P},\overline{P}]$<br>= generator.targetP                                                                                                                                        |
| <y>TRVALPMD</y> | R    | TRNBGROU | Maximum available power<br>= generator.maxP                                                                                                                                                                             |
| <o>TRPUIMIN</o> | R    | TRNBGROU | Minimum power<br>= generator.minP                                                                                                                                                                                       |
| **TRNBTYPE**    | I    | 1        | Number of generator types                                                                                                                                                                                               |
| <o>TRNOMTYP</o> | C    | TRNBTYPE | Names of the generator types                                                                                                                                                                                            |
| <o>TRTYPGRP</o> | I    | TRNBGROU | Index of the generator type in TRNOMTYP                                                                                                                                                                                 |
| <o>SPIMPMOD</o> | I    | TRNBGROU | Indicates whether the generator is available for redispatching (3 = 'OUI_AR'), adequacy (2 = 'OUI_HR'), both (1 = 'OUI_HR_AR') or neither (0 = 'NON_HR_AR')<br>(1 if no generator is configured and 0 otherwise.)       |
| TRDEMBAN        | R    | TRNBGROU | Half-band of regulation in secondary regulation.<br>= prorata of the largest generator contingency (0)                                                                                                                  |

(io-quadripoles)=
### Branches

(io-elementary-quadripoles)=
#### Elementary branches
| Name            | Type | Size     | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|:----------------|:-----|:---------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **CQNBQUAD**    | I    | 1        | Number of elementary branches<br>= $\sum$ (quad = *line* &#124;&#124; *twoWindingsTransformer*) s.t. quad.terminal1.busBreakerView.bus && quad.terminal2.busBreakerView.bus $\in$ main connected component<br>+ $\in$ *switch* s.t. switch.retained && (switch.voltageLevel.busBreakerView.bus1 && switch.voltageLevel.busBreakerView.bus2 $\in$ main connected component)                                                                                                                       |
| <o>CQNOMQUA</o> | C    | CQNBQUAD | Name of the branch<br>= quad.id                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| <o>CQADMITA</o> | R    | CQNBQUAD | 	Admittance of the branch referred to Ubase (inverse of the reactance, i.e. neglecting the value of the resistance)<br>= $1 / (X * nominalU^2 / Unom^2)$<br>For lines:<br>$X = line.x$<br>For transformers:<br>$X_{transfo} = twoWindingsTransformer.x$<br>For phase-shifting transformers:<br>$X = X_{transfo} * (1 + X_{prise} / 100)$<br>$X_{prise} = phaseTapChanger.phaseTapPosition.x$<br>For bus couplers:<br>$X = 10^-5$<br>Unom = line.terminal2.voltageLevel.nominalV                  |
| <o>CQRESIST</o> | R    | CQNBQUAD | Resistance used for the loss computation (in pu, base nominalU)<br>The formulas are the same as for CQADMITA, replacing X by R                                                                                                                                                                                                                                                                                                                                                                   |
| <o>QASURVDI</o> | I    | CQNBQUAD | Monitoring indicator of the branch in N. 2 = flow result only, 1 = monitored by the model, 0 = no result                                                                                                                                                                                                                                                                                                                                                                                         |
| <o>QASURNMK</o> | I    | CQNBQUAD | Monitoring indicator of the branch in N-K. 2 = flow result only, 1 = monitored by the model, 0 = no result                                                                                                                                                                                                                                                                                                                                                                                      |
| <y>TNNORQUA</y> | I    | CQNBQUAD | Index of the origin node of the branch (quad.terminal1.busBreakerView.bus) in the TNNOMNOE table                                                                                                                                                                                                                                                                                                                                                                                                 |
| <y>TNNEXQUA</y> | I    | CQNBQUAD | Index of the end node of the branch (quad.terminal2.busBreakerView.bus) in the TNNOMNOE table                                                                                                                                                                                                                                                                                                                                                                                                    |
| NBOPEBRA        | I    | 1        | Number of open branches kept because they can be reclosed by a remedial action<br>(0)                                                                                                                                                                                                                                                                                                                                                                                                            |
| OPENBRAN        | I    | NBOPEBRA | Indices of the open branches in the CQNOMQUA table                                                                                                                                                                                                                                                                                                                                                                                                                                               |

(io-phase-shifters)=
#### Phase shifters
| Name            | Type | Size     | Description                                                                                                                                              |
|:----------------|:-----|:---------|:---------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DTNBTRDE**    | I    | 1        | Total number of phase-shifting transformers<br>$\sum$ twoWindingsTransformer.phaseTapChanger if twoWindingsTransformer is in the CQNOMQUA table          |
| <o>DTTRDEQU</o> | I    | DTNBTRDE | Phase shifter-elementary branch number correspondence<br>= index of twt.id in the branch tables                                                          |
| <o>DTMODREG</o> | I    | DTNBTRDE | Control type<br>0: Out of service<br>1: optimized angle<br>2: fixed angle<br>3: optimized power (not used)<br>4: fixed power (not used)                   |
| <o>DTVALINF</o> | R    | DTNBTRDE | Minimum phase shift value of the PST in N, in degrees<br>= phaseTapChanger.lowTapPosition.alpha                                                          |
| <o>DTVALSUP</o> | R    | DTNBTRDE | Maximum phase shift value of the PST in N, in degrees <br>= phaseTapChanger.highTapPosition.alpha                                                        |
| <o>DTVALDEP</o> | R    | DTNBTRDE | Initial phase shift value per PST in degrees, this is the value used if DTMODREG $\in {1, 2}$<br>= phaseTapChanger.tapPosition.alpha                     |

(io-dc-lines)=
### HVDC lines

(table_dcnomqua)=

| Name            | Type | Size     | Description                                                                                                                                                                                                          |
|:----------------|:-----|:---------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DCNBLIES**    | I    | 1        | Number of HVDC lines<br>= $\sum$ hvdcLine s.t.  (hvdcLine.converterStation1.terminal.busBreakerView.bus && hvdcLine.converterStation2.terminal.busBreakerView.bus) $\in$ main connected component                     |
| <o>DCNOMQUA</o> | C    | DCNBLIES | Name of the HVDC line<br>= hvdcLine.id                                                                                                                                                                               |
| <o>DCNORQUA</o> | I    | DCNBLIES | Index of the origin node of the line (hvdcLine.converterStation1.terminal.busBreakerView.bus) in the TNNOMNOE table                                                                                                  |
| <o>DCNEXQUA</o> | I    | DCNBLIES | Index of the end node of the line (hvdcLine.converterStation2.terminal.busBreakerView.bus) in the TNNOMNOE table                                                                                                     |
| <o>DCMINPUI</o> | R    | DCNBLIES | Minimum power<br>= - activePowerRange.oPRFromCS2toCS1 if the HvdcOperatorActivePowerRange extension is used, - hvdcLine.maxP otherwise.                                                                              |
| <o>DCMAXPUI</o> | R    | DCNBLIES | Maximum power<br>= activePowerRange.oPRFromCS1toCS2 if the HvdcOperatorActivePowerRange extension is used, hvdcLine.maxP otherwise.                                                                                  |
| <o>DCIMPPUI</o> | R    | DCNBLIES | Imposed power $P_0$<br>= activePowerControl.p0 if the HvdcAngleDroopActivePowerControl extension is used, +/- hvdcLine.activePowerSetPoint otherwise                                                                 |
| <o>DCREGPUI</o> | I    | DCNBLIES | Type of flow control on the line<br>1: fixed power<br>2: optimized power                                                                                                                                             |
| <o>DCTENSDC</o> | R    | DCNBLIES | Nominal voltage of the DC cable<br>= hvdcLine.nominalV                                                                                                                                                               |
| <o>DCRESIST</o> | R    | DCNBLIES | Resistance of the DC cable in pu (base nominalV), normalized with respect to the DC voltage.<br>= hvdcLine.r * nominalU $^2$ / hvdcLine.nominalV $^2$                                                                |
| <o>DCPERST1</o> | R    | DCNBLIES | Loss coefficient (in %) of the origin converter station<br>= hvdcLine.converterStation1.lossFactor                                                                                                                   |
| <o>DCPERST2</o> | R    | DCNBLIES | Loss coefficient (in %) of the end converter station<br>= hvdcLine.converterStation2.lossFactor                                                                                                                      |
| DCNDROOP        | I    | 1        | Number of HVDC lines operated in AC emulation mode ($P = P_0 + k \Delta\Theta$)<br>(0)                                                                                                                               |
| DCDROOPK        | R    | DCNDROOP | For each HVDC line in AC emulation, in the order of the [DCNOMQUA table](#table_dcnomqua), value of the coefficient k (in MW/°)                                                                                      |

(io-contingencies)=
### N-1 and N-k contingencies
| Name         | Type | Size                              | Description                                                                                                                                                                                                                                                                                                                                                                                                           |
|:-------------|:-----|:----------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DMNBDEFK** | I    | 1                                 | Number of contingencies                                                                                                                                                                                                                                                                                                                                                                                               |
| DMNOMDEK     | C    | DMNBDEFK                          | Name of the contingency                                                                                                                                                                                                                                                                                                                                                                                               |
| DMPTDEFK     | I    | DMNBDEFK                          | For each contingency, pointer to DMDESCRK                                                                                                                                                                                                                                                                                                                                                                             |
| DMDESCRK     | I    | DMNBDEFK<br>$+ 2 * \sum nb_{<i>}$ | Description of the contingencies<br>For each contingency:<br>Nb, $Type_{<1>}$, $Indice_{<1>}$, …, $Type_{<nb>}$, $Indice_{<nb>}$<br>Nb: number of elements in the contingency<br>Type:<br>1 for a branch (line, transformer)<br>2 for a generator<br>3 for an HVDC link<br>Indice: in the list of branches, in the list of generators or in the list of HVDC links depending on the type |
| BDEFRES      | I    | 1                                 | Size of the array of faults with detailed results<br>(0)                                                                                                                                                                                                                                                                                                                                                              |
| TDEFRES      | I    | NBDEFRES                          | Sequence of triplets: index of the branch, number of faults with detailed results, list of the indices of these faults.                                                                                                                                                                                                                                                                                               |

(io-curative)=
### Curative
| Name     | Type | Size                    | Description                                                                                                                         |
|:---------|:-----|:------------------------|:------------------------------------------------------------------------------------------------------------------------------------|
| DTNBDEFK | I    | DTNBTRDE                | For each PST, number of contingencies for which the PST can act in curative<br>(0)                                                  |
| DTPTDEFK | I    | ≤ DTNBTRDE  * DMNBDEFK	 | For each PST acting in curative, in increasing index order, indices of the contingencies handled in curative                        |
| DCNBDEFK | I    | DCNBLIES                | For each PST, number of contingencies for which the PST can act in curative<br>(0)                                                  |
| DCPTDEFK | I    | ≤ DCNBLIES * DMNBDEFK   | For each PST acting in curative, in increasing index order, indices of the contingencies handled in curative                        |
| NBLDCURA | I    | 1	                      | Number of loads that can be curtailed in curative<br>(0)                                                                            |
| LDNBDEFK | I    | ECNBCONS                | For each load, number of contingencies handled in curative                                                                          |
| LDCURPER | I    | NBLDCURA                | For each curative load, in index order, percentage of curtailment in curative                                                       |
| LDPTDEFK | I    | ≤ NBLDCURA * DMNBDEFK   | For each curative load, in the order of the [TNNEUCEL table](#table_tnneucel), pointer to the contingencies handled in curative.    |
| GRNBCURA | I    | 1                       | Number of generators that can act in curative<br>(0)                                                                                |
| GRNBDEFK | I    | GRNBCURA                | For each curative generator, number of contingencies handled in curative                                                            |
| GRPTDEFK | I    | ≤ GRNBCURA * DMNBDEFK   | For each curative generator, in the order of the [TRNOMGTH table](#table_trnomgth), pointer to the contingencies handled in curative. |

(io-monitored-sections)=
### Monitored sections
| Name         | Type | Size            | Description                                                                                                                                                                      |
|:-------------|:-----|:----------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **SECTNBSE** | I    | 1               | Number of monitored sections                                                                                                                                                     |
| SECTNOMS     | C    | SECTNBSE        | Name of the section                                                                                                                                                              |
| SECTMAXN     | R    | SECTNBSE        | N threshold of the section                                                                                                                                                       |
| SECTNBQD     | I    | SECTNBSE        | Number of branches of the section (nb1, nb2, …)                                                                                                                                  |
| SECTTYPE     | I    | $\sum nb_{<i>}$ | Table of branch types (list of the nb1 types of section 1, list of the nb2 types of section 2, …)                                                                                |
| SECTNUMQ     | I    | $\sum nb_{<i>}$ | Table of numbers in the list of branches or in the list of HVDC links (list of the nb1 numbers of section 1, list of the nb2 numbers of section 2, …)                            |
| SECTCOEF     | R    | $\sum nb_{<i>}$ | Table of coefficients associated with the branches (list of the nb1 coefficients of section 1, list of the nb2 coefficients of section 2, …)                                     |

*Note: the `SECTMAXN` threshold only applies when the weighted sum of the flows of the section is positive.
The opposite direction is not constrained; to bound both directions, declare a second section with opposite
coefficients, the same threshold and a different name.*

(io-coupled-variables)=
### Coupled variables
| Name     | Type | Size                  | Description                                                                                            |
|:---------|:-----|:----------------------|:-------------------------------------------------------------------------------------------------------|
| NBGBINDS | I    | 1                     | Number of generator couplings<br>(0)                                                                   |
| NBLBINDS | I    | 1                     | Number of load couplings<br>(0)                                                                        |
| GBINDDEF | I    | ≤ NBGBINDS + TRNBGROU | For each coupling, number of generators in the coupling then indices of the coupled generators         |
| GBINDNOM | C    | NBGBINDS              | Names of the generator couplings                                                                       |
| GBINDREF | I    | NBGBINDS              | For each coupling, type of the reference variable (0 = PMAX, 1 = PMIN, 2 = POBJ, 3 = PMAX-POBJ)       |
| LBINDDEF | I    | ≤ NBLBINDS + ECNBCONS | For each coupling, number of loads in the coupling then indices of the coupled loads                   |
| LBINDNOM | C    | NBLBINDS              | Names of the load couplings                                                                            |

(io-detailed-marginal-variations)=
### Detailed marginal variations
| Name     | Type | Size     | Description                                                                                                                            |
|:---------|:-----|:---------|:---------------------------------------------------------------------------------------------------------------------------------------|
| NBVARMAR | I    | 1        | Size of the array of detailed marginal variations (PTVARMAR)<br>(0)                                                                    |
| PTVARMAR | I    | NBVARMAR | Sequence of triplets: index of the branch, number of faults with detailed marginal variations, list of the indices of these faults.    |

(io-variants)=
## Variants
The data specific to each variant are described in a csv file.
The first line indicates the number of draws (i.e. of variants), after the keyword *NT*.
Each line of the file then starts with the draw number followed by the type of the law described, identified by a keyword.
The data specific to the law and to the draw are written after the keyword, namely:
- >**PRODIN**: the number of generators, then the names of the unavailable generators.
- >**PRODIM**: the number of generators, then as many pairs (generator index, setpoint power) as the indicated number of generators.
- >**QUADIN**: the number of unavailable lines (outages) then the names of the unavailable network elements. If these unavailabilities break the connectivity of the network, the variant is ignored (code 4), or the computation stops with an error for the "-1" variant.
- >**TRVALPMD**: the number of generators, then as many pairs (generator index, maximum available power) as the indicated number of generators.
- >**TRPUIMIN**: the number of generators, then as many pairs (generator index, minimum available power) as the indicated number of generators.
- >**CONELE**: the number of loads, then as many pairs (load name, new load value) as the indicated number of loads. These are nodal loads. Eurostag convention: 1 node = 1 load
- >**ECHANG**: law that should rather be called balance by the loads. The number of regions, then as many pairs (region index, target balance for the region) as the indicated number of regions. After applying all the other laws of the same draw (in particular the load and imposed power laws), METRIX computes the power balance, i.e. the sum of the imposed generations minus the sum of the loads of the same region, the result being named $\Delta1$. Note that it is really the balance only if there are no modifiable generators. A check is made on this point and the variant is rejected if there is a modifiable generator in a region for which a balance is to be set.<br>The law provides the desired balance, in MW, in this region, which will be named $\Delta2$; note that if the quantity $\Delta2$ is positive, then there is more generation than load. The difference ($\Delta1 - \Delta2$) corresponds to the power to be redistributed over all the loads of the region. The distribution is done pro rata to the load: $c_i = c_i – c_i*(\Delta1 - \Delta2)  / sumC$; $c_i$ is the load at node $i$ and $sumC$ is the sum of the loads of the whole region concerned.
This law dating from ASSESS is currently not used in ImaGrid.
- >**ECHANGP**: the number of regions, then as many pairs (region index, target balance for the region) as the indicated number of regions. This law is similar to the ECHANG law but it acts on the generators instead of the loads. After applying all the other laws of the same draw, METRIX adjusts the powers of the modifiable generators of the region to balance the balance (sum of the generations minus sum of the loads) to the value indicated in the variant. The selection of the generators to modify is made according to the off-grid merit-order costs (upward and downward). If, in a region, there is too much non-modifiable generation or too little available generation to balance the balance, the variant is rejected. Moreover, METRIX does not guarantee that the balance will not be modified during the off-grid phase. It is up to the user to choose prohibitive off-grid merit-order costs for the region considered compared to the study regions.
This law dating from ASSESS is currently not used in ImaGrid.
- >**CTORDR**: the number of upward costs without network, then as many pairs (generator name, cost) as the indicated number of generators.
- >**COUBHR**: the number of downward costs without network, then as many pairs (generator name, cost) as the indicated number of generators.
- >**COUHAR**: the number of upward costs with network, then as many pairs (generator name, cost) as the indicated number of generators.
- >**COUBAR**: the number of downward costs with network, then as many pairs (generator name, cost) as the indicated number of generators.
- >**DCMINPUI**: the number of HVDC minimum power modifications then as many pairs (HVDC line name, new Pmin) as indicated.
- >**DCMAXPUI**: the number of HVDC maximum power modifications then as many pairs (HVDC line name, new Pmax) as indicated.
- >**DCIMPPUI**: the number of modifications of the setpoint power of the HVDC lines, then as many pairs (HVDC line name, setpoint power) as indicated. If the HVDC line is not in "imposed setpoint" mode in the base case, METRIX automatically changes the operating mode of the HVDC line (for this variant only).

**Remark**: If an HVDC line is in "imposed setpoint" mode, before analyzing the variant, METRIX checks that the
setpoint power of the line lies between the MAX and MIN bounds. If it does not, the variant is rejected.
- >**DTVALDEP**: the number of modifications of the initial phase shift value of the PST, then as many pairs (PST name, initial phase shift) as indicated.
- >**COUEFF**: the number of modifications of the load curtailment cost, then as many pairs (load index, cost) as indicated.
- >**QATI00MN**: the number of modifications of the N threshold, then as many pairs (branch name, threshold value in MW) as indicated.
- >**QATI5MNS**: the number of modifications of the N-1 threshold, then as many pairs (branch name, threshold value in MW) as indicated.
- >**QATI20MN**: the number of modifications of the N-k threshold, then as many pairs (branch name, threshold value in MW) as indicated.
- >**QATITAMN**: the number of modifications of the threshold before switching actions, then as many pairs (branch name, threshold value in MW) as indicated.
- >**GROURAND**: the number of generators, the ordered list of generators for the 'shuffling' so as not to have to add noise to the costs.
- >**PROBABINC**: the number of contingency probabilities, then as many pairs (contingency name, probability).
- >**QATI00MN2**: the number of modifications of the N threshold from the end to the origin (if asymmetric threshold), then as many pairs (branch name, threshold value in MW) as indicated.
- >**QATI20MN2**: the number of modifications of the N-k threshold from the end to the origin (if asymmetric threshold), then as many pairs (branch name, threshold value in MW) as indicated.
- >**QATITAMK**: the number of modifications of the N-k threshold before switching actions, then as many pairs (branch name, threshold value in MW) as indicated.
- >**QATITAMK2**: the number of modifications of the N-k threshold before switching actions from the end to the origin (if asymmetric threshold), then as many pairs (branch name, threshold value in MW) as indicated.
- >**QATITAMN2**: the number of modifications of the threshold before switching actions from the end to the origin (if asymmetric threshold), then as many pairs (branch name, threshold value in MW) as indicated.

**Warning**: for each variant, the model only takes into account a single line for each type of law. If
several laws are defined for the same variant only the last one will be kept.

The number of the first variant is always $\geq 0$.

The "-1" variant makes it possible to indicate modifications of the base case that will be valid for all the variants
(unless they are modified by the data of a variant).

**Example**:

```
NT;2;
-1;PRODIM;1;groupe1;100; (modification commune à toutes les variantes)
0; (variante sans modification par rapport au cas de base)
1;QUADIN;1;ligne indispo;
1;PRODIM;2;groupe1;200;groupe2;300;
```

(io-remedial-actions)=
## Remedial actions

The data specific to each remedial action are described in a csv file.
The first line indicates the number of remedial actions, after the keyword *NB*.
Each line corresponds to a remedial action, such that:

*<Contingency name>*|*<Sections to monitor separated by a "pipe">*;*<Number of first listed branches concerned>*;<List of *<Branch name(s)>* separated by a semicolon>;

**Example**:

```
NB;4;
S_SO_1;1;SS1_SS1_DJ_OMN;
S_SO_1;1;SOO1_SOO1_DJ_OMN;
S_SO_1;2;SS1_SS1_DJ_OMN;SOO1_SOO1_DJ_OMN;
S_SO_1;1;S_SO_2;
```

N.B.: The remedial actions provided are not taken into account if the launch mode is *LOAD FLOW* (i.e. mode 1).

(io-outputs)=
## Output data
To facilitate the integration and testing of METRIX in ImaGrid, the outputs of METRIX v6 keep the format of the
tabulated files of ASSESS (and of METRIS).
Several output files are or can be generated at the end of a simulation.

(io-result-file)=
### Results file
There is a single results file per variant, named as follows: *\<resultsFilepath\>_s\<variantNumber\>*.
Within this file, the data are grouped by theme in output tables. Each line of the file
starts with the identifier of the table.

For each field of each table, the following is defined:
- the name of the stored quantity;
- the format of the data (see [data formats](#io-types));
- and for a numeric quantity (*I* or *R*), the unit of this quantity: MW for a power, m.u. (monetary unit)
for a cost, no unit otherwise.

Some tables are not accessible unless the `--all-outputs` option is given when launching the METRIX simulator executable.
Let `EPSILON_SORTIES = 0.05`.

(io-detailed-situation-tables)=
#### Tables describing the situation
**Table S1**: unavailable network elements (branches and generators)

*Format*: ```S1 ;INDISPONIBILITE; OUVRAGE;```

| Quantity name        | Type | Unit | Description                  |
|:---------------------|:-----|:-----|:-----------------------------|
| Network element type | I    |      | 1: branch; 2: generator      |
| Network element name | C    |      |                              |

**Note**: Table available if the `--all-outputs` option is given when launching METRIX simulator.

-----------------------------

(status_output)=
**Table C1** (single line): execution report

*Format*: ```C1 ;COMPTE RENDU;CODE;```

| Quantity name | Type | Unit | Description                                                                                                                                                                                                                                                                                                         |
|:--------------|:-----|:-----|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Error code    | I    |      | **0** if "OK", otherwise<br>**-1** = error during the resolution<br>**1**  = if no solution to the problem (infeasible problem, often caused by EOD)<br>**2**  = too many constraints (max reached)<br>**3** = too many micro-iterations (max reached)<br>**4** = inconsistent variant data (variant ignored)    |

-----------------------------
**Table C2**: list of contingencies breaking connectivity

*Format*: ```C2 ;NON CONNEXITE;INCIDENT;NB NOEUDS;PROD COUPEE;CONSO COUPEE;```

| Quantity name            | Type | Unit | Description |
|:-------------------------|:-----|:-----|:------------|
| Name of the contingency  | C    |      |             |
| Number of disconnected nodes | I |     |             |
| Volume of generation cut | R    | MW   |             |
| Volume of load cut       | R    | MW   |             |

-----------------------------
(io-table_c4)=
**Table C4**: list of constraining contingencies, of contingencies that could be handled in
curative or of contingencies that generated a maximum flow on contingency (see [table R3B](#io-table_r3b))

*Format*: ```C4 ;INCIDENTS;NUMERO;TYPE;OUVRAGE;```

| Quantity name            | Type | Unit | Description                                                                                                                                                                                                          |
|:-------------------------|:-----|:-----|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Contingency sequence number | I |      | Numbering starting from 1. Numbers then used in tables [R4](#io-table_r4), [R1B](#io-table_r1b), [R2B](#io-table_r2b), [R3B](#io-table_r3b), [R5B](#io-table_r5b) and [R6B](#io-table_r6b) (see below)               |
| Type                     | C    |      | «N-1L», «N-KL», «N-KG», «N-HVDC», «COMBO»                                                                                                                                                                            |
| Name of the contingency  | C    |      | Name of the contingency                                                                                                                                                                                              |

-----------------------------
**Table C5** (single line): initial balances per synchronous area, before balancing

*Format*: ```C5 ;ZONE SYNC;NUM ZONE;BILAN;```

| Quantity name                | Type | Unit | Description |
|:-----------------------------|:-----|:-----|:------------|
| Synchronous area number      | I    |      | E.g. 0      |
| Value of the initial balance | R    | MW   | E.g. -20.5  |

(io-result-table)=
#### Results tables
**Note**: in the output tables, only the values greater than the *EPSILON_SORTIES* threshold are written, if the option
`--all-outputs` is given when launching METRIX simulator.

**Table R1**: results per vertex

*Format*:
* ```R1 ;PAR CONSO;CONSO;DEMANDE;DF HR;CDF HR;DF AR;CDF AR;``` if the `--all-outputs` option is given when launching METRIX simulator,
* ```R1 ;PAR CONSO;CONSO;DEMANDE;DF HR;DF AR;``` otherwise.

| Quantity name          | Type | Unit | Description                                                                                                                                                                                                     |
|:-----------------------|:-----|:-----|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Name of the vertex     | C    |      |                                                                                                                                                                                                                 |
| Demand                 | R    | MW   | Sum of the loads at this vertex                                                                                                                                                                                 |
| Loss of load HR        | R    | MW   | Preventive load shedding during balancing (positive value) |
| Loss of load cost HR   | R    |      | Cost of the preventive load shedding during balancing (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                              |
| Loss of load AR        | R    | MW   | Network-related preventive load shedding (positive value), in addition to any load shedding due to the lack of generation |
| Loss of load cost AR   | R    |      | Cost of the network-related preventive load shedding (result only present if the `--all-outputs` option is given when launching METRIX simulator) |

**Note**: if the `--all-outputs` option is given when launching METRIX simulator and there is no modifiable load then all the fields, except the name, are *0*.

-----------------------------
(io-table_r1b)=
**Table R1B**: results per curative load. Only the loads activated in curative on a contingency are displayed

*Format*: ```R1B ;INCIDENT;CONSO;EFFACEMENT;```

| Quantity name       | Type | Unit | Description                                                  |
|:--------------------|:-----|:-----|:-------------------------------------------------------------|
| Name of the load    | C    |      | Name of the load                                             |
| Contingency number  | I    |      | Reference to the numbering of [table C4](#io-table_c4)       |
| Curtailed power     | R    | MW   | Volume of load curtailed in curative (positive value)        |

-----------------------------
**Table R1C**: results per load coupling

*Format*: ```R1C ;NOM REGROUPEMENT;DELTA_C;```

| Quantity name      | Type | Unit | Description                                                       |
|:-------------------|:-----|:-----|:------------------------------------------------------------------|
| Name of the coupling | C  |      | Name given to the coupling in the configuration                   |
| Variation          | R    | MW   | Sum of the variations over all the loads of the coupling          |

-----------------------------
**Table R2**: results per generator. Only the generators whose preventive setpoint differs from the initial setpoint
(before or after adjustment depending on the chosen option) are displayed

*Format*:
* ```R2 ;PAR GROUPE;GROUPE;PDISPO;DELTA_PIMP;DELTA_P_HR;DELTA_P_AR;CT HR;CT AR;CT ARP;CT GRT;CT GRTP;CT HAUSSE AR;CT BAISSE AR;CT ORDRE;CT EMPIL HR;```
if the `--all-outputs` option is given when launching METRIX simulator
* ```R2 ;PAR GROUPE;GROUPE;PDISPO;DELTA_PIMP;DELTA_P_HR;DELTA_P_AR;``` otherwise.

| Quantity name                                | Type | Unit | Description                                                                                                                                                                                                                                                                                          |
|:---------------------------------------------|:-----|:-----|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Name of the generator                        | C    |      |                                                                                                                                                                                                                                                                                                      |
| Available power                              | R    | MW   | Pmax of the generator in the variant                                                                                                                                                                                                                                                                 |
| Imposed power adjustment                     | R    | MW   | $\Delta P_{cons}$ between the *json* data and that of the variant                                                                                                                                                                                                                                    |
| Power adjustment during the initial balancing | R   | MW   | $\Delta P_{cons}$ between the variant data and the value after the balancing phase (if the `--all-outputs` option is not given when launching METRIX simulator, and the *EQUILRES* option is enabled, value rounded to 10-1 in which case; non-rounded value if the `--all-outputs` option is given) |
| Preventive power adjustment                  | R    | MW   | $\Delta P_{cons}$ between the balancing phase and the preventive setpoint  (if the `--all-outputs` option is not given when launching METRIX simulator, and the *REDISRES* option is enabled, value rounded to 10-1 in which case; non-rounded value if the `--all-outputs` option is given)         |
| Off-grid cost                                | R    |      | Sum of the upward and downward off-grid costs (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                           |
| *CT AR*                                      | R    |      | = 0.0 (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                                                                   |
| *CT ARP*                                     | R    |      | = 0.0 (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                                                                   |
| Cost of the power delta                      | R    |      | (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                                                                         |
| *CT GRTP*                                    | R    |      | = 0.0 (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                                                                   |
| Upward cost AR                               | R    |      | Upward cost with network (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                                                |
| Downward cost AR                             | R    |      | Downward cost with network (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                                              |
| Upward cost HR                               | R    |      | Upward off-grid cost (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                                                    |
| Downward cost HR                             | R    |      | Downward off-grid cost (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                                                                                                                  |

-----------------------------
(io-table_r2b)=
**Table R2B**: curative results per generator. Only the generators whose curative setpoint
differs from the preventive setpoint are displayed

*Format*: ```R2B ;INCIDENT;NOM GROUPE;DELTA_P;```

| Quantity name       | Type | Unit | Description                                                  |
|:--------------------|:-----|:-----|:-------------------------------------------------------------|
| Contingency number  | I    |      | Reference to the numbering of [table C4](#io-table_c4)       |
| Name of the generator | C  |      |                                                              |
| Adjusted power      | R    | MW   |                                                              |

-----------------------------
**Table R2C**: results per generator coupling

*Format*: ```R2C ;NOM REGROUPEMENT;DELTA_P;```

| Quantity name        | Type | Unit | Description                                                       |
|:---------------------|:-----|:-----|:------------------------------------------------------------------|
| Name of the coupling | C    |      | Name given to the coupling in the configuration                   |
| Variation            | R    | MW   | Sum of the variations over all the generators of the coupling     |

-----------------------------
(io-table_r3)=
**Table R3**: flows in N per branch and per monitored section

*Format*: ```R3 ;PAR LIGNE;LIGNE;TRANSIT N;SEUIL N;SEUIL N-k;SEUIL ITAM;```

| Quantity name      | Type | Unit | Description                    |
|:-------------------|:-----|:-----|:-------------------------------|
| Name of the branch | C    |      |                                |
| Flow N             | R    | MW   | Positive from origin to end    |

*Note: if the `--all-outputs` option is given when launching METRIX simulator, the four thresholds are also displayed.*

*Note: a monitored section is reported under its name, with the weighted sum of the flows of its network elements.
The N-k threshold and ITAM threshold columns are 99999 (undefined), a section being monitored in N only.*

-----------------------------
(io-table_r3b)=
**Table R3B**: maximum flows per branch on contingency. Only the monitored network elements or those with results are displayed

*Format*: ```R3B ;PAR LIGNE;LIGNE;INCIDENT AM;MENACE MAX AM;```

| Quantity name                                             | Type | Unit | Description                                                |
|:----------------------------------------------------------|:-----|:-----|:-----------------------------------------------------------|
| name of the branch                                        | C    |      |                                                            |
| contingency causing the max flow before switching actions |      |      | Reference to the numbering of [table C4](#io-table_c4)     |
| max flow before switching actions                         | R    | MW   | positive from origin to end                                |
| contingency causing the 1st max flow on contingency       |      |      | Reference to the numbering of [table C4](#io-table_c4)     |
| 1 $^{st}$ max flow on contingency                         | R    | MW   | positive from origin to end                                |
| *contingency causing the N $^{th}$ max flow on contingency* |   |      | *As many results as requested in the parameters*           |
| *N $^{th}$ max flow on contingency*                       |      |      |                                                            |

*Note: this table is not written if there is no contingency on the network.*

-----------------------------
(io-table_r3c)=
**Table R3C**: specific flows on contingency

*Format*: ```R3C ;PAR LIGNE;LIGNE;INCIDENT;TRANSIT;```

| Quantity name      | Type | Unit | Description                                                |
|:-------------------|:-----|:-----|:-----------------------------------------------------------|
| Name of the branch | C    |      |                                                            |
| Contingency number | I    |      | Reference to the numbering of [table C4](#io-table_c4)     |
| Flow               | R    | MW   | Positive from origin to end                                |

-----------------------------
(io-table_r4)=
**Table R4**: marginal variations per link, in N and on contingency

*Format*: ```R4 ;VAR. MARGINALES;LIGNE;INCIDENT;VMAR;```

| Quantity name       | Type | Unit | Description                                                                                          |
|:--------------------|:-----|:-----|:-----------------------------------------------------------------------------------------------------|
| Name of the branch  | C    |      |                                                                                                      |
| Contingency number  | I    |      | 0 for N, otherwise see [table C4](#io-table_c4)                                                      |
| Marginal variation  | R    | m.u. | Impact on the objective function of a 1 MW increase of the threshold (N, N-k or AM) of this network element |

-----------------------------
(io-table_r4b)=
**Table R4B**: detailed marginal variations per link, in N and on contingency (only the
(network element, contingency) pairs for which the detailed marginal variations have been requested and are non-zero are
stored in the table)

*Format*: ```R4B ;VAR. MARGINALES;LIGNE;INCIDENT;VMAR TYPVAR;NOMVAR;VOL;COUT;```

| Quantity name                                           | Type | Unit | Description                                                                  |
|:--------------------------------------------------------|:-----|:-----|:-----------------------------------------------------------------------------|
| Name of the branch                                      | C    |      |                                                                              |
| Contingency number                                      | I    |      | 0 for N, otherwise see [table C4](#io-table_c4)                              |
| Type of the network element or transaction that varies  | C    |      | Vertex « N », generator « G » or curative action « C »                       |
| Name of the network element that varies                 | C    |      | Vertex (if loss of load varies) or generator (if generation varies)          |
| Volume of variation                                     | R    | MW   | Sensitivity coefficient of the network element                               |
| Cost of the variation                                   | R    | m.u. | Contribution of this variable to the marginal cost                           |

-----------------------------
(io-table_r5)=
**Table R5**: results per phase-shifting transformer. Only the links whose preventive setpoint
differs from the initial setpoint are displayed

*Format*: ```R5 ;PAR TD;TD;CONSIGNE;PRISE;```

| Quantity name      | Type | Unit    | Description                          |
|:-------------------|:-----|:--------|:-------------------------------------|
| Name of the PST    | C    |         |                                      |
| Setpoint           | R    | ° or MW | depending on the control mode of the PST |
| Setpoint HR        | R    | ° or MW | depending on the control mode of the PST |

-----------------------------
(io-table_r5b)=
**Table R5B**: results of the curative actions of the phase-shifting transformers. Only the
links whose curative setpoint differs from the preventive setpoint are displayed

*Format*: ```R5B ;INCIDENT;NOM TD; CONSIGNE;PRISE;```

| Quantity name                | Type | Unit    | Description                                                |
|:-----------------------------|:-----|:--------|:-----------------------------------------------------------|
| Contingency number           | R    |         | Reference to the numbering of [table C4](#io-table_c4)     |
| Name of the PST              | C    |         |                                                            |
| Curative setpoint of the PST | R    | ° or MW | Depending on the control mode of the PST                   |

-----------------------------
(io-table_r6)=
**Table R6**: results per HVDC line. Only the links whose preventive setpoint
differs from the initial setpoint are displayed

*Format*:
* ```R6 ; PAR LCC;NOM;TRANSIT;VM_PREV;VM_GLOBALE;TRANSIT HR;``` if the `--all-outputs` option is given when
launching METRIX simulator
* ```R6 ; PAR LCC;NOM;TRANSIT;VM_GLOBALE;``` otherwise.

| Quantity name                    | Type | Unit | Description                                                                                                                                                                         |
|:---------------------------------|:-----|:-----|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Name of the HVDC line            | C    |      |                                                                                                                                                                                     |
| Flowing power                    | R    | MW   |                                                                                                                                                                                     |
| Preventive marginal variation of the HVDC | R | m.u | Gain on the cost function of 1 MW more on the HVDC in preventive (result only present if the `--all-outputs` option is given when launching METRIX simulator)                     |
| Marginal variation of the HVDC   | R    | m.u  | Gain on the cost function of 1 MW more on the HVDC (this is the max between the preventive marginal variation and the curative marginal variations)                                 |
| Off-grid flow                    | R    | MW   | (result only present if the `--all-outputs` option is given when launching METRIX simulator)                                                                                        |

-----------------------------
(io-table_r6b)=
**Table R6B**: results per curative action of the HVDC lines. Only the links whose curative setpoint
differs from the preventive setpoint are displayed

*Format*: ```R6B ;INCIDENT;NOM HVDC;CONSIGNE;VM_CUR;```

| Quantity name                            | Type | Unit | Description                                                                                |
|:-----------------------------------------|:-----|:-----|:-------------------------------------------------------------------------------------------|
| Contingency number                       | R    |      | Reference to the numbering of [table C4](#io-table_c4)                                     |
| Name of the HVDC line                    | C    |      |                                                                                            |
| Curative setpoint of the HVDC line       | R    | MW   |                                                                                            |
| Curative marginal variation of the HVDC  | R    | m.u  | Gain on the cost function of 1 MW more on the HVDC for this contingency (not used)         |

-----------------------------
(io-table_r7)=
**Table R7**: redispatching per generation type

*Format*: ```R7 ;PAR FILIERE;TYPE;VOL BAISSE;VOL HAUSSE;VOL CUR BAISSE;VOL CUR HAUSSE;```

| Quantity name                       | Type | Unit | Description                          |
|:------------------------------------|:-----|:-----|:-------------------------------------|
| Generation type                     | C    |      |                                      |
| Preventive downward redispatching   | R    | MW   | ∑ generators of the generation type  |
| Preventive upward redispatching     | R    | MW   | ∑ generators of the generation type  |
| Curative downward redispatching     | R    | MW   | max(generators of the generation type) |
| Curative upward redispatching       | R    | MW   | max(generators of the generation type) |

-----------------------------
(io-table_r8)=
**Table R8**: results on the losses computed in active-only

*Format*: ```R8 ;PERTES;VOLUME CALCULE;TAUX UTILISE;```

| Quantity name          | Type | Unit | Description |
|:-----------------------|:-----|:-----|:------------|
| Computed loss volume   | R    | MW   |             |
| Loss rate              | R    | %    |             |

-----------------------------
(io-table_r8b)=
**Table R8B**: results on the losses per region

*Format*: ```R8B ;PERTES;REGION;VOLUME CALCULE;```

| Quantity name          | Type | Unit | Description |
|:-----------------------|:-----|:-----|:------------|
| Region or HVDC         | C    | MW   |             |
| Computed loss volume   | R    | MW   |             |

-----------------------------
(io-table_r9)=
**Table R9**: results of the objective function

*Format*: ```R9 ;FCT OBJECTIF;COUT GROUPES HORS BATTERIES;COUT BATTERIES;COUT DELESTAGE;VOLUME ECARTS N-k;VOLUME ECARTS N;COUT GRP HORS BATTERIES CUR;COUT BATTERIES CUR;COUT CONSO CUR;```

| Quantity name                        | Type | Unit | Description                                                                    |
|:-------------------------------------|:-----|:-----|:-------------------------------------------------------------------------------|
| Redispatching cost                   | R    | m.u. | Cost of the preventive generation adjustments                                  |
| Loss of load cost                    | R    | m.u. | Cost of the preventive load adjustments (i.e. of the preventive load shedding) |
| Overload volume on contingency       | R    | MW   | Sum of the threshold overloads on contingencies for the monitored network elements |
| Overload volume in N                 | R    | MW   | Sum of the overloads in N for the monitored network elements                   |
| Curative redispatching cost          | R    | m.u. | Cost of the curative generation adjustments                                    |
| Curative curtailment cost            | R    | m.u. | Cost of the curative load adjustments (i.e. of the curative load shedding)     |

-----------------------------
(io-table_r10)=
**Table R10**: results of the topological curative (selected remedial actions)

*Format*: ```R10;INCIDENT;NOM INCIDENT;NB ACTIONS;ACTION;```

| Quantity name                         | Type | Unit | Description                                                |
|:--------------------------------------|:-----|:-----|:-----------------------------------------------------------|
| Contingency number                    | I    |      | Reference to the numbering of [table C4](#io-table_c4)     |
| Contingency name                      | C    |      | Name of the initial contingency                            |
| Number of action(s) of the remedial action | I |    |                                                            |
| Name of the remedial action           | C    |      |                                                            |

*Note: The "do nothing" remedial actions are not displayed*

(io-logs)=
### Log file
There are two log files generated per simulation:
- *\<errorFilepath\>*: this file contains the logs for the log level set via the `--log-level` option
without formatting.
- *metrix\<rotation_ID\>.log*: this file contains the logs for the log level set via the
`--log-level` option with formatting (the *\<rotation_ID\>* corresponds to the ID number of the log file generated with a
certain size).

It is also possible to display the logs on the standard output via the `-p [ --print-log ]` option.

(io-lodf)=
### LODF file

A *LODF_matrix.csv* file is generated if the `--write-LODF` option is given. This file corresponds to the
LODF (Line Outage Distribution Factor) matrix of the last simulated variant.

(io-ptdf)=
### PTDF file

A *PTDF_matrix.csv* file is generated if the `--write-PTDF` option is given. This file corresponds to the
PTDF (Power Transfer Distribution Factor) matrix of the last simulated variant.

(io-mps)=
### MPS file

The MPS file (*Donnees_Probleme_Solveur.mps*) of the last problem can be generated via the `--mps-file` option.
