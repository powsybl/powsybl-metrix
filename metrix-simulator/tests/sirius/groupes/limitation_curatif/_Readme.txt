# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Tests d'effacements curatifs
----------------------------
2 charges sur le noeud FVALDI sont configurées pour fonctionner en curatif sur l'incident FS.BIS1 FSSV.O1 1.
La charge FVALDI11_L peut effacer 10% de sa consommation 
La charge FVALDI11_L2 peut effacer 20% de sa consommation 
Lors de la perte de FS.BIS1 FSSV.O1 1, la liaison FS.BIS1 FSSV.O1 2 passe en contrainte.

Lorsque la limite de FS.BIS1 FSSV.O1 2 est de 440, il faut délester 120 MW au total pour respecter la contrainte
Lorsque la limite de FS.BIS1 FSSV.O1 2 est de 400, il faut délester 200 MW au total pour respecter la contrainte

La limite curative pour l'ensemble des groupes est fixée à 100 MW

Variante 0 : Délestage curatif de 100 MW (réparti sur les 2 charges) dû à la limite curative et effacement préventif de 20 MW 

Variante 1 : Délestage curatif de 100 MW sur FVALDI11_L2 uniquement et effacement préventif de 20 MW 

Variante 2 : Délestage préventif de 120 MW pour respecter le seuil ITAM, puis curatif de 80 MW pour respecter le seuil N-1

Variante 3 : Redispatching curatif de 100 MW (limite) et préventif de 20 MW car pas de conso curative sur le poste FSSV.O1_2

Variante 4 : Délestage préventif de 120 MW car pas de groupe curatif disponible pour compenser l'effacement
