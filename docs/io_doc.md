# Sommaire
1. [Introduction](#introduction)
2. [Données d’entrée au format *json*](#input)
    1. [Options de calcul](#computation_options)
    2. [Options de résultats](#results_options)
    3. [Régions](#regions)
    4. [Informations nodales](#nodal_info)
        1. [Sommets](#summits)
        1. [Consommations](#conso)
    5. [Groupes](#groups)
    6. [Quadripôles](#quadri)
        1. [Quadripôles élémentaires](#quadri_elem)
        2. [Déphaseurs](#phase_shifters)
    7. [Lignes à courant continu](#dc_lines)
    8. [Incidents N-1 et N-k](#incidents)
    9. [Curatif](#curative)
    10. [Sections surveillées](#monitored_section)
    11. [Variables couplées](#coupled_vars)
    12. [Variations marginales détaillées](#detailed_marginal_variations)
3. [Variantes](#variants)
4. [Ligne de commande pour lancer METRIX](#command_line)
5. [Données de sorties](#output)
    1. [Tableaux descriptifs de la situation](#detailed_tables)
    2. [Tableaux de résultats](#results_tables)

# Introduction <a id="introduction"></a>
Ce document décrit les entrées et les sorties du modèle METRIX v6 (utilisé dans la plateforme imaGrid). Suite à l’intégration dans imaGrid, certains formats utilisés initialement dans la plate-forme ASSESS ont été conservés ; à savoir CSV pour les variantes, parades et pour les tableaux des sorties. Les autres données d'entrée ont, quant-à-elle, été mises sous forme *json*, au sein desquelles certaines ont été ajoutées et d’autres rendues optionnelles. Le format de données de METRIX v6 n’est donc pas compatible avec les versions précédentes du modèle.

# Données d’entrée au format *json* <a id="input"></a>
La passerelle imaGrid pour METRIX prend en entrée un fichier réseau au format IIDM et un script de configuration au format Groovy. Elle génère 1 fichier *json* : '*fort.json*'.

Les données indiquées en **gras** dans les tableaux suivants doivent toujours être présentes dans les fichiers d’entrée, les autres sont optionnelles.
La valeur par défaut configurée dans METRIX est indiquée entre parenthèses. 

N.B. : 
- B = *BOOLEAN*
- C = *STRING*
- I = *INTEGER*
- R = *FLOAT*


## Options de calcul <a id="computation_options"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **CGCPERTE** | R | 1 | Coefficient de perte initial.<br>= lossFactor (0) |
| **UNOMINAL** | I | 1 | Tension nominale utilisée dans le per-unitage côté imaGrid.<br>= nominalU (0) |
| MODECALC | I | 1 | 0 OPF, 1 Load Flow seulement, 2 OPF sans redispatching (avec variables d’écart) et 3 OPF_WITH_OVERLOAD.<br>= computationType (0) |
| NBMAXMIT | I | 1 | Nombre maximum de micro-itérations par variante.<br>= nbMaxIteration (30) |
| NBMAXCUR | I | 1 | Nombre maximum d’actions curatives par incident. Pas de limitation si la valeur est négative ou nulle.<br>= nbMaxCurativeAction (0) |
| COUTECAR | I | 1 | Coût des variables d’écart<br>= gapVariableCost (10) |
| RELPERTE | I | 1 | Nombre maximum de relance(s) pour écart de pertes (valeur par défaut : 0 = pas de relance)<br>= lossNbRelaunch (0) |
| SEUILPER | I | 1 | Seuil d’écart entre les pertes théoriques et les pertes calculées nécessitant une relance (MW)<br>= lossThreshold (500) |
| COUTDEFA | R | 1 | Coût par défaut de la défaillance<br>= lossOfLoadCost (13000) |
| MAXSOLVE | I | 1 | Temps maximum autorisé pour chaque résolution de solveur. 0 : pas de limite.<br>= maxSolverTime (0) |
| TDPENALI | R | 1 | Coût de déphasage des TD dans la fonction objectif. Une valeur négative ou nulle implique qu’il n’y a pas de coût associé (déphasage non pénalisé).<br>= pstCostPenality (10-2) |
| HVDCPENA | R | 1 | Coût de transit des HVDC dans la fonction objectif. Une valeur négative ou nulle implique qu’il n’y a pas de coût associé (transit non pénalisé).<br>= hvdcCostPenality (10-1) |
| PROBAINC | R | 1 | Probabilité des incidents<br>= contingenciesProbability(10-3) |
| TESTITAM | B | 1 | Prise en compte de l’instant post-incident<br>= preCurativeThresholds (false) |
| INCNOCON | B | 1 | Prise en compte des incidents rompant la connexité<br>= outagesBreakingConnexity (false) |
| PARNOCON | B | 1 | Prise en compte des parades rompant la connexité<br>= remedialActionsBreakingConnexity (false) |
| PAREQUIV | B | 1 | Détection des parades équivalentes<br>= analogousRemedialActionDetection(false) |
| COUENDCU | R | 1 | Coût de l’énergie non distribuée pour un incident rompant la connexité (consommation déconnectée du réseau)<br>= curativeLossOfLoadCost(26000) |
| COUENECU | R | 1 | Coût de l’énergie non évacuée sur incident rompant la connexité (production déconnectée du réseau)<br>= curativeLossOfGenerationCost(100) |
| LIMCURGR | I | 1 | Limite de redispatching curatif<br>= curativeRedispatchingLimit(-1) |
| ADEQUAOF | I | 1 | Offset des coûts de groupes et de délestage dans la phase d’équilibrage<br>= adequacyCostOffset(0) |
| REDISPOF | I | 1 | Offset des coûts de groupes et de délestage dans la phase de redispatching<br>= redispatchingCostOffset(0) |

## Options de résultats <a id="results_options"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| NBTHREAT | I | 1 | Nombre de menaces N-k dans les résultats<br>= nbThreatResults (1) |
| EQUILRES | B | 1 | Sortie des résultats détaillés de l’équilibrage initial<br>= withAdequacyResults (false) |
| REDISRES | B | 1 | Sortie des résultats détaillés du redispatching préventif<br>= withRedispatchingResults (false) |
| VARMARES | B | 1 | Calcul de variations marginales sur les branches<br>= marginalVariationsOnBranches (false) |
| LCCVMRES | B | 1 | Calcul des variations marginales sur les HVDC<br>= marginalVariationsOnHvdc (false) |
| LOSSDETA | B | 1 | Calcul des pertes par régions/pays<br>= lossDetailPerCountry (false) |
| OVRLDRES | B | 1 | Sortie uniquement des ouvrages en contrainte<br>= overloadResultsOnly (false) |

## Régions <a id="regions"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **CGNBREGI** | I | 1 | Nombre total de régions<br>= $\sum$ country |
| CGNOMREG | C | CGNBREGI | Pour région, nom de la région<br>= « paysCvg » si existe « country » sinon |

## Informations nodales <a id="nodal_info"></a>
### Sommets <a id="summits"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **TNNBNTOT** | I | 1 | Nombre total de sommets<br>= $\sum$ bus t.q. voltageLevel.busBreakerView.bus $\in$ composante connexe principale |

### Consommations <a id="conso"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **ECNBCONS** | I | 1 | Nombre de consommations élémentaires<br>= $\sum$ load t.q. load.terminal.busBreakerView.bus $\in$ composante connexe principale |
| **TNNOMNOE** | C | ECNBCONS | Noms des consommations<br>= load.id |
| **TNNEUCEL** <a id="table_tnneucel"></a>| I | ECNBCONS | Numéro du sommet de raccordement de la consommation élémentaire<br>= indice du bus dans la table TNNOMNOE |
| **CPPOSREG** | I | ECNBCONS | Lien sommet-région<br>= indice de la région dans CGNOMREG |
| **ESAFIACT** | I | ECNBCONS | Valeur de la consommation active (somme de la part fixe et affine)<br>= load.p0 |
| TNVAPAL1 | I | ECNBCONS | Pourcentage du premier palier de délestage.<br>= 100 si aucun nœud configuré, 0 sinon |
| TNVACOU1 | R | ECNBCONS | Coût du délestage (13000 par défaut) |

## Groupes <a id="groups"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **TRNBGROU** | I | 1 | Nombre de groupes raccordés<br>= $\sum$ generator t.q. generator.terminal.busBreakerView.bus $\in$  composante connexe principale |
| **TRNOMGTH** <a id="table_trnomgth"></a>| C | TRNBGROU | Nom du groupe<br>= generator.id |
| **TNNEURGT** | I | TRNBGROU | Sommets de raccordement du groupe<br>= indice du bus dans la table TNNOMNOE |
| **SPPACTGT** | R | TRNBGROU | Puissance de consigne $P_{obj} \in [\underline{P},\overline{P}]$<br>= generator.targetP |
| **TRVALPMD** | R | TRNBGROU | Puissance max disponible<br>= generator.maxP |
| **TRPUIMIN** | R | TRNBGROU | Puissance min<br>= generator.minP |
| **TRNBTYPE** | I | 1 | Nombre de types de groupe |
| **TRNOMTYP** | C | TRNBTYPE | Noms des types de groupes |
| **TRTYPGRP** | I | TRNBGROU | Indice du type de groupe dans TRNOMTYP |
| SPIMPMOD | I | TRNBGROU | Indique si le groupe est disponible pour le redispatching (3 = 'OUI_AR'), l’adequacy (2 = 'OUI_HR'), les deux (1 = 'OUI_HR_AR') ou aucune des deux (0 = 'NON_HR_AR')<br>Valeur par défaut = 1 si aucun groupe n’est configuré et 0 sinon.|
| TRDEMBAN | R | TRNBGROU | Demi-bande de réglage en réglage secondaire.<br>= prorata du plus grand incident groupe (valeur par défaut 0) |

## Quadripôles <a id="quadri"></a>
### Quadripôles élémentaires <a id="quadri_elem"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **CQNBQUAD** | I | 1 | Nombre de quadripôles élémentaires<br>= $\sum$ (quad = *line* &#124;&#124; *twoWindingsTransformer*) t.q. quad.terminal1.busBreakerView.bus && quad.terminal2.busBreakerView.bus $\in$ composante connexe principale<br>+ $\in$ *switch* t.q. switch.retained && (switch.voltageLevel.busBreakerView.bus1 && switch.voltageLevel.busBreakerView.bus2 $\in$ composante connexe principale)
| **CQNOMQUA** | C | CQNBQUAD | Nom du quadripôle<br>= quad.id |
| **CQADMITA** | R | CQNBQUAD |	Admittance du quadripôle ramené à Ubase (inverse de la réactance, i.e. négliger la valeur de la résistance)<br>= $1 / (X * nominalU^2 / Unom^2)$<br>Pour les lignes :<br>$X = line.x$<br>Pour les transformateurs :<br>$X_{transfo} = twoWindingsTransformer.x$<br>Pour les transformateurs déphaseurs :<br>$X = X_{transfo} * (1 + X_{prise} / 100)$<br>$X_{prise} = phaseTapChanger.phaseTapPosition.x$<br>Pour les couplages :<br>$X = 10^-5$<br>Unom = line.terminal2.voltageLevel.nominalV
| **CQRESIST** | R | CQNBQUAD | Résistance utilisée pour le calcul des pertes (en pu, base nominalU)<br>Les formules sont les mêmes que pour CQADMITA en remplaçant X par R |
| **QASURVDI** | I | CQNBQUAD | Indicateur de surveillance du quadripôle en N. 2 = résultat de transit uniquement, 1 = surveillé par le modèle, 0 = aucun résultat |
| **QASURNMK** | I | CQNBQUAD | Indicateur de surveillance du quadripôle en N-K. 2 = résultat de transit uniquement, 1 = surveillé par le modèle, 0 = aucun résultat |
| **TNNORQUA** | I | CQNBQUAD | Indice du nœud origine du quadripôle (quad.terminal1.busBreakerView.bus) dans la table TNNOMNOE |
| **TNNEXQUA** | I | CQNBQUAD | Indice du nœud extrémité du quadripôle (quad.terminal2.busBreakerView.bus) dans la table TNNOMNOE |
| NBOPEBRA | I | 1 | Nombre de branches ouvertes conservées car elles peuvent être refermées par une parade (valeur par défaut 0) |
| OPENBRAN | I | NBOPEBRA | Indices des branches ouvertes dans la table CQNOMQUA |

### Déphaseurs <a id="phase_shifters"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **DTNBTRDE** | I | 1 | Nombre total de transformateurs déphaseurs<br>$\sum$ twoWindingsTransformer.phaseTapChanger si twoWindingsTransformer est dans la table CQNOMQUA
| **DTTRDEQU** | I | DTNBTRDE 	Correspondance déphaseur-numéro du quadripôle élémentaire<br>= indice de twt.id dans les tables de quadripôles
| **DTMODREG** | I | DTNBTRDE | Type de contrôle<br>0 : Hors service<br>1 : angle optimisé<br>2 : angle fixe<br>3 : puissance optimisé (non utilisé)<br>4 : puissance fixe (non utilisé)<br>Valeur par défaut = 2 |
| **DTVALINF** | R | DTNBTRDE | Valeur min du déphasage du TD en N, en degrés<br>= phaseTapChanger.lowTapPosition.alpha |
| **DTVALSUP** | R | DTNBTRDE | Valeur max du déphasage du TD en N, en degrés <br>= phaseTapChanger.highTapPosition.alpha |
| **DTVALDEP** | R | DTNBTRDE | Valeur initiale du déphasage par TD en degrés, c’est la valeur utilisée si DTMODREG $\in {1, 2}$<br>= phaseTapChanger.tapPosition.alpha |

## Lignes à courant continu <a id="dc_lines"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **DCNBLIES** | I | 1 | Nombre de lignes à courant continu<br>= $\sum$ hvdcLine t.q.  (hvdcLine.converterStation1.terminal.busBreakerView.bus && hvdcLine.converterStation2.terminal.busBreakerView.bus) $\in$ composante connexe principale |
| **DCNOMQUA** <a id="table_dcnomqua"></a>| C | DCNBLIES | Nom de la ligne à courant continu<br>= hvdcLine.id |
| **DCNORQUA** | I | DCNBLIES | Indice du nœud origine de la ligne (hvdcLine.converterStation1.terminal.busBreakerView.bus) dans la table TNNOMNOE |
| **DCNEXQUA** | I | DCNBLIES | Indice du nœud extrémité de la ligne (hvdcLine.converterStation2.terminal.busBreakerView.bus) dans la table TNNOMNOE |
| **DCMINPUI** | R | DCNBLIES | Puissance minimale<br>= - activePowerRange.oPRFromCS2toCS1 si l’extension HvdcOperatorActivePowerRange est utilisée, - hvdcLine.maxP sinon. |
| **DCMAXPUI** | R | DCNBLIES | Puissance maximale<br>= activePowerRange.oPRFromCS1toCS2 si l’extension HvdcOperatorActivePowerRange est utilisée, hvdcLine.maxP sinon. |
| **DCIMPPUI** | R | DCNBLIES | Puissance imposée $P_0$<br>= activePowerControl.p0 si l’extension  HvdcAngleDroopActivePowerControl est utilisée, +/- hvdcLine.activePowerSetPoint sinon |
| **DCREGPUI** | I | DCNBLIES | Type de réglage de transit sur la ligne<br>1 : puissance fixe (valeur par défaut)<br>2 : puissance optimisée  |
| **DCTENSDC** | R | DCNBLIES | Tension nominale du câble DC<br>= hvdcLine.nominalV |
| **DCRESIST** | R | DCNBLIES | Résistance du câble DC en pu (base nominalV), normalisée par rapport à la tension DC.<br>= hvdcLine.r * nominalU $^2$ / hvdcLine.nominalV $^2$ |
| **DCPERST1** | R | DCNBLIES | Coefficient de pertes (en %) de la station de conversion origine<br>= hvdcLine.converterStation1.lossFactor |
| **DCPERST2** | R | DCNBLIES | Coefficient de pertes (en %) de la station de conversion extrémité<br>= hvdcLine.converterStation2.lossFactor |
| **DCNDROOP** | I | 1 | Nombre de HVDC opérée en mode émulation AC ($P = P_0 + k \Delta\Theta$) |
| DCDROOPK | R | DCNDROOP | Pour chaque HVDC en émulation AC, dans l’ordre de la [table DCNOMQUA](#table_dcnomqua), valeur du coefficient k (en MW/°) |

## Incidents N-1 et N-k <a id="incidents"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **DMNBDEFK** | I | 1 | Nombre d’incidents |
| DMNOMDEK | C | DMNBDEFK | Nom de l’incident |
| DMPTDEFK | I | DMNBDEFK | Par incident, pointeur sur DMDESCRK |
| DMDESCRK | I | DMNBDEFK<br>$+ 2 * \sum nb_{<i>}$ | Description des incidents<br>Pour chaque incident :<br>Nb, $Type_{<1>}$, $Indice_{<1>}$, …, $Type_{<nb>}$, $Indice_{<nb>}$<br>Nb : nombre d’éléments dans l’incident<br>Type :<br>1 pour un quadripôle (ligne, transfo)<br>2 pour un groupe<br>3 pour une liaison à courant continu<br>Indice : dans la liste de quadripôles, dans la liste des groupes ou dans la liste des liaisons à courant continu selon le type |
 BDEFRES | I | 1 Taille du tableau des défauts avec résultats détaillés (valeur par défaut 0) |
 TDEFRES | I | NBDEFRES | Suite de triplets : indice du quadripôle, nombre de défauts avec résultats détaillés, liste des indices de ces défauts. |

## Curatif <a id="curative"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| DTNBDEFK | I | DTNBTRDE 	Pour chaque TD, nombre d’incidents pour lesquels le TD peut agir en curatif<br>= 0 |
| DTPTDEFK | I | ≤ DTNBTRDE  * DMNBDEFK	Pour chaque TD agissant en curatif, dans l’ordre des indices croissants, indices des incidents traités en curatif |
| DCNBDEFK | I | DCNBLIES	Pour chaque TD, nombre d’incidents pour lesquels le TD peut agir en curatif<br>= 0 |
| DCPTDEFK | I | ≤ DCNBLIES * DMNBDEFK	Pour chaque TD agissant en curatif, dans l’ordre des indices croissants, indices des incidents traités en curatif |
| NBLDCURA | I | 1	Nombre de consommations pouvant s’effacer en curatif (valeur par défaut 0) |
| LDNBDEFK | I | ECNBCONS	Pour chaque consommation, nombre d’incidents traités en curatif (valeur par défaut 0) |
| LDCURPER | I | NBLDCURA	Pour chaque consommation curative, dans l’ordre des indices, pourcentage d’effacement en curatif |
| LDPTDEFK | I | ≤ NBLDCURA * DMNBDEFK	Pour chaque consommation curative, dans l’ordre de la [table TNNEUCEL](#table_tnneucel), pointeur des incidents traités en curatif. |
| GRNBCURA | I | 1	Nombre de groupes pouvant agir en curatif (valeur par défaut = 0) |
| GRNBDEFK | I | GRNBCURA	Pour chaque groupe curatif, nombre d’incidents traités en curatif (valeur par défaut 0) |
| GRPTDEFK | I | ≤ GRNBCURA * DMNBDEFK	Pour chaque groupe curatif, dans l’ordre de la [table TRNOMGTH](#table_trnomgth), pointeur des incidents traités en curatif. |

## Sections surveillées <a id="monitored_section"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **SECTNBSE** | I | 1 | Nombre de sections surveillées |
| SECTNOMS | C | SECTNBSE | Nom de la section |
| SECTMAXN | R | SECTNBSE | Seuil N de la section |
| SECTNBQD | I | SECTNBSE | Nombre de quadripôles de la section (nb1, nb2, …) |
| SECTTYPE | I | $\sum nb_{<i>}$ | Table des types de quadripôles (liste des nb1 types de la section 1, liste des nb2 types de la section 2, …) |
| SECTNUMQ | I | $\sum nb_{<i>}$ | Table des numéros dans la liste de quadripôles ou dans la liste des liaisons à courant continu (liste des nb1 numéros de la section 1, liste des nb2 numéros de la section 2, …) |
| SECTCOEF | R | $\sum nb_{<i>}$ | Table des coefficients associés aux quadripôles (liste des nb1 coefficients de la section 1, liste des nb2 coefficients de la section 2, …)|

## Variables couplées <a id="coupled_vars"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| NBGBINDS | I | 1	Nombre de couplages de groupes |
| NBLBINDS | I | 1	Nombre de couplages de consommations |
| GBINDDEF | I | ≤ NBGBINDS + TRNBGROU	Pour chaque couplage, nombre de groupes dans le couplage puis indices des groupes couplés |
| GBINDNOM | C | NBGBINDS	Noms des couplages de groupes |
| GBINDREF | I | NBGBINDS	Pour chaque couplage, type de la variable de référence (0 = PMAX, 1 = PMIN, 2 = POBJ, 3 = PMAX-POBJ) |
| LBINDDEF | I | ≤ NBLBINDS + ECNBCONS	Pour chaque couplage, nombre de consommations dans le couplage puis indices des consommations couplées |
| LBINDNOM | C | NBLBINDS	Noms des couplages de consommations |

## Variations marginales détaillées <a id="detailed_marginal_variations"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| NBVARMAR | I | 1 | Taille du tableau des variations marginales détaillées (PTVARMAR) |
| PTVARMAR | I | NBVARMAR | Suite de triplets : indice du quadripôle, nombre de défauts avec variations marginales détaillées, liste des indices de ces défauts. |

# Variantes <a id="variants"></a>
Les données spécifiques à chaque variante sont décrites dans un fichier au format csv.
La première ligne indique le nombre de tirages (i.e. de variantes), après le mot-clé NT. 
Chaque ligne du fichier commence ensuite par le numéro du tirage suivi par le type de la loi décrite identifiée par un mot-clé.
Les données propres à la loi et au tirage sont écrites à la suite du mot-clé, à savoir :
- >**PRODIN** : le nombre de groupes, puis les noms des groupes indisponibles.
- >**PRODIM** : le nombre de groupes, puis autant de couples (indice de groupe, puissance de consigne) que le nombre de groupes indiqué.
- >**QUADIN** : le nombre de lignes indisponibles (consignations) puis les noms des ouvrages indisponibles 
- >**TRVALPMD** : le nombre de groupes, puis autant de couples (indice de groupe, puissance maximale disponible) que le nombre de groupes indiqué.
- >**TRPUIMIN** : le nombre de groupes, puis autant de couples (indice de groupe, puissance minimale disponible) que le nombre de groupes indiqué.
- >**CONELE** : le nombre de consommations, puis autant de couples (nom de la consommation, nouvelle valeur de consommation) que le nombre de consommations indiqué. Il s’agit de consommations nodales. Convention Eurostag : 1 nœud = 1 conso
- >**ECHANG** : loi qui devrait s’appeler plutôt bilan par les consommations. le nombre de régions, puis autant de couples (indice de région, bilan visé pour la région) que le nombre de régions indiqué. Après application de toutes les autres lois du même tirage (notamment des lois de consommation et puissance imposée), METRIX calcule le bilan  de puissance, i.e. la somme des productions imposées moins la somme des consommations de la même région, le résultat est nommé $\Delta1$. A noter qu’il s’agit réellement du bilan s’il n’y a pas de groupes modifiables. Il y a une vérification sur ce point et un rejet de la variante s’il y a un groupe modifiable dans une région pour laquelle on veut caler un bilan.<br>La loi fournit le bilan voulu, en MW, dans cette région, on le nommera $\Delta2$ ; notons que si la grandeur $\Delta2$ est positive alors il y a plus de production que de consommations. La différence ($\Delta1 - \Delta2$) correspond à la puissance à redistribuer sur toutes les consommations de la région. La répartition se fait au prorata de la consommation : $c_i = c_i – c_i*(\Delta1 - \Delta2)  / sumC$ ; $c_i$ est la consommation au  nœud $i$ et $sumC$ est la somme des consommations de toute la région concernée. 
Cette loi datant d’ASSESS n’est actuellement pas utilisée dans imaGrid.    
- >**ECHANGP** : le nombre de régions, puis autant de couples (indice de région, bilan visé pour la région) que le nombre de régions indiqué. Cette loi est similaire à la loi ECHANG mais elle agit sur les groupes de production au lieu des consommations. Après application de toutes les autres lois du même tirage, METRIX ajuste les puissances des groupes modifiables de la région pour équilibrer le bilan (somme des productions moins somme des consommations) à la valeur indiquée dans la variante. La sélection des groupes à modifier se fait suivant les coûts d’empilement hors réseau (à la hausse et à la baisse). Si, sur une région, il y a trop de production non modifiable ou trop peu de production disponible pour équilibrer le bilan, la variante est rejetée. De plus, METRIX ne garantit pas que le bilan ne sera pas modifié lors de la phase hors réseau. C’est à l’utilisateur de choisir des coûts d’empilement hors réseau prohibitifs pour la région considérée par rapport aux régions d’étude. 
Cette loi datant d’ASSESS n’est actuellement pas utilisée dans imaGrid.
- >**CTORDR** : le nombre de coût à la hausse sans réseau, puis autant de couples (nom du groupe, coût) que le nombre de groupes indiqué.
- >**COUBHR** : le nombre de coût à la baisse sans réseau, puis autant de couples (nom du groupe, coût) que le nombre de groupes indiqué.
- >**COUHAR** : le nombre de coût à la hausse avec réseau, puis autant de couples (nom du groupe, coût) que le nombre de groupes indiqué.
- >**COUBAR** : le nombre de coût à la baisse avec réseau, puis autant de couples (nom du groupe, coût) que le nombre de groupes indiqué.
- >**DCMINPUI** : le nombre de modification de Puissance min HVDC puis autant de couples (nom de la ligne à courant continu, nouvelle Pmin) qu’indiqué.
- >**DCMAXPUI** : le nombre de modification de Puissance max HVDC puis autant de couples (nom de la ligne à courant continu, nouvelle Pmax) qu’indiqué.
- >**DCIMPPUI** : le nombre de modifications de la puissance de consigne des lignes HVDC, puis autant de couples (nom de la ligne à courant continu, puissance de consigne) qu’indiqué. Si la ligne à courant continu n’est pas en mode « consigne imposée » dans le cas de base, METRIX modifie automatiquement le mode de fonctionnement de la ligne HVDC (pour cette variante uniquement). 

**Remarque** : Si une ligne HVDC est en mode « consigne imposée », avant d’analyser la variante, METRIX vérifie que la puissance de consigne de la ligne est bien comprise entre les bornes MAX et MIN. Si ce n’est pas le cas, la variante est rejetée. 
- >**DTVALDEP** : le nombre de modifications de la valeur initiale du déphasage du TD, puis autant de couples (nom du TD, déphasage initial) qu’indiqué.
- >**DTVALSUP** : le nombre de modifications de la valeur maximale du déphasage du TD, puis autant de couples (nom du TD, déphasage max) qu’indiqué.
- >**DTVALINF** : le nombre de modifications de la valeur minimale du déphasage du TD, puis autant de couples (nom du TD, déphasage min) qu’indiqué.
- >**COUEFF** : le nombre de modifications du coût d’effacement de la consommation, puis autant de couples (indice de la consommation, coût) qu’indiqué.
- >**QATI00MN** : le nombre de modifications du seuil N, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATI5MNS** : le nombre de modifications du seuil N-1, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATI20MN** : le nombre de modifications du seuil N-k, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATITAMN** : le nombre de modifications du seuil avant manœuvre, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.

**Attention** : pour chaque variante, le modèle ne tient compte que d’une seule ligne pour chaque type de loi. Si plusieurs lois sont définies pour une même variante seule la dernière sera conservée.

Le numéro de la première variante est toujours $\geq 0$.

La variante « -1 » permet d’indiquer des modifications sur le cas de base qui seront valables pour toutes les variantes (à moins qu’elles ne soient modifiées par les données d’une variante)  

**Exemple** :

--------------------------------------
```
NT;2;
<br>-1;PRODIM;1;groupe1;100; (modification commune à toutes les variantes)
<br>0; (variante sans modification par rapport au cas de base)
<br>1;QUADIN;1;ligne indispo;
<br>1;PRODIM;2;groupe1;200;groupe2;300;
```
--------------------------------------

# Ligne de commande pour lancer METRIX <a id="command_line"></a>
La ligne de commande utilisée est la suivante : 
```
metrix log variantes sorties debut max
```
- >**metrix** : nom l’exécutable METRIX
- >**log** : est le nom du fichier dans lequel sont écrites les traces d’exécution utiles à l’analyse en cas de problème.
- >**variantes** : est le nom du fichier des variantes (ex. Variantes.csv).
- >**sorties** : est le préfixe générique du fichier des résultats auquel on concatène pour chaque variante « _s » suivi du numéro de la variante.
- >**debut** : est le numéro de la première variante à traiter dans le fichier des variantes.
- >**max** : est le nombre maximum de variantes à traiter sous réserve qu’il y en ait suffisamment dans le fichier des variantes. 

Le réseau est lu sous forme d'un fichier *json*. Ce fichier doit être dans le dossier pointé par la variable d’environnement HADES_DIR.

# Données de sorties <a id="output"></a> 
Pour faciliter l’intégration et les tests de METRIX dans imaGrid, les sorties de METRIX v6 conservent le format des fichiers tabulés d’ASSESS (et de METRIS).
Il y a un seul fichier par variante. Au sein de ce fichier, les données sont regroupées dans des tableaux de sortie par thème. Chaque ligne du fichier commence par l’identifiant du tableau. 

Pour chaque champ, on définit :
- le nom de la grandeur stockée
- le format de la donnée : C = Chaîne, I = Entier, R = Réel
- pour une grandeur numérique (I ou R), le type de la grandeur : MW pour une puissance, u.m. (unité monétaire) pour un coût, rien autrement

Certain tableau ne sont pas accessibles sauf si l'option '*--all-outputs*' est donné lors du lancement à Metrix simulator.

## Tableaux descriptifs de la situation <a id="detailed_tables"></a>
**Tableau S1** : ouvrages indisponibles (quadripôles et groupes).
| | | | |
| :-- | :-- | :-- | :-- |
| type de l’ouvrage | I | |	1 : quadripôle ; 2 : groupe |
| nom de l’ouvrage | C | | |

**Note** : Tableau disponible en mode si l'option '*--all-outputs*' est donné lors du lancement à Metrix simulator.

**Tableau C1** (une seule ligne) : compte rendu d’exécution
| | | | |
| :-- | :-- | :-- | :-- |
| code d’erreur | I | | **0** si « OK », sinon<br>**-1** = erreur lors de la résolution<br>**1**  = si pas de solution au problème (problème infaisable, souvent cause EOD)<br>**2**  = trop de contraintes (max atteint)<br>**3** = trop de micro-itération (max atteint)<br>**4** = données de variante non cohérentes (variante ignorée) |

**Tableau C2** : liste des incidents rompant la connexité
| | | | |
| :-- | :-- | :-- | :-- |
| nom de l’incident	C | | |
| Nombre de nœuds déconnectés |	I | | |
| Volume de production coupé |	R | MW | |	
| Volume de consommation coupé | R | MW | |

**Tableau C3** (une seule ligne) : nombre de « non-connexité » sur déclenchement
| | | | |
| :-- | :-- | :-- | :-- |
| nombre de « non connexité » | I | | |

**Note** : *ce tableau n’est pas lu par imaGrid; il n’est pas rempli*

**Tableau C4** <a id="table_c4"></a>: liste des incidents contraignants, des incidents qui ont pu être traités en curatif ou des incidents ayant générés un transit maximal sur incident (cf. [tableau R3B](#table_r3b)).
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro d’ordre de l’incident | I | | Numérotation à partir de 1. Numéros utilisés ensuite dans tableaux [R4](#table_r4), [R1B](#table_r1b), [R2B](#table_r2b), [R3B](#table_r3b), [R5B](#table_r5b) et [R6B](#table_r6b) (cf. ci-après) |
| Type | C | | «N-1L», «N-KL», «N-KG», «N-HVDC», «COMBO» |
| Nom de l’incident | C | | Nom de l’incident |

**Tableau C5** (une seule ligne) : bilans initiaux par zone synchrone, avant équilibrage
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro de zone synchrone | I | | Ex. 0 |
| Valeur du bilan initial | R | MW | Ex. -20.5 |

## Tableaux de résultats <a id="results_tables"></a>
Note : dans les tableaux de sorties, seules les valeurs supérieures au seuil EPSILON_SORTIES = 0.05 sont affichées.

**Tableau R1** : résultats par sommet. 
| | | | |
| :-- | :-- | :-- | :-- |
| nom du sommet	C | | |
| demande | R | MW | Somme des consommations à ce sommet |
| défaillance HR | R | MW | Défaillance lors de l’équilibrage due à un manque de production |
| défaillance AR | R | MW | Défaillance liée au réseau, en plus de l’éventuelle défaillance due au manque de production |

**Tableau R1B** <a id="table_r1b"></a>: résultats par consommation curative. Sont uniquement affichés les consommations activées en curatif sur un incident.
| | | | |
| :-- | :-- | :-- | :-- |
| nom de la consommation | C | | nom de la consommation |
| numéro d’incident | I | | Référence à la numérotation de la [table C4](#table_c4) |
| puissance effacée | R | MW | |

**Tableau R1C** : résultats par couplage de consommations.
| | | | |
| :-- | :-- | :-- | :-- |
| nom du couplage | C | | nom donné au couplage dans la configuration |
| variation | R | MW | Somme des variations sur l’ensemble des consommations du couplage |

**Tableau R2** : résultats par groupe. Seuls les groupes dont la consigne préventive diffère de la consigne initiale (avant ou après ajustement selon l’option choisie) sont affichés.
| | | | |
| :-- | :-- | :-- | :-- |
| nom du groupe | C | | |		
| puissance disponible | R | MW | Pmax du groupe dans la variante |
| ajustement de puissance imposé | R | MW | $\Delta P_{cons}$ entre la donnée *json* et celle de la variante |
| ajustement de puissance lors de l’équilibrage initial | R | MW | $\Delta P_{cons}$ entre la donnée de la variante et la valeur après la phase d’équilibrage (si demandé et > 0,001 en valeur absolue) |
| ajustement de puissance préventif | R | MW | $\Delta P_{cons}$ entre la phase d’équilibrage et la consigne préventive  (si > 0,001 en valeur absolue) |

**Tableau R2B** <a id="table_r2b"></a>: résultats curatifs par groupe. Seuls les groupes dont la consigne curative diffère de la consigne préventive sont affichés.
| | | | |
| :-- | :-- | :-- | :-- |
| numéro d’ incident | I | | Référence à la numérotation de la [table C4](#table_c4) |
| nom du groupe	| C | | |
| puissance ajustée | R | MW | Si supérieure à 0,001 en valeur absolue |

**Tableau R2C** : résultats par couplage de groupes.
| | | | |
| :-- | :-- | :-- | :-- |
| nom du couplage | C | | nom donné au couplage dans la configuration |
| variation | R | MW | Somme des variations sur l’ensemble des groupes du couplage |

**Tableau R3** : transits en N par quadripôle 
| | | | |
| :-- | :-- | :-- | :-- |
| nom du quadripôle | C | | |		
| Transit N | R | MW | positif de départ vers arrivée |

**Note** : en mode DEBUG, les 4 seuils sont également affichés

**Tableau R3B** <a id="table_r3b"></a>: transits maximum par quadripôle sur incident. Seuls les ouvrages surveillés ou avec résultats sont affichés.
| | | | |
| :-- | :-- | :-- | :-- |
| nom du quadripôle | C | | | 		
| incident cause du transit max avant manœuvres | | | Référence à la numérotation de la [table C4](#table_c4) |
| transit max avant manœuvres | R | MW | positif de départ vers arrivée |
| incident cause du 1er transit max sur  incident | | | Référence à la numérotation de la [table C4](#table_c4) |
| 1 $^{er}$ transit max sur incident | R | MW | positif de départ vers arrivée |
| *incident cause du N $^{ème}$ transit max sur  incident* | | | *Autant de résultats que demandé dans les paramètres* |
| *N $^{ème}$ transit max sur incident* | | | |

**Tableau R3C** : transits spécifique sur incident
| | | | |
| :-- | :-- | :-- | :-- |
| nom du quadripôle | C | | |		
| numéro d’ incident | I | | Référence à la numérotation de la [table C4](#table_c4) |
| transit | R | MW | positif de départ vers arrivée |

**Tableau R4** <a id="table_r4"></a>: variations marginales par liaison, en N et sur incident
| | | | |
| :-- | :-- | :-- | :-- |
| nom du quadripôle | C | | |		
| numéro d’incident | I | | 0 pour N, sinon cf. [tableau C4](#table_c4) |
| variation marginale | R | u.m. | Impact sur la fonction objectif d’une augmentation de 1MW sur le seuil (N, N-k ou AM) de cet ouvrage |

**Tableau R4B** : variations marginales détaillées par liaison, en N et sur incident (seuls les couples (ouvrage, incident) pour lesquels les variations marginales détaillées ont été demandées et sont non nulles sont stockées dans le tableau)
| | | | |
| :-- | :-- | :-- | :-- |
| nom du quadripôle | C | | |		
| numéro d’ incident | I | | 0 pour N, sinon cf. [tableau C4](#table_c4) |
| type de l’ouvrage ou de la transaction qui varie | C | | sommet « N », groupe « G » ou action curative « C » |
| nom de l’ouvrage qui varie | C | | sommet (si variation de défaillance) ou groupe (si variation de production) |
| volume de variation | R | MW | Coefficient de sensibilité de l’ouvrage |
| coût de la variation | R | u.m. | Contribution de cette variable au coût marginal |

**Tableau R5** : résultats par transformateur déphaseur. Seules les liaisons dont la consigne préventive diffère de la consigne initiale sont affichées.
| | | | |
| :-- | :-- | :-- | :-- |
| nom du TD	C | | |
| Consigne | R | ° ou MW | selon le mode de pilotage du TD |
| Consigne HR | R | ° ou MW | selon le mode de pilotage du TD |

**Tableau R5B** <a id="table_r5b"></a>: résultats des actions curatives des transformateurs déphaseurs. Seules les liaisons dont la consigne curative diffère de la consigne préventive sont affichées.
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro de l’incident | R | | Référence à la numérotation de la [table C4](#table_c4) |
| Nom du TD | C | | |
| Consigne en curatif du TD | R | ° ou MW | Selon le mode de pilotage du TD |

**Tableau R6** : résultats par ligne à courant continu. Seules les liaisons dont la consigne préventive diffère de la consigne initiale sont affichées.
| | | | |
| :-- | :-- | :-- | :-- |
| nom de la ligne à courant continu | C | | |		
| puissance transitant | R | MW | |
| Variation marginale de la HVDC | R | u.m | Gain sur la fonction coût d’1 MW de plus sur la HVDC (il s’agit du max entre la variation marginale préventive et les variations marginales curatives) |
 
**Tableau R6B** <a id="table_r6b"></a>: résultats par actions curatives des HVDC. Seules les liaisons dont la consigne curative diffère de la consigne préventive sont affichées.
| | | | |
| :-- | :-- | :-- | :-- |
| numéro de l’incident  | R | | Référence à la numérotation de la [table C4](#table_c4) |
| nom de la HVDC | C | | |
| consigne en curatif de la HVDC | R | MW | |
| Variation marginale curative de la HVDC | R | u.m | Gain sur la fonction coût d’1 MW de plus sur la HVDC pour cet incident (non utilisé) |

**Tableau R7** : redispatching par filière
| | | | |
| :-- | :-- | :-- | :-- |
| filière | C | | |
| Redispatching préventif à la baisse | R | MW | ∑ groupes de la filière |
| Redispatching préventif à la hausse | R | MW | ∑ groupes de la filière |
| Redispatching curatif à la baisse | R | MW | ∑ groupes de la filière |
| Redispatching curatif à la hausse | R | MW | ∑ groupes de la filière |

**Tableau R8** : résultats sur les pertes calculées en actif-seul
| | | | |
| :-- | :-- | :-- | :-- |
| volume de pertes calculé | R | MW | |	
| taux de pertes | R | % | |

**Tableau R8B** : résultats sur les pertes par région
| | | | |
| :-- | :-- | :-- | :-- |
| Région ou HVDC | R | MW | |
| volume de pertes calculé | R | % | |

**Tableau R9** : résultats de la fonction objectif
| | | | |
| :-- | :-- | :-- | :-- |
| coût de redispatching | R | u.m. | Coût des ajustements préventifs de production |
| coût de la défaillance | R | u.m. | Coût du délestage préventif |
| volume de dépassement sur incident | | MW | Somme des dépassements de seuils sur incidents pour les ouvrages surveillés |
| volume de dépassement en N | | MW | Somme des dépassement en N pour les ouvrages surveillés |
| coût de redispatching curatif | | u.m. | Coût des ajustements curatifs de production |
| coût d’effacement curatif | | u.m. | Coût des ajustements curatifs de consommation |

**Tableau R10** : résultats du curatif topologique (parades sélectionnées). Les parades « ne rien faire » ne sont pas affichées.
| | | | |
| :-- | :-- | :-- | :-- |
| numéro de l’incident | I | | Référence à la numérotation de la [table C4](#table_c4) |
| nom incident | C | | Nom de l’incident initial |
| nombre d’action(s) de la parade | I | | |
| nom de la parade | C | | |
