/***********************************************************************

   FONCTION: On applique le graphe de conflit quand on simule l'instanciation
	           d'une variable dans le reduced cost fixing.
                
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

void PNE_ReducedCostFixingConflictGraph( PROBLEME_PNE * Pne, int VariableFixee,
                                         double ValeurVariableFixee,
																				 double * DeltaCout, double Delta, double * CoutsReduits,  
																				 double * Xmin, double * Xmax,
																				 int * PositionDeLaVariable, int * Nb, int * T, int * Num,
																				 int * Arret )
{
int Pivot; int Noeud; int Edge; int Complement; int Nv; int * Adjacent; int * Next;
int * First; CONFLICT_GRAPH * ConflictGraph; int * TypeDeBorneTrav;

if ( T[VariableFixee] == 1 ) return;

if ( ValeurVariableFixee == 1 ) {
  if( PositionDeLaVariable[VariableFixee] == HORS_BASE_SUR_BORNE_INF ) {
    *DeltaCout = *DeltaCout + fabs( CoutsReduits[VariableFixee] * ( Xmax[VariableFixee] - Xmin[VariableFixee] ) );
	}
}
else {
  if( PositionDeLaVariable[VariableFixee] == HORS_BASE_SUR_BORNE_SUP ) {
    *DeltaCout = *DeltaCout + fabs( CoutsReduits[VariableFixee] * ( Xmax[VariableFixee] - Xmin[VariableFixee] ) );
	}
}

if ( *DeltaCout > Delta + MARGE_SUR_DELTA_POUR_LE_REDUCED_COST_FIXING ) {
  *DeltaCout = *DeltaCout + MARGE_SUR_DELTA_POUR_LE_REDUCED_COST_FIXING;
	*Arret = OUI_PNE;
  return;
}

Num[*Nb] = VariableFixee;
*Nb = *Nb + 1;
T[VariableFixee] = 1;

ConflictGraph = Pne->ConflictGraph;
if ( ConflictGraph == NULL ) return;
Adjacent = ConflictGraph->Adjacent;
Next = ConflictGraph->Next;
First = ConflictGraph->First;
Pivot = ConflictGraph->Pivot;

TypeDeBorneTrav = Pne->TypeDeBorneTravSv;

Noeud = VariableFixee;
Complement = VariableFixee + Pivot;
if ( ValeurVariableFixee == 0.0 ) {
  Noeud = VariableFixee + Pivot;
  Complement = VariableFixee;
}

if ( First[Noeud] < 0 ) return;

Edge = First[Noeud];
while ( Edge >= 0 ) {
  Nv = Adjacent[Edge];
	if ( Nv == Complement ) goto NextEdge;
	if ( Nv < Pivot ) {
	  /* La valeur 1 de Nv est interdite on doit la passer a 0 et on regarde son impact avec le cout reduit */
	  VariableFixee = Nv;
	  if ( Xmin[VariableFixee] == 1 ) {
      *DeltaCout = Delta + 10; /* Pour fixer la variable etudiee */		   
	    *Arret = OUI_PNE;			
      return;			
		}		
		if ( TypeDeBorneTrav[VariableFixee] != VARIABLE_FIXE ) {		
		  if ( PositionDeLaVariable[VariableFixee] == HORS_BASE_SUR_BORNE_SUP ) {		
        ValeurVariableFixee = Xmin[VariableFixee];		
        PNE_ReducedCostFixingConflictGraph( Pne, VariableFixee, ValeurVariableFixee, DeltaCout, Delta, CoutsReduits, Xmin, Xmax,
		                                        PositionDeLaVariable, Nb, T, Num, Arret );
	      if ( *Arret == OUI_PNE ) break;
		  }
		}		
	}
	else {
	  /* La valeur 0 de Nv est interdite on doit la passer a 1 et on regarde son impact avec le cout reduit */
    VariableFixee = Nv - Pivot;
	  if ( Xmax[VariableFixee] == 0 ) {
      *DeltaCout = Delta + 10; /* Pour fixer la variable etudiee */		   
	    *Arret = OUI_PNE;			
      return;			
		}
		if ( TypeDeBorneTrav[VariableFixee] != VARIABLE_FIXE ) {		
		  if ( PositionDeLaVariable[VariableFixee] == HORS_BASE_SUR_BORNE_INF ) {		
        ValeurVariableFixee = Xmax[VariableFixee];		
        PNE_ReducedCostFixingConflictGraph( Pne, VariableFixee, ValeurVariableFixee, DeltaCout, Delta, CoutsReduits, Xmin, Xmax,
		                                        PositionDeLaVariable, Nb, T, Num, Arret );
	      if ( *Arret == OUI_PNE ) break;
		  }
		}		
	}	
  NextEdge:
  Edge = Next[Edge];
}

return;
}
