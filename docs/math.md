<style>
r { color: Red }
o { color: Orange }
g { color: Green }
y { color: yellow}
</style>

# Variables

## Incidents <a id="inc_var"></a>
Les incidents sont listés dans le conteneur $INCIDENT$. Nous noterins $inc$ un élément de cette liste.
Ce dernier peut provoquer :
- l'ouverture d'un certain nombre de lignes $DEFAUTQUAD_{inc}$ ;
- l'ouverture d'un certain nombre de LCCs $DEFAUTLCC_{inc}$ ;
- et/ou l'arrêt de certains groupes $DEFAUTGRP_{inc}$.

Parallèlement, afin de corriger cet incident, certains moyens curatifs pourront être utilisés. Ils sont stockés dans la liste $CURATIF_{inc}$ et contient des groupes, des consommations, des TDs, des HVDCs et/ou des parades. 

Enfin, cet incident a une certaine probabilité de survenir, notée $proba_{inc}$. Cet incident sera forcément appliqué sur le réseau lors de la simulation de METRIX, mais cette probabilité permet de pondérer le coût d’utilisation des actions curatives.

## Groupes <a id="prod_var"></a>
Les groupes du réseau modélisé dans METRIX sont stockés dans une liste $GROUPE$.
La production d’un groupe peut être modifiée en *Adequacy phase*, en *Redispatching phase*, les deux ou aucune. Nous ne considérerons dans les équations, que les groupes modifiables en toute circonstance.

### Définition des variables
Soit $i$ un groupe de $GROUPE$.

Sa production se définit par une valeur initiale $P_i^0$ et des valeurs minimum et maximum $P_{i}^{min}$ et $P_{i}^{max}$. Il existe deux valeurs pour la puissance minimum : $P_{i_{ad}}^{min}$ et $P_{i_{red}}^{min}$, représentant les minimums pour l'*Adequacy* et le *Redispatching* tels que :
$$
P_{i_{ad}}^{min} = min(0; P_{i_{red}}^{min})
$$

La variation de puissance du groupe $i$ est décrite dans les problèmes d'*Adequacy* et de *Redispatching* en N par deux variables : $p_i^+$ et $p_i^-$, **toutes deux positives**, et représentant respectivement la hausse et la baisse de la production du groupe $i$. De même, pour chaque incident $inc$, nous disposons de deux variables $p_{i_{inc}}^{-}$ et $p_{i_{inc}}^{+}$ représentant la variation de la puissance en curatif sur cet incident $inc$, et d'une variable booléenne d'activation du curatif sur l'incident $actP_{i}^{inc}$. Ces trois variables cutratives sont ainsi reliées de la manière suivante :
$$
p_{i_{inc}}^{-} + p_{i_{inc}}^{+} \leq M \cdot actP_{i}^{inc}
$$
Avec $M$ une très grande valeur.

### Domaines de définition des variables
Les domaines de définitions pour les variables liées à la production sont :
 - *Adequacy phase* (EOD - Équilibre Offre Demande) :
 
 $$
 \text{Si }P_{i_{ad}}^{min}\geq 0\text{ et }P_{i_{ad}}^{min} > P_{i}^{0} \Rightarrow 
 \begin{cases}
    p_{i}^{-} = 0\\
    p_{i}^{+} \in [P_{i_{ad}}^{min} - P_{i}^{0}; P_{i}^{max} - P_{i}^{0}]
 \end{cases}\\
 \text{Sinon }
 \begin{cases}
    p_{i}^{-} \in [0; P_{i}^{0} - P_{i_{ad}}^{min}]\\
    p_{i}^{+} \in [0; P_{i}^{max} - P_{i}^{0}]
 \end{cases}
 $$

 <o>YJ : pourquoi le $P_{i_{ad}}^{min}\geq 0$ ??</o>

 - *Redispatching phase* en préventif :

**Les variables et paramètres sont modifiés à partir du résultat de l’*Adequacy phase*** pour les intégrer dans la *Redispatching phase* : $P_i^0 = p_i^+ - p_i^- + P_i^0$. Autrement dit, la puissance de consigne en préventif prend pour valeur le résultat de l’*Adequacy phase* et les variables de production à la hausse ou à la baisse sont ensuite réinitialisées (N.B. : **c’est un comportement propre aux groupes**). Les bornes sont alors les suivantes :

