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

   FONCTION:   Calcul de pi = Cb * B_MOINS_1 c'est a dire              
               resolution de u B = c

   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "lu_define.h"

/*----------------------------------------------------------------------------*/
												
void SPX_ResoudreUBEgalC( PROBLEME_SPX * Spx,
                          char TypeDEntree, /* Mode de stockage du vecteur second membre */
                          double * U, /* Second membre et resultat */
                          int * IndexDesTermesNonNuls,
													int * NombreDeTermesNonNuls,
													char * TypeDeSortie, 
                          char CalculEnHyperCreux /* Vaut OUI_SPX ou NON_SPX */
												)
{ 
char SecondMembreCreux;     

if ( Spx->UtiliserLaLuUpdate == NON_SPX ) {
  SPX_AppliquerLesEtaVecteursTransposee( Spx, U, IndexDesTermesNonNuls, NombreDeTermesNonNuls,
	                                       CalculEnHyperCreux, TypeDEntree );	
}

/* Terminer par la resolution avec la derniere base factorisee */

SecondMembreCreux = NON_LU;
SPX_ResolutionDeSystemeTransposee( Spx, TypeDEntree, U, IndexDesTermesNonNuls, NombreDeTermesNonNuls,																		
                                   TypeDeSortie, CalculEnHyperCreux, SecondMembreCreux );  
																	 
return;
}
