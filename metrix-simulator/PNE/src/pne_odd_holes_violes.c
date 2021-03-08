/***********************************************************************

   FONCTION: Detection des odd holes violes.
                 
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

# define PROFONDEUR_LIMITE_ODD_CYCLES 1000000 /*10*/

/*----------------------------------------------------------------------------*/

void PNE_DetectionDesOddCyclesVioles( PROBLEME_PNE * Pne )
{
double S; int NombreDeVariables; double * U; ODD_CYCLES * OddCycles; int c;
int * First; int * NbElements; int il; int ilMax;
double * Coeff; int * Indice; double B; int NbT; double X; 
int Pivot; int NbOddCyclesVioles;
 char * LeOddCycleEstDansLePool; double Sec;
int * Noeud; int * TypeDeBorne; BB * Bb; char Ok;
double Seuil; int * SecondMembre;

if ( Pne->ConflictGraph == NULL ) return;
if ( Pne->OddCycles == NULL ) return;

Bb = Pne->ProblemeBbDuSolveur;
if ( Bb->NoeudEnExamen->ProfondeurDuNoeud > PROFONDEUR_LIMITE_ODD_CYCLES ) return;  

NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
U = Pne->UTrav;
OddCycles = Pne->OddCycles;
First = OddCycles->First;  
NbElements = OddCycles->NbElements;
SecondMembre = OddCycles->SecondMembre;
Noeud = OddCycles->Noeud;
LeOddCycleEstDansLePool = OddCycles->LeOddCycleEstDansLePool;
Pivot = Pne->ConflictGraph->Pivot;

Coeff = Pne->Coefficient_CG;
Indice = Pne->IndiceDeLaVariable_CG;

/* Attention: la partie simplexe considere que les variables intervenant dans les coupes ne
	 sont jamais de type fixe, c'est a dire qu'il y a toujours une correspondance des
	 les variables du simplexe. Il faut donc ne pas mettre ces coupes. */
NbOddCyclesVioles = 0;

PNE_MiseAJourSeuilCoupes( Pne, COUPE_CLIQUE, &Seuil ); /* Meme seuil que pour les cliques */

for ( c = 0 ; c < OddCycles->NombreDeOddCycles ; c++ ) {
  if ( LeOddCycleEstDansLePool[c] == OUI_PNE ) continue;  
  il = First[c];
	if ( il < 0 ) continue; /* On ne sait jamais ... */	
  ilMax = il + NbElements[c];
	S = 0;
  Sec = (double) SecondMembre[c];
	Ok = OUI_PNE;
  while ( il < ilMax ) {
	  if ( Noeud[il] < Pivot ) {
	    if ( TypeDeBorne[Noeud[il]] == VARIABLE_FIXE ) {				
				Ok = NON_PNE;
			  break;
			}			
		  S += U[Noeud[il]];
		}
		else {
	    if ( TypeDeBorne[Noeud[il]-Pivot] == VARIABLE_FIXE ) {				
				Ok = NON_PNE;
			  break;
			}			
		  S -= U[Noeud[il]-Pivot];
			Sec -= 1;
		}
	  il++;
	}
	if ( Ok == OUI_PNE ) {
    if ( S > Sec + Seuil ) {
      NbOddCyclesVioles++;
	    X = S - Sec;
			printf("Odd hole viole %d de %e  valeur du pivot %d\n",c,X,Pivot);
      /* On Stocke la coupe */
      il = First[c];
		  NbT = 0;
      while ( il < ilMax ) {		
	      if ( Noeud[il] < Pivot ) {
		      Coeff[NbT] = 1;
			    Indice[NbT] = Noeud[il];			
			  }
	 	    else {
		      Coeff[NbT] = -1;
			    Indice[NbT] = Noeud[il] - Pivot;
		    }			
				printf(" %e*%d (Noeud=%d)",Coeff[NbT],Indice[NbT],Noeud[il]);
			  NbT++;
	      il++;
	    }
      B = Sec;
			printf(" < %e      SecondMembre %d    \n",B,SecondMembre[c]);

      if ( PNE_LaCoupeEstColineaire( Pne, Coeff, Indice, B, NbT ) == OUI_PNE ) continue;			
			
      PNE_EnrichirLeProblemeCourantAvecUneCoupe( Pne, 'K', NbT, B, X, Coeff , Indice );
      Pne->CoupesCalculees[Pne->NombreDeCoupesCalculees-1]->IndexDansOddHole = c;   			
	  }
	}
}

return;
}   















