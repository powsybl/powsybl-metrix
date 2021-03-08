/***********************************************************************

   FONCTION: On essaie de faire des substitutions de variables en
	           exploitant le grpahe de conflits.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h" 
  
# include "bb_define.h"

# include "prs_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define REAFFECTATION_DES_ARCS NON_PNE

/*----------------------------------------------------------------------------*/

void PNE_SubstituerUneVariableDansGrapheDeConflits( PROBLEME_PNE * Pne, int VarSubstituee )																					
{

int Noeud; int Complement;
# if REAFFECTATION_DES_ARCS	== OUI_PNE
  int NoeudSubstitue; int NoeudConserve;
# endif

if ( Pne->ConflictGraph == NULL ) return;
																												 
# if REAFFECTATION_DES_ARCS	== OUI_PNE
  goto MethodeAvecReaffectationDesArcs;
# endif
		
Noeud = VarSubstituee;
Complement = Pne->ConflictGraph->Pivot + Noeud;
/* On elimine l'entree du noeud dans le graphe */
PNE_ConflictGraphSupprimerUnNoeud( Noeud, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
/* On elimine l'entree du complement */
PNE_ConflictGraphSupprimerUnNoeud( Complement, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );

# if REAFFECTATION_DES_ARCS	== OUI_PNE
MethodeAvecReaffectationDesArcs:
/* Si BTilde = 0 il s'agit d'une substitution VarSubstituee = VarRestante */
/* Si BTilde est different de 0 il s'agit d'une substitution VarSubstituee = 1-VarRestante */
if ( BTilde == 0 ) {		   
  NoeudConserve = VarRestante;
  NoeudSubstitue = VarSubstituee;
  PNE_ConflictGraphSubstituerUnNoeud( Pne, NoeudConserve, NoeudSubstitue, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );

  NoeudConserve = Pne->ConflictGraph->Pivot + VarRestante;
  NoeudSubstitue = Pne->ConflictGraph->Pivot + VarSubstituee;																					
  PNE_ConflictGraphSubstituerUnNoeud( Pne, NoeudConserve, NoeudSubstitue, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );																					
}
else {
  NoeudConserve = VarRestante;
  NoeudSubstitue = Pne->ConflictGraph->Pivot + VarSubstituee;																					
  PNE_ConflictGraphSubstituerUnNoeud( Pne, NoeudConserve, NoeudSubstitue, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );

  NoeudConserve = Pne->ConflictGraph->Pivot + VarRestante;
  NoeudSubstitue = VarSubstituee;
  PNE_ConflictGraphSubstituerUnNoeud( Pne, NoeudConserve, NoeudSubstitue, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
}
# endif
		
Noeud = VarSubstituee;
Complement = Pne->ConflictGraph->Pivot + Noeud;
/* On elimine l'entree du noeud dans le graphe */
PNE_ConflictGraphSupprimerUnNoeud( Noeud, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
/* On elimine l'entree du complement */
PNE_ConflictGraphSupprimerUnNoeud( Complement, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
		
return;

}

/*************************************************************************/


