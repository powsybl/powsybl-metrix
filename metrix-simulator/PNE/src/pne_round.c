//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

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
