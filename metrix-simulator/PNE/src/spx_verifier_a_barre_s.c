/***********************************************************************

   FONCTION: Verification de A_BARRE_S = B-1 * AS
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h" 

# include "pne_fonctions.h"

# include "lu_define.h"
# include "lu_fonctions.h"

# define CYCLE_DE_VERIFICATION_DE_ABARRES              10  
# define CYCLE_DE_VERIFICATION_DE_ABARRES_HYPER_CREUX  20

# define SEUIL_DE_VERIFICATION_DE_ABarreS_1    1.e-6 /*1.e-7*/   
# define SEUIL_DE_VERIFICATION_DE_ABarreS_2    1.e-6 /*1.e-7*/   
# define NOMBRE_DE_VERIFICATIONS               100 /*100*/

# define NBITER_RAFFINEMENT 1

/*----------------------------------------------------------------------------*/
/*            Verification du calcul de ABarreS pour savoir s'il faut
              refactoriser la base: on verifie sur k contraintes tirees
              au hasard  		         			      */

void SPX_VerifierABarreS( PROBLEME_SPX * Spx )
{
int il; int ilMax       ; int Cnt     ; int Var   ; double X      ; int NbFois; int Nombre ;   
int NombreDeVerifications; double * Bs  ; int * Cdeb; int * CNbTerm; int * NumeroDeContrainte;
double * ACol; int * Mdeb; int * NbTerm; int * Indcol; double * A;
int * ContrainteDeLaVariableEnBase; double * ABarreS; int NombreDeContraintes; int NmX;
double Ecart; char Imprecision; int * CntDeABarreSNonNuls; int j; 
int ic; int icMax; int * VariableEnBaseDeLaContrainte; double * ErBMoinsUn;
char * PositionDeLaVariable;

/*		   
printf("Verification de ABarreS dans SPX_VerifierABarreS iteration %d\n",Spx->Iteration); 
*/

if ( Spx->TypeDeStockageDeABarreS == COMPACT_SPX ) {
  if ( Spx->Iteration % CYCLE_DE_VERIFICATION_DE_ABARRES_HYPER_CREUX != 0 ) return;
}
else {
  if ( Spx->Iteration % CYCLE_DE_VERIFICATION_DE_ABARRES != 0 ) return;
}

Imprecision = NON_SPX;

NombreDeContraintes = Spx->NombreDeContraintes;
A  = Spx->A;
Bs = Spx->Bs;
ABarreS = Spx->ABarreS;
if ( Spx->TypeDeStockageDeABarreS == COMPACT_SPX ) {
  ErBMoinsUn = Spx->ErBMoinsUn;
	
  /*memset( (char *) ErBMoinsUn , 0 , NombreDeContraintes * sizeof( double ) );*/
	{ int i;
	  for ( i = 0 ; i < NombreDeContraintes ; i++ ) ErBMoinsUn[i]= 0;
	}
	
	CntDeABarreSNonNuls = Spx->CntDeABarreSNonNuls;	
  for ( j = 0 ; j < Spx->NbABarreSNonNuls ; j++ ) ErBMoinsUn[CntDeABarreSNonNuls[j]] = ABarreS[j];	
	ABarreS = ErBMoinsUn;	
}

Cdeb    = Spx->Cdeb;
CNbTerm = Spx->CNbTerm;
NumeroDeContrainte = Spx->NumeroDeContrainte;  
ACol   = Spx->ACol;
Mdeb   = Spx->Mdeb;
NbTerm = Spx->NbTerm;
Indcol = Spx->Indcol;
ContrainteDeLaVariableEnBase = Spx->ContrainteDeLaVariableEnBase;
PositionDeLaVariable = Spx->PositionDeLaVariable;

NombreDeVerifications = NOMBRE_DE_VERIFICATIONS;

NmX = (int) ceil( 0.5 * NombreDeContraintes );
if ( NombreDeVerifications > NmX ) NombreDeVerifications = NmX;

/* On tire un nombre au hasard le nombre de verifications qu'on va faire */
  # if UTILISER_PNE_RAND == OUI_SPX
    Spx->A1 = PNE_Rand( Spx->A1 );
    X = Spx->A1 * (float) NombreDeVerifications;
	# else	
    X = rand() * Spx->UnSurRAND_MAX * (float) NombreDeVerifications;
  # endif

NombreDeVerifications = (int) X;
if ( NombreDeVerifications <= 0 ) NombreDeVerifications = 1;

/*memset( (char *) Bs , 0 , NombreDeContraintes * sizeof( double ) );*/

Var   = Spx->VariableEntrante;
ic    = Cdeb[Var];
icMax = ic + CNbTerm[Var];
while ( ic < icMax ) {
  Bs[NumeroDeContrainte[ic]] = -ACol[ic];
  ic++;
}

NbFois = 0;
while ( NbFois < NombreDeVerifications ) { 
  /* On tire un nombre au hasard compris entre 0 et NombreDeContraintes - 1 */
  # if UTILISER_PNE_RAND == OUI_SPX
    Spx->A1 = PNE_Rand( Spx->A1 );
    X = Spx->A1 * (NombreDeContraintes - 1);		
	# else		
    X = rand() * Spx->UnSurRAND_MAX * (NombreDeContraintes - 1);		
  # endif
	
  Nombre = (int) X;	
  if ( Nombre >= NombreDeContraintes - 1 ) Nombre = NombreDeContraintes - 1; 
  Cnt = Nombre;	
  X   = Bs[Cnt];
  /* Verification */
  il    = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
	  if ( PositionDeLaVariable[Indcol[il]] == EN_BASE_LIBRE ) {
      X+= A[il] * ABarreS[ContrainteDeLaVariableEnBase[Indcol[il]]];
		}
    il++;
  }
  if ( fabs( X ) > SEUIL_DE_VERIFICATION_DE_ABarreS_1 ) {
	  Imprecision = OUI_SPX;
    if ( Spx->NombreDeChangementsDeBase < 10 ) { 
		  /* Si ca se produit dans les premieres iterations apres une factorisation */
		  Spx->FlagStabiliteDeLaFactorisation = 1;						
		}
		break;
  } 
  NbFois++;
}

