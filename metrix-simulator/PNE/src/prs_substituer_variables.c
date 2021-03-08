/***********************************************************************

   FONCTION: On tente de faire des substitutions de variables grace aux
             contraintes d'égalite ne comprenant que 2 termes.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# define TRACES 0
  
# define ATILDE_MIN 1.e-10 /*1.e-5*/
# define ATILDE_MAX 1.e+10 /*1.e+5*/

# define SEUIL_INTEGRALITE 1.e-10

# define SEUIL_DE_PIVOT   1.e-6
# define SEUIL_MARKOWITZ  0.05
# define NOMBRE_DE_CHOIX_MARKOWITZ 5

# define SEUIL_TERMES_DUNE_LIGNE 1000  

# define NOMBRE_MAX_DE_VARIABLES_QUNE_VARIABLE_REMPLACEE_PEUT_SUBSTITUER  100 /*10*/ /* Le test est en > */

void PRS_InitMarkowitzSubstitutionDeVariables( int , char * , char * , int * , int * , int * , double * , int * , int * , int * ,
                                               int * , double * , double * , int * , int * , int * , int * , char * );
void PRS_MajMarkowitzSubstitutionDeVariables( int , char * , char * , int * , int * , int * , double * , int * , int * , int * ,
                                              int * , double * , double * , int * , int * , int * , int * , char * );
void PRS_SubstituerUneVariableSiEquationADeuxInconnes( PRESOLVE * , int , int , double , double , double , double );
char PRS_TestSubstituerUneVariableSiEquationADeuxInconnes( PRESOLVE * , int , int , double , double , double , int );					
void PRS_CalculerLesNouvellesBornesDeLaVariableRestante( PRESOLVE * , int , int , double , double , double );

/*----------------------------------------------------------------------------*/

