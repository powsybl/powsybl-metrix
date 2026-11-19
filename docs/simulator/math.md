<style>
r { color: Red }
o { color: Orange }
g { color: Green }
y { color: yellow}
b { color: blue}
</style>

# Mathematical model

## Variables

(inc_var)=
### Contingencies
The contingencies are listed in the container $INCIDENT$. We denote by $inc$ an element of this list.
A contingency can cause:
- the opening of a certain number of lines $DEFAUTQUAD_{inc}$;
- the opening of a certain number of LCCs $DEFAUTLCC_{inc}$;
- and/or the shutdown of some generators $DEFAUTGRP_{inc}$.

At the same time, in order to fix this contingency, some curative means may be used. They are stored in
the list $CURATIF_{inc}$ and contain generators, loads, PSTs, HVDCs and/or remedial actions.

Finally, this contingency has a certain probability of occurring, denoted $proba_{inc}$. This contingency will always be applied
to the network during the METRIX simulation, but this probability makes it possible to weight the cost of using curative
actions.

(prod_var)=
### Generators
The generators of the network modeled in METRIX are stored in a list $GROUPE$.
The production of a generator can be modified in the *Adequacy phase*, in the *Redispatching phase*, in both or in neither. In the equations, we
will only consider the generators that can be modified in all circumstances.

#### Definition of the variables
Let $i$ be a generator of $GROUPE$.

Its production is defined by an initial value $P_i^0$ and minimum and maximum values $P_{i}^{min}$ and
$P_{i}^{max}$. There are two values for the minimum power: $P_{i_{ad}}^{min}$ and $P_{i_{red}}^{min}$,
representing the minimums for the *Adequacy* and the *Redispatching* such that:

$$
P_{i_{ad}}^{min} = min(0; P_{i_{red}}^{min})
$$

The power variation of generator $i$ is described in the *Adequacy* and *Redispatching* problems in N by
two variables: $p_i^+$ and $p_i^-$, **both positive**, and representing respectively the increase and the decrease
of the production of generator $i$. Likewise, for each contingency $inc$, we have two variables $p_{i_{inc}}^{-}$
and $p_{i_{inc}}^{+}$ representing the power variation in curative on this contingency $inc$, and a
boolean variable activating the curative action on the contingency $actP_{i}^{inc}$. These three curative variables are thus linked
as follows:

$$
p_{i_{inc}}^{-} + p_{i_{inc}}^{+} \leq M \cdot actP_{i}^{inc}
$$

With $M$ a very large value.

#### Domains of definition of the variables
The domains of definition for the variables related to production are:
 - *Adequacy phase* (EOD - supply-demand balance):

 $$
\begin{aligned}
 \text{Si }P_{i_{ad}}^{min}\geq 0\text{ et }P_{i_{ad}}^{min} > P_{i}^{0} \Rightarrow &
 \begin{cases}
    p_{i}^{-} = 0\\
    p_{i}^{+} \in [P_{i_{ad}}^{min} - P_{i}^{0}; P_{i}^{max} - P_{i}^{0}]
 \end{cases}\\
 \text{Sinon }&
 \begin{cases}
    p_{i}^{-} \in [0; P_{i}^{0} - P_{i_{ad}}^{min}]\\
    p_{i}^{+} \in [0; P_{i}^{max} - P_{i}^{0}]
 \end{cases}
\end{aligned}
 $$

 <o>YJ: why $P_{i_{ad}}^{min}\geq 0$ ??</o>

 - *Redispatching phase* in preventive:

**The variables and parameters are modified from the result of the *Adequacy phase*** in order to integrate them into the
*Redispatching phase*: $P_i^0 = p_i^+ - p_i^- + P_i^0$. In other words, the preventive setpoint
power takes the result of the *Adequacy phase* as its value, and the upward or downward production variables
are then reset (N.B.: **this behavior is specific to generators**). The bounds are then
as follows:

$$
\begin{aligned}
p_{i}^{+} &\in [0; P_{i}^{max} - P_{i}^{0}]\\
p_{i}^{-} &
 \begin{cases}
    \in [0; P_{i}^{0} - P_{i_{red}}^{min}]\text{ si }P_{i}^{0}\geq P_{i}^{min}\\
    = 0\text{ sinon}
 \end{cases}
 \end{aligned}
