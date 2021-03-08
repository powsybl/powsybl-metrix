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

   FONCTION: Gestion des temps de calcul
               
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"


/*----------------------------------------------------------------------------*/

void SPX_InitDateDebutDuCalcul( PROBLEME_SPX * Spx )
{
time_t HeureDeCalendrierDebut;

time( &HeureDeCalendrierDebut );
Spx->HeureDeCalendrierDebut = HeureDeCalendrierDebut;

return;
}

/*----------------------------------------------------------------------------*/

void SPX_ControleDuTempsEcoule( PROBLEME_SPX * Spx )
{
time_t HeureDeCalendrierCourant;
double TempsEcoule;

if ( Spx->DureeMaxDuCalcul < 0 ) return;

time( &HeureDeCalendrierCourant );

TempsEcoule = difftime( HeureDeCalendrierCourant , Spx->HeureDeCalendrierDebut );

if ( TempsEcoule <= 0.0 ) TempsEcoule = 0.0;

/* On provoque l'arret du calcul si temps depasse */
if ( TempsEcoule > Spx->DureeMaxDuCalcul ) Spx->Iteration = 10 * Spx->NombreMaxDIterations;

return;
}
