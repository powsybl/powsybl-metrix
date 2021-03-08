/***********************************************************************

   FONCTION: On essai de remettre les bornes initiales tout en restant
	           dual realisable 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_define.h"
# include "spx_fonctions.h"  

# ifdef UTILISER_BORNES_AUXILIAIRES
/*----------------------------------------------------------------------------*/

void SPX_DualSupprimerLesBornesAuxiliaires( PROBLEME_SPX * Spx )
{
int Var; int i; int * NumerosDesVariablesHorsBase; double * CBarre; 
char * StatutBorneSupCourante; char * PositionDeLaVariable;
int NombreDeBornesAuxilairesAuDepart; double * SeuilDAmissibiliteDuale;

NumerosDesVariablesHorsBase = Spx->NumerosDesVariablesHorsBase;
CBarre                      = Spx->CBarre;
StatutBorneSupCourante      = Spx->StatutBorneSupCourante;
PositionDeLaVariable        = Spx->PositionDeLaVariable;
  
NombreDeBornesAuxilairesAuDepart = Spx->NombreDeBornesAuxiliairesUtilisees;

SeuilDAmissibiliteDuale = Spx->SeuilDAmissibiliteDuale2;

/* Boucle sur les variables hors base */
for ( i = 0 ; i < Spx->NombreDeVariablesHorsBase ; i++ ) {
  Var = NumerosDesVariablesHorsBase[i];		
	if ( StatutBorneSupCourante[Var] == BORNE_NATIVE ) continue;	

  if ( StatutBorneSupCourante[Var] == BORNE_AUXILIAIRE_DE_VARIABLE_BORNEE_INFERIEUREMENT ) {      		
    /* Si la variable est devenue duale admissible avec sa borne native on peut la passer sur borne inf
		   et remettre son type de borne initial */
		if ( CBarre[Var] > -SeuilDAmissibiliteDuale[Var] ) {
      SPX_SupprimerUneBorneAuxiliaire( Spx, Var );				
      PositionDeLaVariable[Var] = HORS_BASE_SUR_BORNE_INF;		
		}
  }				
	else if ( StatutBorneSupCourante[Var] == BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE ) {
    if ( fabs( CBarre[Var] ) < SeuilDAmissibiliteDuale[Var] ) {
      SPX_SupprimerUneBorneAuxiliaire( Spx, Var );		
      PositionDeLaVariable[Var] = HORS_BASE_A_ZERO;
		}
	}	
}

if ( Spx->NombreDeBornesAuxiliairesUtilisees != NombreDeBornesAuxilairesAuDepart ) {
  Spx->CalculerBBarre = OUI_SPX;
	# if VERBOSE_SPX
	  printf("Iteration %d Nombre de bornes auxiliaires utilisees %d\n",Spx->Iteration,Spx->NombreDeBornesAuxiliairesUtilisees);
	# endif
}

return;
}

# endif

