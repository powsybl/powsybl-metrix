/***********************************************************************

   FONCTION: 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

/*----------------------------------------------------------------------------*/

void PNE_RecupererLeProblemeInitial( PROBLEME_PNE * Pne ) 
{

Pne->Coupes.NombreDeContraintes = 0;
Pne->NombreDeCoupesCalculees = 0;

Pne->ResolutionDuNoeudReussie = OUI_PNE;

return;
}

