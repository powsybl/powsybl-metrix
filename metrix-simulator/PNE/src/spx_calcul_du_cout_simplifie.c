/***********************************************************************

   FONCTION: Calcul simplifie du cout: on ne fixe pas toutes les
	           variable en fonction de le position pour calculer le cout
						 mais seulement celles dont le cout est non nul.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "bb_define.h"		        
# include "bb_fonctions.h"

# include "pne_define.h"		        

/*----------------------------------------------------------------------------*/

void SPX_CalculDuCoutSimplifie( PROBLEME_SPX * Spx )
{   
int * NumeroDesVariablesACoutNonNul; char Position; int * ContrainteDeLaVariableEnBase;
double * Xmax; double * BBarre; int Var; double Cout; char * PositionDeLaVariable; 
double * C; int i; char * StatutBorneSupCourante; PROBLEME_PNE * Pne; 

NumeroDesVariablesACoutNonNul = Spx->NumeroDesVariablesACoutNonNul;
StatutBorneSupCourante = Spx->StatutBorneSupCourante;
C = Spx->C;			
ContrainteDeLaVariableEnBase = Spx->ContrainteDeLaVariableEnBase;
BBarre = Spx->BBarre;
PositionDeLaVariable = Spx->PositionDeLaVariable;
Xmax = Spx->Xmax;
Cout = 0.0;
if ( Spx->NombreDeBornesAuxiliairesUtilisees == 0 ) { 
  for ( i = 0 ; i < Spx->NombreDeVariablesACoutNonNul ; i++ ) {
    Var = NumeroDesVariablesACoutNonNul[i];
    Position = PositionDeLaVariable[Var];	
    if ( Position == EN_BASE_LIBRE ) { 
      /* La variable est en base */
		  Cout += C[Var] * BBarre[ContrainteDeLaVariableEnBase[Var]];						
    }     
    else if ( Position == HORS_BASE_SUR_BORNE_SUP ) {
		  Cout += C[Var] * Xmax[Var];
    }
  }
}
else {
  for ( i = 0 ; i < Spx->NombreDeVariablesACoutNonNul ; i++ ) {
	  /* Remarque il se peut qu'a cause de la degenerescence on ait bruite des couts */
    Var = NumeroDesVariablesACoutNonNul[i];
    Position = PositionDeLaVariable[Var];	
    if ( Position == EN_BASE_LIBRE ) { 
      /* La variable est en base */
		  Cout += C[Var] * BBarre[ContrainteDeLaVariableEnBase[Var]];						
		}       
    else if ( Position == HORS_BASE_SUR_BORNE_SUP ) {
	    Cout += C[Var] * Xmax[Var];
	  }
	  else {
	    /* La variable est HORS_BASE_A_ZERO ou HORS_BASE_SUR_BORNE_INF */
      if ( Position == HORS_BASE_SUR_BORNE_INF ) {
		    if ( StatutBorneSupCourante[Var] == BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE ) {
	        /* La variable est hors base sur borne inf mais qu'elle a une borne avec un StatutBorneSupCourante egal a
				     BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE alors elle est a -Xmax[Var] */
			    Cout -= C[Var] * Xmax[Var];
				}
	    }
	  }
  }
}
Cout/= Spx->ScaleLigneDesCouts;
Cout+= Spx->PartieFixeDuCout;
Spx->Cout = Cout;

Pne = (PROBLEME_PNE *) Spx->ProblemePneDeSpx;
if ( Pne != NULL ) {
  Spx->Cout += Pne->Z0;	
}

return;

}


