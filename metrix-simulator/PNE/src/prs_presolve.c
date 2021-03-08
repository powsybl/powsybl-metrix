/***********************************************************************

   FONCTION: Presolve
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_fonctions.h"  
# include "pne_define.h"

# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "prs_memoire.h"
# endif

# define NI_AU_DEBUT_NI_A_LA_FIN  0
# define AU_DEBUT  1
# define A_LA_FIN  2
# define AU_DEBUT_ET_A_LA_FIN  3

# if UTILISER_LE_GRAPHE_DE_CONFLITS == OUI_PNE
  # define AMELIORATION_DES_COEFF_BINAIRES  /*NI_AU_DEBUT_NI_A_LA_FIN*/ /*AU_DEBUT*/  /*A_LA_FIN*/ AU_DEBUT_ET_A_LA_FIN	
# else
  # define AMELIORATION_DES_COEFF_BINAIRES  AU_DEBUT_ET_A_LA_FIN 
# endif


/* Modif du 23/8/2016: NI_AU_DEBUT_NI_A_LA_FIN dans tous les cas */
# undef AMELIORATION_DES_COEFF_BINAIRES
# define AMELIORATION_DES_COEFF_BINAIRES  NI_AU_DEBUT_NI_A_LA_FIN 

# define NETTOYAGE_DES_DECIMALES_SUR_BORNES_DES_VARIABLES NON_PNE /*NON_PNE*/
# define COEFF_POUR_NETTOYAGE_DES_DECIMALES 1.e+12 /*1.e+12*/
# define UN_SUR_COEFF_POUR_NETTOYAGE_DES_DECIMALES 1.e-12 /*1.e-12*/

# define TRACES 0

/*----------------------------------------------------------------------------*/

