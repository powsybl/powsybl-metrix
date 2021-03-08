/***********************************************************************

   FONCTION: Utilisation de la dominance des variables pour fixer des variables
	           entieres a l'aide du graphe de conflits.
             Attention: inutile dans le cas ou il n'y a que des contraintes d'egalite.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES 0

void PNE_DominanceDUneVariablePreparerUnVecteurColonne( PROBLEME_PNE * , int , double * , int * , char * , int * , char * );
void PNE_TesterLaDominanceDUneVariableParRapportAUneAutre( PROBLEME_PNE * , int , int , int * , int * , double * , int * , char * , int , char * );

/*----------------------------------------------------------------------------*/
/* Correction 10/4/2016: ContrainteActivable doit etre passe en argument */

void PNE_TesterLaDominanceDUneVariableParRapportAUneAutre( PROBLEME_PNE * Pne,
																											     int Var1, int Var2,
																													 /* En retour le numero de la variable en premier.
																													    Valeurs negatives si aucune ne domine l'autre. */
																													 int * VarDominante, int * VarDominee,
																													 /* Vecteurs de travail */
																													 double * W, int * IndexDeWNonNul, char * T, int NbNonNuls,
																													 char * ContrainteActivable )
{
int ic; int Cnt; int i; int Nb; int * Cdeb; int * Csui; int * NumContrainte; double * A;
char Sens; char * SensContrainte;
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
  if ( ContrainteActivable[Cnt] == NON_PNE ) goto NextIc;
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

void PNE_DominanceDUneVariablePreparerUnVecteurColonne( PROBLEME_PNE * Pne, int Var, double * W, int * IndexDeWNonNul, char * T, int * NbNonNuls ,
                                                        char * ContrainteActivable )
{
int ic; int Cnt; int Nb; int * Cdeb; int * Csui; int * NumContrainte; double * A; char * SensContrainte;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;
SensContrainte = Pne->SensContrainteTrav;
Nb = 0;
ic = Cdeb[Var];
while ( ic >= 0 ) {
  Cnt = NumContrainte[ic];
  if ( ContrainteActivable[Cnt] == NON_PNE ) goto NextIc;
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

void PNE_TesterLaDominanceDesVariablesEntieres( PROBLEME_PNE * Pne, int * Faisabilite, char * ContrainteActivable, char * RefaireUnCycle )
{
double * W; char * T; int Pivot; int Noeud; int Edge; int Nv; int * Adjacent; int * Next;
int * First; CONFLICT_GRAPH * ConflictGraph; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve;
double * ValeurDeBorneSup; double * ValeurDeBorneInf; int NbNoeudsAExaminer; int NbNonNuls;
int Var1; int Var2; int VarDominante; int VarDominee; int * IndexDeWNonNul; int i; int Cnt;
int N; char UneVariableAEteFixee; char BorneMiseAJour; double NouvelleValeur;
int Var; int NombreDeVariables; int * TypeDeVariable;

/*
printf("********************************************************************************\n");
printf("Appel a PNE_TesterLaDominanceDesVariablesEntieres \n");												
printf("********************************************************************************\n");
*/

ConflictGraph = Pne->ConflictGraph;
if ( ConflictGraph == NULL ) return;
ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
if ( ProbingOuNodePresolve == NULL ) return;

W = (double *) malloc( Pne->NombreDeContraintesTrav * sizeof( double ) );
IndexDeWNonNul = (int *) malloc( Pne->NombreDeContraintesTrav * sizeof( int ) );
T = (char *) malloc( Pne->NombreDeContraintesTrav * sizeof( char ) );
if ( W == NULL || IndexDeWNonNul == NULL || T == NULL ) { free( W ); free( IndexDeWNonNul ); free( T ); return; }

memset( (char *) W, 0 , Pne->NombreDeContraintesTrav * sizeof( double ));
memset( (char *) T, 0 , Pne->NombreDeContraintesTrav * sizeof( char ));

Adjacent = ConflictGraph->Adjacent;
Next = ConflictGraph->Next;
First = ConflictGraph->First;
Pivot = ConflictGraph->Pivot;
NbNoeudsAExaminer = Pivot;

ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

NouvelleValeur = -1;
BorneMiseAJour = NON_PNE;
UneVariableAEteFixee = NON_PNE;

/* ???? Ne pas le faire si ce n'est pas un pb en variables entieres pur
   car sinon j'ai experimentalement remarque que ca marche moins bien.
   Pourtant la theorie devrait s'appliquer de la meme facon. */
/*
NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( TypeDeVariable[Var] != ENTIER ) {
    if ( ValeurDeBorneInf[Var] != ValeurDeBorneSup[Var] ) goto FinDominance;			
	}
}
*/

/* Cas des incompatibilites a 1 */
for ( Noeud = 0 ; Noeud < NbNoeudsAExaminer ; Noeud++ ) {
  Var1 = Noeud;
	if ( ValeurDeBorneInf[Var1] == ValeurDeBorneSup[Var1] ) continue;

	N = Noeud;
  Edge = First[N];
  if ( Edge < 0 ) continue;
	
  PNE_DominanceDUneVariablePreparerUnVecteurColonne( Pne, Var1, W, IndexDeWNonNul, T, &NbNonNuls, ContrainteActivable );
  if ( NbNonNuls < 0 ) continue;

  while ( Edge >= 0 ) {
    Nv = Adjacent[Edge];
	  if ( Nv < Pivot ) {
	    /* La valeur 1 de Nv est interdite */
	    Var2 = Nv;
	    if ( ValeurDeBorneInf[Var2] == ValeurDeBorneSup[Var2] ) goto NextEdge_1;			
			
      PNE_TesterLaDominanceDUneVariableParRapportAUneAutre( Pne, Var1, Var2, &VarDominante, &VarDominee, W, IndexDeWNonNul, T, NbNonNuls, ContrainteActivable );
			
			if ( VarDominante >= 0 && VarDominee >= 0 ) {
				if ( VarDominee != Var1 ) {
          /* On fixe VarDominee a 0 */
          # if TRACES == 1
					  printf("Dominance: on fixe %d a %e\n",VarDominee,ValeurDeBorneInf[VarDominee]);
          # endif
					*RefaireUnCycle = OUI_PNE;					
          NouvelleValeur = ValeurDeBorneInf[VarDominee];
				  UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				
					*RefaireUnCycle = OUI_PNE;     					
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, VarDominee, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );					
          if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  	      if ( *Faisabilite == NON_PNE ) {
					  goto FinDominance;
					}
				}
				else {
          /* On fixe Var1 a 0 */          
          # if TRACES == 1
					  printf("Dominance: on fixe %d a %e\n",VarDominee,ValeurDeBorneInf[VarDominee]);
          # endif
					*RefaireUnCycle = OUI_PNE;										
          NouvelleValeur = ValeurDeBorneInf[VarDominee];
				  UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				
					*RefaireUnCycle = OUI_PNE;     					
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, VarDominee, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
          if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  	      if ( *Faisabilite == NON_PNE ) {
					  goto FinDominance;
					}
					/*break;*/ /* Il faut continuer a analyser les vosins de Var1 car Var1 domine peut-etre d'autres variables */
				}
			}
		}	
    NextEdge_1:
    Edge = Next[Edge];
	}
  for ( i = 0 ; i < NbNonNuls ; i++ ) {
	  Cnt = IndexDeWNonNul[i];
		W[Cnt] = 0;
	  T[Cnt] = 0;		
  }	
}

/* Cas des incompatibilites a 0 */
for ( Noeud = 0 ; Noeud < NbNoeudsAExaminer ; Noeud++ ) {
  Var1 = Noeud;
	if ( ValeurDeBorneInf[Var1] == ValeurDeBorneSup[Var1] ) continue;

  N = Noeud + Pivot;
  Edge = First[N];
  if ( Edge < 0 ) continue;
	
  PNE_DominanceDUneVariablePreparerUnVecteurColonne( Pne, Var1, W, IndexDeWNonNul, T, &NbNonNuls, ContrainteActivable );
  if ( NbNonNuls < 0 ) continue;
		
  while ( Edge >= 0 ) {
    Nv = Adjacent[Edge];
	  if ( Nv < Pivot ) goto NextEdge_0;		  
		else {
	    /* La valeur 0 de Nv est interdite */
	    Var2 = Nv - Pivot;
	    if ( ValeurDeBorneInf[Var2] == ValeurDeBorneSup[Var2] ) goto NextEdge_0;
						
      PNE_TesterLaDominanceDUneVariableParRapportAUneAutre( Pne, Var1, Var2, &VarDominante, &VarDominee, W, IndexDeWNonNul, T, NbNonNuls, ContrainteActivable );
			
			if ( VarDominante >= 0 && VarDominee >= 0 ) {
				if ( VarDominante != Var1 ) {
          /* On fixe VarDominante a 1 */
          # if TRACES == 1
					  printf("Dominance: on fixe %d a %e\n",VarDominante,ValeurDeBorneSup[VarDominante]);
          # endif
					*RefaireUnCycle = OUI_PNE;										
          NouvelleValeur = ValeurDeBorneSup[VarDominante];
				  UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;				
					*RefaireUnCycle = OUI_PNE;     					
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, VarDominante, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
          if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  	      if ( *Faisabilite == NON_PNE ) {
					  goto FinDominance;
					}
				}
				else {
          /* On fixe VarDominante a 1 */
          # if TRACES == 1
				    printf("Dominance: on fixe %d a %e\n",VarDominante,ValeurDeBorneSup[VarDominante]);
          # endif
					*RefaireUnCycle = OUI_PNE;										
          ValeurDeBorneInf[VarDominante] = ValeurDeBorneSup[VarDominante];
          NouvelleValeur = ValeurDeBorneSup[VarDominante];
				  UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;				
					*RefaireUnCycle = OUI_PNE;     					
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, VarDominante, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
          if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  	      if ( *Faisabilite == NON_PNE ) {
					  goto FinDominance;
					}
					/*break;*/ /* Il faut continuer a analyser les vosins de Var1 car Var1 domine peut-etre d'autres variables */
				}
			}
		}	
    NextEdge_0:
    Edge = Next[Edge];
	}
  for ( i = 0 ; i < NbNonNuls ; i++ ) {
	  Cnt = IndexDeWNonNul[i];
		W[Cnt] = 0;
	  T[Cnt] = 0;		
  }	
}

FinDominance:
free( W );
free( IndexDeWNonNul );
free( T );
return;
}
