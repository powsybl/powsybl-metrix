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

   FONCTION: Calcul plus grand et plus petit terme de la matrice.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_fonctions.h"  
# include "pne_define.h"

# include "prs_define.h"

/*----------------------------------------------------------------------------*/

void PNE_CalculPlusGrandEtPlusPetitTerme( PROBLEME_PNE * Pne )
{
double Amn; double Amx; double * A; int il; int ilMax; int Cnt; double a; int VarAmn;

/* Calcul du plus grand et du plus petit terme (different de zero) */
Amn =  LINFINI_PNE;
Amx = -LINFINI_PNE;
A = Pne->ATrav;

VarAmn = -1;

for ( Cnt = 0 ; Cnt < Pne->NombreDeContraintesTrav ; Cnt++ ) {
  il    = Pne->MdebTrav[Cnt];
  ilMax = il + Pne->NbTermTrav[Cnt];
  while ( il < ilMax ) {
    a = fabs ( Pne->ATrav[il] );		
    if ( a > 0. /*ZERO_PRESOLVE*/ ) {
      if ( a < Amn ) { Amn = a; VarAmn = Pne->NuvarTrav[il]; }
      else if ( a > Amx ) Amx = a; 
    }
    il++;
  }
}

Pne->PlusGrandTerme = Amx;  
Pne->PlusPetitTerme = Amn;

/*
printf("PNE_CalculPlusGrandEtPlusPetitTerme    Pne->PlusPetitTerme %e ",Pne->PlusPetitTerme);
if ( Pne->TypeDeVariableTrav[VarAmn] == ENTIER ) printf("Plus petit coeff sur une variable ENTIERE %d\n",VarAmn);
else printf("Plus petit coeff sur une variable CONTINUE %d\n",VarAmn);
*/

return;
}
