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

Le mode de lancement est paramétrable via l'option **MODECALC** du fichier *json* d'enrtrée.
| MODECALC | I | 1 | 0 OPF, 1 Load Flow seulement, 2 OPF sans redispatching (avec variables d’écart) et 3 OPF_WITH_OVERLOAD.<br>= computationType (0) |

## Load Flow (LF)
Mode basique de simulation réseau avec des productions et consommations fixes. Metrix retourne alors le flot sur les lignes du réseau.ow on the lines of the network.
Pour ce mode : **MODECALC = 1**.

## Optimal Power Load Flow sans redispatching (OPF w/o redispatching)
Ce mode ne permet pas à Metrix-simulator de faire de redispatching (modifier les injections et consommations) mais il tend à minimiser les violations des différentes contraintes grâce à l'utilisation de parades topologiques, de modifications des déphasages des Transformateurs-Déphaseurs ou encore de modifications des flux sur les Lignes à Courant Continu.
Pour ce mode : **MODECALC = 2**.

## Optimal Power Load Flow (OPF)
Ce mode a la possibilité toures les actions possibles (redispatching en plus des actions énoncées dans le chapitre précédent) afin de minimiser, à moindre coût, les violations des contriantes.
Pour ce mode : **MODECALC = 0**.

## Optimal Power Load Flow avec surcharge (OPF with overload)
Ce mode fonctionne comme le précédent, toutefois si aucune solution n'est trouvée le programme ne retourne pas d'erreur mais des résultats avec surcharge.
Pour ce mode : **MODECALC = 3**.

# Générations fichiers PTDFs

# Codes de retour et gestion des crashs

# Parade topologique en N