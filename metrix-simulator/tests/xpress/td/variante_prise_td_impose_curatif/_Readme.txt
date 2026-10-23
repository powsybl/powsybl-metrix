# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de DTVALDEP en variante numérotée sur un TD en pilotage imposé et curatif
------------------------------------------------------------------------------
Réseau identique à "variante_base_prise_td_impose_curatif" : le TD
"FP.AND1  FTDPRA1  1" est en pilotage puissance imposé (DTMODREG = 4) et
déclaré curatif sur l'incident 1. Le DTVALDEP est ici porté par la variante 0
et non par la variante de base.

Variante 0 : DTVALDEP = 17 + durcissement du seuil N-k de "FS.BIS1 FSSV.O1 2"
             pour rendre l'incident 1 contraignant et forcer la création des
             variables curatives du TD. Doit donner exactement le même out_s0
             que "variante_base_prise_td_impose_curatif".
Variante 1 : même durcissement, sans DTVALDEP -> le TD doit être revenu sur la
             prise 16 du fichier réseau (transit 0.0 MW sur son quadripôle).

Le chemin variante (Reseau::modifReseau) n'écrivait que puiCons_ ; en pilotage
imposé puiMin_ et puiMax_ restaient sur la consigne précédente et
ajouterContraintesBorneCuratif() rendait le problème infaisable ("Pas de
solution trouvée", C1 = 1) :
- sans correction de modifReseau, la variante 0 échoue ;
- sans correction de resetReseau, la variante 1 échoue (bornes restées sur la
  prise 17 alors que puiCons_ est revenu sur la prise 16).
Les deux chemins passent par TransformateurDephaseur::setPuiCons, comme la
variante de base (Reseau::updateBase).
