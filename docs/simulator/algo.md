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
ouvrages et des injections du réseau (production et consommation). Le résultat d'un load-flow permet de constater et d'évaluer les contraintes présentes.
Un **OPF**, quant à lui, peut modifier les injections/consommations et la topologie, de sorte, qu'après le lancement d'un load-flow, il n'y ait pas de
contrainte sur les ouvrages (ou qu'elles soient, du moins, minimisées).

De ce fait, l'objectif de METRIX est de trouver l'ajustement de moindre coût du plan de production afin de n'avoir
aucune contrainte de transit ni en N ni sur incidents. METRIX permet de mesurer le surcoût engendré par le réseau
sur le plan de production initial et permet de valoriser un renforcement de réseau.
Dans sa version 6, METRIX est intégré dans la plate-forme ImaGrid, depuis laquelle il peut être lancé sur des milliers de variantes.
Ces variantes permettent de modéliser des incertitudes sur la consommation, la production renouvelable, la disponibilité
des groupes, etc.

Afin d'obtenir un problème plus facile et rapide à résoudre, METRIX utilise l'**approximation du courant continu** pour
modéliser le réseau. Cependant, cette simplification ne permet pas de détecter les contraintes de tension et encore
moins les problèmes liés à la dynamique des événements.

Que ce soit via ImaGrid ou en exécution directe, un cas de base ainsi qu'un fichier annexe décrivant toutes les variantes
à calculer sont envoyés en entrée de METRIX. Ce dernier applique ensuite chacune des variantes au cas de base et
retourne un résultat pour chaque variante. Les résultats sont présentés sous forme de chroniques dans ImaGrid.
Ce document présente la modélisation du système électrique utilisée par METRIX, la logique de l'algorithme global,
le problème d'optimisation qu'il résout et les fonctionnalités disponibles au sein de METRIX.


## Objectif

**Le but de METRIX est de fournir une estimation du coût d'exploitation horaire d'un réseau fonctionnant de manière
optimale**, i.e. de coût le plus faible possible.

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

Pour résoudre le problème d'optimisation, nous allons **procéder par micro-itérations** : à chaque micro-itération, nous
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

Le déroulé détaillé de cette boucle — critères de sûreté vérifiés, moyens d'action mobilisables,
modes de calcul, puis détails d'implémentation — fait l'objet de la section
{ref}`micro-iterations` en fin de document.

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

La convention de signe pour la puissance transitant dans une station de conversion est la suivante : lorsque la
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

METRIX traite également les incidents composés de perte de groupes et de lignes. Étant donné que nous nous trouvons
dans le contexte de l'approximation du courant continu, les équations sont linéaires et l'ordre d'apparition de la
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

(micro-iterations)=
## Micro-itérations

METRIX ne formule pas d'emblée l'intégralité des contraintes de sûreté : il part d'un plan de production non
contraint et n'ajoute au problème d'optimisation que les contraintes effectivement violées, itération après
itération. Cette section décrit d'abord ce mécanisme d'un point de vue fonctionnel, puis son implémentation.

(micro-it-fonctionnel)=
### Description fonctionnelle

Cette section décrit le fonctionnement de la boucle de micro-itération sans référence au code : critères de
sûreté vérifiés, moyens d'action disponibles et modes de calcul proposés. Le pendant technique est décrit dans
{ref}`micro-it-implementation`.

#### 1. Principe général

METRIX recherche l'**ajustement de moindre coût** du plan de production afin qu'aucune limite thermique
ne soit violée, ni sur le réseau intact (situation N), ni après une contingence crédible (situations N-k).

Description d'une **micro-itération** :

::::{container} diagram-card
```{raw} html
:file: img/micro_it_boucle.svg
```
::::

---

#### 2. Seuils thermiques et régimes de sûreté

Chaque élément surveillé (ligne, transformateur ou section) se voit attribuer des **limites thermiques** qui dépendent du régime d'exploitation.
Ces limites représentent le transit de puissance active maximal que l'élément peut supporter selon différents horizons temporels.

::::{container} diagram-card
```{raw} html
:file: img/micro_it_seuils.svg
```

:::{container} legend
Le seuil retenu ne résulte pas d'une échelle croissante mais d'un arbre de décision à cas exclusifs, réévalué
à chaque comparaison entre un transit et une limite. Le seuil N s'applique en conditions normales
d'exploitation. Le seuil post-incident correspond au régime stabilisé, une fois les actions curatives
appliquées ; il est généralement plus élevé que le seuil N car il reflète la capacité thermique à court terme
de l'ouvrage. Le seuil avant action curative, plus permissif encore, couvre la brève fenêtre séparant
l'incident de l'exécution du curatif : il n'est retenu que sur les incidents dont les parades ont déjà été
activées. Le repli du cas par défaut n'est pas un maximum entre deux seuils : lorsque l'option ITAM est active
et que le seuil post-incident n'est pas renseigné, c'est le seuil avant action curative qui en tient lieu.
:::
::::

Toutes ces limites peuvent être redéfinies par variante, ce qui autorise des calibrages thermiques
propres à un scénario.

---

#### 3. Vérification de sûreté : scan en N et N-k

À chaque micro-itération, après que l'optimiseur a produit un dispatch de production, METRIX vérifie
qu'aucun élément surveillé ne dépasse sa limite thermique. Cette vérification est effectuée en deux étapes.

::::{container} diagram-card
```{raw} html
:file: img/micro_it_scan_surete.svg
```

:::{container} legend
Dans le cas général, les transits post-contingence ne sont pas obtenus en exécutant un load-flow complet par
contingence. METRIX s'appuie sur des facteurs de sensibilité linéaires — facteurs de report et facteurs de
distribution — établis à partir de la Jacobienne pour une topologie donnée, et réutilisés d'une micro-itération à
l'autre tant que la topologie ne change pas. Le transit post-contingence s'exprime alors comme une combinaison
linéaire du transit avant contingence et des variations d'injection, ce qui rend le scan N-k traitable même pour
des milliers de contingences et des centaines d'éléments surveillés.

Une famille de contingences échappe à cette règle : celles qui rompent la connexité en isolant une poche
contenant encore de la consommation ou de la production. La linéarisation ne s'applique plus, et METRIX résout
pour ces contingences un load-flow complet sur la topologie post-incident, à chaque micro-itération. La
factorisation correspondante est mise en cache par topologie, mais la résolution, elle, est refaite à chaque
passage. Le coût de la boucle croît donc avec le nombre de telles contingences, ce qui explique qu'elles soient
soumises à une option d'activation dédiée.
:::
::::

---

#### 4. Moyens d'action curatifs

Lorsqu'une surcharge est détectée sur une contingence, METRIX peut mobiliser deux catégories
d'actions correctives, en plus du redispatching préventif.

::::{container} diagram-card
```{raw} html
:file: img/micro_it_actions_correctives.svg
```

:::{container} legend
Pour une contingence donnée, l'optimiseur doit choisir exactement une parade — la somme des variables
d'activation est contrainte à valoir 1. Cette contrainte ne force jamais une manœuvre : dès qu'un incident
reçoit sa première parade, METRIX insère d'office en tête de liste une parade « ne rien faire », qui laisse la
topologie intacte. Lorsque l'option ITAM est active, cette parade fictive est également créée pour les
incidents qui disposent de moyens curatifs mais d'aucune parade topologique déclarée. Les actions curatives
(production, HVDC, déphaseurs) sont rattachées aux parades et opèrent donc dans la topologie post-parade
retenue ; elles sont partagées entre les parades d'un même incident, non dupliquées. Le coût d'activation
d'une parade est proportionnel à la probabilité de la contingence et au nombre de contraintes déjà associées à
l'incident, de sorte que les parades ne sont mobilisées que lorsque les actions préventives seules sont
insuffisantes.
:::
::::

##### Mécanisme ITAM

Lorsque l'option ITAM est activée (paramètre `TESTITAM` dans `fort.json`),
METRIX vérifie un seuil supplémentaire : la **limite avant action curative**
(`seuilMaxAvantCur_`), qui représente le transit que l'élément peut supporter
pendant la brève fenêtre entre la contingence et l'exécution effective de
l'action curative. Ce seuil est typiquement plus permissif que le seuil N-k
(qui s'applique en régime stabilisé après application du curatif).

L'activation de l'ITAM modifie cinq points du calcul :

1. **Sélection automatique des seuils** : tant qu'aucune parade n'est activée
sur un incident, METRIX utilise le seuil N-k classique (`seuilMaxInc_`). Une
fois les parades activées, c'est le seuil avant-curatif (`seuilMaxAvantCur_`)
qui s'applique pour les détections suivantes sur cet incident.

2. **Création de parades « ne rien faire »** : pour les incidents qui ont du
curatif disponible mais aucune parade topologique définie, METRIX crée
automatiquement une parade fictive « ne rien faire ». Cela permet au
mécanisme de parade de s'enclencher et de pouvoir ensuite vérifier le seuil
avant-curatif.

3. **Re-détection sur incidents à parades activées** : sans ITAM, on ne
re-détecte plus de contraintes sur un incident une fois ses parades
activées. Avec ITAM, la détection continue afin de vérifier que le seuil
avant-curatif est respecté ; si ce n'est pas le cas, de nouvelles
contraintes sont ajoutées.

4. **Contrainte ITAM dédiée** : lors de la première activation des parades
d'un incident, METRIX ajoute une contrainte supplémentaire au LP
correspondant explicitement au seuil avant-curatif, en plus des contraintes
par parade.

5. **Enregistrement de la menace pré-parade** : pour les sorties (résultats
R3 notamment), METRIX conserve trace de la menace maximale rencontrée
avant l'activation des parades (`menaceMaxAvantParade_`), permettant aux
analystes de mesurer le niveau de surcharge transitoire.

Sans ITAM (cas par défaut), seul le seuil N-k stabilisé est vérifié, et le
mécanisme est neutralisé sur les cinq points ci-dessus.

---

#### 5. Modes de calcul

METRIX propose quatre modes de calcul, chacun correspondant à un niveau d'optimisation différent.

::::{container} diagram-card
```{raw} html
:file: img/micro_it_modes_calcul.svg
```

:::{container} legend
En mode Load-flow, un seul passage est effectué (pas de boucle itérative). En OPF sans redispatching, la
production est figée et la défaillance interdite ; seuls les réglages HVDC et déphaseurs sont optimisés, et
les surcharges résiduelles sont signalées via des variables d'écart. Le mode OPF complet utilise la boucle de
micro-itération avec tous les moyens d'action disponibles. Le mode OPF avec tolérance de surcharge en diffère
sur trois points, et pas seulement sur le traitement des surcharges insolubles : les variables d'écart y
rendent les contraintes de transit souples, le délestage curatif n'y est pas lu, et les contraintes de
couplage entre variables ne sont pas posées.
:::
::::

---

#### Compléments fonctionnels

##### Génération de contraintes

Plutôt que de formuler le problème complet de sûreté dès le départ (ce qui inclurait une contrainte
par élément surveillé par contingence — potentiellement des millions de contraintes), METRIX démarre
avec un dispatch non contraint et ajoute progressivement uniquement les contraintes violées.

##### Facteurs de sensibilité pour le scan N-k rapide

Les flux post-contingence ne sont pas calculés par des load-flows indépendants. METRIX pré-calcule
des **facteurs de distribution de transfert de puissance** (PTDF) et des **facteurs de distribution de perte de ligne** (LODF)
à partir du modèle réseau en courant continu. Ces coefficients de sensibilité linéaires permettent d'estimer
les transits post-contingence comme une combinaison linéaire des flux pré-contingence et des
variations d'injection, rendant le scan N-k efficace en temps de calcul.

##### Escalade du solveur

L'optimisation démarre comme un simple **Programme Linéaire** (LP), résolu par la méthode du Simplexe. Dès
qu'une variable entière est introduite, le problème devient un **Programme Linéaire Mixte en Nombres Entiers**
(MIP) et le solveur bascule en Branch & Bound. Les trois situations qui déclenchent cette bascule sont
détaillées dans {ref}`micro-it-choix-solveur`.

##### Ré-estimation des pertes

Après convergence, METRIX peut optionnellement ré-estimer les pertes réseau sur la base de la distribution finale
des flux. Si l'écart entre les pertes ré-estimées et les pertes supposées dépasse le seuil `SEUILPER` (500 MW par
défaut), l'ensemble du calcul est relancé avec le taux de pertes mis à jour. Cette boucle est contrôlée par le paramètre `RELPERTE` (désactivée
par défaut). Quand elle est activée, elle s'exécute au maximum le nombre de fois configuré.