void PRS_SubstituerVariables( PRESOLVE * Presolve, int * NbModifications )
{		       
int Cnt; int Var1; int Var2; double CoeffDeVar1; double CoeffDeVar2; int il; int ilMax; int il1;
int il2; PROBLEME_PNE * Pne; int NombreDeContraintes; char * ContrainteInactive; char * SensContrainte;
int * Mdeb; int * NbTerm; int * Nuvar; int * TypeDeBornePourPresolve; int * TypeDeVariableNative;
double * A; double * BorneInfPourPresolve; double * BorneSupPourPresolve; double * B; double BTilde;
double ATilde; double BCnt; char * ConserverLaBorneSupDuPresolve; char * ConserverLaBorneInfDuPresolve;
double S; double * ValeurDeXPourPresolve; double X; double * CoutLineaire; double BCntChoisi;
int * NumeroDesVariablesSubstituees; double * CoutDesVariablesSubstituees; double CoeffDeVar1Choisi;
double * ValeurDeLaConstanteDeSubstitution; int * IndiceDebutVecteurDeSubstitution; double CoeffDeVar2Choisi;
int * NbTermesVecteurDeSubstitution; double * CoeffDeSubstitution;
int * NumeroDeVariableDeSubstitution; int * IndexDansLeTypeDOperationDePresolve;
char * TypeDOperationDePresolve; int * ContrainteDeLaSubstitution;  
int * CorrespondanceCntPneCntEntree; int * NombreDeVariablesQueLaVariableSubstitue;

int NbCntADeuxTermes; int * NumeroDesCntADeuxTermes; char * CntDansLaListe; int * NbDeMarkowitzDeLaCnt; int Nb; int Var;
int * Cdeb; int * Csui; int * NumContrainte; int NombreDeVariables; int i; int MinMarkowitz; int IndexDeMinMarkowitz;
double MaxAi; int NbTermesUtiles; int * VarMarkowitzDeLaCnt; int Var1Choisi; int Var2Choisi; int CntChoisi;
double UnSurCoeffDeVar1;

*NbModifications = 0;

if ( Presolve->PremierPassageDansLePresolve != OUI_PNE ) {
  # if VERBOSE_PRS == 1
    printf(" On ne passe pas dans PRS_SubstituerVariables\n");
	# endif
  return;
}

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

if ( Pne->YaDesVariablesEntieres == NON_PNE ) {
  /* Si on est en continu, on ne sait pas (pour l'instant) recalculer exactement les variables
	   duales des contraintes quand on fait des substitutions de variables. Donc on prefere ne pas
		 faire ce genre de presolve. Todo: stocker toutes les transfromations de la matrice pour
		 recalculer exactement les variables duales quand on fait des substitutions de variables. */
	/* Par contre on peut le faire s'il y a des variables entieres car de toutes façons les variables
	   duales de la solution optimale ne peuvent pas etre interpretees comme des couts marginaux. */
  return;
}

if ( Pne->NbVariablesSubstituees >= Pne->NombreDeVariablesTrav - 1 ) return;
if ( Pne->NombreDOperationsDePresolve >= Pne->TailleTypeDOperationDePresolve ) return;

CoutLineaire = Pne->LTrav;
TypeDeVariableNative = Pne->TypeDeVariableTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;
SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
CorrespondanceCntPneCntEntree = Pne->CorrespondanceCntPneCntEntree;

ContrainteInactive = Presolve->ContrainteInactive;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
ValeurDeXPourPresolve = Presolve->ValeurDeXPourPresolve;
ConserverLaBorneSupDuPresolve = Presolve->ConserverLaBorneSupDuPresolve;
ConserverLaBorneInfDuPresolve = Presolve->ConserverLaBorneInfDuPresolve;
NombreDeVariablesQueLaVariableSubstitue = Presolve->NombreDeVariablesQueLaVariableSubstitue;

TypeDOperationDePresolve = Pne->TypeDOperationDePresolve;
IndexDansLeTypeDOperationDePresolve = Pne->IndexDansLeTypeDOperationDePresolve;

IndiceDebutVecteurDeSubstitution  = Pne->IndiceDebutVecteurDeSubstitution;
NumeroDesVariablesSubstituees     = Pne->NumeroDesVariablesSubstituees;
CoutDesVariablesSubstituees       = Pne->CoutDesVariablesSubstituees;
ContrainteDeLaSubstitution        = Pne->ContrainteDeLaSubstitution;
ValeurDeLaConstanteDeSubstitution = Pne->ValeurDeLaConstanteDeSubstitution; 
NbTermesVecteurDeSubstitution     = Pne->NbTermesVecteurDeSubstitution; 
CoeffDeSubstitution               = Pne->CoeffDeSubstitution;  
NumeroDeVariableDeSubstitution    = Pne->NumeroDeVariableDeSubstitution;

NombreDeVariables = Pne->NombreDeVariablesTrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;

NumeroDesCntADeuxTermes = (int *) malloc( NombreDeContraintes * sizeof( int ) );
NbDeMarkowitzDeLaCnt = (int *) malloc( NombreDeContraintes * sizeof( int ) );
CntDansLaListe = (char *) malloc( NombreDeContraintes * sizeof( char ) );
VarMarkowitzDeLaCnt = (int *) malloc( NombreDeContraintes * sizeof( int ) );
if ( NumeroDesCntADeuxTermes == NULL || NbDeMarkowitzDeLaCnt == NULL || CntDansLaListe == NULL || VarMarkowitzDeLaCnt == NULL ) {
  free( NumeroDesCntADeuxTermes );
  free( NbDeMarkowitzDeLaCnt );
  free( CntDansLaListe );
  free( VarMarkowitzDeLaCnt);
  return;
} 

PRS_InitMarkowitzSubstitutionDeVariables( NombreDeContraintes, ContrainteInactive, SensContrainte, Mdeb, NbTerm,
																				 Nuvar, A, Cdeb, Csui, NumContrainte, TypeDeBornePourPresolve, BorneInfPourPresolve ,
																				 BorneSupPourPresolve, &NbCntADeuxTermes, NumeroDesCntADeuxTermes,
                                         NbDeMarkowitzDeLaCnt, VarMarkowitzDeLaCnt, CntDansLaListe );				

# if TRACES == 1
  printf("NbCntADeuxTermes %d\n",NbCntADeuxTermes);
# endif

DebutDesSubstitutions:

/* On balaye la liste des contraintes a 2 termes utiles */
MinMarkowitz = 2 * NombreDeContraintes;
IndexDeMinMarkowitz = -1;

Var1Choisi = -1;
Var2Choisi = -1;
CoeffDeVar1Choisi = 1;
CoeffDeVar2Choisi = 1;
BCntChoisi = 0;
CntChoisi = -1;

Nb = 0;

for ( i = 0 ; i < NbCntADeuxTermes ; i++ ) {
  Cnt = NumeroDesCntADeuxTermes[i];
  if ( ContrainteInactive[Cnt] == OUI_PNE ) continue; /* Precaution */
  if ( NbDeMarkowitzDeLaCnt[i] >= MinMarkowitz ) continue;
	/* Verification du pivot */
	Var = VarMarkowitzDeLaCnt[i];

	if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) {
	  printf("Pb dans PRS_SubstituerVariables TypeDeBornePourPresolve[%d] = VARIABLE_FIXE  NbModifications = %d\n",Var,*NbModifications); /* regler ca si ca arrive */
	}
	
	MaxAi = -1;
	NbTermesUtiles = 0;
	il1 = -1;
	il2 = -1;
	Var1 = -1;
	Var2 = -1;
  S = 0;
  il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];	
  while ( il < ilMax ) {
		if ( NbTermesUtiles > 2 ) break;
    if ( A[il] != 0.0 ) {			 
		  if ( TypeDeBornePourPresolve[Nuvar[il]] == VARIABLE_FIXE ) S += A[il] * ValeurDeXPourPresolve[Nuvar[il]];		    
		  else {						
			  if ( fabs( A[il] ) > MaxAi ) MaxAi = fabs( A[il] );
				if ( Nuvar[il] == Var ) { il1 = il; Var1 = Nuvar[il1]; }
				else { il2 = il; Var2 = Nuvar[il2]; }			
			  NbTermesUtiles++;
			}
    }
		il++;
	}

	/*if ( NbTermesUtiles != 2 ) printf("CntCandidat n'a pas pas 2 termes utiles %d\n",Cnt);*/
	
  if ( il1 < 0 || il2 < 0 || NbTermesUtiles != 2 ) continue;
	if ( Var1 < 0 || Var2 < 0 ) continue;
	if ( NombreDeVariablesQueLaVariableSubstitue[Var1] > NOMBRE_MAX_DE_VARIABLES_QUNE_VARIABLE_REMPLACEE_PEUT_SUBSTITUER ) continue; /* On ne peut pas remplacer */	
	if ( TypeDeVariableNative[Var1] != TypeDeVariableNative[Var2] ) continue; 
	
  if ( fabs( A[il1] ) < SEUIL_DE_PIVOT ) continue;
  if ( fabs( A[il1] ) / MaxAi < SEUIL_MARKOWITZ ) continue;
	
  CoeffDeVar1 = A[il1];
	UnSurCoeffDeVar1 = 1. / CoeffDeVar1;
  CoeffDeVar2 = A[il2];
  BCnt = B[Cnt] - S;
	BTilde = BCnt / CoeffDeVar1;
  ATilde = CoeffDeVar2 / CoeffDeVar1;
  /* Si les nombres sont trop differents on ne prend pas a cause des imprecisions */
  if ( fabs( ATilde ) < ATILDE_MIN || fabs( ATilde ) > ATILDE_MAX  ) continue; 	
	if ( TypeDeVariableNative[Var1] == ENTIER && TypeDeVariableNative[Var2] == ENTIER ) {	
    if ( ceil( BTilde ) - BTilde > 1.e-10 && BTilde - floor( BTilde ) > SEUIL_INTEGRALITE ) continue; /* On ne peut pas remplacer */
    if ( ceil( ATilde ) - ATilde > 1.e-10 && ATilde - floor( ATilde ) > SEUIL_INTEGRALITE ) continue; /* On ne peut pas remplacer */
	}
  if ( PRS_TestSubstituerUneVariableSiEquationADeuxInconnes( Presolve, Var1, Var2, CoeffDeVar1, CoeffDeVar2, UnSurCoeffDeVar1, Cnt ) == NON_PNE ) continue;	
	
  MinMarkowitz = NbDeMarkowitzDeLaCnt[i];	
  IndexDeMinMarkowitz = i;
  Var1Choisi = Var1;
	Var2Choisi = Var2;
	CoeffDeVar1Choisi = CoeffDeVar1;
	CoeffDeVar2Choisi = CoeffDeVar2;
	BCntChoisi = BCnt;
  CntChoisi = Cnt;
	Nb++;
  if ( Nb > NOMBRE_DE_CHOIX_MARKOWITZ ) break;
	
}	
		