$$
p_{i}^{+} \in [0; P_{i}^{max} - P_{i}^{0}]\\
p_{i}^{-}
 \begin{cases}
    \in [0; P_{i}^{0} - P_{i_{red}}^{min}]\text{ si }P_{i}^{0}\geq P_{i}^{min}\\
    = 0\text{ sinon}
 \end{cases}
$$

 - *Redispatching phase* en curatif :

$$ 
\begin{cases}
p_{i}^{+} - p_{i}^{-} + p_{i_{inc}}^{+} \leq P_{i}^{max} - P_{i}^{0}\\
min(0; P_{i_{red}}^{min} - P_{i}^{0}) \leq p_{i}^{+} - p_{i}^{-} - p_{i_{inc}}^{-}
\end{cases}
$$

### Coûts

En outre, la modification des variables de produciton des groupes a un coût. Plus précisément, nous allons associer des coûts à la hausse et à la baisse en *Adequacy phase* : $\Gamma_{i_{ad}}^{-}$ et $\Gamma_{i_{ad}}^{+}$ ; et en *Redispatchning phase* : $\Gamma_{i_{red}}^{-}$ et $\Gamma_{i_{red}}^{+}$.

### Appartenance à une zone synchrone

Chaque groupe est rattaché à un unique nœud du réseau, appartenant, lui-même, à une unique zone synchrone. Pour chaque zone synchrone $zc \in ZC$, nous notons $GROUPE_{zc}$ l'ensemble des groupes appartenant à cette zone synchrone.

### Contrainte de couplage des groupes

Lors de la *Redispatching phase*, nous pouvons définir dans le réseau un ensemble de groupes dont la production en N doit varier de façon proportionnelle. Notons $COUPLAGE_{GRP}$ l'ensemble des couplages groupés. Afin de définir cette variation, nous allons définir une valeur de référence pour chacun des groupes de cet ensemble. Cette valeur de référence peut être $P^{max}$, $P^{min}$, $P^{0}$ ou encore $P^{max} - P^{0}$. Celle-ci sera notée $P^{ref}$.

Soit $i_0$ le premier groupe de cet ensemble. $\forall i \in COUPLAGE_{GRP}$, $i \not = i_0$ :
$$
\frac{p_i^+ - p_i^-}{P_i^{ref}} = \frac{p_{i_0}^+ - p_{i_0}^-}{P_{i_0}^{ref}}
$$

### Contrainte de limitation des changements curatifs
Cette contrainte est **facultative**, il faut indiquer dans les données METRIX que nous souhaitons l'appliquer (avec le paramètre *LimiteCurGroupe*). Cette contrainte permet de limiter la baisse cumulée de la production en curatif sur un incident $inc$ :
$$
\sum_{i\in GROUPE} p_{inc}^{-} \leq LimiteCurGroupe
$$

**Note** : la contrainte ne considère que les variations à la baisse afin de prendre en compte le délestage de consommation.

<r>Résumé des notations :</r>

**Données**

Ensembles : $GROUPE$, $COUPLAGE_{GRP}$, $GROUPE_{zc}$

Valeurs :
 - LimiteCurGroupe
 - $\forall i \in GROUPE : P_{i}^{max}, P_{i_{ad}}^{min}, P_{i_{red}}^{min}, P_{i}^{ref}, \Gamma_{i_{ad}}^{-}, \Gamma_{i_{ad}}^{+}, \Gamma_{i_{red}}^{-}, \Gamma_{i_{red}}^{+}$

**Variables**

$\forall i \in GROUPE, \forall inc \in INCIDENT : p_{i}^{+}, p_{i}^{-}, p_{i_{inc}}^{+}, p_{i_{inc}}^{-}, actP_{i}^{inc}$

**Contraintes**
- Contraintes des domaines de définition
- Contrainte de couplage des groupes
- Contrainte de limitation des changements curatifs

## Consommations <a id="conso_var"></a>

Les centres de consommations (appelés consommations) sont dans la liste $CONSO$. 
La puissance utilisée par une consommation peut être modifiée aussi bien en *Adequacy* qu'en *Redispacthing phase*.

### Définition des variables

Soit $i$ une consommation du groupe $CONSO$.

$C_{i}^{0}$ définit la puissance consommée à l'état initial ; le délestage est représenté en *Adequacy phase* et en préventif par la variable $c_i^{-}$ et en curatif sur un incident $inc$ par la variable $c_{i_{inc}}^{-}$ et la variable bouléenne d'activation $actC_i^{inc}$. Tel que :

