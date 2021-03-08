/***********************************************************************

   FONCTION: Calcul de la granularite de la fonction objectif
	 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES 1

# define ZERO_COEFF_ENTIER 1.e-8
# define LIMITE_FACTEUR_MULTIPLICATIF 1.e+30

/*----------------------------------------------------------------------------*/

void PNE_GranulariteDuCout( PROBLEME_PNE * Pne , double * Granularite )
{
double FacteurMultiplicatif; long Pgcd; double * Xmin; double * Xmax; double * X;
double * L; int * TypeDeVariable; int * TypeDeBorne; int NbTermes; long * Coeff;
int NombreDeVariables; int Var; 
double a;

*Granularite = 0;
FacteurMultiplicatif = 1;
Pgcd = 0;

NombreDeVariables = Pne->NombreDeVariablesTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
X = Pne->UTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
L = Pne->LTrav;

Coeff = (long *) malloc( NombreDeVariables * sizeof( long ) );
if ( Coeff == NULL ) {
  printf("Saturation memoire dans PNE_GranulariteDuCout\n");
  return;
}
/* Test: toutes les variables a cout non nul sont entieres ? */

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( L[Var] == 0 ) continue;
	if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
	if ( Xmin[Var] == Xmax[Var] ) continue;
  if ( TypeDeVariable[Var] == ENTIER ) goto MajFacteurMultiplicatif;
  # if TRACES == 1
    /*printf("Les cout contient des variables continues non fixes\n");*/
	# endif
	goto FinGranularite;
	MajFacteurMultiplicatif:
  while ( 1 ) {
	  a = fabs( L[Var] ) * FacteurMultiplicatif;
	  if ( a - floor( a ) < ZERO_COEFF_ENTIER || ceil ( a ) - a < ZERO_COEFF_ENTIER ) break;
    FacteurMultiplicatif *= 10;
		if ( FacteurMultiplicatif > LIMITE_FACTEUR_MULTIPLICATIF ) {
		  printf("Limite atteinte pour le facteur multiplicatif %e valeur de L %e a = %e\n",FacteurMultiplicatif,L[Var],a);
		  printf(" a - floor( a ) = %e\n",a - floor( a ));
		  printf(" ceil ( a ) - a = %e\n",ceil ( a ) - a);			
		  goto FinGranularite;
		}
	}	
}

/*printf("On peut calculer une granularite FacteurMultiplicatif %e \n",FacteurMultiplicatif);*/

NbTermes = 0;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( L[Var] == 0 ) continue;
	if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
	if ( Xmin[Var] == Xmax[Var] ) continue;
  if ( TypeDeVariable[Var] == ENTIER ) {
	  Coeff[NbTermes] = (long) ( fabs( L[Var] ) * FacteurMultiplicatif );
	  /*printf("Coeff %ld fabs( L[Var] ) %e\n",Coeff[NbTermes],fabs( L[Var] ));*/
	  NbTermes++;
	}	
}

PNE_Pgcd( NbTermes, Coeff, &Pgcd );

if ( Pgcd > 1 ) {
  *Granularite = (double) Pgcd / FacteurMultiplicatif;
  printf("Fonction objectif Pgcd %ld Granularite %e NbTermes %d\n",Pgcd,*Granularite,NbTermes);
}

FinGranularite:

free ( Coeff );

return;
}