(micro-it-implementation)=
### Détails d'implémentation

*Surveillance, détection de contraintes et construction du problème*

Les sections suivantes décrivent le processus de micro-itération au niveau du code, avec des
références aux structures de données internes, aux noms de fonctions et aux détails
algorithmiques. Elles s'adressent aux développeurs travaillant sur le code C++ de metrix-simulator.


#### 1. Vue d'ensemble d'une micro-itération

Chaque micro-itération suit un pipeline séquentiel : résolution du LP, recalcul des phases (θ), détection
des contraintes violées, puis ajout de ces contraintes au problème. La boucle s'arrête quand plus aucune
contrainte n'est détectée, ou quand un des critères d'arrêt est atteint.

::::{container} diagram-card
```{raw} html
:file: img/micro_it_vue_ensemble.svg
```

:::{container} legend
Le solveur n'est pas appelé à l'itération 1 ni quand le problème ne contient aucune contrainte (on part de
l'empilement économique). Le choix Simplexe/PNE dépend de la présence de variables entières (parades,
émulation AC, comptage curatif). La boucle s'arrête lorsque aucune contrainte n'a été *écrite* dans le
problème au cours de l'itération — ce qui n'est pas strictement équivalent à « aucune contrainte détectée » :
des contraintes peuvent avoir été détectées puis toutes ignorées, par équivalence entre parades ou par absence
de levier d'action.
:::
::::

