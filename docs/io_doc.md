<style>
r { color: Red }
o { color: Orange }
g { color: Green }
y { color: yellow}
</style>

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
4. [Parades](#parades)
5. [Données de sorties](#output)
    1. [Tableaux descriptifs de la situation](#detailed_tables)
    2. [Tableaux de résultats](#results_tables)

# Introduction <a id="introduction"></a>
Ce document décrit les entrées et les sorties du modèle METRIX v6 (utilisé dans la plateforme imaGrid). Suite à l’intégration dans imaGrid, certains formats utilisés initialement dans la plate-forme ASSESS ont été conservés ; à savoir CSV pour les variantes, parades et pour les tableaux des sorties. Les autres données d'entrée ont, quant-à-elle, été mises sous forme *json*, au sein desquelles certaines ont été ajoutées et d’autres rendues optionnelles. Le format de données de METRIX v6 n’est donc pas compatible avec les versions précédentes du modèle.

# Données d’entrée au format *json* <a id="input"></a>
La passerelle imaGrid pour METRIX prend en entrée un fichier réseau au format IIDM et un script de configuration au format Groovy. Elle génère 1 fichier *json* : '*fort.json*'.

Les données indiquées en **gras** dans les tableaux suivants doivent toujours être présentes dans les fichiers d’entrée, les autres sont optionnelles.
La valeur par défaut configurée dans METRIX est indiquée entre parenthèses.
Celles indiquées, quant-à-elles, en <o>orange</o>, sont des données non obligatoires mais qui provoquent des erreurs de segmentation si elles ne sont pas renseignées. Enfin, celles indiquées en <y>jaune</y> provoquent des erreurs (cf. fichier de logs) si elles ne sont pas renseignées.

N.B. : Les types de données sont définis par une mettre comme suit <a id="types"></a>:
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
| <o>TNNOMNOE</o> | C | ECNBCONS | Noms des consommations<br>= load.id |
| <o>TNNEUCEL</o> <a id="table_tnneucel"></a>| I | ECNBCONS | Numéro du sommet de raccordement de la consommation élémentaire<br>= indice du bus dans la table TNNOMNOE |
| <o>CPPOSREG</o> | I | ECNBCONS | Lien sommet-région<br>= indice de la région dans CGNOMREG |
| <o>ESAFIACT</o> | R | ECNBCONS | Valeur de la consommation active (somme de la part fixe et affine)<br>= load.p0 |
| TNVAPAL1 | I | ECNBCONS | Pourcentage du premier palier de délestage.<br>(100 si aucun nœud configuré) |
| TNVACOU1 | R | ECNBCONS | Coût du délestage (valeur de COUTDEFA par défaut) |

## Groupes <a id="groups"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **TRNBGROU** | I | 1 | Nombre de groupes raccordés<br>= $\sum$ generator t.q. generator.terminal.busBreakerView.bus $\in$  composante connexe principale |
| <o>TRNOMGTH</o> <a id="table_trnomgth"></a>| C | TRNBGROU | Nom du groupe<br>= generator.id |
| <o>TNNEURGT</o> | I | TRNBGROU | Sommets de raccordement du groupe<br>= indice du bus dans la table TNNOMNOE |
| <o>SPPACTGT</o> | R | TRNBGROU | Puissance de consigne $P_{obj} \in [\underline{P},\overline{P}]$<br>= generator.targetP |
| <y>TRVALPMD</y> | R | TRNBGROU | Puissance max disponible<br>= generator.maxP |
| <o>TRPUIMIN</o> | R | TRNBGROU | Puissance min<br>= generator.minP |
| **TRNBTYPE** | I | 1 | Nombre de types de groupe |
| <o>TRNOMTYP</o> | C | TRNBTYPE | Noms des types de groupes |
| <o>TRTYPGRP</o> | I | TRNBGROU | Indice du type de groupe dans TRNOMTYP |
| <o>SPIMPMOD</o> | I | TRNBGROU | Indique si le groupe est disponible pour le redispatching (3 = 'OUI_AR'), l’adequacy (2 = 'OUI_HR'), les deux (1 = 'OUI_HR_AR') ou aucune des deux (0 = 'NON_HR_AR')<br>(1 si aucun groupe n’est configuré et 0 sinon.) |
| TRDEMBAN | R | TRNBGROU | Demi-bande de réglage en réglage secondaire.<br>= prorata du plus grand incident groupe (0) |

## Quadripôles <a id="quadri"></a>
### Quadripôles élémentaires <a id="quadri_elem"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **CQNBQUAD** | I | 1 | Nombre de quadripôles élémentaires<br>= $\sum$ (quad = *line* &#124;&#124; *twoWindingsTransformer*) t.q. quad.terminal1.busBreakerView.bus && quad.terminal2.busBreakerView.bus $\in$ composante connexe principale<br>+ $\in$ *switch* t.q. switch.retained && (switch.voltageLevel.busBreakerView.bus1 && switch.voltageLevel.busBreakerView.bus2 $\in$ composante connexe principale)
| <o>CQNOMQUA</o> | C | CQNBQUAD | Nom du quadripôle<br>= quad.id |
| <o>CQADMITA</o> | R | CQNBQUAD |	Admittance du quadripôle ramené à Ubase (inverse de la réactance, i.e. négliger la valeur de la résistance)<br>= $1 / (X * nominalU^2 / Unom^2)$<br>Pour les lignes :<br>$X = line.x$<br>Pour les transformateurs :<br>$X_{transfo} = twoWindingsTransformer.x$<br>Pour les transformateurs déphaseurs :<br>$X = X_{transfo} * (1 + X_{prise} / 100)$<br>$X_{prise} = phaseTapChanger.phaseTapPosition.x$<br>Pour les couplages :<br>$X = 10^-5$<br>Unom = line.terminal2.voltageLevel.nominalV
| <o>CQRESIST</o> | R | CQNBQUAD | Résistance utilisée pour le calcul des pertes (en pu, base nominalU)<br>Les formules sont les mêmes que pour CQADMITA en remplaçant X par R |
| <o>QASURVDI</o> | I | CQNBQUAD | Indicateur de surveillance du quadripôle en N. 2 = résultat de transit uniquement, 1 = surveillé par le modèle, 0 = aucun résultat |
| <o>QASURNMK</o> | I | CQNBQUAD | Indicateur de surveillance du quadripôle en N-K. 2 = résultat de transit uniquement, 1 = surveillé par le modèle, 0 = aucun résultat |
| <y>TNNORQUA</y> | I | CQNBQUAD | Indice du nœud origine du quadripôle (quad.terminal1.busBreakerView.bus) dans la table TNNOMNOE |
| <y>TNNEXQUA</y> | I | CQNBQUAD | Indice du nœud extrémité du quadripôle (quad.terminal2.busBreakerView.bus) dans la table TNNOMNOE |
| NBOPEBRA | I | 1 | Nombre de branches ouvertes conservées car elles peuvent être refermées par une parade<br>(0) |
| OPENBRAN | I | NBOPEBRA | Indices des branches ouvertes dans la table CQNOMQUA |

### Déphaseurs <a id="phase_shifters"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **DTNBTRDE** | I | 1 | Nombre total de transformateurs déphaseurs<br>$\sum$ twoWindingsTransformer.phaseTapChanger si twoWindingsTransformer est dans la table CQNOMQUA
| <o>DTTRDEQU</o> | I | DTNBTRDE | Correspondance déphaseur-numéro du quadripôle élémentaire<br>= indice de twt.id dans les tables de quadripôles
| <o>DTMODREG</o> | I | DTNBTRDE | Type de contrôle<br>0 : Hors service<br>1 : angle optimisé<br>2 : angle fixe<br>3 : puissance optimisé (non utilisé)<br>4 : puissance fixe (non utilisé) |
| <o>DTVALINF</o> | R | DTNBTRDE | Valeur min du déphasage du TD en N, en degrés<br>= phaseTapChanger.lowTapPosition.alpha |
| <o>DTVALSUP</o> | R | DTNBTRDE | Valeur max du déphasage du TD en N, en degrés <br>= phaseTapChanger.highTapPosition.alpha |
| <o>DTVALDEP</o> | R | DTNBTRDE | Valeur initiale du déphasage par TD en degrés, c’est la valeur utilisée si DTMODREG $\in {1, 2}$<br>= phaseTapChanger.tapPosition.alpha |

## Lignes à courant continu <a id="dc_lines"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **DCNBLIES** | I | 1 | Nombre de lignes à courant continu<br>= $\sum$ hvdcLine t.q.  (hvdcLine.converterStation1.terminal.busBreakerView.bus && hvdcLine.converterStation2.terminal.busBreakerView.bus) $\in$ composante connexe principale |
| <o>DCNOMQUA</o> <a id="table_dcnomqua"></a>| C | DCNBLIES | Nom de la ligne à courant continu<br>= hvdcLine.id |
| <o>DCNORQUA</o> | I | DCNBLIES | Indice du nœud origine de la ligne (hvdcLine.converterStation1.terminal.busBreakerView.bus) dans la table TNNOMNOE |
| <o>DCNEXQUA</o> | I | DCNBLIES | Indice du nœud extrémité de la ligne (hvdcLine.converterStation2.terminal.busBreakerView.bus) dans la table TNNOMNOE |
| <o>DCMINPUI</o> | R | DCNBLIES | Puissance minimale<br>= - activePowerRange.oPRFromCS2toCS1 si l’extension HvdcOperatorActivePowerRange est utilisée, - hvdcLine.maxP sinon. |
| <o>DCMAXPUI</o> | R | DCNBLIES | Puissance maximale<br>= activePowerRange.oPRFromCS1toCS2 si l’extension HvdcOperatorActivePowerRange est utilisée, hvdcLine.maxP sinon. |
| <o>DCIMPPUI</o> | R | DCNBLIES | Puissance imposée $P_0$<br>= activePowerControl.p0 si l’extension  HvdcAngleDroopActivePowerControl est utilisée, +/- hvdcLine.activePowerSetPoint sinon |
| <o>DCREGPUI</o> | I | DCNBLIES | Type de réglage de transit sur la ligne<br>1 : puissance fixe<br>2 : puissance optimisée  |
| <o>DCTENSDC</o> | R | DCNBLIES | Tension nominale du câble DC<br>= hvdcLine.nominalV |
| <o>DCRESIST</o> | R | DCNBLIES | Résistance du câble DC en pu (base nominalV), normalisée par rapport à la tension DC.<br>= hvdcLine.r * nominalU $^2$ / hvdcLine.nominalV $^2$ |
| <o>DCPERST1</o> | R | DCNBLIES | Coefficient de pertes (en %) de la station de conversion origine<br>= hvdcLine.converterStation1.lossFactor |
| <o>DCPERST2</o> | R | DCNBLIES | Coefficient de pertes (en %) de la station de conversion extrémité<br>= hvdcLine.converterStation2.lossFactor |
| DCNDROOP | I | 1 | Nombre de HVDC opérée en mode émulation AC ($P = P_0 + k \Delta\Theta$)<br>(0) |
| DCDROOPK | R | DCNDROOP | Pour chaque HVDC en émulation AC, dans l’ordre de la [table DCNOMQUA](#table_dcnomqua), valeur du coefficient k (en MW/°) |

## Incidents N-1 et N-k <a id="incidents"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| **DMNBDEFK** | I | 1 | Nombre d’incidents |
| DMNOMDEK | C | DMNBDEFK | Nom de l’incident |
| DMPTDEFK | I | DMNBDEFK | Par incident, pointeur sur DMDESCRK |
| DMDESCRK | I | DMNBDEFK<br>$+ 2 * \sum nb_{<i>}$ | Description des incidents<br>Pour chaque incident :<br>Nb, $Type_{<1>}$, $Indice_{<1>}$, …, $Type_{<nb>}$, $Indice_{<nb>}$<br>Nb : nombre d’éléments dans l’incident<br>Type :<br>1 pour un quadripôle (ligne, transfo)<br>2 pour un groupe<br>3 pour une liaison à courant continu<br>Indice : dans la liste de quadripôles, dans la liste des groupes ou dans la liste des liaisons à courant continu selon le type |
| BDEFRES | I | 1 | Taille du tableau des défauts avec résultats détaillés<br>(0) |
| TDEFRES | I | NBDEFRES | Suite de triplets : indice du quadripôle, nombre de défauts avec résultats détaillés, liste des indices de ces défauts. |

## Curatif <a id="curative"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| DTNBDEFK | I | DTNBTRDE | Pour chaque TD, nombre d’incidents pour lesquels le TD peut agir en curatif<br>(0) |
| DTPTDEFK | I | ≤ DTNBTRDE  * DMNBDEFK	| Pour chaque TD agissant en curatif, dans l’ordre des indices croissants, indices des incidents traités en curatif |
| DCNBDEFK | I | DCNBLIES | Pour chaque TD, nombre d’incidents pour lesquels le TD peut agir en curatif<br>(0) |
| DCPTDEFK | I | ≤ DCNBLIES * DMNBDEFK | Pour chaque TD agissant en curatif, dans l’ordre des indices croissants, indices des incidents traités en curatif |
| NBLDCURA | I | 1	| Nombre de consommations pouvant s’effacer en curatif<br>(0) |
| LDNBDEFK | I | ECNBCONS | Pour chaque consommation, nombre d’incidents traités en curatif |
| LDCURPER | I | NBLDCURA | Pour chaque consommation curative, dans l’ordre des indices, pourcentage d’effacement en curatif |
| LDPTDEFK | I | ≤ NBLDCURA * DMNBDEFK | Pour chaque consommation curative, dans l’ordre de la [table TNNEUCEL](#table_tnneucel), pointeur des incidents traités en curatif. |
| GRNBCURA | I | 1 | Nombre de groupes pouvant agir en curatif<br>(0) |
| GRNBDEFK | I | GRNBCURA | Pour chaque groupe curatif, nombre d’incidents traités en curatif |
| GRPTDEFK | I | ≤ GRNBCURA * DMNBDEFK | Pour chaque groupe curatif, dans l’ordre de la [table TRNOMGTH](#table_trnomgth), pointeur des incidents traités en curatif. |

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
| NBGBINDS | I | 1 | Nombre de couplages de groupes<br>(0) |
| NBLBINDS | I | 1 | Nombre de couplages de consommations<br>(0) |
| GBINDDEF | I | ≤ NBGBINDS + TRNBGROU | Pour chaque couplage, nombre de groupes dans le couplage puis indices des groupes couplés |
| GBINDNOM | C | NBGBINDS | Noms des couplages de groupes |
| GBINDREF | I | NBGBINDS | Pour chaque couplage, type de la variable de référence (0 = PMAX, 1 = PMIN, 2 = POBJ, 3 = PMAX-POBJ) |
| LBINDDEF | I | ≤ NBLBINDS + ECNBCONS | Pour chaque couplage, nombre de consommations dans le couplage puis indices des consommations couplées |
| LBINDNOM | C | NBLBINDS | Noms des couplages de consommations |

## Variations marginales détaillées <a id="detailed_marginal_variations"></a>
| Nom | Type | Taille | Description |
| :-- | :--- | :----- | :---------- |
| NBVARMAR | I | 1 | Taille du tableau des variations marginales détaillées (PTVARMAR)<br>(0) |
| PTVARMAR | I | NBVARMAR | Suite de triplets : indice du quadripôle, nombre de défauts avec variations marginales détaillées, liste des indices de ces défauts. |

# Variantes <a id="variants"></a>
Les données spécifiques à chaque variante sont décrites dans un fichier au format csv.
La première ligne indique le nombre de tirages (i.e. de variantes), après le mot-clé *NT*. 
Chaque ligne du fichier commence ensuite par le numéro du tirage suivi par le type de la loi décrite identifiée par un mot-clé.
Les données propres à la loi et au tirage sont écrites à la suite du mot-clé, à savoir :
- >**PRODIN** : le nombre de groupes, puis les noms des groupes indisponibles.
- >**PRODIM** : le nombre de groupes, puis autant de couples (indice de groupe, puissance de consigne) que le nombre de groupes indiqué.
- >**QUADIN** : le nombre de lignes indisponibles (consignations) puis les noms des ouvrages indisponibles 
- >**TRVALPMD** : le nombre de groupes, puis autant de couples (indice de groupe, puissance maximale disponible) que le nombre de groupes indiqué.
- >**TRPUIMIN** : le nombre de groupes, puis autant de couples (indice de groupe, puissance minimale disponible) que le nombre de groupes indiqué.
- >**CONELE** : le nombre de consommations, puis autant de couples (nom de la consommation, nouvelle valeur de consommation) que le nombre de consommations indiqué. Il s’agit de consommations nodales. Convention Eurostag : 1 nœud = 1 conso
- >**ECHANG** : loi qui devrait s’appeler plutôt bilan par les consommations. Le nombre de régions, puis autant de couples (indice de région, bilan visé pour la région) que le nombre de régions indiqué. Après application de toutes les autres lois du même tirage (notamment des lois de consommation et puissance imposée), METRIX calcule le bilan  de puissance, i.e. la somme des productions imposées moins la somme des consommations de la même région, le résultat est nommé $\Delta1$. A noter qu’il s’agit réellement du bilan s’il n’y a pas de groupes modifiables. Il y a une vérification sur ce point et un rejet de la variante s’il y a un groupe modifiable dans une région pour laquelle on veut caler un bilan.<br>La loi fournit le bilan voulu, en MW, dans cette région, on le nommera $\Delta2$ ; notons que si la grandeur $\Delta2$ est positive alors il y a plus de production que de consommations. La différence ($\Delta1 - \Delta2$) correspond à la puissance à redistribuer sur toutes les consommations de la région. La répartition se fait au prorata de la consommation : $c_i = c_i – c_i*(\Delta1 - \Delta2)  / sumC$ ; $c_i$ est la consommation au  nœud $i$ et $sumC$ est la somme des consommations de toute la région concernée. 
Cette loi datant d’ASSESS n’est actuellement pas utilisée dans imaGrid.    
- >**ECHANGP** : le nombre de régions, puis autant de couples (indice de région, bilan visé pour la région) que le nombre de régions indiqué. Cette loi est similaire à la loi ECHANG mais elle agit sur les groupes de production au lieu des consommations. Après application de toutes les autres lois du même tirage, METRIX ajuste les puissances des groupes modifiables de la région pour équilibrer le bilan (somme des productions moins somme des consommations) à la valeur indiquée dans la variante. La sélection des groupes à modifier se fait suivant les coûts d’empilement hors réseau (à la hausse et à la baisse). Si, sur une région, il y a trop de production non modifiable ou trop peu de production disponible pour équilibrer le bilan, la variante est rejetée. De plus, METRIX ne garantit pas que le bilan ne sera pas modifié lors de la phase hors réseau. C’est à l’utilisateur de choisir des coûts d’empilement hors réseau prohibitifs pour la région considérée par rapport aux régions d’étude. 
Cette loi datant d’ASSESS n’est actuellement pas utilisée dans imaGrid.
- >**CTORDR** : le nombre de coût à la hausse sans réseau, puis autant de couples (nom du groupe, coût) que le nombre de groupes indiqué.
- >**COUBHR** : le nombre de coût à la baisse sans réseau, puis autant de couples (nom du groupe, coût) que le nombre de groupes indiqué.
- >**COUHAR** : le nombre de coût à la hausse avec réseau, puis autant de couples (nom du groupe, coût) que le nombre de groupes indiqué.
- >**COUBAR** : le nombre de coût à la baisse avec réseau, puis autant de couples (nom du groupe, coût) que le nombre de groupes indiqué.
- >**DCMINPUI** : le nombre de modification de puissance min HVDC puis autant de couples (nom de la ligne à courant continu, nouvelle Pmin) qu’indiqué.
- >**DCMAXPUI** : le nombre de modification de puissance max HVDC puis autant de couples (nom de la ligne à courant continu, nouvelle Pmax) qu’indiqué.
- >**DCIMPPUI** : le nombre de modifications de la puissance de consigne des lignes HVDC, puis autant de couples (nom de la ligne à courant continu, puissance de consigne) qu’indiqué. Si la ligne à courant continu n’est pas en mode « consigne imposée » dans le cas de base, METRIX modifie automatiquement le mode de fonctionnement de la ligne HVDC (pour cette variante uniquement). 

**Remarque** : Si une ligne HVDC est en mode « consigne imposée », avant d’analyser la variante, METRIX vérifie que la puissance de consigne de la ligne est bien comprise entre les bornes MAX et MIN. Si ce n’est pas le cas, la variante est rejetée. 
- >**DTVALDEP** : le nombre de modifications de la valeur initiale du déphasage du TD, puis autant de couples (nom du TD, déphasage initial) qu’indiqué.
- >**COUEFF** : le nombre de modifications du coût d’effacement de la consommation, puis autant de couples (indice de la consommation, coût) qu’indiqué.
- >**QATI00MN** : le nombre de modifications du seuil N, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATI5MNS** : le nombre de modifications du seuil N-1, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATI20MN** : le nombre de modifications du seuil N-k, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATITAMN** : le nombre de modifications du seuil avant manœuvre, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**GROURAND** : le nombre de groupes, la liste ordonnée des groupes pour le 'mélange' pour ne pas avoir à bruiter les coûts.
- >**PROBABINC** : le nombre de probabilités des incidents, puis autant de couples (nom de l'incident, probabilité).
- >**QATI00MN2** : le nombre de modifications du seuil N de l'extrémité vers l'origine (si seuil asymétrique), puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATI20MN2** : le nombre de modifications du seuil N-k de l'extrémité vers l'origine (si seuil asymétrique), puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATITAMK** : le nombre de modifications du seuil N-k avant manœuvre, puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATITAMK2** : le nombre de modifications du seuil N-k avant manœuvre de l'extrémité vers l'origine (si seuil asymétrique), puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué.
- >**QATITAMN2** : le nombre de modifications du seuil avant manœuvre de l'extrémité vers l'origine (si seuil asymétrique), puis autant de couples (nom du quadripôle, valeur du seuil en MW) qu’indiqué. 

**Attention** : pour chaque variante, le modèle ne tient compte que d’une seule ligne pour chaque type de loi. Si plusieurs lois sont définies pour une même variante seule la dernière sera conservée.

Le numéro de la première variante est toujours $\geq 0$.

La variante « -1 » permet d’indiquer des modifications sur le cas de base qui seront valables pour toutes les variantes (à moins qu’elles ne soient modifiées par les données d’une variante)  

**Exemple** :

--------------------------------------
```
NT;2;
-1;PRODIM;1;groupe1;100; (modification commune à toutes les variantes)
0; (variante sans modification par rapport au cas de base)
1;QUADIN;1;ligne indispo;
1;PRODIM;2;groupe1;200;groupe2;300;
```
--------------------------------------

# Parades <a id="parades"></a>

Les données spécifiques à chaque parade sont décrites dans un fichier au format csv.
La première ligne indique le nombre de parades, après le mot-clé *NB*.
Chacune des lignes correspond à une parade, telle que :

*Nom de l'incident*|*Sections à surveiller séparées par un pipe*;*Nombre des premiers quadripôles listés concernés*;Liste de *Nom(s) du(es) quadripôle(s)* séparé(s) par un point virgule;

**Exemple** :

--------------------------------------
```
NB;4;
S_SO_1;1;SS1_SS1_DJ_OMN;
S_SO_1;1;SOO1_SOO1_DJ_OMN;
S_SO_1;2;SS1_SS1_DJ_OMN;SOO1_SOO1_DJ_OMN;
S_SO_1;1;S_SO_2;
```
--------------------------------------

N.B. : Les parades renseignées ne sont pas considérées si le mode de lancement est *LOAD FLOW* (i.e. mode 1).

# Données de sorties <a id="output"></a> 
Pour faciliter l’intégration et les tests de METRIX dans imaGrid, les sorties de METRIX v6 conservent le format des fichiers tabulés d’ASSESS (et de METRIS).
Plusieurs fichiers de sortie sont ou peuvent être générés à l'issue d'une simulation.

## Fichier de résultats
Il y a un seul fichier de résultats par variante nommé de la manière suivante : *\<resultsFilepath\>_s\<numéroVariante\>*. Au sein de ce fichier, les données sont regroupées par thème dans des tableaux de sortie. Chaque ligne du fichier commence par l’identifiant du tableau. 

Pour chaque champ de chaque tableau, on définit :
- le nom de la grandeur stockée ;
- le format de la donnée (cf. [formats de données](#types)) ;
- et pour une grandeur numérique (*I* ou *R*), l'unité de cette grandeur : MW pour une puissance, u.m. (unité monétaire) pour un coût, sans unité autrement.

Certains tableaux ne sont pas accessibles sauf si l'option '*--all-outputs*' est donnée lors du lancement de l'exécutable METRIX simulator.
Notons *EPSILON_SORTIES = 0.05*.

### Tableaux descriptifs de la situation <a id="detailed_tables"></a>
**Tableau S1** : ouvrages indisponibles (quadripôles et groupes) : ```S1 ;INDISPONIBILITE; OUVRAGE;```

| | | | |
| :-- | :-- | :-- | :-- |
| Type de l’ouvrage | I | | 1 : quadripôle; 2 : groupe |
| Nom de l’ouvrage | C | | |

**Note** : Tableau disponible si l'option '*--all-outputs*' est donnée lors du lancement à METRIX simulator.

**Tableau C1** (une seule ligne) : compte rendu d’exécution : ```C1 ;COMPTE RENDU;CODE;```
| | | | |
| :-- | :-- | :-- | :-- |
| Code d’erreur | I | | **0** si « OK », sinon<br>**-1** = erreur lors de la résolution<br>**1**  = si pas de solution au problème (problème infaisable, souvent cause EOD)<br>**2**  = trop de contraintes (max atteint)<br>**3** = trop de micro-itération (max atteint)<br>**4** = données de variante non cohérentes (variante ignorée) |

**Tableau C2** : liste des incidents rompant la connexité : ```C2 ;NON CONNEXITE;INCIDENT;NB NOEUDS;PROD COUPEE;CONSO COUPEE;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom de l’incident	C | | |
| Nombre de nœuds déconnectés |	I | | |
| Volume de production coupé |	R | MW | |	
| Volume de consommation coupé | R | MW | |

**Tableau C4** <a id="table_c4"></a>: liste des incidents contraignants, des incidents qui ont pu être traités en curatif ou des incidents ayant générés un transit maximal sur incident (cf. [tableau R3B](#table_r3b)) : ```C4 ;INCIDENTS;NUMERO;TYPE;OUVRAGE;```
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro d’ordre de l’incident | I | | Numérotation à partir de 1. Numéros utilisés ensuite dans tableaux [R4](#table_r4), [R1B](#table_r1b), [R2B](#table_r2b), [R3B](#table_r3b), [R5B](#table_r5b) et [R6B](#table_r6b) (cf. ci-après) |
| Type | C | | «N-1L», «N-KL», «N-KG», «N-HVDC», «COMBO» |
| Nom de l’incident | C | | Nom de l’incident |

**Tableau C5** (une seule ligne) : bilans initiaux par zone synchrone, avant équilibrage : ```C5 ;ZONE SYNC;NUM ZONE;BILAN;```
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro de zone synchrone | I | | Ex. 0 |
| Valeur du bilan initial | R | MW | Ex. -20.5 |

### Tableaux de résultats <a id="results_tables"></a>
**Note** : dans les tableaux de sorties, seules les valeurs supérieures au seuil *EPSILON_SORTIES*, si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator.

**Tableau R1** : résultats par sommet :  ```R1 ;PAR CONSO;CONSO;DEMANDE;DF HR;CDF HR;DF AR;CDF AR;``` si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator, ```R1 ;PAR CONSO;CONSO;DEMANDE;DF HR;DF AR;``` sinon.
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du sommet	C | | |
| Demande | R | MW | Somme des consommations à ce sommet |
| Défaillance HR | R | MW | Défaillance lors de l’équilibrage due à un manque de production |
| Coût défaillance HR | R | | Coût de la défaillance lors de l’équilibrage due à un manque de production (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| Défaillance AR | R | MW | Défaillance liée au réseau, en plus de l’éventuelle défaillance due au manque de production |
| Coût défaillance AR | R | | Coût de la défaillance liée au réseau, en plus de l’éventuelle défaillance due au manque de production (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |

**Note** : si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator et qu'il n'existe pas de consommation modifiable alors tout les champs, hormis le nom, valent *0*.

**Tableau R1B** <a id="table_r1b"></a>: résultats par consommation curative. Sont uniquement affichés les consommations activées en curatif sur un incident : ```R1B ;INCIDENT;CONSO;EFFACEMENT;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom de la consommation | C | | Nom de la consommation |
| Numéro d’incident | I | | Référence à la numérotation de la [table C4](#table_c4) |
| Puissance effacée | R | MW | |

**Tableau R1C** : résultats par couplage de consommations : ```R1C ;NOM REGROUPEMENT;DELTA_C;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du couplage | C | | Nom donné au couplage dans la configuration |
| Variation | R | MW | Somme des variations sur l’ensemble des consommations du couplage |

**Tableau R2** : résultats par groupe. Seuls les groupes dont la consigne préventive diffère de la consigne initiale (avant ou après ajustement selon l’option choisie) sont affichés :  ```R2 ;PAR GROUPE;GROUPE;PDISPO;DELTA_PIMP;DELTA_P_HR;DELTA_P_AR;CT HR;CT AR;CT ARP;CT GRT;CT GRTP;CT HAUSSE AR;CT BAISSE AR;CT ORDRE;CT EMPIL HR;``` si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator, ```R2 ;PAR GROUPE;GROUPE;PDISPO;DELTA_PIMP;DELTA_P_HR;DELTA_P_AR;``` sinon.
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du groupe | C | | |		
| Puissance disponible | R | MW | Pmax du groupe dans la variante |
| Ajustement de puissance imposé | R | MW | $\Delta P_{cons}$ entre la donnée *json* et celle de la variante |
| Ajustement de puissance lors de l’équilibrage initial | R | MW | $\Delta P_{cons}$ entre la donnée de la variante et la valeur après la phase d’équilibrage (si l'opton '*--all-outputs*' n'est pas donnée lors du lancement à METRIX simulator, que l'option *EQUILRES* est activée, valeur arrondie à 10-1 auquel cas; valeur non arrondie si option '*--all-outputs*') |
| Ajustement de puissance préventif | R | MW | $\Delta P_{cons}$ entre la phase d’équilibrage et la consigne préventive  (si l'opton '*--all-outputs*' n'est pas donnée lors du lancement à METRIX simulator, que l'option *REDISRES* est activée, valeur arrondie à 10-1 auquel cas; valeur non arrondie si option '*--all-outputs*') |
| Coût hors réseau | R | | Somme des coûts à la hausse et à la baisse hors réseau (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| *CT AR* | R | | = 0.0 (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| *CT ARP* | R | | = 0.0 (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| Coût du delta de puissance | R | | (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| *CT GRTP* | R | | = 0.0 (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| Coût hausse AR | R | | Coût à la hausse avec réseau (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| Coût baisse AR | R | | Coût à la baisse avec réseau (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| Coût hausse HR | R | | Coût à la hausse hors réseau (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| Coût baisse HR | R | | Coût à la baisse hors réseau (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |

**Tableau R2B** <a id="table_r2b"></a>: résultats curatifs par groupe. Seuls les groupes dont la consigne curative diffère de la consigne préventive sont affichés : ```R2B ;INCIDENT;NOM GROUPE;DELTA_P;```
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro d’ incident | I | | Référence à la numérotation de la [table C4](#table_c4) |
| Nom du groupe	| C | | |
| Puissance ajustée | R | MW | |

**Tableau R2C** : résultats par couplage de groupes : ```R2C ;NOM REGROUPEMENT;DELTA_P;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du couplage | C | | Nom donné au couplage dans la configuration |
| Variation | R | MW | Somme des variations sur l’ensemble des groupes du couplage |

**Tableau R3** : transits en N par quadripôle : ```R3 ;PAR LIGNE;LIGNE;TRANSIT N;SEUIL N;SEUIL N-k;SEUIL ITAM;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du quadripôle | C | | |		
| Transit N | R | MW | Positif de départ vers arrivée |

**Note** : si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator, les 4 seuils sont également affichés.

**Tableau R3B** <a id="table_r3b"></a>: transits maximum par quadripôle sur incident. Seuls les ouvrages surveillés ou avec résultats sont affichés : ```R3B ;PAR LIGNE;LIGNE;INCIDENT AM;MENACE MAX AM;```
| | | | |
| :-- | :-- | :-- | :-- |
| nom du quadripôle | C | | | 		
| incident cause du transit max avant manœuvres | | | Référence à la numérotation de la [table C4](#table_c4) |
| transit max avant manœuvres | R | MW | positif de départ vers arrivée |
| incident cause du 1er transit max sur  incident | | | Référence à la numérotation de la [table C4](#table_c4) |
| 1 $^{er}$ transit max sur incident | R | MW | positif de départ vers arrivée |
| *incident cause du N $^{ème}$ transit max sur  incident* | | | *Autant de résultats que demandé dans les paramètres* |
| *N $^{ème}$ transit max sur incident* | | | |

**Tableau R3C** : transits spécifique sur incident : ```R3C ;PAR LIGNE;LIGNE;INCIDENT;TRANSIT;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du quadripôle | C | | |		
| Numéro d’ incident | I | | Référence à la numérotation de la [table C4](#table_c4) |
| Transit | R | MW | Positif de départ vers arrivée |

**Tableau R4** <a id="table_r4"></a>: variations marginales par liaison, en N et sur incident : ```R4 ;VAR. MARGINALES;LIGNE;INCIDENT;VMAR;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du quadripôle | C | | |		
| Numéro d’incident | I | | 0 pour N, sinon cf. [tableau C4](#table_c4) |
| Variation marginale | R | u.m. | Impact sur la fonction objectif d’une augmentation de 1MW sur le seuil (N, N-k ou AM) de cet ouvrage |

**Tableau R4B** : variations marginales détaillées par liaison, en N et sur incident (seuls les couples (ouvrage, incident) pour lesquels les variations marginales détaillées ont été demandées et sont non nulles sont stockées dans le tableau) : ```R4B ;VAR. MARGINALES;LIGNE;INCIDENT;VMAR TYPVAR;NOMVAR;VOL;COUT;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du quadripôle | C | | |		
| Numéro d’ incident | I | | 0 pour N, sinon cf. [tableau C4](#table_c4) |
| Type de l’ouvrage ou de la transaction qui varie | C | | Sommet « N », groupe « G » ou action curative « C » |
| Nom de l’ouvrage qui varie | C | | Sommet (si variation de défaillance) ou groupe (si variation de production) |
| Volume de variation | R | MW | Coefficient de sensibilité de l’ouvrage |
| Coût de la variation | R | u.m. | Contribution de cette variable au coût marginal |

**Tableau R5** : résultats par transformateur déphaseur. Seules les liaisons dont la consigne préventive diffère de la consigne initiale sont affichées : ```R5 ;PAR TD;TD;CONSIGNE;PRISE;```
| | | | |
| :-- | :-- | :-- | :-- |
| Nom du TD	C | | |
| Consigne | R | ° ou MW | selon le mode de pilotage du TD |
| Consigne HR | R | ° ou MW | selon le mode de pilotage du TD |

**Tableau R5B** <a id="table_r5b"></a>: résultats des actions curatives des transformateurs déphaseurs. Seules les liaisons dont la consigne curative diffère de la consigne préventive sont affichées : ```R5B ;INCIDENT;NOM TD; CONSIGNE;PRISE;```
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro de l’incident | R | | Référence à la numérotation de la [table C4](#table_c4) |
| Nom du TD | C | | |
| Consigne en curatif du TD | R | ° ou MW | Selon le mode de pilotage du TD |

**Tableau R6** : résultats par ligne à courant continu. Seules les liaisons dont la consigne préventive diffère de la consigne initiale sont affichées : ```R6 ; PAR LCC;NOM;TRANSIT;VM_PREV;VM_GLOBALE;TRANSIT HR;``` si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator, ```R6 ; PAR LCC;NOM;TRANSIT;VM_GLOBALE;``` sinon.
| | | | |
| :-- | :-- | :-- | :-- |
| Nom de la ligne à courant continu | C | | |		
| Puissance transitant | R | MW | |
| Variation marginale préventive de la HVDC | R | u.m | Gain sur la fonction coût d’1 MW de plus sur la HVDC en préventif (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
| Variation marginale de la HVDC | R | u.m | Gain sur la fonction coût d’1 MW de plus sur la HVDC (il s’agit du max entre la variation marginale préventive et les variations marginales curatives) |
| Transit hor réseau | R | MW | (résultat uniquement présent si l'opton '*--all-outputs*' est donnée lors du lancement à METRIX simulator) |
 
**Tableau R6B** <a id="table_r6b"></a>: résultats par actions curatives des HVDC. Seules les liaisons dont la consigne curative diffère de la consigne préventive sont affichées : ```R6B ;INCIDENT;NOM HVDC;CONSIGNE;VM_CUR;```
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro de l’incident  | R | | Référence à la numérotation de la [table C4](#table_c4) |
| Nom de la HVDC | C | | |
| Consigne en curatif de la HVDC | R | MW | |
| Variation marginale curative de la HVDC | R | u.m | Gain sur la fonction coût d’1 MW de plus sur la HVDC pour cet incident (non utilisé) |

**Tableau R7** : redispatching par filière : ```R7 ;PAR FILIERE;TYPE;VOL BAISSE;VOL HAUSSE;VOL CUR BAISSE;VOL CUR HAUSSE;```
| | | | |
| :-- | :-- | :-- | :-- |
| Filière | C | | |
| Redispatching préventif à la baisse | R | MW | ∑ groupes de la filière |
| Redispatching préventif à la hausse | R | MW | ∑ groupes de la filière |
| Redispatching curatif à la baisse | R | MW | ∑ groupes de la filière |
| Redispatching curatif à la hausse | R | MW | ∑ groupes de la filière |

**Tableau R8** : résultats sur les pertes calculées en actif-seul : ```R8 ;PERTES;VOLUME CALCULE;TAUX UTILISE;```
| | | | |
| :-- | :-- | :-- | :-- |
| Volume de pertes calculé | R | MW | |	
| Taux de pertes | R | % | |

**Tableau R8B** : résultats sur les pertes par région : ```R8B ;PERTES;REGION;VOLUME CALCULE;```
| | | | |
| :-- | :-- | :-- | :-- |
| Région ou HVDC | R | MW | |
| Volume de pertes calculé | R | % | |

**Tableau R9** : résultats de la fonction objectif : ```R9 ;FCT OBJECTIF;COUT GROUPES;COUT DELESTAGE;VOLUME ECARTS N-k;VOLUME ECARTS N;COUT GRP CUR;COUT CONSO CUR;```
| | | | |
| :-- | :-- | :-- | :-- |
| Coût de redispatching | R | u.m. | Coût des ajustements préventifs de production |
| Coût de la défaillance | R | u.m. | Coût du délestage préventif |
| Volume de dépassement sur incident | | MW | Somme des dépassements de seuils sur incidents pour les ouvrages surveillés |
| Volume de dépassement en N | | MW | Somme des dépassement en N pour les ouvrages surveillés |
| Coût de redispatching curatif | | u.m. | Coût des ajustements curatifs de production |
| Coût d’effacement curatif | | u.m. | Coût des ajustements curatifs de consommation |

**Tableau R10** : résultats du curatif topologique (parades sélectionnées). Les parades « ne rien faire » ne sont pas affichées : ```R10;INCIDENT;NOM INCIDENT;NB ACTIONS;ACTION;```
| | | | |
| :-- | :-- | :-- | :-- |
| Numéro de l’incident | I | | Référence à la numérotation de la [table C4](#table_c4) |
| Nom incident | C | | Nom de l’incident initial |
| Nombre d’action(s) de la parade | I | | |
| Nom de la parade | C | | |

### Fichier de logs
Il y a deux fichiers de logs générés par simulation :
- *\<errorFilepath\>* : ce fichier contient les logs relatifs au niveau de logs renseigné via l'option *--log-level* sans formattage.
- *metrix\<ID_de_rotation\>.log* : ce fichier contient les logs relatifs au niveau de logs renseigné via l'option *--log-level* avec formattage (l'*\<ID_de_rotation\>* correspond au numéro d'ID du fichier de logs généré avec une certaine taille).

Il est également possible d'afficher les logs sur la sortie standard via l'option *-p [ --print-log ]*.

### Fichier LODF

Un fichier *LODF_matrix.csv* est généré si l'opion *--write-LODF* est renseignée. Ce fichier correspond à la matrice LODF (Line Outage Distribution Factor) de la dernière variante simulée.

### Fichier PTDF

Un fichier *PTDF_matrix.csv* est généré si l'opion *--write-PTDF* est renseignée. Ce fichier correspond à la matrice PTDF (Power Transfer Distribution Factor) de la dernière variante simulée.

### Fichier MPS

Le fichier MPS (*Donnees_Probleme_Solveur.mps*) du dernier problème peut être généré via l'option *--mps-file*.
