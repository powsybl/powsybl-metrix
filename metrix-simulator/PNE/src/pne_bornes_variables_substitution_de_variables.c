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

   FONCTION: Prise en compte des substitutions de variables dans les
	           contraintes de bornes variables.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"


# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif
  
/*----------------------------------------------------------------------------*/

void PNE_BornesVariableSubstituerUneVariable( PROBLEME_PNE * Pne, int VarSubstituee,
                                              int VarRestante, double BTilde )
{
int Cnt; int * First; int * Colonne; double * Coefficient; double * SecondMembre;
int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable; int ilVarBin; 
int NombreDeContraintesDeBorne;

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;	
NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;

/* La variable continue est toujours placee en premier */
for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;	
	ilVarBin =  First[Cnt] + 1;	
  Varbin = Colonne[ilVarBin];
	if ( Varbin != VarSubstituee ) continue;	
	if ( BTilde == 0 ) {
		/* Cas de la substitution x = y */
	  Colonne[ilVarBin] = VarRestante;
	}
	else {
	  /* Cas de la substitution x = 1-y */
	  Colonne[ilVarBin] = VarRestante;		
	  SecondMembre[Cnt] -= Coefficient[ilVarBin];		
    Coefficient[ilVarBin] *= -1;
	}			
}
			
return;
}

/*----------------------------------------------------------------------------*/


