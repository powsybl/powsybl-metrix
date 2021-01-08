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
Yet we can also override this value for specific loads.  
On peut aussi le définir pour une consommation donnée (voir syntaxe ci-dessous). Le coût en curatif peut être défini en se référant à une chronique de la base ou via une constante. Ce coût sera automatiquement pondéré de la probabilité de défaillance (10^-3 par défaut, modifiable dans les paramètres).

On peut définir un % max de la consommation écrêtable en préventif et curatif. Après action, aussi bien préventive que curative, l'équilibre P=C sera respecté, il faut donc qu'il y ait au moins un groupe en mesure de baisser sa production pour que Metrix active de l'écrêtement de conso.

La syntaxe pour configurer les consommations ajustables est la suivante :

Précisions:

    Si aucune consommation n'est configurée en préventif, alors toutes les consommations du réseau peuvent être délestées à 100% en préventif .
    Les consommations pouvant bouger dans la phase d'adequacy initiale sont automatiquement les mêmes qu'en préventif. 
    Si au moins une consommation est configurée, alors seules les consommations configurées pourront être utilisées par le modèle.
    Les consommations configurées en préventif et en curatif ne sont pas nécessairement les mêmes.

Exemple :
load("FVALDI11_L") {
    preventiveSheddingPercentage 20 
    preventiveSheddingCost 10000 //si non renseigné, la valeur par défaut est utilisée
    curativeSheddingPercentage 10 // à renseigner si on souhaite du curatif
    curativeSheddingCost 40 // à renseigner si on souhaite du curatif
    onContingencies 'FS.BIS1 FSSV.O1 1' // à renseigner si on souhaite du curatif
}

### Phase-shifting transformer

Pour les TD, on peut définir un mode de pilotage (par défaut ils sont à déphasage fixe) avec la syntaxe suivante :
phaseShifter(id) {
  controlType X // type de contrôle du TD (voir ci-dessous les valeurs possibles, attention pas de guillemet)
  onContingencies 'a','b'... // optionnel, liste des noms d’incidents pour lesquels le TD peut agir en curatif
}

Les valeurs possibles pour le type de contrôle (controlType) du TD sont :
- FIXED_ANGLE_CONTROL // /* Valeur par défaut */ le déphasage de la situation initiale est inchangé
- OPTIMIZED_ANGLE_CONTROL //le déphasage est optimisé pour résoudre toutes les contraintes
- CONTROL_OFF // mise hors service du TD, équivaut à un déphasage nul

Précisions:

    Metrix gère le déphasage de manière continue et ignore donc la discrétisation des prises.

    En revanche, pour l’affichage de la prise finale, il cherchera la prise la plus proche du déphasage trouvé. Il est à noter que la prise remontée est exprimé en considérant que le numéro de la première prise est 0, ce qui ne correspond pas toujours à la définition de Convergence.
    un TD optimisé en curatif est toujours également optimisé en préventif
    Deux autres modes de contrôle existent mais ne sont pas d'usage courant: FIXED_POWER_CONTROL et OPTIMIZED_POWER_CONTROL. Le TD équivaut alors à une HVDC. Voir la documentation complète de Metrix pour plus de détails.

En optimized_angle_control, on peut demander à Metrix de restreindre en préventif l’excursion du TD autour de la prise courante par la définition d’un nombre de prises utilisables à la hausse ou à la baisse autour de la prise courante. C’est toujours un chiffre positif, et il est à noter que le TD peut toujours parcourir toute sa plage de déphasage en curatif. La syntaxe est a suivante :

phaseShifter(id){

preventiveLowerTapRange X

preventiveUpperTapRange Y

}

Si l’une ou l’autre des grandeurs n’est pas précisée, le TD pourra alors aller jusqu’à sa prise minimale/maximale.

#### Examples

```groovy
phaseShifter('FP.AND1FTDPRA11') {
  controlType OPTIMIZED_ANGLE_CONTROL // mode optimisé
  onContingencies 'FVALDI1FTDPRA11','FVALDI1FTDPRA12'... // liste des noms d’incidents pour lesquels le TD peut agir en curatif
  preventiveLowerTapRange 5
}
```


### HVDC