$$
c_{i_{inc}}^{-} \leq M \cdot actC_{i}^{inc}
$$

Avec $M$ une valeur très élevée.

### Domaines de définition des variables

Pour fixer la valeur maximale du délestage, un pourcentage est utilisé. Celui-ci est défini par les paramètres $\Phi_i$  en *Adequacy phase* et en préventif, et $\Phi_{i_{cur}}$ en curatif. Ainsi, nous obtenons les encadrements suivants :
 - Pour l'*Adequacy phase* et le préventif :
 
 $$ 
 c_{i}^{-} \in
 \begin{cases}
 [0; \Phi_i \cdot C_i^0]\text{, si }C_i^{0} \geq 0\\
 [\Phi_i \cdot C_i^0; 0]\text{, sinon. }
 \end{cases}
 $$

 - Pour le curatif :

 $$
 \forall inc \in INCIDENT, c_{i_{inc}}^{-} \in [0; max(0; C_i^{0} \cdot \Phi_{i_{cur}})]
 $$

### Définition des coûts

Pout chaque groupe, nous définissons un coût pour l'*Adequacy phase* et le préventif $\Gamma_i^{conso}$ et un autre pour le curatif $\Gamma_{i_{cur}}^{conso}$.

### Appartenance à une zone synchrone

Chaque consommation se rattache à un unique nœud du réseau, qui fait lui-même parti d’une unique zone synchrone. Pour chaque zone synchrone $zc \in ZC$ , nous notons $CONSO_{zc}$ les consos appartenant à cette zone synchrone.

### Contrainte de couplage des consos
Tout comme pour les groupes, des consommations peuvent être couplées afin que leur délestage en N soient proportionnels. Notons $COUPLAGE_{CONSO}$ la liste de ces consommations couplées.
Soit i$_0$ le premier groupe de cette liste. $\forall i \in COUPLAGE_{CONSO}, i \neq i_0$ : 

$$
\frac{c_i^{-}}{C_i^{0}} = \frac{c_{i_0}^{-}}{C_{i_0}^{0}}
$$

<r>Résumé des notations :</r>

**Données**

Ensembles : $CONSO$, $COUPLAGE_{CONSO}$, $CONSO_{zc}$

Valeurs : $\forall i \in CONSO : C_i^{0}, \Phi_i, \Phi_{i_{cur}}$

**Variables**

$$
\forall i \in CONSO, \forall inc \in INCIDENT : c_i^{-}, c_{i_{inc}}^{-}, actC_i^{inc}
$$

**Contraintes**
Contrainte de couplage des consos.

## Transformateur-Déphaseur (TD) <a id="td_var"></a>

Les TDs sont stockés dans une liste $TD$. Soit $i$ un élément de cette liste.

### Valeurs min et max des TDs

Dans METRIX, le déphasage se fera sans passer d’une prise à l’autre de manière discrète, mais avec des variables continues évoluant entre une puissance min et une puissance max. Les listes des prises de déphasage d'un TD sont toutefois utilisées afin de fixer ces puissances min et max, notées $TD_i^{min}$ et $TD_i^{max}$.
Ces valeurs pourront être tirées des données d'entrée si les bornes de changement de prises à la hausse et à la baisse ne sont pas définies.

### Définition des variables

La puissance de consigne transmise par $i$ du nœud *Or* au nœud *Nf* va être représentée par le paramètre $TD^{0}_i$. La variation de cette puissance en préventif  va être représentée par les variables **positives** $td^{+}_i$ à la hausse et $td^{-}_i$ à la baisse. De même, si le TD est autorisé à agir en curatif sur l’incident $inc$, nous allons utiliser les variables curatives **positives** $td^{+}_{i_{inc}}$ et $td^{-}_{i_{inc}}$ et la vraiable booléenne $actTD_{i}^{inc}$. Ces trois variables sont reliées par la contrainte suivante :

$$
td^{+}_{i_{inc}} + td^{-}_{i_{inc}}\leq M \cdot actTD_{i}^{inc}
$$
Avec $M$ ue valeur très élevée.

### Domaines de définition des variables

Chaque TD est pilotable de quatre manières différentes : 
 - En puissance imposée
 - En puissance optimisée
 - En angle imposé
 - En angle optimisé

