# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Tests de TD en curatif avec prise en compte du seuil ITAM
--------------------------------------------------------------------------
La liaison "FS.BIS1 FSSV.O1 2" est initialement consignée.
Lors de l'incident "FS.BIS1 FSSV.O1 1" des lignes passent en contrainte.
 
Variante 0 : le seuil ITAM de toutes les liaisons est OK : pas besoin de déphaser en préventif, il faut déphaser en curatif pour respecter le seuil N-k
Variante 1 : le seuil ITAM du TD est trop bas, il faut déphaser en N pour le respecter puis déphaser de nouveau en curatif pour respecter le seuil N-k
Variante 2 : le seuil ITAM de la liaison "FSSV.O1  FP.AND1" est trop bas, il faut faire du redispatching préventif