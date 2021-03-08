//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#ifndef PARAMETRES_METRIX
#define PARAMETRES_METRIX
/***************************************************************************************

Modele      : OPF en Actif Seul con�u pour �tre int�gr� dans la logique statistique d'ASSESS
Auteur      : Yacine HASSAINE
Description : parametres utilises dans toutes les fonctions du projet
COPYRIGHT RTE 2008

*****************************************************************************************/
#include <algorithm>
#include <cctype>
#include <cmath>
#include <cstdlib>
#include <functional>
#include <stdexcept>
#include <string>

/**
 * @brief Remove spaces at the end of the string
 */
inline void rtrim(std::string& s)
{
    // s.erase(find_if_not(s.rbegin(), s.rend(),std::ptr_fun<int, int>(std::isspace)).base(), s.end()); // not working
    // with utf8 chars
    auto it = std::find_if_not(s.rbegin(), s.rend(), [](char c) { return c == ' '; });
    s.erase(it.base(), s.end());
}

#endif
