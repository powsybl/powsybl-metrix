# ifndef CONSTANTES_EXTERNES_PNE_DEJA_DEFINIES
/*******************************************************************************************/
/* Coix de l'algorithme */
# define SIMPLEXE         2     
# define POINT_INTERIEUR  1					    			      
  
/* Les codes de sorties retournes par le solveur: */
# define PAS_DE_SOLUTION_TROUVEE                                             0
# define SOLUTION_OPTIMALE_TROUVEE                                           1
# define SOLUTION_OPTIMALE_TROUVEE_MAIS_QUELQUES_CONTRAINTES_SONT_VIOLEES    2
# define PROBLEME_INFAISABLE                                                 3
# define PROBLEME_NON_BORNE                                                  4
# define ARRET_PAR_LIMITE_DE_TEMPS_AVEC_SOLUTION_ADMISSIBLE_DISPONIBLE       5
# define ARRET_CAR_ERREUR_INTERNE                                            6

/* Constantes symboliques du OUI et du NON */ 
# define OUI_PNE  1
# define NON_PNE  0

/* Type de bornes sur les variables fournies en entree */
# define VARIABLE_FIXE     		  1
# define VARIABLE_BORNEE_DES_DEUX_COTES   2  	
# define VARIABLE_BORNEE_INFERIEUREMENT   3  	
# define VARIABLE_BORNEE_SUPERIEUREMENT   4  	
# define VARIABLE_NON_BORNEE     	  5

# define REEL    1
# define ENTIER  2  /* Uniquement variables 1 2 pour l'instant */

# define LINFINI_PNE      1.e+75 

/*******************************************************************************************/
# define CONSTANTES_EXTERNES_PNE_DEJA_DEFINIES	
# endif

  
