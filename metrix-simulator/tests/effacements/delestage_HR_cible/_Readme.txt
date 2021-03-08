# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

D�faillance sur certains noeuds lors de l'�quilibrage
-----------------------------------------------------
Diff�rentes variantes pour lesquelles la production est insuffisante et ne peux �tre augment�e, 
ce qui conduit � du d�lestage sur le seul noeud configur� lors de la phase d'�quilibrage (phase 'hors r�seau').
Variante 0 : D�lestage de 180 MW sur FVALDI1_1
Variante 1 : Pas de solution, car pas de consommation sur FVALDI1_1
Variante 2 : Redispatching au lieu de d�lestage car les co�ts sont inf�rieurs
Variante 3 : D�lestage insuffisant (20 MW) compl�t� par du redispatching
