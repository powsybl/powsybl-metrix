Test PERTURBCOST : empilement economique HR degenere.

Les 4 groupes ont des couts de demarrage HR (CTORDR) strictement identiques :
sans perturbation, la repartition de l'empilement entre eux est un optimum
alternatif (resultat dependant du solveur et de son pivotage). PERTURBCOST=0.01
rend les couts strictement croissants avec l'indice de variable et impose une
repartition unique et deterministe, identique quel que soit le solveur.

La perturbation ne s'applique qu'a la phase hors reseau (voir io_doc.md).
