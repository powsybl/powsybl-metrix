/***********************************************************************

   FONCTION: Application des eta vecteurs pour la forme produit de
	           l'inverse dans le calcul d'une resolution de systeme
						 avec forme produit de l'inverse.

   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "lu_define.h"

/*----------------------------------------------------------------------------*/
/* Reste ADRESSAGE_INDIRECT_LU a pogrammer */
void SPX_AppliquerLesEtaVecteursTransposee( PROBLEME_SPX * Spx,
                                            double * U /* Second membre et resultat */,
                                            int * IndexDesTermesNonNuls,
					    int * NombreDeTermesNonNuls,
				            char   CalculEnHyperCreux,
	                                    char   TypeDeStockage /* COMPACT_LU
                                                                     ADRESSAGE_INDIRECT_LU
				          	                     VECTEUR_LU */ 
					  )
{
int k; int il; int ilMax; double S; int * EtaDeb; int * EtaNbTerm;
int * EtaIndiceLigne; int * EtaColonne;  double * EtaMoins1Valeur;

int j; int NbN; int Col; MATRICE * Matrice; char * T; double * W;

EtaDeb    = Spx->EtaDeb;
EtaNbTerm = Spx->EtaNbTerm;
EtaIndiceLigne  = Spx->EtaIndiceLigne;
EtaColonne      = Spx->EtaColonne;
EtaMoins1Valeur = Spx->EtaMoins1Valeur;   

if ( CalculEnHyperCreux == OUI_SPX ) {
  if ( TypeDeStockage == COMPACT_LU ) goto HyperCreux;
  if ( TypeDeStockage == ADRESSAGE_INDIRECT_LU ) {
	  printf("Attention TypeDeStockage = ADRESSAGE_INDIRECT_LU pas teste dans SPX_AppliquerLesEtaVecteursTransposee\n");
	  goto HyperCreux;
	}
	else if ( TypeDeStockage != VECTEUR_LU ) {
	  printf("AppliquerLesEtaVecteursTransposee: type de stockage non gere\n");
	  exit(0);
	}
}

k  = Spx->NombreDeChangementsDeBase - 1;
while ( k >= 0 ) {
  il    = EtaDeb[k];
  ilMax = il + EtaNbTerm[k];
  S     = 0.;
  while ( il < ilMax ) {
    S+= EtaMoins1Valeur[il] * U[EtaIndiceLigne[il]];
    il++;
  }
  /* Colonne egal aussi contrainte pour le terme diagonal */
  U[EtaColonne[k]] = S;
  k--;
}
return;

HyperCreux:

Matrice = Spx->MatriceFactorisee;
T = Matrice->Marqueur;

NbN = *NombreDeTermesNonNuls;

if ( TypeDeStockage == COMPACT_LU ) {
  W = Matrice->W;  
  for ( j = 0 ; j < NbN ; j++ ) {	
    Col = IndexDesTermesNonNuls[j];
    W[Col] = U[j];
	  T[Col] = 1;
  }
}
else {
  W = U;
  for ( j = 0 ; j < NbN ; j++ ) T[IndexDesTermesNonNuls[j]] = 1;
}

k  = Spx->NombreDeChangementsDeBase - 1;
while ( k >= 0 ) {
  il    = EtaDeb[k];
  ilMax = il + EtaNbTerm[k];
  S     = 0.;
  while ( il < ilMax ) {
    S+= EtaMoins1Valeur[il] * W[EtaIndiceLigne[il]];
    il++;
  }
  /* Colonne egal aussi contrainte pour le terme diagonal */
	Col = EtaColonne[k];
	if ( S != 0.0 ) {
    W[Col] = S;
		if ( T[Col] == 0 ) {
		  T[Col] = 1;
	    IndexDesTermesNonNuls[NbN] = Col;				
			NbN++;
		}
	}
	else W[Col] = 0.0;
  k--;
}

if ( TypeDeStockage == COMPACT_LU ) {
  for ( j = 0 ; j < NbN ; j++ ) {
    Col = IndexDesTermesNonNuls[j];
	  U[j] = W[Col];
    W[Col] = 0;
	  T[Col] = 0;
  }
}
else {
  for ( j = 0 ; j < NbN ; j++ ) T[IndexDesTermesNonNuls[j]] = 0;
}

/*printf("AppliquerLesEtaVecteursTransposee: NombreDeTermesNonNuls %d  %d\n",*NombreDeTermesNonNuls,NbN);*/

*NombreDeTermesNonNuls = NbN;

return;
}
