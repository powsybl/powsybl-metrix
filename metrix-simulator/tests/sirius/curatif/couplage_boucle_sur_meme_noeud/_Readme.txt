# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

La fermeture de couplage d�finie dans la parade cr�e une boucle sur une m�me noeud, ce qui rend la matrice singuli�re et pose un probl�me dans LU.
La correction consiste � ne pas mettre le couplage dans la matrice et donc ignorer la parade.