if ( CntChoisi < 0 ) goto FinDesSubstitutions;
if ( IndexDeMinMarkowitz < 0 ) goto FinDesSubstitutions;

Var1 = Var1Choisi;
Var2 = Var2Choisi;
CoeffDeVar1 =	CoeffDeVar1Choisi;
CoeffDeVar2 =	CoeffDeVar2Choisi;
BCnt = BCntChoisi;
Cnt = CntChoisi;

UnSurCoeffDeVar1 = 1. / CoeffDeVar1;
BTilde = BCnt / CoeffDeVar1;
ATilde = CoeffDeVar2 / CoeffDeVar1;


# if TRACES == 1
  printf("Variable %d substituee par la %d et mise en inactivite de la contrainte %d ATilde %lf BTilde %lf\n",Var1,Var2,Cnt,ATilde,BTilde); 
# endif

/* On supprime la contrainte de la liste des contraintes a 2 termes */
Cnt = NumeroDesCntADeuxTermes[IndexDeMinMarkowitz];
CntDansLaListe[Cnt] = 0;

i = NbCntADeuxTermes - 1;
NumeroDesCntADeuxTermes[IndexDeMinMarkowitz] = NumeroDesCntADeuxTermes[i];
NbDeMarkowitzDeLaCnt[IndexDeMinMarkowitz] = NbDeMarkowitzDeLaCnt[i];
VarMarkowitzDeLaCnt[IndexDeMinMarkowitz] = VarMarkowitzDeLaCnt[i];
NbCntADeuxTermes--;
 	
if ( Pne->NbVariablesSubstituees >= Pne->NombreDeVariablesTrav - 1 ) {
  printf("Fin par NbVariablesSubstituees = %d\n",Pne->NbVariablesSubstituees);
  goto FinDesSubstitutions;
}
if ( Pne->NombreDOperationsDePresolve >= Pne->TailleTypeDOperationDePresolve ) {
  printf("Fin par NombreDOperationsDePresolve = %d\n",Pne->NombreDOperationsDePresolve);
  goto FinDesSubstitutions;
}
	
TypeDOperationDePresolve[Pne->NombreDOperationsDePresolve] = SUBSITUTION_DE_VARIABLE;
IndexDansLeTypeDOperationDePresolve[Pne->NombreDOperationsDePresolve] = Pne->NbVariablesSubstituees;
Pne->NombreDOperationsDePresolve++;	
	
IndiceDebutVecteurDeSubstitution [Pne->NbVariablesSubstituees] = Pne->IndexLibreVecteurDeSubstitution;
NumeroDesVariablesSubstituees    [Pne->NbVariablesSubstituees] = Var1;
CoutDesVariablesSubstituees      [Pne->NbVariablesSubstituees] = CoutLineaire[Var1];	
ContrainteDeLaSubstitution       [Pne->NbVariablesSubstituees] = CorrespondanceCntPneCntEntree[Cnt]; 	
ValeurDeLaConstanteDeSubstitution[Pne->NbVariablesSubstituees] = BTilde; 
NbTermesVecteurDeSubstitution    [Pne->NbVariablesSubstituees] = 1;	
CoeffDeSubstitution           [Pne->IndexLibreVecteurDeSubstitution] = -ATilde;  
NumeroDeVariableDeSubstitution[Pne->IndexLibreVecteurDeSubstitution] = Var2;	
Pne->IndexLibreVecteurDeSubstitution++;		
Pne->NbVariablesSubstituees++;	
	
ConserverLaBorneSupDuPresolve[Var2] = OUI_PNE;
ConserverLaBorneInfDuPresolve[Var2] = OUI_PNE;
	
if ( NombreDeVariablesQueLaVariableSubstitue[Var1] + 1 > NombreDeVariablesQueLaVariableSubstitue[Var2] ) {
	NombreDeVariablesQueLaVariableSubstitue[Var2] = NombreDeVariablesQueLaVariableSubstitue[Var1] + 1;
}

