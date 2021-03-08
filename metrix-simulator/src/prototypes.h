//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#ifndef PROTOTYPE_METRIX
#define PROTOTYPE_METRIX
/***************************************************************************************

Modele      : OPF en Actif Seul con�u pour �tre int�gr� dans la logique statistique d'ASSESS
Auteur      : Yacine HASSAINE
Description : Fichier d'en-tete pour la declaration des prototypes des fonctions
         utilisees par le programme principal
COPYRIGHT RTE 2008

*****************************************************************************************/
#include "reseau.h"
#include "variante.h"

void afficherVariantesCle(
    MapQuadinVar, Quadripole::SetQuadripoleSortedByName IndispoLignes = Quadripole::SetQuadripoleSortedByName());
void afficherMapVariante(MapQuadinVar);

#endif
