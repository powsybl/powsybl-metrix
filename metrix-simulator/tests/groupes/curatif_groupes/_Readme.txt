Test de curatif de groupe
-------------------------
Les groupes "FSSV.O12_G", "FVERGE11_G" et "FVALDI11_G" peuvent agir en curatif. 
Lors du défaut "FS.BIS1 FSSV.O1 1", la liaison "FS.BIS1 FSSV.O1 2" passe en contrainte. 
Variante 0 : Le groupe FVALDI11_G est monté et le groupe "FSSV.O12_G" est baissé en curatif
Variante 1 : Les groupes "FVALDI11_G" et "FVERGE11_G" sont indisponibles, action en préventif
Variante 2 : Le groupe "FSSV.O12_G" ne produit pas et ne peut pas être baissé, action en préventif
Variante 3 : Le groupe "FSSV.O12_G" ne produit pas mais peut être suffisamment baissé en curatif
Variante 4 : Le groupe "FSSV.O12_G" ne produit pas et ne peut pas être suffisamment baissé en curatif, combinaison de préventif et curatif
Variante 5 : Le groupe "FSSV.O12_G" ne produit pas assez pour être suffisamment baissé en curatif, combinaison de préventif et curatif
Variante 6 : Le seuil ITAM sur la liaison "FS.BIS1 FSSV.O1 2"impose en baisse en préventif en plus de l'action curative
Variante 7 : Le groupe "FVALDI11_G" est indisponible, action curative du groupe "FVERGE11_G"
Variante 8 : Le groupe "FVERGE11_G" est moins efficace mais moins cher, c'est lui qui est appelé. Malgre sa Pmin > 0 le groupe "FVALDI11_G" n'est pas démarré
Variante 9 : Le groupe "FVALDI11_G" est indisponible et l'action curative du groupe "FVERGE11_G" est limitee par la Pmin du groupe "FSSV.O12_G" -> délestage préventif nécessaire