# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Défaillance sur certains noeuds lors de l'équilibrage
-----------------------------------------------------
Différentes variantes pour lesquelles la production est insuffisante et ne peux être augmentée, 
ce qui conduit à du délestage sur le seul noeud configuré lors de la phase d'équilibrage (phase 'hors réseau').
Variante 0 : Délestage de 180 MW sur FVALDI1_1
Variante 1 : Pas de solution, car pas de consommation sur FVALDI1_1
Variante 2 : Redispatching au lieu de délestage car les coûts sont inférieurs
Variante 3 : Délestage insuffisant (20 MW) complété par du redispatching
