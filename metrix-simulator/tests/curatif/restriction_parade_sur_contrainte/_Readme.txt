# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Restriction de l'utilisation des parades à certaines contraintes
-----------------------------------------------------------------

Suite à l'incident 'FS.BIS1 FSSV.O1 1', la ligne 'FS.BIS1 FSSV.O1 2' entre en contrainte.

La parade 'FS.BIS1 FSSV.O1 2' fait tomber cette ligne si elle est en contrainte.
La parade 'FS.BIS1  FVALDI1  1' qui est aussi efficace peut-être activée s'il y a une contrainte sur 'FS.BIS1 FSSV.O1 2' ou ''

Variante 0 : Référence, pas de contrainte 

Variante 1 : La parade 'FS.BIS1 FSSV.O1 2' est choisie 

Variante 2 : La parade 'FS.BIS1  FVALDI1  1' est choisie car la précédente crée une contrainte

Variante 3 : Aucune parade n'est choisie car les transits sont en dessous des seuils d'activation

Variante 4 : La parade 'FS.BIS1  FVALDI1  1' est choisie 

Variante 5 : Aucune parade n'est choisie car les transits sont en dessous des seuils d'activation

Variante 6 : Aucune parade n'est choisie car les transits sont en dessous des seuils d'activation

Variante 7 : Une autre parade est choisie car les contraintes sont différentes