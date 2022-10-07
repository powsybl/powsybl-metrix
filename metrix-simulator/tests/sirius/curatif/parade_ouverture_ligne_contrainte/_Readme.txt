# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de parade qui ouvre la ligne en contrainte
----------------------------------------
Lors de l'incident "FS.BIS1 FSSV.O1 1" la liaison "FS.BIS1 FSSV.O1 2" passe en contrainte.
Dans la variante 0, aucune contrainte
Dans la variante 1, il suffit de la laisser partir car les autres liaisons peuvent supporter le report de charge.
Dans la variante 2, la parade précédente ne peut s'appliquer en raison du seuil ITAM sur "FS.BIS1 FSSV.O1 2"
Dans la variante 3, la parade précédente ne peut s'appliquer en raison du seuil final sur "FP.AND1  FTDPRA1  1"