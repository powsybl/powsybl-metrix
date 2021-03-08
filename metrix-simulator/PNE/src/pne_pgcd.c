/***********************************************************************

   FONCTION: Calcul de pgcd d'une liste de nombres entiers.
	 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"
# include "pne_fonctions.h"

/*----------------------------------------------------------------------------*/

void PNE_Pgcd( int NbTermes, long * Coeff, long * Pgcd )
{
long i; long a; long b; long c; 

*Pgcd = 1;
if ( NbTermes < 2 ) {
  *Pgcd = 0;
	return;
}

/* Calcul du GCD des coeffs */
if ( Coeff[0] > Coeff[1] ) { a = Coeff[0]; b = Coeff[1]; }
else                       { a = Coeff[1]; b = Coeff[0]; }

i = 1;
GCD:
if ( a == 0 && b == 0 ) c = 0;
else if ( b == 0 ) c = a;
else if ( a == 0 ) c = b;
else {
  c = a % b;
  while ( c != 0 ) { 
    a = b;
    b = c;
    c = a % b;
  }
  c = b;
}

i++;
if ( i < NbTermes ) {
  if ( Coeff[i] > c ) { a = Coeff[i]; b = c; }
  else                { a = c; b = Coeff[i]; }
	goto GCD;
}

*Pgcd = c;

/*printf("Pgcd %ld \n",*Pgcd);*/

return;
}
