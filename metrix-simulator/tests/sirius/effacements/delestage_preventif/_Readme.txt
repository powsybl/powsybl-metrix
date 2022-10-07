# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Défaillance sur tous les noeuds en préventif
---------------------------------------------
Différentes variantes pour lesquelles un délestage préventif est nécessaire pour respecter les seuils en N.
Variante 0 : Délestage préventif à FVALDI1 pour respecter la contrainte de transit en N sur FS.BIS1 FSSV.O1 2
Variante 1 : Délestage préventif à FSSV.O1 pour respecter la contrainte de transit en N sur FS.BIS1 FSSV.O1 2
Variante 2 : Délestage total à FVALDI1 pour respecter une contrainte de transit à 0 (le cout de redispatching à la hausse est trop élevé)
Variante 3 : Pas de délestage car les coûts de redispatching préventifs sont plus intéressants

