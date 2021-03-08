/***********************************************************************

   FONCTION: Mise a jour des bornes d'une variable duale a chaque fois qu'on
	           calcule une nouvelle borne.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_fonctions.h"  
# include "pne_define.h"

# define TRACES_BORNES_IDENTIQUES 0  
# define TRACES 0

/*----------------------------------------------------------------------------*/
/* Il se peut qu'en ameliorant les bornes, la variable en soit fixee */
void PRS_VariablesDualesDeBornesIdentiques( PRESOLVE * Presolve, int Cnt,																		
																		  double BorneInf, double BorneSup,
                                      char * BorneAmelioree )  
{
*BorneAmelioree = NON_PNE;
if ( Presolve->ConnaissanceDeLambda[Cnt] == LAMBDA_CONNU ) return;
/* Il se peut qu'en ameliorant les bornes, la variable en soit fixee */
if ( fabs( BorneSup - BorneInf ) > ECART_NUL_LAMBDAMIN_LAMBDAMAX ) return;
# if TRACES_BORNES_IDENTIQUES == 1
  printf("fixation de la variable duale de la contrainte %d car bornes identiques BorneInf %e BorneSup %e\n",
          Cnt,BorneInf,BorneSup);
# endif			
Presolve->Lambda[Cnt] = 0.5 * ( BorneInf + BorneSup );
Presolve->LambdaMin[Cnt] = Presolve->Lambda[Cnt];
Presolve->LambdaMax[Cnt] = Presolve->Lambda[Cnt];
Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;
*BorneAmelioree = OUI_PNE;				
return;
}

/*----------------------------------------------------------------------------*/

void PRS_MettreAJourLesBornesDUneVariableDuale( PRESOLVE * Presolve, int Cnt,
																		  char BorneInfCalculee, double BorneInf, 
																		  char BorneSupCalculee, double BorneSup, 
                                      char * BorneAmelioree )   
{
double * LambdaMin; double * LambdaMax; double * Lambda; char * ConnaissanceDeLambda;

*BorneAmelioree = NON_PNE;
LambdaMin = Presolve->LambdaMin;
LambdaMax = Presolve->LambdaMax;
Lambda = Presolve->Lambda;
ConnaissanceDeLambda = Presolve->ConnaissanceDeLambda;

if ( BorneInfCalculee == OUI_PNE ) {
  
  PRS_VariablesDualesDeBornesIdentiques( Presolve, Cnt, BorneInf, LambdaMax[Cnt], BorneAmelioree );
	if ( *BorneAmelioree == OUI_PNE ) return;

  if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
    if ( BorneInf > LambdaMin[Cnt] ) {
			LambdaMin[Cnt] = BorneInf;
      *BorneAmelioree = OUI_PNE;			
		}
	}
	else {
    LambdaMin[Cnt] = BorneInf;
    *BorneAmelioree = OUI_PNE;				
    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
		else ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_CONNU;			
	}
}

if ( BorneSupCalculee == OUI_PNE ) {
 
  PRS_VariablesDualesDeBornesIdentiques( Presolve, Cnt, LambdaMin[Cnt], BorneSup, BorneAmelioree );
	if ( *BorneAmelioree == OUI_PNE ) return;

  if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
    if ( BorneSup < Presolve->LambdaMax[Cnt] ) {
		  Presolve->LambdaMax[Cnt] = BorneSup;
      *BorneAmelioree = OUI_PNE;						
		}
	}
	else {
    LambdaMax[Cnt] = BorneSup;
    *BorneAmelioree = OUI_PNE;			
    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
		else ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;			
	}		
}

if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
  if ( fabs( LambdaMin[Cnt] - LambdaMax[Cnt] ) < ECART_NUL_LAMBDAMIN_LAMBDAMAX ) {
	  # if TRACES == 1
	    printf("Cnt %d LambdaMin = LambdaMax = %e\n",Cnt,Presolve->LambdaMin[Cnt]);
		# endif
	  Lambda[Cnt] = 0.5 * ( LambdaMin[Cnt] + LambdaMax[Cnt] );
		LambdaMin[Cnt] = Lambda[Cnt];
		LambdaMax[Cnt] = Lambda[Cnt];
    ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;
    *BorneAmelioree = OUI_PNE;				
	}
}

return;
}


