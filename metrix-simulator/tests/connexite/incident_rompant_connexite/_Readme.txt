# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test en LF de diff�rents incidents rompant la connexite
---------------------------------------
'incident_connexite' : Perte des 3 noeuds du bas du r�seau comportant les consos : tout le r�seau � 0
'incident_connexite_2': Perte du noeud FVERGE
'incident_connexite_mixte' : Perte du noeud FVERGE + ligne 'FS.BIS1 FSSV.O1 2' (hors poche)
'incident_connexite_hvdc' perte d'une HVDC et rupture de connexit� : non support�
'incident_connexite_groupe' perte d'un groupe et rupture de connexit� : non support�
'faux_incident_connexit�' connexit� assur�e uniquement par HVDC : invalid� lors de calcul des coefficients