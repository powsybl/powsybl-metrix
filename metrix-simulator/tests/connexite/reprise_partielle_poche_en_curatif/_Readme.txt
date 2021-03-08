Reprise (partielle) d'une poche perdue par une parade
-----------------------------------------------------

L'incident "incident_poche" crée une poche si la ligne "FTDPRA1  FVERGE1  2" est consignée.
La parade consiste à remettre la ligne consignée en service, ce qui reconnecte la poche.

Pour les variantes 1 et 2, on utilise une légère contrainte sur "FS.BIS1  FVALDI1  1" suite à l'incident pour activer la parade.
Dans les autres variantes, l'incident ne génère aucune contrainte et on utilise donc le mécanisme de la contrainte fictive.


Variante 0 : la ligne n'est pas consignée, pas de poche

Variante 1 : poche de production perdue récupérée par la parade 

Variante 2 : poche de production perdue récupérée par la parade, mais la limite de transit sur "FTDPRA1  FVERGE1  2" oblige à du délestage curatif 

Variante 3 : poche de production perdue récupérée par la parade 

Variante 4 : poche de production perdue récupérée par la parade, mais la limite de transit sur "FTDPRA1  FVERGE1  2" oblige à du délestage curatif 

Variante 5 : poche de consommation perdue récupérée par la parade 

Variante 6 : poche de consommation perdue récupérée par la parade, mais la limite de transit sur "FTDPRA1  FVERGE1  2" oblige à de l'effacement curatif 
