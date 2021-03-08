/***********************************************************************

   FONCTION: Variable probing et node presolve
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
  
# include "bb_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define MAJ_BMIN 1
# define MAJ_BMAX 2
# define FORCING_BMIN 1
# define FORCING_BMAX 2
# define MAX_TERMES_CONTRAINTES_A_ANALYSER 10
# define SEUIL_NOMBRE_DE_CONTRAINTES_POUR_MAX_TERMES 100000

# define PETITE_MARGE_SUR_BORNES 1.e-9 /*1.e-9*/
# define GRANDE_MARGE_SUR_BORNES 1.e-3 /*1.e-3*/ 
# define NBTERMES_POUR_APPLICATION_DE_LA_MARGE 10
# define ECART_MIN_XI_XS_POUR_MARGE_SUR_BORNES 1.e-6

# define COEFF_MULTIPLICATEUR_SEUIL_DADMISSIBILITE 1 /* 1 */

# define INFAISABILITE 1.e-3

void PNE_ProbingControleFaisabiliteSiMajBminOuBmax( PROBLEME_PNE * , char , char , double , char , double , char , double , int );
void ControleBmin( PROBLEME_PNE * , int , double );
void ControleBmax( PROBLEME_PNE * , int , double );
void PNE_TesterLesContraintesDeBornesVariables( PROBLEME_PNE * , char * );
void PNE_TesterLesCoupesDeProbing( PROBLEME_PNE * );
																										
/*----------------------------------------------------------------------------------------------------*/

void PNE_ProbingControleFaisabiliteSiMajBminOuBmax( PROBLEME_PNE * Pne, char SensContrainte,
                                                    char BorneMiseAJour, double B,
                                                    char BminValide, double Bmin,
                                                    char BmaxValide, double Bmax, int Cnt )
{
if ( BminValide == OUI_PNE && BmaxValide == OUI_PNE ) {
  if ( Bmin > Bmax + ( SEUIL_DADMISSIBILITE * COEFF_MULTIPLICATEUR_SEUIL_DADMISSIBILITE ) ) {
    Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;		
		return;
	}  
}

if ( BorneMiseAJour == MAJ_BMIN ) {
  if ( Bmin > B + ( SEUIL_DADMISSIBILITE * COEFF_MULTIPLICATEUR_SEUIL_DADMISSIBILITE ) ) {	
    Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;		
    return;
  }
}
else if ( BorneMiseAJour == MAJ_BMAX ) {
  if ( SensContrainte == '=' ) {
    if ( Bmax < B - ( SEUIL_DADMISSIBILITE * COEFF_MULTIPLICATEUR_SEUIL_DADMISSIBILITE ) ) {		
			Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;
			return;							
		}		
	}
	else { /* Contrainte < */
		if ( Pne->ProbingOuNodePresolve->VariableInstanciee < 0 ) return;
		/* Comme c'est pour augmenter un coefficient, on prend une valeur plus grande que SEUIL_DABMISSIBILITE */
    if ( Bmax < B - 1.e-3 /*SEUIL_DADMISSIBILITE*/ && BmaxValide == OUI_PNE ) {		
		  /* On va essayer une coupe de probing sur la contrainte */
      if ( Pne->ProbingOuNodePresolve->FlagCntCoupesDeProbing[Cnt] == 0 ) {
        Pne->ProbingOuNodePresolve->NumCntCoupesDeProbing[Pne->ProbingOuNodePresolve->NbCntCoupesDeProbing] = Cnt;
				Pne->ProbingOuNodePresolve->NbCntCoupesDeProbing++;
        Pne->ProbingOuNodePresolve->FlagCntCoupesDeProbing[Cnt] = 1;
			}			
		}	
	}
}
return;
}

/*----------------------------------------------------------------------------------------------------*/
void ControleBmin( PROBLEME_PNE * Pne, int Cnt, double Bmin )
{ int il; int ilMax; double S; double * A; int * Mdeb; int * NbTerm; int * Nuvar; int Var; char BrnInfConnue;
char * BorneSupConnue; char * BorneInfConnue; double * ValeurDeBorneSup; double * ValeurDeBorneInf;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;
BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
S = 0;
il = Mdeb[Cnt];
ilMax =il + NbTerm[Cnt];
while ( il < ilMax) {
  Var = Nuvar[il];		
  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {
	  S += A[il] * ValeurDeBorneInf[Var];
	}
	else {
	  if ( A[il] > 0.0 ) S += A[il] * ValeurDeBorneInf[Var];				       								
	  else S += A[il] * ValeurDeBorneSup[Var];			
	}
  il++;
}
if ( fabs( S - Bmin ) > 1.e-8 && 0 ) {
  printf("S %e Bmin %e Cnt %d  NbFoisContrainteModifiee %d\n",S,Bmin,Cnt,Pne->ProbingOuNodePresolve->NbFoisContrainteModifiee[Cnt]);
  Pne->ProbingOuNodePresolve->Bmin[Cnt] = S;	
	/*exit(0);*/
}
return;
}
/*----------------------------------------------------------------------------------------------------*/
void ControleBmax( PROBLEME_PNE * Pne, int Cnt, double Bmax )
{ int il; int ilMax; double S; double * A; int * Mdeb; int * NbTerm; int * Nuvar; int Var; char BrnInfConnue;
char * BorneSupConnue; char * BorneInfConnue; double * ValeurDeBorneSup; double * ValeurDeBorneInf;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;
BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
S = 0;
il = Mdeb[Cnt];
ilMax =il + NbTerm[Cnt];
while ( il < ilMax) {
  Var = Nuvar[il];		
  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {
	  S += A[il] * ValeurDeBorneInf[Var];
	}
	else {
	  if ( A[il] > 0.0 ) S += A[il] * ValeurDeBorneSup[Var];				       								
	  else S += A[il] * ValeurDeBorneInf[Var];			
	}
  il++;
}
if ( fabs( S - Bmax ) > 1.e-8 && 0 ) {
  printf("S %e Bmax %e Cnt %d  NbFoisContrainteModifiee %d\n",S,Bmax,Cnt,Pne->ProbingOuNodePresolve->NbFoisContrainteModifiee[Cnt]);
  Pne->ProbingOuNodePresolve->Bmax[Cnt] = S;	
	/*exit(0);*/
}
return;
}

