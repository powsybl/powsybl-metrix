# ifdef __cplusplus
  extern "C"
	{
# endif
# ifndef CONSTANTES_INTERNES_PNE_DEJA_DEFINIES	
/*******************************************************************************************/

# define VERBOSE_PNE  0
# define DEBUG_PNE    0

# define PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE 
# undef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE

# define MODE_PNE 1
# define MODE_PRESOLVE 2
	
# define MPCC_DANS_PI NON_PNE

# define VALEUR_NON_INITIALISEE 1.e+75
                                	  				      				   	              	     		              
# define TOLERANCE_SUR_LES_ENTIERS  1.e-6

# define ZERO_PRESOLVE    1.e-12 /* Valeur du ZERO pour le Presolve */
# define ZERO_VARFIXE     1.e-8
# define ZERO_COUT_REDUIT 1.e-8

/*
# define VALEUR_DE_FRACTIONNALITE_NULLE  1.e-7
# define VALEUR_DE_FRACTIONNALITE_NULLE_AJUSTEE_MIN  5.e-8
# define VALEUR_DE_FRACTIONNALITE_NULLE_AJUSTEE_MAX  5.e-7
*/
				                    
# define VALEUR_DE_FRACTIONNALITE_NULLE  1.e-6   
# define VALEUR_DE_FRACTIONNALITE_NULLE_AJUSTEE_MIN  5.e-8
# define VALEUR_DE_FRACTIONNALITE_NULLE_AJUSTEE_MAX  5.e-6

# define SEUIL_DADMISSIBILITE   1.e-6  /* Tolerance sur la satifaction des contraintes */

# define INCREMENT_DALLOCATION_POUR_LE_NOMBRE_DE_VARIABLES_PNE      256   /* 256 variables */
# define INCREMENT_DALLOCATION_POUR_LE_NOMBRE_DE_CONTRAINTES_PNE    256   /* 256 contraintes */
# define INCREMENT_DALLOCATION_POUR_LA_MATRICE_DES_CONTRAINTES_PNE  4096  /* 4096 termes */

# define STRONG_BRANCHING_NON_DEFINI       0
# define STRONG_BRANCHING_MXITER_OU_REFACT 1
# define STRONG_BRANCHING_REFACTORISATION  2
# define STRONG_BRANCHING_OPTIMALITE       3
# define STRONG_BRANCHING_COUT_MAX_DEPASSE 4
# define STRONG_BRANCHING_PAS_DE_SOLUTION  5

# define PENTE_DE_LA_VARIATION           1
# define VALEUR_ABSOLUE_DE_LA_VARIATION  2

# define PLAGE_REDUCED_COST_FIXING 1.e+15
# define REDUCED_COST_FIXING_QUE_SUR_VARIABLES_ENTIERES  OUI_PNE /*OUI_PNE*/

# define SIMULATION 1
# define CALCUL     2

# define IMPOSSIBLE    0 
# define INF_POSSIBLE  1
# define SUP_POSSIBLE  2
# define INF_ET_SUP_POSSIBLE 3

# define SEUIL_EVITEMENT_MIR_MARCHAND_WOLSEY  15   
# define NB_ECHECS_INHIB_MIR                   3  

# define NB_TERMES_FORCE_CALCUL_DE_K 200
# define SEUIL_VARIABLE_DUALE_POUR_CALCUL_DE_COUPE 1.e-6

/* Constantes pour les coupes de Gomory: on se base sur une etude de Cornuejols Margot et Nannicini:
   On the Safety of Gomory cut generators */
# define NORMALISER_LES_COUPES_SUR_LES_G_ET_I             NON_PNE /*NON_PNE*/
# define SEUIL_POUR_NORMALISER_LES_COUPES_SUR_LES_G_ET_I  1.e-6 /*1.e-6*/
/* Valeurs utilisees dans le simplexe */
# define RAPPORT_MAX_COEFF_COUPE_GOMORY        1.e+6 /*1.e+6*/  
# define COEFFICIENT_DELARGISSEMENT_DU_RAPPORT_MAX_GOMORY_AU_NOEUD_RACINE 100.
# define RAPPORT_MAX_COEFF_COUPE_INTERSECTION  1.e+6 /*1.e+6*/ 
# define COEFFICIENT_DELARGISSEMENT_DU_RAPPORT_MAX_COUPE_INTERSECTION_AU_NOEUD_RACINE 1.

# define ZERO_POUR_COEFF_VARIABLE_DECART_DANS_COUPE_GOMORY_OU_INTERSECTION   1.e-13 /*13*/  
# define ZERO_POUR_COEFF_VARIABLE_NATIVE_DANS_COUPE_GOMORY_OU_INTERSECTION   1.e-11 /*11*/  
# define RELAX_RHS_GOMORY_ABS 0.e-9  /*0.e-9*/ 
# define RELAX_RHS_GOMORY_REL 0.e-10 /*0.e-10*/ 
# define RELAX_RHS_INTERSECTION_ABS 1.e-7 /*1.e-7*/   
# define RELAX_RHS_INTERSECTION_REL 1.e-8 /*1.e-8*/   
 
# define SEUIL_FRACTIONNALITE_POUR_FAIRE_UNE_COUPE_DE_GOMORY  1.e-4 /*1.e-4*/
# define SEUIL_FRACTIONNALITE_POUR_COUPE_INTERSECTION         1.e-4 /*1.e-4*/

# define CALCULER_COUPES_DE_GOMORY       OUI_PNE /* OUI_PNE */
# define CALCULER_COUPES_DINTERSECTION   OUI_PNE /*OUI_PNE*/ /* On ne peut pas calculer de coupes d'intersection sans les gomory */
# define CALCULER_COUPES_KNAPSACK_SIMPLE OUI_PNE /*OUI_PNE*/
# define CALCULER_MIR_MARCHAND_WOLSEY    OUI_PNE /*OUI_PNE*/   

# define UTILISER_LE_GRAPHE_DE_CONFLITS  OUI_PNE /*OUI_PNE*/ 
# define FAIRE_DU_NODE_PRESOLVE          OUI_PNE /*OUI_PNE*/ 
# define UTILISER_LES_ODD_HOLES          NON_PNE /*NON_PNE*/  
# define REDUCED_COST_FIXING_AU_NOEUD_RACINE OUI_PNE /*OUI_PNE*/
# define REDUCED_COST_FIXING_AUX_NOEUD_DANS_ARBRE OUI_PNE /*OUI_PNE*/
# define RELANCE_PERIODIQUE_DU_SIMPLEXE_AU_NOEUD_RACINE NON_PNE /* Necessite une mise au point complementaire */
# define UTILISER_LES_COUPES_DE_PROBING  OUI_PNE /*OUI_PNE*/
# define CONSTRUIRE_BORNES_VARIABLES     OUI_PNE /*OUI_PNE*/ /* Contraintes de bornes variable qui decoulent du variable probing */

# define UTILISER_LES_COUPES_DE_COUTS_REDUITS NON_PNE /*OUI_PNE*/ 

# define RELATION_DORDRE_DANS_LE_PROBING NON_PNE /*NON_PNE*/ 
# define CONTRAINTES_DORDRE_DANS_LE_GRAPHE 1 
# define CONTRAINTES_DORDRE_DANS_LES_COUPES 2 
# define METHODE_DE_PRISE_EN_COMPTE_DES_CONTRAINTES_DORDRE CONTRAINTES_DORDRE_DANS_LES_COUPES  

/****** Cela pose parfois des problemes de precision numerique *****/ # define TENIR_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTES_POUR_LE_BRANCHING NON_PNE /*OUI_PNE*/
/****   si on met OUI_PNE on trouve une solution fausse sur LaureCClentEtObjIncorrect.mps
        et je n'ai pas compris pourquoi ************/

# define EXPLOITER_GRAPHE_POUR_SUBSTITUTIONS_DE_VARIABLES OUI_PNE /* OUI_PNE */

/********************** on garde cette modif ******************************/ # define TENIR_COMPTE_DE_LA_PRESENCE_DE_BORNES_VARIABLES_NATIVE_DANS_CALCUL_DES_BORNES_VARIABLE OUI_PNE /*NON_PNE*/

# define FAIRE_DU_POST_PROBING OUI_PNE /* OUI_PNE */

# define UTILISER_UNE_CONTRAINTE_DE_COUT_MAX NON_PNE  /* Pour eviter de recalculer le cout de la solution dans le simplexe et la comparer
                                                         au cout de la meilleure solution pour eventuellement arreter les iteration */

# define SEUIL_VIOLATION_COUPE_DE_GOMORY     1.e-6 /*1.e-5*/ 
# define SEUIL_VIOLATION_COUPE_DINTERSECTION 1.e-4 /*1.e-5*/ /* 29/11/2016 on remet le seuil initial, les essais montrent que cela marche mieux avec le nouveau calcul de coupes de sac a dos */
# define SEUIL_VIOLATION_KNAPSACK            1.e-2 /*1.e-3*/ /* 29/11/2016 on augmente le seuil car il y a plus de coupes et des combinaisons de contraintes */
# define SEUIL_VIOLATION_MIR_MARCHAND_WOLSEY 1.e-2 /*1.e-2*/ 
# define SEUIL_VIOLATION_CLIQUES             1.e-3 /*1.e-3*/ /*SEUIL_DADMISSIBILITE*/  
# define SEUIL_VIOLATION_IMPLICATIONS        1.e-3 /*1.e-3*/ /*SEUIL_DADMISSIBILITE*/ 
# define SEUIL_VIOLATION_COUPES_DE_PROBING   1.e-1 /* On met un seuil eleve car il y a generalement beaucoup de coupes de probing */
# define SEUIL_VIOLATION_BORNES_VARIABLES    1.e-2 /*1.e-2*/

# define BORNES_INF_AUXILIAIRES OUI_PNE /* OUI_PNE */

# define NOMBRE_MAX_ECHECS_MIR_MARCHAND_WOLSEY_SUR_UNE_CONTRAINTE 10

# define MARGE_SUR_DELTA_POUR_LE_REDUCED_COST_FIXING 1.e-8

# define COUPE_KNAPSACK 1
# define COUPE_MIR_MARCHAND_WOLSEY 2
# define COUPE_CLIQUE 3
# define COUPE_IMPLICATION 4
# define COUPE_DE_BORNE_VARIABLE 5
# define COUPE_CONTRAINTE_DORDRE 6

# define UTILISER_LE_GRAPHE_DE_CONFLITS_DANS_REDUCED_COST_FIXING_AU_NOEUD_RACINE NON_PNE /*NON_PNE*/
# define UTILISER_LE_GRAPHE_DE_CONFLITS_DANS_REDUCED_COST_FIXING NON_PNE                 /*NON_PNE*/

# define UTILISER_LES_GUB NON_PNE  
# define MIN_TERMES_GUB   4
# define MAX_TERMES_GUB   10 /* 10 */
# define PROFONDEUR_LIMITE_POUR_UTILISATION_DES_GUB 100000 /*10*/

# define VIOLATION_MIN_POUR_K_SUR_COUPE  1.e-2 /* 1.e-2 */
# define KNAPSACK_SUR_GOMORY NON_PNE /*OUI_PNE*/ /* Experimentalement le K sur Gomory sont interesantes mais dans certains cas */
                                                 /* cela pose des problemes numeriques: en particulier c'est incompatible avec
																								    l'option UTILISER_UNE_CONTRAINTE_DE_COUT_MAX car cette contrainte peut etre tres
																										pleine si beaucoup de variables ont un cout non nul */
                                                 /* RG 11/2016: on met NON_PNE car parfois pb qui semblent dus aux imprecisions (meme petites) sur les
																								    coefficents de la gomory ont une imprecision */																										
# define KNAPSACK_SUR_COUPE_DINTERSECTION NON_PNE /*NON_PNE*/ 

# define KNAPSACK_SUR_COMBINAISONS_DE_CONTRAINTES NON_PNE /*NON_PNE*/ 

# define CALCULS_SUR_MIXED_0_1_KNAPSACK NON_PNE

# define KNAPSACK_SUR_CONTRAINTES_DEGALITE NON_PNE /*NON_PNE*/

# define MIN_TERMES_POUR_KNAPSACK 2 /*3*/
# define MAX_TERMES_POUR_KNAPSACK 10000  /* Il faut limiter car si la contrainte a trop de termes
                                            la recherche des K est trop longue */
# define MAX_TERMES_POUR_KNAPSACK_COMBINEES 10000 /* Il faut limiter car si la contrainte a trop de termes
                                                     la recherche des K est trop longue */																					
# define SEUIL_POUR_PROGRAMMATION_DYNAMIQUE 256  /* Pour le calcul de Knapsack */
/* Malgre beaucoup de test on n'a pas vu de cas ou la programmation dynamique faisait mieux */
# define UTILISER_AUSSI_LA_PROGRAMMATION_DYNAMIQUE NON_PNE /* Pour le calcul de Knapsack */ 
# define COEFFS_ENTIERS_DANS_KNAPSACK NON_PNE
# define DECROISSANT 1
# define CROISSANT   2

# define DUMALGE_MENDELSON_PNE NON_PNE

# define PNE_ACTIVATION_SUPPRESSION_PETITS_TERMES NON_PNE /*OUI_PNE*/

/************************************************************************/
/* Pour les heuristiques */

# define CYCLE_HEURISTIQUES 3
# define NB_MAX_ECHECS_SUCCESSIFS_HEURISTIQUE 3
# define NB_MAX_REFUS_SUCCESSIFS 2
# define NB_MAX_REACTIVATION_SANS_SUCCES 3 /* Quand on depasse ce seuil sans trouver de solution on arrete l'heuristique */

# define UTILISER_LES_COUPES NON_PNE


/************************************************************************/
/* Pour la reduction des coeff des variables entieres */

# define EPS_COEFF_REDUCTION   1.e-3 /* 1.e-3 */ /******************************/
# define ECART_ABSOLU_MIN      1.e-3 /* 1.e-3 */ /*****************************/
# define ECART_RELATIF_MIN     1.e-3 /* 1.e-4 */
# define MARGE_REDUCTION       0.e-3 /*1.e-3*/ /*****************************/
# define NB_ITER_MX_REDUCTION  1 /*10*/ /* Si on met un nombre superieur a 1 la reduction est trop agressive et peut creer trop d'imprecisions */

/************************************************************************/
/* Pour le node presolve et le probing */

# define PROBING_JUSTE_APRES_LE_PRESOLVE OUI_PNE  

# define FIXE_AU_DEPART         10
# define FIXATION_SUR_BORNE_INF 11
# define FIXATION_SUR_BORNE_SUP 12
# define FIXATION_A_UNE_VALEUR  13
# define PROFONDEUR_MIN_POUR_NODE_PRESOLVE 2 /* 10 */
# define CYCLE_NODE_PRESOLVE 1
# define PRESOLVE_SIMPLIFIE_POUR_NODE_PRESOLVE 1
# define PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE 2

# define MODIF_BORNE_INF 1
# define MODIF_BORNE_SUP 2

# define MARGE_INITIALE_VARIABLES_CONTINUES 5.e-6 /*1.5e-6*/
# define MARGE_INITIALE_VARIABLES_ENTIERES  5.e-6 /*1.5e-6*/
# define ZERO_NP_PROB  1.e-8 /* 1.e-7 */

/************************************************************************/
/* Pour les groupes de variables equivalents */
# define PRISE_EN_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTS NON_PNE /* NON_PNE */
# define INCREMENT_TAILLE_GROUPES 50
# define INCREMENT_TAILLE_NOMBRE_DE_VARIABLES_DUN_GROUPE 10

/************************************************************************/

# define MARGE_EN_FIN_DE_CONTRAINTE 5 /* Nombre de termes qu'on peut ajouter a chaque contrainte */

/************************************************************************/

# define UTILISER_UMIN_AMELIORE NON_PNE /* Ou OUI_PNE */ /* On stocke pour les reutiliser les bornes inf deduites du reduced cost fixing */

# define RECUPERER_XMAX_AU_NOEUD_RACINE_DANS_LE_NODE_PRESOLVE OUI_PNE       /*OUI_PNE*/
# define RECUPERER_XMAX_DANS_LE_REDUCED_COST_FIXING_AU_NOEUD_RACINE OUI_PNE /*OUI_PNE*/

# define BORNES_SPECIFIQUES_POUR_CALCUL_BMIN_BMAX NON_PNE

# define FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES NON_PNE /* Ca ne sert a rien car à la fin du simplexe les variables prennent automatiquement
                                                                   une valeur superieure a la borne mofidiee par le presolve ou le reduced cost
																																	 fixing au noeud racine.
																																	 Par contre en met OUI_PNE pour les utiliser dans le presolve simplifie */

/*******************************************************************************************/

# define NETTOYAGE_DES_DECIMALES_SUR_BORNES_DES_VARIABLES NON_PNE
# define MARGE_SUR_LA_MODIFICATION_DE_BORNE 1.e-6  /*1.e-2*/ /*1.e-5*/
# define COEFF_POUR_NETTOYAGE_DES_DECIMALES 1.e+12 /*1.e+12*/
# define UN_SUR_COEFF_POUR_NETTOYAGE_DES_DECIMALES 1.e-12 /*1.e-12*/
																																	 
/*******************************************************************************************/
# define CONSTANTES_INTERNES_PNE_DEJA_DEFINIES 
# endif  
# ifdef __cplusplus
  }
# endif		
