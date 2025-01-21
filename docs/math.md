<style>
r { color: Red }
o { color: Orange }
g { color: Green }
y { color: yellow}
</style>

# Variables

## Production <a id="prod_var"></a>
Les groupes du réseau modélisé dans METRIX sont stockés dans une liste $GROUPE$.
La production d’un groupe peut être modifiée en *Adequacy Phase*, en *Redispatching Phase*, les deux ou aucune. Nous ne considérerons dans les équations, que les groupes modifiables en toute circonstance.

### Définition des variables
Soit $i$ un groupe de $GROUPE$.

Sa production se définit par une valeur initiale $P_i^0$ et des valeurs minimum et maximum $P_{i}^{min}$ et $P_{i}^{max}$. Il existe deux valeurs pour la puissance minimum : $P_{i_{ad}}^{min}$ et $P_{i_{red}}^{min}$, représentant les minimums pour l'*Adequacy* et le *Redispatching* tels que :
$$
P_{i_{ad}}^{min} = min(0; P_{i_{red}}^{min})
$$

La variation de puissance du groupe $i$ est décrite dans les problèmes d'*Adequacy* et de *Redispatching* en N par deux variables : $P_i^+$ et $P_i^-$, **toutes deux positives**, et représentant respectivement la hausse et la baisse de la production du groupe $i$. 

# Formulations mathématiques des deux problèmes

Notons $W$ le nombre entier de variantes à traiter et $w$ l’indice de la variante courante.

## *Adequacy phase* <a id="adeq_math"></a>
Notons $P_w$ ma matrice colonne des puissances produites par les groupes et $C_w$ a matrice colonne des puissances consommées par les zones de consommation. Notons également $P_{w}^{0}$ et $C_{w}^{0}$ les matrices des valeurs initiales.

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
<a id="Pbounds_eq"></a>
$$
\begin{equation}
P_{w}^{min} \leq P_{w} = P_{w}^{0} + \Delta P_{w} \leq P_{w}^{max}
\end{equation} 
$$
<a id="Cbounds_eq"></a>
$$
\begin{equation}
C_{w}^{min} \leq C_{w} = C_{w}^{0} + \Delta C_{w} \leq C_{w}^{max}
\end{equation}
$$
<a id="adeq_eq"></a>
$$
\begin{equation}
P_{w} = C_{w}
\end{equation}
$$

Les contraintes [(1)](#Pbounds_eq) et [(2)](#Cbounds_eq), définissent les limites des matrices $P_w$ et $C_w$, ainsi que leur lien via la fonction objectif. La contrainte [(3)](#adeq_eq) consitue la contrainte de base du réseau : à tout instant, la production et la consommation doivent être égales.

## *Redispatching phase* <a id="redis_math"></a>
Nommons $U_w$ la matrice colonne des actions préventives et $V_w$ la matrice colonne des actions curatives pour la variante $w$. Ces deux matrices contiennent les variables représentant les changements de production des groupes, de consommation des zones de consommation, de déphasage des Transfo-Déphaseurs (TDs) du réseau et de flux des Lignes à Courant Continu (LCCs). $V_w$ contiendra également les variables booléennes d’activation des parades topologiques. 
En notant $p_i$, $c_i$, $td_i$, $lcc_i$, $prd_i$ les valeurs de production, consommation, de déphasage des TDs, de flux sur les LCCs et d’activation des parades, et en notant $n_1$, $n_2$, $n_3$, $n_4$, $n_5$ leurs cardinalités, on peut formuler $U_w$ et $V_w$ de la manière suivante : 
$$
U_w=(p_1, …, p_{n_1}, c_1, …,c_{n_2}, td_1, …, td_{n_3}, lcc_1, …, lcc_{n_4}, 0, …, 0)^t \\
V_w=(p_1, …, p_{n_1}, c_1, …,c_{n_2}, td_1, …, td_{n_3}, lcc_1, …, lcc_{n_4}, prd_1, …, prd_{n_5})^t
$$

$U_w$ et $V_w$ sont donc toutes deux de tailles $n_1 + n_2 + n_3 + n_4 + n_5$. De même, notons $U_{w}^{1}$ et $V_{w}^{1}$ les matrices de leurs valeurs initiales pour ce problème. Nous noterons que, dans le cas de $U_{w}^{1}$, les paramètres $p_1, ..., p_{n_1}, c_1, ..., c_{n_2}$ sont des $argmin$ du problème résolu en *Adequacy phase*.

Notons également, $F_w$ la matrice des flux des lignes et $M_w$ la matrice de répartition, qui à partir des actions préventives et curatives, permet de calculer le flux sur les lignes en N et sur les différents incidents. En notant $m$ le nombre de lignes et $k$ le nombre d'incidents, $F_w$ est une matrice colonne de taille $m \cdot k$ et $M_w$ une matrice rectangulaire de taille $(m \cdot k) \cdot 2(n_1 + n_2 + n_3 + n_4 + n_5)$.

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
<a id="Ubounds_eq"></a>
$$
\begin{equation}
U_{w}^{min} \leq U_{w} = U_{w}^{1} + \Delta U_{w} \leq U_{w}^{max}
\end{equation} 
$$
<a id="Vbounds_eq"></a>
$$
\begin{equation}
V_{w}^{min} \leq V_{w} = V_{w}^{1} + \Delta V_{w} \leq V_{w}^{max}
\end{equation}
$$
<a id="UeqV_eq"></a>
$$
\begin{equation}
V_{w}^{1} = U_{w}
\end{equation}
$$
<a id="PCfUV_eq"></a>
$$
\begin{equation}
(P_{w}, C_{w}) = f(U_{w}, V_{w})
\end{equation}
$$
<a id="PC_eq"></a>
$$
\begin{equation}
P_{w} = C_{w}
\end{equation}
$$
<a id="FM_eq"></a>
$$
\begin{equation}
F_{w}^{min} \leq F_{w} = M_{w} \cdot  \begin{align*} U_{w}\\ V_{w}\end{align*} \leq F_{w}^{max}
\end{equation}
$$

Les équations [(4)](#Ubounds_eq) et [(5)](#Vbounds_eq) définissent les encadrements des atrices $U_{w}$ et $V_{w}$, ainsi que leur lien avec la fonction objectif. Autrement dit, elles définissent les limites des différentes productions, consommations, des TD, des HVDC, etc., en préventif et curatif. L’équation [(6))](#UeqV_eq) définit le fait que l’état initial des actions curatives correspond à l’état du réseau en N, i.e. ce qui a été déterminé avec les actions préventives. L'équation [(7)](#PCfUV_eq) indique que l'état de la production et de la consommation en N et en incident est stockée dans $U_w$ et $V_w$. L’équation [(8)](#PC_eq) rappelle le nécessaire équilibre production – consommation en N et en incidents. Enfin, l’équation [(9)](#FM_eq) définit et encadre le flux des lignes.