/*----------------------------------------------------------------------------------------------------*/

void PNE_ProbingMajBminBmax( PROBLEME_PNE * Pne, int Var, double ValeurDeVar, char BorneMiseAJour )
{
int ic; int * Cdeb; int * Csui; int * NumContrainte; int Cnt; double * A; long double Ai; 
char * BminValide; char * BmaxValide; double * Bmin; double * Bmax; long double ValeurDeBorneInf;
long double ValeurDeBorneSup; char * SensContrainte; long double X; long double ValDeVar;
double * B; int * NumeroDeContrainteModifiee; int * NbFoisContrainteModifiee;
char MajCnt; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; 

Cdeb = Pne->CdebTrav;   
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
A = Pne->ATrav;
SensContrainte = Pne->SensContrainteTrav;   
B = Pne->BTrav;
ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
BminValide = ProbingOuNodePresolve->BminValide;
BmaxValide = ProbingOuNodePresolve->BmaxValide;
Bmin = ProbingOuNodePresolve->Bmin;
Bmax = ProbingOuNodePresolve->Bmax;
ValeurDeBorneInf = (long double) ProbingOuNodePresolve->ValeurDeBorneInf[Var];
ValeurDeBorneSup = (long double) ProbingOuNodePresolve->ValeurDeBorneSup[Var];
NumeroDeContrainteModifiee = ProbingOuNodePresolve->NumeroDeContrainteModifiee;
NbFoisContrainteModifiee = ProbingOuNodePresolve->NbFoisContrainteModifiee;
ValDeVar = (long double) ValeurDeVar;

ic = Cdeb[Var];
while ( ic >= 0 ) {
  Ai = (long double) A[ic];	
  Cnt = NumContrainte[ic];	
  if ( BminValide[Cnt] == OUI_PNE ) {	
	  MajCnt = NON_PNE;
    X = (long double) Bmin[Cnt];		
    if ( BorneMiseAJour == MODIF_BORNE_INF ) {
		  /* C'est une modification de borne inf : peut etre concentre en 1 seul test */
			if ( Ai > 0 ) {
				/*ControleBmin( Pne, Cnt, Bmin[Cnt] );*/        
				X += Ai * (-ValeurDeBorneInf + ValDeVar);
			  Bmin[Cnt] = (double) X;				
	      MajCnt = OUI_PNE;				
			}
		}
		else  if ( BorneMiseAJour == MODIF_BORNE_SUP ) {
		  /* C'est une modification de borne sup */
			if ( Ai < 0 ) {			
				/*ControleBmin( Pne, Cnt, Bmin[Cnt] );*/ 
				X += Ai * (-ValeurDeBorneSup + ValDeVar);
			  Bmin[Cnt] = (double) X;
	      MajCnt = OUI_PNE;				
			}
		}
    else {
			/*ControleBmin( Pne, Cnt, Bmin[Cnt] );*/
	    if ( Ai >= 0.0 ) X += Ai * (-ValeurDeBorneInf + ValDeVar);			
			else X += Ai * (-ValeurDeBorneSup + ValDeVar);
		  Bmin[Cnt] = (double) X;
	    MajCnt = OUI_PNE;			
		}
	  if ( MajCnt == OUI_PNE ) {		
      if ( NbFoisContrainteModifiee[Cnt] == 0 ) {
        NumeroDeContrainteModifiee[ProbingOuNodePresolve->NbContraintesModifiees] = Cnt;
	      ProbingOuNodePresolve->NbContraintesModifiees++;
      }					
     	NbFoisContrainteModifiee[Cnt]++;
      PNE_ProbingControleFaisabiliteSiMajBminOuBmax( Pne, SensContrainte[Cnt], MAJ_BMIN, B[Cnt], BminValide[Cnt],
			                                               Bmin[Cnt], BmaxValide[Cnt], Bmax[Cnt], Cnt );
	    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;			 																													 
		}
	}	
  if ( BmaxValide[Cnt] == OUI_PNE ) {
	  MajCnt = NON_PNE;
    X = (long double) Bmax[Cnt];
    if ( BorneMiseAJour == MODIF_BORNE_INF ) {
		  /* C'est une modification de borne inf */
			if ( Ai < 0 ) {
				/*ControleBmax( Pne, Cnt, Bmax[Cnt] );*/ 
				X += Ai * (-ValeurDeBorneInf + ValDeVar);
			  Bmax[Cnt] = (double) X;				
	      MajCnt = OUI_PNE;				
			}
		}
		else  if ( BorneMiseAJour == MODIF_BORNE_SUP ) {  
		  /* C'est une modification de borne sup */
			if ( Ai > 0 ) {
				/*ControleBmax( Pne, Cnt, Bmax[Cnt] );*/
				X += Ai * (-ValeurDeBorneSup + ValDeVar);
			  Bmax[Cnt] = (double) X;				
	      MajCnt = OUI_PNE;				
			}
		}
    else {
			/*ControleBmax( Pne, Cnt, Bmax[Cnt] );*/
	    if ( Ai >= 0.0 ) X += Ai * (-ValeurDeBorneSup +ValDeVar);			
			else X += Ai * (-ValeurDeBorneInf + ValDeVar);
			Bmax[Cnt] = (double) X;
	    MajCnt = OUI_PNE;			
		}
	  if ( MajCnt == OUI_PNE ) {		
      if ( NbFoisContrainteModifiee[Cnt] == 0 ) {
        NumeroDeContrainteModifiee[ProbingOuNodePresolve->NbContraintesModifiees] = Cnt;
	      ProbingOuNodePresolve->NbContraintesModifiees++;
      }				
     	NbFoisContrainteModifiee[Cnt]++;
      PNE_ProbingControleFaisabiliteSiMajBminOuBmax( Pne, SensContrainte[Cnt], MAJ_BMAX, B[Cnt], BminValide[Cnt],
			                                               Bmin[Cnt], BmaxValide[Cnt], Bmax[Cnt], Cnt );
	    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;			 			
		}
	}
	ic = Csui[ic];
}

return;
}
   
