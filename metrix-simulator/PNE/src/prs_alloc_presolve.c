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

   FONCTION: Presolve, allocs .
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_fonctions.h"  
# include "pne_define.h"

# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "prs_memoire.h"
# endif

/*----------------------------------------------------------------------------*/

void PRS_AllocPresolve( void * PneE )
{		      
PRESOLVE * Presolve; PROBLEME_PNE * Pne;

Pne = (PROBLEME_PNE *) PneE;

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

if ( Pne->ProblemePrsDuSolveur != NULL ) {
  /* Le Presolve est deja alloue */
	return;
}

Presolve = (PRESOLVE *) malloc( sizeof( PRESOLVE ) );
if ( Presolve == NULL ) {
  printf(" Solveur PNE , memoire insuffisante. Sous-programme: PRS_AllocPresolve \n");
  Pne->AnomalieDetectee = OUI_PNE;
  longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
}		
Presolve->Tas = NULL;

Pne->ProblemePrsDuSolveur = (void *) Presolve;  
Presolve->ProblemePneDuPresolve = PneE;

PRS_AllocationsStructure( Presolve );

return;
}

/*----------------------------------------------------------------------------*/

void PRS_FreePresolve( void * PneE )
{		      
PRESOLVE * Presolve; PROBLEME_PNE * Pne;

Pne = (PROBLEME_PNE *) PneE;

Presolve = (PRESOLVE *) Pne->ProblemePrsDuSolveur;

if ( Presolve == NULL ) {
  printf("PRS_FreePresolve: demande de liberation d'une structure presolve qui n'a pas ete allouee\n");
  return;
}		

PRS_LiberationStructure( Presolve );

return;
}

/*----------------------------------------------------------------------------*/
