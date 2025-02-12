<style>
r { color: Red }
o { color: Orange }
g { color: Green }
y { color: yellow}
</style>

# Mode de lancements

Il existe 4 modes de lancement :
- Load Flow (LF)
- Optimal Power Load Flow sans redispatching (OPF w/o redispatching)
- Optimal Power Load Flow (OPF)
- Optimal Power Load Flow avec surcharge (OPF with overload)

Pour tous ces modes, les premières étapes sont :
- construction de la jacobienne et factorisation LU de cette dernière
- pour chacune des variantes :
 - mise à jour de la topologie réseau
 - mise à jour de la jacobienne
 - refactorisation de la jacobienne
 - calculs LODFs et PTDFs
 

Les différents types de contraintes sont :
- CONTRAINTE_EMUL_AC_N = 1 (contrainte en N sur un quadripôle fictif)
- CONTRAINTE_EMULATION_AC = 2 (contrainte en N-k sur un quadripôle fictif)
- CONTRAINTE_N = 3 (contrainte en N sur un quadripôle non-fictif)
- CONTRAINTE_N_MOINS_K = 4 (contrainte en N-k sur un quadripôle non-fictif)
- CONTRAINTE_PARADE = 5 (contrainte sur une parade)
- CONTRAINTE_ACTIVATION = 6 (contrainte d'activation d'une parade)
- CONTRAINTE_NON_DEF = -1

## Load Flow (LF)
Mode basique de simulation réseau avec des productions et consommations fixes. Metrix retourne alors le flot sur les lignes du réseau.
Pour ce mode : **MODECALC = 1**.
Dans ce mode :
 - Les parades ne sont pas lues
 - Si des groupes ou consommateurs sont renseignés comme étant disponibles pour le curatif alors ils sont ignorés
 - Il n'y a pas de phase de détection de contraintes

## Optimal Power Load Flow sans redispatching (OPF w/o redispatching)
Ce mode ne permet pas à Metrix-simulator de faire du redispatching (modifier les injections et consommations) mais il tend à minimiser les violations des différentes contraintes grâce à l'utilisation de parades topologiques, de modifications des déphasages des Transformateurs-Déphaseurs ou encore de modifications des flux sur les Lignes à Courant Continu.
Pour ce mode : **MODECALC = 2**.
Dans ce mode :
- Si un groupe est indiqué comme disponible pour le redispatching :
  - et uniquement pour le redispatching (i.e. 'OUI_AR') alors il est considéré comme n'étant pas disponible ni pour le redispatching ni pour l'adequacy (i.e. 'NON_HR_AR')
  - mais aussi pour l'adequacy (i.e. 'OUI_HR_AR') alors il est considéré comme étant uniquement disponible pour l'adequacy (i.e. 'OUI_HR')
- Si des groupes sont renseignés comme étant disponibles pour le curatif alors ils sont ignorés
- Les défaillances sont interdites ici (i.e. les variables de variations de consommation sont fixes et )
- Des variables d'écart sont ajoutées pour toute contrainte avec un type autre que 'CONTRAINTE_EMUL_AC_N', 'CONTRAINTE_EMULATION_AC' ou 'CONTRAINTE_ACTIVATION'.

## Optimal Power Load Flow (OPF)
Ce mode a la possibilité toures les actions possibles (redispatching en plus des actions énoncées dans le chapitre précédent) afin de minimiser, à moindre coût, les violations des contriantes.
Pour ce mode : **MODECALC = 0**.

## Optimal Power Load Flow avec surcharge (OPF with overload)
Ce mode fonctionne comme le précédent, toutefois si aucune solution n'est trouvée le programme ne retourne pas d'erreur mais des résultats avec surcharge.
Pour ce mode : **MODECALC = 3**.

Si des consommateurs sont renseignés comme étant disponibles pour le curatif alors ils sont ignorés.
Il n'y a pas de contraintes de couplage ici.
Des variables d'écart sont ajoutées pour toute contrainte avec un type autre que 'CONTRAINTE_EMUL_AC_N', 'CONTRAINTE_EMULATION_AC' ou 'CONTRAINTE_ACTIVATION'.

# Générations fichiers PTDFs

La génération des fichiers se fait via l'appel au simulateur. Il n'est pour l'instant pas possible d'uniquement générer les fichiers PTDFs pour chacune des variantes sans lancer de résolution.
Toutefois, il n'y a, a priori, pas de raison à ce que cela ne soit pas possible.

# Codes de retour et gestion des crashs

Les diférents status de retour sont les suivants :
- METRIX_PROBLEME (= -1) : KO
- METRIX_PAS_PROBLEME (= 0) : OK
- METRIX_PAS_SOLUTION (= 1) : problème infaisable ou pas de solution trouvée
- METRIX_NB_MAX_CONT_ATTEINT (= 2) : nombre maximum de contraintes ajoutées atteint
- METRIX_NB_MICROIT (= 3) : nombre maximum de micro-iterations atteint
- METRIX_VARIANTE_IGNOREE (= 4) : variante ignorée
- METRIX_CONTRAINTE_IGNOREE (= 3) : contrainte ignorée car équivalente, Tmax non défini ou pas d'action possible (dans le cas d'un OPF sans redispatching ou d'un OPF avec surcharge)

# Parade topologique en N

Il est possible de ???????

# Seuils ITAM

Il est possible de paramétrer l'outil afin d'utiliser les seuils avant manoeuvre (i.e. post incident mais avant curatif) via le paramètre *TESTITAM*.
Si ces seuils sont activés, une parade "ne rien faire" est ajoutée sur les incidents avec curatif mais sans parade.

## Contexte LF+
En LF+, lorsqu’il y a dépassement d’un seuil ITAM avant utilisation des parades topologiques curatives, il n’y a pas d’alerte ou d‘indicateur indiquant ce dépassement. Alors que la solution retournée peut préconiser l’utilisation de parades curatives qui favorisent, potentiellement, le respect des seuils ITAM d’après les logs finaux.
Ce comportement n’est pas réaliste puisqu’en situation réelle, les parades curatives n’ont pas le temps d’être activée afin de respecter ces seuils ITAM.

## Problématique
Metrix résolve le problème via de l’optimisation globale. Autrement dit, ce dernier tente de trouver une solution optimale respectant l’ensemble des contraintes tout en minimisant l’ensemble des objectifs définis (ex : minimisation des dépassements en LF+). Ceci implique que, du point de vue de Metrix, la minimisation des dépassement se fait « d’un coup » et non pas séquentiellement comme ce pourrait être le cas manuellement. Ainsi, en sortie de Metrix, ce dernier peut considérer qu’il n’y a plus de dépassement alors que ce sont des parades curatives qui permettent de respecter les seuils ITAM (non réaliste d’après le paragraphe précédent). 
 

## Options possibles :
 
 - Utiliser les fichiers « tmp » afin de lever une alerte
 - Modifier le modèle afin que seules les parades topologiques curatives n’ayant pas d’impact sur les éléments avec dépassements de leur seuil ITAM soient activables  cette solution semble complexe à implémenter a priori
 - Pour tout élément avec un seuil N-k ainsi qu’un seuil ITAM mais pas de parade préventive pour cet élément : suppression de seuils N-k de cet élément ?
 
Compte tenu du paradigme LF+ (minimisation des dépassements de différents seuils pour un même élément et optimisation globale), le process le plus réaliste semble être de lever une alerte/warning indiquant qu’un seuil n’est pas respecté.
