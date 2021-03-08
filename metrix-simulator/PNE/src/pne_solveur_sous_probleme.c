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

   FONCTION:  Solveur de PLNE appele par le solveur pour resoudre un
	            sous probleme reduit.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

void PNE_SolveurProblemeReduit( PROBLEME_A_RESOUDRE * Probleme,
                                CONTROLS * Controls )
{
PROBLEME_PNE * Pne; void * Tas;

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE
  Probleme->ExistenceDUneSolution = ARRET_CAR_ERREUR_INTERNE;
  return; /* On n'utilise pas le malloc proprietaire */
  Tas = MEM_Init(); 
  Pne = (PROBLEME_PNE *) MEM_Malloc( Tas, sizeof( PROBLEME_PNE ) );
  if ( Pne == NULL ) {
    printf("Saturation memoire, impossible d'allouer l'objet PROBLEME_PNE\n");
    Probleme->ExistenceDUneSolution = ARRET_CAR_ERREUR_INTERNE;
    return;
  }				
	Pne->Tas = Tas;
# else 		
  Pne = (PROBLEME_PNE *) malloc( sizeof( PROBLEME_PNE ) );
  if ( Pne == NULL ) {
    printf("Saturation memoire, impossible d'allouer l'objet PROBLEME_PNE\n");
    Probleme->ExistenceDUneSolution = ARRET_CAR_ERREUR_INTERNE;
    return;
  }
	Tas = NULL;
	Pne->Tas = Tas;
  Pne->Controls = (CONTROLS *) malloc( sizeof( CONTROLS ) );
  if ( Pne->Controls == NULL ) {
    printf("Saturation memoire, impossible d'allouer l'objet PROBLEME_PNE\n");
    Probleme->ExistenceDUneSolution = ARRET_CAR_ERREUR_INTERNE;
    return;
  }	
# endif

Pne->AnomalieDetectee = NON_PNE ;
setjmp( Pne->Env );  
if ( Pne->AnomalieDetectee == OUI_PNE ) {
  /* Liberation du probleme */ 
  PNE_LibereProbleme( Pne );
  Probleme->ExistenceDUneSolution = ARRET_CAR_ERREUR_INTERNE;
  fflush(stdout);
  return;
} 
else {
 
  if ( Probleme->SortirLesDonneesDuProbleme == OUI_PNE ) {
    PNE_EcrireJeuDeDonneesMPS( Pne, Probleme );   
  }

  /* Optimisation */
  
  Pne->ProblemeSpxDuSolveur = NULL; 
  Pne->ProblemeBbDuSolveur  = NULL; 
  Pne->ProblemePrsDuSolveur = NULL;

  /* Initialisation du temps */
  time( &(Pne->HeureDeCalendrierDebut) );

  memcpy( (char *) Pne->Controls, (char *) Controls, sizeof( CONTROLS ) );
	
  PNE_SolveurCalculs( Probleme , Pne ); 
}

/* Pour vider le buffer des traces */
fflush(stdout);

return;
}
