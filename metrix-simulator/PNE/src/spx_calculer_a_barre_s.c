/***********************************************************************

   FONCTION: Calcul de A_BARRE_S = B-1 * AS
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"   

# include "lu_define.h"

/*----------------------------------------------------------------------------*/
void SPX_TenterRestaurationCalculABarreSEnHyperCreux( PROBLEME_SPX * Spx )
{
int Cnt; double * ABarreS; int Count;
	
if ( Spx->CountEchecsABarreS == 0 ) {
  if ( Spx->Iteration % CYCLE_TENTATIVE_HYPER_CREUX == 0 ) {
		Spx->NbEchecsABarreS    = SEUIL_REUSSITE_CREUX;
		Spx->CountEchecsABarreS = SEUIL_REUSSITE_CREUX + 2;
	}
}
if ( Spx->CountEchecsABarreS == 0 ) return;

Spx->CountEchecsABarreS--;		
/* On compte le nombre de termes non nuls du resultat */
ABarreS = Spx->ABarreS;
Count = 0;
for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) if ( ABarreS[Cnt] != 0.0 ) Count++;
if ( Count < 0.1 * Spx->NombreDeContraintes ) Spx->NbEchecsABarreS--;
if ( Spx->NbEchecsABarreS <= 0 ) {
  # if VERBOSE_SPX
    printf("Remise en service de l'hyper creux pour le calcul de ABarreS, iteration %d\n",Spx->Iteration);
	# endif
  Spx->AvertissementsEchecsABarreS = 0;
  Spx->CountEchecsABarreS = 0;
	Spx->CalculABarreSEnHyperCreux = OUI_SPX;
}
else if ( Spx->CountEchecsABarreS <= 0 ) {
  Spx->CountEchecsABarreS = 0;
  if ( Spx->CalculABarreSEnHyperCreux == NON_SPX ) Spx->AvertissementsEchecsABarreS ++;
  if ( Spx->AvertissementsEchecsABarreS >= SEUIL_ABANDON_HYPER_CREUX ) {
    # if VERBOSE_SPX
      printf("Arret prolonge de l'hyper creux pour le calcul de ABarreS, iteration %d\n",Spx->Iteration);
	  # endif	
	  Spx->CalculABarreSEnHyperCreuxPossible = NON_SPX;		
	}
}

return;
}
/*----------------------------------------------------------------------------*/

