/***********************************************************************

   FONCTION: Application des eta vecteurs pour la forme produit de
	           l'inverse
	 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"
   
# include "lu_define.h"

/*----------------------------------------------------------------------------*/
/* Reste ADRESSAGE_INDIRECT_LU a pogrammer */
void SPX_AppliquerLesEtaVecteurs( PROBLEME_SPX * Spx,
                                  double * A,
                                  int * IndexDesTermesNonNuls,
			          int * NombreDeTermesNonNuls,
				  char   CalculEnHyperCreux,
				  char   TypeDeStockage /* COMPACT_LU
							   ADRESSAGE_INDIRECT_LU
							   VECTEUR_LU */ 
      		                )
{
int   k; int Colonne; int il; int ilMax; 
double S; int * EtaIndiceLigne; double * EtaMoins1Valeur   ;
int * EtaNbTerm               ; int * EtaColonne          ;

int j; int NbN; int Cnt; MATRICE * Matrice; char * T; double * W;
	
EtaIndiceLigne  = Spx->EtaIndiceLigne;
EtaMoins1Valeur = Spx->EtaMoins1Valeur;
EtaNbTerm       = Spx->EtaNbTerm;
EtaColonne      = Spx->EtaColonne;
  
if ( CalculEnHyperCreux == OUI_SPX ) {
  if ( TypeDeStockage == COMPACT_LU ) goto HyperCreux;
  else if ( TypeDeStockage == ADRESSAGE_INDIRECT_LU ) goto HyperCreux;
	else if ( TypeDeStockage != VECTEUR_LU ){
	  printf("AppliquerLesEtaVecteurs: type de stockage non gere\n");
	  exit(0);
  }
}

k  = 0;
il = 0;
while ( k < Spx->NombreDeChangementsDeBase ) {	
  ilMax   = il + EtaNbTerm[k];
  Colonne = EtaColonne[k];
  S          = A[Colonne];
  A[Colonne] = 0.;
  if ( S != 0.0 ) {		  			
    while ( il != ilMax ) {			    				
      A[EtaIndiceLigne[il]]+= EtaMoins1Valeur[il] * S;
      il++;
    }
  }
  else il = ilMax;
  k++;
}
return;

HyperCreux:

Matrice = Spx->MatriceFactorisee;
T = Matrice->Marqueur;

NbN = *NombreDeTermesNonNuls;

if ( TypeDeStockage == COMPACT_LU ) {
  W = Matrice->W;  
  for ( j = 0 ; j < NbN ; j++ ) {
    Cnt = IndexDesTermesNonNuls[j];
    W[Cnt] = A[j];
	  T[Cnt] = 1;
  }
}
else {
  W = A;
  for ( j = 0 ; j < NbN ; j++ ) T[IndexDesTermesNonNuls[j]] = 1;
}

k  = 0;
il = 0;
while ( k < Spx->NombreDeChangementsDeBase ) {	
  ilMax   = il + EtaNbTerm[k];
  Colonne = EtaColonne[k];
  S          = W[Colonne];
  W[Colonne] = 0.;
  if ( S != 0.0 ) {		  			
    while ( il != ilMax ) {
		  Cnt = EtaIndiceLigne[il];
      W[Cnt] += EtaMoins1Valeur[il] * S;
			if ( T[Cnt] == 0 ) {
			  T[Cnt] = 1;
	      IndexDesTermesNonNuls[NbN] = Cnt;				
				NbN++;
			}
      il++;
    }
  }
  else il = ilMax;
  k++;
}

if ( TypeDeStockage == COMPACT_LU ) {
  for ( j = 0 ; j < NbN ; j++ ) {
    Cnt = IndexDesTermesNonNuls[j];
	  A[j] = W[Cnt];
    W[Cnt] = 0;
	  T[Cnt] = 0;
  }
}
else {
  for ( j = 0 ; j < NbN ; j++ ) T[IndexDesTermesNonNuls[j]] = 0;
}

/*printf("AppliquerLesEtaVecteurs: NombreDeTermesNonNuls %d  %d\n",*NombreDeTermesNonNuls,NbN);*/

*NombreDeTermesNonNuls = NbN;

return;
}


