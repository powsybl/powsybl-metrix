/***********************************************************************

   FONCTION: Resolution du systeme a chaque iteration de point interieur 
       
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pi_sys.h"

# include "pi_fonctions.h"
# include "pi_define.h"

# include "lu_constantes_externes.h"
# include "lu_definition_arguments.h"

# include "lu_define.h"
# include "lu_fonctions.h"

/*--------------------------------------------------------------------------------*/

void PI_Resolution( PROBLEME_PI * Pi )
{

PI_ResolutionSystemeAugmente( Pi );
	
return;
}