void SPX_CalculerABarreS( PROBLEME_SPX * Spx )
{
int il; int ilMax; char Save; char SecondMembreCreux; double * ABarreS; char OK;
int * Cdeb; int * CNbTerm; int * NumeroDeContrainte ; double * ACol;
int NbTermesNonNuls; int CntBase; int * CntDeABarreSNonNuls; int i; 
char TypeDEntree; char TypeDeSortie; char CalculEnHyperCreux; char HyperCreuxInitial;

ABarreS            = Spx->ABarreS;
Cdeb               = Spx->Cdeb;
CNbTerm            = Spx->CNbTerm;
NumeroDeContrainte = Spx->NumeroDeContrainte;
ACol               = Spx->ACol;

CntDeABarreSNonNuls = NULL;

CalculEnHyperCreux = NON_SPX;
HyperCreuxInitial  = NON_SPX;
if ( Spx->TypeDeCreuxDeLaBase == BASE_HYPER_CREUSE && Spx->CalculABarreSEnHyperCreux == OUI_SPX &&
     Spx->FaireDuRaffinementIteratif <= 0 ) { 
  if ( CNbTerm[Spx->VariableEntrante] < TAUX_DE_REMPLISSAGE_POUR_VECTEUR_HYPER_CREUX * Spx->NombreDeContraintes ) {
    CalculEnHyperCreux = OUI_SPX;
    HyperCreuxInitial  = OUI_SPX;		
  }
}

if ( CalculEnHyperCreux ==  OUI_SPX ) {

	CntDeABarreSNonNuls = Spx->CntDeABarreSNonNuls;	
	NbTermesNonNuls = 0;
  il    = Cdeb[Spx->VariableEntrante];		   
  ilMax = il + CNbTerm[Spx->VariableEntrante];
  while ( il < ilMax ) {
    ABarreS            [NbTermesNonNuls] = ACol[il];
		CntDeABarreSNonNuls[NbTermesNonNuls] = NumeroDeContrainte[il];
		NbTermesNonNuls++;
    il++;
	}
	TypeDEntree  = COMPACT_LU;
	TypeDeSortie = COMPACT_LU;
	
}
else {

  /*memset( (char *) ABarreS , 0 , Spx->NombreDeContraintes * sizeof( double ) );*/
	{ int i;
	  for ( i = 0 ; i < Spx->NombreDeContraintes ; i++ ) ABarreS[i] = 0;
	}

  il    = Cdeb[Spx->VariableEntrante];
  ilMax = il + CNbTerm[Spx->VariableEntrante];
  while ( il < ilMax ) {
    ABarreS[NumeroDeContrainte[il]] = ACol[il];	
    il++;
  }
  TypeDEntree  = VECTEUR_LU;
  TypeDeSortie = VECTEUR_LU;
	
}
  
/* Resolution du systeme */
Save = OUI_LU; /* Mis a NON_LU si pas de LU update */
SecondMembreCreux = OUI_LU;
SPX_ResoudreBYegalA( Spx, TypeDEntree, ABarreS, CntDeABarreSNonNuls, &NbTermesNonNuls,
                     &TypeDeSortie, CalculEnHyperCreux, Save, SecondMembreCreux );

if ( CalculEnHyperCreux == OUI_SPX ) {
  if ( TypeDeSortie != TypeDEntree ) {
    CalculEnHyperCreux = NON_SPX;
    /* Ca s'est pas bien passe et on s'est forcement retrouve en VECTEUR_LU */		
		Spx->NbEchecsABarreS++;
		/*printf("Echec hyper creux ABarreS iteration %d\n",Spx->Iteration);*/
		if ( Spx->NbEchecsABarreS >= SEUIL_ECHEC_CREUX ) {
      # if VERBOSE_SPX
        printf("Arret de l'hyper creux pour le calcul de ABarreS, iteration %d\n",Spx->Iteration);
	    # endif			
		  Spx->CalculABarreSEnHyperCreux = NON_SPX;
			Spx->CountEchecsABarreS  = 0;
		}		
  }
  else Spx->NbEchecsABarreS = 0;
}
										 
if ( CalculEnHyperCreux ==  OUI_SPX ) {
	Spx->TypeDeStockageDeABarreS = COMPACT_SPX;

  Spx->NbABarreSNonNuls = NbTermesNonNuls;
  CntBase = Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante];

	/* Dangereux. On lui prefere la methode suivant */
  /*for ( i = 0 ; CntDeABarreSNonNuls[i] != CntBase ; i++ );*/
	/*Spx->ABarreSCntBase = ABarreS[i];*/

  OK = NON_SPX;
  for ( i = 0 ; i < Spx->NbABarreSNonNuls ; i++ ) {
	  if ( CntDeABarreSNonNuls[i] == CntBase ) {
      Spx->ABarreSCntBase = ABarreS[i];
			OK = OUI_SPX;
      break;
		}
	}
	if ( OK == NON_SPX ) {
	  /* Pb d'epsilon, on prend la aleur de NBarreR */
    Spx->ABarreSCntBase = Spx->NBarreR[Spx->VariableEntrante];		
	}	
}
else {
  Spx->TypeDeStockageDeABarreS = VECTEUR_SPX;
  Spx->ABarreSCntBase = ABarreS[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]];

  /* Si on est pas en hyper creux, on essaie d'y revenir */
	if ( HyperCreuxInitial == NON_SPX ) {
    if ( Spx->CalculABarreSEnHyperCreux == NON_SPX ) {
      if ( Spx->CalculABarreSEnHyperCreuxPossible == OUI_SPX ) {
        SPX_TenterRestaurationCalculABarreSEnHyperCreux( Spx );
	    }
    }   
	}
	
}

return;
}

