//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

/***********************************************************************

   FONCTION: 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

/*----------------------------------------------------------------------------*/

void PNE_RecupererLeProblemeInitial( PROBLEME_PNE * Pne ) 
{

Pne->Coupes.NombreDeContraintes = 0;
Pne->NombreDeCoupesCalculees = 0;

Pne->ResolutionDuNoeudReussie = OUI_PNE;

return;
}

