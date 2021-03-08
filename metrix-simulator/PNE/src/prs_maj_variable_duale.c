/***********************************************************************

   FONCTION: Mise a jour des bornes d'une variable duale.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"

# include "pne_define.h"

# include "prs_define.h"
 
# define TRACES 0
										 
/*----------------------------------------------------------------------------*/

void PRS_MajVariableDuale( PRESOLVE * Presolve, int Cnt, double CBarre,
                           double Coeff, char SensInegaliteDuCoutReduit,
													 int * NbModifications )
{
double Lbd; 

if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_CONNU ) return;

Lbd = CBarre / Coeff;
if ( SensInegaliteDuCoutReduit == '=' ) {
  Presolve->Lambda[Cnt] = Lbd;
	Presolve->LambdaMin[Cnt] = Lbd;
	Presolve->LambdaMax[Cnt] = Lbd;	
  Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;
  *NbModifications = *NbModifications + 1;
}
else if ( SensInegaliteDuCoutReduit == '<' ) {
  if ( Coeff > 0.0 ) {
    if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
      if ( Lbd > Presolve->LambdaMin[Cnt] ) {
			  Presolve->LambdaMin[Cnt] = Lbd;
       	*NbModifications = *NbModifications + 1;
			}
		}
		else {
      Presolve->LambdaMin[Cnt] = Lbd;
      *NbModifications = *NbModifications + 1;
      if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU ) Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
			else Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_CONNU;			
		}
	}
	else {
    if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
      if ( Lbd < Presolve->LambdaMax[Cnt] ) {
			  Presolve->LambdaMax[Cnt] = Lbd;
        *NbModifications = *NbModifications + 1;
			}
		}
		else {
      Presolve->LambdaMax[Cnt] = Lbd;
      *NbModifications = *NbModifications + 1;
      if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU ) Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
			else Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;			
		}		
	}
}
else if ( SensInegaliteDuCoutReduit == '>' ) {
  if ( Coeff > 0.0 ) {
    if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
      if ( Lbd < Presolve->LambdaMax[Cnt] ) {
			  Presolve->LambdaMax[Cnt] = Lbd;
        *NbModifications = *NbModifications + 1;
			}
		}
		else {
      Presolve->LambdaMax[Cnt] = Lbd;
      *NbModifications = *NbModifications + 1;
      if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU ) Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
			else Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;			
		}				
	}
	else {
    if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
      if ( Lbd > Presolve->LambdaMin[Cnt] ) {
			  Presolve->LambdaMin[Cnt] = Lbd;
       *NbModifications = *NbModifications + 1;
			}
		}
		else {
      Presolve->LambdaMin[Cnt] = Lbd;
      *NbModifications = *NbModifications + 1;
      if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU ) Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
			else Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_CONNU;			
		}		
	}
}

if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
  if ( fabs( Presolve->LambdaMin[Cnt] - Presolve->LambdaMax[Cnt] ) < ECART_NUL_LAMBDAMIN_LAMBDAMAX ) {
	  # if TRACES == 1
	    printf("Cnt %d LambdaMin = LambdaMax = %e\n",Cnt,Presolve->LambdaMin[Cnt]);
		# endif
	  Presolve->Lambda[Cnt] = 0.5 * ( Presolve->LambdaMin[Cnt] + Presolve->LambdaMax[Cnt] );
		Presolve->LambdaMin[Cnt] = Presolve->Lambda[Cnt];
		Presolve->LambdaMax[Cnt] = Presolve->Lambda[Cnt];
    Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;
    *NbModifications = *NbModifications + 1;
	}
}

return;
}




