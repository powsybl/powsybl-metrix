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

La variante de base (-1) le repositionne sur la prise 0 (-9.167°). Comme pour
un fichier produit par le mapping, un DTVALDEP en variante -1 n'est jamais
redéfini dans les variantes numérotées.

Variante 0 : pas de DTVALDEP -> part de la prise 0. Le TD remonte jusqu'à
             -0.53° (prise la plus proche : 1), butée par le seuil N-k de
             900 MW sur son propre quadripôle, atteint exactement (-900.0).

Avant correction, Reseau::updateBase n'écrivait que puiConsBase_ : le TD
restait sur la prise 16 du fichier réseau (R5 : consigne 0.00, prise 16) et le
DTVALDEP de la variante de base était ignoré pour toutes les variantes.
