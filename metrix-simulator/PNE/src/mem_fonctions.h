//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

# ifdef __CPLUSPLUS
  extern "C"
	{
# endif
# ifndef FONCTION_EXTERNES_MEMOIRE_DEJA_DEFINIES
/*****************************************************************/

void * MEM_Init( void );
void   MEM_Quit( void * );
char * MEM_Malloc( void * , size_t );
void   MEM_Free( void * ); 
char * MEM_Realloc( void * , void * , size_t );
long   MEM_QuantiteLibre( BLOCS_LIBRES * );

/*****************************************************************/
# define FONCTION_EXTERNES_MEMOIRE_DEJA_DEFINIES
# endif
# ifdef __CPLUSPLUS
  }
# endif 
