/***********************************************************************

   FONCTION: Prise en compte des substitutions de variables dans les
	           cliques.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"
  
# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

void PNE_NettoyerClique( int * , int * , int * , int , int , int , int );
void PNE_AnnulerLesVariablesDeLaClique( PROBLEME_PNE * , int * , int * , int * , int , int , int , int );
																				
/*----------------------------------------------------------------------------*/

void PNE_NettoyerClique( int * First, int * NbElements, int * NoeudDeClique,
                         int c, int VarSubstituee, int VarRestante, int Pivot )
{
int il; int ilMax; int Var;

il = First[c];
ilMax = il + NbElements[c];
NbElements[c] = 0;
while ( il < ilMax ) {
  if ( NoeudDeClique[il] < Pivot ) Var = NoeudDeClique[il];
  else Var = NoeudDeClique[il] - Pivot;				
  if ( Var == VarSubstituee || Var == VarRestante ) {
    NoeudDeClique[il] = NoeudDeClique[ilMax-1];
    ilMax--;
  }
  else {
    il++;
    NbElements[c]++;
  }
}
if ( NbElements[c] <= 1 ) {
  First[c] = -1;
  NbElements[c] = 0;
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_AnnulerLesVariablesDeLaClique( PROBLEME_PNE * Pne, int * First, int * NbElements, int * NoeudDeClique,
                                        int c, int VarSubstituee, int VarRestante, int Pivot )
{
int il; int ilMax; int Var; double NouvelleValeur; int Noeud; int Complement;

il = First[c];
ilMax = il + NbElements[c];
while ( il < ilMax ) {
  if ( NoeudDeClique[il] < Pivot ) Var = NoeudDeClique[il];
  else Var = NoeudDeClique[il] - Pivot;				
  if ( Var != VarSubstituee && Var != VarRestante ) {
    if ( NoeudDeClique[il] < Pivot ) NouvelleValeur = 0; 
    else NouvelleValeur = 1;
    Pne->UTrav[Var] = NouvelleValeur;      
    Pne->UminTrav[Var] = NouvelleValeur;
    Pne->UmaxTrav[Var] = NouvelleValeur;
    Pne->TypeDeBorneTrav[Var] = VARIABLE_FIXE;			
    Pne->TypeDeVariableTrav[Var] = REEL;

    Noeud = Var;
    Complement = Pne->ConflictGraph->Pivot + Var;
    /* On elimine l'entree du noeud dans le graphe */
    PNE_ConflictGraphSupprimerUnNoeud( Noeud, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
    PNE_ConflictGraphSupprimerUnNoeud( Complement, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
										
  }
  il++;
}
/* et la clique est supprimee */
First[c] = -1;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_SubstituerUneVariableDansCliques( PROBLEME_PNE * Pne, int VarSubstituee,
                                           int VarRestante, double BTilde )
{
int il; int ilVarRestante; int * First; int * NbElements; int Noeud; int Complement;
int * NoeudDeClique; CLIQUES * Cliques; int c; int Pivot; int ilMax; int Var;
int ilVarSubtituee; 

if ( Pne->Cliques == NULL ) return;

Cliques = Pne->Cliques;
First = Cliques->First;  
NbElements = Cliques->NbElements;
NoeudDeClique = Cliques->Noeud;
Pivot = Pne->ConflictGraph->Pivot;
				
for ( c = 0 ; c < Cliques->NombreDeCliques ; c++ ) {				
  il = First[c];
  if ( il < 0 ) continue; /* On ne sait jamais ... */	
  ilMax = il + NbElements[c];	
	ilVarSubtituee = -1;
	ilVarRestante = -1;
  while ( il < ilMax ) {
	  if ( NoeudDeClique[il] < Pivot ) Var = NoeudDeClique[il];
		else Var = NoeudDeClique[il] - Pivot;				
    if ( Var == VarSubstituee ) {		
			ilVarSubtituee = il;	
		}
		else if ( Var == VarRestante ) {		
			ilVarRestante = il;	
		}		
		il++;
	}
	if ( ilVarSubtituee < 0 ) continue;	
		
  if ( BTilde == 0 ) {
		/* Cas x = y */
		if ( ilVarRestante < 0 ) {		
      if ( NoeudDeClique[ilVarSubtituee] < Pivot ) NoeudDeClique[ilVarSubtituee] = VarRestante; 
			else NoeudDeClique[ilVarSubtituee] = VarRestante + Pivot;			
		}
		else {		
      if ( NoeudDeClique[ilVarSubtituee] < Pivot ) {			
				if ( NoeudDeClique[ilVarRestante] < Pivot ) {						
          /* Dans ce cas VarRestante est obligatoirement nulle */					
  	      Pne->UTrav[VarRestante] = 0;      
	        Pne->UminTrav[VarRestante] = 0;
          Pne->UmaxTrav[VarRestante] = 0;
        	Pne->TypeDeBorneTrav[VarRestante] = VARIABLE_FIXE;			
	        Pne->TypeDeVariableTrav[VarRestante] = REEL;
								
					/* Suppression de VarRestante dans le graphe */
          Noeud = VarRestante;
          Complement = Pivot + VarRestante;
          /* On elimine l'entree du noeud dans le graphe */
          PNE_ConflictGraphSupprimerUnNoeud( Noeud, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
          PNE_ConflictGraphSupprimerUnNoeud( Complement, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
								               							
					/* Le reste de la clique est utilisable */
          PNE_NettoyerClique( First, NbElements, NoeudDeClique, c, VarSubstituee, VarRestante, Pivot );						
        }				
				else {				
          /* Dans ce cas les autres variables de la clique sont nulles */				
          PNE_AnnulerLesVariablesDeLaClique( Pne, First, NbElements, NoeudDeClique, c, VarSubstituee, VarRestante, Pivot );						
				}
			}
			else {
				if ( NoeudDeClique[ilVarRestante] < Pivot ) {
          /* Dans ce cas les autres variables de la clique sont nulles */					
          PNE_AnnulerLesVariablesDeLaClique( Pne, First, NbElements, NoeudDeClique, c, VarSubstituee, VarRestante, Pivot );																			
				}
				else {
          /* Dans ce cas VarRestante est obligatoirement egale a 1 */
 	        Pne->UTrav[VarRestante] = 1;      
	        Pne->UminTrav[VarRestante] = 1;
          Pne->UmaxTrav[VarRestante] = 1;
        	Pne->TypeDeBorneTrav[VarRestante] = VARIABLE_FIXE;			
	        Pne->TypeDeVariableTrav[VarRestante] = REEL;

					/* Suppression de VarRestante dans le graphe */
          Noeud = VarRestante;
          Complement = Pne->ConflictGraph->Pivot + Noeud;
          /* On elimine l'entree du noeud dans le graphe */
          PNE_ConflictGraphSupprimerUnNoeud( Noeud, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
          PNE_ConflictGraphSupprimerUnNoeud( Complement, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );										
																
				  /* Le reste de la clique est utilisable */
          PNE_NettoyerClique( First, NbElements, NoeudDeClique, c, VarSubstituee, VarRestante, Pivot );										 
		    }								
	    }
		}		 
	}
	else {	
	  /* Cas x = 1-y */
		if ( ilVarRestante < 0 ) {		
      if ( NoeudDeClique[ilVarSubtituee] < Pivot ) NoeudDeClique[ilVarSubtituee] = VarRestante + Pivot;	
			else NoeudDeClique[ilVarSubtituee] = VarRestante; 													
		}
		else {		
      if ( NoeudDeClique[ilVarSubtituee] < Pivot ) {
			  if ( NoeudDeClique[ilVarRestante] < Pivot ) {				
          /* Dans ce cas les autres variables de la clique sont nulles */					
          PNE_AnnulerLesVariablesDeLaClique( Pne, First, NbElements, NoeudDeClique, c, VarSubstituee, VarRestante, Pivot );
				}
        else {
			    /* Dans ce cas VarRestante est egal a 1 */										
 	        Pne->UTrav[VarRestante] = 1;      
	        Pne->UminTrav[VarRestante] = 1;
          Pne->UmaxTrav[VarRestante] = 1;
        	Pne->TypeDeBorneTrav[VarRestante] = VARIABLE_FIXE;			
	        Pne->TypeDeVariableTrav[VarRestante] = REEL;

					/* Suppression de VarRestante dans le graphe */
          Noeud = VarRestante;
          Complement = Pne->ConflictGraph->Pivot + Noeud;
          /* On elimine l'entree du noeud dans le graphe */
          PNE_ConflictGraphSupprimerUnNoeud( Noeud, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
          PNE_ConflictGraphSupprimerUnNoeud( Complement, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );											
								
					/* Le reste de la clique est utilisable */
          PNE_NettoyerClique( First, NbElements, NoeudDeClique, c, VarSubstituee, VarRestante, Pivot );										                															
				}							
			}
			else {
				if ( NoeudDeClique[ilVarRestante] < Pivot ) {
          /* Dans ce cas VarRestante est egal a 0 */					
  	      Pne->UTrav[VarRestante] = 0;      
	        Pne->UminTrav[VarRestante] = 0;
          Pne->UmaxTrav[VarRestante] = 0;
        	Pne->TypeDeBorneTrav[VarRestante] = VARIABLE_FIXE;			
	        Pne->TypeDeVariableTrav[VarRestante] = REEL;

					/* Suppression de VarRestante dans le graphe */
          Noeud = VarRestante;
          Complement = Pne->ConflictGraph->Pivot + Noeud;
          /* On elimine l'entree du noeud dans le graphe */
          PNE_ConflictGraphSupprimerUnNoeud( Noeud, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );
          PNE_ConflictGraphSupprimerUnNoeud( Complement, Pne->ConflictGraph->First, Pne->ConflictGraph->Adjacent, Pne->ConflictGraph->Next );													
								
					/* Le reste de la clique est utilisable */
          PNE_NettoyerClique( First, NbElements, NoeudDeClique, c, VarSubstituee, VarRestante, Pivot );					            												
				}
				else {
          /* Dans ce cas les autres variables de la clique sont nulles */					
          PNE_AnnulerLesVariablesDeLaClique( Pne, First, NbElements, NoeudDeClique, c, VarSubstituee, VarRestante, Pivot );
				}																							 														  				
			}
		}  		
	}					
}

return;

}

/*************************************************************************/


