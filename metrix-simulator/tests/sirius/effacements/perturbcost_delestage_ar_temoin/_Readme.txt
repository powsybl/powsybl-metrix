#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

Test canari : PRTBCOST ne doit avoir AUCUN effet sur la phase AR.

Reseau minimal : un groupe (FGEN.A11_G, seul degre de liberte HR -> empilement
unique) derriere un quadripole limite a 150 MW en N (QATI00MN), alimentant deux
consommations strictement identiques de 100 MW (FCHA.A1_1 et FCHA.B1_1) situees
derriere ce meme quadripole (sensibilites identiques). La phase AR doit baisser
le groupe de 50 MW et delester 50 MW a repartir entre deux consos aux couts de
defaillance identiques : optimum degenere.

Jumeaux :
- perturbcost_delestage_ar_temoin : PRTBCOST = 0.0
- perturbcost_delestage_ar        : PRTBCOST = 0.01

Invariant garde par le meta-test 'perturbcost_ar_neutralite' : les references
out_s0 des deux jumeaux doivent rester identiques octet pour octet, puisque la
perturbation est retiree des couts consos au passage en phase AR (cf.
resolutionUnProblemeDodu) et que les couts AR des groupes sont reecrits par
fixerProdSansReseau(). Si la restauration regresse, le jumeau 0.01 s'ecarte de
sa reference : la composante COUT DELESTAGE de R9, calculee depuis
pbCoutLineaire_ sur la solution AR (calculmacrofonctions.cpp), embarque alors
le facteur de perturbation (de l'ordre de +0.5 % ici), independamment de la
repartition du delestage -- que les solveurs concentrent deja naturellement
sur la conso de plus petit indice, comme le ferait la perturbation.

Protocole de (re)generation des references :
1. compiler, lancer les tests (ils echouent tant que la reference n'existe pas) ;
2. copier metrixOut.txt et out_s0 du repertoire du test vers sa reference ;
3. validation par mutation : neutraliser localement la restauration dans
   resolutionUnProblemeDodu (remplacer la condition du bloc 'Fin de portee de
   PRTBCOST' par 'if (false)'), recompiler, relancer perturbcost_delestage_ar
   et VERIFIER qu'il devient rouge (COUT DELESTAGE de R9 gonfle d'environ
   +0.5 %) pendant que le temoin reste vert, puis retablir et recompiler.
