# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de fermeture d'une ligne en parade.
La liaison "FS.BIS1 FSSV.O1 2" est initialement consignée.
Lors de l'incident "FS.BIS1 FSSV.O1 1" des lignes passent en contrainte.
Une des parades proposées consiste à refermer "FS.BIS1 FSSV.O1 2". C'est celle qui doit être choisie. 

Variante 0 : le seuil final sur l'ouvrage "FS.BIS1 FSSV.O1 2" n'est pas suffisant.
Variante 1 : tout est Ok.
Variante 2 : le seuil ITAM du TD est limitant