Dans la situation réseau, chaque HVDC a une consigne P0 qui soit régit directement le transit, soit intervient dans le transit en émulation AC (sous la forme P0 + k*DeltaTheta). Cette consigne est héritée de l'export Convergence mais peut être redéfinie dans la configuration multi-situations. La consigne est ensuite envoyée à Metrix qui laissera le P0 inchangé (en mode "FIXED") ou pourra l'optimiser (en mode "OPTIMIZED"). La syntaxe à utiliser pour le mode de pilotage des HVDCs est proche de celle des TDs :
```groovy
hvdc(id) {
  controlType X /* type de contrôle de la HVDC  (voir ci-dessous les valeurs possibles, attention pas de guillemets) */
  onContingencies 'a','b'/* ... liste des noms d’incidents pour lesquels la HVDC peut agir en curatif. */
}
```

Valeurs possibles pour le controlType de la HVDC :
- `OPTIMIZED`    /* rend optimisé en préventif, pour la phase d'équilibrage P=C, et en curatif si le paramètre "onContigencies" est renseigné */  
- `FIXED`        /* Valeur par défaut */
Sections surveillées

Pour donner une contrainte à METRIX sur la somme pondérée des transits en N d’un ensemble de branches, il est possible de définir une section surveillée : METRIX respecte alors la contrainte :

\sum (coef_i * transitN_i) \leq maxFlowN 

La syntaxe est la suivante :
sectionMonitoring(id) { // Nom de la section
  maxFlowN <valeur du seuil>
  branch(id1, <coef1>)
  branch(id2, <coef2>)
  // ... liste des autres branches : quadripôles ou HVDC
}

## Contingency DSL

Les incidents sont déclarés dans un fichier spécifique. Ils représentent la perte d'un ou plusieurs quadripôles ou groupes. Chaque incident sera pris en compte par Metrix dans la simulation.

On déclare les incidents à simuler dans ce fichier avec la syntaxe suivante :
contingency (id) { // nom de l'incident à définir comme on le souhaite
equipments id1,id2...} //ouvrage(s) à déclencher

Exemple pour définir des défauts sur une liste de noms d'ouvrages:
listeOuvrages=[
'A.NOUL61FLEAC',
'AIGREL41ZVERV',
'AIRVAL41BRESS',
'AIRVAL41PARTH']

for (ouvrage in listeOuvrages) {
contingency (ouvrage) {equipments ouvrage} //l'incident est ici nommé du nom de l'ouvrage
}

Exemple pour définir des défauts sur tout le 400kV :
for (l in network.lines) {
  if (l.terminal1.voltageLevel.nominalV >= 380) {
     contingency (l.id) {equipments l.id}
  }
}

Il est possible de définir des défauts multiples à partir d'une liste d'identifiants d'ouvrages :
map_defaut_doubles = [
  "defaut_dbl1":['ouvrage1_id', 'ouvrage2_id'],
  "defaut_dbl2":['ouvrage3_id', 'ouvrage4_id']
]

map_defaut_doubles.each {nom, ouvrages ->
  contingency (nom) {equipments (*ouvrages)}
}

Précisions :

    Pour propager un défaut sur des ouvrages sans disjoncteur (par exemple un défaut sur un piquage), il faut soit explicitement donner toutes les lignes à déclencher soit activer l'option "propagateBranchTripping" dans les paramètres généraux de Metrix.
    Par défaut, les incidents rompant la connexité du réseau sont ignorés. Pour les simuler, il faut activer l'option "outagesBreakingConnexity" dans les paramètres généraux de Metrix. La perte de consommation/production résultant est alors compensée par tous les groupes du réseau au prorata de leur Pmax et Metrix renvoie l'information de la production et de la consommation coupées.
    Les HVDC au sein d'une composante synchrone peuvent être déclarées dans les ouvrages à déclencher. En revanche, le cas des pertes de HVDC entre composantes synchrones n'est pas géré. S'il y en a, ces défauts sont ignorés.

exemple de fichier

## Remedial actions

