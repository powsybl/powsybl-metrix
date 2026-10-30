# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de DTVALDEP en variante de base sur un TD en pilotage imposé et curatif
-----------------------------------------------------------------------------
Réseau identique à "variante_base_prise_td", mais le TD "FP.AND1  FTDPRA1  1"
est en pilotage puissance imposé (DTMODREG = 4) et déclaré curatif sur
l'incident 1 (DTNBDEFK = 1, DTPTDEFK = 1).

Cette configuration est produite par la couche Java : le DSL accepte
"controlType FIXED_POWER_CONTROL" avec "onContingencies" sans avertissement
(PhaseTapChangerDslData.addPtc ne filtre pas sur le type, et
MetrixInputData.writeCurativePtc non plus). Elle figure telle quelle dans les
entrées de test du projet (metrix-integration, inputs/automaticList.groovy).

La variante de base déplace le TD d'une seule prise (17), la variante 0 durcit
le seuil N-k de "FS.BIS1 FSSV.O1 2" pour rendre l'incident 1 contraignant et
forcer la création des variables curatives du TD.

En pilotage imposé, puiMin_ et puiMax_ sont définis égaux à la consigne (cf.
constructeur de TransformateurDephaseur). S'ils ne suivent pas le déphasage
imposé par la variante de base, ajouterContraintesBorneCuratif() pousse un
second membre négatif sur des variables positives ou nulles et le problème
devient infaisable : METRIX renvoie "Pas de solution trouvée" (compte rendu
C1 = 1) sur un cas qui se résout normalement.