/*----------------------------------------------------------------------------*/
/* Si on fixe la variable Var a la valeur ValeurDeVar, on met a jour la liste
   des contraintes a analyser (celles qui sont modifiees), les seconds membres
	 et leurs plages de variation */
void PNE_InitListeDesContraintesAExaminer( PROBLEME_PNE * Pne, int Var, double ValeurDeVar,
                                           char BorneMiseAJour )
{
int ic; int * Cdeb; int * Csui; int * NumContrainte; int Cnt; int NombreDeContraintesAAnalyser;
int * NumeroDeContrainteAAnalyser; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve;
char * SensContrainte; double * A; char * BmaxValide; double * Bmax; double * B; char * ContrainteAAnalyser;
int NombreDeContraintes; int IndexLibreContraintesAAnalyser; int * NbTerm; double DeltaX0; double DeltaX1; char DeltaX1Valide;

PNE_ProbingMajBminBmax( Pne, Var, ValeurDeVar, BorneMiseAJour );
if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;

if ( Pne->ProbingOuNodePresolve->VariableInstanciee < 0 ) return;

Cdeb = Pne->CdebTrav;     
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;

NombreDeContraintesAAnalyser = ProbingOuNodePresolve->NombreDeContraintesAAnalyser;
NumeroDeContrainteAAnalyser = ProbingOuNodePresolve->NumeroDeContrainteAAnalyser;
ContrainteAAnalyser = ProbingOuNodePresolve->ContrainteAAnalyser;
IndexLibreContraintesAAnalyser = ProbingOuNodePresolve->IndexLibreContraintesAAnalyser;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
NbTerm = Pne->NbTermTrav;
SensContrainte = Pne->SensContrainteTrav;
A = Pne->ATrav;

if ( Pne->ProbingOuNodePresolve->BorneInfConnue[Var] != NON_PNE && Pne->ProbingOuNodePresolve->BorneSupConnue[Var] != NON_PNE ) {
  if (  Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var] != Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var] ) {
    DeltaX0 = Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var] - Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var];
		DeltaX1Valide = NON_PNE;
	  if ( BorneMiseAJour == MODIF_BORNE_SUP ) {
      DeltaX1 = ValeurDeVar - Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var];
		  DeltaX1Valide = OUI_PNE;
		}
	  else if ( BorneMiseAJour == MODIF_BORNE_INF ) {
      DeltaX1 = Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var] - ValeurDeVar;
		  DeltaX1Valide = OUI_PNE;
		}
		if ( DeltaX1Valide == OUI_PNE ) {		
		  if ( DeltaX1 > 0.9 * DeltaX0 ) return;
		}
	}
}

BmaxValide = ProbingOuNodePresolve->BmaxValide;
Bmax = ProbingOuNodePresolve->Bmax;
B = Pne->BTrav;