ContrainteInactive[Cnt] = OUI_PNE;
*NbModifications = *NbModifications + 1;

PRS_SubstituerUneVariableSiEquationADeuxInconnes( Presolve, Var1, Var2, CoeffDeVar1, CoeffDeVar2, UnSurCoeffDeVar1, BCnt );						
	
/* Si necessaire on ajuste les bornes sur Var2 en fonction des bornes sur Var1 */	
PRS_CalculerLesNouvellesBornesDeLaVariableRestante( Presolve, Var1, Var2, CoeffDeVar1, CoeffDeVar2, BCnt );

CoutLineaire[Var1] = 0.0;
X = 0.; /* Pas d'importance mais ainsi on n'a pas besoin de la supprimer de la liste des 
           variables ni de la supprimer des contraintes */
PRS_FixerUneVariableAUneValeur( Presolve, Var1, X );

/* Pour toutes les contraintes qui ont Var2 (c'est la variable qui reste) on regarde si parmi les contraintes pas a 2 termes
   il y en a de nouvelles et on recalcule les nombres de Markowitz des contraintes a 2 termes qui ont ete impactees */

PRS_MajMarkowitzSubstitutionDeVariables( Var2, ContrainteInactive, SensContrainte, Mdeb, NbTerm, Nuvar, A, Cdeb, Csui, NumContrainte,
                                         TypeDeBornePourPresolve, BorneInfPourPresolve , BorneSupPourPresolve,																							
                                         &NbCntADeuxTermes, NumeroDesCntADeuxTermes, NbDeMarkowitzDeLaCnt, VarMarkowitzDeLaCnt,																							
                                         CntDansLaListe );			

goto DebutDesSubstitutions;

FinDesSubstitutions:
	
free( NumeroDesCntADeuxTermes );
free( NbDeMarkowitzDeLaCnt );
free( CntDansLaListe );
free( VarMarkowitzDeLaCnt);
	
#if VERBOSE_PRS || TRACES == 1
  printf("-> Nombre de contraintes supprimees par substitution de variables %d\n",*NbModifications); 
	printf("Reste NbCntADeuxTermes %d\n",NbCntADeuxTermes);
#endif

return;
}

/*----------------------------------------------------------------------------*/

void PRS_InitMarkowitzSubstitutionDeVariables( int NombreDeContraintes, char * ContrainteInactive,
                                               char * SensContrainte, int * Mdeb, int * NbTerm,
																					 	   int * Nuvar, double * A,
																						   int * Cdeb, int * Csui, int * NumContrainte,
                                               int * TypeDeBornePourPresolve, double * BorneInfPourPresolve , double * BorneSupPourPresolve,																							
                                               int * NbCntADeuxTermes_S,
                                               int * NumeroDesCntADeuxTermes,
                                               int * NbDeMarkowitzDeLaCnt,
	                                             int * VarMarkowitzDeLaCnt,																							
                                               char * CntDansLaListe )																																													
{
int Cnt; int NbCntADeuxTermes; int il; int ilMax; int NbTermesUtiles; int Var; int ic; 
int Var1; int Var2; int Nb1; int Nb2; int il1; int il2; int NbM; int VarM;
double Pivot; double PivotRejete; int NbRejete; int VarRejetee;

for ( Cnt = 0 ; Cnt < NombreDeContraintes; Cnt++ ) {
  CntDansLaListe[Cnt] = 0;
}

NbCntADeuxTermes = 0;

for ( Cnt = 0 ; Cnt < NombreDeContraintes; Cnt++ ) {
  if ( ContrainteInactive[Cnt] == OUI_PNE ) continue;
  if ( SensContrainte[Cnt] != '=' ) continue;
	Var1 = -1;
	Var2 = -1;
	il1 = -1;
	il2 = -1;
	il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
	NbTermesUtiles = 0;
  while ( il < ilMax ) {
	  if ( NbTermesUtiles > 2 ) break;
    if ( A[il] != 0.0 ) {
      Var = Nuvar[il];		
		  if ( TypeDeBornePourPresolve[Var] != VARIABLE_FIXE ) {
        if ( BorneInfPourPresolve[Var] != BorneSupPourPresolve[Var] ) {
				  if ( Var1 < 0 ) { Var1 = Var; il1 = il; }
					else { Var2 = Var; il2 = il; }
				  NbTermesUtiles++;
				}
			}
    }
    il++;
  }
	if ( NbTermesUtiles == 2 ) {
		NbDeMarkowitzDeLaCnt[NbCntADeuxTermes] = -1;
		VarMarkowitzDeLaCnt[NbCntADeuxTermes] = -1;
    /* Calcul des nombres de Markowitz */
    Nb1 = 0;
		ic = Cdeb[Var1];
		while ( ic >= 0 ) {
      if ( ContrainteInactive[NumContrainte[ic]] != OUI_PNE && A[ic] != 0 ) Nb1++;					  
		  ic = Csui[ic];
		}
    Nb2 = 0;
		ic = Cdeb[Var2];
		while ( ic >= 0 ) {
      if ( ContrainteInactive[NumContrainte[ic]] != OUI_PNE && A[ic] != 0 ) Nb2++;					  
		  ic = Csui[ic];
		}

		if ( Nb1 < Nb2 ) { NbM = Nb1; VarM = Var1; Pivot = fabs( A[il1] ); NbRejete = Nb2; VarRejetee = Var2; PivotRejete = fabs( A[il2] ); }
		else if ( Nb2 < Nb1 ) { NbM = Nb2; VarM = Var2; Pivot = fabs( A[il2] ); NbRejete = Nb1; VarRejetee = Var1; PivotRejete = fabs( A[il1] ); }
		else {
      if ( fabs( A[il1] ) > fabs( A[il2] ) ) { NbM = Nb1; VarM = Var1; Pivot = fabs( A[il1] ); NbRejete = Nb2; VarRejetee = Var2; PivotRejete = fabs( A[il2] ); }		 
		  else { NbM = Nb2; VarM = Var2; Pivot = fabs( A[il2] ); NbRejete = Nb1; VarRejetee = Var1; PivotRejete = fabs( A[il1] ); }
		}		
		/* Si le pivot choisi ne respecte pas les seuils mais que l'autre oui et que les nombres de MArkowitz ne different aue de 1
		   on echange les pivots */
		if ( fabs( Nb2 - Nb1 ) <= 1 && 0 ) {
      if ( fabs( Pivot ) < SEUIL_DE_PIVOT || fabs( Pivot ) / fabs( PivotRejete) < SEUIL_MARKOWITZ ) {
        if ( fabs( PivotRejete ) >= SEUIL_DE_PIVOT && fabs( PivotRejete ) / fabs( Pivot ) >= SEUIL_MARKOWITZ ) {
          /* On echange les pivots */  
					NbM = NbRejete;
					VarM = VarRejetee;
				}
		  }
		}
		
    CntDansLaListe[Cnt] = 1;
    NumeroDesCntADeuxTermes[NbCntADeuxTermes] = Cnt;
		NbDeMarkowitzDeLaCnt[NbCntADeuxTermes] = NbM;
		VarMarkowitzDeLaCnt[NbCntADeuxTermes] = VarM;		
    NbCntADeuxTermes++;
	}
}
*NbCntADeuxTermes_S = NbCntADeuxTermes;
return;
}

