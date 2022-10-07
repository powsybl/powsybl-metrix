# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test d'ouverture du DJ d'un groupe et d'une conso
---------------------------------------------
Lors de l'incident "FS.BIS1 FSSV.O1 1" la ligne "FS.BIS1 FSSV.O1 2" passe en contrainte.

Une parade consiste à ouvrir le DJ du groupe "FS.BIS11_G", l'autre à ouvrir la consommation à VALDI. on a donc des couts d'end et d'ene sur les variantes 1 et 2. 
Ici, on teste l'ajout d'une probabilite sur l'incident "FS.BIS1 FSSV.O1 1" égale 0.002 sur la variante -1, 0.008 sur la variante 1, et de 0.007 sur la variante 2. Cela impacte la probabilite des parades d'ouverture du groupe et d'ouverture de la conso, et donc les coûts finaux d'END et ENE de chaque variante. Pour la variante 0, les grandeurs ne varient pas bien que la probabilite de l'incident ait changé, car il n'y a pas de contrainte.

Variante 0 : référence RAS.
Variante 1 : parade ouverture du groupe.
Variante 2 : parade ouverture de la conso.