$$

 - *Redispatching phase* in curative:

$$
\begin{cases}
p_{i}^{+} - p_{i}^{-} + p_{i_{inc}}^{+} \leq P_{i}^{max} - P_{i}^{0}\\
min(0; P_{i_{red}}^{min} - P_{i}^{0}) \leq p_{i}^{+} - p_{i}^{-} - p_{i_{inc}}^{-}
\end{cases}
$$

#### Costs

In addition, modifying the production variables of the generators has a cost. More precisely, we associate
costs with the increase and the decrease in the *Adequacy phase*: $\Gamma_{i_{ad}}^{-}$ and $\Gamma_{i_{ad}}^{+}$; and in the
*Redispatching phase*: $\Gamma_{i_{red}}^{-}$ and $\Gamma_{i_{red}}^{+}$.

#### Membership of a synchronous area

Each generator is attached to a single node of the network, which itself belongs to a single synchronous area. For each
synchronous area $zc \in ZC$, we denote by $GROUPE_{zc}$ the set of generators belonging to this synchronous area.

(coup_grp_ctr)=
#### Generator coupling constraint

During the *Redispatching phase*, we can define in the network a set of generators whose production in N
must vary proportionally. Let $COUPLAGE^{GRP}$ be the set of grouped couplings. In order to define this
variation, we define a reference value for each of the generators of this set. This reference
value can be $P^{max}$, $P^{min}$, $P^{0}$ or $P^{max} - P^{0}$. It is denoted $P^{ref}$.

Let $i_0$ be the first generator of this set. $\forall i \in COUPLAGE^{GRP}$, $i \not = i_0$:

$$
\frac{p_i^+ - p_i^-}{P_i^{ref}} = \frac{p_{i_0}^+ - p_{i_0}^-}{P_{i_0}^{ref}}
$$

(lim_cur_ctr)=
#### Curative changes limitation constraint
This constraint is **optional**; the METRIX data must indicate that we wish to apply it (with
the *LimiteCurGroupe* parameter). This constraint limits the cumulative decrease in production in curative
on a contingency $inc$:

$$
\sum_{i\in GROUPE} p_{inc}^{-} \leq LimiteCurGroupe
$$

**Note**: the constraint only considers downward variations in order to take load
shedding into account.

<r>Summary of notations:</r>

**Data**

Sets: $GROUPE$, $COUPLAGE^{GRP}$, $GROUPE_{zc}$

Values:
 - LimiteCurGroupe
 - $\forall i \in GROUPE : P_{i}^{max}, P_{i_{ad}}^{min}, P_{i_{red}}^{min}, P_{i}^{ref}, \Gamma_{i_{ad}}^{-}, \Gamma_{i_{ad}}^{+}, \Gamma_{i_{red}}^{-}, \Gamma_{i_{red}}^{+}$

**Variables**

$\forall i \in GROUPE, \forall inc \in INCIDENT : p_{i}^{+}, p_{i}^{-}, p_{i_{inc}}^{+}, p_{i_{inc}}^{-}, actP_{i}^{inc}$

**Constraints**
- Domain of definition constraints
- Generator coupling constraint
- Curative changes limitation constraint

(conso_var)=
### Loads

The consumption centers (called loads) are in the list $CONSO$.
The power used by a load can be modified in the *Adequacy* as well as in the *Redispatching phase*.

#### Definition of the variables

Let $i$ be a load of the set $CONSO$.

$C_{i}^{0}$ defines the power consumed in the initial state; load shedding is represented in the *Adequacy phase* and in
preventive by the variable $c_i^{-}$ and in curative on a contingency $inc$ by the variable $c_{i_{inc}}^{-}$ and the
boolean activation variable $actC_i^{inc}$. Such that:

$$
c_{i_{inc}}^{-} \leq M \cdot actC_{i}^{inc}
$$

With $M$ a very high value.

#### Domains of definition of the variables

