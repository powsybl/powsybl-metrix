Test d'ouverture du DJ d'un groupe et d'une conso
---------------------------------------------
Lors de l'incident "FS.BIS1 FSSV.O1 1" la ligne "FS.BIS1 FSSV.O1 2" passe en contrainte.

Une parade consiste � ouvrir le DJ du groupe "FS.BIS11_G", l'autre � ouvrir la consommation � VALDI. on a donc des couts d'end et d'ene sur les variantes 1 et 2. 
Ici, on teste l'ajout d'une probabilite sur l'incident "FS.BIS1 FSSV.O1 1" �gale 0.002 sur la variante -1, 0.008 sur la variante 1, et de 0.007 sur la variante 2. Cela impacte la probabilite des parades d'ouverture du groupe et d'ouverture de la conso, et donc les co�ts finaux d'END et ENE de chaque variante. Pour la variante 0, les grandeurs ne varient pas bien que la probabilite de l'incident ait chang�, car il n'y a pas de contrainte.

Variante 0 : r�f�rence RAS.
Variante 1 : parade ouverture du groupe.
Variante 2 : parade ouverture de la conso.
