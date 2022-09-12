# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Tests complexe de rassemblement des variantes par ensemble de QUADIN commun
--------------------------------------------------------------------------
Ce test, comme le test simple, vise v�rifier que le code organise les variantes en rassemblant celles dont les QUADIN sont communs.
En revanche, contrairement au test variantes_regroupees_simple, le but de ce test n'est pas d'avoir des combinaisons de fichiers d'entr�e particuli�res, mais plut�t de v�rifier qu'il n'y a pas d'impact sur les r�sultats sur un cas r�seau plus r�aliste. De plus, il permet de v�rifier que le temps de calcul chute avec la r�organisation des variantes.

