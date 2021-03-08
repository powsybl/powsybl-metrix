/***********************************************************************

   FONCTION: Algorithme dual: choix de la variable sortante.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h" 
# include "spx_define.h"

# ifdef SPX_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "spx_memoire.h"
# endif

# include "pne_define.h"
# include "pne_fonctions.h"

# include "lu_define.h"

/*----------------------------------------------------------------------------*/

void SPX_DualChoixDeLaVariableSortante( PROBLEME_SPX * Spx )
{
double PlusGrandeViolation; int ContrainteDeLaVariableSortante; int i; int iSor;
double * ValeurDeViolationDeBorne; double PlusPetiteViolation; 

/* RechercherLaVariableSortante */

Spx->VariableSortante = -1;
Spx->SortSurXmaxOuSurXmin = SORT_PAS;

ContrainteDeLaVariableSortante = -1;

iSor = -1;
PlusGrandeViolation = -1.;
PlusPetiteViolation = LINFINI_SPX;
ValeurDeViolationDeBorne = Spx->ValeurDeViolationDeBorne;

for ( i = 0 ; i < Spx->NombreDeContraintesASurveiller ; i++ ) {	
	if ( ValeurDeViolationDeBorne[i] > PlusGrandeViolation ) {
	  iSor = i;				
	  PlusGrandeViolation = ValeurDeViolationDeBorne[i];
  }
}

if ( iSor >= 0 ) {
	ContrainteDeLaVariableSortante = Spx->NumerosDesContraintesASurveiller[iSor];	
	if ( Spx->BBarre[ContrainteDeLaVariableSortante] < 0.0 ) Spx->SortSurXmaxOuSurXmin = SORT_SUR_XMIN;
	else Spx->SortSurXmaxOuSurXmin = SORT_SUR_XMAX;	
	Spx->VariableSortante = Spx->VariableEnBaseDeLaContrainte[ContrainteDeLaVariableSortante];	
}

/* Traces */
#if VERBOSE_SPX
if ( Spx->LaBaseDeDepartEstFournie == OUI_SPX && Spx->StrongBranchingEnCours != OUI_SPX ) {
  if ( Spx->VariableSortante >= 0 ) {
    printf("  *** Iteration %d\n",Spx->Iteration);
    if ( Spx->OrigineDeLaVariable[Spx->VariableSortante] == NATIVE ) {
      printf("  -> Algorithme dual variable de base sortante %d de type NATIVE ",Spx->VariableSortante);
    }
    else if ( Spx->OrigineDeLaVariable[Spx->VariableSortante] == ECART ) {
      printf("  -> Algorithme dual variable de base sortante %d de type ECART ",Spx->VariableSortante);
    }
    else if ( Spx->OrigineDeLaVariable[Spx->VariableSortante] == BASIQUE_ARTIFICIELLE ) {
      printf("  -> Algorithme dual variable de base sortante %d de type BASIQUE_ARTIFICIELLE ",Spx->VariableSortante);
    }
    else {
      printf("Bug dans l algorithme dual, sous-programme SPX_DualChoixDeLaVariableKiKitLaBase\n");
      exit(0);
    }
    if ( Spx->SortSurXmaxOuSurXmin == SORT_SUR_XMIN ) printf("elle SORT_SUR_XMIN "); 
    else if ( Spx->SortSurXmaxOuSurXmin == SORT_SUR_XMAX ) printf("elle SORT_SUR_XMAX ");
    else {
      printf("Bug dans l algorithme dual, sous-programme SPX_DualChoixDeLaVariableKiKitLaBase\n");
      exit(0);
    }  
    printf("valeur %15.8e contrainte associee %d B %e Xmax %e Scale %15.8e\n",
             Spx->BBarre[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]],Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante],
             Spx->B[Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante]],Spx->Xmax[Spx->VariableSortante],
             Spx->ScaleX[Spx->VariableSortante]);             	                
    fflush(stdout);
  }
}
#endif
/* Fin traces */

return; 

}

/*----------------------------------------------------------------------------*/

void SPX_DualChoixDeLaVariableSortanteAuHasard( PROBLEME_SPX * Spx )
{
int Cnt; int Var; double Xx; int Nombre; int NombreDeVariablesCandidates; int * VariableCandidate;       
int * VariableEnBaseDeLaContrainte; double * X; double * BBarre; double * Xmax; double * Xmin;
char * TypeDeVariable; double * BorneInfInfaisabilitePrimale; double * BorneSupInfaisabilitePrimale;
int ContrainteDeLaVariableSortante; int i; MATRICE * Matrice;

/*VariableCandidate = (int *) Spx->Bs;*/

Matrice = Spx->MatriceFactorisee;
VariableCandidate = (int *) (Matrice->W);  
			      
Spx->VariableSortante        = -1;
NombreDeVariablesCandidates = 0;

VariableEnBaseDeLaContrainte = Spx->VariableEnBaseDeLaContrainte;
X = Spx->X;
Xmin = Spx->Xmin;
Xmax = Spx->Xmax;    
BBarre = Spx->BBarre;
TypeDeVariable = Spx->TypeDeVariable;

BorneInfInfaisabilitePrimale = Spx->BorneInfInfaisabilitePrimale;
BorneSupInfaisabilitePrimale = Spx->BorneSupInfaisabilitePrimale;

for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
  Var = VariableEnBaseDeLaContrainte[Cnt];
	X[Var] = BBarre[Cnt];
  if ( TypeDeVariable[Var] == NON_BORNEE ) continue; /* Une variable libre ne sort pas de la base */
  /* La variable est donc bornee ( des 2 cotes ou seulement bornee inferieurement ) */    
  if ( BBarre[Cnt] < BorneInfInfaisabilitePrimale[Cnt] ) {
    VariableCandidate[NombreDeVariablesCandidates] = Var;
    NombreDeVariablesCandidates++;
    continue;
  }
  else if ( TypeDeVariable[Var] == BORNEE ) { 
    if ( BBarre[Cnt] > BorneSupInfaisabilitePrimale[Cnt] ) {
      VariableCandidate[NombreDeVariablesCandidates] = Var;
      NombreDeVariablesCandidates++;
    }  
  }
}

if ( NombreDeVariablesCandidates <= 0 ) goto FinSelectionAuHasard;

/* On tire un nombre au hasard compris entre 0 et NombreDeVariablesCandidates - 1 */
# if UTILISER_PNE_RAND == OUI_SPX
  Spx->A1 = PNE_Rand( Spx->A1 );
  Xx = Spx->A1 * (NombreDeVariablesCandidates - 1);
# else
  Xx = rand() * Spx->UnSurRAND_MAX * (NombreDeVariablesCandidates - 1);
# endif

Nombre = (int) Xx;
if ( Nombre >= NombreDeVariablesCandidates - 1 ) Nombre = NombreDeVariablesCandidates - 1; 

Spx->VariableSortante = VariableCandidate[Nombre];
ContrainteDeLaVariableSortante = Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante];

if ( BBarre[ContrainteDeLaVariableSortante] < 0.0 ) Spx->SortSurXmaxOuSurXmin = SORT_SUR_XMIN;
else Spx->SortSurXmaxOuSurXmin = SORT_SUR_XMAX;

FinSelectionAuHasard:

for ( i = 0 ; i < NombreDeVariablesCandidates ; i++ ) VariableCandidate[i] = 0;

return; 
}

