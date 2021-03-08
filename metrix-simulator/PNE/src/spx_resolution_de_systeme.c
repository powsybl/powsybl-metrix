/***********************************************************************

   FONCTION: Resoudre B x = b 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "lu_define.h"   
# include "lu_fonctions.h"
   
/*----------------------------------------------------------------------------*/																		 
void SPX_ResolutionDeSysteme( PROBLEME_SPX * Spx,
                              char TypeDEntree, /* Mode de stockage du vecteur second membre */
                              double * A , /* Second membre et solution */
                              int * IndexDesTermesNonNuls,
													    int * NombreDeTermesNonNuls,
													    char * TypeDeSortie, /* Mode de stockage demande pour la solution */
													
                              char CalculEnHyperCreux, /* Vaut OUI_SPX ou NON_SPX */
                              char Save,  /* Sauvegarde ou non du resultat de la triangulaire inferieure */
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
PrecisionDemandee = SEUIL_DE_VIOLATION_DE_BORNE;
	 
if ( Spx->UtiliserLaLuUpdate != OUI_SPX ) Save = NON_LU;

if ( CalculEnHyperCreux == NON_SPX ) {
  /* On appelle toujours LU_LuSolvLuUpdate qui de toutes facons appelle LU_LuSolv.
     Mais si on ne fait pas de LuUpdate, on force Save a NON_LU */
  LU_LuSolvLuUpdate( Spx->MatriceFactorisee, A, &CodeRetour, Save, SecondMembreCreux,
  	                 &Matrice, NbIterRaffinement, PrecisionDemandee );

}
else {
  LU_LuSolvSecondMembreHyperCreux( Spx->MatriceFactorisee, A, IndexDesTermesNonNuls,
		                               NombreDeTermesNonNuls, TypeDEntree, Save, TypeDeSortie );		
}
		     
if ( CodeRetour == PRECISION_DE_RESOLUTION_NON_ATTEINTE ) {
  /* Tant pis on continue quand-meme */
    CodeRetour = 0;     
}

return;
}


  
