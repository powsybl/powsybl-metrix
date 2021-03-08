/***********************************************************************

   FONCTION: Analyse du graphe de conflits dans laquelle on etudie les
	           variables qui ont ete fixees.
                 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_define.h"  
# include "pne_fonctions.h"  
		
# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "prs_memoire.h"
# endif

void PRS_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( PRESOLVE * , int , char , int * );

/*----------------------------------------------------------------------------*/

void PRS_AnalyseDuGrapheDeConflits( PRESOLVE * Presolve, int * NbModifications )
{
int * Adjacent; int * Next; int * First; int Edge; int Var; int Pivot; int NombreDeVariables;
PROBLEME_PNE * Pne; int * TypeDeBornePourPresolve; char VariableBinaire; int Noeud;
double ValeurDeVar; double * ValeurDeXPourPresolve; char PremierPassage;

*NbModifications = 0;

return;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
if ( Pne->ConflictGraph == NULL ) return;

NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
ValeurDeXPourPresolve = Presolve->ValeurDeXPourPresolve;

Adjacent = Pne->ConflictGraph->Adjacent;
Next = Pne->ConflictGraph->Next; 				
First = Pne->ConflictGraph->First;
Pivot = Pne->ConflictGraph->Pivot;

/* On regarde les graphe pour toutes les variables entieres fixees */

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( TypeDeBornePourPresolve[Var] != VARIABLE_FIXE ) continue;
	/* Quand on fixe on variable on dit qu'elle devient reelle donc
	   il va falloir rechercher systematiquement dans le graphe */
  VariableBinaire = NON_PNE;
  Noeud = Var;
  Edge = First[Noeud];
	if ( Edge >= 0 ) VariableBinaire = OUI_PNE;
	if ( VariableBinaire == NON_PNE ) {
    Noeud = Pivot + Noeud;
    Edge = First[Noeud];
	  if ( Edge >= 0 ) VariableBinaire = OUI_PNE;		
	}
  if ( VariableBinaire == NON_PNE ) continue;
  /* Analyse du graphe de conflits */
	
  ValeurDeVar = ValeurDeXPourPresolve[Var];
	Noeud = Var;
	if ( ValeurDeVar == 0 ) Noeud = Pivot + Noeud;

	PremierPassage = OUI_PNE;
	PRS_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Presolve, Noeud, PremierPassage, NbModifications );

}

return;
}

/*----------------------------------------------------------------------------*/

void PRS_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( PRESOLVE * Presolve, int Noeud, char PremierPassage, int * NbModifications )
{
int * Adjacent; int * Next; int * First; int Edge; int Nv; int Var; int Pivot; int Complement; 
double * BorneInfPourPresolve; double * BorneSupPourPresolve; int * TypeDeBornePourPresolve;
PROBLEME_PNE * Pne; 

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

if ( Pne->ConflictGraph == NULL ) return;

Adjacent = Pne->ConflictGraph->Adjacent;
Next = Pne->ConflictGraph->Next; 				
First = Pne->ConflictGraph->First;
Pivot = Pne->ConflictGraph->Pivot;

TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;

if ( PremierPassage == NON_PNE ) {
  *NbModifications = *NbModifications + 1;
  if ( Noeud < Pivot ) {
    /* C'est a valeur Max qui est imposee */
    Var = Noeud;
    PRS_FixerUneVariableAUneValeur( Presolve, Var, BorneSupPourPresolve[Var] );  
  }
  else {
    /* C'est a valeur Min qui est imposee */
    Var = Noeud - Pivot;
    PRS_FixerUneVariableAUneValeur( Presolve, Var, BorneInfPourPresolve[Var] );  
  }
}

if ( Noeud < Pivot ) Complement = Pivot + Noeud;
else Complement = Noeud - Pivot;

Edge = First[Noeud];
while ( Edge >= 0 ) {
  Nv = Adjacent[Edge];
	/* Attention a ne pas prendre le complement */
	if ( Nv == Complement ) goto NextEdge;
  if ( Nv < Pivot ) {
	  Var = Nv;
    if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) goto NextEdge;		
    /* La valeur borne sup est interdite pour la variable */
		/* On doit donc fixer la variable a Umin et fixer les voisins de ce noeud */
	  Nv = Pivot + Nv;
		/* On fixe la variable correspondant a Nv */		
		printf("Fixation de %d a 0\n",Var);
		PremierPassage = NON_PNE;
    PRS_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Presolve, Nv, PremierPassage, NbModifications );
		/* On redemarre au debut de Noeud car le chainage a pu changer */
		Edge = First[Noeud];
		continue; /* Pour ne pas faire Edge = Next[Edge] */		
	}
  else {
    /* La valeur borne inf est interdite pour la variable */
		/* On doit donc fixer la variable a Umax et fixer les voisins de ce noeud */
	  Nv = Nv - Pivot;
	  Var = Nv;		
    if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) goto NextEdge;		
		/* On fixe la variable correspondant a Nv */		
		printf("Fixation de %d a 1\n",Var);
		PremierPassage = NON_PNE;
    PRS_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Presolve, Nv, PremierPassage, NbModifications );
		Edge = First[Noeud];
		continue; /* Pour ne pas faire Edge = Next[Edge] */		
	}
	NextEdge:
  Edge = Next[Edge];
}			
/* On elimine l'entree du noeud dans le graphe */
PNE_ConflictGraphSupprimerUnNoeud( Noeud, First, Adjacent, Next );

/* On elimine l'entree du complement */
PNE_ConflictGraphSupprimerUnNoeud( Complement, First, Adjacent, Next );

return;
}


   
