# ifndef DEFINITIONS_CONSTANTES_INTERNES_SPX_FAITE  
/*******************************************************************************************/
 
# define VERBOSE_SPX  0
# if VERBOSE_SPX  == 0
  # define VERBOSE_SPX_SCALING 0 /* mettre 0  si on ne veut tout de meme pas les traces */
# else
  # define VERBOSE_SPX_SCALING VERBOSE_SPX
# endif

# define SPX_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE  
# undef SPX_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE

# define RELAXATION_CONTRAINTES NON_SPX
			      	    		  	  			   	      	         
/* */					     	  	 				             
# ifndef CREUX               
  # define CREUX    1     
# endif      
# ifndef COMPACT         
  # define COMPACT  2		       			        
# endif       
					 	              
# define PHASE_1  1		        	        
# define PHASE_2  2

# define BORNEE                VARIABLE_BORNEE_DES_DEUX_COTES /* La variable est bornee des deux cotes */	 
# define BORNEE_INFERIEUREMENT VARIABLE_BORNEE_INFERIEUREMENT /* La variable n'est bornee qu'inferieurement */	 
# define BORNEE_SUPERIEUREMENT VARIABLE_BORNEE_SUPERIEUREMENT /* La variable n'est bornee que superieurement. 
                                                                 Rq: en entree un traitement supplémentaire la transforme en bornee inferieurement
			                                                               de telle sorte qu'en cours d'algorithme il n'y a plus que des variables 
					                                                           bornees inferieurement */	 
# define NON_BORNEE            VARIABLE_NON_BORNEE /* La variable n'est ni inferieurement ni superieurement bornee */

/* Origine des variables du simplexe */  
# define NATIVE		       1  /* Variable native (elle correspond a une variable du probleme d'entree) */
# define ECART		       2  /* Variable d'ecart creee pour mettre une contrainte d'inegalite sous la forme standard */
# define BASIQUE_ARTIFICIELLE  3  /* Variable basique de depart creee si necessaire pour construire la 
                                     premiere base inversible */	   
 
# define SORT_SUR_XMIN  1
# define SORT_SUR_XMAX  2  
# define SORT_PAS       3 				   			  
   
# define COEFFICIENT_A_BASE_INITIALE_PHASE_1  1.
                                                            
# define NOMBRE_MAX_DITERATIONS  100000

# define LINFINI_SPX   1.e+80

# define SEUIL_DADMISSIBILITE  1.e-6

/* Seuil qui s'applique aux variables natives */
# define SEUIL_DE_VIOLATION_DE_BORNE      1.e-7  /* 1.e-7 Seuil de violation de borne pour l'algorithme dual, c'est le seuil de 
                                                     convergence de l'algorithme dual */
# define SEUIL_MIN_DE_VIOLATION_DE_BORNE  ( 0.001  /*0.01*/ * SEUIL_DE_VIOLATION_DE_BORNE ) 
# define SEUIL_MAX_DE_VIOLATION_DE_BORNE  ( 1000   /*2.0*/  * SEUIL_DE_VIOLATION_DE_BORNE )

/* Seuil qui s'applique aux variables d'ecart et artificielles de base */
# define SEUIL_DE_VIOLATION_DE_BORNE_NON_NATIVE       1.e-7 /* 1.e-7 */
# define SEUIL_MIN_DE_VIOLATION_DE_BORNE_NON_NATIVE   ( 0.001  /*0.01*/ * SEUIL_DE_VIOLATION_DE_BORNE_NON_NATIVE )
# define SEUIL_MAX_DE_VIOLATION_DE_BORNE_NON_NATIVE   ( 1000   /*2.0*/  * SEUIL_DE_VIOLATION_DE_BORNE_NON_NATIVE )

/* seuil qui s'applique aux variables d'ecart de coupes */
# define SEUIL_DE_VIOLATION_DE_BORNE_VARIABLES_ECART_COUPES  1.e-7 /*1.e-7*/

# define LINFINI_POUR_LE_COUT 1.e+80        
# define LINFINI_POUR_X       1.e+80 
									     
