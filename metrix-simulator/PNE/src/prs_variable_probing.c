/***********************************************************************

   FONCTION: Pour les colonnes singleton qui correspondent a des variables
	           bornees des 2 cotes on teste une instanciation sur chaque borne.
						 Si une instanciation n'est pas realisable on en deduit une borne
						 sur la variable duale de la contrainte dans laquelle la variable
						 apparait.
                
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

# define EPS 1.e-3

# define TRACES 0

# if VARIABLE_PROBING_SUR_VARIABLES_BORNEES_DES_2_COTES == OUI_PNE

void PRS_InstanciationSurUneBorne( PRESOLVE * , int , int , double , double , char * , char * , int * , int * , int * );
int PRS_ColonneSingleton( PRESOLVE * , int , double * );
void PRS_AnalyserLesContraintesdeLaListe( PRESOLVE * , char * , char * , int , int * );
void PRS_ReinitialiserLesDonnees( int , int * , double * , double * , double * , double * );

/*----------------------------------------------------------------------------*/

int PRS_ColonneSingleton( PRESOLVE * Presolve, int Var , double * Ai )
{
int NbTermes; int il; int Cnt; int CntDeLaVariable; int ContrainteDeLaVariable;
int * Cdeb; int * Csui; int * NumContrainte; char * ContrainteInactive;
double * A; PROBLEME_PNE * Pne;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
A = Pne->ATrav;
ContrainteInactive = Presolve->ContrainteInactive;
NbTermes = 0;
ContrainteDeLaVariable = -1;	
*Ai = 1;
il  = Cdeb[Var];
while ( il >= 0 ) {
  Cnt = NumContrainte[il]; 
  if ( ContrainteInactive[Cnt] == NON_PNE ) {
    if ( A[il] != 0.0 ) { 
      NbTermes++; 
	    CntDeLaVariable = Cnt;
      if ( NbTermes > 1 ) { ContrainteDeLaVariable = -1; break; }
	    ContrainteDeLaVariable = Cnt;
		  *Ai = A[il];
    }
  }
  il = Csui[il]; 
}
return( ContrainteDeLaVariable );
}

/*----------------------------------------------------------------------------*/

