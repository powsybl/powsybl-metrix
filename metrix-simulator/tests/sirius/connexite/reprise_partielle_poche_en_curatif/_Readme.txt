# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Reprise (partielle) d'une poche perdue par une parade
-----------------------------------------------------

L'incident "incident_poche" cr�e une poche si la ligne "FTDPRA1  FVERGE1  2" est consign�e.
La parade consiste � remettre la ligne consign�e en service, ce qui reconnecte la poche.

Pour les variantes 1 et 2, on utilise une l�g�re contrainte sur "FS.BIS1  FVALDI1  1" suite � l'incident pour activer la parade.
Dans les autres variantes, l'incident ne g�n�re aucune contrainte et on utilise donc le m�canisme de la contrainte fictive.


Variante 0 : la ligne n'est pas consign�e, pas de poche

Variante 1 : poche de production perdue r�cup�r�e par la parade 

Variante 2 : poche de production perdue r�cup�r�e par la parade, mais la limite de transit sur "FTDPRA1  FVERGE1  2" oblige � du d�lestage curatif 

Variante 3 : poche de production perdue r�cup�r�e par la parade 

Variante 4 : poche de production perdue r�cup�r�e par la parade, mais la limite de transit sur "FTDPRA1  FVERGE1  2" oblige � du d�lestage curatif 

Variante 5 : poche de consommation perdue r�cup�r�e par la parade 

Variante 6 : poche de consommation perdue r�cup�r�e par la parade, mais la limite de transit sur "FTDPRA1  FVERGE1  2" oblige � de l'effacement curatif 
