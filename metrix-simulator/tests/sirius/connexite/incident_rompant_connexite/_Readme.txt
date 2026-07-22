# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test en LF de différents incidents rompant la connexite
---------------------------------------
'incident_connexite' : Perte des 3 noeuds du bas du réseau comportant les consos : tout le réseau à 0
'incident_connexite_2': Perte du noeud FVERGE
'incident_connexite_mixte' : Perte du noeud FVERGE + ligne 'FS.BIS1 FSSV.O1 2' (hors poche)
'incident_connexite_hvdc' perte d'une HVDC et rupture de connexité : non supporté
'incident_connexite_groupe' perte d'un groupe et rupture de connexité : non supporté
'faux_incident_connexité' connexité assurée uniquement par HVDC : invalidé lors de calcul des coefficients