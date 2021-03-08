/***********************************************************************

   FONCTION: Pour réduire la précision des coefficients utilisés dans les calculs 
                
   AUTEUR: N. Lhuiller

************************************************************************/

# include "pne_sys.h"  
# include "pne_fonctions.h"

/* Pour réduire la précision des coefficients utilisés dans les calculs */
double PNE_Round(double x, double prec) {

       if (x > 0)
             x = floor(x * prec + 0.5) / prec;
       else if (x < 0)
             x = ceil(x * prec - 0.5) / prec;
						 
       return x;
}