To set the maximum value of the load shedding, a percentage is used. It is defined by the parameters $\Phi_i$
in the *Adequacy phase* and in preventive, and $\Phi_{i_{cur}}$ in curative. Thus, we obtain the following bounds:
 - For the *Adequacy phase* and the preventive:

 $$
 c_{i}^{-} \in
 \begin{cases}
 [0; \Phi_i \cdot C_i^0]\text{, si }C_i^{0} \geq 0\\
 [\Phi_i \cdot C_i^0; 0]\text{, sinon. }
 \end{cases}
 $$

 - For the curative:

 $$
 \forall inc \in INCIDENT, c_{i_{inc}}^{-} \in [0; max(0; C_i^{0} \cdot \Phi_{i_{cur}})]
 $$

#### Definition of the costs

For each load, we define a cost for the *Adequacy phase* and the preventive $\Gamma_i^{conso}$ and another one
for the curative $\Gamma_{i_{cur}}^{conso}$.

#### Membership of a synchronous area

Each load is attached to a single node of the network, which itself is part of a single synchronous area. For
each synchronous area $zc \in ZC$, we denote by $CONSO_{zc}$ the loads belonging to this synchronous area.

(coup_conso_ctr)=
#### Load coupling constraint
As for generators, loads can be coupled so that their load shedding in N is proportional.
Let $COUPLAGE_{CONSO}$ be the list of these coupled loads.

Let i$_0$ be the first load of this list. $\forall i \in COUPLAGE_{CONSO}, i \neq i_0$:

$$
\frac{c_i^{-}}{C_i^{0}} = \frac{c_{i_0}^{-}}{C_{i_0}^{0}}
$$

<r>Summary of notations:</r>

**Data**

Sets: $CONSO$, $COUPLAGE_{CONSO}$, $CONSO_{zc}$

Values: $\forall i \in CONSO : C_i^{0}, \Phi_i, \Phi_{i_{cur}}$

**Variables**

$$
\forall i \in CONSO, \forall inc \in INCIDENT : c_i^{-}, c_{i_{inc}}^{-}, actC_i^{inc}
$$

**Constraints**
Load coupling constraint.

(td_var)=
### Phase tap changer (PST)

The PSTs are stored in a list $TD$. Let $i$ be an element of this list.

#### Min and max values of the PSTs

In METRIX, the phase shift is not done by moving from one tap to another in a discrete way, but with continuous
variables evolving between a minimum power and a maximum power. The lists of phase shift taps of a PST are
nevertheless used to set these minimum and maximum powers, denoted $TD_i^{min}$ and $TD_i^{max}$.

These values may be taken from the input data if the upward and downward tap change
bounds are not defined.

#### Definition of the variables

The setpoint power transmitted by $i$ from node *Or* to node *Nf* is represented by the parameter $TD^{0}_i$.
The variation of this power in preventive is represented by the **positive** variables $td^{+}_i$ for the
increase and $td^{-}_i$ for the decrease. Likewise, if the PST is allowed to act in curative on the contingency $inc$, we use
the **positive** curative variables $td^{+}_{i_{inc}}$ and $td^{-}_{i_{inc}}$ and the boolean variable
$actTD_{i}^{inc}$. These three variables are linked by the following constraint:

$$
td^{+}_{i_{inc}} + td^{-}_{i_{inc}}\leq M \cdot actTD_{i}^{inc}
$$

With $M$ a very high value.

#### Domains of definition of the variables

Each PST can be controlled in four different ways:
 - With imposed power
 - With optimized power
 - With imposed angle
 - With optimized angle

Imposed controls imply that the PST will always exchange the same power between the two nodes *Or* and *Nf*:
$td^{+}_{i} = td^{-}_{i} = td^{+}_{i_{inc}} = td^{-}_{i_{inc}} = 0$.

For power controls (imposed and optimized), the *quadFictif* is open: only the power of the PST will be
transmitted between the nodes *Or* and *Nf*.

For optimized control (angle or power), the parameters $TD_i^{min}$ and $TD_i^{max}$ defined
previously are used such that:

$$
0 \leq td^{+}_{i} \leq max(TD_i^{max} - TD_i^{0}; 0) \text{ et } 0 \leq td^{-}_{i} \leq max(TD_i^{0} - TD_i^{min}; 0)\\
\forall inc \in INCIDENT:\\
0 \leq td^{+}_{i_{inc}} \leq max(TD_i^{max} - TD_i^{min}; 0) \text{ et } 0 \leq td^{-}_{i_{inc}} \leq max(TD_i^{max} - TD_i^{min}; 0)
$$

