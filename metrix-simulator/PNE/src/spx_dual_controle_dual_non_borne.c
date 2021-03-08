/***********************************************************************

   FONCTION: Verifications pour confirmer l'absence de solution
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"      

/*# define VERBOSE_SPX 1*/

/*----------------------------------------------------------------------------*/

void SPX_DualControleDualNonBorne( PROBLEME_SPX * Spx, char * ConfirmationDualNonBorneEnCours )

{
int AdmissibiliteRestauree; char CoutsReduitsAJour;

#if VERBOSE_SPX 
  if ( Spx->TypeDeVariable[Spx->VariableSortante] == BORNEE ) { 
    printf("Detection dual non borne a l iteration %d variable sortante %d valeur %10.8lf Min %lf Max %lf SeuilDeViolationDeBorne %e\n",
    Spx->Iteration,Spx->VariableSortante,Spx->BBarre[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]],
		Spx->Xmin[Spx->VariableSortante],Spx->Xmax[Spx->VariableSortante],Spx->SeuilDeViolationDeBorne[Spx->VariableSortante]);
  }
  else if ( Spx->TypeDeVariable[Spx->VariableSortante] == BORNEE_INFERIEUREMENT ) {
    printf("Detection dual non borne a l iteration %d variable sortante %d valeur %10.8lf Min %lf Max INFINI SeuilDeViolationDeBorne %e\n",
    Spx->Iteration,Spx->VariableSortante,Spx->BBarre[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]],
		Spx->Xmin[Spx->VariableSortante],Spx->SeuilDeViolationDeBorne[Spx->VariableSortante]);
  }
  else if ( Spx->TypeDeVariable[Spx->VariableSortante] == BORNEE_SUPERIEUREMENT ) {
    printf("Detection dual non borne a l iteration %d variable sortante %d valeur %10.8lf Min -INFINI Max %lf SeuilDeViolationDeBorne %e\n",
    Spx->Iteration,Spx->VariableSortante,Spx->BBarre[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]],
		Spx->Xmax[Spx->VariableSortante],Spx->SeuilDeViolationDeBorne[Spx->VariableSortante]);
  }
#endif

if ( Spx->NombreDeChangementsDeBase > 0 ) CoutsReduitsAJour = NON_SPX;
else CoutsReduitsAJour = OUI_SPX;

if ( Spx->LesCoutsOntEteModifies == OUI_SPX ) {
  /* Il est preferable de calculer les couts reduit par une resolution de systeme plutot que par une mise a jour */
  memcpy( (char *) Spx->C, (char *) Spx->Csv, Spx->NombreDeVariables * sizeof( double ) );
  Spx->LesCoutsOntEteModifies = NON_SPX;
  CoutsReduitsAJour = NON_SPX;		
}

/* Il n'y a pas de variable entrante possible et donc en principe le dual est non borne */
/* Si on a deja reinitilise les poids pour obtenir une confirmation de dual non borne alors 
   on continue: on ne trouvera pas de variable sortante et on sera amene a tester l'indicateur Spx->AdmissibilitePossible 
   si l'admissibilite est tout de meme possible, alors on repartira dans la sequence standard deroulee lorsqu'on 
   obtient une solution */	 
if ( *ConfirmationDualNonBorneEnCours == NON_SPX ) {
  /* On n'est pas encore dans la phase de confirmation: on controle tout d'abord l'admissibilite duale de 
     la base courante */		 
  if ( Spx->NombreDeChangementsDeBase > 0 ) {
    /* Si la factorisee est trop ancienne on refactorise */
    SPX_FactoriserLaBase( Spx );	
    if ( Spx->ChoixDeVariableSortanteAuHasard == OUI_SPX ) {
		  /* Probleme: la refactorisation n'a pas marche */
      return;
    }
    CoutsReduitsAJour = NON_SPX;	
  }
  if ( CoutsReduitsAJour == NON_SPX ) {
	  /* Recalcul des couts reduits car ils peuvent etre imprecis */
    SPX_CalculerPi( Spx );             
    SPX_CalculerLesCoutsReduits( Spx );
    CoutsReduitsAJour = OUI_SPX;
    Spx->CalculerCBarre = NON_SPX;
	}		
	/* Permet de creer des bornes si necessaire */
  SPX_VerifierAdmissibiliteDuale( Spx , &AdmissibiliteRestauree );
  /* Reinitialisation des poids du steepest edge pour choisir une variable sortante sur la base de la violation de borne */
  /* Non, il vaut mieux conserver les poids car parfois on se retourve dans des cas degeneres et dans ce cas
	   les poids sont utiles. La consequence est qu'on aura toujours la meme variable sortante (sauf s'il y a des
		 petites differences entre la mise a jour et le calcul complet de BBarre, mais le calcul de la variable entrante
		 sera fait avec les couts non bruites */
	/*SPX_InitDualPoids( Spx );*/ 

  *ConfirmationDualNonBorneEnCours = OUI_SPX;   
  Spx->ModifCoutsAutorisee         = NON_SPX;
			
	/* On remet le seuil de pivotage initial */
  Spx->SeuilDePivotDual = VALEUR_DE_PIVOT_ACCEPTABLE;
	Spx->ChoixDeVariableSortanteAuHasard = NON_SPX;
  Spx->NombreDeChoixFaitsAuHasard      = 0;
	return;
}
else {
  /* Mise a jour de l'indicateur: Spx->AdmissibilitePossible */
	/* Quand on en arrive a cet extreme, on conserve les couts courants (donc eventuellement bruites) et
	   les couts reduits correspondants */
  *ConfirmationDualNonBorneEnCours = NON_SPX;   
  SPX_DualConfirmerDualNonBorne( Spx );
}
  
return;
}