---

#### 2. Détection de contraintes — logique détaillée

La détection se fait en deux phases : d'abord en situation N (réseau intact), puis en N-k pour
chaque incident. Quatre filtres successifs éliminent les contraintes redondantes ou insolubles avant de les retenir.

::::{container} diagram-card
```{raw} html
:file: img/micro_it_detection.svg
```

:::{container} legend
**Sémantique des « skip » :** un skip signifie « ne pas écrire cette contrainte dans le LP » et passer à
l'élément suivant — la variante n'est pas marquée insoluble. Quand un filtre pose `ecarts_[icdt] = -1`
(filtres « action possible » et N-k filtre 2), le marqueur est permanent : la contrainte est définitivement
ignorée et la surcharge éventuelle apparaîtra dans les sorties comme non résolue.

Les filtres marqués *(WITHOUT_REDISPATCH / WITH_OVERLOAD)* ne s'exécutent que dans ces deux modes (variables
d'écart). Le check ITAM bloque la re-détection sur les incidents dont les parades sont déjà choisies, sauf si
le seuil avant-curatif (ITAM) doit être vérifié. Après la boucle interne, par incident : si l'incident rompt
la connexité (poche récupérable via une parade), qu'aucune contrainte n'a été trouvée, et que les parades ne
sont pas déjà activées, une contrainte fictive est ajoutée pour forcer l'entrée de l'incident dans le LP.

