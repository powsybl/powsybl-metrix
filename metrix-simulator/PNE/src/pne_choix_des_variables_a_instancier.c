/***********************************************************************

   FONCTION: Determination des variables a instancier.
             SP appele par la partie Branch and Bound.
                            
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"
							     
# include "pne_fonctions.h"
# include "pne_define.h"

/*----------------------------------------------------------------------------*/

void PNE_ChoixDesVariablesAInstancier( PROBLEME_PNE * Pne,
                                       int *  ValeurDInstanciationAGauche,
                                       int *  NombreDeVariablesAInstancierAGauche,
                                       int ** NumerosDesVariablesAInstancierAGauche,
				                               int *  ValeurDInstanciationADroite,
                                       int *  NombreDeVariablesAInstancierADroite,
                                       int ** NumerosDesVariablesAInstancierADroite
				     )
{

/* S'il y a des Gub on les prend en priorite */
if ( Pne->NbVarGauche <= 0 || Pne->NbVarDroite <= 0 ) {
   Pne->ValeurAGauche = 0;
   Pne->NbVarGauche   = 1;
   Pne->PaquetDeGauche[0] = Pne->VariableLaPlusFractionnaire;
   Pne->ValeurADroite = 1;
   Pne->NbVarDroite   = 1;
   Pne->PaquetDeDroite[0] = Pne->VariableLaPlusFractionnaire; 
}

*ValeurDInstanciationAGauche           = Pne->ValeurAGauche;
*NombreDeVariablesAInstancierAGauche   = Pne->NbVarGauche;
*NumerosDesVariablesAInstancierAGauche = Pne->PaquetDeGauche;

*ValeurDInstanciationADroite           = Pne->ValeurADroite;
*NombreDeVariablesAInstancierADroite   = Pne->NbVarDroite;
*NumerosDesVariablesAInstancierADroite = Pne->PaquetDeDroite;

return;
}
