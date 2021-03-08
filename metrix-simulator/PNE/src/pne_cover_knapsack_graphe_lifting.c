/***********************************************************************

   FONCTION: On essaie de lifter une knapsack avec le graphe de conflits.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"

# include "bb_define.h"
# include "bb_fonctions.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

/*----------------------------------------------------------------------------*/
/* Appele a chaque fois qu'on trouve une solution entiere */
void PNE_LifterKnapsackAvecLeGrapheDeConflits( PROBLEME_PNE * Pne, int * NombreDeTermes, double * SecMembre,
                                               double * Coefficients, int * IndiceColonne )
{ 
int NbTermes; int Pivot; int Var; int Noeud; int Nv; int Edge; int VarNv; int EdgeNv;
int NoeudAdjacentDeNv; int VarDeNoeudAdjacentDeNv; double S; int * First; int * Adjacent;
int * Next; char * VariablePresenteDansLaCoupe; int i; double ValeurMaxDeLaCoupe;
double * Xmin; double * Xmax; int i1; int * TypeDeBorne; double Alpha; double SecondMembre;

return;

if ( Pne->ConflictGraph == NULL ) return;

VariablePresenteDansLaCoupe = (char *) malloc( Pne->NombreDeVariablesTrav * sizeof( char ) );
if ( VariablePresenteDansLaCoupe == NULL ) return;

memset( (char *) VariablePresenteDansLaCoupe, NON_PNE, Pne->NombreDeVariablesTrav * sizeof( char ) );

NbTermes = *NombreDeTermes;
SecondMembre = *SecMembre;
for ( i = 0 ; i < NbTermes ; i++ ) VariablePresenteDansLaCoupe[IndiceColonne[i]] = OUI_PNE;

TypeDeBorne = Pne->TypeDeBorneTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
ValeurMaxDeLaCoupe = 0; 
for ( i = 0 ; i < NbTermes ; i++ ) {
  if ( Coefficients[i] > 0 ) ValeurMaxDeLaCoupe += Coefficients[i];	
}

Pivot = Pne->ConflictGraph->Pivot;
First = Pne->ConflictGraph->First;
Adjacent = Pne->ConflictGraph->Adjacent;  
Next = Pne->ConflictGraph->Next;

/* On essai d'ameliorer les coeff des variables de la coupe */
goto AAA;
for ( i = 0 ; i < NbTermes ; i++ ) {	
	Var = IndiceColonne[i];
	if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
	if ( Coefficients[i] > 0 ) Noeud = Var; /* On examine ce qu'il se passe quand la variable passe a 1 */
	else Noeud = Pivot + Var; /* On examine ce qu'il se passe quand la variable passe a 1 */
	S = ValeurMaxDeLaCoupe;	
  Edge = First[Noeud];
  while ( Edge >= 0 ) {
	  Nv = Adjacent[Edge];
		if ( Nv < Pivot ) VarNv = Nv;
		else VarNv = Nv - Pivot;
		if ( TypeDeBorne[VarNv] == VARIABLE_FIXE ) goto NextEdge0;
	  if ( VariablePresenteDansLaCoupe[VarNv] == NON_PNE ) goto NextEdge0;
		if ( Nv < Pivot ) {
			/* Si Var = 1 ou 0 (selon la valeur de Noeud ) alors VarNv = 0 */
      /* Recherche du coeff */
			for ( i1 = 0 ; i1 < NbTermes ; i1++ ) {			
				if ( IndiceColonne[i1] == VarNv ) {
          if ( Coefficients[i1] > 0 ) S -= Coefficients[i1];
					break;							
				}				
			}
		}					
		else {
			/* Si Var = 1 ou 0 (selon la valeur de Noeud) alors VarNv = 1 */
      /* Recherche du coeff */
			for ( i1 = 0 ; i1 < NbTermes ; i1++ ) {			
				if ( IndiceColonne[i1] == VarNv ) {
          if ( Coefficients[i1] < 0 ) S += Coefficients[i1];													
					break;
				}				
			}					
		}
	  NextEdge0:			
	  Edge = Next[Edge];
	}
	if ( S < SecondMembre ) {
	  printf("Augentation d'un coeff\n");
		Alpha = SecondMembre-S;
    if ( Var < Pivot ) {
			/* Correspond au cas ou la valeur de VarNv est 1 */
      Coefficients[i] += Alpha;
		}
		else {
			/* Correspond au cas ou la valeur de VarNv est 0 */
      Coefficients[i] -= Alpha;
			SecondMembre -= Alpha;							
		}							
		/* Recalcul de ValeurMaxDeLaCoupe */
    ValeurMaxDeLaCoupe = 0; 
    for ( i1 = 0 ; i1 < NbTermes ; i1++ ) {
      if ( Coefficients[i1] > 0 ) ValeurMaxDeLaCoupe += Coefficients[i1];	
    }		
	}		  	
}
AAA:
/* On essai d'ajouter d'autres variables */

for ( i = 0 ; i < NbTermes ; i++ ) {
	Var = IndiceColonne[i];
	Noeud = Var;
  /* On examine les noeuds voisins de Noeud qui ne sont pas dans la knapsack */
  Edge = First[Noeud];
  while ( Edge >= 0 ) {
	  Nv = Adjacent[Edge];
		if ( Nv < Pivot ) VarNv = Nv;
		else VarNv = Nv - Pivot;

		if ( TypeDeBorne[VarNv] == VARIABLE_FIXE ) goto NextEdge1;
	  if ( VariablePresenteDansLaCoupe[VarNv] == OUI_PNE ) goto NextEdge1;

		S = ValeurMaxDeLaCoupe;
    EdgeNv = First[Nv];
    while ( EdgeNv >= 0 ) {
			NoeudAdjacentDeNv = Adjacent[EdgeNv];
		  if ( NoeudAdjacentDeNv < Pivot ) {
				VarDeNoeudAdjacentDeNv = NoeudAdjacentDeNv;
				/* Si VarNv = 1 ou 0 (selon la valeur de Nv) alors VarDeNoeudAdjacentDeNv = 0 */
			  if ( VariablePresenteDansLaCoupe[VarDeNoeudAdjacentDeNv] == OUI_PNE ) {
          /* Recherche du coeff */
					for ( i1 = 0 ; i1 < NbTermes ; i1++ ) {
						if ( IndiceColonne[i1] == VarDeNoeudAdjacentDeNv ) {
              if ( Coefficients[i1] > 0 ) S -= Coefficients[i1];
							break;							
						}
					}
				}					
			}
			else {
				VarDeNoeudAdjacentDeNv = NoeudAdjacentDeNv - Pivot;
				/* Si VarNv = 1 ou 0 (selon la valeur de Nv) alors VarDeNoeudAdjacentDeNv = 1 */
			  if ( VariablePresenteDansLaCoupe[VarDeNoeudAdjacentDeNv] == OUI_PNE ) {
          /* Recherche du coeff */
					for ( i1 = 0 ; i1 < NbTermes ; i1++ ) {
						if ( IndiceColonne[i1] == VarDeNoeudAdjacentDeNv ) {
              if ( Coefficients[i1] < 0 ) S += Coefficients[i1];								
						  break;
						}
					}
				}					
			}
	    EdgeNv = Next[EdgeNv];
		}
		if ( S < SecondMembre ) {
      VariablePresenteDansLaCoupe[VarNv] = OUI_PNE;
      IndiceColonne[NbTermes] = VarNv;
			Alpha = SecondMembre-S;
      if ( Nv < Pivot ) {
			  /* Correspond au cas ou la valeur de VarNv est 1 */
			  printf("On lifte la coupe avec la variable %d S = %e SecondMembre = %e Coefficient possible %e\n",
					      VarNv,S,SecondMembre,Alpha);				 				
        Coefficients[NbTermes] = Alpha;
			}
			else {
			  /* Correspond au cas ou la valeur de VarNv est 0 */
			  printf("On lifte la coupe avec la variable %d S = %e SecondMembre = %e Coefficient possible %e\n",
					      VarNv,S,SecondMembre,-Alpha);						
        Coefficients[NbTermes] = -Alpha;
			  SecondMembre -= Alpha;							
			}							
			/* Recalcul de ValeurMaxDeLaCoupe */
      if ( Coefficients[NbTermes] > 0 ) ValeurMaxDeLaCoupe += Coefficients[NbTermes];			
      NbTermes++;			
		}		
		NextEdge1:
	  Edge = Next[Edge];
  }  	
}

*NombreDeTermes = NbTermes;
*SecMembre = SecondMembre;

free( VariablePresenteDansLaCoupe );

return;
}
