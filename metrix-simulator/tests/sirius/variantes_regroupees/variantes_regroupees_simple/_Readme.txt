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
Ce test vise à rassembler les variantes dont les QUADIN sont communs et les traiter dans un ordre bien précis: en priorité ceux qui n'impactent pas le réseau (aucune consignation de quadin), puis par ordre alphabétique (les premiers quadin du vecteur de quadin sont comparés selon leur nom, puis les second quadin et ainsi de suite...).
Dans ce cas test précis, on doit obtenir le rassemblement suivant:
Pour les variantes: 4; 6; il n'y a aucune consignation à réaliser
pour les variantes: 0; 5; 7; les consignations des ouvrages suivants: FP.AND1  FVERGE1  1; FS.BIS1 FSSV.O1 1; FS.BIS1 FSSV.O1 2;
Pour les variantes 1; 3; les consignations des ouvrages suivants: FP.AND1  FVERGE1  1; FS.BIS1 FSSV.O1 2;
Pour les variantes 2; les consignations des ouvrages suivants: FS.BIS1 FSSV.O1 2;

Ce cas permet de tester plusieurs configurations de variantes:
variante de base avec quadin (variante 0)
présence d'une variation sur le cas de base avec quadin (variante -1)
présence de variantes avec quadin définis sur deux lignes distinctes (variante 5)
présence de variante sans quadin (variante 4, 6)
présence d'une variante où le quadin n'est pas en première ligne (variante 7)
