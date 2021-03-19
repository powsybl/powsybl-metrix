# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test avec un incident complexe
------------------------------ 
2 incidents "FS.BIS1 FSSV.O1 1" et "FVALDI1  FTDPRA1  1" qui est défini comme incident complexe.

Variante 0 : Aucune contrainte
Variante 1 : Contrainte sur "FS.BIS1 FSSV.O1 1" suite à l'incident complexe
Variante 2 : La ligne "FVALDI1  FTDPRA1  1" est déconnectée, plus de contrainte
Variante 3 : La ligne "FS.BIS1 FSSV.O1 1" est déconnectée, plus de contrainte
Variante 4 : Contrainte sur "FS.BIS1 FSSV.O1 2" suite à l'incident simple
Variante 5 : 2 contraintes sur "FS.BIS1 FSSV.O1 2" levées par la parade et du redispatching 
Variante 6 : Contrainte sur seuil ITAM
Variante 7 : Contrainte sur seuil N
Variante 8 : Contrainte sur seuil ITAM complexe