ic = Cdeb[Var];
while ( ic >= 0 ) {
  Cnt = NumContrainte[ic];
	if ( NombreDeContraintes > SEUIL_NOMBRE_DE_CONTRAINTES_POUR_MAX_TERMES ) {		
    if ( NbTerm[Cnt] > MAX_TERMES_CONTRAINTES_A_ANALYSER ) goto NextIc;
	}
	if ( SensContrainte[Cnt] == '<' ) {
	  if ( BmaxValide[Cnt] == OUI_PNE ) {
      if ( Bmax[Cnt] <= B[Cnt] + SEUIL_DADMISSIBILITE ) goto NextIc;
    }
	}
	/* Il faut aussi ne pas tenir compte des contraintes qui ont ete eliminees parce qu'elles
	   etaient des forcing constraints */
	
  if ( A[ic] > 0 ) {
		/* C'est la borne sup qui conditionne le max. Si elle a ete abaissee on passe a la suite */
		if ( BorneMiseAJour == MODIF_BORNE_SUP ) goto NextIc;
	}					
  else if ( A[ic] < 0 ) {
		/* A est negatif */
		/* C'est la borne inf qui conditionne le max. Si elle a ete abaissee on passe a la suite */
		if ( BorneMiseAJour == MODIF_BORNE_INF ) goto NextIc;
	}
	else goto NextIc;
	
	if ( ContrainteAAnalyser[Cnt] == NON_PNE ) { 
    if ( NombreDeContraintesAAnalyser < NombreDeContraintes ) {	
      /* On l'ajoute a la liste */
	    NumeroDeContrainteAAnalyser[NombreDeContraintesAAnalyser] = Cnt;			
	    NombreDeContraintesAAnalyser++;
		}
		else {
		  /* Pour eviter les debordements de tables */
			if ( IndexLibreContraintesAAnalyser < NombreDeContraintes ) {
	      NumeroDeContrainteAAnalyser[IndexLibreContraintesAAnalyser] = Cnt;			
        IndexLibreContraintesAAnalyser++;
			}
		}
		ContrainteAAnalyser[Cnt] = OUI_PNE;
	}
	
	NextIc:
  ic = Csui[ic];
}

