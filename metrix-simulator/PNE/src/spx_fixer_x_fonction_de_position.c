/***********************************************************************

   FONCTION: On calcule les valeurs de X en fonction de la position 
             hors base ou en base.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

/*----------------------------------------------------------------------------*/

void SPX_FixerXEnFonctionDeSaPosition( PROBLEME_SPX * Spx )
{
int Var; char PositionDeLaVariable; double X; double * ArrayX; char * TypeDeVariable; 
double SommeDesInfaisabilitesPrimales; double * ArrayXmax; double * BBarre;
char * ArrayPositionDeLaVariable; int * ContrainteDeLaVariableEnBase;
char * StatutBorneSupCourante;

ArrayX         = Spx->X;
ArrayXmax      = Spx->Xmax;
BBarre         = Spx->BBarre;
TypeDeVariable = Spx->TypeDeVariable;
ArrayPositionDeLaVariable    = Spx->PositionDeLaVariable;
ContrainteDeLaVariableEnBase = Spx->ContrainteDeLaVariableEnBase;
StatutBorneSupCourante       = Spx->StatutBorneSupCourante;
SommeDesInfaisabilitesPrimales = 0.;

goto AutreMethode;

for ( Var = 0 ; Var < Spx->NombreDeVariables ;  Var++ ) {
  PositionDeLaVariable = ArrayPositionDeLaVariable[Var];	
  if ( PositionDeLaVariable == EN_BASE_LIBRE ) { 
    /* La variable est donc en base */
    X = BBarre[ContrainteDeLaVariableEnBase[Var]]; 
    ArrayX[Var] = X; 
    /* Attention ici on utilise le fait que par translation on a toujours Xmin = 0. */
    if ( X < 0. && TypeDeVariable[Var] != NON_BORNEE ) SommeDesInfaisabilitesPrimales += -X;
    else {
      if ( X > ArrayXmax[Var] ) SommeDesInfaisabilitesPrimales += X - ArrayXmax[Var];			
    }
  }
  else if ( PositionDeLaVariable == HORS_BASE_SUR_BORNE_SUP ) ArrayX[Var] = ArrayXmax[Var];
	else {
	  /* La variable est HORS_BASE_A_ZERO ou HORS_BASE_SUR_BORNE_INF */
	  ArrayX[Var] = 0.0;
    if ( PositionDeLaVariable == HORS_BASE_SUR_BORNE_INF ) {
		  if ( StatutBorneSupCourante[Var] == BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE ) {
	      /* La variable est hors base sur borne inf mais qu'elle a une borne avec un StatutBorneSupCourante egal a
				   BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE alors elle est a -Xmax[Var] */
	      ArrayX[Var] = -ArrayXmax[Var];				
	    }
			/*else ArrayX[Var] = 0.0;*/
		}
	  /*ArrayX[Var] = 0.0;*/
	}
}

Spx->SommeDesInfaisabilitesPrimales = SommeDesInfaisabilitesPrimales;

return;

AutreMethode:

if ( Spx->NombreDeBornesAuxiliairesUtilisees == 0 ) {
  for ( Var = 0 ; Var < Spx->NombreDeVariables ;  Var++ ) {
    PositionDeLaVariable = ArrayPositionDeLaVariable[Var];	
    if ( PositionDeLaVariable == EN_BASE_LIBRE ) { 
      /* La variable est donc en base */
      X = BBarre[ContrainteDeLaVariableEnBase[Var]]; 
      ArrayX[Var] = X;			
      /* Attention ici on utilise le fait que par translation on a toujours Xmin = 0. */
      if ( X < 0. && TypeDeVariable[Var] != NON_BORNEE ) SommeDesInfaisabilitesPrimales += -X;
      else {
        if ( X > ArrayXmax[Var] ) SommeDesInfaisabilitesPrimales += X - ArrayXmax[Var];			
      }
    }
    else if ( PositionDeLaVariable == HORS_BASE_SUR_BORNE_SUP ) ArrayX[Var] = ArrayXmax[Var];
	  else ArrayX[Var] = 0.0;		
  }
}
else {
  for ( Var = 0 ; Var < Spx->NombreDeVariables ;  Var++ ) {
    PositionDeLaVariable = ArrayPositionDeLaVariable[Var];	
    if ( PositionDeLaVariable == EN_BASE_LIBRE ) { 
      /* La variable est donc en base */
      X = BBarre[ContrainteDeLaVariableEnBase[Var]]; 
      ArrayX[Var] = X; 
      /* Attention ici on utilise le fait que par translation on a toujours Xmin = 0. */
      if ( X < 0. && TypeDeVariable[Var] != NON_BORNEE ) SommeDesInfaisabilitesPrimales += -X;
      else {
        if ( X > ArrayXmax[Var] ) SommeDesInfaisabilitesPrimales += X - ArrayXmax[Var];			
      }
    }
    else if ( PositionDeLaVariable == HORS_BASE_SUR_BORNE_SUP ) ArrayX[Var] = ArrayXmax[Var];
	  else {
	    /* La variable est HORS_BASE_A_ZERO ou HORS_BASE_SUR_BORNE_INF */
	    ArrayX[Var] = 0.0;
      if ( PositionDeLaVariable == HORS_BASE_SUR_BORNE_INF ) {
		    if ( StatutBorneSupCourante[Var] == BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE ) {
	        /* La variable est hors base sur borne inf mais qu'elle a une borne avec un StatutBorneSupCourante egal a
				     BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE alors elle est a -Xmax[Var] */
	        ArrayX[Var] = -ArrayXmax[Var];				
	      }
		  }
	  }
  }
}

Spx->SommeDesInfaisabilitesPrimales = SommeDesInfaisabilitesPrimales;
	
return;
}

