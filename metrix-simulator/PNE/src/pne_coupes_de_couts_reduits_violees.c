/***********************************************************************

   FONCTION: Detection des coupes de couts reduits violees.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"

# include "bb_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES 1

# if UTILISER_LES_COUPES_DE_COUTS_REDUITS == OUI_PNE
													
/*----------------------------------------------------------------------------*/

void PNE_DetectionDesCoupesDeCoutsReduitsViolees( PROBLEME_PNE * Pne )
{
int c; COUPES_DE_COUTS_REDUITS * CoupesDeCoutsReduits; int NbT; int * NbElements;
double * SecondMembre; int * First; int * IndiceDeLaVariable; double * Coefficient;
double * X; int * TypeDeBorne; double S; int il; int ilMax; double Sec; int Var;
double Seuil; double * Coeff; int * Indice;

if ( Pne->CoupesDeCoutsReduits == NULL ) return;

CoupesDeCoutsReduits = Pne->CoupesDeCoutsReduits;
NbElements = CoupesDeCoutsReduits->NbElements;
SecondMembre = CoupesDeCoutsReduits->SecondMembre;
First = CoupesDeCoutsReduits->First;
IndiceDeLaVariable = CoupesDeCoutsReduits->IndiceDeLaVariable;
Coefficient = CoupesDeCoutsReduits->Coefficient;

TypeDeBorne = Pne->TypeDeBorneTrav;
X = Pne->UTrav;

Coeff = Pne->Coefficient_CG;
Indice = Pne->IndiceDeLaVariable_CG;

/* On utilise le seuil des cliques */
PNE_MiseAJourSeuilCoupes( Pne, COUPE_CLIQUE, &Seuil );

for ( c = 0 ; c < CoupesDeCoutsReduits->NombreDeCoupes ; c++ ) {
  il = First[c];
	if ( il < 0 ) continue; /* On ne sait jamais ... */	
  ilMax = il + NbElements[c];
	S = 0;
	Sec = SecondMembre[c];
  while ( il < ilMax ) {
	  Var = IndiceDeLaVariable[il];
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) Sec -= Coefficient[il] * X[Var];		
		else S += Coefficient[il] * X[Var];		
	  il++;
	}
	
  if ( S < Sec + Seuil ) continue;

	# if TRACES == OUI_PNE
	  printf("****************************** Coupe de couts reduits %d violee de %e\n",c,S-Sec);
	# endif

  NbT = 0;
  il = First[c];
	if ( il < 0 ) continue; /* On ne sait jamais ... */	
  ilMax = il + NbElements[c];
	S = 0;
	Sec = SecondMembre[c];
  while ( il < ilMax ) {
	  Var = IndiceDeLaVariable[il];
	  if ( TypeDeBorne[Var] != VARIABLE_FIXE ) {
		  Coeff[NbT] = Coefficient[il];
			Indice[NbT] = IndiceDeLaVariable[il];
			NbT++;
		}
	  il++;
	}
		
  PNE_EnrichirLeProblemeCourantAvecUneCoupe( Pne, 'K', NbT, Sec, fabs( S - Sec ) + 1, Coeff , Indice );
  /* Pne->CoupesCalculees[Pne->NombreDeCoupesCalculees-1]->IndexDansCliques = c; */
      		
}

return;
}

/*----------------------------------------------------------------------------*/

# endif