ProbingOuNodePresolve->NombreDeContraintesAAnalyser = NombreDeContraintesAAnalyser;
ProbingOuNodePresolve->IndexLibreContraintesAAnalyser = IndexLibreContraintesAAnalyser;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_VariableProbingAppliquerLeConflictGraph( PROBLEME_PNE * Pne, int Var, double ValeurDeVar,
                                                  char BorneMiseAJour, char UneVariableAEteFixee )
{
int Edge; int Noeud; int Complement; int Nv; int Pivot; double * ValeurDeBorneSup; double * ValeurDeBorneInf;   
char * BorneInfConnue; char * BorneSupConnue; int * First; int * Adjacent; int * Next;

ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;

PNE_InitListeDesContraintesAExaminer( Pne, Var, ValeurDeVar, BorneMiseAJour );
if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;

PNE_MajIndicateursDeBornes( Pne, ValeurDeBorneInf, ValeurDeBorneSup, BorneInfConnue, BorneSupConnue,
                            ValeurDeVar, Var, UneVariableAEteFixee, BorneMiseAJour );			

if ( Pne->TypeDeVariableTrav[Var] != ENTIER ) return;

/* Si la variable est entiere on regarde si elle intervient dans une contrainte de borne variable.
   Si c'est le cas, cela permet de modifier une borne d'une variable continue et on peut recalculer Bmin Bmax. */

if ( Pne->ProbingOuNodePresolve->VariableInstanciee >= 0 && 0 ) {
  PNE_AppliquerToutesLesContraintesDeBorneVariablePourUneVariableEntiere( Pne, Var );
  if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
}


if ( Pne->ConflictGraph == NULL ) return;
Pivot = Pne->ConflictGraph->Pivot;

BorneMiseAJour = NON_PNE;

if ( ValeurDeVar == 1.0 ) { Noeud = Var; Complement = Pivot + Var; }
else { Noeud = Pivot + Var; Complement = Var; }

First = Pne->ConflictGraph->First;
Adjacent = Pne->ConflictGraph->Adjacent;
Next = Pne->ConflictGraph->Next;

Edge = First[Noeud];
while ( Edge >= 0 ) {
  Nv = Adjacent[Edge];
	/* Attention a ne pas prendre le complement */
	if ( Nv == Complement ) goto NextEdge;
  if ( Nv < Pivot ) {
	  Var = Nv;
		/* On ne doit pas avoir U[Var] = 1.0 */
		if ( ValeurDeBorneInf[Var] > 0.0001 ) { Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE; return; } 				
		if ( ValeurDeBorneInf[Var] == ValeurDeBorneSup[Var] ) goto NextEdge;
    /* On place la variable dans la liste des implications */
		/*  */
		ValeurDeVar = 0.0;
    UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;
		/*  */
    PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, ValeurDeVar, BorneMiseAJour, UneVariableAEteFixee );
    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
	}
  else {
    /* La valeur borne inf est interdite pour la variable */
		/* On doit donc fixer la variable a Umax et fixer les voisins de ce noeud */
	  Var = Nv - Pivot;
		/* On ne doit pas avoir U[Var] = 0.0 */
		if ( ValeurDeBorneSup[Var] < 0.9999 ) { Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE; return; }				
		if ( ValeurDeBorneInf[Var] == ValeurDeBorneSup[Var] ) goto NextEdge;
    /* On place la variable dans la liste des implications */
		/*  */		
		ValeurDeVar = 1.0;
    UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;
		/*  */		
    PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, ValeurDeVar, BorneMiseAJour, UneVariableAEteFixee );		
    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
	}
	NextEdge:
  Edge = Next[Edge];
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_AnalyseListeDeContraintes( PROBLEME_PNE * Pne )
{
int i; int Cnt; char XsValide; char XiValide; double Ai; double MargeSurBorne;
int il; int ilMax;int Var; char BorneMiseAJour; char UneVariableAEteFixee;  
int * NumeroDeContrainteAAnalyser; int * Mdeb; int * NbTerm; double * A; int * Nuvar;
double Xi; double Xs; int NombreDeContraintesAAnalyser; int * TypeDeVariable;  char * BorneInfConnue;
char * BorneSupConnue; char BrnInfConnue; double * ValeurDeBorneSup; double * ValeurDeBorneInf;
double BminNew; double BmaxNew; double S; char * SensContrainte; double NouvelleValeur;
char * BminValide; char * BmaxValide; double * Bmin; double * Bmax; double * B; int Nb;
char Flag; long double Xi0; long double Xs0; int NbParcours; int SeuilNbTermesMatrice;
char SensCnt; char BmnValide; char BmxValide; double Bmn; double Bmx; double BCnt; char ForcingContraint;
PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; char * ContrainteAAnalyser;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;

NbParcours = ProbingOuNodePresolve->NbParcours;
SeuilNbTermesMatrice = ProbingOuNodePresolve->SeuilNbTermesMatrice;
NombreDeContraintesAAnalyser = ProbingOuNodePresolve->NombreDeContraintesAAnalyser;
NumeroDeContrainteAAnalyser = ProbingOuNodePresolve->NumeroDeContrainteAAnalyser;
ContrainteAAnalyser = ProbingOuNodePresolve->ContrainteAAnalyser;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;   
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;
BminValide = ProbingOuNodePresolve->BminValide;
BmaxValide = ProbingOuNodePresolve->BmaxValide;
Bmin = ProbingOuNodePresolve->Bmin;
Bmax = ProbingOuNodePresolve->Bmax;

SensContrainte = Pne->SensContrainteTrav;  
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;  
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
TypeDeVariable = Pne->TypeDeVariableTrav;

/* Remarque: NombreDeContraintesAAnalyser peut varier en cours d'analyse parce qu'on peut etre amene a en ajouter.
   C'est la raison pour laquelle il faut utiliser ProbingOuNodePresolve->NombreDeContraintesAAnalyser et
	 pas seulement NombreDeContraintesAAnalyser */
	 
/*
PNE_TesterLesCoupesDeProbing( Pne );
*/
	 
AnalyserListeDeContraintes:

ProbingOuNodePresolve->IndexLibreContraintesAAnalyser = 0;

for ( i = 0 ; i < ProbingOuNodePresolve->NombreDeContraintesAAnalyser ; i++ ) {
  if ( NbParcours > SeuilNbTermesMatrice ) {
		if ( Pne->AffichageDesTraces == OUI_PNE ) {
			printf("Probing stopped after checking %d elements of the matrix (max is %d)\n",NbParcours,SeuilNbTermesMatrice);
    }		
		Pne->ArreterCliquesEtProbing = OUI_PNE;
	}

  Cnt = NumeroDeContrainteAAnalyser[i];
	/* On met tout de suite a non ContrainteAAnalyser */
	ContrainteAAnalyser[Cnt] = NON_PNE;
	  
	/* Que si au moins une des 2 bornes de la contrainte est connue */
	/*if ( BminValide[Cnt] != OUI_PNE && BmaxValide[Cnt] != OUI_PNE ) continue;*/
	SensCnt = SensContrainte[Cnt];
	BmnValide = BminValide[Cnt];
	BmxValide = BmaxValide[Cnt];
	Bmn = Bmin[Cnt];
	Bmx = Bmax[Cnt];
	BCnt = B[Cnt];
	/* Deja fait dans le tri des contraintes a analyser */
	/*
  if ( SensCnt == '<' ) {
	  if ( BmxValide == OUI_PNE ) {
      if ( Bmx <= BCnt + SEUIL_DADMISSIBILITE ) continue;
		}
	}
	*/
	
  ForcingContraint = PNE_DeterminerForcingConstraint( Pne, ProbingOuNodePresolve, Cnt, SensCnt, BmnValide, BmxValide, Bmn, Bmx, BCnt );						
  if ( ForcingContraint == OUI_PNE ) continue;
			
  il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
	NbParcours += NbTerm[Cnt];
	Nb = 0;
	Flag = 0;
  while ( il < ilMax ) {
	  Ai = A[il];
		if ( Ai == 0.0 ) goto NextIl;
		Var = Nuvar[il];		
		BrnInfConnue = BorneInfConnue[Var];
		/* Dans le cas ou la variable a ete fixee, BorneInfConnue et BorneSupConnue ont la meme valeur */
	  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
		     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIl;				 
	  XsValide = NON_PNE;   
	  XiValide = NON_PNE;		
		Nb++;
	  Xs = ValeurDeBorneSup[Var];
	  Xi = ValeurDeBorneInf[Var];
		Xs0 = Xs;
		Xi0 = Xi;		
		
    UneVariableAEteFixee = NON_PNE;
    BorneMiseAJour = NON_PNE;
				
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
    else { /* SensContrainte est '<' */
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
		
		/* Que si Xi ou Xs sont valides et si une des 2 bornes est plus petite ou plus grande */
		if ( XiValide != OUI_PNE && XsValide != OUI_PNE ) goto NextIl;
		
		if ( TypeDeVariable[Var] != ENTIER ) {
		
		  if ( NbTerm[Cnt] > NBTERMES_POUR_APPLICATION_DE_LA_MARGE ) MargeSurBorne = GRANDE_MARGE_SUR_BORNES;
			else MargeSurBorne = PETITE_MARGE_SUR_BORNES;
		
		  /* Ne pas empecher la fixation des variables */
		  if (  XiValide == OUI_PNE && XsValide == OUI_PNE ) {
        if ( fabs( Xs - Xi ) > ECART_MIN_XI_XS_POUR_MARGE_SUR_BORNES ) {
          /*if ( PNE_LaValeurEstEntiere( &Xi ) == NON_PNE )*/ Xi -= MargeSurBorne;					
          /*if ( PNE_LaValeurEstEntiere( &Xs ) == NON_PNE )*/ Xs += MargeSurBorne;					
			  }
		  }
		  else {		
		    if ( XiValide == OUI_PNE ) {				
          /*if ( PNE_LaValeurEstEntiere( &Xi ) == NON_PNE )*/ Xi -= MargeSurBorne;
				}
		    if ( XsValide == OUI_PNE ) {
          /*if ( PNE_LaValeurEstEntiere( &Xs ) == NON_PNE )*/ Xs += MargeSurBorne;
				}
		  }
		}
				
		if ( Xi <= Xi0 && Xs >= Xs0 ) goto NextIl;
		
    PNE_ModifierLaBorneDUneVariable( Pne, Var, SensCnt, XsValide, (double) Xs, XiValide, (double) Xi, &NouvelleValeur,
		                                 &BorneMiseAJour, &UneVariableAEteFixee,
		                                 &(Pne->ProbingOuNodePresolve->Faisabilite) );		
    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;		
				
		/* Si la variable a ete fixee on met a jour la liste des contraintes a examiner au prochain coup */
		if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE  ) {		
		  Flag = 1;
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );			
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
		}
		
    NextIl:			
	  il++;
	}  

	if ( Nb == 1 && Flag == 0 ) {
    PNE_CalculXiXsContrainteAUneSeuleVariable( Pne, &Var, Cnt, Mdeb[Cnt], ilMax, A, Nuvar, BorneInfConnue, ValeurDeBorneInf, 
                                               &XiValide, &XsValide, &Xi, &Xs );
		if ( Var >= 0 ) {
      UneVariableAEteFixee = NON_PNE;
      BorneMiseAJour = NON_PNE;			
      PNE_ModifierLaBorneDUneVariable( Pne, Var, SensCnt, XsValide, (double) Xs, XiValide, (double) Xi, &NouvelleValeur,
		                                   &BorneMiseAJour, &UneVariableAEteFixee,
		                                   &(Pne->ProbingOuNodePresolve->Faisabilite) );																		 
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
						
		  /* Si la variable a ete fixee on met a jour la liste des contraintes a examiner au prochain coup */
		  if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE  ) {
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;			
		  }
		}
	}
		
}