/*-------------------------------------------------------------------------------*/
/* On met a jour la liste des contraintes a 2 termes et les nombres de Markowitz */
void PRS_MajMarkowitzSubstitutionDeVariables( int VarRestante, char * ContrainteInactive,
                                              char * SensContrainte, int * Mdeb, int * NbTerm,
																						  int * Nuvar, double * A,
																						  int * Cdeb, int * Csui, int * NumContrainte,
                                              int * TypeDeBornePourPresolve, double * BorneInfPourPresolve , double * BorneSupPourPresolve,																							
                                              int * NbCntADeuxTermes_S,
                                              int * NumeroDesCntADeuxTermes,
                                              int * NbDeMarkowitzDeLaCnt,
	                                            int * VarMarkowitzDeLaCnt,																							
                                              char * CntDansLaListe )			
{
int Nb1; int ic; int il; int ilMax; int NbTermesUtiles; int Var; int il1; int il2; int Var2; int Cnt; int Nb2;
int NbM; int VarM; int Trouve; int NbCntADeuxTermes; int i; int iFin; int ic2; 
double Pivot; double PivotRejete; int NbRejete; int VarRejetee;

NbCntADeuxTermes = *NbCntADeuxTermes_S;

Nb1 = 0;
ic = Cdeb[VarRestante];
while ( ic >= 0 ) {
  Cnt = NumContrainte[ic];
  if ( ContrainteInactive[NumContrainte[ic]] != OUI_PNE && A[ic] != 0 ) Nb1++;
	if ( CntDansLaListe[Cnt] == 1 ) {
    /* Si la contrainte etait a 2 termes on l'enleve */
    CntDansLaListe[Cnt] = 0;
    Trouve = NON_PNE;
		for( i = 0 ; i < NbCntADeuxTermes ; i++ ) {
      if ( NumeroDesCntADeuxTermes[i] == Cnt ) {
        iFin= NbCntADeuxTermes - 1;
        NumeroDesCntADeuxTermes[i] = NumeroDesCntADeuxTermes[iFin];
        NbDeMarkowitzDeLaCnt[i] = NbDeMarkowitzDeLaCnt[iFin];
        VarMarkowitzDeLaCnt[i] = VarMarkowitzDeLaCnt[iFin];
        NbCntADeuxTermes--;				                  
        Trouve = OUI_PNE;
			}
		}
		if ( Trouve == NON_PNE ) {
      printf("1- Attention BUG dans PRS_MajMarkowitzSubstituionDeVariables !!!\n");
			exit(0);
		}		 
	}
	ic = Csui[ic];
}

ic = Cdeb[VarRestante];
while ( ic >= 0 ) {
  Cnt = NumContrainte[ic];
  if ( SensContrainte[Cnt] != '=' ) goto NextIc;
  if ( ContrainteInactive[Cnt] == OUI_PNE || A[ic] == 0 ) goto NextIc;
	/* Contrainte a 2 termes ? */
	Var2 = -1;
	il1 = -1;
	il2 = -1;
	il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
	NbTermesUtiles = 0;	
  while ( il < ilMax ) {
	  if ( NbTermesUtiles > 2 ) break;
    if ( A[il] != 0.0 ) {
      Var = Nuvar[il];		
		  if ( TypeDeBornePourPresolve[Var] != VARIABLE_FIXE ) {
        if ( BorneInfPourPresolve[Var] != BorneSupPourPresolve[Var] ) {
				  if ( Var == VarRestante ) { il1 = il; }
					else { Var2 = Var; il2 = il; }
				  NbTermesUtiles++;
				}
			}
    }
    il++;
  }	
	if ( NbTermesUtiles == 2 ) {
	
	  /* On calcule le nombre de markowitz */
    Nb2 = 0;
		ic2 = Cdeb[Var2];
		while ( ic2 >= 0 ) {		
      if ( ContrainteInactive[NumContrainte[ic2]] != OUI_PNE && A[ic2] != 0 ) Nb2++;					  
		  ic2 = Csui[ic2];
		}
		
		if ( Nb1 < Nb2 ) { NbM = Nb1; VarM = VarRestante; Pivot = fabs( A[il1] ); NbRejete = Nb2; VarRejetee = Var2; PivotRejete = fabs( A[il2] ); }
		else if ( Nb2 < Nb1 ) { NbM = Nb2; VarM = Var2; Pivot = fabs( A[il2] ); NbRejete = Nb1; VarRejetee = VarRestante; PivotRejete = fabs( A[il1] ); }
		else {
      if ( fabs( A[il1] ) > fabs( A[il2] ) ) { NbM = Nb1; VarM = VarRestante; Pivot = fabs( A[il1] ); NbRejete = Nb2; VarRejetee = Var2; PivotRejete = fabs( A[il2] ); }		 
		  else { NbM = Nb2; VarM = Var2; Pivot = fabs( A[il2] ); NbRejete = Nb1; VarRejetee = VarRestante; PivotRejete = fabs( A[il1] ); }
		}		
		/* Si le pivot choisi ne respecte pas les seuils mais que l'autre oui et que les nombres de Markowitz ne different aue de 1
		   on echange les pivots */
		if ( fabs( Nb2 - Nb1 ) <= 1 && 0 ) {
      if ( fabs( Pivot ) < SEUIL_DE_PIVOT || fabs( Pivot ) / fabs( PivotRejete) < SEUIL_MARKOWITZ ) {
        if ( fabs( PivotRejete ) >= SEUIL_DE_PIVOT && fabs( PivotRejete ) / fabs( Pivot ) >= SEUIL_MARKOWITZ ) {
          /* On echange les pivots */
					NbM = NbRejete;
					VarM = VarRejetee;
				}
		  }
		}
		if ( CntDansLaListe[Cnt] == 1 ) {
      /* Mise a jour */
      Trouve = NON_PNE;
			for( i = 0 ; i < NbCntADeuxTermes ; i++ ) {
        if ( NumeroDesCntADeuxTermes[i] == Cnt ) {
          NumeroDesCntADeuxTermes[i] = Cnt;
		      NbDeMarkowitzDeLaCnt[i] = NbM;
		      VarMarkowitzDeLaCnt[i] = VarM;          
          Trouve = OUI_PNE;
				}
			}
			if ( Trouve == NON_PNE ) {
        printf("2- Attention BUG dans PRS_MajMarkowitzSubstituionDeVariables !!!\n");
				exit(0);
			}
		}
		else {
      CntDansLaListe[Cnt] = 1;
      NumeroDesCntADeuxTermes[NbCntADeuxTermes] = Cnt;
		  NbDeMarkowitzDeLaCnt[NbCntADeuxTermes] = NbM;
		  VarMarkowitzDeLaCnt[NbCntADeuxTermes] = VarM;
		  NbCntADeuxTermes++;		
		}	
	}
	NextIc:
	ic = Csui[ic];
}

*NbCntADeuxTermes_S = NbCntADeuxTermes;

return;
}

