/***********************************************************************

   FONCTION: Calcul de NBarre pour la variable sortante dans le cas
	           hyper creux.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "lu_define.h"

# define TEST_DES_TRES_PETITES_VALEURS OUI_SPX

# define VALEUR_NULLE 1.e-17 /* 1.e-20 */

/*----------------------------------------------------------------------------*/

void SPX_DualCalculerNBarreRHyperCreux( PROBLEME_SPX * Spx )
{
int Var; int il; int ilMax; double X; int i; double * ErBMoinsUn; 
int Cnt; int * Indcol; double * A; int j; char * T; char * PositionDeLaVariable;
int * IndexTermesNonNulsDeErBMoinsUn; int NbTermesNonNulsDeErBMoinsUn; int * NumVarNBarreRNonNul;
int NombreDeValeursNonNullesDeNBarreR; double * ACol; int * Cdeb;
int * CNbTerm;	int * NumeroDeContrainte; int * NumerosDesVariablesHorsBase;
double * NBarreR; double * Erb; int * Mdeb; int * NbTerm; 
MATRICE * Matrice;

# ifdef ENLEVER_LES_VARIABLES_XMIN_EGAL_XMAX_DE_LA_LISTE_HORS_BASE
  double * Xmin; double * Xmax; 
# endif
# if TEST_DES_TRES_PETITES_VALEURS == NON_SPX
  int jFin;
# endif

ErBMoinsUn = Spx->ErBMoinsUn;
IndexTermesNonNulsDeErBMoinsUn = Spx->IndexTermesNonNulsDeErBMoinsUn;
NBarreR = Spx->NBarreR;									
NbTermesNonNulsDeErBMoinsUn = Spx->NbTermesNonNulsDeErBMoinsUn;

if ( NbTermesNonNulsDeErBMoinsUn < 0.3 * Spx->NombreDeContraintes ) {
  /* Cette methode pose des problemes de precision car il y a plus d'allers
	   retour en memoire or a chaque aller retour il y a un ecretage */
  Spx->TypeDeStockageDeNBarreR = ADRESSAGE_INDIRECT_SPX;
	
  Mdeb   = Spx->Mdeb;
  NbTerm = Spx->NbTerm;
  Indcol = Spx->Indcol; 
  A      = Spx->A;

	PositionDeLaVariable = Spx->PositionDeLaVariable;

  T = Spx->T;
  NumVarNBarreRNonNul = Spx->NumVarNBarreRNonNul;
  NombreDeValeursNonNullesDeNBarreR = 0;
			
	for ( j = 0 ; j < NbTermesNonNulsDeErBMoinsUn ; j++ ) {
    X = ErBMoinsUn[j];
    Cnt = IndexTermesNonNulsDeErBMoinsUn[j];						
    il = Mdeb[Cnt];
    ilMax = il + NbTerm[Cnt];
    while ( il < ilMax ) {
	    Var = Indcol[il];						
			if ( PositionDeLaVariable[Var] != EN_BASE_LIBRE ) {
        if ( T[Var] == 1 ) {
			    NBarreR[Var] += X * A[il];			
        }
			  else {
			    T[Var] = 1;
		 	    NBarreR[Var] = X * A[il];
			 	  NumVarNBarreRNonNul[NombreDeValeursNonNullesDeNBarreR] = Var;
          NombreDeValeursNonNullesDeNBarreR++;			
			  }
			}						
      il++;
		}
	}
	
	# if TEST_DES_TRES_PETITES_VALEURS == OUI_SPX	
	  if ( Spx->PresenceDeVariablesDeBornesIdentiques != OUI_SPX ) {		
      for ( i = 0 ; i < NombreDeValeursNonNullesDeNBarreR ; i++ ) {
	      T[NumVarNBarreRNonNul[i]] = 0;
        if ( fabs( NBarreR[NumVarNBarreRNonNul[i]] ) < VALEUR_NULLE ) {				
			    NombreDeValeursNonNullesDeNBarreR--;
	        NumVarNBarreRNonNul[i] = NumVarNBarreRNonNul[NombreDeValeursNonNullesDeNBarreR];
			    i--;
		    }	
	    }
    }
	  else {		
	    Xmin = Spx->Xmin;
	    Xmax = Spx->Xmax;
      for ( i = 0 ; i < NombreDeValeursNonNullesDeNBarreR ; i++ ) {
			  Var = NumVarNBarreRNonNul[i];
	      T[Var] = 0;				
        if ( fabs( NBarreR[Var] ) < VALEUR_NULLE || Xmin[Var] == Xmax[Var] ) {				
			    NombreDeValeursNonNullesDeNBarreR--;
	        NumVarNBarreRNonNul[i] = NumVarNBarreRNonNul[NombreDeValeursNonNullesDeNBarreR];
			    i--;
		    }				
	    }			
	  }
	# else 
    for ( i = 0 ; i < NombreDeValeursNonNullesDeNBarreR ; i++ ) T[NumVarNBarreRNonNul[i]] = 0;	

	  /* Il faut supprimer les variables pour lesquelles Xmin = Xmax */
		if ( Spx->PresenceDeVariablesDeBornesIdentiques == OUI_SPX ) {
	    Xmin = Spx->Xmin;
	    Xmax = Spx->Xmax;		
      j = 0;
	    jFin = NombreDeValeursNonNullesDeNBarreR - 1;		
	    while ( j <= jFin ) {
	      Var = NumVarNBarreRNonNul[j];						
	      if ( Xmin[Var] == Xmax[Var] ) {			
	        NumVarNBarreRNonNul[j] = NumVarNBarreRNonNul[jFin];
		      jFin--;
		 	    continue;
	      }
		    j++;
	    }
	   	NombreDeValeursNonNullesDeNBarreR = jFin + 1;
		}
  # endif

  Spx->NombreDeValeursNonNullesDeNBarreR = NombreDeValeursNonNullesDeNBarreR;
			
}
else {

  Spx->TypeDeStockageDeNBarreR = ADRESSAGE_INDIRECT_SPX /*VECTEUR_SPX*/;
  NumVarNBarreRNonNul = Spx->NumVarNBarreRNonNul;
  NombreDeValeursNonNullesDeNBarreR = 0;
	
	/* Expand de ErBMoinsUn */
	
  Matrice = Spx->MatriceFactorisee;
  Erb = Matrice->W;  
	
	for ( j = 0 ; j < NbTermesNonNulsDeErBMoinsUn ; j++ ) Erb[IndexTermesNonNulsDeErBMoinsUn[j]] = ErBMoinsUn[j];
 	ErBMoinsUn = Erb;
		
  Cdeb               = Spx->Cdeb;
  CNbTerm            = Spx->CNbTerm;
  NumeroDeContrainte = Spx->NumeroDeContrainte;
  ACol               = Spx->ACol;
	NumerosDesVariablesHorsBase = Spx->NumerosDesVariablesHorsBase;
	
  for ( i = 0 ; i < Spx->NombreDeVariablesHorsBase ; i++ ) {
	  Var = NumerosDesVariablesHorsBase[i];					
    il    = Cdeb[Var];
    ilMax = il + CNbTerm[Var];
		/* Normalement cela ne devrait pas se produire mais on veut se premunir contre le fait
		   qu'une variable hors base n'a aucun terme non nul dans sa colonne */
    X = 0.0;
    while ( il < ilMax ) {
      X += ErBMoinsUn[NumeroDeContrainte[il]] * ACol[il];
      il++;
    }

		# if TEST_DES_TRES_PETITES_VALEURS == OUI_SPX
		  if ( fabs( X ) > VALEUR_NULLE ) {
        NBarreR[Var] = X;
			  NumVarNBarreRNonNul[NombreDeValeursNonNullesDeNBarreR] = Var;
        NombreDeValeursNonNullesDeNBarreR++;			
		  }
		# else 				
		  if ( X != 0.0 ) {
        NBarreR[Var] = X;
			  NumVarNBarreRNonNul[NombreDeValeursNonNullesDeNBarreR] = Var;
        NombreDeValeursNonNullesDeNBarreR++;				
		  }
		# endif
		
  }
			
  Spx->NombreDeValeursNonNullesDeNBarreR = NombreDeValeursNonNullesDeNBarreR;
	
	for ( j = 0 ; j < NbTermesNonNulsDeErBMoinsUn ; j++ ) Erb[IndexTermesNonNulsDeErBMoinsUn[j]] = 0.0;	
	
}
			
return;
}