if ( Pne->ProbingOuNodePresolve->Faisabilite == OUI_PNE ) {
  if ( Pne->ArreterCliquesEtProbing == NON_PNE ) {
	  if ( ProbingOuNodePresolve->IndexLibreContraintesAAnalyser != 0 ) {
      ProbingOuNodePresolve->NombreDeContraintesAAnalyser = ProbingOuNodePresolve->IndexLibreContraintesAAnalyser;
			goto AnalyserListeDeContraintes;
		}
  }
}

/* Il y a peut-etre quelque chose a faire en plus mais pour l'instant ce n'est pas concluant */
/*
{
char RefaireUnCycle;
  PNE_TesterLesContraintesDeBornesVariables( Pne, &RefaireUnCycle );
  if ( RefaireUnCycle == OUI_PNE ) {
    goto AnalyserListeDeContraintes;
  }
}
*/

/*
PNE_TesterLesCoupesDeProbing( Pne );
*/

return;

}

/*----------------------------------------------------------------------------*/
# if CONSTRUIRE_BORNES_VARIABLES == OUI_PNE

/* On ne test que les cas qui permettent de fixer des variables entieres */

void PNE_TesterLesContraintesDeBornesVariables( PROBLEME_PNE * Pne, char * RefaireUnCycle )
{
int Cnt; int * First; int ilbin; int ilcont; double B; int Varcont; int * Colonne; double * SecondMembre;
double * Coefficient; int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
int NombreDeContraintesDeBorne; int * TypeDeVariable; char * BorneSupConnue; char * BorneInfConnue;
double * ValeurDeBorneSup; double * ValeurDeBorneInf; char BrnInfConnue; double S0; double S1;
PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; char UneVariableAEteFixee; char BorneMiseAJour;
char * AppliquerUneMargeEventuelle; 

*RefaireUnCycle = NON_PNE;

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

if ( Pne->ProbingOuNodePresolve == NULL ) return;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

TypeDeVariable = Pne->TypeDeVariableTrav;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;
AppliquerUneMargeEventuelle = ContraintesDeBorneVariable->AppliquerUneMargeEventuelle;

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;

/* La variable continue est toujours placee en premier */

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {

  if ( First[Cnt] < 0 ) continue;
	
  ilcont = First[Cnt];
	ilbin = ilcont + 1;
	B = SecondMembre[Cnt];

	Varcont = Colonne[ilcont];	
	Varbin = Colonne[ilbin];
	
  BrnInfConnue = BorneInfConnue[Varbin];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;			 			 
	
  /* La variable binaire n'est pas fixee */
	/* Fixation a 0 */
	S0 = ( B - ( Coefficient[ilbin] * ValeurDeBorneInf[Varbin] ) ) / Coefficient[ilcont];
	/* Fixation a 1 */
	S1 = ( B - ( Coefficient[ilbin] * ValeurDeBorneSup[Varbin] ) ) / Coefficient[ilcont];
		
  if ( Coefficient[ilcont] > 0 ) {
		/* La contrainte est une contrainte de borne sup */
		/* Fixation a 0 */			
		if ( ValeurDeBorneInf[Varcont] > S0 + INFAISABILITE ) {
			# if TRACES == OUI_PNE 
				printf("Contrainte de borne variable: variable entiere %d valeur 0 interdite\n",Varbin);
			# endif

				printf("Contrainte de borne variable: variable entiere %d valeur 0 interdite\n",Varbin);
			
			/* La variable entiere est fixee a 1 */
      UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;
      BorneMiseAJour = NON_PNE;
      *RefaireUnCycle = OUI_PNE;			
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varbin, ValeurDeBorneSup[Varbin], BorneMiseAJour, UneVariableAEteFixee );
      if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
		    return;		        
			}				
			/* Passer a la contrainte suivante */
			continue;				
		}						
		/* Fixation a 1 */			
		if ( ValeurDeBorneInf[Varcont] > S1 + INFAISABILITE ) {
			# if TRACES == OUI_PNE 
				printf("Contrainte de borne variable: variable entiere %d valeur 1 interdite\n",Varbin);
			# endif

				printf("Contrainte de borne variable: variable entiere %d valeur 1 interdite\n",Varbin);
			
			/* La variable entiere est fixee a 0 */
      UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;
      BorneMiseAJour = NON_PNE;								
      *RefaireUnCycle = OUI_PNE;			
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varbin, ValeurDeBorneInf[Varbin], BorneMiseAJour, UneVariableAEteFixee );
      if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
		    return;		        
			}				
			continue;				
		}												
	}
	else {
		/* La contrainte est une contrainte de borne inf */
		/* Fixation a 0 */
		if ( ValeurDeBorneSup[Varcont] < S0 - INFAISABILITE ) {
			# if TRACES == OUI_PNE 
			  printf("Contrainte de borne variable: variable entiere %d valeur 0 interdite\n",Varbin);
			# endif

			  printf("Contrainte de borne variable: variable entiere %d valeur 0 interdite\n",Varbin);
			
 			/* La variable entiere est fixee a 1 */
      UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;
      BorneMiseAJour = NON_PNE;								
      *RefaireUnCycle = OUI_PNE;			
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varbin, ValeurDeBorneSup[Varbin], BorneMiseAJour, UneVariableAEteFixee );
      if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
		    return;		        
			}			
			/* Passer a la contrainte suivante */
			continue;						
		}			
		/* Fixation a 1 */
		if ( ValeurDeBorneSup[Varcont] < S1 - INFAISABILITE ) {
			# if TRACES == OUI_PNE 
			  printf("Contrainte de borne variable: variable entiere %d valeur 1 interdite\n",Varbin);
			# endif

			  printf("Contrainte de borne variable: variable entiere %d valeur 1 interdite\n",Varbin);
			
 			/* La variable entiere est fixee a 0 */
      UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;
      BorneMiseAJour = NON_PNE;								
      *RefaireUnCycle = OUI_PNE;			
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varbin, ValeurDeBorneInf[Varbin], BorneMiseAJour, UneVariableAEteFixee );
      if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
		    return;		        
			}				
			continue;						
		}						  							
	}			 	
}