void PRS_ProbingSurLesColonnesSingleton( PRESOLVE * Presolve, int * NbModifications )
{
int Var; int * TypeDeVariable; char TypeBrnPresolve; int * TypeDeBornePourPresolve; double * CoutLineaire;
double * BorneInfPourPresolve; double * BorneSupPourPresolve; double Ai; char Admissibilite; int NombreDeVariables;
int ContrainteDeLaVariable; PROBLEME_PNE * Pne; char * T; int * ListeDesContraintes; int NbBornesModifiees;
int * ListeDesBornesModifiees; double * BorneInfPourPresolveSv; double * BorneSupPourPresolveSv;

*NbModifications = 0;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
CoutLineaire = Pne->LTrav;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;

T = (char *) malloc( Pne->NombreDeContraintesTrav * sizeof( char ) );
ListeDesContraintes = (int *) malloc( Pne->NombreDeContraintesTrav * sizeof( int ) );
ListeDesBornesModifiees = (int *) malloc( NombreDeVariables * sizeof( int ) );
BorneInfPourPresolveSv = (double *) malloc( NombreDeVariables * sizeof( double ) );
BorneSupPourPresolveSv = (double *) malloc( NombreDeVariables * sizeof( double ) );

if ( T == NULL || ListeDesContraintes == NULL || ListeDesBornesModifiees == NULL ||
     BorneInfPourPresolveSv == NULL || BorneSupPourPresolveSv == NULL ) {
  free( T );
	free( ListeDesContraintes );
	free( ListeDesBornesModifiees );
	free( BorneInfPourPresolveSv );
	free( BorneSupPourPresolveSv );
	return;
}
memset( (char *) T, 0, Pne->NombreDeContraintesTrav * sizeof( char ) );
memcpy( (char *) BorneInfPourPresolveSv, (char *) BorneInfPourPresolve, NombreDeVariables * sizeof( double ) );
memcpy( (char *) BorneSupPourPresolveSv, (char *) BorneSupPourPresolve, NombreDeVariables * sizeof( double ) );

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( TypeDeVariable[Var] == ENTIER ) continue;
  TypeBrnPresolve = TypeDeBornePourPresolve[Var];
  if ( TypeBrnPresolve == VARIABLE_FIXE ) continue;
  if ( TypeBrnPresolve != VARIABLE_BORNEE_DES_DEUX_COTES ) continue;

	ContrainteDeLaVariable = PRS_ColonneSingleton( Presolve, Var, &Ai );	
	if ( ContrainteDeLaVariable < 0 ) continue;
	/* On teste l'instanciation a la borne sup */
  # if TRACES == 1
	  printf("-> Probing de la variable la variable continue %d a la valeur %e\n",Var,BorneSupPourPresolve[Var]); 
  # endif
	NbBornesModifiees = 0;
  PRS_InstanciationSurUneBorne( Presolve, Var, ContrainteDeLaVariable, Ai, BorneSupPourPresolve[Var], &Admissibilite,
	                              T, ListeDesContraintes, &NbBornesModifiees, ListeDesBornesModifiees );
	if ( Admissibilite == NON_PNE ) {
	  /* Le cout reduit doit etre positif ou nul */
    # if TRACES == 1
		  printf("Creation d'une borne sur la variable duale de la contraine %d\n",ContrainteDeLaVariable);
		# endif
    PRS_MajVariableDuale( Presolve, ContrainteDeLaVariable, CoutLineaire[Var], Ai, '>', NbModifications );		
	}
	PRS_ReinitialiserLesDonnees( NbBornesModifiees, ListeDesBornesModifiees, BorneInfPourPresolve, BorneSupPourPresolve, BorneInfPourPresolveSv, BorneSupPourPresolveSv );	
	/* On teste l'instanciation a la borne inf */
  # if TRACES == 1
	  printf("-> Probing de la variable la variable continue %d a la valeur %e\n",Var,BorneInfPourPresolve[Var]); 
  # endif
	NbBornesModifiees = 0;
  PRS_InstanciationSurUneBorne( Presolve, Var, ContrainteDeLaVariable, Ai, BorneInfPourPresolve[Var], &Admissibilite,
	                              T, ListeDesContraintes, &NbBornesModifiees, ListeDesBornesModifiees );
	if ( Admissibilite == NON_PNE ) {
	  /* Le cout reduit doit etre negatif ou nul */
    # if TRACES == 1
		  printf("Creation d'une borne sur la variable duale de la contraine %d\n",ContrainteDeLaVariable);
		# endif		
    PRS_MajVariableDuale( Presolve, ContrainteDeLaVariable, CoutLineaire[Var], Ai, '<', NbModifications );		
	}
	PRS_ReinitialiserLesDonnees( NbBornesModifiees, ListeDesBornesModifiees, BorneInfPourPresolve, BorneSupPourPresolve, BorneInfPourPresolveSv, BorneSupPourPresolveSv );	
}

free( T );
free( ListeDesContraintes );
free( ListeDesBornesModifiees );
free( BorneInfPourPresolveSv );
free( BorneSupPourPresolveSv );

printf("Nombre de bornes duales modifiees %d\n",*NbModifications);

return;
}

/*----------------------------------------------------------------------------*/

void PRS_ReinitialiserLesDonnees( int NbBornesModifiees, int * ListeDesBornesModifiees, double * BorneInfPourPresolve, double * BorneSupPourPresolve,
                                  double * BorneInfPourPresolveSv, double * BorneSupPourPresolveSv )
{
int i; int Var; 
for ( i = 0 ; i < NbBornesModifiees ; i++ ) {
  Var = ListeDesBornesModifiees[i];
	BorneInfPourPresolve[Var] = BorneInfPourPresolveSv[Var]; 
	BorneSupPourPresolve[Var] = BorneSupPourPresolveSv[Var];
}
return;
}

