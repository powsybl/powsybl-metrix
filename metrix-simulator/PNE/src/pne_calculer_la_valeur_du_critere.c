/***********************************************************************

   FONCTION: Calcul du critere.
                            
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"
							     
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
# include "spx_fonctions.h"

/*----------------------------------------------------------------------------*/

void PNE_CalculerLaValeurDuCritere( PROBLEME_PNE * Pne )
{
int i ; 

/* Calcul du critere */
Pne->Critere = Pne->Z0;

for ( i = 0 ; i < Pne->NombreDeVariablesTrav ; i++ ) Pne->Critere+= Pne->LTrav[i] * Pne->UTrav[i];
 
#if VERBOSE_PNE
  printf(" Valeur du critere apres optimisation du probleme relaxe: %lf \n",Pne->Critere);
  printf(" ********************************************************     \n");
#endif

if ( Pne->NombreDeCoupesCalculees == 0 ) Pne->ValeurOptimaleDuProblemeCourantAvantNouvellesCoupes = Pne->Critere; 

return;
}

