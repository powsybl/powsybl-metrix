/***********************************************************************

   FONCTION: Resolution de systeme avec B transposee.
	 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "lu_define.h"
# include "lu_fonctions.h" 

/*----------------------------------------------------------------------------*/
void SPX_ResolutionDeSystemeTransposee( PROBLEME_SPX * Spx,
                                        char TypeDEntree, /* Mode de stockage du vecteur second membre */
                                        double * U, /* Second membre et resultat */
                                        int * IndexDesTermesNonNuls,
													              int * NombreDeTermesNonNuls,

																				char * TypeDeSortie, 
                                        char CalculEnHyperCreux, /* Vaut OUI_SPX ou NON_SPX */
			                                  char SecondMembreCreux /* Vaut OUI_LU ou NON_LU */
																			)
{
int CodeRetour; int NbIterRaffinement; double PrecisionDemandee;
MATRICE_A_FACTORISER Matrice;

CodeRetour = 0;

Matrice.ValeurDesTermesDeLaMatrice = Spx->ACol;
Matrice.IndicesDeLigne             = Spx->NumeroDeContrainte;
Matrice.IndexDebutDesColonnes	     = Spx->CdebBase;     
Matrice.NbTermesDesColonnes	       = Spx->NbTermesDesColonnesDeLaBase;

NbIterRaffinement = 0;  
if ( Spx->UtiliserLaLuUpdate == OUI_SPX ) {
  if ( Spx->FaireDuRaffinementIteratif > 0 ) NbIterRaffinement = 1;	
}
PrecisionDemandee = SEUIL_ADMISSIBILITE_DUALE_2;	

if ( CalculEnHyperCreux == NON_SPX ) {
  /* On appelle toujours LU_LuSolvTransposeeLuUpdate qui de toutes facons
     appelle LU_LuSolvTransposee */	 
  LU_LuSolvTransposeeLuUpdate( Spx->MatriceFactorisee,
                               U,           /* Le vecteur second membre et solution */			      
                               &CodeRetour, /* Le code retour ( 0 si tout s'est bien passe ) */
		                           SecondMembreCreux,
			                         &Matrice, NbIterRaffinement, PrecisionDemandee
                             );
}
else {
  LU_LuSolvTransposeeSecondMembreHyperCreux( Spx->MatriceFactorisee, U, IndexDesTermesNonNuls,
																					   NombreDeTermesNonNuls, TypeDEntree, TypeDeSortie );
}
														 
if ( CodeRetour == PRECISION_DE_RESOLUTION_NON_ATTEINTE ) {
  /* Tant pis on continue quand-meme */
  CodeRetour = 0;     
}
    
return;
}
    