/*----------------------------------------------------------------------------*/

void PRS_CalculerLesNouvellesBornesDeLaVariableRestante( PRESOLVE * Presolve, int VarSubstituee, int VarRestante,
                                                         double CoeffDeVarSubstituee, double CoeffDeVarRestante,
																												 double BCnt )
{
int TypeBorne; char XmaxExiste; char XminExiste; double Xmax; double Xmin; double ATilde;
double X; PROBLEME_PNE * Pne;

Pne = Presolve->ProblemePneDuPresolve;

TypeBorne = Presolve->TypeDeBornePourPresolve[VarSubstituee];
XmaxExiste = NON_PNE;
XminExiste = NON_PNE;
Xmin = 0;
Xmax = 0;
ATilde = -CoeffDeVarRestante / CoeffDeVarSubstituee;

if ( ATilde > 0.0 ) {
  if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
    /*Xmax = ( Presolve->BorneSupPourPresolve[VarSubstituee] - BTilde ) / ATilde;*/
    Xmax = -( ( Presolve->BorneSupPourPresolve[VarSubstituee] * CoeffDeVarSubstituee ) - BCnt ) / CoeffDeVarRestante;		
		XmaxExiste = OUI_PNE;
	}
	if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {	
    /*Xmin = ( Presolve->BorneInfPourPresolve[VarSubstituee] - BTilde ) / ATilde;*/
    Xmin = -( ( Presolve->BorneInfPourPresolve[VarSubstituee] * CoeffDeVarSubstituee ) - BCnt ) / CoeffDeVarRestante;				
    XminExiste = OUI_PNE;
	}
}
else {
  if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {
    /*Xmax = ( Presolve->BorneInfPourPresolve[VarSubstituee] - BTilde ) / ATilde;*/
    Xmax = -( ( Presolve->BorneInfPourPresolve[VarSubstituee] * CoeffDeVarSubstituee ) - BCnt ) / CoeffDeVarRestante;						
		XmaxExiste = OUI_PNE;
	}
	if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
    /*Xmin = ( Presolve->BorneSupPourPresolve[VarSubstituee] - BTilde ) / ATilde;*/
    Xmin = -( ( Presolve->BorneSupPourPresolve[VarSubstituee] * CoeffDeVarSubstituee ) - BCnt ) / CoeffDeVarRestante;				
    XminExiste = OUI_PNE;
	}
}

