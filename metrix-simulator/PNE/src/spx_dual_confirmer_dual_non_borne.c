/***********************************************************************

   FONCTION: Confirmation dual non borne (i.e. pas de solution) 
            
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "lu_define.h"
  
/*# define VERBOSE_SPX 1*/
  
/*----------------------------------------------------------------------------*/

void SPX_DualConfirmerDualNonBorne( PROBLEME_SPX * Spx )

{
double NBarreR          ; double X           ; /* char PositionDeLaVariable; */ double MinRapport   ;  
double CBarreTPlus      ; double CBarreTMoins; double NBarreRTPlus        ; double NBarreRTMoins;
double S                ; double SeuilDePivot; double Violation           ; int CntChoix       ;
int   Cnt               ; int j; double * ErBMoinsUn; double * Erb; /* int   Var; int il ; int ilMax; */
char   NBarreRInitialise; double Seuil       ; MATRICE * Matrice;

Spx->AdmissibilitePossible = NON_SPX;

Cnt = Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante];
/* Si la variable sortante viole trop ses bornes, on ne cherche pas plus loin */
if ( Spx->BBarre[Cnt] < Spx->Xmin[Spx->VariableSortante] ) {
  Violation = Spx->Xmin[Spx->VariableSortante] - Spx->BBarre[Cnt];
}
else Violation = Spx->BBarre[Cnt] - Spx->Xmax[Spx->VariableSortante];

/* Il faut ramener la Violation dans le contexte des bornes natives */
/*
Violation = fabs( Violation );
if ( Spx->FaireDuScalingSPX == OUI_SPX ) {
  Violation *= Spx->ScaleX[Spx->VariableSortante];
}
*/

if ( fabs( Violation ) > SEUIL_MAX_DE_VIOLATION_DE_BORNE ) {
  #if VERBOSE_SPX
    printf("Violation trop importante %e on confirme le dual non borne\n",Violation); fflush(stdout);
  #endif
  return;
}

SeuilDePivot = VALEUR_DE_PIVOT_ACCEPTABLE;
NBarreRInitialise = NON_SPX;
NBarreR = 1; /* Pour ne pas avoir de warning a la compilation */

#if VERBOSE_SPX
  printf("Recherche des variables entrantes TPlus et TMoins pour confirmer le dual non borne\n"); fflush(stdout);
#endif

Erb = NULL;
if ( Spx->TypeDeStockageDeErBMoinsUn != COMPACT_SPX ) {
  ErBMoinsUn = Spx->ErBMoinsUn;
}
else {
  Matrice = Spx->MatriceFactorisee;
  Erb = Matrice->W;  	
	for ( j = 0 ; j < Spx->NbTermesNonNulsDeErBMoinsUn ; j++ ) {
	  Erb[Spx->IndexTermesNonNulsDeErBMoinsUn[j]] = Spx->ErBMoinsUn[j];
	}
 	ErBMoinsUn = Erb;
}
	
/* Norme infinie */
for ( S = -1., Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
    if ( fabs( ErBMoinsUn[Cnt] ) > S ) S = fabs( ErBMoinsUn[Cnt] );
}
S = 1. / S;

/* Le vecteur dual */
for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) Spx->Pi[Cnt] = -ErBMoinsUn[Cnt] * S;

/* Si la seule variable qui peut sortir est une variable t alors on est bien non admissible */
MinRapport = LINFINI_SPX;
CntChoix = -1;
for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
  CBarreTPlus  = 1. - Spx->Pi[Cnt];
  CBarreTMoins = 1. + Spx->Pi[Cnt];
  if ( CBarreTPlus < 0. || CBarreTMoins < 0. ) {
    printf("Iteration %d CBarreTPlus %lf CBarreTMoins %lf Cnt %d\n",Spx->Iteration,CBarreTPlus,CBarreTMoins,Cnt);
  }
  NBarreRTPlus  = ErBMoinsUn[Cnt];
  NBarreRTMoins = -ErBMoinsUn[Cnt];		
  if ( Spx->SortSurXmaxOuSurXmin == SORT_SUR_XMIN ) {
    /* Si le produit CBarre * NBarreR est négatif la variable peut rentre en base */		
    if ( fabs( NBarreRTPlus ) >= SeuilDePivot ) { 
      X = CBarreTPlus / NBarreRTPlus;
      if ( X < 0. && fabs( X ) < MinRapport ) { 
        /*printf("Contrainte %d la variable TPlus peut entrer en base, CBarreTPlus %lf NBarreRTPlus %lf\n",Cnt,CBarreTPlus,NBarreRTPlus);*/
        MinRapport = fabs( X ); 
        NBarreR    = NBarreRTPlus;
	      NBarreRInitialise = OUI_SPX;
				CntChoix = Cnt;
      }
    }
    if ( fabs( NBarreRTMoins ) >= SeuilDePivot ) {
      X = CBarreTMoins / NBarreRTMoins;
      if ( X < 0. && fabs( X ) < MinRapport ) {
        /*printf("Contrainte %d la variable TMoins peut entrer en base, CBarreTMoins %lf NBarreRTMoins %lf\n",Cnt,CBarreTMoins,NBarreRTMoins);*/
        MinRapport = fabs( X ); 
        NBarreR    = NBarreRTMoins;
	      NBarreRInitialise = OUI_SPX;
				CntChoix = Cnt;
      }
    }
  }
  else if ( Spx->SortSurXmaxOuSurXmin == SORT_SUR_XMAX ) {
    /* Si le produit CBarre * NBarreR est positif la variable peut rentre en base */				
    if ( fabs( NBarreRTPlus ) >= SeuilDePivot ) { 
      X = CBarreTPlus / NBarreRTPlus;
      if ( X > 0. && X < MinRapport ) { 
        /*printf("Contrainte %d la variable TPlus peut entrer en base, CBarreTPlus %lf NBarreRTPlus %lf\n",Cnt,CBarreTPlus,NBarreRTPlus);*/
        MinRapport = X; 
        NBarreR    = NBarreRTPlus;
	      NBarreRInitialise = OUI_SPX;
				CntChoix = Cnt;
      }
    }		
    if ( fabs( NBarreRTMoins ) >= SeuilDePivot ) {
      X = CBarreTMoins / NBarreRTMoins;
      if ( X > 0. && X < MinRapport ) {
        /*printf("Contrainte %d la variable TMoins peut entrer en base, CBarreTMoins %lf NBarreRTMoins %lf\n",Cnt,CBarreTMoins,NBarreRTMoins);*/
        MinRapport = X;  
        NBarreR    = NBarreRTMoins;
	      NBarreRInitialise = OUI_SPX;
				CntChoix = Cnt;
      }
    }
  }
}

