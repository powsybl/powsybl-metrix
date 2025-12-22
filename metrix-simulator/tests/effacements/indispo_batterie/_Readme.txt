# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test d'indisponibilité de batteries
------------------------------------
  - 1 conso : Conso_Est
  - 1 prod 'classique' : Prod_Ouest (Pmin=Pmax=Pconsigne=100MW)
  - 1 prod 'batterie' : Prod_Ouest_Batterie (Pconsigne=0MW, Pmax=50MW, Pmin=-100MW)

Variantes :
  V0 : Prod_Ouest_Batterie rendue indisponible (Pmax=Pmin=Pconsigne=0)

Objectif :
  Vérifier que la section S1 des résultats affiche correctement le type 3 pour les batteries.