For the curative bounds, the following constraints are added:

$$
\forall inc \in INCIDENT:\\
td^{+}_{i} + td^{+}_{i_{inc}} \leq max(TD^{max}_{i} - TD^{0}_i; 0)\\
td^{-}_{i} + td^{-}_{i_{inc}} \leq max(TD^{0}_{i} - TD^{min}_i; 0)
$$

#### Domains of definition of the variables
Regarding the cost, the PSTs all have the same preventive usage cost $\Gamma^{TD}$, defined in the
METRIX parameters. In curative, this cost is weighted by the probability of occurrence of the contingency, and therefore
depends on each contingency.

**In summary**: PSTs make it possible to modify the flow on lines to avoid overloads, without modifying
the supply-demand balance.

<r>Summary of notations:</r>

**Data**

Sets: $TD$

Values: $TD^{0}_i$, $TD^{max}_i$, $TD^{min}_i$, $\Gamma^{TD}$

**Variables**

$$
\forall i \in TD, \forall inc \in INCIDENT : td^{+}_{i}, td^{-}_{i}, td^{+}_{i_{inc}}, td^{-}_{i_{inc}}, actTD_{i}^{inc}
$$

(lcc_var)=
### HVDC lines (LCC)

The LCCs of the network simulated by METRIX are stored in the set $LCC$.

#### Variables and bounds

Let $i \in LCC$, we denote:
- $Lcc_i^{0}$ its setpoint power;
- $Lcc_i^{min}$ its minimum power;
- and $Lcc_i^{max}$ its maximum power.

The variations of $i$ are represented:
- in preventive by the **positive** variables $lcc_i^{+}$ and $lcc_i^{-}$;
- and in curative by the **positive** variables $lcc_{i_{inc}}^{+}$ and $lcc_{i_{inc}}^{-}$ as well as by the
boolean variable $actLCC_{i}^{inc}$.

These three variables are linked by the following constraint:

$$
lcc_{i_{inc}}^{+} + lcc_{i_{inc}}^{-} \leq M \cdot actLCC_{i}^{inc}
$$

With $M$ a very large value.

The variables representing the power variations of the LCC have the following bounds:
- If the control is optimized power or optimized AC emulation:

$$
lcc_i^+ \in [0; max(Lcc_i^{max}-Lcc_i^{0}; 0)]\text{ et }lcc_i^- \in [0; max(Lcc_i^{0}-Lcc_i^{min}; 0)]
$$

- Otherwise, it is imposed:

$$
lcc_i^{+} = lcc_i^{-} = 0
$$

In curative, the bounds for the optimized controls are as follows:

$$
lcc_i^{+} + lcc_{i_{inc}}^{+} \leq Lcc_i^{max} - Lcc_i^{0}\\
lcc_i^{-} + lcc_{i_{inc}}^{-} \leq Lcc_i^{0} - Lcc_i^{min}
$$

#### Definition of the costs
Regarding the cost, the LCCs all have the same preventive usage cost $\Gamma^{LCC}$, defined in
the METRIX parameters. In curative, this cost is weighted by the probability of occurrence of the contingency, and therefore depends
on each contingency.

#### Link between synchronous areas
Let $\forall zc \in ZC$, $LCC_{zc}^+$ be the set of LCCs carrying power from the synchronous area $zc$ to
another synchronous area. Likewise, $LCC_{zc}^-$ is the set of LCCs drawing power from the synchronous area
$zc$ to send it to another synchronous area.

#### LCC in AC emulation
The fictitious PST can never act in preventive: $\forall inc \in INCIDENT$, an activation constraint links the
preventive variables of the PST to the curative activation variable of the same PST on this contingency ($actLCC_{inc}^i$); and
this activation variable is fixed at $0$, preventing any variation of the preventive variables. Later, if
the activation variable is unblocked, the preventive variables will only be used by the problem in the
computation of the flow on the quadripole, and in any case the associated coefficient will be zero.
As for the curative variables (linear and boolean), they are linked by an activation constraint. Let
$i \in TD, inc \in INCIDENT$:

$$
td_{i_{inc}}^+ + td_{i_{inc}}^- \leq M \cdot actLCC_{inc}^i
$$

