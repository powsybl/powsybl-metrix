# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Tests de limitation en préventif d'un TD
----------------------------------------

Dans ce test, 3 TD sont configurés: 
CHATEY761 (25 prises, prise initiale 0, prise courante 9) : limite préventive sup à +1 prise
INNERY761 (31 prises, prise initiale 0, prise courante 15) : limite préventive inf à -4 prises
ARGI2L61ARGIA (33 prises, prise initiale 0, prise courante 16) : limite préventive sup à +5 prises, limite préventive inf à -6 prises

Variante 0 : les TDs sont sur leur prise courante
Variante 1 : CHATEY761 sur prise 24, INNERY761 sur prise 30 et ARGI2L61ARGIA sur prise 30
Variante 2 : CHATEY761 sur prise 0, INNERY761 sur prise 3 et ARGI2L61ARGIA sur prise 4
Variante 3 : CHATEY761 sur prise -1, INNERY761 sur prise -1 et ARGI2L61ARGIA sur prise -1 -> hors limites
Variante 4 : CHATEY761 sur prise 25, INNERY761 sur prise 31 et ARGI2L61ARGIA sur prise 33 -> hors limites
