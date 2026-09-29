# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de prise initiale de TD imposée par la variante de base
------------------------------------------------------------
Le TD "FP.AND1  FTDPRA1  1" est sur la prise 16 dans le fichier réseau
(DTVALDEP = 0.0 = DTTAPDEP[16]). Il est configuré pour bouger de +3 prises
et -2 prises en préventif (DTUPPRAN = 3, DTLOWRAN = 2).

La variante de base (-1) le repositionne sur la prise 0 (-9.167°). Cette prise
doit servir de point de départ à toutes les variantes qui ne la redéfinissent
pas, et de valeur de retour après une variante qui la redéfinit.

Variante 0 : pas de DTVALDEP -> part de la prise 0. Le TD remonte jusqu'à
             -0.53° (prise la plus proche : 1), butée par le seuil N-k de
             900 MW sur son propre quadripôle, atteint exactement (-900.0).
Variante 1 : DTVALDEP = 20 -> part de la prise 20 et ne bouge pas (aucune
             contrainte active à cette position).
Variante 2 : identique à la variante 0 -> doit redonner exactement le même
             résultat.

Les variantes 0 et 2 sont identiques : elles encadrent la variante 1 pour
vérifier que le retour à la valeur de base est indépendant de l'ordre de
traitement. Avant correction, seule la variante 0 était fausse (elle partait
de la prise 16 du fichier réseau) : les variantes suivant un DTVALDEP étaient
correctes par effet du reset, d'où un comportement dépendant de l'ordre.

Le test utilise metrix_test_all_outputs : en variante 1 le TD ne bouge pas et
la ligne R5 serait filtrée en sortie standard, alors que c'est précisément
l'information testée.