/*----------------------------------------------------------------------------*/

void PRS_InstanciationSurUneBorne( PRESOLVE * Presolve, int Var, int Cnt, double a, double ValeurDeLaVariable, char * Admissibilite,
                                   char * T, int * ListeDesContraintes, int * NbBornesModifiees, int * ListeDesBornesModifiees )
{
int Var1; double * A; int * Nuvar; int il; int ilMax; double Ai; char BmnValide; char BmxValide;
double Bmn; double Bmx; char SensCnt; double BCnt; double * BorneInfPourPresolve; double * BorneSupPourPresolve;
int * TypeDeBornePourPresolve; char XsValide; char XiValide; double Xi; double Xs; double Xi0;
double Xs0; double BminNew; double BmaxNew; double S; PROBLEME_PNE * Pne; int NbContraintesDeLaListe;
int * Cdeb; int * Csui; int * NumContrainte; char * ContrainteInactive; int ic; int Nbb;

*Admissibilite = OUI_PNE;
Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
BmnValide = Presolve->MinContrainteCalcule[Cnt];    
BmxValide = Presolve->MaxContrainteCalcule[Cnt];
if ( BmnValide == NON_PNE && BmxValide == NON_PNE ) return;
Bmn = Presolve->MinContrainte[Cnt];
Bmx = Presolve->MaxContrainte[Cnt];

il = Pne->MdebTrav[Cnt];
ilMax = il + Pne->NbTermTrav[Cnt];
SensCnt = Pne->SensContrainteTrav[Cnt];
BCnt = Pne->BTrav[Cnt];
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;

BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;

NbContraintesDeLaListe = 0;
Nbb = 0;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
ContrainteInactive = Presolve->ContrainteInactive;

/* On met a jour le second membre */
if ( BmxValide == OUI_PNE ) {
  if ( a > 0 ) Bmx = Bmx - ( a * BorneSupPourPresolve[Var] ) + ( a * ValeurDeLaVariable );
	else Bmx = Bmx - ( a * BorneInfPourPresolve[Var] ) + ( a * ValeurDeLaVariable );	
}
if ( BmnValide == OUI_PNE ) {
  if ( a > 0 ) Bmn = Bmn - ( a * BorneInfPourPresolve[Var] ) + ( a * ValeurDeLaVariable );	
	else Bmn = Bmn - ( a * BorneSupPourPresolve[Var] ) + ( a * ValeurDeLaVariable );
}

/* On calcule les bornes sur les variables de la contrainte */
while ( il < ilMax ) {
  Ai = A[il];
	if ( Ai == 0.0 ) goto NextIl;
	Var1 = Nuvar[il];
	if ( Var1 == Var ) goto NextIl;
  if ( TypeDeBornePourPresolve[Var1] == VARIABLE_NON_BORNEE || TypeDeBornePourPresolve[Var1] == VARIABLE_FIXE ) goto NextIl;
 		
	XsValide = NON_PNE;   
	XiValide = NON_PNE;		
	Xs = BorneSupPourPresolve[Var1];
	Xi = BorneInfPourPresolve[Var1];
	Xs0 = Xs;
	Xi0 = Xi;		
						
  if ( SensCnt == '=' ) {		
	  /* On regarde le min et le max */
	  if ( BmnValide == OUI_PNE ) {
		  BminNew = Bmn;
      if ( Ai > 0.0 ) BminNew -= Ai * Xi0; /* On avait pris le min */
      else BminNew -= Ai * Xs0; /* On avait pris le max */		
		  S = BCnt - BminNew;
		  if ( Ai > 0 ) { Xs = S / Ai; XsValide = OUI_PNE; }
	    else { Xi = -S / fabs( Ai ); XiValide = OUI_PNE; }		
	  }
	  if ( BmxValide == OUI_PNE ) {	
      BmaxNew = Bmx;
		  if ( Ai > 0.0 ) BmaxNew -= Ai * Xs0; /* On avait pris le max */
      else BmaxNew -= Ai * Xi0; /* On avait pris le min */
      S = BCnt - BmaxNew;
		  if ( Ai > 0 ) { Xi = S / Ai; XiValide = OUI_PNE; }	
		  else { Xs = -S / fabs( Ai ); XsValide = OUI_PNE; }				
	  }	
  }
  else if ( SensCnt == '<' ) {		
    /* On peut calculer un majorant */			
	  if ( BmnValide == OUI_PNE ) {						
		  BminNew = Bmn;
      if ( Ai > 0.0 ) BminNew -= Ai * Xi0; /* On avait pris le min */				
      else BminNew -= Ai * Xs0; /* On avait pris le max */				
		  S = BCnt - BminNew;
		  if ( Ai > 0 ) { Xs = S / Ai; XsValide = OUI_PNE; }
		  else { Xi = -S / fabs( Ai ); XiValide = OUI_PNE; }					
	  }			 
  }
  else if ( SensCnt == '>' ) {
    /* On peut calculer un minorant */
		if ( BmxValide == OUI_PNE ) {
      BmaxNew = Bmx;
		  if ( Ai > 0.0 ) BmaxNew -= Ai * Xs0; /* On avait pris le max */
      else BmaxNew -= Ai * Xi0; /* On avait pris le min */
      S = BCnt - BmaxNew;
		  if ( Ai > 0 ) { Xi = S / Ai; XiValide = OUI_PNE; }
		  else { Xs = -S / fabs( Ai ); XsValide = OUI_PNE; }					
		}
	}
	if ( XiValide == OUI_PNE ) {
		if ( Xi > Xi0 + EPS ) {
			/* Ajouter les contraintes de la variable dans la liste a examiner */
			BorneInfPourPresolve[Var1] = Xi;
			ListeDesBornesModifiees[Nbb] = Var1;
			Nbb++;
			ic = Cdeb[Var1];
			while ( ic >= 0 ) {
			  Cnt = NumContrainte[ic];
        if ( T[Cnt] == 0 ) {
				  T[Cnt] = 1;
					ListeDesContraintes[NbContraintesDeLaListe] = Cnt;
					NbContraintesDeLaListe++;
				}
			  ic = Csui[ic];
			}
		}
	}
	if ( XsValide == OUI_PNE ) {
		if ( Xs < Xs0 - EPS ) {
			/* Ajouter les contraintes de la variable dans la liste a examiner */
			BorneSupPourPresolve[Var1] = Xs;
			ListeDesBornesModifiees[Nbb] = Var1;
			Nbb++;			
			ic = Cdeb[Var1];
			while ( ic >= 0 ) {
			  Cnt = NumContrainte[ic];
        if ( T[Cnt] == 0 ) {
				  T[Cnt] = 1;
					ListeDesContraintes[NbContraintesDeLaListe] = Cnt;
					NbContraintesDeLaListe++;
				}
			  ic = Csui[ic];
			}			
		}
	}		
	NextIl:
	il++;
}

if ( NbContraintesDeLaListe > 0 ) {
  PRS_AnalyserLesContraintesdeLaListe( Presolve, Admissibilite, T, NbContraintesDeLaListe, ListeDesContraintes );
}

*NbBornesModifiees = Nbb;

return;
}

