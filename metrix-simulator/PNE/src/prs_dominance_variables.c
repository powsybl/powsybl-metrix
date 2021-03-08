/***********************************************************************

   FONCTION: Presolve dominance des variables.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_fonctions.h"  
# include "pne_define.h"

# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "prs_memoire.h"
# endif

# define TRACES 1

void PRS_DominanceDUneVariablePreparerUnVecteurColonne( PROBLEME_PNE * , PRESOLVE * , int , double * , int * , char * , int * );

void PRS_TesterLaDominanceDUneVariableParRapportAUneAutre( PROBLEME_PNE * , PRESOLVE * , int , int , int * , int * , double * , int * , char * , int );

/*----------------------------------------------------------------------------*/

void PRS_TesterLaDominanceDUneVariableParRapportAUneAutre( PROBLEME_PNE * Pne, PRESOLVE * Presolve,
																											     int Var1, int Var2,
																													 /* En retour le numero de la variable en premier.
																													    Valeurs negatives si aucune ne domine l'autre. */
																													 int * VarDominante, int * VarDominee,
																													 /* Vecteurs de travail */
																													 double * W, int * IndexDeWNonNul, char * T, int NbNonNuls )
{
int ic; int Cnt; int i; int Nb; int * Cdeb; int * Csui; int * NumContrainte; double * A; char * ContrainteInactive;
char Sens; char * SensContrainte;

ContrainteInactive = Presolve->ContrainteInactive;
Sens = '?';
if ( Pne->LTrav[Var1] < Pne->LTrav[Var2] ) Sens = '<';
else if ( Pne->LTrav[Var1] > Pne->LTrav[Var2] ) Sens = '>';
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;
SensContrainte = Pne->SensContrainteTrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;
*VarDominante = -1;
*VarDominee = -1; 
Nb = NbNonNuls;
ic = Cdeb[Var2];
while ( ic >= 0 ) {
  Cnt = NumContrainte[ic];
  if ( ContrainteInactive[Cnt] == OUI_PNE ) goto NextIc;
	if ( Sens == '<' ) {
	  if ( SensContrainte[Cnt] == '<' ) {
      if ( W[Cnt] > A[ic] ) goto FinTestDominance;
		}
		else if ( W[Cnt] != A[ic] ) goto FinTestDominance;
	}
	else if ( Sens == '>' ) {
	  if ( SensContrainte[Cnt] == '<' ) {	
      if ( W[Cnt] < A[ic] ) goto FinTestDominance;
		}
		else if ( W[Cnt] != A[ic] ) goto FinTestDominance;		
	}
	else { /* Sens = ? */
	  if ( SensContrainte[Cnt] == '<' ) {	
	    if ( W[Cnt] < A[ic] ) Sens = '<';
      else if ( W[Cnt] > A[ic] ) Sens = '>';
		}
		else if ( W[Cnt] != A[ic] ) goto FinTestDominance;				
	}
	if ( T[Cnt] == 1 ) { T[Cnt] = 0 ; Nb--; }
	NextIc:
  ic = Csui[ic];
}
if ( Nb != 0 ) {
  for ( i = 0 ; i < NbNonNuls ; i++ ) {
	  Cnt = IndexDeWNonNul[i];
	  if ( T[Cnt] == 0 ) continue;		
    if ( Sens == '<' ) {
	    if ( SensContrainte[Cnt] == '<' ) {		
        if ( W[Cnt] > 0 ) goto FinTestDominance;
			}
			else if ( W[Cnt] != 0 ) goto FinTestDominance;
		}
  	else if ( Sens == '>' ) {
	    if ( SensContrainte[Cnt] == '<' ) {		
        if ( W[Cnt] < 0 ) goto FinTestDominance;
			}
			else if ( W[Cnt] != 0 ) goto FinTestDominance;			
	  }
	  else { /* Sens = ? */
	    if ( SensContrainte[Cnt] == '<' ) {	
	      if ( W[Cnt] < 0 ) Sens = '<';
        else if ( W[Cnt] > 0 ) Sens = '>';
		  }
		  else if ( W[Cnt] != 0 ) goto FinTestDominance;				
	  }		
	}
}
/* Si on arrive ici c'est qu'on peut tirer une conclusion */
if ( Sens == '<' ) { *VarDominante = Var1; *VarDominee = Var2; }
else if ( Sens == '>' ) { *VarDominante = Var2; *VarDominee = Var1; }

FinTestDominance:

for ( i = 0 ; i < NbNonNuls ; i++ ) T[IndexDeWNonNul[i]] = 1;		

return;
}

/*----------------------------------------------------------------------------*/

void PRS_DominanceDUneVariablePreparerUnVecteurColonne( PROBLEME_PNE * Pne, PRESOLVE * Presolve, int Var, double * W, int * IndexDeWNonNul, char * T, int * NbNonNuls )
{
int ic; int Cnt; int Nb; int * Cdeb; int * Csui; int * NumContrainte; double * A; char * SensContrainte; char * ContrainteInactive;
ContrainteInactive = Presolve->ContrainteInactive;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;
SensContrainte = Pne->SensContrainteTrav;
Nb = 0;
ic = Cdeb[Var];
while ( ic >= 0 ) {
  Cnt = NumContrainte[ic];
  if ( ContrainteInactive[Cnt] == OUI_PNE ) goto NextIc;
  W[Cnt] = A[ic];
	IndexDeWNonNul[Nb] = Cnt;
	T[Cnt] = 1;
	Nb++;
	NextIc:
  ic = Csui[ic];  
}
*NbNonNuls = Nb;
return;
}