The activation variable is fixed at $0$, unless the quadripole *quad0* is constrained. In that case, the
variable is free to change its value.

This fictitious PST will have a zero usage cost. It will have the same preventive bounds as normal PSTs, with
max and min powers coming from the LCC in AC emulation. It will also have the following curative bounds:

$$
0 \leq td_{i_{inc}}^+ \leq td_i^{max}\text{ et }0\leq td_{i_{in}}^- \leq -td_i^{min}
$$

The fictitious PSTs are stored in the set $TDF$.

<r>Summary of notations:</r>

**Data**

Sets: $LCC$, $TDF$, $\forall zc \in ZC: LCC_{zc}^+, LCC_{zc}^-$

Values: $\forall i \in LCC : Lcc_i^{0}, Lcc_i^{max}, Lcc_i^{min}, \Gamma^{LCC}$

**Variables**

$$
\forall i \in LCC, \forall inc \in INCIDENT: lcc_{i}^{+}, lcc_{i}^{-}, lcc_{i_{inc}}^{+}, lcc_{i_{inc}}^{-}
$$

(parades_var)=
### Remedial actions

The remedial actions are listed in $PARADE$. Let $prd \in PARADE$. Each remedial action is associated with a single contingency
$inc \in INCIDENT$.

$prd$ has as parameters the list of couplings it closes ($COUPLAGEFERMER_{prd}$) and those it opens
($COUPLAGEOUVRIR_{prd}$).

We define, for a contingency $inc$ and a remedial action $prd$, the activation variable of the remedial action on this contingency
$actPRD_{prd}^{inc}$, with cost $\Gamma^{PRD}$ in the objective function.

(unicity_prd_ctr)=
#### Remedial action uniqueness constraint

For each contingency, a single remedial action is applicable, $\forall inc \in INDICENT$:

$$
\sum_{prd \in PARADE \cap CURATIF_{inc}} actPRD_{prd}^{inc} \leq 1
$$

In the code, the inequality of this constraint is transformed into an equality, by introducing
a “Do Nothing” remedial action for each contingency having topological remedial actions as curative actions. This
remedial action has no effect on the network.

(usage_prd_ctr)=
#### Remedial action usage constraint

The activation of a remedial action $prd$ can be prevented if there is no unconstrained line among a certain set of lines.
Let $QUADNECESSAIRES_{prd}$ be this set.

Let $QUADENCONTRAINTE_{inc}$ be the list of the overloaded lines following the contingency $inc$. Then
$\forall inc \in INCIDENT, \forall prd \in PARADE \cap CURATIF_{inc}$:

$$
(\nexists quad \in QUADNECESSAIRES_{prd}\text{ tel que }quad \in QUADENCONTRAINTE_{inc}) \Rightarrow actPRD_{prd}^{inc} = 0
$$

(val_prd_ctr)=
#### Lost pockets valuation constraint

When it is used, a remedial action may break the connectivity of the network by disconnecting nodes of
the latter. The generation and loads present on these nodes are then lost. If this occurs, an
economic penalty must be applied in the objective function.

Let $inc \in INCIDENT$ and $prd \in PARADE$ a remedial action breaking the connectivity of the network by disconnecting some nodes
of it.

Let us denote:
- $\Gamma^{ENE}$ the cost of the energy not evacuated
- $\Gamma^{END}$ the cost of the energy not distributed
- $GRPDECO$ the set of generators linked to the nodes of the lost pocket
- $CONSODECO$ the set of loads linked to the nodes of the lost pocket
- $VALOMax = proba_{inc} \cdot (\Gamma_{ENE} \cdot \sum_{i \in GRPDECO} (|P_i^0| + P_i^{max}) + \Gamma^{END} \cdot \sum_{i \in CONSODECO} C_i^0)$

In order to quantify the economic penalty, a new **positive** variable is introduced such that:
$val_{prd}^{inc} \in [0; max(0; VALOMax)]$.

<r>Summary of notations:</r>

**Data**

Sets: $PARADE$, $\forall prd \in PARADE$ :

$$
COUPLAGEFERMER_{prd}, COUPLAGEOUVRIR_{prd}, QUADNECESSAIRES_{prd}
$$

