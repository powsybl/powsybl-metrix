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

   FONCTION: Calcul du critere.
                            
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"
							     
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
# include "spx_fonctions.h"

/*----------------------------------------------------------------------------*/

void PNE_CalculerLaValeurDuCritere( PROBLEME_PNE * Pne )
{
int i ; 

/* Calcul du critere */
Pne->Critere = Pne->Z0;

for ( i = 0 ; i < Pne->NombreDeVariablesTrav ; i++ ) Pne->Critere+= Pne->LTrav[i] * Pne->UTrav[i];
 
#if VERBOSE_PNE
  printf(" Valeur du critere apres optimisation du probleme relaxe: %lf \n",Pne->Critere);
  printf(" ********************************************************     \n");
#endif

if ( Pne->NombreDeCoupesCalculees == 0 ) Pne->ValeurOptimaleDuProblemeCourantAvantNouvellesCoupes = Pne->Critere; 

return;
}

