# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de parade qui ouvre un couplage
----------------------------------------
Lors de l'incident "FS.BIS1 FSSV.O1 1" la liaison "FS.BIS1 FSSV.O1 2" passe en contrainte.
Variante 0, aucune contrainte 
Variante 1, l'ouverture du couplage à SSV.O répartit les flux et lève la contrainte
Variante 2, c'est l'ouverture du couplage à S.BIS qui est choisi en raison de la valeur du seuil final sur "FSSV.O1  FP.AND1  1"
Variante 3, ouverture du couplage à SSV.O en raison de la répartition initiale de la production  
Variante 4, la valeur du seuil ITAM sur "FS.BIS1 FSSV.O1 2" requiert un léger redispatching en N en plus de la parade précédente