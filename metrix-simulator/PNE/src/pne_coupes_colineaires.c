/***********************************************************************

   FONCTION: Pour une nouvelle coupe on regarde si elle n'est pas
	           colineaire a celles deja calculees.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"  

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define ZERO_COLINEAIRE  1.e-7 /*1.e-15*/
# define CONTROLE_COLINEARITE NON_PNE /*NON_PNE*/
  
/*----------------------------------------------------------------------------*/

char PNE_LaCoupeEstColineaire( PROBLEME_PNE * Pne, double * Coefficient, int * IndiceDeLaVariable,
                               double SecondMembre, int NombreDeTermes )

{
int i; int * T; double * V; double Rapport; int Var; int Var1; double * CoefficientTest;
int * IndiceTestDeLaVariable; double SecondMembreTest; char Colinearite; int j; double Epsilon;

# if CONTROLE_COLINEARITE == NON_PNE
  return( NON_PNE );
# endif

if ( Pne->ValeurDeCoeffCoupe == NULL ) {
  Pne->ValeurDeCoeffCoupe = (double *) malloc( Pne->NombreDeVariablesTrav * sizeof( double ) );
  Pne->IndiceDeCoeffCoupe  = (int *)    malloc( Pne->NombreDeVariablesTrav * sizeof( int   ) );
  if ( Pne->ValeurDeCoeffCoupe == NULL || Pne->IndiceDeCoeffCoupe == NULL ) {
    free( Pne->ValeurDeCoeffCoupe );
		free( Pne->IndiceDeCoeffCoupe );
		return( NON_PNE );
  }
  memset( (double *) Pne->ValeurDeCoeffCoupe, 0, Pne->NombreDeVariablesTrav * sizeof( double ) );
  memset( (char *) Pne->IndiceDeCoeffCoupe, 0, Pne->NombreDeVariablesTrav * sizeof( int ) );	
}

V = Pne->ValeurDeCoeffCoupe;
T = Pne->IndiceDeCoeffCoupe;
		
/* Controle de colinearite avec les coupes deja trouvees */
for ( i = 0 ; i < NombreDeTermes ; i++ ) { Var = IndiceDeLaVariable[i]; T[Var] = 1; V[Var] = Coefficient[i]; }
		
Colinearite = NON_PNE; /* Pour le cas ou il a 0 coupes deja calculees */
for ( i = 0 ; i < Pne->NombreDeCoupesCalculees ; i++ ) {
  if ( Pne->CoupesCalculees[i]->NombreDeTermes != NombreDeTermes ) continue;

  CoefficientTest = Pne->CoupesCalculees[i]->Coefficient;
	IndiceTestDeLaVariable = Pne->CoupesCalculees[i]->IndiceDeLaVariable;
	SecondMembreTest = Pne->CoupesCalculees[i]->SecondMembre,

	Rapport = -1;
	Colinearite = OUI_PNE;
			
	for ( j = 0 ; j < Pne->CoupesCalculees[i]->NombreDeTermes ; j++ ) {
		if (  CoefficientTest[j] == 0.0 ) continue;
		Var1 = IndiceTestDeLaVariable[j];
    if ( T[Var1] != 1 ) { Colinearite = NON_PNE; break; }				
    Rapport = V[Var1] / CoefficientTest[j];
		if ( Rapport <= 0.0 ) { Colinearite = NON_PNE; break; }
		j++;
		break;
	}
	if ( Colinearite == OUI_PNE ) {
		for ( ; j < Pne->CoupesCalculees[i]->NombreDeTermes ; j++ ) {
			if (  CoefficientTest[j] == 0.0 ) continue;
			Var1 = IndiceTestDeLaVariable[j];
      if ( T[Var1] != 1 ) { Colinearite = NON_PNE; break;};										
			Epsilon = fabs( V[Var1] - (Rapport * CoefficientTest[j]) );      
			if ( Epsilon > ZERO_COLINEAIRE ) { Colinearite = NON_PNE; break; };				
		}
		if ( Colinearite == OUI_PNE ) { 
			Epsilon = fabs( SecondMembre - (Rapport * SecondMembreTest) );      
			/*if ( Epsilon > ZERO_COLINEAIRE ) { Colinearite = NON_PNE; break; };*/ /* RG 18/11/2016: attention il y a un break en trop */		
			if ( Epsilon > ZERO_COLINEAIRE ) Colinearite = NON_PNE; 
		}				
	}
  if ( Colinearite == OUI_PNE ) break;	
}

/* On remet T et V a 0 */
for ( i = 0 ; i < NombreDeTermes ; i++ ) { Var = IndiceDeLaVariable[i]; T[Var] = 0; V[Var] = 0.0; }
		
/*
if ( Colinearite == OUI_PNE ) {
  printf("Coupe deja calculee \n");		
}
*/

return( Colinearite );
}
