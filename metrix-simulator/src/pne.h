#ifndef PNE_METRIX
#define PNE_METRIX
/***************************************************************************************

Modele : OPF en Actif Seul con�u pour �tre int�gr� dans la logique statistique d'ASSESS
Auteur : Yacine HASSAINE
COPYRIGHT RTE 2008

*****************************************************************************************/

/**
 * @file LU/PNE/SPX header interface
 *
 */

extern "C" {
#include "lu_constantes_externes.h"
#include "lu_definition_arguments.h"
#include "lu_fonctions.h"
#include "pne_constantes_externes.h"
#include "pne_definition_arguments.h"
#include "pne_fonctions.h"
#include "spx_constantes_externes.h"
#include "spx_definition_arguments.h"
#include "spx_fonctions.h"

#undef malloc
#undef free
#undef realloc
}
#endif