Les pilotages imposés impliquent que le TD échangera toujours la même puissance entre les deux nœuds *Or* et *Nf* : $td^{+}_{i} = td^{-}_{i} = td^{+}_{i_{inc}} = td^{-}_{i_{inc}} = 0$.

Pour les pilotages en puissance (imposée et optimisée), le *quadFictif* est ouvert : seule la puissance du TD sera transmise entre les nœuds *Or* et *Nf*.

Pour le pilotage optimisé (en angle ou en puissance), les paramètres  $TD_i^{min}$ et $TD_i^{max}$ définis précédemment, sont utilisés tels que :

$$
0 \leq td^{+}_{i} \leq max(TD_i^{max} - TD_i^{0}; 0) \text{ et } 0 \leq td^{-}_{i} \leq max(TD_i^{0} - TD_i^{min}; 0)\\
\forall inc \in INCIDENT:\\
0 \leq td^{+}_{i_{inc}} \leq max(TD_i^{max} - TD_i^{min}; 0) \text{ et } 0 \leq td^{-}_{i_{inc}} \leq max(TD_i^{max} - TD_i^{min}; 0)
$$

Pour l'encadrement curatif, les contraintes suivantes sont ajoutées :

$$
\forall inc \in INCIDENT:\\
td^{+}_{i} + td^{+}_{i_{inc}} \leq max(TD^{max}_{i} - TD^{0}_i; 0)\\
td^{-}_{i} + td^{-}_{i_{inc}} \leq max(TD^{0}_{i} - TD^{min}_i; 0)
$$

### Domaines de définition des variables
En ce qui concerne le coût, les TDs ont tous le même coût d’utilisation en préventif $\Gamma^{TD}$, défini dans les paramètres de METRIX. En curatif, ce coût est pondéré par la probabilité d’apparition de l’incident, et va donc dépendre de chaque incident.

**En résumé** : Les TDs permettent de modifier le transit des lignes pour éviter les surcharges, sans modifier l’équilibrage offre-demande. 

<r>Résumé des notations :</r>

**Données**

Ensembles : $TD$

Valeurs : $TD^{0}_i$, $TD^{max}_i$, $TD^{min}_i$, $\Gamma^{TD}$

**Variables**

$$
\forall i \in TD, \forall inc \in INCIDENT : td^{+}_{i}, td^{-}_{i}, td^{+}_{i_{inc}}, td^{-}_{i_{inc}}, actTD_{i}^{inc}
$$

## Lignes à Courant Continu (LCC) <a id="lcc_var"></a>

Les LCCs du réseau simulé par METRIX sont stockées dans l'ensemble $LCC$.

### Variables et encadrement

Soit $i \in LCC$, notons :
- $Lcc_i^{0}$ sa puissance de consigne ;
- $Lcc_i^{min}$ sa puissance minimale ;
- et $Lcc_i^{max}$ sa puissance maximale.

Les variations de $i$ sont représentées :
- en préventif par les variables **positives** $lcc_i^{+}$ et $lcc_i^{-}$ ;
- et en curatif par les variables **positives** $lcc_{i_{inc}}^{+}$ et $lcc_{i_{inc}}^{-}$ ainsi que par la variable booléenne $actLCC_{i}^{inc}$.

Ces trois variables sont reliées pâr la contrainte suivante :

$$
lcc_{i_{inc}}^{+} + lcc_{i_{inc}}^{-} \leq M \cdot actLCC_{i}^{inc}
$$

Avec $M$ une valeur très grande. 

Les variables représentant les variations de puissance de la LCC ont l’encadrement suivant : 
- Si le pilotage est en puissance optimisée ou en émulation AC optimisée :
$$
lcc_i^+ \in [0; max(Lcc_i^{max}-Lcc_i^{0}; 0)]\text{ et }lcc_i^- \in [0; max(Lcc_i^{0}-Lcc_i^{min}; 0)]
$$
- Sinon, il est imposé :
$$
lcc_i^{+} = lcc_i^{-} = 0
$$

En curatif, on se retrouve avec les encadrements suivants pourt les pilotages optimisés :
$$
lcc_i^{+} + lcc_{i_{inc}}^{+} \leq Lcc_i^{max} - Lcc_i^{0}
\\
lcc_i^{-} + lcc_{i_{inc}}^{-} \leq Lcc_i^{0} - Lcc_i^{min}
$$

