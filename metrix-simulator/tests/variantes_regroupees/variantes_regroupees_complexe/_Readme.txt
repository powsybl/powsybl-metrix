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
Ce test, comme le test simple, vise vérifier que le code organise les variantes en rassemblant celles dont les QUADIN sont communs.
En revanche, contrairement au test variantes_regroupees_simple, le but de ce test n'est pas d'avoir des combinaisons de fichiers d'entrée particulières, mais plutôt de vérifier qu'il n'y a pas d'impact sur les résultats sur un cas réseau plus réaliste. De plus, il permet de vérifier que le temps de calcul chute avec la réorganisation des variantes.