Value: $\Gamma^{PRD}$

**Variables**

$\forall inc \in INCIDENT, \forall prd \in PARADE : actPRD_{prd}^{inc}, val_{prd}^{inc}$

**Constraints**

- Remedial action uniqueness constraint
- Remedial action usage constraint
- Lost pockets valuation constraint

## Optimization problems

Below are the mathematical models of the two optimization phases:
- the *Adequacy phase* (i.e. generation - load balancing);
- and the *Redispatching phase* (i.e. resolution of network constraints).

### Mathematical model of the *Adequacy phase*

During this phase, we only deal with the generation - load balancing, without taking into account the
electrical network itself.

#### Constraints

**Global supply-demand balance**

Balancing the network amounts to having an equality between generation and load so that the demand is
satisfied without any excess of production:

$$
\sum_{i \in GROUPE}(P_i^0 + p_i^+ + p_i^-) - \sum_{i \in CONSO}(C_i^0 - c_i^-) = 0
$$

**Supply-demand balance per synchronous area**

The network is made of nodes, distributed in synchronous areas. HVDC lines can serve
as interconnections between these synchronous areas, and the power transmitted by these interconnections can be varied.

Hence, $\forall zc \in ZC$:

$$
\sum_{i \in GROUPE_{zc}}(P_i^0 + p_i^+ + p_i^-) - \sum_{i \in CONSO_{zc}}(C_i^0 - c_i^-) + \sum_{i \in LCC_{zc}^+}(Lcc_i^{0} + lcc_i^ + lcc_i^-) - \sum_{i \in LCC_{zc}^-}(Lcc_i^{0} + lcc_i^ + lcc_i^-) = 0
$$

**Objective function**

$$
min \sum_{i \in GROUPE}(p_i^+ \cdot \Gamma_{i_{ad}}^+ + p_i^- \cdot \Gamma_{i_{ad}}^-) + \sum_{i \in CONSO}(|c_i^-|\cdot \Gamma_i^{conso}) + \sum_{i \in LCC}(lcc_i^+ + lcc_i^-) \cdot \Gamma^{LCC}
$$

### Mathematical model of the *Redispatching phase*

During this phase, we add the consideration of the network via the management of flows on the lines. All the
data, variables and constraints of the *Adequacy* problem are kept and we add some elements to them: **none
of the previous constraints is affected**. <o>Only the value of some parameters may be modified.</o>

As a result, flow constraints appear, but also constraints on the PSTs, LCCs, remedial actions and contingencies.

#### Constraints

