/***********************************************************************

   FONCTION: Normalisation d'une coupe
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
# include "spx_fonctions.h"     

# include "bb_define.h"
# include "bb_fonctions.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

/*----------------------------------------------------------------------------*/

void PNE_NormaliserUnCoupe( double * Coefficient, double * SecondMembre, int NombreDeTermes, double PlusGrandCoeff )
{
int i; double Normalisation; double Sec; 

# if NORMALISER_LES_COUPES_SUR_LES_G_ET_I != OUI_PNE    
  return;
# endif

if ( PlusGrandCoeff < SEUIL_POUR_NORMALISER_LES_COUPES_SUR_LES_G_ET_I ) return;

Normalisation = 1.0 / PlusGrandCoeff;  
SPX_ArrondiEnPuissanceDe2( &Normalisation );  

for ( i = 0 ; i < NombreDeTermes ; i++ ) Coefficient[i] *= Normalisation;          
Sec = *SecondMembre;
Sec*= Normalisation;
*SecondMembre = Sec;
   
return;
}