TypeBorne = Presolve->TypeDeBornePourPresolve[VarRestante];
if ( XminExiste == OUI_PNE ) {
	if ( TypeBorne == VARIABLE_NON_BORNEE || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
    Presolve->BorneInfPourPresolve[VarRestante] = Xmin;
		if ( TypeBorne == VARIABLE_NON_BORNEE ) TypeBorne = VARIABLE_BORNEE_INFERIEUREMENT;
		else if ( TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeBorne = VARIABLE_BORNEE_DES_DEUX_COTES;
	}
	else if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {
    if ( Xmin > Presolve->BorneInfPourPresolve[VarRestante] ) {
		  Presolve->BorneInfPourPresolve[VarRestante] = Xmin;
		}
	}    
}
if ( XmaxExiste == OUI_PNE ) {
	if ( TypeBorne == VARIABLE_NON_BORNEE || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {
    Presolve->BorneSupPourPresolve[VarRestante] = Xmax;
		if ( TypeBorne == VARIABLE_NON_BORNEE ) TypeBorne = VARIABLE_BORNEE_SUPERIEUREMENT;
		else if ( TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) TypeBorne = VARIABLE_BORNEE_DES_DEUX_COTES;
	}
	else if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
    if ( Xmax < Presolve->BorneSupPourPresolve[VarRestante] ) {
		  Presolve->BorneSupPourPresolve[VarRestante] = Xmax;
		}
	} 
}

Presolve->TypeDeBornePourPresolve[VarRestante] = TypeBorne;
if ( Pne->TypeDeVariableTrav[VarRestante] == ENTIER ) {	
  if ( Presolve->BorneSupPourPresolve[VarRestante] < Pne->UmaxTrav[VarRestante] - 1.e-7 && 
       Presolve->BorneInfPourPresolve[VarRestante] > Pne->UminTrav[VarRestante] + 1.e-7 ) {
	  Pne->YaUneSolution = PROBLEME_INFAISABLE;
		return;
	}
  if ( Presolve->BorneSupPourPresolve[VarRestante] < Pne->UmaxTrav[VarRestante] - 1.e-7 ) {
	  X = Pne->UminTrav[VarRestante];
    PRS_FixerUneVariableAUneValeur( Presolve, VarRestante, X );   
	}
	else if ( Presolve->BorneInfPourPresolve[VarRestante] > Pne->UminTrav[VarRestante] + 1.e-7 ) {
	  X = Pne->UmaxTrav[VarRestante];
    PRS_FixerUneVariableAUneValeur( Presolve, VarRestante, X );   
	}
}

/* 14/01/2015: Il faut reajuster ConserverLaBorneSupDuPresolve et ConserverLaBorneInfDuPresolve */
if ( Presolve->TypeDeBornePourPresolve[VarRestante] == VARIABLE_BORNEE_INFERIEUREMENT ) {
	Presolve->ConserverLaBorneSupDuPresolve[VarRestante] = NON_PNE;
}
else if ( Presolve->TypeDeBornePourPresolve[VarRestante] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
	Presolve->ConserverLaBorneInfDuPresolve[VarRestante] = NON_PNE;
}
else if ( Presolve->TypeDeBornePourPresolve[VarRestante] == VARIABLE_NON_BORNEE ) {
	Presolve->ConserverLaBorneSupDuPresolve[VarRestante] = NON_PNE;
	Presolve->ConserverLaBorneInfDuPresolve[VarRestante] = NON_PNE;
}

return;
}

/*----------------------------------------------------------------------------*/

char PRS_TestSubstituerUneVariableSiEquationADeuxInconnes( PRESOLVE * Presolve, int VarSubstituee, int VarRestante, 
                                                           double CoeffDeVarSubstituee, double CoeffDeVarVarRestante,
                                                           double UnSurCoeffDeVarSubstituee, int CntSubstitution )																												 
{
int il ; double Ai; int Cnt; int ilCnt; int ilMaxCnt; char VarRestanteTrouvee; 
double X ; PROBLEME_PNE * Pne; int * Cdeb; int * NumContrainte; int * Mdeb;
int * NbTerm; int * Nuvar; int * Csui; double * A; char * ContrainteInactive;
double PlusGrandTerme; double PlusPetitTerme; int SeuilNbTermes;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
 
# if TRACES != 1
  CntSubstitution = -1; /* Pour ne pas avoir de warning a la compilation */
# endif

SeuilNbTermes = (int) ceil( 0.1 * Pne->NombreDeVariablesTrav );
if ( SeuilNbTermes < SEUIL_TERMES_DUNE_LIGNE ) SeuilNbTermes = SEUIL_TERMES_DUNE_LIGNE;

Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
A = Pne->ATrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
PlusGrandTerme = Pne->PlusGrandTerme;
PlusPetitTerme = Pne->PlusPetitTerme;

ContrainteInactive = Presolve->ContrainteInactive;

/* On balaye la colonne de la variable substituee */
il  = Cdeb[VarSubstituee];
while ( il >= 0 ) {   
  Cnt = NumContrainte[il];	
  if ( ContrainteInactive[Cnt] == OUI_PNE ) goto Next_il;
  VarRestanteTrouvee = NON_PNE;  
  Ai = A[il];
	if ( Ai == 0 ) goto Next_il;
  /* On balaye la contrainte pour faire les actions necessaires au cas ou 
     la variable VarRestante y serait deja presente */
		 
	if ( NbTerm[Cnt] > SeuilNbTermes ) goto Next_il; /*return( NON_PNE );*/
	
  ilCnt    = Mdeb[Cnt];
  ilMaxCnt = ilCnt + NbTerm[Cnt];
  while ( ilCnt < ilMaxCnt ) {
	  if ( A[ilCnt] == 0.0 ) goto Next_ilCnt;
    if ( Nuvar[ilCnt] == VarRestante ) {				
      X =  fabs( UnSurCoeffDeVarSubstituee * ( ( A[ilCnt] * CoeffDeVarSubstituee ) - ( Ai * CoeffDeVarVarRestante ) ) );			    			
      if ( X > PlusGrandTerme || ( X < PlusPetitTerme && X != 0.0 ) ) {
        /* On refuse la substitution de variable */
	      # if TRACES == 1
	        printf("refus 1 car Cnt %d CntSubstitution %d X %e PlusGrandTerme %e PlusPetitTerme %e ATrav %e Ai %e \n",
					        Cnt,CntSubstitution,X,PlusGrandTerme,PlusPetitTerme,Pne->ATrav[ilCnt],Ai);
	      # endif				
	      return( NON_PNE );
      }      			
      VarRestanteTrouvee = OUI_PNE;
      break;  
    }
		Next_ilCnt:
    ilCnt++;
  }	
  if ( VarRestanteTrouvee == NON_PNE ) {  
    X = fabs( -UnSurCoeffDeVarSubstituee * ( Ai * CoeffDeVarVarRestante ) );				
    if ( X > PlusGrandTerme || ( X < PlusPetitTerme && X != 0.0 ) ) {
      /* On refuse la substitution de variable */
      # if TRACES == 1
        printf("refus 2 car Cnt %d CntSubstitution %d X = %e Ai = %e \n",Cnt,CntSubstitution,X,Ai);
      # endif			
      return( NON_PNE );
    }                
  }  
  Next_il:
  il = Csui[il];    
}

return( OUI_PNE );
}  