##### Defined in the section [Generators](#prod_var)
- [Generator coupling constraint](#coup_grp_ctr)
- [Curative changes limitation constraint](#lim_cur_ctr)

##### Defined in the section [Loads](#conso_var)
- [Load coupling constraint](#coup_conso_ctr)

##### Defined in the section [Remedial actions](#parades_var)
- [Remedial action uniqueness constraint](#unicity_prd_ctr)
- [Remedial action usage constraint](#usage_prd_ctr)
- [Lost pockets valuation constraint](#val_prd_ctr)

##### New constraints

**Supply-demand balance in curative**

Within this phase, the balance between generation and load must also be respected in curative, whatever the
contingency.

$\forall inc \in INCIDENT, \forall zc \in ZC$:

$$
\sum_{i \in CURATIF_{inc} \cap GROUPE_{zc}}(p_{i_{inc}}^+ -  p_{i_{inc}}^-) + \sum_{i \in CURATIF_{inc} \cap CONSO_{zc}}c_{i_{inc}}^{-} + \sum_{i \in CURATIF_{inc} \cap LCC_{zc}^{+}}(lcc_{i_{inc}}^+ -  lcc_{i_{inc}}^-) - \sum_{i \in CURATIF_{inc} \cap LCC_{zc}^{-}}(lcc_{i_{inc}}^+ -  lcc_{i_{inc}}^-) = 0
$$

**N.B.**: Expressed this way, this constraint should be wrong in the case of the loss of a generator, because it maintains the EOD
whereas the loss of a generator would break the balance. In practice, METRIX provides in pre-processing a “half
regulation band” on each generator, depending on the most dimensioning generator contingency. Before each resolution, METRIX
lowers the Pmax of each generator by its “half regulation band”. When a generator is lost, the lost
production is then distributed over all the other available generators.

**Flow threshold constraints**

The flow on the lines flagged in the data as “To be monitored” is computed at each micro-iteration
of the resolution. Indeed, these lines must respect three thresholds:
- In N
- In N-k after curative actions
- In N-k after contingency, but before curative actions or switching operations (i.e. ITAM threshold)

If one of these thresholds is not respected at the end of the micro-iteration, the associated constraint is added.
For each of these situations (denoted $situ$), and $\forall i \in QUADRIPOLE$ (i.e. for every line):

$$
transit_i^{situ} \leq Max_i^{situ}
$$

This flow is determined via the influence of a set of variables $VARINFLU^{situ}$ on the flow of $i$. This set depends on the situation $situ$ considered and, in the case of an N-k situation, on the contingency concerned. Each variable of this set is thus associated with an influence coefficient. These coefficients depend on the topology of the network in situation $situ$ and are stored in the set $COEFINFLU_i^{situ}$. These two sets have the same cardinality, denoted $N$, since every variable has, by definition, an influence and therefore an influence coefficient. Hence:

$$
transit_i^{situ} = \sum_{j = 1}^N COEFINFLU_i^{situ}[j] \cdot VARINFLU^{situ}[j]
$$

For a given line, the value of the maximum $Max_i^{situ}$, the influence coefficients $COEFINFLU_i^{situ}$ (because the
network topology can change with the contingencies), and above all the set $VARINFLU^{situ}$, change from one
situation to another. More precisely, depending on the situation the set $VARINFLU^{situ}$ contains:
- In N: all the preventive variables of the generators, loads, HVDCs and PSTs.
- In N-k after curative actions: all the continuous preventive variables of the generators, loads, HVDCs and PSTs
that can act in preventive, as well as the continuous curative variables of these same elements that can act in curative
on the contingency concerned.
- In N-k after a contingency, but before curative actions: all the preventive variables of the generators,
loads, HVDCs and PSTs, as well as the continuous curative variables of all the fictitious PSTs of the HVDCs in
AC emulation.

<b>Case of contingencies with remedial actions causing overloads</b>

The formulation of the flow constraint is different when remedial actions can be used.

Like the other flow constraints, these constraints are added progressively over the micro-iterations,
as soon as a constraint on a monitored line is detected. If a contingency having remedial actions as
curative actions generates a constraint on a quadripole, then constraints for each remedial action of this contingency are
also added. Let $inc \in INCIDENT$ be such a contingency, $quad \in QUADRIPOLE$ the constrained quadripole and
$prd \in PARADE \cap CURATIF_{inc}$ a remedial action that is curative for this contingency.
Let us also set $M$ a very large value, and $situ$ the situation "N-k after curative actions". Two scenarios are
possible:
- either the remedial action $prd$ opens the quadripole $quad$, in which case there is no reason to introduce a flow
constraint linking $quad$ and $prd$.
- otherwise, the following constraint is introduced:

$$
transit_{quad}^{situ} + M \cdot act_{prd}^{inc} \leq Max_{quad}^{situ} + M
$$

With $transit_{quad}^{situ}$ the flow on $quad$ when $prd$ is activated. The flow on $quad$ is therefore only
constrained (with respect to $Max_{quad}^{situ}$) if $prd$ is activated (which can be the "Do nothing" remedial action).

In addition, if the remedial action has a non-empty list $QUADNECESSAIRES_{prd}$ (i.e. a set of lines of which at least
one must be constrained for $prd$ to be activable):
- If $quad \notin QUADNECESSAIRES_{prd}$, then $prd$ is, at first, not activable on $inc$:
$act_{prd}^{inc} = 0$. $prd$ becomes activable again as soon as $inc$ causes an overload on another
quadripole $quad' \in QUADNECESSAIRES_{prd}$.
- Otherwise, the following remedial action activation constraint is added:

$$
transit_{quad}^{situ} - M \cdot act_{prd}^{inc} \geq Max_{quad}^{situ} - M
$$

This constraint only makes sense if $quad$ is overloaded.

If the conditions required to have the two previous flow constraints are met, then using the
remedial action amounts to having the constraint $transit_{quad}^{situ} = Max_{quad}^{situ}$. This is translated computationally
as $transit_{quad}^{situ} \in [Max_{quad}^{situ} - \epsilon; Max_{quad}^{situ} + \epsilon]$ with $\epsilon > 0 $ and
very small. Functionally, this means that one must be very close to the overload in order to be able to
use the remedial action without really being in overload.

<b>Case of remedial actions causing overloads</b>

When used to fix an overload generated by a contingency $inc$, a remedial action can also cause
overloads on another quadripole $quad$. Thus, $\forall prd \in PARADE \cap CURATIF_{inc}$ s.t. $prd$ does not cut
$quad$, the following constraint is added:

$$
transit_{quad}^{situ} + M \cdot act_{prd}^{inc} \leq Max_{quad}^{situ} + M
$$

We do not worry about the activation variables or the activation constraints since it is not a
contingency that causes the overload, but a remedial action. Only the constraints making it possible to respect the flow
threshold of the quadripole are added if the remedial actions are used.

<b>Constraint on the number of curative actions</b>

METRIX imposes a maximum number of curative actions per contingency. Fictitious PSTs are not counted, and the
lines opened or closed by the remedial actions are counted rather than the activated remedial actions.

Let $inc \in INCIDENT, prd \in PARADE \cap CURATIF_{inc}$. Let us denote respectively $NbLignesACouper$, $NbLignesAFermer$
and $NbLignesCoupees$ the cardinalities of the sets $COUPLAGEOUVRIR_{prd}$, $COUPLAGEFERMER_{prd}$ and $DEFAUTQUAD_{inc}$.

We thus obtain the number of actions: $NbActions = NbLignesACouper + NbLignesAFermer - NbLignesCoupees$.

Likewise, let us denote:
- $ECSTF = CURATIF_{inc}/TDF$ the set of curative elements of the contingency without the fictitious PSTs.
- $CoutAction$ a set of coefficients, with the same cardinality as $ECSTF$, s.t. $\forall j \in ECSTF$:
   - $CoutAction[j] = 0.5$ if $j$ is a generator or a load;
   - $CoutAction[j] = 1$ otherwise.

Finally, let us denote $NbMaxActCur$ the maximum number of curative actions allowed on a contingency.

The constraint is thus formulated as:

$$
NbActions \cdot act_{prd}^{inc} + \sum_{j \in ECSTF} act_{j}^{inc} \cdot CoutAction[j] < NbMaxActCur
$$

#### Objective function

Let $\forall inc \in INCIDENT, NbCtre_{inc}$ be the number of constraints due to $inc$. Let us also set:

$$
\gamma_{inc}^{tot} = \sum_{i \in GROUPE \cap CURATIF_{inc}} (p_{i_{inc}}^+ \cdot \Gamma_{i_{red}}^{+} + p_{i_{inc}}^- \cdot \Gamma_{i_{red}}^{-}) \cdot proba_{inc} + \sum_{i \in CONSO \cap CURATIF_{inc}} (|c_{i_{inc}}^-| \cdot \Gamma_{i_{cur}}^{CONSO}) \cdot proba_{inc} + \sum_{i \in LCC \cap CURATIF_{inc}} (td_{i_{inc}}^+ + td_{i_{inc}}^-) \cdot \Gamma^{TD} \cdot proba_{inc} + \sum_{prd \in PARADE \cap CURATIF_{inc}} (act_{prd}^{inc} \cdot \Gamma^{PRD} \cdot proba_{inc} \cdot  NbCtre_{inc} + val_{prd}^{inc})
$$

We obtain the following objective function:

$$
min \sum_{i \in GROUPE} (p_{i}^+ \cdot \Gamma_{i_{red}}^{+} + p_{i}^- \cdot \Gamma_{i_{red}}^{-}) + \sum_{i \in CONSO} (|c_{i}^-| \cdot \Gamma_{i}^{CONSO}) + \sum_{i \in LCC} (lcc_{i}^+ + lcc_{i}^-) \cdot \Gamma^{LCC} + \sum_{i \in TD} (td_{i}^+ + td_{i}^-) \cdot \Gamma^{TD} + \sum_{inc \in INCIDENT} \gamma_{inc}^{tot}
$$