if ( NBarreRInitialise == OUI_SPX && CntChoix >= 0 ) {
  if ( Spx->SortSurXmaxOuSurXmin == SORT_SUR_XMIN ) {
    Spx->DeltaXSurLaVariableHorsBase = Spx->BBarre[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]] / NBarreR;
  }
  else { /* SORT_SUR_XMAX */
    Spx->DeltaXSurLaVariableHorsBase = ( Spx->BBarre[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]] - Spx->Xmax[Spx->VariableSortante] ) / NBarreR;
  }

	/* Les variables t representent des variables d'ecart i.e. des possibilites de violation des contraintes */
	/*
  Seuil = SEUIL_DE_VIOLATION_DE_BORNE_NON_NATIVE * Spx->ScaleB[CntChoix];
  if ( Seuil < SEUIL_MIN_DE_VIOLATION_DE_BORNE_NON_NATIVE ) Seuil = SEUIL_MIN_DE_VIOLATION_DE_BORNE_NON_NATIVE;
  else if ( Seuil > SEUIL_MAX_DE_VIOLATION_DE_BORNE_NON_NATIVE ) Seuil = SEUIL_MAX_DE_VIOLATION_DE_BORNE_NON_NATIVE;
	*/
	
  Seuil = SEUIL_DADMISSIBILITE * Spx->ScaleB[CntChoix];	 
	if ( Seuil < 0.01 * SEUIL_DADMISSIBILITE ) Seuil = 0.01 * SEUIL_DADMISSIBILITE;  
  else if ( Seuil > 10 * SEUIL_DADMISSIBILITE ) Seuil = 10 * SEUIL_DADMISSIBILITE;	  
	
  #if VERBOSE_SPX
    printf("Type de sortie %d Delta X a faire sur T %15.10lf Seuil %e\n",Spx->SortSurXmaxOuSurXmin,Spx->DeltaXSurLaVariableHorsBase,
		        Seuil); 
  #endif
		
  if ( fabs( Spx->DeltaXSurLaVariableHorsBase ) < Seuil ) {
    Spx->AdmissibilitePossible = OUI_SPX;		
    #if VERBOSE_SPX
      printf("Rectification du diagnostique: admissibilite possible\n");
      printf("Iteration %d DeltaXSurLaVariableHorsBase %e SeuilDeViolationDeBorne %e SEUIL_DE_VIOLATION_DE_BORNE %e\n",
              Spx->Iteration,
	            Spx->DeltaXSurLaVariableHorsBase,
	            Spx->SeuilDeViolationDeBorne[Spx->VariableSortante],
	            SEUIL_DE_VIOLATION_DE_BORNE ); 
      printf("             Variable sortante:  Xmin %e X %e Xmax %e\n",
              Spx->Xmin[Spx->VariableSortante],
	            Spx->BBarre[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]],
	            Spx->Xmax[Spx->VariableSortante] );	  
    #endif
  }
	else {
    #if VERBOSE_SPX
      printf("On confirme le dual non borne\n");
    #endif
	}
}
else {
  if ( fabs( Violation ) > SEUIL_DE_VIOLATION_DE_BORNE ) {
    #if VERBOSE_SPX
      printf("On confirme le dual non borne NBarreRInitialise %d CntChoix %d\n",NBarreRInitialise,CntChoix);
    #endif
	}
	else {
    #if VERBOSE_SPX
      printf("Rectification du diagnostique: admissibilite possible\n");
    #endif
    Spx->AdmissibilitePossible = OUI_SPX;
	}
}

/* Raz eventuelle du vecteur W de Matrice */
if ( Spx->TypeDeStockageDeErBMoinsUn == COMPACT_SPX && Erb != NULL ) {
	for ( j = 0 ; j < Spx->NbTermesNonNulsDeErBMoinsUn ; j++ ) Erb[Spx->IndexTermesNonNulsDeErBMoinsUn[j]] = 0.0;
}

return;
}




