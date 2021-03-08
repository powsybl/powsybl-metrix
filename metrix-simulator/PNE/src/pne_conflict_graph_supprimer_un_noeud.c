/***********************************************************************

   FONCTION: Suppression d'un noeud dans le conflict graph.
	           On ne recupere pas la place car trop couteux en temps
						 de calcul.
                 
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

/*----------------------------------------------------------------------------*/
/* Suppression de l'arc partant de Nv vers Noeud */
void PNE_ConflictGraphSupprimerUnArc( int Nv, int Noeud, int * First, int * Adjacent, int * Next )
{
int PreviousEdge; int Edge; char Found;
Found = NON_PNE;
PreviousEdge = -1;
Edge = First[Nv];
while ( Edge >= 0 ) {
	if ( Adjacent[Edge] == Noeud ) {
	  if ( PreviousEdge >= 0 ) Next[PreviousEdge] = Next[Edge];
		else First[Nv] = Next[Edge];  
		Found = OUI_PNE;
    break;
	}
	PreviousEdge = Edge;
	Edge = Next[Edge];
}
if ( Found == NON_PNE ) {
  printf("BUG arc partant du noeud %d vers le noeud %d pas trouve\n",Nv,Noeud);
	exit(0);
}
return;
}

/*----------------------------------------------------------------------------*/
/* Pour chaque arc d'egalite, si un noeud est voisin de chaque extremite de l'arc,
   on peut fixer sa valeur et supprimer tous les arcs qui partent du noeud */
	 
void PNE_ConflictGraphSupprimerUnNoeud( int Noeud, int * First, int * Adjacent, int * Next )
{
int Edge; int Nv;
/*printf("Suppression noeud %d\n",Noeud);*/
Edge = First[Noeud];
while ( Edge >= 0 ) {
  Nv = Adjacent[Edge];
  PNE_ConflictGraphSupprimerUnArc( Nv, Noeud, First, Adjacent, Next );
	Edge = Next[Edge];
}
First[Noeud] = -1;
return;
}

/*----------------------------------------------------------------------------*/
/* On reporte les arcs sur le noeud restant et on elimine les arcs du neoud
   supprime */
	 
void PNE_ConflictGraphSubstituerUnNoeud( PROBLEME_PNE * Pne, int NoeudConserve, int NoeudSubstitue,
                                         int * First, int * Adjacent, int * Next )
{
int Edge; int Nv; int ComplementDuNoeudConserve; int Pivot; int Var; double Valeur; int VarNv;
char * BorneInfConnue ;PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; char * BorneInfConnueSv;
int * NumeroDesVariablesFixees; int NombreDeVariablesFixees; int i;

Pivot = Pne->ConflictGraph->Pivot;
ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;   
BorneInfConnueSv = ProbingOuNodePresolve->BorneInfConnueSv;
NumeroDesVariablesFixees = ProbingOuNodePresolve->NumeroDesVariablesFixees;

if ( NoeudConserve < Pivot ) {
  ComplementDuNoeudConserve = NoeudConserve + Pivot;
	Var = NoeudConserve;
	Valeur = 1;
}
else {
  ComplementDuNoeudConserve = NoeudConserve - Pivot;
	Var = ComplementDuNoeudConserve;
	Valeur = 0;
}

NombreDeVariablesFixees = 0;

Edge = First[NoeudSubstitue];
while ( Edge >= 0 ) {
  Nv = Adjacent[Edge];
  if ( Nv == NoeudConserve || Nv == ComplementDuNoeudConserve ) goto NextEdge;

	if ( Nv < Pivot ) {
    VarNv = Nv;
	  BorneInfConnue[VarNv] = FIXATION_SUR_BORNE_INF;
	}
	else {
    VarNv = Nv - Pivot;
	  BorneInfConnue[VarNv] = FIXATION_SUR_BORNE_SUP;
	}
	
  NumeroDesVariablesFixees[NombreDeVariablesFixees] = VarNv;
  NombreDeVariablesFixees += 1;		

	NextEdge:
	Edge = Next[Edge];
}

ProbingOuNodePresolve->NombreDeVariablesFixees = NombreDeVariablesFixees;

/* On recupere les arcs qu'il faut faire pointer sur NoeudConserve */
PNE_MajConflictGraph( Pne, Var, Valeur );

/* On remet les valeurs initiales de BorneInfConnue */
for ( i = 0 ; i < NombreDeVariablesFixees ; i++ ) {
  Var = NumeroDesVariablesFixees[i];
  BorneInfConnue[Var] = BorneInfConnueSv[Var];
}
ProbingOuNodePresolve->NombreDeVariablesFixees = 0;		

return;
}








