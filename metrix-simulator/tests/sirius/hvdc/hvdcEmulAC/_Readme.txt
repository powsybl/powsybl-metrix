# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

﻿Test d'une HVDC en émulation AC 
Var  0 : RAS
Var  1 : HVDC en butée en N et N-1 (consigne trop forte)
Var  2 : HVDC en consigne inversée mais pas en butée, ouvrage reste en contrainte
Var  3 : HVDC à consigne nulle, ouvrage reste en contrainte
Var  4 : HVDC en butée en N et N-1
Var  5 : HVDC pas en butée mais ouvrage en contrainte 
Var  6 : HVDC en butée, ouvrage plus fortement en contrainte que la variante précédente
Var 7 : Test précédent mais dans le sens inverse