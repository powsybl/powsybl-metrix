
/***********************************************************************

   FONCTION: Creation de la matrice du point interieur.
        
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pi_sys.h"

# include "pi_fonctions.h"
# include "pi_define.h"

# include "lu_constantes_externes.h"
# include "lu_definition_arguments.h"

# include "lu_define.h"
# include "lu_fonctions.h"

/*-------------------------------------------------------------------------*/
/*     Allocation de la matrice, calcul des termes, stockage des termes    */
 
void PI_Cremat( PROBLEME_PI * Pi )
{

PI_CrematSystemeAugmente( Pi );
		  
return;
}

/*-------------------------------------------------------------------------*/
/*  Calcul de termes de la matrice a chaque iteration puis factorisation   */

void PI_Crebis( PROBLEME_PI * Pi )
{

PI_CrebisSystemeAugmente( Pi );

return;
}