/*----------------------------------------------------------------------------*/

void PRS_SubstituerUneVariableSiEquationADeuxInconnes( PRESOLVE * Presolve, int VarSubstituee, int VarRestante, 
                                                       double CoeffDeVarSubstituee, double CoeffDeVarVarRestante,
                                                       double UnSurCoeffDeVarSubstituee, double BCnt )														
{
int il ; double Ai; int Cnt; int ilCnt; int ilMaxCnt; char VarRestanteTrouvee; int ilR; int ilPrec;
PROBLEME_PNE * Pne; int * Cdeb; int * NumContrainte; int * Mdeb; int * NbTerm; int * Nuvar;
int * Csui; double * A; double * B; char * ContrainteInactive; double * CoutLineaire; 

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

/* On balaye la colonne de la variable substituee */

Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
CoutLineaire = Pne->LTrav;
NumContrainte = Pne->NumContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
B = Pne->BTrav;
ContrainteInactive = Presolve->ContrainteInactive;

il  = Cdeb[VarSubstituee];
while ( il >= 0 ) {
  Cnt = NumContrainte[il];	
  VarRestanteTrouvee = NON_PNE;	
  /* Pas de substitution sur des contraintes inactives */	
  Ai = A[il];	
  if ( ContrainteInactive[Cnt] != OUI_PNE && Ai != 0.0 ) {	
    /* On balaye la contrainte pour faire les actions necessaires au cas ou 
       la variable VarRestante y serait deja presente */
    ilCnt    = Mdeb[Cnt];
    ilMaxCnt = ilCnt + NbTerm[Cnt];
    while ( ilCnt < ilMaxCnt ) {
      if ( Nuvar[ilCnt] == VarRestante ) {							
				A[ilCnt] = UnSurCoeffDeVarSubstituee * ( ( A[ilCnt] * CoeffDeVarSubstituee ) - ( Ai * CoeffDeVarVarRestante ) );				
        VarRestanteTrouvee = OUI_PNE;  
        break;
      } 
      ilCnt++;
    }		
    B[Cnt] = UnSurCoeffDeVarSubstituee * ( ( B[Cnt]* CoeffDeVarSubstituee ) - ( Ai * BCnt ) );		
  }

  ilPrec = il;
  il = Csui[il];

  if ( VarRestanteTrouvee == NON_PNE && ContrainteInactive[Cnt] != OUI_PNE && Ai != 0.0 ) {
	  /* La variable restante ne figurait pas dans la contrainte: il y a creation d'un terme
		   dans la colonne VarRestante */
    Nuvar[ilPrec] = VarRestante;		
    A[ilPrec] = -UnSurCoeffDeVarSubstituee * ( Ai * CoeffDeVarVarRestante );		
    /* Attention comme on a change la colonne d'un terme il faut modifier le chainage 
       de la colonne VarRestante */
    ilR = Cdeb[VarRestante];
    Cdeb[VarRestante] = ilPrec; 
    Csui[ilPrec] = ilR;         
  }
  else {
    /* On peut mettre le coefficient de la variable substituee à 0 s'il y avait la 
       variable restante dans la contrainte */		
    A[ilPrec] = 0.;  
  }
	
}

CoutLineaire[VarRestante] = UnSurCoeffDeVarSubstituee * ( ( CoutLineaire[VarRestante] * CoeffDeVarSubstituee ) - ( CoutLineaire[VarSubstituee] * CoeffDeVarVarRestante ) );

Pne->Z0 += CoutLineaire[VarSubstituee] * BCnt * UnSurCoeffDeVarSubstituee;

Cdeb[VarSubstituee] = -1;

return;
}  

/*----------------------------------------------------------------------------*/

