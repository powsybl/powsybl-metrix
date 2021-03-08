Test de limitation de la plage d'excursion en préventif pour un TD
------------------------------------------------------------------
Le TD "FP.AND1  FTDPRA1  1" intialement sur la prise 16 
Il est configuré pour bouger de +3 prises et -2 prises en préventif  

Variante 0 : aucune contrainte
Variante 1 : contrainte sur "FS.BIS1 FSSV.O1 2", le TD bouge de -1 prise pour la résoudre
Variante 2 : contrainte (plus forte) sur "FS.BIS1 FSSV.O1 2", le TD bouge de -2 prise + redispatching
Variante 3 : TD initialement sur prise 17, contrainte sur "FS.BIS1 FSSV.O1 2", le TD bouge de -2 prises + redispatching
Variante 4 : TD initialement sur prise 18, contrainte sur "FS.BIS1 FSSV.O1 2", le TD bouge de -2 prises + redispatching plus important
Variante 5 : contrainte sur "FVALDI1  FTDPRA1  2", le TD bouge de +2 prises pour la résoudre
Variante 6 : contrainte (plus forte) sur "FVALDI1  FTDPRA1  2", le TD bouge de +3 prises + redispatching
Variante 7 : TD initialement sur prise 15, contrainte sur "FS.BIS1 FSSV.O1 2", le TD bouge de +3 prises + redispatching
Variante 8 : TD initialement sur prise 0 (prise min), il bouge de +3 prises
Variante 9 : TD initialement sur prise 32 (prise max), il bouge de -2 prises