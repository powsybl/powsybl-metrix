# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de DTVALDEP en variante de base sur un TD hors service
------------------------------------------------------------
Réseau identique à "variante_base_prise_td", mais le TD "FP.AND1  FTDPRA1  1"
est déclaré hors service (DTMODREG = 0).

La variante de base (-1) demande de le repositionner sur la prise 0 (-9.167°).
Cette demande doit être ignorée, avec l'avertissement "Le TD ... est hors
service", exactement comme sur le chemin variante (Reseau::modifReseau).

Sans la garde, la consigne serait appliquée et un TD hors service injecterait
un déphasage réel dans le réseau : le transit de "FP.AND1  FTDPRA1  1" passerait
de -96.0 à -898.3 MW.
