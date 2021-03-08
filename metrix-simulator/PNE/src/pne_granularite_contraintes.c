/***********************************************************************

   FONCTION: Calcul de la granularite des contraintes d'egalite
	 
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

void PNE_GranulariteDesContraintes( PROBLEME_PNE * Pne )
{
double FacteurMultiplicatif; long Pgcd; double * Xmin; double * Xmax; double * X;
double * L; int * TypeDeVariable; int * TypeDeBorne; int NbTermes; long * Coeff;
int NombreDeVariables; int Var; int NombreDeContraintes; char * SensContrainte;
double * B; int * Mdeb; int * NbTerm; double * A; int * Nuvar; int Cnt; int il; 
int ilMax; double a; double Granularite; char CalculDuPgcd; double b; char TousLesCoeffValentUn;

NombreDeVariables = Pne->NombreDeVariablesTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
X = Pne->UTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
L = Pne->LTrav;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

NbTermes = 0;
Coeff = (long *) malloc( NombreDeVariables * sizeof( long ) );
if ( Coeff == NULL ) {
  printf("Saturation memoire dans PNE_GranulariteDuCout\n");
  return;
}

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  if ( SensContrainte[Cnt] != '=' && 0 ) continue;

  Granularite = 0;
  FacteurMultiplicatif = 1;
  Pgcd = 0;
	CalculDuPgcd = OUI_PNE;
  TousLesCoeffValentUn = OUI_PNE;

	il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];
	while ( il < ilMax ) {
		if ( CalculDuPgcd == NON_PNE ) break;
    Var = Nuvar[il];
    if ( A[il] == 0 ) goto NextIl1;
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) goto NextIl1;
	  if ( Xmin[Var] == Xmax[Var] ) goto NextIl1;		
    if ( TypeDeVariable[Var] == ENTIER ) {		
      while ( 1 ) {
	      a = fabs( A[il] ) * FacteurMultiplicatif;
	      if ( a - floor( a ) < ZERO_COEFF_ENTIER || ceil ( a ) - a < ZERO_COEFF_ENTIER ) break;
        FacteurMultiplicatif *= 10;
		    if ( FacteurMultiplicatif > LIMITE_FACTEUR_MULTIPLICATIF ) {
		      printf("Limite atteinte pour le facteur multiplicatif %e valeur de L %e a = %e\n",FacteurMultiplicatif,L[Var],a);
		      printf(" a - floor( a ) = %e\n",a - floor( a ));
		      printf(" ceil ( a ) - a = %e\n",ceil ( a ) - a);			
		      CalculDuPgcd = NON_PNE; break;
		    }
				if ( fabs( a ) > 1.0000001 ) TousLesCoeffValentUn = NON_PNE;
	    }
		}
		else {
		  CalculDuPgcd = NON_PNE; break;
		}
		NextIl1:
    il++;
	}
	if ( TousLesCoeffValentUn == OUI_PNE ) continue;
	if ( CalculDuPgcd == NON_PNE ) continue;

	b = B[Cnt];
  NbTermes = 0;
	il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];
	while ( il < ilMax ) {
    Var = Nuvar[il];
    if ( A[il] == 0 ) goto NextIl2;
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) {
		  b -= A[il] * X[Var];
		  goto NextIl2;
		}
	  if ( Xmin[Var] == Xmax[Var] ) {
		  b -= A[il] * Xmin[Var];
		  goto NextIl2;
		}
    if ( TypeDeVariable[Var] == ENTIER ) {
	    Coeff[NbTermes] = (long) ( fabs( A[il] ) * FacteurMultiplicatif );
	    /*printf("Coeff %ld fabs( A[il] ) %e\n",Coeff[NbTermes],fabs( A[il] ));*/
	    NbTermes++;
		}
		NextIl2:
    il++;	
  }

	if ( b == 0 ) continue;
	
  PNE_Pgcd( NbTermes, Coeff, &Pgcd );

	if ( Pgcd > 1 ) {
    Granularite = (double) Pgcd / FacteurMultiplicatif;
    printf("Cnt %d sens %c Pgcd %ld NbTermes %d  Granularite = %e b = %e\n",Cnt,SensContrainte[Cnt],Pgcd,NbTermes,Granularite,b);
	}

}

free ( Coeff );

return;
}
