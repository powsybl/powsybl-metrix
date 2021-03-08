# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test d'action conjointe d'un groupe en curatif et d'une parade
--------------------------------------
Suite au d�faut "FS.BIS1 FSSV.O1 1", la ligne "FS.BIS1 FSSV.O1 2" passe en contrainte
Variante 0 : la parade est suffisante pour lever la contrainte
Variante 1 : le seuil sur "FS.BIS1 FSSV.O1 2" a �t� abaiss�, redispatching curatif n�cessaire � FSSV.O1
Variante 2 : le groupe FSSV.O12_G ne peut pas baisser, le redispatching se fait � FVERGE11
Variante 3 : la marge de redispatching sur FSSV.O1 n'est pas suffisante et le nombre d'actions curative est limit�e � 2 : redispatching � FVERGE1
Variante 4 : la marge de redispatching est insuffisante � FSSV.O et FVERGE1, redispatching pr�ventif n�cessaire
Variante 5 : redispatching curatif impossible � FSSV.O et FVERGE1, redispatching pr�ventif total
Variante 6 : redispatching pr�ventif en raison de l'abaissement du seuil ITAM sur "FS.BIS1 FSSV.O1 2"
