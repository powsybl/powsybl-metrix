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

   FONCTION: Initialisation des inticateurs hyper creux
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"
  
/*----------------------------------------------------------------------------*/

void SPX_InitialiserLesIndicateursHyperCreux( PROBLEME_SPX * Spx )
{

Spx->CalculErBMoinsUnEnHyperCreux       = OUI_SPX;
Spx->CalculErBMoinsEnHyperCreuxPossible = OUI_SPX;
Spx->CountEchecsErBMoins                = 0;                
Spx->AvertissementsEchecsErBMoins       = 0;      
Spx->NbEchecsErBMoins                   = 0;

Spx->CalculABarreSEnHyperCreux         = OUI_SPX;
Spx->CalculABarreSEnHyperCreuxPossible = OUI_SPX;
Spx->CountEchecsABarreS                = 0;                
Spx->AvertissementsEchecsABarreS       = 0;    
Spx->NbEchecsABarreS                   = 0;

return;
}