Le tri final n'est pas cosmétique : c'est lui qui établit l'ordre de priorité exploité ensuite par
l'arbitrage de `ajoutContraintes`. Le type d'une contrainte dépend de la situation évaluée, N ou N-k, et de la
présence d'une HVDC en émulation AC dans l'élément surveillé — cette dernière étant une propriété de l'élément,
un même élément ne mélange jamais les deux familles. En pratique le tri revient donc, sur un élément donné, à
placer sa contrainte N avant ses contraintes N-k, et à ordonner celles-ci par écart décroissant. Ce classement
est ce qui permet ensuite à la contrainte N de primer, comme le fait déjà en amont le filtre « masquée par
N ».

Enfin, les deux boucles de détection sont bornées par la capacité du tableau `icdtQdt_` : au-delà, la détection
s'arrête pour cette micro-itération et reprendra à la suivante.
:::
::::

---

#### 3. Ajout des contraintes au problème — logique détaillée

Une fois les contraintes détectées, elles sont traduites en lignes de la matrice du problème
d'optimisation. C'est ici que les parades sont activées, que les variables entières et curatives
sont créées, et que les coupes de transit sont écrites.

::::{container} diagram-card
```{raw} html
:file: img/micro_it_ajout_contraintes.svg
```

:::{container} legend
Les 4 cas correspondent à la chaîne if/else if/else if/else du code. Case A: première contrainte d'un incident
avec parades — crée les variables entières, curatives, les contraintes d'activation, et — si `TESTITAM` est
activé et que l'écart pré-curatif dépasse le seuil de détection — une contrainte ITAM dédiée sur
`seuilMaxAvantCur_`. Cas B : la contrainte a été détectée sur une parade (activée dans une micro-itération
précédente) — crée les contraintes par parade sans contrainte d'activation. Cas B' : autre contrainte sur un
incident dont les parades ont été activées plus tôt dans cette même micro-itération — crée les contraintes par
parade avec contrainte d'activation. Cas C : contrainte en N ou N-k simple, ajout direct.