void PRS_Presolve( void * PneE )
{		      
int NbModifications; int Relancer; int NombreDeVariables; int BorneAmelioree; int VariableFixee;
int NbCycles; int NbCntInact; int Var; char * ContrainteInactive; int * TypeDeBornePourPresolve;
int * TypeDeBorne; double * U; double * Umin; double * Umax; int * TypeDeVariable; int NbMacroIt;
double * ValeurDeXPourPresolve; double * BorneInfPourPresolve; double * BorneSupPourPresolve;
char * ConserverLaBorneSupDuPresolve; char * ConserverLaBorneInfDuPresolve; double Marge;
PRESOLVE * Presolve; PROBLEME_PNE * Pne; double NouvelleBorneTropGrande; 

Pne = (PROBLEME_PNE *) PneE;
if ( Pne == NULL ) return;

Presolve = ( PRESOLVE *) Pne->ProblemePrsDuSolveur;
if ( Presolve == NULL ) return;
 
#if VERBOSE_PRS  
  printf("Phase de Presolve\n");  
#endif

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

PNE_CalculPlusGrandEtPlusPetitTerme( Pne );

PRS_InitPresolve( Presolve );

NbCycles = 0; /* Pour eviter les warning de compilation */
NbMacroIt = 0;

/***********************************************************************************/

/* Attention: ne peut etre appele que sur les bornes natives des variables ou sur les
   bornes definitives. Donc pas pendant les iterations de presolve car les bornes
	 du presolve ne sont pas toutes conservees. */
# if AMELIORATION_DES_COEFF_BINAIRES == AU_DEBUT || AMELIORATION_DES_COEFF_BINAIRES == AU_DEBUT_ET_A_LA_FIN
  PNE_AmeliorerLesCoefficientsDesVariablesBinaires( Pne, (void *) Presolve, MODE_PRESOLVE );	
# endif

Relancer = OUI_PNE;   /* C'est juste pour passer dans le while */

while ( Relancer == OUI_PNE && NbCycles < 5 /*5*/ && Pne->YaUneSolution != PROBLEME_INFAISABLE ) {

  NbCycles++;
	
  #if VERBOSE_PRS
    printf("Cycle de presolve numero %d \n",NbCycles);
  #endif
  Relancer = NON_PNE;
			
	/* Singleton sur ligne */	
	NbModifications = 1;	
	while( NbModifications > 0 ) {	
	  NbModifications = 0;		
    PRS_SingletonsSurLignes( Presolve, &NbModifications );		
		# if TRACES == 1
	    if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_SingletonsSurLignes\n");
		# endif
	  if ( Pne->YaUneSolution != OUI_PNE ) break;
    if ( NbModifications != 0 ) Relancer = OUI_PNE;		
  }
	
	/* Singleton sur colonne */	
  NbModifications = 1;
  while( NbModifications > 0 ) {	
	  NbModifications = 0;
	  PRS_SingletonsSurColonnes( Presolve, &NbModifications );
		# if TRACES == 1
	    if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_SingletonsSurColonnes\n");
		# endif		
	  if ( Pne->YaUneSolution != OUI_PNE ) break;
	  if ( NbModifications != 0 ) Relancer = OUI_PNE;	  	
	}
			
	/* On cherche a borner les limites infinies a l'aide contraintes dont il manque
	   des bornes a une seule variable */
		 
	PRS_BornerLesVariablesNonBornees( Presolve, &NbModifications );
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_BornerLesVariablesNonBornees\n");
	# endif			
  if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( NbModifications != 0 ) Relancer = OUI_PNE;
	
	/********************************************/	
	if ( NbCycles <= 3 || 1 ) { 
	
		PNE_CalculPlusGrandEtPlusPetitTerme( Pne );
		
    PNE_AmeliorerLesCoefficientsDesVariablesBinaires( Pne, (void *) Presolve, MODE_PRESOLVE );
		
	}	
	/********************************************/
	
  /* On cherche a ameliorer les bornes */	
  PRS_CalculerLesBornesDeToutesLesContraintes( Presolve, &NbModifications );
	
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_CalculerLesBornesDeToutesLesContraintes\n");
	# endif			
	if ( Pne->YaUneSolution != OUI_PNE ) break;
	
  if ( NbModifications != 0 ) Relancer = OUI_PNE;	    	
	
  PRS_ContraintesToujoursInactives( Presolve, &NbCntInact );
	
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_ContraintesToujoursInactives\n");
	# endif			
	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( NbCntInact != 0 ) Relancer = OUI_PNE;
	
  PRS_AmeliorerLesBornes( Presolve, &BorneAmelioree );	
  # if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_AmeliorerLesBornes\n");
  # endif		
  if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( BorneAmelioree == OUI_PNE ) Relancer = OUI_PNE;
		
	PRS_ClasserLignesEtColonnesEnFonctionDuNombreDeTermes( Presolve );
		
	/***********************************************************************************************/
	
  /* Desactivation de certaines contraintes et augmentation du creux si possible */
	
 	PRS_AugmenterLeCreuxDesContraintes( Presolve, &NbModifications );
 	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_AugmenterLeCreuxDesContraintes\n");
	# endif			
	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( NbModifications != 0 ) Relancer = OUI_PNE;

	PRS_CalculerLesBornesDeToutesLesContraintes( Presolve, &NbModifications );	
  PRS_ContraintesToujoursInactives( Presolve, &NbCntInact );	
  
	/***********************************************************************************************/	
			
  # if VARIABLE_PROBING_SUR_VARIABLES_BORNEES_DES_2_COTES == OUI_PNE
    PRS_ProbingSurLesColonnesSingleton( Presolve, &NbModifications );
    # if TRACES == 1
      printf("ProbingSurLesColonnesSingleton, nombre de bornes calculees sur les variables duales: %d\n",NbModifications);
		# endif
    if ( NbModifications != 0 ) Relancer = OUI_PNE;
  # endif	

	
	PRS_AmeliorerLesBornes( Presolve, &BorneAmelioree );	
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_AmeliorerLesBornes\n");
	# endif		
 	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( BorneAmelioree == OUI_PNE ) Relancer = OUI_PNE;

  PRS_AnalyseDuGrapheDeConflits( Presolve, &NbModifications );
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_AnalyseDuGrapheDeConflits\n");
	# endif		
 	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( BorneAmelioree == OUI_PNE ) Relancer = OUI_PNE;

  PRS_BornerLesVariablesDualesNonBornees( Presolve, &NbModifications );	
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_BornerLesVariablesDualesNonBornees\n");
	# endif			
  if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( NbModifications != 0 ) Relancer = OUI_PNE;	
  
	/* A verifier: il ne faut pas de substitution de variable ou si presolve uniquement */
  PRS_VariablesDualesEtCoutsReduits( Presolve, &BorneAmelioree );
	
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_VariablesDualesEtCoutsReduits\n");
	# endif			
	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( BorneAmelioree == OUI_PNE ) Relancer = OUI_PNE;

  /* On regarde si on peut fixer des variables a l'aide de leurs couts: si une variable 
  n'intervient que dans des inegalites (ici toujours <) et que le signe de son cout et 
  le meme que ceux de ses coeff. dans les contraintes alors on peut fixer la variable 
  sur une de ses bornes */
	
  PRS_FixerVariablesSurCritere( Presolve, &VariableFixee );	
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_FixerVariablesSurCritere\n");
	# endif				
	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( VariableFixee == OUI_PNE ) Relancer = OUI_PNE;
	
  /* Si une variable n'intervient pas dans les contraintes et n'est pas deja fixee on peut la 
     fixer sur une de ses bornes en fonction de son cout */	
 	PRS_VariablesHorsContraintes( Presolve, &NbModifications );
		
	# if TRACES == 1  
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_VariablesHorsContraintes\n");
	# endif		
	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( NbModifications != 0 ) Relancer = OUI_PNE;
				 
	/* A mettre en service parce que ca marche */
	PRS_SubstituerVariables( Presolve, &NbModifications );	
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_SubstituerVariables\n");
	# endif			
	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( NbModifications != 0 ) Relancer = OUI_PNE;
	
	PRS_ContraintesColineaires( Presolve, &NbModifications );	
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_ContraintesColineaires\n");
	# endif			
	if ( Pne->YaUneSolution != OUI_PNE ) break;
  if ( NbModifications != 0 ) Relancer = OUI_PNE;		
	
	/* Si presolve uniquement par appel pendant un branch and bound: on ne supprime pas les colonnes colineaires */
  if ( Pne->Controls != NULL ) {
    if ( Pne->Controls->PresolveUniquement == OUI_PNE ) {
	    NbModifications = 0;
	  }
  } 
	else {	
	  PRS_ColonnesColineaires( Presolve, &NbModifications );

	  /***********************************************************************************************/
	  /* Augmentation du creux des colonnes si possible */		
    PRS_AugmenterLeCreuxDesColonnes( Presolve, &NbModifications );		
 	  # if TRACES == 1
	    if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_AugmenterLeCreuxDesColonnes\n");
	  # endif			
	  if ( Pne->YaUneSolution != OUI_PNE ) break;
    if ( NbModifications != 0 ) Relancer = OUI_PNE;	

	  /***********************************************************************************************/	
		
	}
	# if TRACES == 1
	  if ( Pne->YaUneSolution != OUI_PNE ) printf("Probleme infaisable apres PRS_ColonnesColineaires\n");
	# endif			
	if ( Pne->YaUneSolution != OUI_PNE ) break;
	if ( NbModifications != 0 ) Relancer = OUI_PNE;

	/***************** Test *********************/
  /* Ca ne sert a rien */
	/*
  PRS_TesterLaDominanceDesVariables( Presolve );
  */
  /***************** Fin test *****************/
				
}

#if VERBOSE_PRS
  printf("Fin des cycles de presolve\n");
#endif

/* Les contraintes dont toutes les variables sont fixees ne servent a rien */

PRS_SupprimerLesContraintesAvecQueDesVariablesFixees( Presolve );

if ( Pne->Controls != NULL ) {
  if ( Pne->Controls->PresolveUniquement == OUI_PNE ) {
	  Pne->Controls->Presolve = (void *) Presolve;
	  return;
	}
}

NombreDeVariables = Pne->NombreDeVariablesTrav;
U = Pne->UTrav;  
Umin = Pne->UminTrav;
Umax = Pne->UmaxTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;

TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
ValeurDeXPourPresolve = Presolve->ValeurDeXPourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;
ConserverLaBorneSupDuPresolve = Presolve->ConserverLaBorneSupDuPresolve;
ConserverLaBorneInfDuPresolve = Presolve->ConserverLaBorneInfDuPresolve;
ContrainteInactive = Presolve->ContrainteInactive;

NouvelleBorneTropGrande = 1.e+20;
/*NouvelleBorneTropGrande = 0.5 * LINFINI_PNE;*/

Marge = MARGE_SUR_LA_MODIFICATION_DE_BORNE;

for ( Var = 0 ; Var < Pne->NombreDeVariablesTrav ; Var++ ) {

  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) { /* C'est entre autres toujours vrai si c'est dans les donnees de depart */		
    U[Var] = ValeurDeXPourPresolve[Var];
    Umin[Var] = BorneInfPourPresolve[Var];
    Umax[Var] = BorneSupPourPresolve[Var];		
    TypeDeBorne[Var] = TypeDeBornePourPresolve[Var];				
		continue;
	}
	else if ( ConserverLaBorneInfDuPresolve[Var] == OUI_PNE ) {			
	  U[Var] = 0.0; /* Pas d'importance */		
		Umin[Var] = BorneInfPourPresolve[Var];
	  if ( ConserverLaBorneSupDuPresolve[Var] == OUI_PNE ) {		
		  Umax[Var] = BorneSupPourPresolve[Var];						
			TypeDeBorne[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;			
		}
		else {
			if ( TypeDeBorne[Var] == VARIABLE_NON_BORNEE ) TypeDeBorne[Var] = VARIABLE_BORNEE_INFERIEUREMENT;
			else if ( TypeDeBorne[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;			
		}
	}
	else if ( ConserverLaBorneSupDuPresolve[Var] == OUI_PNE ) {	
	  /* Donc ConserverLaBorneInfDuPresolve[Var] est egal a NON_PNE */  
	  U[Var] = 0.0; /* Pas d'importance */		
		Umax[Var] = BorneSupPourPresolve[Var];		
		if ( TypeDeBorne[Var] == VARIABLE_NON_BORNEE ) TypeDeBorne[Var] = VARIABLE_BORNEE_SUPERIEUREMENT;
		else if ( TypeDeBorne[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;				
	}
  else {				
    /* On garde les bornes du presolve mais on les elargi un petit peu */
		/* Si c'est un entier on le garde */		
		if ( TypeDeVariable[Var] == ENTIER ) continue;		
	  U[Var] = 0.0;
    if ( TypeDeBornePourPresolve[Var] == VARIABLE_NON_BORNEE ) {
      TypeDeBorne[Var] = VARIABLE_NON_BORNEE;
      Umin[Var] = -LINFINI_PNE;
			Umax[Var] = LINFINI_PNE;						
			continue;
		}
		else if ( TypeDeBornePourPresolve[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {
		  if ( TypeDeBorne[Var] == VARIABLE_NON_BORNEE ) {
			  if ( BorneInfPourPresolve[Var] > -NouvelleBorneTropGrande ) {
          TypeDeBorne[Var] = VARIABLE_BORNEE_INFERIEUREMENT;
					Umin[Var] = BorneInfPourPresolve[Var] - Marge;					
				  Umax[Var] = LINFINI_PNE;									
				}
			}
			else if ( TypeDeBorne[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {
			  if ( BorneInfPourPresolve[Var] - Marge > Umin[Var] ) {
				  /* On reste sur le meme type de borne mais on change la borne si possible */
			    Umin[Var] = BorneInfPourPresolve[Var] - Marge;				
				  Umax[Var] = LINFINI_PNE;					
				}
      }			
		}
		else if ( TypeDeBornePourPresolve[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
		  if ( TypeDeBorne[Var] == VARIABLE_NON_BORNEE ) {
			  if ( BorneSupPourPresolve[Var] < NouvelleBorneTropGrande ) {
          TypeDeBorne[Var] = VARIABLE_BORNEE_SUPERIEUREMENT;
          Umin[Var] = -LINFINI_PNE;				
			    Umax[Var] = BorneSupPourPresolve[Var] + Marge;					
				}
			}
			else if ( TypeDeBorne[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
			  if ( BorneSupPourPresolve[Var] + Marge < Umax[Var] ) {
				  /* On reste sur le meme type de borne mais on change la borne si possible */
          Umin[Var] = -LINFINI_PNE;
			    Umax[Var] = BorneSupPourPresolve[Var] + Marge;										
				}
			}
		}				
		else if ( TypeDeBornePourPresolve[Var] == VARIABLE_BORNEE_DES_DEUX_COTES ) {
		  if ( TypeDeBorne[Var] == VARIABLE_NON_BORNEE ) {
			  if ( BorneInfPourPresolve[Var] > -NouvelleBorneTropGrande && BorneSupPourPresolve[Var] < NouvelleBorneTropGrande ) {
          TypeDeBorne[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;
			    Umin[Var] = BorneInfPourPresolve[Var] - Marge;				
			    Umax[Var] = BorneSupPourPresolve[Var] + Marge;					
				}
			}
			else if ( TypeDeBorne[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {			
			  if ( BorneInfPourPresolve[Var] - Marge > Umin[Var] ) Umin[Var] = BorneInfPourPresolve[Var] - Marge; 
			  if ( BorneSupPourPresolve[Var] < NouvelleBorneTropGrande ) {			
			    Umax[Var] = BorneSupPourPresolve[Var] + Marge;					
          TypeDeBorne[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;										
				}				
			}
			else if ( TypeDeBorne[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) {			
			  if ( BorneSupPourPresolve[Var] + Marge < Umax[Var] ) Umax[Var] = BorneSupPourPresolve[Var] + Marge;	
			  if ( BorneInfPourPresolve[Var] > -NouvelleBorneTropGrande ) {
			    Umin[Var] = BorneInfPourPresolve[Var] - Marge;					
          TypeDeBorne[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;										
				}
			}  
			else if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES ) {			
			  if ( BorneInfPourPresolve[Var] - Marge > Umin[Var] ) {
				  Umin[Var] = BorneInfPourPresolve[Var] - Marge;										
				}
			  if ( BorneSupPourPresolve[Var] + Marge < Umax[Var] ) {
				  Umax[Var] = BorneSupPourPresolve[Var] + Marge;										
				}				
			}			
		}				
	}	
}

/* Attention: ne peut etre appele que sur les bornes natives des variables ou sur les
   bornes definitives. Donc pas pendant les iterations de presolve car les bornes
	 du presolve ne sont pas toutes conservees. */
# if AMELIORATION_DES_COEFF_BINAIRES == A_LA_FIN || AMELIORATION_DES_COEFF_BINAIRES == AU_DEBUT_ET_A_LA_FIN 
  /* Ici on beneficie de la creation de bornes la ou il n'y en n'avait pas */
  for ( Var = 0 ; Var < Pne->NombreDeVariablesTrav ; Var++ ) {
    TypeDeBornePourPresolve[Var] = TypeDeBorne[Var];
		BorneInfPourPresolve[Var] = Umin[Var];
    BorneSupPourPresolve[Var] = Umax[Var];
  }
  PNE_AmeliorerLesCoefficientsDesVariablesBinaires( Pne, (void *) Presolve, MODE_PRESOLVE );		
# endif

/* Pour sortir le graphe au format DIMACS */
/*PRS_ConstruireLeGrapheAuFormatDIMACS( Presolve );*/

PRS_EnleverLesContraintesInactives( Presolve );

/* Utile dans le cas ou une variable de bornes identiques n'etait fournie en tant que variable fixe */
# if NETTOYAGE_DES_DECIMALES_SUR_BORNES_DES_VARIABLES == OUI_PNE
  for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
    if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
    if ( Umin[Var] == Umax[Var] ) {
      TypeDeBorne[Var] = VARIABLE_FIXE;
      TypeDeVariable[Var] = REEL;			
		  Marge = Umin[Var];
      Marge *= COEFF_POUR_NETTOYAGE_DES_DECIMALES; 
	    Marge = floor( Marge );
	    Marge *= UN_SUR_COEFF_POUR_NETTOYAGE_DES_DECIMALES;		
      U[Var] = Marge;
		  Umin[Var] = Marge;
		  Umax[Var] = Marge;			
		  continue;
	  }
    if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {
      Umin[Var] *= COEFF_POUR_NETTOYAGE_DES_DECIMALES;
	    Umin[Var] = floor( Umin[Var] );
	    Umin[Var] *= UN_SUR_COEFF_POUR_NETTOYAGE_DES_DECIMALES;
	  }
    if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
      Umax[Var] *= COEFF_POUR_NETTOYAGE_DES_DECIMALES;
	    Umax[Var] = ceil( Umax[Var] );
	    Umax[Var] *= UN_SUR_COEFF_POUR_NETTOYAGE_DES_DECIMALES;  
    }	
  }
# else
  for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
    if ( Umin[Var] == Umax[Var] ) {
      TypeDeBorne[Var] = VARIABLE_FIXE;
      TypeDeVariable[Var] = REEL;
      U[Var] = Umin[Var];
	  }
  }
# endif

#if VERBOSE_PRS
  printf("Nombre de cycles de presolve %d \n",NbCycles);
#endif

return;
}
 