/*----------------------------------------------------------------------------*/

void PRS_TesterLaDominanceDesVariables( PRESOLVE * Presolve )
{
double * W; char * T; int VarDominante; int VarDominee; int Var1; int Var2; int TypeBrnVar1; int TypeBrnVar2;
int NombreDeVariables; int * TypeDeVariable; int * TypeDeBornePourPresolve; int * IndexDeWNonNul;
char * TypeDeValeurDeBorneInf; char * TypeDeValeurDeBorneSup; int * ContrainteBornanteInferieurement;
int * ContrainteBornanteSuperieurement; int * TypeDeBorneNative; int NbNonNuls; double * BorneInfPourPresolve;
double * BorneSupPourPresolve; double * BorneInfNative; double * BorneSupNative; char * ConserverLaBorneInfDuPresolve;
char * ConserverLaBorneSupDuPresolve; PROBLEME_PNE * Pne; int i; int Cnt;

return; /* Je le laisse pour memoire mais a mon avis, apres tests, il semble que ca seet a rien */

printf("PRS_TesterLaDominanceDesVariables \n");


Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

NombreDeVariables = Pne->NombreDeVariablesTrav;

W = (double *) malloc( Pne->NombreDeContraintesTrav * sizeof( double ) );
IndexDeWNonNul = (int *) malloc( Pne->NombreDeContraintesTrav * sizeof( int ) );
T = (char *) malloc( Pne->NombreDeContraintesTrav * sizeof( char ) );
if ( W == NULL || IndexDeWNonNul == NULL || T == NULL ) { free( W ); free( IndexDeWNonNul ); free( T ); return; }

memset( (char *) W, 0 , Pne->NombreDeContraintesTrav * sizeof( double ));
memset( (char *) T, 0 , Pne->NombreDeContraintesTrav * sizeof( char ));

TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorneNative = Pne->TypeDeBorneTrav;
BorneInfNative = Pne->UminTrav;
BorneSupNative = Pne->UmaxTrav;

TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;
TypeDeValeurDeBorneInf = Presolve->TypeDeValeurDeBorneInf;
TypeDeValeurDeBorneSup = Presolve->TypeDeValeurDeBorneSup;
ContrainteBornanteInferieurement = Presolve->ContrainteBornanteInferieurement;
ContrainteBornanteSuperieurement = Presolve->ContrainteBornanteSuperieurement;
ConserverLaBorneInfDuPresolve = Presolve->ConserverLaBorneInfDuPresolve;
ConserverLaBorneSupDuPresolve = Presolve->ConserverLaBorneSupDuPresolve;


for ( Var1 = 0 ; Var1 < NombreDeVariables ; Var1++ ) {

  if ( TypeDeVariable[Var1] == ENTIER ) continue;
  if ( TypeDeBornePourPresolve[Var1] == VARIABLE_FIXE ) continue;

  TypeBrnVar1 = PRS_ChoisirLaBorneLaPlusContraignantePourLesVariablesDuales( 
                      TypeDeBorneNative[Var1], TypeDeBornePourPresolve[Var1],
										  BorneInfPourPresolve[Var1], BorneInfNative[Var1],
										  BorneSupPourPresolve[Var1], BorneSupNative[Var1],
										  ConserverLaBorneInfDuPresolve[Var1], ConserverLaBorneSupDuPresolve[Var1],
										  TypeDeValeurDeBorneInf[Var1], TypeDeValeurDeBorneSup[Var1] );

  if ( TypeBrnVar1 == VARIABLE_BORNEE_INFERIEUREMENT ) continue;
												
  PRS_DominanceDUneVariablePreparerUnVecteurColonne( Pne, Presolve, Var1, W, IndexDeWNonNul, T, &NbNonNuls );

  for ( Var2 = Var1 + 1 ; Var2 < NombreDeVariables ; Var2++ ) {
    if ( TypeDeVariable[Var2] == ENTIER ) continue;

    if ( TypeDeBornePourPresolve[Var2] == VARIABLE_FIXE ) continue;

    TypeBrnVar2 = PRS_ChoisirLaBorneLaPlusContraignantePourLesVariablesDuales( 
                        TypeDeBorneNative[Var2], TypeDeBornePourPresolve[Var2],
										    BorneInfPourPresolve[Var2], BorneInfNative[Var2],
									  	  BorneSupPourPresolve[Var2], BorneSupNative[Var2],
										    ConserverLaBorneInfDuPresolve[Var2], ConserverLaBorneSupDuPresolve[Var2],
										    TypeDeValeurDeBorneInf[Var2], TypeDeValeurDeBorneSup[Var2] );

    if ( TypeBrnVar2 != VARIABLE_BORNEE_INFERIEUREMENT ) continue;
	
    PRS_TesterLaDominanceDUneVariableParRapportAUneAutre( Pne, Presolve, Var1, Var2, &VarDominante, &VarDominee, W, IndexDeWNonNul, T, NbNonNuls );

		if ( VarDominante >= 0 && VarDominee >= 0 ) {
      # if TRACES == 1
				printf("Dominance: VarDominante %d VarDominee %d \n",VarDominante,VarDominee);
      # endif
			/* On fixe VarDominee a min */

      PRS_FixerUneVariableAUneValeur( Presolve, VarDominee, BorneInfPourPresolve[VarDominee] );
			
		}			
  }

  for ( i = 0 ; i < NbNonNuls ; i++ ) {
	  Cnt = IndexDeWNonNul[i];
		W[Cnt] = 0;
	  T[Cnt] = 0;		
  }	
	
}

free( W );
free( IndexDeWNonNul );
free( T );

return;
}
