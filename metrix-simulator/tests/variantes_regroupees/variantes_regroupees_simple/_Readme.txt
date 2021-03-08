# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Tests simple de rassemblement des variantes par ensemble de QUADIN commun
--------------------------------------------------------------------------
Ce test vise � rassembler les variantes dont les QUADIN sont communs et les traiter dans un ordre bien pr�cis: en priorit� ceux qui n'impactent pas le r�seau (aucune consignation de quadin), puis par ordre alphab�tique (les premiers quadin du vecteur de quadin sont compar�s selon leur nom, puis les second quadin et ainsi de suite...).
Dans ce cas test pr�cis, on doit obtenir le rassemblement suivant:
Pour les variantes: 4; 6; il n'y a aucune consignation � r�aliser
pour les variantes: 0; 5; 7; les consignations des ouvrages suivants: FP.AND1  FVERGE1  1; FS.BIS1 FSSV.O1 1; FS.BIS1 FSSV.O1 2;
Pour les variantes 1; 3; les consignations des ouvrages suivants: FP.AND1  FVERGE1  1; FS.BIS1 FSSV.O1 2;
Pour les variantes 2; les consignations des ouvrages suivants: FS.BIS1 FSSV.O1 2;

Ce cas permet de tester plusieurs configurations de variantes:
variante de base avec quadin (variante 0)
pr�sence d'une variation sur le cas de base avec quadin (variante -1)
pr�sence de variantes avec quadin d�finis sur deux lignes distinctes (variante 5)
pr�sence de variante sans quadin (variante 4, 6)
pr�sence d'une variante o� le quadin n'est pas en premi�re ligne (variante 7)
