<style>
r { color: Red }
o { color: Orange }
g { color: Green }
y { color: yellow}
</style>

# Algorithm description

## Introduction
Dans le cadre de ses activités, RTE a besoin d'un outil capable, pour un réseau donné, **de simuler son fonctionnement 
heure par heure** et de fournir une **estimation de son coût d'exploitation, en situation normale et face à différents 
incidents**. Cela est, par exemple, nécessaire pour l'étude du développement du réseau électrique. 

METRIX comprend à la fois un calcul de réseau (load-flow) en actif seul ainsi qu'un modèle d'optimisation de flux 
(Optimal Power Flow ou OPF).

Un **load-flow** calcule la répartition des flux sur les ouvrages du réseau en fonction des caractéristiques de ces 
ouvrages et des injections du réseau (production et consommation). Le résultat d'un load-flow permet de constater les contraintes.
Un **OPF** peut modifier les injections/consommations et la topologie de sorte, qu'après load-flow, il n'y ait pas de 
contrainte sur les ouvrages (ou qu'elles soient, du moins, minimisées).

De ce fait, l'objectif de METRIX est de trouver l'ajustement de moindre coût du plan de production afin de n'avoir 
aucune contrainte de transit ni en N ni sur incidents. Cet outil permet de mesurer le surcoût engendré par le réseau 
sur le plan de production initial et permet de valoriser un renforcement de réseau.
Dans sa version 6, METRIX est intégré dans la plate-forme imaGrid où il peut être lancé sur des milliers de variantes. 
Ces variantes permettent de modéliser des incertitudes sur la consommation, la production renouvelable, la disponibilité 
des groupes, etc.

Afin d'obtenir un problème plus facile et rapide à résoudre, METRIX utilise l'**approximation du courant continu** pour 
modéliser le réseau. Cependant, cette simplification ne permet pas de détecter les contraintes de tension et encore 
moins les problèmes liés à la dynamique des événements.

Que ce soit via imaGrid ou en exécution directe, un cas de base ainsi qu'un fichier annexe décrivant toutes les variantes
à calculer sont envoyés en entrée de METRIX. Ce dernier applique ensuite chacune des variantes au cas de base et 
retourne un résultat pour chaque variante. Les résultats sont présentés sous forme de chroniques dans imaGrid.
Ce document présente la modélisation du système électrique utilisée par METRIX, la logique de l'algorithme global, 
le problème d'optimisation qu'il résout et les fonctionnalités disponibles au sein de METRIX.


## Objectif

**Le but de METRIX est de fournir une estimation du coût d'exploitation horaire d'un réseau fonctionnant de manière 
optimale**, i.e. coûtant le moins possible.

Considérons un réseau donné. Le comportement d'un réseau étant aléatoire, le coût d'exploitation l'est également. 
Posons $X$ la variable aléatoire du coût d'exploitation de ce réseau. Ce que nous voulons calculer avec METRIX, c'est 
une minimisation de l'espérance de cette variable aléatoire :

$$
\begin{aligned}
min(\mathbb{E}(X))
\end{aligned}
$$

Puisque nous ne disposons pas de loi pour déterminer cette espérance, nous allons utiliser une approche statistique 
et l'approximer comme la moyenne d'un grand nombre de réalisations équiprobables. Il va donc falloir déterminer le 
coût d'exploitation du réseau sur un grand nombre d'heures (que nous nommerons variantes). En notant $n$ le nombre de 
réalisations :

$$
\begin{aligned}
min(\mathbb{E}(X)) \approx  min(\frac{1}{n}\sum_{i=0}^{n}x_i)
\end{aligned}
$$

D'autre part, pour simplifier le modèle et paralléliser les simulations des variantes, il a été décidé de les rendre 
indépendantes les unes des autres : la réalisation de la variable sur une variante est totalement indépendante de sa 
réalisation sur une autre variante. Par conséquent, minimiser la somme des coûts revient à minimiser chaque coût horaire :


$$
\begin{aligned}
min(\mathbb{E}(X)) \approx  min(\frac{1}{n}\sum_{i=0}^{n}x_i) = \frac{1}{n}\sum_{i=0}^{n}min(x_i)
\end{aligned}
$$

Pour estimer le minimum de l'espérance de $X$, nous allons donc devoir minimiser le coût de fonctionnement du réseau 
de chaque variante, et ce, sur un grand nombre de variantes. Cela équivaut à simuler, pour chaque variante, un 
fonctionnement optimal du réseau. Pour ce faire, nous allons utiliser les outils de **Recherche Opérationnelle**.
Par ailleurs, pour avoir des variantes différentes, nous allons avoir besoin de situations différentes dans le réseau : 
en termes de coûts de production, de groupes indisponibles, de consommations des centres de consommation, etc. Ces 
paramètres du réseau seront représentés par des variables aléatoires, chaque variante ayant sa réalisation. METRIX 
résout donc un problème d'**optimisation stochastique**.

## Énoncé du problème
Pour simuler le fonctionnement réel et optimal du réseau sur une variante, nous allons décomposer le problème en deux 
problèmes d'optimisation, résolus l'un après l'autre.

### Premier problème
Tout d'abord, un premier problème d'équilibrage entre production et consommation (***Adequacy phase***), simulant le 
fait que, suite à un changement de la consommation, les producteurs d'électricité adaptent leur production pour répondre 
à la nouvelle demande, tout en assurant un coût d'opération minimal. Si la capacité de production est insuffisante, du 
délestage de consommation est également possible. Le réseau électrique n'est pas pris en compte dans cette phase, nous 
supposons que tous les éléments producteurs et consommateurs se trouvent sur une même plaque de cuivre sans résistance. 
Avec la résolution de ce problème, nous obtenons une **égalité production – consommation**. 

Toutes les **variables du problème étant continues**, nous utilisons la **Programmation Linéaire**. Pour garder une 
approche statistique, le choix des groupes (pour des coûts égaux) se fait **aléatoirement**. Toutefois, pour éviter 
d'avoir une solution différente à chaque résolution, ce choix aléatoire est identique pour un même environnement 
informatique. 

Cette étape est explicitée dans [le schéma ci-après](#algo-adequacy-fig) : la situation initiale est déséquilibrée entre la demande 
(150 MW) et la production (30 + 20 MW). Les groupes augmentent donc leur production, en appelant d'abord les productions 
les moins chères.

```{figure} img/adequacy_phase.png
:name: algo-adequacy-fig
:alt: Adequacy figure
:width: 600px
:align: center

Adequacy figure
```

Cf. [Adequacy model](#algo-adeq_math).

### Deuxième problème
Mais puisque les lignes électriques n'ont pas été prises en compte, ce **nouvel équilibre peut entraîner des surcharges 
sur celles-ci**. Dans une deuxième phase (***Redispatching phase***), il faut donc considérer le réseau dans son 
intégralité et choisir des **actions préventives** pour éviter les surcharges tout en assurant l'équilibrage 
Production – Consommation. Ces actions préventives regroupent les moyens d'actions de RTE sur le réseau, à savoir la 
**modification des déphasages des Transformateurs-Déphaseurs et des flux sur les Lignes à Courant Continu**. Nous avons 
également la possibilité de modifier la production ou la consommation. Néanmoins, ces actions concernent des acteurs 
extérieurs à RTE, leur utilisation sera donc défavorisée par un coût plus élevé dans la simulation. 

En outre, des incidents peuvent se produire sur le réseau et perturber son fonctionnement (perte d'un groupe de 
production, d'une ligne, etc.) ; il faut donc choisir des **actions curatives** pour éviter d'autres surcharges. Ces 
actions sont les mêmes qu'en préventif, avec, en supplément, des **parades topologiques** : ces dernières ouvrent ou 
ferment des lignes ou des couplages du réseau, de façon à en modifier la topologie, et ainsi à modifier les flux sur les 
lignes. 

La résolution de ce deuxième problème d'optimisation va donc permettre de simuler le choix des actions préventives et 
curatives garantissant le bon fonctionnement du réseau pour un coût minimal. Certaines actions curatives étant du type 
“interrupteur”, **des variables booléennes** apparaissent dans le problème : nous résolvons ainsi un **Problème Linéaire 
Mixte en Nombres Entiers**. 

Le [schéma ci-après](#algo-redis_fig) montre la partie préventive de ce second problème : après l'équilibrage du réseau, les 
contraintes de seuil des lignes sont ajoutées, ce qui provoque la surcharge d'une ligne. Pour que son seuil ne soit pas 
dépassé, nous modifions les productions des groupes tout en maintenant l'équilibre offre – demande, ce qui entraîne un 
surcoût de 200€.

```{figure} img/redis_phase.png
:name: algo-redis_fig
:alt: Redispatching figure
:width: 600px
:align: center

Redispatching figure
```

Cf. [Redispathcing model](#algo-redis_math)

Voici donc les deux problèmes qui doivent être résolus afin de simuler le fonctionnement du réseau sur la durée souhaitée. 
Cependant, <r>pour plus de rapidité et pour rester dans une approche statistique</r>, le choix a été fait de **ne pas 
lier la solution de la variante $t$ à la situation initiale de la variante $t+1$**. Les résultats de chaque variante 
sont **indépendants** des autres variantes. Dès lors, chaque ensemble de problème {*Adequacy phase*, *Redispatching 
phase*} est indépendant d'une variante à une autre, et chaque variante peut se résoudre en parallèle.

## Hypothèses et simplifications du problème

### Représentation des lignes et gestion de leurs contraintes

Pour simuler la *Redispatching phase*, nous résolvons un problème d'optimisation incluant des contraintes sur les 
transits des lignes. Cependant, un réseau est fait d'un très grand nombre de lignes. Sachant que **seules quelques-unes 
seront intéressantes à surveiller** lors d'une simulation, nous indiquons en entrée de la simulation quelles sont **les 
lignes dont il faut contraindre le transit** et quelles sont **les lignes qui peuvent être laissées sans surveillance**. 
Cela permet de réduire la quantité de calculs à faire sans diminuer l'utilité de la simulation.

En outre, grâce à la structure du problème, **nous pouvons éviter d'ajouter d'emblée toutes les contraintes de transit 
sur les lignes**. En effet, dans un réseau électrique, si des lignes se trouvent en surcharge, cela est généralement dû 
à une surcharge sur un groupe réduit de lignes qui transmettent ensuite l'excès à d'autres. En empêchant les surcharges 
sur ce groupe original de lignes, nous faisons donc disparaître aussi les surcharges sur les autres lignes. **Le 
problème d'optimisation peut ainsi être simplifié en réduisant le nombre de contraintes** à prendre en compte, sans 
changer la validité de la simulation. 

Pour résoudre le problème d'optimisation, nous allons **procéder par micro-itérations* : à chaque micro-itération, nous 
résolvons le problème ; puis nous cherchons si, dans la solution trouvée, il y a des lignes en surcharge. Parmi ces 
lignes, nous allons rechercher quel sous-groupe de lignes engendre des surcharges ailleurs, et donc quel sous-groupe de 
lignes, il faut contraindre pour faire disparaître toutes les surcharges. Ces contraintes sont alors ajoutées au 
problème, puis nous relançons la résolution. Et ce, ainsi de suite, jusqu'à ce qu'il n'y ait plus de surcharges qui 
apparaissent.

```{figure} img/micro_it.png
:name: algo-microIt_fig
:alt: Micro-iterations process
:width: 600px
:align: center

Micro-iterations process
```

###	Actions préventives et curatives pour satisfaire les contraintes de seuil

Pour satisfaire les contraintes réseau lors de la *Redispatching phase*, nous utilisons des actions préventives (pour 
modifier le réseau avant que les incidents ne surviennent, de façon à ce qu'ils ne provoquent pas de défaillances) et 
curatives (pour corriger les défaillances survenues et continuer à satisfaire la consommation).

Parmi ces actions, il y a la modification de la production des groupes et le délestage de la consommation, tout comme 
lors de l'*Adequacy phase*. Mais il y a également l'emploi des **Transfo-Déphaseurs** (TD), des **Lignes à Courant 
Continu** (LCC) et des **Parades Topologiques**. 

Les TDs servent à modifier la phase sur les lignes sur lesquelles ils sont implantés, et donc de changer le rapport 
$\frac{\text{puissance active}}{\text{puissance réactive}}$. Autrement dit, ils permettent de régler la “puissance” 
circulant sur les lignes, sans toucher à la production ou la consommation. 

Le nom des LCCs est relativement explicite : ces lignes ne transportent pas du courant alternatif. Tout comme pour les 
TDs, nous pouvons régler la puissance circulant dessus. Elles permettent ainsi de transférer une puissance réglable 
entre deux nœuds, et notamment entre deux zones synchrones (i.e. deux ensembles de nœuds aux caractéristiques alternatives 
différentes).

Enfin, les parades topologiques (utilisables uniquement en curatif) correspondent à l'ouverture ou la fermeture de 
lignes ou de couplages dans le réseau : elles modifient la typologie du réseau. En conséquence, elles modifient la 
matrice de répartition permettant de calculer les flux sur les lignes, et donc modifient ces flux eux-mêmes. Leur 
utilisation nécessitant une étude à part entière avant de l'intégrer comme action dans une simulation, nous 
n'autorisons qu'**une parade topologique maximum par incident**.

### Approximation de l'actif seul

#### Hypothèses
Ces moyens d'actions sus-cités, servent donc à éviter ou réduire les surcharges sur les lignes. Mais pour les détecter, 
il faut d'abord calculer les transits sur ces lignes. 

Pour cela, nous pouvons utiliser la modélisation physique habituelle, représentant une ligne électrique comme une 
inductance et une résistance en série (cf. [schéma ci-après](#algo-indRes_fig)).

```{figure} img/indRes.png
:name: algo-indRes_fig
:alt: Inductance et résistance en série
:width: 600px
:align: center

Inductance et résistance en série
```

Dans ce cas, le transit de $i$ vers $j$, noté $T_{ij}$, est calculé de la façon suivante :

$$
T_{ij} = \frac{V_iV_j}{Z_{ij}}sin(\theta_i - \theta_j + \gamma_{ij}) + \frac{V_i^2}{Z_{ij}}sin(\gamma_{ij}) \\
Z = R + i\omega L \\
\gamma_{ij} = arctan(\frac{R}{\omega L})
$$

Cette expression est calculable. Mais <r>dans le cadre de la programmation linéaire, il est plus approprié d'utiliser 
des expressions linéaires. Cela peut être obtenu avec une correspondance relativement bonne à l'aide de 
l'</r>**<r>approximation de l'actif seul</r>, dans laquelle nous supposons que la source impose un déphasage global 
constant, ainsi qu'une tension commune**.

L'approximation s'appuie sur 3 hypothèses simplificatrices :
1. Chaque liaison est assimilée à une réactance pure : la résistance est supposée nulle. Nous négligeons également les 
conductances et susceptances des lignes.
2. Les différences de phases entre sommets voisins sont petites ; par conséquent, nous considérons que 
$sin(\theta_{i} − \theta_{j}) \approx \theta_{i}- \theta_{j}$.
3. La tension est supposée uniforme sur l'ensemble du réseau à une valeur fixée $V_{ref}$.

$$
V_i = V_j = V_{ref} = \text{constante} \\
R <<< \omega L \Rightarrow \gamma_{ij} \approx 0 \text{ car } arctan(0) = 0\\
\theta_i \approx \theta_j
$$

Ce qui implique que le transit de puissance active sur une ligne entre les nœuds $i$ et $j$ s'écrit :
$$
T_{ij} \approx V_{ref}^{2}Y_{ij}(\theta_i - \theta_j)\text{ avec }Y_{ij} = \frac{1}{Z_{ij}}
$$

De plus, à chaque nœud $i$ du réseau, l'injection active s'exprime :
$$
P_{i} = V_{ref}^{2} \times \sum_{j \in \alpha(i)} Y_{ij} \times (\theta_i - \theta_j)
$$
avec $alpha(ji)$ l'ensemble des nœuds voisins du nœud $i$.
Cette approximation permet donc d'obtenir une **relation linéaire entre les transits et les injections**.

Dans METRIX, nous raisonnons à partir de puissances et non d'angles ; mais nous déduisons les seconds des premiers 
grâce aux coefficients PTDF en approximation du courant continu. Ensuite, nous pouvons calculer le transit sur chaque 
ligne à partir de la production des groupes.

En contrepartie, nous n'avons aucune information sur le transit réactif des lignes, sur les chutes de tension, les 
problèmes de « puissance maximale transmissible », etc. Les pertes ne sont pas modélisées sur les lignes : le transit 
entrant dans une ligne ressort en intégralité de l'autre côté : $T_{ij} = -T_{ji}$.

#### Expression des transits pour METRIX

Les équations ci-dessus peuvent s'écrire sous forme matricielle :
$$
[p] = V_{ref}^{2} \times [A] \times [\theta]
$$

Avec :
 - $[p]$ le vecteur d'injecion active nodale,
 - $[\theta]$ le vecteur des phases à chaque nœud,
 - $[A]$ la matrice d'admittance du réseau. Elle est carrée et symétrique et de taille le nombre de nœuds dans le réseau :
 $$
 [A] = 
    \begin{cases}
        \alpha_{ii} = \sum_{j \in \alpha(i)} Y_{ij}\\
        \alpha_{ij} = 
            \begin{cases}
                0\text{ si }j \notin \alpha(i)\\
                -Y_{ij}\text{ sinon}
            \end{cases}
    \end{cases}
 $$

Dans METRIX, les variables de contrôle sont des données d'injections (production, délestage ou consigne de TD et HVDC) :
$$
[\theta] = \frac{1}{V_{ref}^{2}}[A]^{-1}\times[p]
$$

Or $T_{ij} = V_{ref}^{2}\times Y_{ij}\times (\theta_i - \theta_j)$, donc $T_{ij} = [Y][A]^{-1}[p]$, avec $[Y]$ un 
vecteur ligne de la taille des nœuds :
$$
[Y] = 
\begin{cases}
    y_{ij} = Y_{ij}\\
    y_{ji} = -Y_{ij}\\
    0\text{ sinon}
\end{cases}
$$

Il est possible de calculer le vecteur ligne :
$$
[B] = [Y][A]^{-1}
$$

Nous pouvons alors exprimer le transit dans une ligne en fonction des variables d'injections (dnc de toutes les 
variables de contrôle) :
$$
T_{ij} = [B]\times[p]
$$

METRIX utilise cette expression du transit pour écrire une contrainte dans le problème d'optimisation.

#### Existence de solutions équivalentes
Il est important de noter que, puisque simuler l'*Adequacy phase* correspond à résoudre un problème d'optimisation, 
il peut y avoir des **solutions équivalentes**, i.e. des solutions avec le même coût optimal, mais des actions 
différentes sur le réseau pour atteindre l'équilibre. Cela entraîne des situations initiales différentes pour la 
*Redispatching phase*, et donc des solutions optimales différentes, avec des coûts optimaux complètement différents. 
Identiquement, pour une même situation initale issue de l'*Adequacy phase*, la *Redispatching phase* peut aussi 
renvoyer des solutions équivalentes.

## Modélisation du réseau
Nous allons désormais présenter la modélisation des éléments du réseau et du problème d'optimisation dans METRIX. 
Pour cela, nous allons nous focaliser sur **une seule variante**.

### Zones sychrones
Tous les éléments du réseau (groupes, consommations, TDs, LCCs) sont liés à un ou plusieurs nœuds. Chaque nœud fait 
partie d'une unique zone synchrone, et chaque zone synchrone en contient plusieurs milliers. Notons $ZC$ l'ensemble 
des zones synchrones du réseau.

### Groupes de production

Le modèle de METRIX n'est pas destiné à l'optimisation fine de la production. De ce fait, la modélisation des groupes 
est simplifiée. En tant que modèle statique, METRIX ignore la dynamique de démarrage des groupes et certaines contraintes 
de fonctionnement des différents moyens de production. METRIX ne connaît que leurs bornes de variations et leurs coûts. 
Tous les groupes sont décrits de la même manière indépendamment de leur type.

Dans les phases d'*Adequacy* (i.e. d'équilibrage) et de *Redispatching*, METRIX tient toujours compte de la puissance 
maximale (*Pmax*) du groupe.
En revanche, dans la phase d'équilibrage, METRIX ne tient pas compte de la puissance minimale (*Pmin*) si celle-ci est 
positive. En d'autres termes, METRIX peut donc démarrer un groupe entre 0 et Pmax. En pratique, cela se produit tout au 
plus pour le groupe marginal (dernier groupe ajusté).

Dans la phase de *Redispatching* les Pmin sont prises en compte et METRIX ne peut donc pas arrêter un groupe qui est 
démarré dans la phase d'équilibrage.

Il est possible de contrôler pour chaque groupe sa participation dans chacune des phases via la définition de coûts. 
À chaque groupe, nous pouvons associer :
- Le coût d'équilibrage à la hausse qui représente le coût de production par MW utilisé dans la phase d'équilibrage.
- Le coût d'équilibrage à la baisse est utilisé dans la phase d'équilibrage.
- Le coût de 'redispatching' à la hausse correspond au coût d'augmentation en préventif de la puissance de consigne du 
groupe dans le mécanisme d'ajustement. Ce coût est également utilisé en curatif.
- Le coût de 'redispatching' à la baisse correspond au coût de baisse en préventif de la puissance de consigne du 
groupe dans le mécanisme d'ajustement. Ce coût est également utilisé en curatif.

Pour une même phase, il faut toujours définir un coût à la hausse et à la baisse.
Le coût de démarrage des groupes n'est pas pris en compte par METRIX.

Cf. [Variables de production](#prod_var)

#### Convention de signe
Soit un coût à la hausse $\Gamma^{+}$ et un coût à la baisse $\Gamma^{-}$. Si nous augmentons la production du groupe 
de $P^{+}$, cela coûtera $\Gamma^{+} \times P^{+}$. Si nous baissons la production de $P^{-}$ ($P^{-} \geq 0$), cela 
coûtera $\Gamma^{-} \times P^{-}$.

Avec un coût positif, cela « coûte » de modifier une production. Avec un coût négatif, cela « rapporte » de modifier 
une production.

En conséquence, si des valeurs négatives sont utilisées pour les coûts à la baisse et que le coût à la baisse d'un 
groupe est supérieur en valeur absolue au coût à la hausse d'un autre groupe, METRIX peut modifier le coût de 
production uniquement pour bénéficier de cette « opportunité » sans que cela soit motivé par une contrainte 
d'équilibrage ou de transit. Les paramètres `adequacyCostOffset` et `redispatchingCostOffset` permettent de contrer 
ce comportement dans chacune des phases et doivent être positionnés à la valeur absolue du plus grand coût négatif.

Si rien n'est spécifié (i.e. aucun coût n'est défini), tous les groupes du réseau peuvent participer aux deux phases à 
coût nul.

Dès qu'au moins un groupe est configuré, seuls les groupes pour lesquels un coût est défini pour une phase peuvent 
participer à cette phase. La consigne de production des autres groupes ne peut pas être modifiée.
Si trop peu de groupes peuvent agir, le modèle peut ne pas pouvoir trouver de solution aux contraintes et retournera 
alors un code d'erreur 1 (ex. contrainte d'évacuation sur un groupe non modifiable).

### Consommations

METRIX n'utilise que la consommation active puisqu'il repose sur l'approximation du courant continu. Une autre 
conséquence de cette approximation est qu'il n'y a pas de pertes modélisées sur les lignes. Les pertes sont estimées 
a posteriori (cf. [Pertes calculées a posteriori](#posteriori_losses)).
Les consommations doivent donc être renseignées pertes incluses.

Afin de résoudre des contraintes de transit, METRIX a la possibilité de délester de la consommation.

Ce délestage peut prendre 3 formes :
- Délestage dans la phase d'équilibrage pour respecter $P = C$
- Délestage préventif pour respecter les contraintes de transit
- Délestage curatif pour respecter les contraintes de transit

Le niveau de délestage est défini par un seuil qui correspond au pourcentage de la consommation qui peut être délesté. 
Le coût de ce délestage peut également être défini (sinon la valeur par défaut, `COUTDEFA`, est utilisée). Le coût 
et le seuil de délestage sont utilisés, à la fois pour le délestage de la phase d'équilibrage et pour le délestage
préventif de la phase de *Redispatching*. Le délestage curatif utilise un autre coût et un autre seuil.

Par défaut, si rien n'est spécifié, toutes les consommations sont délestables à 100% dans les deux phases. Dès qu'au 
moins une consommation est configurée, seules les consommations configurées sont délestables.
Si trop peu de consommations sont délestables, le modèle peut ne pas pouvoir trouver de solution aux contraintes et 
retournera un code d'erreur 1.
Si nous souhaitons modéliser plusieurs coûts de délestage/effacement associés à différents seuils, il faut créer des 
charges fictives supplémentaires sur le même nœud et répartir la consommation active sur ces charges.

Cf. [Variables de consommation](#conso_var)

### Lignes et transformateurs

#### Transformateur-Déphaseurs

##### Modélisation
Dans METRIX, un TD est lié à un quadripôle et sert d'échangeur de puissance entre les deux nœuds dudit quadripôle : 
il prélève de la puissance sur un nœud pour l'envoyer à un autre.

Pour bien comprendre le fonctionnement des TDs, il faut, tout d'abord, concevoir que METRIX utilise des puissances 
pour faire ces calculs, alors que les TDs fonctionnent avec des angles, en modifiant le déphasage du signal électrique. 
Cependant, changer le déphasage revient à changer la puissance active dudit signal électrique, grâce à une simple 
multiplication : 

$$
Puissance = angle \cdot \frac{\pi}{180} \cdot U^2 \cdot Y_{i,j}
$$

Avec $U^2$ et $Y_{i,j}$ les valeurs de tension et d'impédance du quadripôle support du TD, qui sont des paramètres 
dans METRIX. Puisque nous allons être amenés à parler d'angles ou de puissances pour décrire le fonctionnement des 
TDs, afin de ne pas être perturbés, il suffit de se souvenir que les deux sont proportionnels selon cette formule.

Pour les calculs de METRIX, le quadripôle de support du TD va être dissocié en deux quadripôles en série. Nommons 
***quad*** le quadripôle initial, avec $(y, r)$ son impédance et sa résistance. Celui-ci relie les nœuds *Or* et *Dest* 
(i.e. origine et destination). Nous allons ensuite créer un **nœud fictif** *Nf* ainsi qu'un **quadripôle fictif** 
***quadFictif***, allant du nœud *Or* au nœud *Nf* et de caractéristiques $(y/0.1, 0)$. Parallèlement, nous modifions 
*quad* afin qu'il aille du nœud *Nf* vers le nœud *Dest*. Nous modifions également ses caractéristiques pour qu'elles 
vaillent $(y/0.9, r)*.

```{figure} img/quad.png
:name: quad_fig
:alt: Transformation du TD des données à la modélisation dans METRIX
:width: 600px
:align: center

Transformation du TD des données à la modélisation dans METRIX
```

Le TD est porté par *quadFictif*, et assure donc le déphasage. Le quad réel *quad* assure la partie réactance. 
Les pertes liées à r sont calculées à posteriori, lors de l'affichage de la solution.

**N.B.** : Les TDs sont portés par les lignes. Ces dernières étant ignorées en *Adequacy phase*, les TDs n'ont aucun 
rôle à jouer en *Adequacy phase*.

##### Définition des valeurs min et max des TDs

Dans la réalité, le déphasage du signal électrique se fait en passant d'une prise à l'autre. De ce fait, au sein de 
METRIX, chaque TD va être associé à une liste de prises de déphasage croissant ainsi qu'à deux bornes maximum 
correspondantes aux nombres maximum de changements de prises à la hausse et à la baisse : *lowran* et *uppran*. 
Ces bornes sont utilisées de la manière suivante, avec $X$ le numéro de la prise du déphasage initial du TD : 
 - à la hausse le déphasage ne pourra dépasser $X + uppran$
 - à la baisse le déphasage ne pourra dépasser $X - lowran$

Dans METRIX, le numéro de la prise de déphasage correspond à celui de la prise minimisant la distance entre les 
déphasages du TD et celui associé à la prise.

Cf. [Variables TDs](#td_var)

#### Lignes à courant continu

Les lignes à courant continu (ou LCC) permettent de transporter du courant continu, plutôt que de l'alternatif 
comme les quadripôles traditionnels. Une LCC est décrite par :
- Deux convertisseurs, chaque convertisseur se situe entre un nœud AC et un nœud DC ;
- et une ligne entre les deux nœuds DC.

De ce fait, une ligne LCC fait donc le lien entre deux nœuds AC :

```{figure} img/lcc_AC_DC.png
:name: lcc_AC_DC_fig
:alt: Schéma descriptif d'une LCC
:width: 600px
:align: center

Schéma descriptif d'une LCC
```

Une ligne HVDC est simulée, par METRIX, comme deux injections sur les nœuds AC origine (nœud 1) et extrémité (nœud 2) :

```{figure} img/lcc_injection.png
:name: lcc_injection_fig
:alt: Schéma descriptif de la simulation d'une LCC par METRIX
:width: 600px
:align: center

Schéma descriptif de la simulation d'une LCC par METRIX
```

La convention de signe pour la puissance transitant dans une station de conversion est la suivante : lorque la 
consigne de $n$ MW est données sur la station 1, cela revient à faire transiter $n$ MW de 1 vers 2, ce qui équivaut 
au soutirage de $n$ MW de la station 1 et à une injection de $n$ MW dans la station 2.

Une LCC peut être pilotée de différentes manières : en puissance ou en émulation AC (cf. paramètres `DCNDROOP` et 
`DCDROOPK`). En outre, pour ces deux types pilotages, celui-ci peut être imposé, (la puissance de transit est alors 
fixe), ou optimisé (la puissance de transit peut varier, au moins en préventif lors de la *Redispatching phase*).

$P_0$ étant la puissance de consigne sur la ligne, celle-ci peut être modifiée ou non suivant le caractère du pilotage retenu.

Les pertes HVDC sont calculées a posteriori (cf. [Pertes calculées a posteriori](#posteriori_losses)).

Cf. [Variables LCCs](#lcc_var)

##### Lien entre zones synchrones
Les LCCs servant de lien entre deux nœuds et transportant du courant continu, elles peuvent aussi servir 
d'interconnexions entre des zones synchrones différentes (contrairement à des quadripôles classiques). 

##### Lignes à courant continu pilotées en émulation AC

Une ligne peut être pilotée en émulation AC (cf. paramètres *`DCNDROOP`* et *`DCDROOPK`*). Le transit de la liaison HVDC 
vaut alors $𝑃_0 + 𝑘(\theta_2 − \theta_1)$. METRIX insère alors un TD d'impédance $1/𝑘$ entre les deux injections afin de
respecter la contrainte précédente. Le TD assure que la valeur du transit global de la liaison HVDC soit toujours 
comprise entre $P_{min}$ et $P_{max}$. La valeur de $P_0$, quant à elle, peut être fixe ou optimisée par METRIX 
(cf. paramètre *`DCREGPUI`*).

**Modélisation des LCCs en émulation AC**
Pour une LCC en émulation AC, un quadripôle fictif (nommé *quad0*) lui est associé, de mêmes nœuds origine et 
destination. La résistance de ce quadripôle est nulle et son admittance est déterminée par un paramètre fourni 
individuellement à chaque LCC en émulation AC. Nous associons ensuite, à ce quadripôle, un TD fictif en pilotage 
d'angle optimisé, qui provoque donc la création d'un nouveau nœud fictif *Nf* et d'un quadripôle (doublement) fictif 
*quad1*. Le schéma ci-dessous résume cette situation.

```{figure} img/lcc_model.png
:name: lcc_model_fig
:alt: Modélisation des LCCs en émulation AC dans METRIX
:width: 600px
:align: center

Modélisation des LCCs en émulation AC dans METRIX
```

Le quadripôle *quad0* et la LCC formeront un élément à surveiller en $N$ et en $N-k$. Quant au TD fictif, il sera mis 
à disposition en curatif de tous les incidents simulés.

**En résumé** : Les LCCs permettent de transporter une puissance choisie d'un nœud à un autre (et possiblement d'une 
zone synchrone à une autre). Ce qui peut par exemple éviter les surcharges sur les lignes adjointes.

### Incidents

La solution trouvée par METRIX (plan de production et délestage) doit être robuste à une liste d'incidents donnée en 
entrée. Nous distinguons les incidents de lignes et les incidents de groupes.

Cf. [Variables Incidents](#inc_var)

#### Incidents lignes

METRIX est capable de simuler la perte d'une ou plusieurs ligne(s). Il utilise toujours l'approximation du courant 
continu et donc des coefficients de report pour simuler ces incidents. Ici, le transit sur la ligne $ik$ après 
l'incident qui simule la perte de la ligne $mn$ s'écrit : 
$$
T_{ik}^{N-k} = T_{ik}^{N} + \rho_{ik}^{mn} \cdot T_{mn}^{N}
$$

Afin de déterminer $\rho_{ik}^{mn}$ (coefficient de report), METRIX utilise la formule de Woodbury.

À noter que, les lignes à courant continu ne permettent pas d'assurer la connexité entre leur nœud origine et extrémité. 
Par conséquent, les incidents simulant la perte de la ligne d'alimentation de la ligne à courant continu seront écartés 
(ils rompent la connexité). Par contre, si l'incident est bien défini sur la ligne DC (i.e. sur la ligne entre les deux 
nœuds DC), alors l'incident simulant la perte de la ligne à courant continu sera bien simulé.

#### Incidents groupes

Dans le cas où des incidents de groupes sont définis, METRIX répartit la production perdue lors de l'incident sur 
l'ensemble des autres groupes disponibles (même s'ils ne sont pas réellement démarrés).

La réserve de fréquence globale du réseau est calculée à partir de la puissance perdue par l'incident groupe le plus 
dimensionnant. Cela permet d'obtenir la « demi-bande de réglage » sur chaque groupe (qui vaut 0 s'il n'y a pas 
d'incident groupe). Avant de commencer la résolution, METRIX abaisse la Pmax de chaque groupe de sa « demi-bande de 
réglage ».

Pour simuler les incidents de groupe, METRIX utilise un coefficient de sensibilité. Pour chaque incident $i$, METRIX 
calcule l'influence sur chaque ligne $ik$, noté $\rho_{ik}^{g}$, de la perte de 1 MW sur le groupe en incident $g$ et 
la reprise de ce MW perdu par l'ensemble des autres groupes disponibles au prorata de leur Pmax.

MERIX déduit ensuite le transit sur la ligne $ik$ après la perte du groupe $g$ :
$$
T_{ik}^{N-k} = T_{ik}^{N} + \rho_{ik}^{g} \cdot P_{g}^{N}
$$

avec $P_{g}^{N}$ la puissance délivrée en N par le groupe qui a été déclenché.

La même stratégie est utilisée pour simuler la perte d'une ligne DC ; la seule différence est que la variation de 1 MW 
se compense entre le nœud origine et le nœud extrémité de la ligne DC.

#### Incidents composés

METRIX traite également les incidents composés de perte de groupes et de lignes. Etant donné que nous nous trouvons 
dans le conetxte de l'approximation du courant continu, les équations sont linéaires et l'ordre d'apparition de la 
perte du groupe ou de la ligne n'a pas d'importance.

Le transit sur la ligne $ik$ après perte de la ligne $mn$ et du groupe $g$ vaut :
$$
T_{ik}^{N-k} = T_{ik}^{N} + \rho_{ik}^{g} \cdot P_{g}^{N} + \rho_{ik}^{mn} \cdot (T_{mn}^{N} + \rho_{mn}^{g} + P_{g}^{N})
$$

Cela revient à considérer que l'incident groupe se produit dans un premier temps, puis que l'incident ligne et l'impact 
produit de l'incident groupe sur cette ligne est bien simulé ($T_{mn}^{N} + \rho_{mn}^{g} + P_{g}^{N}$).

#### Incidents rompant la conenxité

Par défaut, les incidents rompant la connexité sont exclus du calcul. Il est toutefois possible de les prendre en 
compte via le paramètre `INCNOCON`. Dans ce cas, pour chaque incident, METRIX renseigne dans les sorties le volume 
de production et/ou de consommation perdu lors de l'incident.

Si une parade topologique permet de récupérer une partie de cette puissance, cette information est donnée pour 
l'incident initial et pour la parade.

### Actions curatives

Afin de corriger des incidents, l'utilisateur peut modéliser des actions curatives (i.e. activables une fois l'incident 
survenu) représentant les actions prises par un opérateur ou un automate pour rétablir le transit conformément au seuil 
admissible.

Dans METRIX les actions curatives peuvent être :
- des modifications de consigne des groupes, TD ou HVDC ;
- ou du délestage curatif de consommation.

L'optimiseur choisira s'il est utile de modifier la consigne des éléments curatifs suite à l'incident. Il combinera 
les actions si nécessaire et s'assurera que les modifications de consigne n'engendrent évidemment pas de nouvelle 
contrainte sur une autre ligne du réseau.

Pour respecter une contrainte sur un seuil temporaire avant activation d'actions curatives, METRIX utilisera 
obligatoirement une action préventive. Cette action préventive pourra être complétée, si nécessaire, par une action 
curative pour respecter le seuil permanent après incident.

Si la fonctionnalité du curatif est utilisée pour modéliser le fonctionnement d'un automate, il faut bien prendre en 
compte que la modélisation est optimiste : en effet, un automate agira uniquement dans le but de lever la contrainte 
sur la ligne qu'il surveille ; de plus, il agira même si cela engendrait une contrainte ailleurs. METRIX peut utiliser 
un levier pour lever n'importe quelle contrainte présente sur le réseau et il fait en sorte de ne pas créer de 
nouvelles surcharges ailleurs.

### Manœuvres topologiques curatives

Les parades topologiques sont des actions curatives traitées de manière différente des autres actions curatives, car 
ayant un impact sur la topologie, elles modifient les coefficients de report et de sensibilité. Elles ont également un 
fort impact sur le temps de résolution de l'optimisation.

Pour METRIX, une parade topologique est toujours liée à un incident et consiste en l'ouverture (ou la fermeture) de
lignes ou de couplages supplémentaires. Quand une parade est sélectionnée par le solveur, METRIX vérifie que cette 
parade ne génère pas de contraintes sur les autres ouvrages surveillés du réseau.

Pour un incident donné, METRIX privilégiera toujours les parades situées en début de la liste. Ainsi si aucune des 
parades fournie n'est vraiment efficace, METRIX choisira la parade « ne rien faire » qui est automatiquement ajoutée 
en tout début de liste.

METRIX peut combiner une parade topologique avec d'autres actions curatives, mais ne peut pas combiner deux parades 
topologiques. Il faut explicitement renseigner toutes les combinaisons souhaitées dans la liste fournie en entrée du 
calcul.

Des parades peuvent avoir des effets très proches, difficilement différentiables, ce qui complique la résolution du 
problème. Pour contrer ces cas pathologiques, le paramètre `PAREQUIV` permet de masquer les parades dont l'effet 
semble similaire à celui d'une autre parade.

Une parade topologique sélectionnée par METRIX permet de lever (ou soulager) les contraintes de transit liées à un 
incident. Cependant, si cette parade est composée de plusieurs actions, il n'y a pas de garantie qu'il sera possible 
de réaliser toutes ces actions en pratique. De même, une parade peut être utilisée pour reconnecter une proche perdue 
par un incident, mais compte tenu des simplifications du modèle METRIX, il n'est absolument pas assuré que cela serait 
effectivement possible en pratique.

Par défaut, une parade ne peut pas aggraver la rupture de connexité d'un incident (i.e. augmenter le nombre de sommets 
déconnectés), sauf si le paramètre `PARNOCON` est utilisé.

Il est possible de restreindre l'action d'une parade à la présence d'une contrainte sur un ouvrage spécifique. Dans 
ce cas, la parade ne peut pas être sélectionnée tant que l'ouvrage en question n'est pas en contrainte.

## Problèmes d'optimisation

Notons $W$ le nombre entier de variantes à traiter et $w$ l'indice de la variante courante.


(algo-adeq_math)=
### Adequacy phase
Notons $P_w$ ma matrice colonne des puissances produites par les groupes et $C_w$ la matrice colonne des puissances 
consommées par les zones de consommation. Notons également $P_{w}^{0}$ et $C_{w}^{0}$ les matrices des valeurs
initiales.

$$
\forall w \leq W \text{ :}
$$

$$
\text{Données : }\\
P_{w}^{max}, P_{w}^{min}, C_{w}^{max}, C_{w}^{min}, P_{w}^{0}, C_{w}^{0}
$$

$$
\text{Variables :}\\
P_{w}, C_{w}, \Delta P_{w}, \Delta C_{w}
$$

$$
\text{Objectif :}\\
min(\Delta P_{w} + \Delta C_{w})
$$

$$
\text{s.c. :}
$$

(algo-Pbounds_eq)=
$$
\begin{equation}
P_{w}^{min} \leq P_{w} = P_{w}^{0} + \Delta P_{w} \leq P_{w}^{max}
\end{equation} 
$$

(algo-Cbounds_eq)=
$$
\begin{equation}
C_{w}^{min} \leq C_{w} = C_{w}^{0} + \Delta C_{w} \leq C_{w}^{max}
\end{equation}
$$

(algo-adeq_eq)=
$$
\begin{equation}
P_{w} = C_{w}
\end{equation}
$$

Les contraintes [(1)](#algo-Pbounds_eq) et [(2)](#algo-Cbounds_eq), définissent les limites des matrices $P_w$ et $C_w$, ainsi 
que leur lien via la fonction objectif. La contrainte [(3)](#algo-adeq_eq) consitue la contrainte de base du réseau : à 
tout instant, la production et la consommation doivent être égales.

(algo-redis_math)=
### Redispatching phase
Nommons $U_w$ la matrice colonne des actions préventives et $V_w$ la matrice colonne des actions curatives pour la 
variante $w$. Ces deux matrices contiennent les variables représentant les changements de production des groupes, de 
consommation des zones de consommation, de déphasage des Transfo-Déphaseurs (TDs) du réseau et de flux des Lignes à 
Courant Continu (LCCs). $V_w$ contiendra également les variables booléennes d'activation des parades topologiques. 
En notant $p_i$, $c_i$, $td_i$, $lcc_i$, $prd_i$ les valeurs de production, consommation, de déphasage des 
TDs, de flux sur les LCCs et d'activation des parades, et en notant $n_1$, $n_2$, $n_3$, $n_4$, $n_5$ leurs 
cardinalités, nous pouvons formuler $U_w$ et $V_w$ de la manière suivante : 
$$
U_w=(p_1, …, p_{n_1}, c_1, …, c_{n_2}, td_1, …, td_{n_3}, lcc_1, …, lcc_{n_4}, 0, …, 0)^t \\
V_w=(p_1, …, p_{n_1}, c_1, …, c_{n_2}, td_1, …, td_{n_3}, lcc_1, …, lcc_{n_4}, prd_1, …, prd_{n_5})^t
$$

$U_w$ et $V_w$ sont donc toutes deux de tailles $n_1 + n_2 + n_3 + n_4 + n_5$. De même, notons $U_{w}^{1}$ et 
$V_{w}^{1}$ les matrices de leurs valeurs initiales pour ce problème. Nous noterons que, dans le cas de $U_{w}^{1}$, 
les paramètres $p_1, ..., p_{n_1}, c_1, ..., c_{n_2}$ sont des $argmin$ du problème résolu en *Adequacy phase*.

Notons également, $F_w$ la matrice des flux des lignes et $M_w$ la matrice de répartition, qui, à partir des actions 
préventives et curatives, permet de calculer le flux sur les lignes en N et sur les différents incidents. En notant 
$m$ le nombre de lignes et $k$ le nombre d'incidents, $F_w$ est une matrice colonne de taille $m \cdot k$ et $M_w$ 
une matrice rectangulaire de taille $(m \cdot k) \cdot 2(n_1 + n_2 + n_3 + n_4 + n_5)$.

Enfin notons, 

$$
\forall w \leq W \text{ :}
$$

$$
\text{Données : }\\
U_{w}^{1}, U_{w}^{min}, U_{w}^{max}, V_{w}^{min}, V_{w}^{max}, F_{w}^{min}, F_{w}^{max}, M_w
$$

$$
\text{Variables :}\\
U_{w}, V_{w}, \Delta U_{w}, \Delta V_{w}
$$

$$
\text{Objectif :}\\
min(\Delta U_{w} + \Delta V_{w})
$$

$$
\text{s.c. :}
$$

(Ubounds_eq)=
$$
\begin{equation}
U_{w}^{min} \leq U_{w} = U_{w}^{1} + \Delta U_{w} \leq U_{w}^{max}
\end{equation} 
$$

(Vbounds_eq)=
$$
\begin{equation}
V_{w}^{min} \leq V_{w} = V_{w}^{1} + \Delta V_{w} \leq V_{w}^{max}
\end{equation}
$$

(UeqV_eq)=
$$
\begin{equation}
V_{w}^{1} = U_{w}
\end{equation}
$$

(PCfUV_eq)=
$$
\begin{equation}
(P_{w}, C_{w}) = f(U_{w}, V_{w})
\end{equation}
$$

(PC_eq)=
$$
\begin{equation}
P_{w} = C_{w}
\end{equation}
$$

(FM_eq)=
$$
\begin{equation}
F_{w}^{min} \leq F_{w} = M_{w} \cdot  \begin{align*} U_{w}\\ V_{w}\end{align*} \leq F_{w}^{max}
\end{equation}
$$

Les équations [(4)](#Ubounds_eq) et [(5)](#Vbounds_eq) définissent les encadrements des atrices $U_{w}$ et $V_{w}$, 
ainsi que leur lien avec la fonction objectif. Autrement dit, elles définissent les limites des différentes productions, 
consommations, des TD, des HVDC, etc., en préventif et curatif. L'équation [(6))](#UeqV_eq) définit le fait que l'état 
initial des actions curatives correspond à l'état du réseau en N, i.e. ce qui a été déterminé avec les actions 
préventives. L'équation [(7)](#PCfUV_eq) indique que l'état de la production et de la consommation en N et en incident 
est stockée dans $U_w$ et $V_w$. L'équation [(8)](#PC_eq) rappelle le nécessaire équilibre production – consommation 
en N et en incidents. Enfin, l'équation [(9)](#FM_eq) définit et encadre le flux des lignes.

## Formulation du problème

### Variantes simulables

Lorsque les modifications sont communes à l'ensemble des variantes, elles sont décrites dans une variante d'index 
« -1 ». Ces variantes sont alors directement appliquées sur le cas nominal.

Une variante METRIX peut modifier :
- La disponibilité de lignes (y compris couplage)
- Les valeurs des consommations
- Le coût de délestage des consommations
- La disponibilité des groupes
- La production des groupes
- La puissance min et max des groupes
- Les coûts d'empilement hausse/baisse des groupes (phase d'équilibrage)
- Les coûts d'ajustement hausse/baisse des groupes
- Les puissances min, max et de consigne des lignes HVDC
- Les seuils N, N-1 sur incident spécifique et avant manœuvre des ouvrages
- Le déphasage initial et les déphasages min et max des TDs

## Sorties de METRIX

Les résultats de METRIX se présentent sous forme de chronique avec une valeur par variante. Pour réduire le volume des 
sorties, seules les valeurs qui diffèrent des données d'entrée sont fournies.

Cf. [Fichiers de résultats](#io-result-file)

### Statut du calcul

Cf. [Tableau C1](#status_output)

### Transits sur les ouvrages

Cf. [Tableau R3](#io-table_r3), [Tableau R3B](#io-table_r3b) et [Tableau R3C](#io-table_r3c).

### Résultats sur les variables de contrôles

Cf. [Tableau R5](#io-table_r5), [Tableau R5B](#io-table_r5b), [Tableau R6](#io-table_r6) et [Tableau R6B](#io-table_r6b).

### Résultats sur les parades

Cf. [Tableau R10](#io-table_r10)

### Le coût GRT ou de 'redispatching'

Cf. [Tableau R7](#io-table_r7)

### La défaillance du réseau

En résultat de METRIX, l'utilisateur dispose du volume de dépassement et des coûts en préventif et en curatif 
(i.e. post-incident).

Cf. [Tableau R9](#io-table_r9)

(posteriori_losses)=
### Pertes calculées a postétiori

À l'issue du calcul, METRIX calcule les pertes a posteriori.

Cf. [Tableau R8](#io-table_r8) et [Tableau R8B](#io-table_r8b).

#### Sur les lignes AC

Compte tenu des flux actifs qu'il a calculés, il estime les pertes sur les dipôles par la formule : 
$pertes = R \times (\frac{T}{V})^2$ où $R$ est la résistance de la ligne et $T$ la puissance active transitant sur 
le quadripôle.

**N.B.** : il s'agit d'une estimation des pertes sur la partie active seulement, on suppose que le transit réactif est 
nul.

#### Sur les lignes DC

En ce qui concerne les liaisons à courant continu, METRIX trouve une puissance de consigne $P$ appliquée au redresseur. 
Les pertes sur les liaisons à courant continu sont décomposées en trois parties : les pertes dans chacun des deux 
convertisseurs et les pertes dans le câble. Le détail du calcul ci-dessous ($red$=redresseur et $ond$=onduleur) :
 - **Pertes dans la station de conversion côté redresseur**
 Les pertes sont proportionnelles à la puissance transitée dans la station : $P$. METRIX utilise le coefficient de 
pertes par station. $PerteStationOr=coeffPerteOr \times P$. À l'origine du câble, il y a donc la puissance $P_{orCable}=(1−coeffPerteOr)\times P$.
 - **Pertes dans le câble**
 $pertesCable=R\times I_{DC}^{2}$ avec $I_{DC}=\frac{(V_{DC}^{red}−V_{DC}^{ond})}{R}$, 
$P_{orCable}=V_{DC}^{red} \times I_{DC}$ et $V_{DC}^{ond}=V_{DC}^{nom}$ (i.e. la tension à l'onduleur est égale à la
tension nominale DC). La valeur de $V_{DC}^{red}$ est trouvée en résolvant un polynôme de degré 2 en $V_{DC}^{red}$, 
ce qui permet de déterminer les pertes sur le câble.
 - **Pertes dans la station de conversion côté onduleur**
 Les pertes sont proportionnelles à la puissance transitée dans la station $P_{exCable}$. METRIX utilise le 
coefficient de perte par station : $PerteStationOnduleur = coeffPerteOnduleur \times P_{exCable}$, avec 
$P_{exCable} = P - pertesCable$. Ce calcul de perte dans les liaisons DC est le même que celui utilisé dans Convergence.

### Variations marginales

Les variations marginales permettent d'avoir des informations sur les contraintes qui limitent la solution.
Cf. [Tableau R4](#io-table_r4) et [Tableau R4B](#io-table_r4b).

#### Variations marginales sur les lignes AC

La variation marginale (VM) d'une ligne à courant alternatif (AC) indique le gain sur la fonction de coût si la limite 
de transit sur la ligne AC était de 1MW supplémentaire. Si ce qui limitait le problème était :
- Un transit en N, la VM donne le gain sur la fonction de coût si le seuil en N de la ligne était de 1 MW supplémentaire.
- Un transit en N-1, la VM donne le gain sur la fonction de coût si le seuil en N-1 de la ligne était de 1 MW supplémentaire. 
Pour une VM en N-1, il est possible de savoir quel est l'incident qui a conduit à cette contrainte limitante.

#### Variations marginales sur les lignes HVDC

Une VM sur une HVDC indique le gain sur la fonction de coût si la capacité de la liaison HVDC était d'1 MW supplémentaire.
Dans le cas où la liaison HVDC peut agir en curatif plusieurs variations marginales seront données :
- La VM globale HVDC indique le gain sur la fonction de coût si la capacité de la liaison HVDC était d'1 MW 
supplémentaire. Le gain est ensuite détaillé dans les VM suivantes.
- La VM préventive correspond au gain sur la fonction de coût si la plage admissible pour la consigne préventive 
était augmentée de 1 MW.
- La VM curative par incident i correspond au gain sur la fonction de coût si la plage admissible pour la consigne 
curative sur l'incident i était augmentée de 1 MW.

**N.B.** : aujourd'hui, dans Assess, il n'y a, en pratique, qu'une plage admissible pour la consigne des HVDC. Cette 
plage est utilisée par les HVDC en préventif comme en curatif. Les 2 dernières VM permettent cependant d'apporter des 
informations supplémentaires : la VM globale annonce un gain lorsque la plage admissible est augmentée d'1 MW. Grâce 
aux VM préventives et curatives, nous pouvons savoir si ce MW supplémentaire disponible serait utilisé en préventif 
ou en curatif et sur quel incident.

**Remarque** : les VM d'une HVDC sont directement liées à la borne de la HVDC (i.e. plage admissible). Il est donc 
nécessaire de prêter attention à l'interprétation si les bornes des HVDC sont tirées des variantes : d'une variante 
à l'autre, le gain annoncé par la VM ne correspond pas au même MW supplémentaire (à la même plage de fonctionnement).

#### Variations marginales sur les consommations

Si le mécanisme d'ajustement n'est pas simulé, la VM est donnée pour toutes les consommations. La VM de consommation 
indique l'impact sur la fonction de coût si la consommation était de 1MW de moins.

Inversement, si le mécanisme d'ajustement est simulé, la VM de consommation n'est retournée que sur les nœuds pour 
lesquels la défaillance est non nulle. Pour les autres nœuds, METRIX n'est pas capable d'évaluer le gain.

#### Variations marginales sur les sections surveillées

La VM d'une section surveillée indique le gain sur la fonction coût si la borne supérieure de la somme des transits 
sur la section était augmenté d'1 MW.

### Limitations

#### Domaine de validité limité

Les VM non nulles pointent un ensemble de lignes ou de contraintes de sécurité qui ont un impact direct sur la 
fonction à minimiser : ce sont les premières contraintes à résoudre pour faire baisser le coût de redispatching. 
La valeur de la VM donne un gain sur la fonction coût pour une modification du seuil de 1MW ; toutefois, il n'est pas 
garanti que cette VM fonctionne dans le cas où la variation n'est plus marginale (i.e. variation de nombreux MW). 
Autrement dit, augmenter le seuil N d'une ligne de $x$ MW, n'apportera peut-être pas un gain sur la fonction de coût 
de $x$ fois celui de la VM. 

Prenons l'exemple d'une antenne composée de deux lignes, la première ayant une IST plus faible que la deuxième. La VM 
sera associée à la première ligne. Toutefois, si nous augmentons l'IMAP de cette première ligne, la deuxième peut 
devenir limitante et ainsi le gain sera moindre par rapport à ce qu'annonçait la VM. Ce même phénomène peut se produire 
avec des groupes qui rentrent en butée Pmin ou Pmax.

#### Toutes choses égales par ailleurs

Le gain exprimé par la VM doit être compris « toute chose égale par ailleurs », c'est-à-dire que ce gain correspond 
au gain si seulement la capacité de la ligne est augmentée. Or, généralement, un tel changement sur le terrain implique 
également une modification de la réactance de la ligne. Cela modifierait donc le résultat.

#### Contraintes masquées

Comme décrit précédemment, la variation marginale indique le gain sur la fonction de coût si la plage admissible de la
ligne concernée était élargie. Par contre, ce résultat ne tient pas compte des limites des autres lignes. Par 
conséquent, en relâchant la limite sur une ligne, une autre ligne devenir limitante pour le problème.

METRIX n'a pas la possibilité d'expliciter à l'avance ces contraintes jugées masquées.

### Conclusions sur les VM

Les VM apportent des informations intéressantes pour l'étape d'analyse des contraintes que ce soit avant renforcement, 
ou après renforcement (contraintes résiduelles).

L'intérêt des VM est de pointer parmi toutes les lignes surveillées et les incidents, des éléments coûteux pour la 
fonction objectif. Un interclassement de ces VM permet de classer l'importance de ces contraintes.
