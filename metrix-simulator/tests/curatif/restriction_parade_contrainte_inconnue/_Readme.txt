# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

Restriction d'une parade avec une contrainte inconnue
-----------------------------------------------------
Suite à l'incident 'FS.BIS1 FSSV.O1 1', la ligne 'FS.BIS1 FSSV.O1 2' entre en contrainte.

La parade 'FS.BIS1  FVALDI1  1' est restreinte à 'FVALDI1  FTDPRA1  1' et 'FOO'. 'FOO' n'est pas un élément
surveillé : la restriction porte sur 'FVALDI1  FTDPRA1  1' seul, qui n'est pas en contrainte, et la parade ne
peut pas être activée.

Dans la parade 'FS.BIS1 FSSV.O1 2', le nom de l'incident est suivi d'espaces avant le "pipe".

Variante 0 : la parade 'FS.BIS1 FSSV.O1 2' est choisie.

Avant correction, 'FVALDI1  FTDPRA1  1|FOO' était lu comme une seule contrainte inconnue : la restriction était
ignorée et la parade 'FS.BIS1  FVALDI1  1' était choisie. Le nom d'incident suivi d'espaces n'était pas reconnu
et la parade 'FS.BIS1 FSSV.O1 2' était ignorée.
