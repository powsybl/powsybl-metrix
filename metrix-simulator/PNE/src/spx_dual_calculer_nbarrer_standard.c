/***********************************************************************

   FONCTION: Calcul de NBarre pour la variable sortante
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# define COEFF_VARIABLES   0.75

# define TEST_DES_TRES_PETITES_VALEURS OUI_SPX

# define VALEUR_NULLE 1.e-20

/*----------------------------------------------------------------------------*/

void SPX_DualCalculerNBarreRStandard( PROBLEME_SPX * Spx )

{
int il; int ilMax; double X; int i; int NombreDeContraintes;  int * NumerosDesVariablesHorsBase;
double * ErBMoinsUn; double * NBarreR; int * Cdeb; int * CNbTerm; double * ACol;
int * NumeroDeContrainte; int Cnt; int * Mdeb; int * NbTerm; int * Indcol; double * A;
int NombreDeVariablesHorsBase; int NombreDeVariables; char Methode; int Var;
char * PositionDeLaVariable; 

NombreDeContraintes         = Spx->NombreDeContraintes;   
ErBMoinsUn                  = Spx->ErBMoinsUn; 
NombreDeVariables           = Spx->NombreDeVariables;
NombreDeVariablesHorsBase   = Spx->NombreDeVariablesHorsBase;
NumerosDesVariablesHorsBase = Spx->NumerosDesVariablesHorsBase;
NBarreR                     = Spx->NBarreR;   

Spx->TypeDeStockageDeNBarreR = VECTEUR_SPX;

/* Si ErBMoinsUn est tres creux il vaut mieux la methode 1, sinon la methode 2 permet de faire
   un calcul plus precis */

if ( Spx->InverseProbablementDense == OUI_SPX ) {
  Methode = 2;
}
else {
  if ( NombreDeContraintes < (int) ceil ( COEFF_VARIABLES * NombreDeVariables ) ) {
    Methode = 1;
  }
  else { 
    Methode = 2;
  }
}

/* 2 methodes de calcul des produits scalaires */

if ( Methode == 1 ) {
  /*memset( (char *) NBarreR , 0 , NombreDeVariables * sizeof( double ) );*/
	{ int i;
	  for ( i = 0 ; i < NombreDeVariables ; i++ ) NBarreR[i] = 0;
	}	
  Mdeb   = Spx->Mdeb;
  NbTerm = Spx->NbTerm;

	PositionDeLaVariable = Spx->PositionDeLaVariable;
	
  Indcol = Spx->Indcol;
  A      = Spx->A;
  for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
    if ( ErBMoinsUn[Cnt] == 0.0 ) continue;
    X  = ErBMoinsUn[Cnt];		
    il = Mdeb[Cnt];
    ilMax = il + NbTerm[Cnt];
    while ( il < ilMax ) {
			if ( PositionDeLaVariable[Indcol[il]] != EN_BASE_LIBRE ) {
			  NBarreR[Indcol[il]] += X * A[il];
			}
      il++;
		}		    		
  }
	# if TEST_DES_TRES_PETITES_VALEURS == OUI_SPX
    for ( i = 0 ; i < NombreDeVariablesHorsBase ; i++ ) {
      if ( fabs( NBarreR[NumerosDesVariablesHorsBase[i]] ) < VALEUR_NULLE ) {
        NBarreR[NumerosDesVariablesHorsBase[i]] = 0.0;
      }			
		}
	# endif	
}
else {
  Cdeb               = Spx->Cdeb;
  CNbTerm            = Spx->CNbTerm;
  NumeroDeContrainte = Spx->NumeroDeContrainte;
  ACol               = Spx->ACol;	
  for ( i = 0 ; i < NombreDeVariablesHorsBase ; i++ ) {
	  Var = NumerosDesVariablesHorsBase[i];
    il    = Cdeb[Var];				
    ilMax = il + CNbTerm[Var];
		/*
    X = ErBMoinsUn[NumeroDeContrainte[il]] * ACol[il];
    il++;
    while ( il < ilMax ) {
      X+= ErBMoinsUn[NumeroDeContrainte[il]] * ACol[il];
      il++;
    }
		*/
		/* Normalement cela ne devrait pas se produire mais on veut se premunir contre le fait
		   qu'une variable hors base n'a aucun terme non nul dans sa colonne */
    X = 0;
    while ( il < ilMax ) {
      X+= ErBMoinsUn[NumeroDeContrainte[il]] * ACol[il];
      il++;
    }
	  # if TEST_DES_TRES_PETITES_VALEURS == OUI_SPX
      if ( fabs( X ) < VALEUR_NULLE ) X = 0.0;
	  # endif			
    NBarreR[Var] = X;		
  }	
}

return;
}



