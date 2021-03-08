/*************************************************************************

   FONCTION: Evaluation des 2 fils du noeud comportant le meilleur
             minorant

   AUTEUR: R. GONZALEZ

**************************************************************************/

#include "bb_sys.h"
#include "bb_define.h"
#include "bb_fonctions.h"

#include "pne_define.h"

/*---------------------------------------------------------------------------------------------------------*/

void BB_EvaluerLesDeuxFilsDuMeilleurMinorant( BB * Bb, NOEUD * NoeudDuMeilleurMinorant )
{
int YaUneSolution; NOEUD * NoeudCourant; int SolutionEntiereTrouvee;  

#if VERBOSE_BB
  printf("Minorant du meilleur noeud %12.8e\n",NoeudDuMeilleurMinorant->MinorantDuCritereAuNoeud);
#endif
  
NoeudCourant = NoeudDuMeilleurMinorant->NoeudSuivantGauche;
if ( NoeudCourant != 0 ) {
  if ( NoeudCourant->StatutDuNoeud == A_EVALUER && 
       NoeudCourant->NoeudTerminal != OUI       && 
       NoeudCourant->StatutDuNoeud != A_REJETER ) {

    #if VERBOSE_BB
      printf("Evaluation fils gauche du meilleur minorant\n");
    #endif

    Bb->NoeudEnExamen = NoeudCourant;
    YaUneSolution = BB_ResoudreLeProblemeRelaxe( Bb, NoeudCourant , &SolutionEntiereTrouvee ); 

    BB_NettoyerLArbre( Bb, &YaUneSolution , NoeudCourant );  /* Fait aussi la mise a jour du statut */

    
    BB_CreerLesNoeudsFils( Bb, NoeudCourant );    

  }
}

NoeudCourant = NoeudDuMeilleurMinorant->NoeudSuivantDroit;
if ( NoeudCourant != 0 ) {
  if ( NoeudCourant->StatutDuNoeud == A_EVALUER && 
       NoeudCourant->NoeudTerminal != OUI       && 
       NoeudCourant->StatutDuNoeud != A_REJETER ) {

    #if VERBOSE_BB
      printf("Evaluation fils droit du meilleur minorant\n");
    #endif

    Bb->NoeudEnExamen = NoeudCourant;
    YaUneSolution = BB_ResoudreLeProblemeRelaxe( Bb, NoeudCourant , &SolutionEntiereTrouvee ); 
  
    BB_NettoyerLArbre( Bb, &YaUneSolution , NoeudCourant );  /* Fait aussi la mise a jour du statut */

    BB_CreerLesNoeudsFils( Bb, NoeudCourant );    

  }
}

return;
}

