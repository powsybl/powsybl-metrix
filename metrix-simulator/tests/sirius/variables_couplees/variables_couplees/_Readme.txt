# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

﻿Variables couplées
------------------
Les générateurs FSSV.O11_G et FSSV.O12_G sont couplés par le Pobj
Les générateurs FVALDI11_G et FVERGE11_G sont couplés par le Pmax
Les charges FVALDI11_L et FVALDI11_L2 (du noeud FVALDI3) sont couplées.

On joue l'incident "FS.BIS1 FSSV.O1 1" qui fait entrer en contrainte "FS.BIS1 FSSV.O1 2"

Variante 0 : Pas de contrainte
Variante 1 : Les groupes à SSV.O sont baissé de 110 (2/3 sur FSSV.O11_G et 1/3 sur FSSV.O12_G), les 2 autres groupes sont montés de 110 (4/5 sur FVALDI11_G et 1/5 sur FVERGE11_G)  
Variante 2 : Erreur car FVALDI11_G a une Pmax nulle
Variante 3 : Baisse de 200 à SSV.O (2/3 sur FSSV.O11_G et 1/3 sur FSSV.O12_G), augmentation de 199.9 sur FVERGE11_G (car la Pmax de FVALDI11_G est 0.1)  
Variante 4 : Baisse de 110 à SSV.O (1/2 sur FSSV.O11_G et 1/2 sur FSSV.O12_G), augmentation de 110 sur les 2 autres (4/5 sur FVALDI11_G et 1/5 sur FVERGE11_G) 
Variante 5 : Erreur car FSSV.O12_G a une Pobj nulle
Variante 6 : Pas de solution car pas assez de marge à la baisse sur FSSV.O12_G
Variante 7 : Délestage à VALDI car pas assez de marge à la hausse sur FVALDI11_G et FVERGE11_G
