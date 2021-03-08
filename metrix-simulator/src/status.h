//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#pragma once

// type de sorties du probleme
constexpr int METRIX_PROBLEME = -1;           // retour des sous programmes en presence d'un probleme
constexpr int METRIX_PAS_PROBLEME = 0;        // retour des sous programmes en absense de probleme
constexpr int METRIX_PAS_SOLUTION = 1;        // retour des sous programmes n ayant pas de solutions
constexpr int METRIX_NB_MAX_CONT_ATTEINT = 2; // retour des sous programmes nb max de contraintes ajoutee atteint
constexpr int METRIX_NB_MICROIT = 3;          // retour des sous programmes nb max de micro iteration atteint
constexpr int METRIX_VARIANTE_IGNOREE = 4;    // retour des sous programmes demandant d ignorer la variante

constexpr int METRIX_CONTRAINTE_IGNOREE = 3;
