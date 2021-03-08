Tests d'effacements curatifs
----------------------------
2 charges sur le noeud FVALDI sont configurées pour fonctionner en curatif sur l'incident FS.BIS1 FSSV.O1 1.
La charge FVALDI11_L peut effacer 10% de sa consommation 
La charge FVALDI11_L2 peut effacer 20% de sa consommation 
Lors de la perte de FS.BIS1 FSSV.O1 1, la liaison FS.BIS1 FSSV.O1 2 passe en contrainte.

Lorsque la limite de FS.BIS1 FSSV.O1 2 est de 440, il faut délester 120 MW au total pour respecter la contrainte
Lorsque la limite de FS.BIS1 FSSV.O1 2 est de 400, il faut délester 200 MW au total pour respecter la contrainte


Variante 0 : Délestage curatif de 120 MW dont 10 MW sur FVALDI11_L (=10% de 100) et 110 MW (<20% de 900) sur FVALDI11_L2 

Variante 1 : Délestage curatif de 120 MW sur FVALDI11_L2 (<20% de 900) car le cout d'effacement est plus faible que sur FVALDI11_L

Variante 2 : Délestage préventif de 11,1 MW et curatif de 8,9 MW sur FVALDI11_L (=10% de 100-11,1) et curatif de 180 MW sur FVALDI11_L2 (=20% d 900)

Variante 3 : Délestage préventif de 110 MW de FVALDI11_L et curatif de 10 MW uniquement sur FVALDI11_L (=10% de 100) car le curatif sur FVALDI11_L2 est plus cher que le préventif

Variante 4 : Délestage préventif de 120 MW car c'est le seuil ITAM qui est contraignant

Variante 5 : Délestage préventif de 120 MW pour respecter le seuil ITAM, puis curatif de 80 MW pour respecter le seuil N-1

Variante 6 : Délestage préventif de 120 MW car pas de conso curative sur le poste FSSV.O1_2

Variante 7 : Délestage préventif de 120 MW car pas de groupe curatif disponible pour compenser l'effacement