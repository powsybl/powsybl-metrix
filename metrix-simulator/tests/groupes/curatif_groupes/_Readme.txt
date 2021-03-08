# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Test de curatif de groupe
-------------------------
Les groupes "FSSV.O12_G", "FVERGE11_G" et "FVALDI11_G" peuvent agir en curatif. 
Lors du d�faut "FS.BIS1 FSSV.O1 1", la liaison "FS.BIS1 FSSV.O1 2" passe en contrainte. 
Variante 0 : Le groupe FVALDI11_G est mont� et le groupe "FSSV.O12_G" est baiss� en curatif
Variante 1 : Les groupes "FVALDI11_G" et "FVERGE11_G" sont indisponibles, action en pr�ventif
Variante 2 : Le groupe "FSSV.O12_G" ne produit pas et ne peut pas �tre baiss�, action en pr�ventif
Variante 3 : Le groupe "FSSV.O12_G" ne produit pas mais peut �tre suffisamment baiss� en curatif
Variante 4 : Le groupe "FSSV.O12_G" ne produit pas et ne peut pas �tre suffisamment baiss� en curatif, combinaison de pr�ventif et curatif
Variante 5 : Le groupe "FSSV.O12_G" ne produit pas assez pour �tre suffisamment baiss� en curatif, combinaison de pr�ventif et curatif
Variante 6 : Le seuil ITAM sur la liaison "FS.BIS1 FSSV.O1 2"impose en baisse en pr�ventif en plus de l'action curative
Variante 7 : Le groupe "FVALDI11_G" est indisponible, action curative du groupe "FVERGE11_G"
Variante 8 : Le groupe "FVERGE11_G" est moins efficace mais moins cher, c'est lui qui est appel�. Malgre sa Pmin > 0 le groupe "FVALDI11_G" n'est pas d�marr�
Variante 9 : Le groupe "FVALDI11_G" est indisponible et l'action curative du groupe "FVERGE11_G" est limitee par la Pmin du groupe "FSSV.O12_G" -> d�lestage pr�ventif n�cessaire