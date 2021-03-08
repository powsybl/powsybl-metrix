La fermeture de couplage définie dans la parade crée une boucle sur une même noeud, ce qui rend la matrice singulière et pose un problème dans LU.
La correction consiste à ne pas mettre le couplage dans la matrice et donc ignorer la parade.