return;
}
# endif

/*----------------------------------------------------------------------------*/

void PNE_TesterLesCoupesDeProbing( PROBLEME_PNE * Pne )
{
COUPES_DE_PROBING * CoupesDeProbing; int NombreDeCoupesDeProbing; int il; int ilMax; int Var;
int * First; int * NbElements; double * SecondMembre; int * Colonne; double * Coefficient; double a;
double Smin; int NumCoupe; int NbNonFixes; char BrnInfConnue; char * BorneInfConnue;
double * ValeurDeBorneInf; char * BorneSupConnue; double * ValeurDeBorneSup; double NouvelleValeur;
char UneVariableAEteFixee; char BorneMiseAJour; double X; int VarNonFix; int aNonFix;

return;

if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;

CoupesDeProbing = Pne->CoupesDeProbing;
if ( CoupesDeProbing == NULL ) return;

BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;

NombreDeCoupesDeProbing = CoupesDeProbing->NombreDeCoupesDeProbing;
First = CoupesDeProbing->First;
NbElements = CoupesDeProbing->NbElements;
SecondMembre = CoupesDeProbing->SecondMembre;
Coefficient = CoupesDeProbing->Coefficient;
Colonne = CoupesDeProbing->Colonne;
for ( NumCoupe = 0 ; NumCoupe < NombreDeCoupesDeProbing ; NumCoupe++ ) {
  il = First[NumCoupe];
	if ( il < 0 ) continue;
	ilMax = il + NbElements[NumCoupe];
	Smin = 0;
	NbNonFixes = 0;
	VarNonFix = -1;
	aNonFix = 1;
	while ( il < ilMax ) {
    Var = Colonne[il];		
	  a = Coefficient[il];
    BrnInfConnue = BorneInfConnue[Var];
    if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	       BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {
	    Smin += a * ValeurDeBorneInf[Var];						
	  }
		else {
			NbNonFixes++;
			VarNonFix = Var;
			aNonFix = a;
	    if ( a > 0.0 ) {
			  if ( BorneInfConnue[Var] != NON_PNE ) Smin += a * ValeurDeBorneInf[Var];
				else {
				  Smin = VALEUR_NON_INITIALISEE;
			 		break;
			 	}
			}
	    else {
				if ( BorneSupConnue[Var] != NON_PNE ) Smin += a * ValeurDeBorneSup[Var];
				else {
					Smin = VALEUR_NON_INITIALISEE;
					break;
				}
			}
		}			
		il++;
	}
	if ( Smin != VALEUR_NON_INITIALISEE && NbNonFixes > 0 ) {
		if ( NbNonFixes == 1 && Smin <= SecondMembre[NumCoupe] ) {
			/*printf("Coupe de probing %d a 1 seule variable\n",NumCoupe);*/
			Var = VarNonFix;
			if ( Pne->TypeDeVariableTrav[Var] == ENTIER ) {
				BorneMiseAJour = NON_PNE;
        UneVariableAEteFixee = NON_PNE;	
        X = ( SecondMembre[NumCoupe] - Smin ) / aNonFix;
        if ( aNonFix > 0 ) {
				  if ( X < ValeurDeBorneSup[Var] - MARGE_INITIALE_VARIABLES_ENTIERES ) {
            /*printf("On fixe la variable entiere %d a %e\n",Var,ValeurDeBorneInf[Var]);*/
			      NouvelleValeur = ValeurDeBorneInf[Var];
			      UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;							
					}
				}
				else {
				  if ( X > ValeurDeBorneInf[Var] + MARGE_INITIALE_VARIABLES_ENTIERES ) {
            /*printf("On fixe la variable entiere %d a %e\n",Var,ValeurDeBorneSup[Var]);*/
            NouvelleValeur = ValeurDeBorneSup[Var];
			      UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;									
          }
				}
	      if ( UneVariableAEteFixee == OUI_PNE ) {
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
          if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
	      }					
			}
		}
    if ( Smin > SecondMembre[NumCoupe] + 1.e-6 ) {
		  printf("Coupe de probing %d violee\n",NumCoupe);
		}
		else if ( fabs( Smin > SecondMembre[NumCoupe] ) < 1.e-8 ) {
		  /*printf("Coupe de probing %d forcing\n",NumCoupe);*/
			BorneMiseAJour = NON_PNE;
      il = First[NumCoupe];
      ilMax = il + NbElements[NumCoupe];
      while ( il < ilMax ) {
        Var = Colonne[il];		
	      BrnInfConnue = BorneInfConnue[Var];	
	      if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
		         BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIlDoForcing;		
        a = Coefficient[il];
	      if ( a == 0.0 ) goto NextIlDoForcing;		
        UneVariableAEteFixee = NON_PNE;	
        if ( a > 0.0 ) {
			    NouvelleValeur = ValeurDeBorneInf[Var];
			    UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				
		    }
		    else {
          NouvelleValeur = ValeurDeBorneSup[Var];
			    UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;								
		    }	      	
	      if ( UneVariableAEteFixee == OUI_PNE ) {
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
          if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
	      }	
	      NextIlDoForcing:
        il++;
      }			
		}
	}
}	

return;
}