### Définition des coûts
En ce qui concerne le coûts, les LCCs ont toutes le même coût d’utilisation en préventif $\Gamma^{LCC}$, défini dans les paramètres de METRIX. En curatif, ce coût est pondéré par la probabilité d’apparition de l’incident, et dépendra donc de chaque incident.

### Lien entre zones synchrones
Notons $\forall zc \in ZC$ , $LCC_{zc}^+$ l’ensemble des LCCs transportant le courant de la zone synchrone $zc$ vers une autre zone synchrone. De même, $LCC_{zc}^-$ est l’ensemble des LCCs prélevant de la puissance à la zone synchrone $zc$ pour l’envoyer vers une autre zone synchrone.

### LCC en émulation AC
Le TD fictif ne pourra jamais agir en préventif : $\forall inc \in INCIDENT$, une contrainte d’activation relie les variables préventives du TD à la variable d’activation en curatif du même TD sur cet incident ($actLCC_{inc}^i$) ; et cette variable d’activation va être bloquée à $0$, empêchant toute variation des variables préventives. Plus tard, si la variable d’activation est débloquée, les variables préventives ne seront utilisées par le problème que dans le calcul du transit sur quadripôle, et de toute façon le coefficient associé sera nul.  
Quant aux variables curatives (linéaires et booléennes), elles sont reliées par une contrainte d’activation. Soit $i \in TD, inc \in INCIDENT$ :
$$
td_{i_{inc}}^+ + td_{i_{inc}}^- \leq M \cdot actLCC_{inc}^i
$$

La variable d’activation est bloquée à $0$, à moins que le quadripôle *quad0* soit en contrainte. Dans ce cas, la variable est libre de changer de valeur. 
Ce TD fictif aura un coût d’utilisation nul. Il aura le même encadrement en préventif que les TDs normaux, avec des puissances max et min issues de la LCC en émulation AC. Il aura également comme encadrement en curatif :
$$
0 \leq td_{i_{inc}}^+ \leq td_i^{max}\text{ et }0\leq td_{i_{in}}^- \leq -td_i^{min}
$$
Les TDs fictifs sont stockés dans l’ensemble $TDF$.

<r>Résumé des notations :</r>

**Données**

Ensembles : $LCC$, $TDF$, $\forall zc \in ZC: LCC_{zc}^+, LCC_{zc}^-$

Valeurs : $\forall i \in LCC : Lcc_i^{0}, Lcc_i^{max}, Lcc_i^{min}, \Gamma^{LCC}$

**Variables**

$$
\forall i \in LCC, \forall inc \in INCIDENT: lcc_{i}^{+}, lcc_{i}^{-}, lcc_{i_{inc}}^{+}, lcc_{i_{inc}}^{-}
$$

## Parades <a id="parades_var"></a>

Les parades sont listées dans $PARADE$. Soit $prd \in PARADE$. Chaque parade est associée à un unique incident $inc \in INCIDENT$.

$prd$ possède en paramètre la liste des couplages qu’elle ferme ($COUPLAGEFERMER_{prd}$) et ceux qu’elle ouvre ($COUPLAGEOUVRIR_{prd}$).
On définit, pour un incident $inc$ et une parade $prd$, la variable d’activation de la parade sur cet incident $actPRD_{prd}^{inc}$, de coût dans la fonction objectif $\Gamma_{prd}$.

### Contrainte d'unicité des parades

Pour chaque incident, une parade unique est applicable, $\forall inc \in INDICENT$ :
$$
\sum_{prd \in PARADE \cap CURATIF_{inc}} actPRD_{prd}^{inc} \leq 1
$$

Dans le code, l’inégalité de cette contrainte est transformée en une égalité, par l'intermédiaire de l'introduction d'une parade “Ne Rien Faire” pour chaque incident possédant des parades topologiques en actions curatives. Cette parade n’a aucune action sur le réseau.

### Contrainte d'utilisation des parades

L'activatigon d'une parade $prd$ peut être empêchée tant qu'il existe une ligne, parmi un certain ensemble de lignes, non contrainte. Notons $QUADNECESSAIRES_{prd}$ cet ensemble.

Notons $QUADENCONTRAINTE_{inc}$ la liste des lignes en surtension suite à l'incident $inc$. Alors $\forall inc \in INCIDENT, \forall prd \in PARADE \cap CURATIF_{inc}$ :
$$
(\nexists quad \in QUADNECESSAIRES_{prd}\text{ tel que }quad \in QUADENCONTRAINTE_{inc}) \Rightarrow actPRD_{prd}^{inc} = 0
$$

