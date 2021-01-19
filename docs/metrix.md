# Metrix simulation

To launch a metrix simulation, you need : 
- the mapping requirements :
    - an iidm situation case
    - a timeserie store (csv)
    - a mapping groovy script using the [mapping dsl](./mapping.md#mapping-dsl)
- a metrix configuration script using the [metrix configuration dsl](#configuration-dsl) 
- (optional) [contingency script](#contingency-dsl)
- (optional) [remedial action list](#remedial-actions)


## Configuration DSL

As many of the powsybl tools scripts, this DSL is based on the groovy language. It is used to describe the general configuration
of the solver and indicate which network items are to be monitored and controlled.

### General parameters

There is a lot of tunable parameters that will be briefly describe inline. Yet the most important one is the `computationType` which
determine which kind of computation will be operated :
- LF : the LOAD FLOW mode is a basic network flow simulation, production and consumption is fixed and Metrix returns the flow on the lines of the network.
- OPF_WITHOUT_REDISPATCHING : in this mode, Metrix is allowed to use some actions (topological actions, use of pst, hvdc) to minimize constraints.
- OPF : in OPTIMAL POWER FLOW, Metrix will leverage all available actions (that of the previous mode + generator and load dispatching) to minimize constraints at best cost. If no solution is found, the program will exit with error code 1. 

All parmaters are optional :
```groovy
parameters {
  adequacyCostOffset // (0) permet de définir une translation des coûts de tous les groupes dans la phase d'équilibrage pour éviter les fausses opportunités (valeur par défaut : 0)
  analogousRemedialActionDetection // (false) detect similar topology remedial actions. Allow to improve the speed of the simulation but can hide non strictly equivalent remedial actions. 
  computationType // (LF) Simulation Mode : LF, OPF_WITHOUT_REDISPATCHING, OPF
  contingenciesProbability // (0.001) Contingency probability  
  gapVariableCost // (10) Gap variable cost
  hvdcCostPenality // (0.01) Penality cost for HVDC usage 
  lossDetailPerCountry // (false) Output the loss detail per country
  lossOfLoadCost // (13000) Cost of the load shedding
  marginalVariationsOnBranches // (false) Output the marginal variation on branches
  marginalVariationsOnHvdc // (false) Output the marginal variation on HVDC
  maxSolverTime // (0) Max time allowed to solve one micro-iteration (0 is infinite)
  nbMaxIteration // (30) Max number of micro-iterations per variation (valeur par défaut : 30)  
  nbMaxCurativeAction // (-1) Max number of remedial actions per contingency (-1 is infinite)
  nbThreatResults // (1) Number of N-k results to output  
  outagesBreakingConnexity // (false) : allow to simulate the outages that breaks network connectivity (automatically set to true if propagateBranchTripping is set)
  overloadResultsOnly // (false) Only output results on constrained items  
  preCurativeResults // (false) Use the threshold value before remedial actions (automatically activated if a threshold before action is filled in)
  propagateBranchTripping // (false) Propagate contingencies if no breaker is present
  pstCostPenality // (0.001) Penality cost of the PST usage
  redispatchingCostOffset // (0) permet de définir une translation des coûts de tous les groupes dans la phase de redispatching pour éviter les fausses opportunités (valeur par défaut : 0) 
  remedialActionsBreakingConnexity // (false) Allow remedial actions to cut pockets  
  withAdequacyResults // (false) Sortie des résultats de l'équilibrage offre-demande initial (valeur par défaut : false)
  withRedispatchingResults // (false) Sortie des résultats détaillés du redispatching préventif et curatif (valeur par défaut : false)
}
```

Since there is default value for each parameters, only useful parameters can be filled in. For instance :
```
parameters {
  computationType OPF_WITHOUT_REDISPATCHING  
  nbMaxCurativeAction 3
  preCurativeResults true
  withAdequacyResults true
  nbThreatResults 5
}
```

Note that to use a negative value, you must surround it with parenthesis, eg: `maxCurativeAction: (-1)`.

### Monitored branches and observed branches

There is a separation between "monitored branches" where Metrix will take action to enforce threshold limits and the "observed branches" where we only want the resulting flow values. 
In the LF (Load Flow) mode, there is no difference between monitored components and observed components. 

To indicate that a component should be "monitored" we have to define thresholds in MW. It can be a time series name (originating from the mapping script provided) or an fixed value (constant time series).
The `branchRatingsBaseCase` parameter will contain the threshold for the basecase (network N), the `branchRatingsBeforeCurative` and `branchRatingsOnContingency` parameters will contain the threshold to be used after a contingency (N-k) respectively before and after remedial actions. 
Threshold can be defined with a direction constraint with default being the origin -> end direction (the opposite direction is specified with the same parameter name followed by `EndOr`). Origin is the node corresponding to `voltageLevelId1` in the iidm case file.  

The syntax to define monitored/observed components is :

```groovy
branch('component_id') { // component_id is the string id of the component as found in the iidm network case file
   baseCaseFlowResults true // true if we want to "observe" the component
   maxThreatFlowResults true // true to have the maximum contingency threat and related flow result 
   contingencyFlowResults 'a', 'b'// contingency list for which we want the flow result
   branchRatingsBaseCase 'tsName' // to "monitor" the branch, can be a fixed value or a named time series (here in the example, a named time series)
   branchRatingsBaseCaseEndOr 'tsName' // to "monitor" the branch with a threshold in direction End->Origin, can be a fixed value or a named time series (here in the example, a named time series)
   branchRatingsOnContingency 'tsName' // to "monitor" the branch in N-k, can be a fixed value or a named time series (here in the example, a named time series)
   branchRatingsOnContingencyEndOr 'tsName' // to "monitor" the branch in N-k with a threshold in direction End->Origin, can be a fixed value or a named time series (here in the example, a named time series)
   branchRatingsBeforeCurative 100 // to "monitor" the branch in N-k before remedial actions, can be a fixed value or a named time series (here in the example, a fixed value)
   branchRatingsOnSpecificContingency 100 // to "monitor" the branch in N-k with specified contingencies list, can be a fixed value or a named time series (here in the example, a fixed value)
   branchRatingsBeforeCurativeOnSpecificContingency 100 // to "monitor" the branch in N-k before remedial actions with specified contingencies list, can be a fixed value or a named time series (here in the example, a fixed value)
   contingencyDetailedMarginalVariations 'a', 'b'// contingency list for which we want the marginal variation flow result
   branchAnalysisRatingsBaseCase 'tsName' // to "observe" a branch in basecase with a threshold for postprocessing results
   branchAnalysisRatingsBaseCaseEndOr 'tsName' // to "observe" a branch in basecase with a threshold in direction End->Origin
   branchAnalysisRatingsOnContingency 'tsName' // to "observe" a branch in N-1 with a threshold
   branchAnalysisRatingsOnContingencyEndOr 'tsName' // to "observe" a branch in N-1 with a threshold in direction End->Origin
}
```

Note that monitored branches (when `branchRatingsXXX` is specified) automatically provides resulting flows (when `baseCaseFlowResults` is true).
If no branch is monitored, then Metrix does not compute anything beside balance adjustment phase.
HVDC lines cannot be monitored. Flows for HVDC lines in AC emulation mode are always provided, alongside their optimized tuning. 

#### Examples

To monitor branches on basecase and N-k with a threshold of 100MW :
```groovy
branchList=[
'A.NOUL61FLEAC',
'AIGREL41ZVERV',
'AIRVAL41BRESS',
'AIRVAL41PARTH'
]

for (branchId in branchList) {
  branch(branchId) {
   baseCaseFlowResults true 
   maxThreatFlowResults true 
   branchRatingsBaseCase 100
   branchRatingsOnContingency 100
  }     
}
```

or 

```groovy
monitoredBranchList=[
'A.NOUL61FLEAC',
'AIGREL41ZVERV']

allBranchList = network.branches.collect{it.id} // retrieve all branches from network
for (branchId in monitoredBranchList) {
  if (allBranchList.contains(branchId)) {
     branch(branchId) {
        baseCaseFlowResults true 
        maxThreatFlowResults true 
     } 
  }
}
```

To gather the flow result of every 400KV lines and transformers in basecase and N-k:
```groovy
for (l in network.branches) {
  if (l.terminal1.voltageLevel.nominalV >= 380 || l.terminal2.voltageLevel.nominalV >= 380) {
   branch(l.id) {
     baseCaseFlowResults true 
     maxThreatFlowResults true
    } 
  }
}
```

or with more checks :

```groovy
branchList=[
'A.NOUL61FLEAC',
'AIGREL41ZVERV']

for (lig in branchList) {

        l = network.getBranch(lig)

        if (l == null) {
                println(lig + " doesn't exist in the network") 
                continue
        }

        if (!l.terminal1.isConnected() || !l.terminal2.isConnected()) {
                println(l.id + " is disconnected") 
                continue
        }
       
        branch(l.id) {
                baseCaseFlowResults true
                maxThreatFlowResults true
        }
}
```

To define the list of contingencies where threshold might be differents :
```groovy
contingencies {
  specificContingencies 'a', 'b', 'c' // Contingency list where special threshold will be applied
}
```
The threshold defined for this list is then defined with the keywords `branchRatingsOnSpecificContingency` or `branchRatingsBeforeCurativeOnSpecificContingency` 
```groovy
branch("line") {
   branchRatingsBaseCase 100
   branchRatingsOnContingency 100
   branchRatingsOnSpecificContingency 200 // Specific threshold
   branchRatingsBeforeCurativeOnSpecificContingency 300 // Specific threshold before remedial actions
}
```

### Generators

We can define generators which Metrix can change the target :
- In the adequacy phase to match the production and load
- In remedial actions to comply with the defined monitored branch thresholds (only in OPF mode)

For this to happen, we must define ramp up/down costs. Metrix will then choose the cheapest generator to resolve the constraints. 
There should be at least two generators for Metrix to be able to lower and increase production in order to respect power balance. 
If no generator is configured to be managed by Metrix, then all generators are implicitly managed with zero cost.\
Note that Metrix will take into account the Pmin and Pmax values of generators (which can be modified in the mapping script).
In the same way than most parameters, the value can be a fixed integer/float or a time series name.    

The syntax to define a managed generator is :

```groovy
generator(id) { // id of the generator which will be managed by Metrix 
  adequacyDownCosts 'ts_cost_down' // Cost of ramping down for the adequacy phase (here a time series name is used) 
  adequacyUpCosts 'ts_cost_up' // Cost of ramping up for the adequacy phase (here a time series name is used) 
  redispatchingDownCosts (-10) // Cost of ramping down (preventive) for the OPF simulation (here a fixed value is used)
  redispatchingUpCosts 100 // Cost of ramping up (preventive) for the OPF simulation (here a fixed value is used)
  onContingencies 'a','b' // list of contingencies where Metrix can use this generator in (curative) remedial actions
}
```

Note that if at least one generator is managed, then only defined generators will be managed to match adequacy. 
In some cases, it could result in a program failure (return code -1) where constraints cannot be resolved in OPF mode.
Also :
- Generators used for the adequacy phase are not necessarily the same used in redispatching. If `onContingency` isn't defined but `redispatchingCost` is, then the generator will be used only in preventive actions. For the generator to be fully used on preventive and curative remedial action, both of these parameters must be defined.
- Cost must always be defined for both directions. If we want to prevent a generator to ramp up or down, we can set a high prohibitive cost.
- Rules that take into account Pmax and Pmin are :
    - if the targetP is out of bounds (targetP > Pmax or targetP < Pmin) then targetP is adjusted the closest bound. This can happen when `ignore-limits` is set in the mapping script or the tool parameter.
    - during the adequacy phase, les Pmax constraints are enforced but Pmin are temporarly set to 0. It results that a (only one at most) generator can have a targetP out of its lower bound (0 < targetP < Pmin).
    - in the redispatching phase, generators with Pmin<targetP<Pmax are enforced between their bounds. If a group have an initial targetP below Pmin, the constraints will be initialTargetP < targetP < Pmax.
- In theory, the down cost of generators is negative. We should be careful not to create opportunities that would interfere with the planned production. For instance if a generator delivering at Pmin with a up cost of 15€ whereas another generator has it down cost at -25€, event in the absence of constraints, Metrix will choose to ramp up the first low cost generator and decrease the output of the second one. Beside entering fine grained realistic costs, to remedy to this issue, we could make sure that no down cost are higher (in absolute value) then up cost. We can do that using `adequacyCostOffset` and `redispatchingCostOffset` global parameters that will translate cost (we could take the maximum offset between all up and down cost for instance). This option will preserve the original values in the Metrix results though.

#### Examples

To allow the slack generator to ramp : 
```groovy
generator('slack_generator') {
  adequacyDownCosts 0
  adequacyUpCosts 0
  redispatchingDownCosts 'cost_slack_down'
  redispatchingUpCosts 'cost_slack_up'
}
```

To allow generators to ramp depending on zone or type (provided this properties exists in iidm source file):
```groovy
for (g in network.generators) {
        if (g.terminal.voltageLevel.substation.regionDI=='04' & g.genreCvg=="TAC") {
              generator(g.id) {
                redispatchingDownCosts 'cost_TAC_down_AR'
                redispatchingUpCosts 'cost_TAC_up_AR'
              }
       }
}
```

### Loads

Similarly to generators, loads can be adjusted in the OPF simulation (preventive and curative), yet only in decrease.
The cost in preventive action is fixed (default 13000€/MWh) and can be modified in the global parameters with the keyword `lossOfLoadCost`. 
Yet we can also override this value for specific loads. The specified cost will automatically be weighted with the contingency probability (default : 10^-3).  
We also can limit the max percentage of load shedding (in preventive and curative mode).

Here is the corresponding syntax :
```groovy
load(load_id) {
    preventiveSheddingPercentage 20 // shedding max percentage for preventive actions 
    preventiveSheddingCost 10000 // cost for preventive shedding action
    curativeSheddingPercentage 10 // optional shedding max percentage for curative actions
    curativeSheddingCost 40 // optional cost for curative shedding action
    onContingencies 'a', 'b' // optional contingency upon which curative action are operated 
}
```

Note that, like generators, if no load is configured then all loads can be used with 100% shedding capabilities.

### Phase-shifting transformer

Phase shifting transformers can be controlled with the following syntax (default mode is fixed) :

```groovy
phaseShifter(id) {
  controlType X // control type (see available values below)
  onContingencies 'a','b'... // optional list of contingencies upon which phase shifter control apply in curative mode
  preventiveLowerTapRange X // optional min bound range available for preventive actions
  preventiveUpperTapRange Y // optional max bound range available for curative actions
}
```

Control types :
- `FIXED_ANGLE_CONTROL` : (Default) the original phase tap is fixed
- `OPTIMIZED_ANGLE_CONTROL` : can shift to different phase tap to solve constraints
- `CONTROL_OFF` : the transformer is deactivated (phase shift is equal to 0)

Note that while the optimization will use phase in a continuous range, metrix will then find the closest phase tap in the results.

### HVDC

For HVDC the syntax is similar to that of the PST :
```groovy
hvdc(id) {
  controlType X // control type (see available values below)
  onContingencies 'a','b' // optional list of contingencies upon which phase shifter control apply in curative mode
}
```

Control types : 
- `OPTIMIZED` : can optimize the p0 target in adequacy phase and preventive (and curative if contingencies were defined)  
- `FIXED` : the target p0 is fixed

### Monitored sections

It is possible to monitor a constraint on a weighted sum of a set of branch flows :

```groovy
sectionMonitoring(id) { // Nom de la section
  maxFlowN 'ts' // threshold (can be a time series name or a fixed value) 
  branch(id1, 0.5f) // weight assigned to the flow of branch with id id1
  branch(id2, 0.5f) // wiehgt assigned to the flow of branch with id id2
  // ... 
}
```

## Contingency DSL

Contingencies are specified in an other configuration file. They can represent the loss of one or several network components.
Every contingency will be simulated.

To define a contingency :
```groovy
contingency (id) { // name of the contingency
equipments id1,id2...} // component ids that will be put out of order
```

Note that to propagate a contingency defined on branches without breaker, the global option `propagateBranchTripping` can be used to propagate the contingency on connected branches.\
Contingencies that breaks the network connection are ignored unless `outagesBreakingConnexity` global parameter is set to true. In this case, metrix will try to reach adequacy using all loads and generators equally and the results will show the cut off equipments.
HVDC can be tripped off only within a synchronized network.

### Examples

To define a list of contingency for some component list :
```groovy
components=[
  'A.NOUL61FLEAC',
  'AIGREL41ZVERV',
  'AIRVAL41BRESS',
  'AIRVAL41PARTH'
]

for (component in components) {
  contingency (component) {equipments component} // the contingency here has the same name as the affected component
}
```

and for multiple components :
```groovy
map_dual_contingencies = [
  "defaut_dbl1":['ouvrage1_id', 'ouvrage2_id'],
  "defaut_dbl2":['ouvrage3_id', 'ouvrage4_id']
]

map_dual_contingencies.each {name, components ->
  contingency (name) {equipments (*components)}
}
```

To simulate every single contingency on the 400kV network :
```groovy
for (l in network.lines) {
  if (l.terminal1.voltageLevel.nominalV >= 380) {
     contingency (l.id) {equipments l.id}
  }
}
```


## Remedial actions

Only defined topological remedial action can be used by Metrix. They are defined per contingency in an third file with the following syntax.\
On the first line is the keyword `NB` followed by the number of remedial actions defined, eg :
```text
NB;5;
```
Then each line will define a remedial action with :
```text
CONTINGENCY_NAME;ACTIONS_COUNT;EQUIPMENT1_ACTION;...;
```
where "EQUIPMENT1_ACTION" being the id of a branch or bus coupling. The default action is to open the related breakers. To close a line the `+` sign must be preppended to the branch id.

Remedial action can be limited to specific branch constraints with :
```text
CONTINGENCY_NAME | BRANCH_CONSTRAINT_1 | ... ;ACTIONS_COUNT;EQUIPMENT1_ACTION;...;
```
where "BRANCH_CONSTRAINT_1" should be the id of a [monitored component](#monitored-branches-and-observed-branches) (`onBranchRatingContingencies`).
   
Note that Metrix won't combine multiple defined remedial actions set, each line is tried independently.\
These topological actions may be combined with generators/loads/pst/... optimizations.
If several actions are equivalents, the first defined one will be used.\

### Example

```text
NB;4;
FS.BIS1 FSSV.O1 1;1;FS.BIS1_FS.BIS1_DJ_OMN;
FS.BIS1 FSSV.O1 1;1;FSSV.O1_FSSV.O1_DJ_OMN;
FS.BIS1 FSSV.O1 1;2;FS.BIS1_FS.BIS1_DJ_OMN;FSSV.O1_FSSV.O1_DJ_OMN;
FS.BIS1 FSSV.O1 1;1;FS.BIS1 FSSV.O1 2;
```

Using a lot of remedial action will increase the simulation duration, especially with numerous alternative for a same contingency. The option `analogousRemedialActionDetection` may detect equivalent remedial actions and speed up the process. They will be indicated in logs.

## Binding constraints

Lastly, we can define binding constraints between some variables so to force them to vary along. 
Each item of the binding constraint set will then vary proportionnaly to a reference variable. 

For generators, the syntax is :
```groovy
generatorsGroup("name of the set") {
  filter { generator.id == ... } // filter generators
  referenceVariable PMAX // reference variable (default is PMAX), other values can be PMIN, POBJ, PMAX_MINUS_POBJ
}
```

For loads, the only reference variable is the initial load :
```groovy
loadsGroup("name of the sed") {
  filter { load.id == ... } // filter loads
}
```

Note that when one of the item in the set reach its limit (eg. its pmax), no further variation can be made on other items. 

## Outputs

All outputs of Metrix are time series that will be stored in a single file.

### Simulation result

La chronique ERROR_CODE renseigne sur le bon déroulement du calcul Metrix. Une valeur 0 indique que le calcul de cette variante s'est déroulé normalement.

Toute autre valeur est le signe d'un problème :

    Pas de solution : retour du solveur qui indique que le problème d'optimisation ne présente pas solution (cause possible : impossibilité de baisser un groupe ou de délester une charge)
    Nombre maximum de contraintes atteint : le problème comporte trop de contraintes (ce genre d'erreur ne devrait pas arriver car ce nombre est très élevé)
    Nombre maximum de micro-itérations atteint : le nombre maximum de micro-itération a été atteint avant la fin de la résolution. Ce n'est pas bon signe, mais il peut être augmenté (voir paramètres).
    Variante ignorée: incohérence dans les données d'entrée de la variante (ex. Pconsigne non comprise entre Pmin et Pmax). Aucun calcul n'est effectué et Metrix passe à la variante suivante.  

### Global results
Résultats généraux

LOSSES : total losses (in MW) (for all synchonized zones)

LOSSES_country : total losses per country (in MW) (only available if option `lossDetailPerCountry` is enabled)

LOSSES_hvdc : total losses for HVDC (in MW) (only available if option `lossDetailPerCountry` is enabled)

OVERLOAD_BASECASE : sum of threshold overage on monitored components in base case (en MW)

OVERLOAD_OUTAGES : sum of threshold overage on monitored components for all defined contingencies (en MW)

GEN_COST : total cost of generator redispatching in preventive actions

GEN_CUR_COST : total cost of generator redispatching in curative actions

LOAD_COST : total cost of preventive load shedding

LOAD_CUR_COST : total cost of curative load shedding

basecaseLoad_branchId : load percentage (the flow divided by the threshold) 

basecaseOverload_branchId : overload flow (difference between flow and threshold if greater than threshold)

outageLoad_branchId : load percentage for N-1 (the flow divided by the N-1 threshold)

outageOverload_branchId : overload flow for N-1 (difference between flow and N-1 threshold if greater than threshold)

overallOverload_branchID : sum of basecase and outage overload values

Complément sur le calcul des pertes
Metrix utilisant l'approximation du courant continu, la tension est considérée constante sur l'ensemble du réseau. Le calcul des pertes fourni Metrix est donc une approximation effectuée a posteriori sur la base des transits actifs obtenus en fin de calcul et en supposant que les transits réactifs sont nuls.
Les pertes des quadripôles sont calculées à partir de leur puissance nominale Unom, en considérant cos(ϕ)=1, via la formule :
Pertes = R * (Transit/Unom)2

Note : pour utiliser une valeur de cos(ϕ) différente de 1, il suffit de diviser le résultat de pertes de Metrix par cos²(ϕ). 

Pour les pertes des HVDC, se référer à la note "METRIX comment ça marche". 

### Load flow results

FLOW_branchId : flow in basecase (in MW)

FLOW_branchId_contingencyId : flow for a specific contingency outage (when using the option `contingencyFlowResults` )

MAX_THREAT_index_FLOW_branchId : (index-th) maximum flow (in MW) on branch after contingencies (default only index 1 is returned but may be increased using option `nbMaxThreat`)

MAX_THREAT_index_NAME_branchId : id of the contingency that caused the maximum flow for specified index (see previous result time series)

MAX_TMP_THREAT_FLOW_branchId : maximum flow on branch after contingency and before remedial action (if option `preCurativeResults` is enabled).

MAX_TMP_THREAT_NAME_branchId : id of the contingency that caused the maximum flow after contingency and before remedial action (see previous result time series)

### Adequacy results

INIT_BAL_AREA_X : initial difference between load and production (in MW) for synchronous zone X before adequacy phase

With option `withAdequacyResults` :

INIT_BAL_GEN_generator : if not nul, the MW adjustement for the generator in the adequacy phase

INIT_BAL_LOAD_node : if not nul, load shedding for node (en MW) in the adequacy phase

With option `outagesBreakingConnexity` or `remedialActionsBreakingConnexity`:

LOST_LOAD_contingencyId : total load loss (in MW) when network connexity is broken

LOST_LOAD_BEFORE_CURATIVE_contingencyId : total load loss (in MW) when network connexity is broken (before remedial actions)

LOST_GEN_contingencyId : total production loss (in MW) when network connexity is broken

### Preventive actions results

PST_pstId : angle value for phase shift if different of initial value

PST_TAP : phase shifting tap if different of initial value

HVDC_hvdcId : p0 value (in MW) for the HVDC if different of initial value

GEN_VOL_DOWN_genType : total volume (in MW) of decreased production for generator of type genType

GEN_VOL_UP_genType : total volume (in MW) of increased production for generator of type genType

GEN_genId : variation of targetP for a generator in preventive actions (in MW) (if not nul and if option `withRedispatchingResults` is enabled)

LOAD_loadId : shedding volume (in MW) for a load in remedial actions

### Curative actions results

PST_CUR_pstId_contingencyId : angle value for phase shift if different of preventive action value

PST_CUR_TAP : phase shifting tap for the curative angle value (previous time series)

HVDC_CUR_hvdc_contingencyId : p0 value (in MW) for the HVDC if different of initial value

GEN_CUR_VOL_DOWN_genType : total volume (in MW) of decreased production on the maximal threat contingency for generator of type genType 

GEN_CUR_VOL_UP_genType : total volume (in MW) of increased production on the maximal threat contingency for generator of type genType 

GEN_CUR_generator_contingencyId : variation of production (in MW) for the generator under the specified contingency (if different of the preventive value and if option `withRedispatchingResults` is enabled)

LOAD_CUR_load_contingencyId : shedding volume (in MW) of a load  under the specified contingency (if different of the preventive value)

TOPOLOGY_contingencyId : topological remedial action chosen for the specified contingency

### Marginal variation results

The following results will be produced when option `marginalVariationsOnBranches` is enabled.

MV_branchId : theoretical effect on the cost function if the threshold were to be increase of 1 MW on the branch.

MV_branchId_contingencyId : theoretical effect on the cost function if the threshold were to be increase of 1 MW on the branch under contingency.

With option `contingencyDetailedMarginalVariations` :

MV_POW_branchId_equipmentId : load variation on generator or load "equipmentId" to obtain a 1 MW increase on branch.

MV_POW_branchId_contingencyId_equipmentId : load variation on generator or load "equipmentId" to obtain a 1 MW increase on branch after contingency.

MV_COST_branchId_equipmentId : cost variation on generator or load "equipmentId" to obtain a 1 MW increase on branch.

MV_COST_branchId_contingencyId_equipmentId : cost variation on generator or load "equipmentId" to obtain a 1 MW increase on branch after contingency.

### Binding constraints results

GEN_setId : sum of variations on the binding constraint set (in MW).

LOAD_setId : sum of load shedding variations on the binding constraint set (in MW).



