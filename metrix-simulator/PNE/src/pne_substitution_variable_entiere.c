/***********************************************************************

   FONCTION: Prise en compte de la substitution d'une variable entiere.
	           On substitue une variable entiere par une autre.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

/*----------------------------------------------------------------------------*/

int PNE_SubtituerUneVariableUneVariableEntiere( PROBLEME_PNE * Pne, int Mode, int VarSubstituee, int VarRestante, double ATilde, double BTilde,
                                                double PlusPetitCout, double PlusGrandCout,
                                                int Simulation, int Remplacement, int SubstitutionPossible, int SubstitutionImpossible )																								 
{
double * CoutLineaire; int * Cdeb; double X; 

CoutLineaire = Pne->LTrav;
Cdeb = Pne->CdebTrav;

if ( Mode == Simulation ) {
	X = CoutLineaire[VarRestante] - ( CoutLineaire[VarSubstituee] * ATilde );
	if ( X != 0.0 ) {
    /*printf("CoutLineaire[VarRestante] = %e PlusPetitCout %e PlusGrandCout %e \n",X,PlusPetitCout,PlusGrandCout);*/
	  if ( fabs( X ) > PlusGrandCout ) return( SubstitutionImpossible );
	  else if ( fabs( X ) < PlusPetitCout ) return( SubstitutionImpossible );
	}
}
else if ( Mode == Remplacement ) {
  CoutLineaire[VarRestante] -= CoutLineaire[VarSubstituee] * ATilde;
  Pne->Z0 += CoutLineaire[VarSubstituee] * BTilde;
  Cdeb[VarSubstituee] = -1; 
  CoutLineaire[VarSubstituee] = 0.0;

  Pne->TypeDeVariableTrav[VarSubstituee] = REEL;
  Pne->TypeDeBorneTrav[VarSubstituee] = VARIABLE_FIXE;
  Pne->UTrav[VarSubstituee] = 0;
	
}

return( SubstitutionPossible );
}

/*----------------------------------------------------------------------------*/


