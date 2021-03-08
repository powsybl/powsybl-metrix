/***********************************************************************

   FONCTION: Calcul d'une borne auxiliaire pour rendre une variable
	           duale admissible.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# define INF_XMAX 100.
 
/*----------------------------------------------------------------------------*/

# ifdef UTILISER_BORNES_AUXILIAIRES

/*----------------------------------------------------------------------------*/
/* Utilisation de bornes auxilaires pour forcer l'admissibilite duale */
double SPX_CalculerUneBorneAuxiliaire( PROBLEME_SPX * Spx , int Var )	
{
double Amoy; int il; int ilMax; int * Cdeb; int * CNbTerm;
double Xmax; double * ACol; int * NumeroDeContrainte; double NormeL1deB;
double NormeL1deA; double * B; double Xmx1; double Xmx2; 

B = Spx->B;

Cdeb    = Spx->Cdeb;
CNbTerm = Spx->CNbTerm;
ACol    = Spx->ACol;
NumeroDeContrainte = Spx->NumeroDeContrainte;

/* On regarde si on dispose d'une borne auxiliaire */			
if ( Spx->TypeDeVariable[Var] == BORNEE_INFERIEUREMENT ) {		
  if ( Spx->StatutBorneSupAuxiliaire[Var] == BORNE_AUXILIAIRE_PRESOLVE ) {
  	Xmax = Spx->BorneSupAuxiliaire[Var];
		/* Maintenant que la borne auxiliaire a ete utilisee on l'invalide pour ne plus l'utiliser */
    Spx->StatutBorneSupAuxiliaire[Var] = BORNE_AUXILIAIRE_INVALIDE;				
		return( Xmax );
  }
}

B = Spx->B;

Cdeb    = Spx->Cdeb;
CNbTerm = Spx->CNbTerm;
ACol    = Spx->ACol;
NumeroDeContrainte = Spx->NumeroDeContrainte;

il    = Cdeb[Var];
ilMax = il + CNbTerm[Var];
Amoy  = 0.0;
NormeL1deB = 0.0;
NormeL1deA = 0.0;						
while ( il < ilMax ) {							
  NormeL1deB+= fabs( B[NumeroDeContrainte[il]] );
  NormeL1deA+= fabs( ACol[il] );
  Amoy+= fabs( ACol[il] );								   
  il++;
}
Amoy/= CNbTerm[Var];
Xmx1 = (1. + Spx->ValeurMoyenneDuSecondMembre ) / ( 1. + Amoy );            
Xmx2 = (1. + NormeL1deB ) / ( 1. + NormeL1deA );						
if ( Xmx1 > Xmx2 ) Xmax = Xmx1;
else Xmax = Xmx2;

if ( Spx->TypeDeVariable[Var] == NON_BORNEE ) Xmax *= 2.0;
Xmax *= (double) Spx->CoeffMultiplicateurDesBornesAuxiliaires;

if ( Xmax < INF_XMAX ) Xmax = INF_XMAX;

return( Xmax );     

}

# endif





