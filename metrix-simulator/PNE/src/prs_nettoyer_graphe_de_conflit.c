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

   FONCTION: Nettoyage du graphe de conflits quand une variable entiere
	           est remplacee par une autre.
                 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_define.h"  
# include "pne_fonctions.h"  
		
# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "prs_memoire.h"
# endif

/*----------------------------------------------------------------------------*/

void PRS_NettoyerLeGrapheDeConflits( PRESOLVE * Presolve, int Var )
{
int * Adjacent; int * Next; int * First; int Pivot; int Complement; int Noeud;
PROBLEME_PNE * Pne; 

return;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

if ( Pne->ConflictGraph == NULL ) return;

printf("PRS_NettoyerLeGrapheDeConflits \n");

Adjacent = Pne->ConflictGraph->Adjacent;
Next = Pne->ConflictGraph->Next; 				
First = Pne->ConflictGraph->First;
Pivot = Pne->ConflictGraph->Pivot;

/* On elimine l'entree du noeud dans le graphe */
Noeud = Var;
PNE_ConflictGraphSupprimerUnNoeud( Noeud, First, Adjacent, Next );
   
/* On elimine l'entree du complement */
Complement = Pivot + Noeud;
PNE_ConflictGraphSupprimerUnNoeud( Complement, First, Adjacent, Next );

return;
}