Le cas A suit le parcours des parades de l'incident. Une parade qui déconnecte tous les ouvrages de l'élément
surveillé ne peut pas être en contrainte sur cet élément : elle reçoit sa variable et sa contrainte
d'activation, mais aucune coupe de transit. Les parades restantes passent par l'heuristique de pré-filtrage,
qui écarte celles dont le transit après manœuvre resterait proche du transit avant. Deux contraintes
optionnelles s'ajoutent au passage : une limitation du nombre d'actions curatives lorsque la parade en offre
plus que le maximum configuré, et une valorisation de la poche perdue lorsque la parade en isole une contenant
consommation ou production. Après la boucle viennent la contrainte de choix topologique — qui rend vraie
l'affirmation « exactement une parade est activée » de la partie fonctionnelle — puis, sous ITAM, la contrainte
sur le seuil avant curatif.

Deux précisions de localisation, utiles pour naviguer dans le code : la variable entière d'activation d'une
parade et le test d'équivalence entre contraintes de parades sont créés dans `ecrireCoupeTransit`, et les
variables curatives de l'incident dans `coeffPourQuadInc` — donc en aval de `ajoutContraintes`, pas dans le
corps du cas A.
:::
::::

---

#### Compléments d'implémentation

(micro-it-choix-solveur)=
##### Choix du solveur

Le solveur démarre en mode simplexe. Il bascule en PNE (Branch & Bound) dès qu'une variable entière
est créée. Cela se produit lors de l'activation de parades topologiques (variables d'activation
binaires), lors de la modélisation des HVDC en émulation AC (TD fictifs — variables fixées initialement
puis déverrouillées si une contrainte les concerne), et lors de la création de variables de comptage des
actions curatives si `NBMAXCUR` > 0 (=nb max d’actions curatives par incident). À la micro-itération 1, ou
quand le problème ne contient encore aucune contrainte, le solveur n'est pas appelé : on utilise
directement le résultat de l'empilement économique (phase hors-réseau).

