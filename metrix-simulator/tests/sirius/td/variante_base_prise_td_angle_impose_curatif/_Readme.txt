# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de DTVALDEP en variante de base sur un TD en pilotage angle imposé et curatif
----------------------------------------------------------------------------------
Réseau identique à "variante_base_prise_td_impose_curatif", mais le TD
"FP.AND1  FTDPRA1  1" est en pilotage angle imposé (DTMODREG = 2) au lieu de
puissance imposée (DTMODREG = 4). En pilotage angle, le quadripôle fictif du TD
reste connecté (cf. constructeur de TransformateurDephaseur) : la prise imposée
est un déphasage et le transit du TD résulte du réseau, d'où des résultats
différents du cas en puissance imposée pour la même prise.

Variante -1 : DTVALDEP = 17 (le fichier réseau est sur la prise 16, angle 0.0).
Variante 0  : durcissement du seuil N-k de "FS.BIS1 FSSV.O1 2" pour rendre
              l'incident 1 contraignant et forcer la création des variables
              curatives du TD.

Sans DTVALDEP le transit du TD vaut -40.0 MW ; avec la prise 17 il vaut
-23.2 MW : la consigne de la variante de base est bien appliquée.

Le cas couvre la branche PILOTAGE_ANGLE_IMPOSE de
TransformateurDephaseur::setPuiCons : en pilotage imposé, puiMin_ et puiMax_
doivent suivre la consigne, sinon ajouterContraintesBorneCuratif() rend le
problème infaisable ("Pas de solution trouvée", C1 = 1).