# define SEUIL_DE_DEGENERESCENCE     1.e-7   /*1.e-7*/
# define VALEUR_DE_PIVOT_ACCEPTABLE  1.e-6   /*1.e-6*/ /* Diviseur acceptable pour un test de ratio */
/* Valeur limite complementaire utilise dans SPX_DualCalculerNBarreR: lorsque le test donne d'emblee un
   nombre nul de variables potentiellement entrantes, on refait un test avec ce seuil plus petit pour confirmation
   car sur certains jdd, ca peut interdire certains pivotage et conduire a dire qu'il n'y a pas de solution alors
	 que c'est faux */
/* Rq: il faudra reporter la modif dans le nouveau simplexe avec pricing dual partiel */
# define DERNIERE_VALEUR_DE_PIVOT_ACCEPTABLE  1.e-7    /* Diviseur acceptable limite pour un test de ratio */

# define COEFF_AUGMENTATION_VALEUR_DE_PIVOT_ACCEPTABLE  10.0 /*5.0*/
# define DIVISEUR_VALEUR_DE_PIVOT_ACCEPTABLE            1.1
 
# define CYCLE_DE_REFACTORISATION  50 /*30*/ /* Mettre 0 pour factoriser a chaque iteration mettre la meme valeur que
                                                CYCLE_DE_REFACTORISATION_DUAL */
																								
# define CYCLE_DE_REFACTORISATION_DUAL  50 /* Mettre 0 pour factoriser a chaque iteration */

# define SEUIL_DE_DEGENERESCENCE_DUAL         1.e-9 /* 1.e-9 */

# define NOMBRE_MAX_DE_CONTROLES_FINAUX 10  /* 20 */
# define SEUIL_POUR_RECONSTRUCTION_BASE  9  /* Doit toujours etre plus petit que NOMBRE_MAX_DE_CONTROLES_FINAUX */

# define SEUIL_ADMISSIBILITE_DUALE_1       1.e-8   /* 1.e-8 */
# define SEUIL_ADMISSIBILITE_DUALE_2       2.e-8   /* 2.e-8*/ /*1.75e-8*/ /* Doit toujours etre superieur à SEUIL_ADMISSIBILITE_DUALE_1 */
/*# define SEUIL_ADMISSIBILITE_DUALE_3       1.1e-8*/  /*1.1e-8*/ /*2.e-8*/ /*1.75e-8*/ /* Pour les variables non bornees */
# define COEFF_MIN_SEUIL_DUAL              0.001 /*0.001*/
# define COEFF_MAX_SEUIL_DUAL              1000  /*1000*/
 
# define SEUIL_POUR_MODIFICATION_DE_COUT        (10*SEUIL_ADMISSIBILITE_DUALE_1) /*(10*SEUIL_ADMISSIBILITE_DUALE_1)*/ /* Anti degenrescence */  
# define COEFF_SEUIL_POUR_MODIFICATION_DE_COUT  10 /* 10 S'applique au seuil d'admissibilite duale 1 */

# define NOMBRE_MAX_DE_PERTURBATIONS_DE_COUT   1 /*1*/    
# define VALEUR_PERTURBATION_COUT_A_POSTERIORI (100*SEUIL_POUR_MODIFICATION_DE_COUT) /*(100.*SEUIL_POUR_MODIFICATION_DE_COUT)*/
# define COEFF_VALEUR_PERTURBATION_COUT_A_POSTERIORI 100 

/*# define TOLERANCE_POUR_LE_TEST_DE_HARRIS       (0.5*SEUIL_ADMISSIBILITE_DUALE_1)*/  /*(0.5*SEUIL_ADMISSIBILITE_DUALE_1)*/
# define COEFF_TOLERANCE_POUR_LE_TEST_DE_HARRIS  0.1  /* 0.5 S'applique au seuil d'admissibilite 1 */

# define CYCLE_DE_VERIF_ADMISSIBILITE_DUALE   150 /* 150 */
								   
# define DUALE_FAISABLE                            0 
# define DUALE_INFAISABLE_PAR_COUT_REDUIT_NEGATIF  1 
# define DUALE_INFAISABLE_PAR_COUT_REDUIT_POSITIF  2     						        

# define MAX_BOUND_FLIP 1000 /*100*/

# define NOMBRE_DITERATIONS_DE_STRONG_BRANCHING  5

