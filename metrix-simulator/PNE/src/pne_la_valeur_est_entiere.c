
/***********************************************************************

   FONCTION: Determine si la valeur est entiere ou pas a Epsilon pres.
                 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# define EPSILON 1.e-12 /*1.e-9*/

# define MODIFIER_LA_VALEUR NON_PNE

/*----------------------------------------------------------------------------*/

int PNE_LaValeurEstEntiere( double * Valeur ) 
{

/*return( NON_PNE );*/

if ( fabs( *Valeur - ceil( *Valeur ) ) < EPSILON ) {
  # if MODIFIER_LA_VALEUR == OUI_PNE
	  *Valeur = ceil( *Valeur );
	# endif
  return( OUI_PNE );
}
else if ( fabs( *Valeur - floor( *Valeur ) ) < EPSILON ) {
  # if MODIFIER_LA_VALEUR == OUI_PNE
    *Valeur = floor( *Valeur );
	# endif
  return( OUI_PNE );
}
return( NON_PNE );
}
