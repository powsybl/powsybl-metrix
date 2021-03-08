#ifndef PROTOTYPE_METRIX
#define PROTOTYPE_METRIX
/***************************************************************************************

Modele      : OPF en Actif Seul con�u pour �tre int�gr� dans la logique statistique d'ASSESS
Auteur      : Yacine HASSAINE
Description : Fichier d'en-tete pour la declaration des prototypes des fonctions
         utilisees par le programme principal
COPYRIGHT RTE 2008

*****************************************************************************************/
#include "reseau.h"
#include "variante.h"

void afficherVariantesCle(
    MapQuadinVar, Quadripole::SetQuadripoleSortedByName IndispoLignes = Quadripole::SetQuadripoleSortedByName());
void afficherMapVariante(MapQuadinVar);

#endif
