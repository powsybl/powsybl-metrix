# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de fermeture d'une ligne en parade avec un TD pouvant agir en curatif
--------------------------------------------------------------------------
La liaison "FS.BIS1 FSSV.O1 2" est initialement consignée.
Lors de l'incident "FS.BIS1 FSSV.O1 1" des lignes passent en contrainte.
 
Variante 0 : la parade est moins chère que l'action du TD
Variante 1 : la parade ne peut s'appliquer, l'action curative du TD est suffisante pour lever les contraintes
Variante 2 : le seuil final de "FSSV.O1  FP.AND1  1" est trop bas, il faut à la fois activer la parade et agir sur le TD