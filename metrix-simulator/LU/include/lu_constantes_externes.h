//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

# ifndef CONSTANTES_EXTERNES_LU_DEJA_DEFINIES  
/*******************************************************************************************/
/* 
  Definition des constantes symboliques a utiliser par le module appelant la 
  factorisation lu 
*/ 
			 									        
# define OUI_LU      1  	             				     
# define NON_LU      0  	 	         
# define SATURATION_MEMOIRE     2        	      
# define MATRICE_SINGULIERE     3
# define PRECISION_DE_RESOLUTION_NON_ATTEINTE  4  /* Quand on fait du raffinement iteratif, si on atteint
                                                     pas la precision demandee */
  
/* Les contextes d'utilisation de la factorisation */
# define LU_SIMPLEXE         1
# define LU_POINT_INTERIEUR  2
# define LU_GENERAL          3

/*******************************************************************************************/
# define CONSTANTES_EXTERNES_LU_DEJA_DEFINIES	
# endif 

 


