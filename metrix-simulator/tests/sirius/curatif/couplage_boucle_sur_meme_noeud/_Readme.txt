# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

La fermeture de couplage définie dans la parade crée une boucle sur une même noeud, ce qui rend la matrice singulière et pose un problème dans LU.
La correction consiste à ne pas mettre le couplage dans la matrice et donc ignorer la parade.
