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

   FONCTION:   Calcul de Pi = c_B * B^{-1} c'est a dire              
               resolution de u B = c

   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "lu_define.h"

/*----------------------------------------------------------------------------*/

void SPX_CalculerPi( PROBLEME_SPX * Spx )
{
int Cnt; double * Pi; double * C; int * VariableEnBaseDeLaContrainte;
char TypeDEntree; char TypeDeSortie; char CalculEnHyperCreux; 

Pi = Spx->Pi;
C  = Spx->C;
VariableEnBaseDeLaContrainte = Spx->VariableEnBaseDeLaContrainte;

/* Boucle sur les variables en base */
for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
  Pi[Cnt] = C[VariableEnBaseDeLaContrainte[Cnt]];
}

TypeDEntree  = VECTEUR_LU;
TypeDeSortie = VECTEUR_LU;
CalculEnHyperCreux = NON_SPX;
SPX_ResoudreUBEgalC( Spx, TypeDEntree, Pi, NULL, NULL, NULL, CalculEnHyperCreux  );

return;  
}

/*----------------------------------------------------------------------------*/
/*                        Mise a jour de Pi                                   */
							       
void SPX_MettreAJourPi( /* PROBLEME_SPX * Spx */ )
{
/* A faire dans le futur car cela n'a pas grand interet */

return;
}
