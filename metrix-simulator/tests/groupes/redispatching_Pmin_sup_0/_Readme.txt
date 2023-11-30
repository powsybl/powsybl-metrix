# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Teste si le solver respecte la règle du démarrage des groupes à Pmin>0 en redispatching : un groupe avec P<Pmin et Pmin>0 ne peut pas démarrer en redispacthing. 
Variante 1 : Sans la règle en question, il utiliserait FVERGE11_G et FSSV.O12_G. Mais FVERGE11_G ayant une production inférieure à Pmin>0, alors il ne peut démarrer : Metrix doit délester de la prod (cher) et utiliser FSSV.O12_G (cher).
Variante 2 : Cette fois, Pas de contraintes à l'utilisation de FVERGE11_G.