/*----------------------------------------------------------------------------*/

void PRS_AnalyserLesContraintesdeLaListe( PRESOLVE * Presolve, char * Admissibilite, char * T, int NbContraintesDeLaListe, int * ListeDesContraintes )
{
int i; int Cnt; PROBLEME_PNE * Pne; char * MinContrainteCalcule; char * MaxContrainteCalcule;
double * MinContrainte; double * MaxContrainte; int * Mdeb; int * NbTerm; int il; int ilMax;
char * SensCnt; double * B; double * A; int * Nuvar; double * ValeurDeXPourPresolve; int Var;
double * BorneInfPourPresolve; double * BorneSupPourPresolve; int * TypeDeBornePourPresolve; double Smin; double Smax;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
MinContrainteCalcule = Presolve->MinContrainteCalcule;    
MaxContrainteCalcule = Presolve->MaxContrainteCalcule;
MinContrainte = Presolve->MinContrainte;
MaxContrainte = Presolve->MaxContrainte;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
SensCnt = Pne->SensContrainteTrav;
B = Pne->BTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;

ValeurDeXPourPresolve = Presolve->ValeurDeXPourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;

for ( i = 0 ; i < NbContraintesDeLaListe ; i++ ) {
  Cnt = ListeDesContraintes[i];
	T[Cnt] = 0;
	if ( SensCnt[Cnt] == '=' ) {
	  if ( MinContrainteCalcule[Cnt] == OUI_PNE ) {
		  Smin = 0;			
      il = Mdeb[Cnt];
      ilMax = il + NbTerm[Cnt];
      while ( il < ilMax ) {
			  Var = Nuvar[il];
			  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) Smin += A[il] * ValeurDeXPourPresolve[Var];				
        else if ( A[il] > 0 ) Smin += A[il] * BorneInfPourPresolve[Var];
				else Smin += A[il] * BorneSupPourPresolve[Var];				
		    il++;
		  }
			if ( Smin > B[Cnt] + SEUIL_DADMISSIBILITE ) {
        *Admissibilite = NON_PNE;
				goto FinAnalyse;
			}
		}
	  if ( MaxContrainteCalcule[Cnt] == OUI_PNE ) {
		  Smax = 0;			
      il = Mdeb[Cnt];
      ilMax = il + NbTerm[Cnt];
      while ( il < ilMax ) {
			  Var = Nuvar[il];
			  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) Smax += A[il] * ValeurDeXPourPresolve[Var];				
        else if ( A[il] > 0 ) Smax += A[il] * BorneSupPourPresolve[Var];
				else Smax += A[il] * BorneInfPourPresolve[Var];				
		    il++;
		  }
			if ( Smax < B[Cnt] - SEUIL_DADMISSIBILITE ) {
        *Admissibilite = NON_PNE;
				goto FinAnalyse;
			}
		}		
	}
	else if ( SensCnt[Cnt] == '<' ) {
	  if ( MinContrainteCalcule[Cnt] == OUI_PNE ) {
		  Smin = 0;			
      il = Mdeb[Cnt];
      ilMax = il + NbTerm[Cnt];
      while ( il < ilMax ) {
			  Var = Nuvar[il];
			  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) Smin += A[il] * ValeurDeXPourPresolve[Var];				
        else if ( A[il] > 0 ) Smin += A[il] * BorneInfPourPresolve[Var];
				else Smin += A[il] * BorneSupPourPresolve[Var];				
		    il++;
		  }
			if ( Smin > B[Cnt] + SEUIL_DADMISSIBILITE ) {
        *Admissibilite = NON_PNE;
				goto FinAnalyse;
			}
		}
	}
	else if ( SensCnt[Cnt] == '>' ) {
	  if ( MaxContrainteCalcule[Cnt] == OUI_PNE ) {
		  Smax = 0;			
      il = Mdeb[Cnt];
      ilMax = il + NbTerm[Cnt];
      while ( il < ilMax ) {
			  Var = Nuvar[il];
			  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) Smax += A[il] * ValeurDeXPourPresolve[Var];				
        else if ( A[il] > 0 ) Smax += A[il] * BorneSupPourPresolve[Var];
				else Smax += A[il] * BorneInfPourPresolve[Var];				
		    il++;
		  }
			if ( Smax < B[Cnt] - SEUIL_DADMISSIBILITE ) {
        *Admissibilite = NON_PNE;
				goto FinAnalyse;
			}
		}		
	}	
}
FinAnalyse:
for ( ; i < NbContraintesDeLaListe ; i++ ) T[ListeDesContraintes[i]] = 0;

return;
}

/*----------------------------------------------------------------------------*/


# endif