# define CYCLE_DE_CONTROLE_DE_DEGENERESCENCE_AGRESSIF      0 
# define CYCLE_DE_CONTROLE_DE_DEGENERESCENCE_PEU_AGRESSIF  5

# define INCREMENT_DALLOCATION_POUR_LE_NOMBRE_DE_VARIABLES_SPX 	    256   /* 256 variables */
# define INCREMENT_DALLOCATION_POUR_LE_NOMBRE_DE_CONTRAINTES_SPX    256   /* 256 contraintes */
# define INCREMENT_DALLOCATION_POUR_LA_MATRICE_DES_CONTRAINTES_SPX  4096  /* 4096 termes */

/* Pour les coupes de Gomory */
# define SEUIL_DE_VERIFICATION_DE_NBarreR_GOMORY  1.e-7
# define ZERO_GOMORY_1_F0  1.e-7 /*1.e-7*/   /* Pour eviter les divisions par 0 sur 1-F0 */
# define ZERO_TERMES_DU_TABLEAU_POUR_GOMORY 1.e-14 /*15*/

/* Pour sortir les variable avec Xmin=Xmax de la liste des variables hors base des
   qu'elles passent hors base */
# define ENLEVER_LES_VARIABLES_XMIN_EGAL_XMAX_DE_LA_LISTE_HORS_BASE  
  /*# undef ENLEVER_LES_VARIABLES_XMIN_EGAL_XMAX_DE_LA_LISTE_HORS_BASE*/

# define BORNE_NATIVE 1
# define BORNE_AUXILIAIRE_DE_VARIABLE_BORNEE_INFERIEUREMENT 2
# define BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE            3

# define BORNE_AUXILIAIRE_PRESOLVE  4
# define BORNE_AUXILIAIRE_FICTIVE   5
# define BORNE_AUXILIAIRE_INVALIDE  6

/* Pour les bornes auxiliaires */
# define UTILISER_BORNES_AUXILIAIRES
  /*# undef UTILISER_BORNES_AUXILIAIRES*/
# define ITERATION_POUR_BORNES_AUXILIAIRES 0 /*1*/ /*100*/ 

# define CYCLE_POUR_SUPPRESSION_DES_BORNES_AUXILIAIRES 10

/* Pour l'hyper creux */
/* 3 valeurs qui servents a decrire le stockage d'un vecteur */
# define COMPACT_SPX              1  /* Les valeurs non nulles sont compactees au debut du vecteur */
# define ADRESSAGE_INDIRECT_SPX   2  /* Adressage indirect des valeurs non nulles */
# define VECTEUR_SPX              3  /* Pas d'adressage particulier */

# define TAUX_DE_REMPLISSAGE_POUR_BASE_HYPER_CREUSE    0.03 /*0.03*/
# define TAUX_DE_REMPLISSAGE_POUR_VECTEUR_HYPER_CREUX  0.03 /*0.03*/     

# define BASE_HYPER_CREUSE 1
# define BASE_CREUSE       2
# define BASE_PLEINE       3

# define SEUIL_ECHEC_CREUX            10   /* C'est le nombre de fois ou le resultat doit etre plein
                                              pour passer en plein */
# define SEUIL_ECHEC_CREUX_STEEPEST   20   /* C'est SEUIL_ECHEC_CREUX pour le steepest edge */
# define SEUIL_REUSSITE_CREUX         3 /*3*/  /* C'est le nombre de fois ou le resultat doit etre creux
                                                  avant de repasser en hyper creux ou l'inverse */
# define CYCLE_TENTATIVE_HYPER_CREUX  4 /*4*/  /* Cycle pour la tentative de repassage en mode hyper creux */
# define SEUIL_ABANDON_HYPER_CREUX    4 /*4*/  /* C'est le nombre max de tentative infructueuses a partir
                                                  duquel on essai plus de revenir en hyper creux */

# define SPX_ACTIVATION_SUPPRESSION_PETITS_TERMES NON_PNE /*OUI_SPX*/

# define UTILISER_PNE_RAND OUI_SPX

/*******************************************************************************************/
# define DEFINITIONS_CONSTANTES_INTERNES_SPX_FAITE  
# endif   

 
  