if ( Imprecision == OUI_SPX ) {
  /* Suspiscion: on recalcule l'ecart moyen pour voir s'il faut refactoriser */
	VariableEnBaseDeLaContrainte = Spx->VariableEnBaseDeLaContrainte;
  for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
    Var   = VariableEnBaseDeLaContrainte[Cnt];
		X     = ABarreS[Cnt];
    ic    = Cdeb[Var];
    icMax = ic + CNbTerm[Var];
    while ( ic < icMax ) {
		  Bs[NumeroDeContrainte[ic]]+= ACol[ic] * X;
      ic++;
    }		
	}
  Ecart = 0.0;
  for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) Ecart+= fabs( Bs[Cnt] );
  Ecart/= NombreDeContraintes;
  if ( Ecart > SEUIL_DE_VERIFICATION_DE_ABarreS_2 && Spx->NombreDeChangementsDeBase > 0 ) {  
    #if VERBOSE_SPX
      printf("SPX_VerifierABarreS Iteration %d erreur de resolution sur ABarreS: %e ",Spx->Iteration,Ecart); 
      printf(" ecart trop grand on refactorise la base\n"); 
    #endif				
    Spx->FactoriserLaBase = OUI_SPX;
		/*Spx->FaireDuRaffinementIteratif = NBITER_RAFFINEMENT;*/
		/* On augmente le seuil dual de pivotage */
 	  Spx->SeuilDePivotDual = COEFF_AUGMENTATION_VALEUR_DE_PIVOT_ACCEPTABLE * VALEUR_DE_PIVOT_ACCEPTABLE;	

    Spx->FaireChangementDeBase = NON_SPX;
		
	}	
	else {
	  Spx->FaireDuRaffinementIteratif = NBITER_RAFFINEMENT;				
    Spx->FaireChangementDeBase = NON_SPX;
 	  Spx->SeuilDePivotDual = COEFF_AUGMENTATION_VALEUR_DE_PIVOT_ACCEPTABLE * VALEUR_DE_PIVOT_ACCEPTABLE;		
	}
}

/*memset( (char *) Bs , 0 , NombreDeContraintes * sizeof( double ) );*/
{ int i;
  for ( i = 0 ; i < NombreDeContraintes ; i++ ) Bs[i] = 0;
}

return;
}

