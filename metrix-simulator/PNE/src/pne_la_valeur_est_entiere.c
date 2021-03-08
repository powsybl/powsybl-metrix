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