On peut indiquer une liste de parades topologiques à Metrix. Elle sont à déclarer ainsi (le format est hérité d'ASSESS/METRIX et évoluera prochainement) :

    en première ligne : NB;X; avec X le nombre de parades qu'on va définir.
    sur chaque ligne suivante : une parade : NOM_DEFAUT ; NOMBRE_ACTIONS ; OUVRAGE_ACTION_1 ; OUVRAGE_ACTION_2 ...;. Les ouvrages peuvent être des lignes (saisir l’id de la ligne car ne traite pas pour l’instant l’id d’un DJ ligne)  ou des couplages (la manière la plus simple de récupérer le nom des couplages est de les actionner dans Convergence, de récupérer leur nom dans les traces et de rechercher l’id correspondant dans le fichier iidm). Par défaut, ce sont des ouvertures. Si on souhaite indiquer la fermeture d'un ouvrage, il faut faire précéder le nom de l'ouvrage d'un +.

Il faut bien noter les points suivants :

    Metrix n'invente pas de parade, il faut lui donner une liste dans laquelle il choisira
    Une parade est propre à un incident. Si elle fonctionne sur plusieurs incidents, il faut la déclarer pour chacun d'eux.
    Il n'y a pas d'obligation d'utiliser une parade (de type si contrainte sur X alors Y), Metrix choisit d'activer une parade si et seulement si elle est utile
    Metrix ne combine pas de parades. Si une parade consiste en plusieurs actions il faut la définir comme une parade à part entière
    Une parade ne peut pas consister à changer le panachage des postes (pas d'action sur les sectionneurs)
    A impact équivalent, Metrix retient la première parade dans la liste fournie
    Les parades consistant à faire des ajustements sur des groupes ou des consommations sont à modéliser avec des ajustements de groupes ou consommations et peuvent être combinées avec des parades topologiques

Exemple de fichier de parades :
NB;4;
FS.BIS1 FSSV.O1 1;1;FS.BIS1_FS.BIS1_DJ_OMN;
FS.BIS1 FSSV.O1 1;1;FSSV.O1_FSSV.O1_DJ_OMN;
FS.BIS1 FSSV.O1 1;2;FS.BIS1_FS.BIS1_DJ_OMN;FSSV.O1_FSSV.O1_DJ_OMN;
FS.BIS1 FSSV.O1 1;1;FS.BIS1 FSSV.O1 2;
Restreindre l'activation d'une parade à une contrainte

Il est possible de conditionner l'activation d'une parade à la présence d'une contrainte sur des ouvrages spécifiques.

La parade doit alors être défini de la façon suivante :
NOM_DEFAUT | OUVRAGE_CONTRAINTE_1 | OUVRAGE_CONTRAINTE_2 ... ; NOMBRE_ACTIONS ; OUVRAGE_ACTION_1 ; OUVRAGE_ACTION_2 ...;

Les contraintes doivent bien évidemment être des ouvrages surveillés sur incident : branchRatingOnContingencies.
L'utilisation de parades topologiques peut considérablement allonger les temps de calcul. En particulier s'il y a un grand nombre de parades pour un même incident et que ces parades ont des actions sensiblement équivalentes. L'option analogousRemedialActionDetection permet dans certains cas de détecter si une parade est équivalente à une précédente dans la liste. Dans ce cas, la seconde parade est ignorée et un trace "=> Contraintes equivalentes :" est visible dans les logs Metrix.
Variables couplées

Il est possible de définir des liens entre certaines variables préventives du problème pour qu'elles varient de manière conjointe. Cela peut être utilisé pour forcer des groupes ou des consommations à agir dans le même sens.

Chaque élément du regroupement varie alors proportionnellement à une variable de référence.

Pour les consommations, la variable de référence est toujours la charge initiale. Le délestage de chaque consommation appartenant au regroupement sera donc proportionnel à sa charge intiiale.

La syntaxe est la suivante :
loadsGroup("nom du regroupement") {
  filter { load.id == ... } //un filtre comme pour le mapping
}

Pour les générateurs, la variable de référence peut être, au choix : la puissance maximale, la puissance minimale, la puissance de consigne initiale ou la marge à la hausse (puissance maximale - puissance de consigne).

La syntaxe est la suivante :
generatorsGroup("nom du regroupement") {
  filter { generator.id == ... } //un filtre comme pour le mapping
  referenceVariable PMAX // c’est la valeur par défaut, les autres possibilités sont PMIN, POBJ, PMAX_MINUS_POBJ
}
Des chroniques correspondant aux variations de ces regroupements sont alors disponibles dans les sorties.
Attention, la variation de tous les éléments de l'ensemble est limitée par le premier élément de l'ensemble qui atteint sa limite.

## Outputs

Les sorties de Metrix sont représentées sous forme de chroniques visualisables ou exportables en csv à partir de l'objet "simulation Metrix" correspondant.

exemple de csv de sorties d'un lancement OPF 
Résultats sur le calcul

La chronique ERROR_CODE renseigne sur le bon déroulement du calcul Metrix. Une valeur 0 indique que le calcul de cette variante s'est déroulé normalement.

Toute autre valeur est le signe d'un problème :

    Pas de solution : retour du solveur qui indique que le problème d'optimisation ne présente pas solution (cause possible : impossibilité de baisser un groupe ou de délester une charge)
    Nombre maximum de contraintes atteint : le problème comporte trop de contraintes (ce genre d'erreur ne devrait pas arriver car ce nombre est très élevé)
    Nombre maximum de micro-itérations atteint : le nombre maximum de micro-itération a été atteint avant la fin de la résolution. Ce n'est pas bon signe, mais il peut être augmenté (voir paramètres).
    Variante ignorée: incohérence dans les données d'entrée de la variante (ex. Pconsigne non comprise entre Pmin et Pmax). Aucun calcul n'est effectué et Metrix passe à la variante suivante.  

Résultats généraux

LOSSES : estimation (en MW) des pertes totales (toutes zones synchrones)

LOSSES_country : estimation (en MW) des pertes du pays country (disponible si l'option  lossDetailPerCountry  est activée)

LOSSES_hvdc : estimation (en MW) des pertes de la liaison à courant continu hvdc (disponible si l'option  lossDetailPerCountry est activée)

OVERLOAD_BASECASE : somme de tous les dépassements de seuils en N sur tous les ouvrages surveillés (en MW)

OVERLOAD_OUTAGES : somme de tous les dépassements de seuils sur tous les ouvrages surveillés et pour tous les incidents (en MW)

GEN_COST : coût total du redispatching des groupes en préventif

GEN_CUR_COST : coût total du redispatching des groupes en curatif

LOAD_COST : coût total du délestage préventif. Convention de signe : Un signe positif indique une baisse de consommation. Un délestage négatif est possible pour les seules consommations négatives.

LOAD_CUR_COST : coût total du délestage curatif. Convention de signe : Un signe positif indique une baisse de consommation. Un délestage négatif est possible pour les seules consommations négatives.

Les chroniques suivantes sont également calculées à partir des sorties Metrix pour préparer la synthèse des résultats :

basecaseLoad_ouvrage : transit en N sur l'ouvrage ouvrage divisé par la valeur absolue du seuil en N sur cet ouvrage

basecaseOverload_ouvrage : transit en N sur l'ouvrage ouvrage au-delà du seuil en N si transit N > seuil N

outageLoad_ouvrage : transit en N-1 sur l'ouvrage ouvrage divisé par la valeur absolue du seuil en N-1 sur cet ouvrage

outageOverload_ouvrage : transit en N-1 sur l'ouvrage ouvrage au-delà du seuil en N-1 si transit N-1 > seuil N-1

overallOverload_ouvrage : abs(basecaseOverload) + abs(outageOverload) (uniquement pour déterminer le nombre d’heures en contraintes)
LOAD_COST et LOAD_CUR_COST (resp. GEN) sont les sommes des coûts des variables de consommation (resp. production) que le modèle peut ajuster. Les effets liés à l'activation de l'option outagesBreakingConnexity ou remedialActionsBreakingConnexity ne sont pas pris en compte dans ces variables. 

Complément sur le calcul des pertes
Metrix utilisant l'approximation du courant continu, la tension est considérée constante sur l'ensemble du réseau. Le calcul des pertes fourni Metrix est donc une approximation effectuée a posteriori sur la base des transits actifs obtenus en fin de calcul et en supposant que les transits réactifs sont nuls.
Les pertes des quadripôles sont calculées à partir de leur puissance nominale Unom, en considérant cos(ϕ)=1, via la formule :
Pertes = R * (Transit/Unom)2

Note : pour utiliser une valeur de cos(ϕ) différente de 1, il suffit de diviser le résultat de pertes de Metrix par cos²(ϕ). 

Pour les pertes des HVDC, se référer à la note "METRIX comment ça marche". 
Résultats sur les transits

FLOW_ligne : transit en N (en MW) sur l'ouvrage ligne

FLOW_ligne_incident : transit (en MW) sur l'ouvrage ligne pour le défaut incident. (lié à l'utilisation de l'option contingencyFlowResults )

MAX_THREAT_indice_FLOW_ligne : transit maximal (en MW) numéro indice sur l'ouvrage ligne après incident (par défaut un seul transit maximal est renseigné par ouvrage mais ce nombre peut être augmenté via l'option nbMaxThreat)

MAX_THREAT_indice_NAME_ligne : nom du défaut correspondant au transit maximal numéro indice pour l'ouvrage ligne (cf. chronique précédente).

MAX_TMP_THREAT_FLOW_ligne : transit maximal sur l'ouvrage ligne après incident et avant toute manœuvre curative (renseigné uniquement si l'option  preCurativeResults est activée).

MAX_TMP_THREAT_NAME_ligne : nom du défaut correspondant au transit maximal avant manœuvre sur l'ouvrage ligne (cf. chronique précédente).
Résultats sur l'équilibre offre-demande

INIT_BAL_AREA_X : écart entre production et consommation (MW) pour la zone synchrone X avant la phase d'équilibrage.

(Les résultats ci-dessous sont liés à l'activation de l'option withAdequacyResults)

INIT_BAL_GEN_groupe : modification initiale de la puissance de consigne du groupe (en MW) pour réaliser l'équilibre P=C (renseigné si non nul).

INIT_BAL_LOAD_nœud : délestage initial de la consommation du nœud (en MW) pour réaliser l'équilibre P=C (renseigné si non nul).

(Les résultats ci-dessous sont liés à l'activation de l'option outagesBreakingConnexity ou remedialActionsBreakingConnexity)

LOST_LOAD_defaut : volume (en MW) de consommation perdu sur l'incident défaut qui rompt la connexité (i.e. perte d'une poche). Convention de signe : une valeur positive indique une perte de consommation.

LOST_LOAD_BEFORE_CURATIVE_defaut : volume (en MW) de consommation perdu sur l'incident défaut qui rompt la connexité (i.e. perte d'une poche) avant actions curatives. Convention de signe : une valeur positive indique une perte de consommation.

LOST_GEN_defaut : volume (en MW) de production perdu sur  l'incident défaut qui rompt la connexité. Convention de signe : une valeur positive indique une perte de consommation.
Si une parade topologique permet de récupérer une partie de la consommation ou de la production perdue, le volume perdu avant application de la parade est renseigné sous le nom de l'incident, préfixé par "BEFORE_CURATIVE_" et le volume perdu après application de la parade est renseigné sous le nom de l'incident.
Résultats sur le préventif

PST_déphaseur : valeur (en degré) du déphasage préventif sur le déphaseur (renseigné si différent de la valeur initiale).

PST_TAP : valeur de la prise finale en préventif, qui correspond à la prise la plus proche par rapport au déphasage préventif du PST.

HVDC_hvdc : valeur (en MW) de la consigne en N sur la hvdc (renseigné si différent de la valeur initiale).

GEN_VOL_DOWN_typeGrp: volume total de redispatching à la baisse (en MW) de tous les groupes de la filière typeGrp. Convention de signe : valeur négative correspondant à une baisse de production

GEN_VOL_UP_typeGrp : volume total de redispatching à la hausse (en MW) de tous les groupes de la filière typeGrp. Convention de signe : valeur positive correspondant à une hausse de production

GEN_grp : modification en préventif de la puissance de consigne du groupe grp (en MW) pour la résolution de contraintes (renseigné si différent de 0 et si l'option withRedispatchingResults est activée). Convention de signe : une valeur positive indique une hausse de production (delta de sa consigne de production).

LOAD_load : valeur du délestage préventif sur la load (renseigné si non nul) caractérisée par son id. Convention de signe : Un signe positif indique une baisse de consommation. Un délestage négatif est possible pour les seules consommations négatives.
Résultats sur le curatif

PST_CUR_déphaseur_défaut : valeur (en degré) du déphasage curatif du déphaseur pour le défaut (angle résultant des actions cumulées préventif + curatif, renseigné si différent de la valeur préventive).

PST_CUR_TAP : valeur de la prise finale en curatif, qui correspond à la prise la plus proche par rapport au déphasage curatif du PST.

HVDC_CUR_hvdc_défaut : valeur (en MW) de la consigne curative sur la hvdc  pour le défaut (renseigné si différent de la valeur initiale).

GEN_CUR_VOL_DOWN_typeGrp : volume total de redispatching à la baisse (en MW) sur tous les groupes de la filière typeGrp observé sur le pas de temps pour l'incident générant le volume maximal pour cette fillière. Convention de signe : valeur négative correspondant à une baisse de production. 

GEN_CUR_VOL_UP_typeGrp : volume total de redispatching à la hausse (en MW) de tous les groupes de la filière typeGrp observé sur le pas de temps pour l'incident générant le volume maximal pour cette fillière. Convention de signe : valeur positive correspondant à une hausse de production. 
Warning concernant GEN_CUR_VOL_DOWN_typeGrp et GEN_CUR_VOL_UP_typeGrp: les valeurs pour les différentes fillières peuvent correspondre à des défauts différents sur un même pas de temps et une même filiaire peut avoir à la fois un volume à la hausse et à la baisse correspondant à deux défauts différents. Pour étudier ces volumes aggrégés en curratifs sur un défaut particuliers, il est par exemple possible de limiter la liste de défaut à celui souhaité dans le paramétrage de la simulation. 

GEN_CUR_groupe_défaut : valeur (en MW) de l'ajustement de production curatif du groupe pour le défaut (renseigné si différent de la valeur préventive et si l'option withRedispatchingResults est activée). Convention de signe : une valeur positive indique une hausse de production (delta de sa consigne de production).

LOAD_CUR_load_défaut : valeur (en MW) de l'effacement curatif d'une consommation de la load pour le défaut (renseigné si différent de la valeur préventive). Convention de signe : Un signe positif indique une hausse de consommation.

TOPOLOGY_défaut : parade topologique sélectionnée pour le défaut.
Résultats sur les variations marginales
Les variations marginales sont une aide à l'analyse et l'interprétation des résultats en mode OPF (i.e. quand il n'y a plus d'ouvrage en contrainte dans la solution).
Elles permettent en effet de connaitre les couples "incident / ouvrage en contrainte" qui limitent la solution.
Plus une variation marginale est élevée, plus la contrainte correspondante est coûteuse pour la solution.
L'option contingencyDetailedMarginalVariations permet de connaitre le détail des groupes (et consommations) qui sont utilisés pour résoudre une contrainte particulière.

(Les résultats ci-dessous sont liés à l'activation de l'option marginalVariationsOnBranches)

MV_branche : impact (théorique) sur la fonction coût si on pouvait augmenter de 1 MW sur le seuil en N de l'ouvrage branche.

MV_branche_défaut : impact (théorique) sur la fonction coût si on pouvait augmenter de 1 MW sur le seuil après défaut de l'ouvrage branche en raison de l'incident défaut.

(Les résultats ci-dessous sont liés à l'activation de l'option contingencyDetailedMarginalVariations)

MV_POW_branche_élément : puissance à ajuster en N sur le groupe (ou la consommation) élément pour obtenir un gain de 1 MW sur le transit de l'ouvrage branche.

MV_POW_branche_défaut_élément : puissance à ajuster sur le groupe (ou la consommation) élément, suite à l'incident défaut, pour obtenir un gain de 1 MW sur le transit de l'ouvrage branche.

MV_COST_branche_élément : coût de l'ajustement en N sur le groupe (ou la consommation) élément pour obtenir un gain de 1 MW sur le transit de l'ouvrage branche.

MV_COST_branche_défaut_élément : coût de l'ajustement sur le groupe (ou la consommation) élément, suite à l'incident défaut, pour obtenir un gain de 1 MW sur le transit de l'ouvrage branche.
Résultats sur les variables couplées

GEN_regroupement : somme des modifications des puissances de consigne de l'ensemble des groupes du regroupement (en MW).

LOAD_regroupement : somme des délestages réalisés sur les consommations du regroupement (en MW).