##### Arbitrage des contraintes à ajouter (`choixContraintesAajouter`)

Cette heuristique ne s'exécute que si le nombre de contraintes détectées dépasse
`nb_max_contraints_by_iteration`, en modes `OPF` ou `OPF_WITH_OVERLOAD`, et durant les micro-itérations 1 à 5.
Lorsqu'elle s'exécute, le nombre de contraintes effectivement écrites dans le problème pour cette micro-itération
est en outre plafonné à `nb_max_contraints_by_iteration` ; sinon, toutes les contraintes détectées sont écrites.

Deux filtrages distincts sont combinés. Les contraintes d'un même élément surveillé sont parcourues dans l'ordre du
tri, et une contrainte de rang inférieur est éclipsée (`ecrireContrainte_ = false`) si l'une des deux conditions
suivantes est vraie.

**(a) Priorité de type.** Elle n'est pas du même type que la contrainte prioritaire. Le tri plaçant les contraintes
N avant les N-k, cela revient, sur un élément en contrainte à la fois en N et sur incident, à ne conserver que la
contrainte N : on parie que corriger la situation N suffira à résorber la surcharge post-incident. Le pari est
fondé, car le transit post-incident se déduit du transit en N amplifié par le report — agir sur le transit en N
agit donc sur le transit N-k avec un levier plus important. Il reste un pari : si la contrainte N-k subsiste, elle
sera simplement redétectée à la micro-itération suivante, `ecrireContrainte_` étant remis à `true` à chaque
détection.

**(b) Quasi-redondance.** Les deux contraintes sont du même type, l'incident n'est pas la situation N, il n'a pas
d'éléments curatifs, et les transits sont à moins de 1 % d'écart avec des seuils distants de moins de 1 MW. Cela
couvre le cas d'un même ouvrage chargé de façon quasi identique par plusieurs incidents.

Cette heuristique prolonge le filtre « masquée par N » de la détection, qui traite le cas certain — une surcharge
N-k inférieure à la surcharge N sur le même élément. Ici le pari est étendu au cas incertain, mais uniquement sous
pression de volume. Le résultat final n'en dépend pas, puisque la boucle ne s'arrête que lorsque plus aucune
contrainte n'est détectée ; seul le nombre de micro-itérations nécessaires peut varier.

##### Contrainte fictive de poche

Quand un incident rompt la connexité et que la poche perdue est récupérable via une parade
(`pocheRecuperableEncuratif_`), qu'aucune contrainte de transit n'a été détectée sur aucun
élément surveillé pour cet incident, et que les parades n'ont pas déjà été activées dans une
micro-itération précédente, une contrainte fictive est ajoutée pour forcer l'entrée de l'incident
dans le problème. Sans cela, le mécanisme de parade (Cas A) ne se déclencherait jamais et le
solveur ne saurait pas qu'il peut « récupérer » la poche.

##### Variables d'écart

En modes `OPF_WITHOUT_REDISPATCH` et `OPF_WITH_OVERLOAD`, chaque contrainte de transit peut recevoir une
variable d'écart pénalisée dans la fonction objectif. Cela permet au solveur de tolérer des surcharges
(moyennant un coût élevé) plutôt que de déclarer « pas de solution ». Les contraintes d'activation de parade et
les contraintes d'émulation AC en sont exclues.

C'est aussi ce qui explique l'asymétrie de traitement des contraintes sans levier d'action. Lorsque la coupe de
transit ne contient aucun coefficient non nul — autrement dit lorsque aucune variable du problème n'influence le
transit concerné — les deux modes à variables d'écart marquent la contrainte comme définitivement ignorée et
poursuivent le calcul, tandis que le mode `OPF` retourne « pas de solution ». Un utilisateur qui constate un échec
de convergence en `OPF` alors que le même cas passe en `OPF_WITH_OVERLOAD` se heurte le plus souvent à cette
différence.