### Contrainte de valorisation des poches perdues

Lors de son utilisation, il est possible qu’une parade rompe la connexité du réseau en déconnectant des noeuds de ce dernier. Les productions et consommations présentes sur ces nœuds vont alors être perdues. Si cela survient, une sanction économique dans la fonction objectif doit être appliquée.

Soit $inc \in INCIDENT$ et $prd \in PARADE$ une parade rompant la connexité du réseau en déconnectant certains noeuds de celui-ci.

Notons :
- $\Gamma^{ENE}$ le coût de l'énergie non évacuée
- $\Gamma^{END}$ le coût de l'énergie non distribuée
- $GRPDECO$ l'ensemble des groupes liés aux noeuds de la poche perdue
- $CONSODECO$ l'ensemble des consos liées aux noeuds de la poche perdue
- $VALOMax = proba_{inc} \cdot (\Gamma_{ENE} \cdot \sum_{i \in GRPDECO} (|P_i^0| + P_i^{max}) + \Gamma^{END} \cdot \sum_{i \in CONSODECO} C_i^0)$

Afin de quantifier la sanction économique, une nouvelle variable **positive** est introduite telle que : $val_{prd}^{inc} \in [0; max(0; VALOMax)]$.

<r>Résumé des notations :</r>

**Données**

Ensembles : $PARADE$, $\forall prd \in PARADE$ :
$$
COUPLAGEFERMER_{prd}, COUPLAGEOUVRIR_{prd}, QUADNECESSAIRES_{prd}
$$

Valeurs : $\forall prd \in PARADE : \Gamma_{prd}$

**Variables**

$\forall inc \in INCIDENT, \forall prd \in PARADE : actPRD_{prd}^{inc}, val_{prd}^{inc}$

**Contraintes**

- Contrainte d'unicité des parades
- Contrainte d'utilisation des parades
- Contrainte de valorisation des poches perdues

# Problèmes d'optimisation

Ci-après, les modèles mathématiques des deux phases d'optimisation :
- l'*Adequacy phase* (i.e. équilibrage production - consommation);
- et la *Redispatching phase* (i.e. résolution des contraintes réseau).

## Modèle mathématique de l'*Adequacy phase*

Lors de cette phase, nous ne nous occuppons que de l'équilibrage production - consommation, sans tenir compte du réseau électrique en lui-même. 

### Contraintes

**Équilibrage Offre-Demande global**

L'équilibrage du réseau revient à avoir une égalité entre la production et la consommation afin que la demande soit satisfaite sans aucun excès de prodution :
$$
\sum_{i \in GROUPE}(P_i^0 + p_i^+ + p_i^-) - \sum_{i \in CONSO}(C_i^0 - c_i^-) = 0
$$

**Équilibrage Offre-Demande par zone synchrone**

Le réseau est composé de nœuds, se répartissant dans des zones synchrones. Les Lignes à Courant Continu peuvent servir d’interconnexion entre ces zones synchrones, et il est possible de faire varier la puissance transmise par ces interconnexions.

Dès lors, $\forall zc \in ZC$ :

$$
\sum_{i \in GROUPE_{zc}}(P_i^0 + p_i^+ + p_i^-) - \sum_{i \in CONSO_{zc}}(C_i^0 - c_i^-) + \sum_{i \in LCC_{zc}^+}(Lcc_i^{0} + lcc_i^ + lcc_i^-) - \sum_{i \in LCC_{zc}^-}(Lcc_i^{0} + lcc_i^ + lcc_i^-) = 0
$$

**Fonction objectif**

$$
min \sum_{i \in GROUPE}(p_i^+ \cdot \Gamma_{i_{ad}}^+ + p_i^- \cdot \Gamma_{i_{ad}}^-) + \sum_{i \in CONSO}(|c_i^-|\cdot \Gamma_i^{conso}) + \sum_{i \in LCC}(lcc_i^+ + lcc_i^-) \cdot \Gamma^{LCC}
$$

## Modèle mathématique de la *Redispatching phase*

Lors de cette phase, nous ajoutons la prise en compte du réseau via la gestion des flux sur les lignes. L'ensemble des données, variables et contraintes du problème d'*Adequacy* sont gardées et nous y ajoutons certains éléments : **aucune des contraintes précédentes n'est affectée**.