# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Redispatching curatif avec des coûts négatifs et un offset de coûts de redispatching
------------------------------------------------------------------------------------

Même réseau que curatif_groupes, avec FSSV.O11_G également autorisé à agir en curatif, et REDISPOF = 100.
Sur l'incident "FS.BIS1 FSSV.O1 1", la ligne "FS.BIS1 FSSV.O1 2" est en contrainte : FVALDI11_G est monté en curatif
et FSSV.O11_G et/ou FSSV.O12_G (même noeud, même efficacité) sont baissés en curatif.

L'offset de coûts de redispatching est ajouté au coût avant application de la borne inférieure NULLCOST (0.5 par défaut) :
un coût à la baisse de -100 donne max(-100 + 100, 0.5) = 0.5 et un coût à la baisse de -90 donne max(-90 + 100, 0.5) = 10,
l'ordre de mérite entre coûts négatifs est donc conservé en curatif.

Variante 0 : FSSV.O12_G (-100) est moins cher que FSSV.O11_G (-90), FSSV.O12_G est baissé en curatif
Variante 1 : les coûts sont échangés, FSSV.O11_G (-100) est baissé en curatif
Variante 2 : FSSV.O12_G (-100) ne produit que 100 MW, il est baissé de 100 MW et FSSV.O11_G (-90) complète en curatif
