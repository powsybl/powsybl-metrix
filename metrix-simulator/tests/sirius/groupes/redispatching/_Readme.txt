# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Variante 0 : les 2 groupes "CORD56CORD55" et "G.RIV.TG1" permettent de lever la contrainte avec un ajustement de 21 MW
Variante 1 : le groupe "G.RIV.TG1" est trop cher, c'est le groupe "SLACK_GROUPE" qui est utilisé, mais comme il est inefficace sur la contrainte, l'ajustement passe à 507 MW
Variante 2 : les groupes "G.RIV.TG1" et "SLACK_GROUPE" sont trop chers, c'est un délestage à "SSNAZP1_1" qui est le plus efficace 
Variante 3 : le groupe "G.RIV.TG1" est utilisé pour lever la contrainte et le groupe "SLACK_GROUPE" est utilisé pour compenser (en partie) 