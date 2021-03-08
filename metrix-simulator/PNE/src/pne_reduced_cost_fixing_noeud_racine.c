/***********************************************************************

   FONCTION: Reduced cost fixing au noeud racine. Appele a chaque fois
	           qu'on trouve une nouvelle solution entiere pour fixer certaines
						 variables en fonction de la valeur de leur cout reduit au
						 noeud racine.
						 Remarque: si on fixe des variables on pourrait en profiter
						 pour refaire un probing avec determination de nouvelles
						 cliques.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_fonctions.h"
# include "spx_define.h"

# include "bb_define.h"
# include "bb_fonctions.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES 0
# define TEST_RELANCE_BRANCH_AND_BOUND 0  

# define MARGE_CONTINUE 10
# define MARGE_DE_SECURITE 0.05
# define DEPASSEMENT_MIN   1.

# define MARGE_POUR_RELAXATION 1.e-8 
																																			
/*----------------------------------------------------------------------------*/

void PNE_ArchivagesPourReducedCostFixingAuNoeudRacine( PROBLEME_PNE * Pne,
																											 int * PositionDeLaVariable,
																											 double * CoutsReduits,
																											 double Critere )
{
int i ; double * CoutsReduitsAuNoeudRacine; int * PositionDeLaVariableAuNoeudRacine;
double MxCoutReduit; double * Umin; double * Umax; double X; int * TypeDeBorneTrav;
double * UminALaResolutionDuNoeudRacine; double * UmaxALaResolutionDuNoeudRacine;

if ( Pne->CoutsReduitsAuNoeudRacine == NULL ) {
  Pne->CoutsReduitsAuNoeudRacine = (double *) malloc( Pne->NombreDeVariablesTrav * sizeof( double ) );
	if ( Pne->CoutsReduitsAuNoeudRacine == NULL ) return;
}

if ( Pne->PositionDeLaVariableAuNoeudRacine == NULL ) {
  Pne->PositionDeLaVariableAuNoeudRacine = (int *) malloc( Pne->NombreDeVariablesTrav * sizeof( int ) );
	if ( Pne->PositionDeLaVariableAuNoeudRacine == NULL ) {
	  free( Pne->CoutsReduitsAuNoeudRacine );
	  return;    
	}
}

if ( Pne->UminALaResolutionDuNoeudRacine == NULL ) {
  Pne->UminALaResolutionDuNoeudRacine = (double *) malloc( Pne->NombreDeVariablesTrav * sizeof( double ) );
	if ( Pne->UminALaResolutionDuNoeudRacine == NULL ) {
	  free( Pne->CoutsReduitsAuNoeudRacine );
	  free( Pne->PositionDeLaVariableAuNoeudRacine );
	  return;    
	}
}

if ( Pne->UmaxALaResolutionDuNoeudRacine == NULL ) {
  Pne->UmaxALaResolutionDuNoeudRacine = (double *) malloc( Pne->NombreDeVariablesTrav * sizeof( double ) );
	if ( Pne->UmaxALaResolutionDuNoeudRacine == NULL ) {
	  free( Pne->CoutsReduitsAuNoeudRacine );
	  free( Pne->PositionDeLaVariableAuNoeudRacine );
	  free( Pne->UminALaResolutionDuNoeudRacine );
	  return;    
	}
}

CoutsReduitsAuNoeudRacine = Pne->CoutsReduitsAuNoeudRacine;
PositionDeLaVariableAuNoeudRacine = Pne->PositionDeLaVariableAuNoeudRacine;
UminALaResolutionDuNoeudRacine = Pne->UminALaResolutionDuNoeudRacine;
UmaxALaResolutionDuNoeudRacine = Pne->UmaxALaResolutionDuNoeudRacine;
 
Pne->CritereAuNoeudRacine = Critere;

MxCoutReduit = -1;
Umin = Pne->UminTrav;
Umax = Pne->UmaxTrav;
TypeDeBorneTrav = Pne->TypeDeBorneTrav;
for ( i = 0 ; i < Pne->NombreDeVariablesTrav ; i++ ) {
  UminALaResolutionDuNoeudRacine[i] = Umin[i];
  UmaxALaResolutionDuNoeudRacine[i] = Umax[i];	
  PositionDeLaVariableAuNoeudRacine[i] = PositionDeLaVariable[i];	
  CoutsReduitsAuNoeudRacine[i] = CoutsReduits[i];
	X = fabs( CoutsReduits[i] * ( Umax[i] - Umin[i] ) );
  if ( TypeDeBorneTrav[i] != VARIABLE_BORNEE_DES_DEUX_COTES ) {	
    if ( X > MxCoutReduit ) {
      MxCoutReduit = X;
    }
	}
}

Pne->MxCoutReduitAuNoeudRacineFoisDeltaBornes = MxCoutReduit;

return;
}

/*----------------------------------------------------------------------------*/
/* Appele a chaque fois qu'on trouve une solution entiere */
void PNE_ReducedCostFixingAuNoeudRacine( PROBLEME_PNE * Pne )														
{
int Var; double Delta; double h; int * TypeDeBorne; int * TypeDeVariable; int Arret;
double * CoutsReduitsALaResolutionDuNoeudRacine; int * PositionDeLaVariableALaResolutionDuNoeudRacine;
int NbFix; int * T; int Nb; int * Num; int j; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; int NombreDeVariables;
double NouvelleValeur; char * SensContrainte; double * B; char * ContrainteActivable;
int Cnt; int NbCntRelax; int NombreDeContraintes; int NbFixBin; int Faisabilite; char CntActiv;
char * BorneSupConnue; double * ValeurDeBorneSup; char * BorneInfConnue; double * ValeurDeBorneInf;
char BrnInfConnue; char BorneMiseAJour; char UneVariableAEteFixee; double * Umin;
double * UminSv; double * Umax; double * UmaxSv; char * BminValide; char * BmaxValide;
double * BminSv; double * BmaxSv; double * Bmin; double * Bmax; char Mode; int * TypeDeBorneSv;
int NombreDeVariablesNonFixes; int * NumeroDesVariablesNonFixes; int i; int NbBranchesAjoutees;
double * UminALaResolutionDuNoeudRacine; double * UmaxALaResolutionDuNoeudRacine;
double Marge;
# if UTILISER_UMIN_AMELIORE == OUI_PNE
  double * UminAmeliore;
# endif
# if TEST_RELANCE_BRANCH_AND_BOUND == 1
  BB * Bb;
# endif
# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
  double * UminAmeliorePourCoupes;
# endif

# if REDUCED_COST_FIXING_AU_NOEUD_RACINE == NON_PNE  
  return;
# endif

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
if ( ProbingOuNodePresolve == NULL ) return;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;
NombreDeVariablesNonFixes = Pne->NombreDeVariablesNonFixes;
NumeroDesVariablesNonFixes = Pne->NumeroDesVariablesNonFixes;

T = (int *) malloc( NombreDeVariables * sizeof( int ) );
if ( T == NULL ) return;
Num = (int *) malloc( NombreDeVariables * sizeof( int ) );
if ( Num == NULL ) {
  free( T );
	return;
}
Nb = 0;
memset( (char *) T, 0, NombreDeVariables * sizeof( int ) );

TypeDeVariable = Pne->TypeDeVariableTrav;

/* Attention c'est le type de borne au noeud racine qu'il faut prendre */
TypeDeBorneSv = Pne->TypeDeBorneTravSv;
TypeDeBorne = Pne->TypeDeBorneTrav;
memcpy( (char *) TypeDeBorne, (char *) TypeDeBorneSv, NombreDeVariables * sizeof( int ) );

Umin = Pne->UminTrav;
UminSv = Pne->UminTravSv;
Umax = Pne->UmaxTrav;
UmaxSv = Pne->UmaxTravSv;

# if UTILISER_UMIN_AMELIORE == OUI_PNE
  UminAmeliore = Pne->UminAmeliore;
# endif

# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
  UminAmeliorePourCoupes = Pne->UminAmeliorePourCoupes;
# endif

/* Attention c'est Umin au noeud racine qu'il faut prendre */
memcpy( (char *) Umin, (char *) UminSv, NombreDeVariables * sizeof( double ) );
/* Attention c'est Umax au noeud racine qu'il faut prendre */
memcpy( (char *) Umax, (char *) UmaxSv, NombreDeVariables * sizeof( double ) );

# if UTILISER_UMIN_AMELIORE == OUI_PNE
  for ( Var = 0 ; Var < NombreDeVariables; Var++ ) {
    if ( UminAmeliore[Var] != VALEUR_NON_INITIALISEE ) Umin[Var] = UminAmeliore[Var];
  } 
# endif

/* Initialisation des bornes inf et sup des variables */
PNE_InitBorneInfBorneSupDesVariables( Pne );

BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

CoutsReduitsALaResolutionDuNoeudRacine = Pne->CoutsReduitsAuNoeudRacine;
PositionDeLaVariableALaResolutionDuNoeudRacine = Pne->PositionDeLaVariableAuNoeudRacine;
UminALaResolutionDuNoeudRacine =  Pne->UminALaResolutionDuNoeudRacine;
UmaxALaResolutionDuNoeudRacine =  Pne->UmaxALaResolutionDuNoeudRacine;

/* On recupere les bornes des contraintes au noeud racine */
Bmin = ProbingOuNodePresolve->Bmin;
Bmax = ProbingOuNodePresolve->Bmax;
BminSv = ProbingOuNodePresolve->BminSv;
BmaxSv = ProbingOuNodePresolve->BmaxSv;
memcpy( (char *) Bmin, (char *) BminSv, NombreDeContraintes * sizeof( double ) );
memcpy( (char *) Bmax, (char *) BmaxSv, NombreDeContraintes * sizeof( double ) );
BminValide = ProbingOuNodePresolve->BminValide;
BmaxValide = ProbingOuNodePresolve->BmaxValide;

SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;
ContrainteActivable = Pne->ContrainteActivable;

Delta = Pne->CoutOpt - Pne->CritereAuNoeudRacine;
if ( Delta < 0. ) Delta = 0.; /* Car le cout entier est toujours superieur ou egal au cout relaxe */

Mode = PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE;

NbFix = 0;
NbFixBin = 0;
NbCntRelax = 0;
NbBranchesAjoutees = 0;

ProbingOuNodePresolve->Faisabilite = OUI_PNE;
ProbingOuNodePresolve->VariableInstanciee = -1;
ProbingOuNodePresolve->NbVariablesModifiees = 0;
ProbingOuNodePresolve->NbContraintesModifiees = 0;
ProbingOuNodePresolve->NombreDeContraintesAAnalyser = 0;
ProbingOuNodePresolve->IndexLibreContraintesAAnalyser = 0;

Faisabilite = OUI_PNE;

for ( i = 0 ; i < NombreDeVariablesNonFixes ; i++ ) {

  Var = NumeroDesVariablesNonFixes[i];
	
  if ( fabs( CoutsReduitsALaResolutionDuNoeudRacine[Var] ) < ZERO_COUT_REDUIT ) continue;
	
  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;
				 	  
	h = 0.0;
	Nb = 0;

  if ( TypeDeVariable[Var] == ENTIER ) {		
    if( PositionDeLaVariableALaResolutionDuNoeudRacine[Var] == HORS_BASE_SUR_BORNE_SUP ) {	
	    /* On regarde ce qu'il se passe si la variable passe a 0 */
		  Arret = NON_PNE;			
      PNE_ReducedCostFixingConflictGraph( Pne, Var, UminALaResolutionDuNoeudRacine[Var], &h, Delta,
			                                    CoutsReduitsALaResolutionDuNoeudRacine,
			                                    UminALaResolutionDuNoeudRacine,
			                                    UmaxALaResolutionDuNoeudRacine,
			                                    PositionDeLaVariableALaResolutionDuNoeudRacine,
																					&Nb, T, Num, &Arret );																					
      if ( h > Delta ) {		
	      /* La variable entiere est fixee a Umax */				
        BorneMiseAJour = NON_PNE;
			  UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;
				NouvelleValeur = UmaxALaResolutionDuNoeudRacine[Var];
        /* Applique le graphe de conflit s'il y a lieu, modifie Bmin et Bmax, fixe les variables entieres du graphe
				   si necessaire */
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
	
			  NbFix++;
				NbFixBin++;
      }
      for ( j = 0 ; j < Nb ; j++ ) T[Num[j]] = 0;			
    }  
    else if( PositionDeLaVariableALaResolutionDuNoeudRacine[Var] == HORS_BASE_SUR_BORNE_INF ) {
	    /* On regarde ce qu'il se passe si la variable passe a 1 */
		  Arret = NON_PNE;
      PNE_ReducedCostFixingConflictGraph( Pne, Var, UmaxALaResolutionDuNoeudRacine[Var], &h, Delta,
			                                    CoutsReduitsALaResolutionDuNoeudRacine,
			                                    UminALaResolutionDuNoeudRacine,
			                                    UmaxALaResolutionDuNoeudRacine,
			                                    PositionDeLaVariableALaResolutionDuNoeudRacine,
																					&Nb, T, Num, &Arret );																					
      if ( h > Delta ) {		
	      /* La variable entiere sest fixee a Umin */
        BorneMiseAJour = NON_PNE;
			  UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;
				NouvelleValeur = UminALaResolutionDuNoeudRacine[Var];
        /* Applique le graphe de conflit s'il y a lieu, modifie Bmin et Bmax, fixe les variables entieres du graphe
				   si necessaire */
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
				
			  NbFix++;
				NbFixBin++;		
      }
      for ( j = 0 ; j < Nb ; j++ ) T[Num[j]] = 0;			
    }
	}
	else	if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES ) {	
    h = fabs( CoutsReduitsALaResolutionDuNoeudRacine[Var] * ( UmaxALaResolutionDuNoeudRacine[Var] - UminALaResolutionDuNoeudRacine[Var] ) );						
	  /* Variable continue */
    if( PositionDeLaVariableALaResolutionDuNoeudRacine[Var] == HORS_BASE_SUR_BORNE_INF ) {		
	    /* On regarde ce qu'il se passe si la variable passe a BorneSup */
      if ( h > Delta ) {
		    NouvelleValeur = ( Delta / fabs( CoutsReduitsALaResolutionDuNoeudRacine[Var] ) ) + UminALaResolutionDuNoeudRacine[Var];				
			  if ( ValeurDeBorneSup[Var] - NouvelleValeur > DEPASSEMENT_MIN /*MARGE_CONTINUE*/ ) {
          /* On abaisse la borne sup */
          BorneMiseAJour = MODIF_BORNE_SUP;
			    UneVariableAEteFixee = NON_PNE;				
					NouvelleValeur = ( MARGE_DE_SECURITE * ValeurDeBorneSup[Var] ) + ( NouvelleValeur * ( 1 - MARGE_DE_SECURITE ) );
					if ( BorneInfConnue[Var] != NON_PNE ) {
					  if ( NouvelleValeur < ValeurDeBorneInf[Var] ) NouvelleValeur = ValeurDeBorneInf[Var];						
					}
				  # if TRACES == 1
            printf("On peut abaisser la borne Sup de la variable %d : %e -> %e   abaissement %e  CoutsReduits %e\n",
						        Var,ValeurDeBorneSup[Var],NouvelleValeur,ValeurDeBorneSup[Var]-NouvelleValeur,CoutsReduitsALaResolutionDuNoeudRacine[Var]);
          # endif					
          /* Applique le graphe de conflit s'il y a lieu, modifie Bmin et Bmax, fixe les variables entieres du graphe
				     si necessaire */
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
				
			    NbFix++;					
			  }			
      }
    }
    else if( PositionDeLaVariableALaResolutionDuNoeudRacine[Var] == HORS_BASE_SUR_BORNE_SUP ) {		
	    /* On regarde ce qu'il se passe si la variable passe a Xmin */
      if ( h > Delta ) {				  
		    NouvelleValeur = UmaxALaResolutionDuNoeudRacine[Var] - ( Delta / fabs( CoutsReduitsALaResolutionDuNoeudRacine[Var] ) );
			  if ( NouvelleValeur - ValeurDeBorneInf[Var] > DEPASSEMENT_MIN /*MARGE_CONTINUE*/ ) {					  
			    /* On remonte la borne inf */
          BorneMiseAJour = MODIF_BORNE_INF;
			    UneVariableAEteFixee = NON_PNE;			
				  NouvelleValeur = ( MARGE_DE_SECURITE * ValeurDeBorneInf[Var] ) + ( NouvelleValeur * ( 1 - MARGE_DE_SECURITE ) );
					if ( BorneSupConnue[Var] != NON_PNE ) {
					  if ( NouvelleValeur > ValeurDeBorneSup[Var] ) NouvelleValeur = ValeurDeBorneSup[Var];						
					}					
				  # if TRACES == 1
            printf("On peut relever la borne Inf de la variable %d : %e -> %e   relevement %e  Umax %e CoutsReduits %e\n",
						        Var,ValeurDeBorneInf[Var],NouvelleValeur,NouvelleValeur-ValeurDeBorneInf[Var],ValeurDeBorneSup[Var],CoutsReduitsALaResolutionDuNoeudRacine[Var]);				
          # endif				 
          /* Applique le graphe de conflit s'il y a lieu, modifie Bmin et Bmax, fixe les variables entieres du graphe
				     si necessaire */
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
				
			    NbFix++;																	  
        }
      }
	  }
	}	
}


free( T );
free( Num );
			
/* Pour l'instant on n'utilise pas Faisabilite */
PNE_PresolveSimplifie( Pne, ContrainteActivable, Mode, &Faisabilite );
if ( Faisabilite == NON_PNE ) {
  /*
  if ( Pne->AffichageDesTraces == OUI_PNE ) {
	  printf("Alert: PNE_PresolveSimplifie report infeasability after reduced cost fixing. Problem may be infaisible\n");
	}
	*/
}
																													 
/* Synthese */
/* On recupere Umin, Umax */

NbFix = 0;
NbFixBin = 0;

Marge = MARGE_SUR_LA_MODIFICATION_DE_BORNE;
			
for ( i = 0 ; i < NombreDeVariablesNonFixes ; i++ ) {

  Var = NumeroDesVariablesNonFixes[i];

  if ( BorneInfConnue[Var] == FIXE_AU_DEPART ) continue;
  if ( TypeDeVariable[Var] == ENTIER ) {	
	  if ( ValeurDeBorneInf[Var] > UminSv[Var] + 1.e-6 ) { NbFix++; NbFixBin++; }
	  UminSv[Var] = ValeurDeBorneInf[Var];
		
    # if BORNES_INF_AUXILIAIRES == OUI_PNE
      Pne->XminAuxiliaire[Var] = ValeurDeBorneInf[Var];
      Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */			
    # endif
		
	  if ( ValeurDeBorneSup[Var] < UmaxSv[Var] - 1.e-6 ) { NbFix++; NbFixBin++; }
	  UmaxSv[Var] = ValeurDeBorneSup[Var];		
	}
  else {
	
	  if ( ValeurDeBorneInf[Var] > UminSv[Var] + 1.e-6 ) NbFix++; 	
		if ( ValeurDeBorneSup[Var] < UmaxSv[Var] - 1.e-6 ) NbFix++;

    # if BORNES_INF_AUXILIAIRES == OUI_PNE
			if ( BorneInfConnue[Var] != NON_PNE ) {						
        if ( ValeurDeBorneInf[Var] > Pne->XminAuxiliaire[Var] + Marge ) {
          Pne->XminAuxiliaire[Var] = ValeurDeBorneInf[Var] - (0.1*Marge);
          Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */			
        }
			}
    # endif

    # if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
		  if ( UminAmeliorePourCoupes != NULL ) {
		    if ( UminAmeliorePourCoupes[Var] == VALEUR_NON_INITIALISEE ) {
		      if ( ValeurDeBorneInf[Var] - Marge > UminSv[Var] ) UminAmeliorePourCoupes[Var] = ValeurDeBorneInf[Var];         
		    }
		    else if ( ValeurDeBorneInf[Var] - Marge > UminAmeliorePourCoupes[Var] ) UminAmeliorePourCoupes[Var] = ValeurDeBorneInf[Var];		    		
      }		
    # endif
		
    # if UTILISER_UMIN_AMELIORE == OUI_PNE
		  if ( TypeDeBorneSv[Var] != VARIABLE_NON_BORNEE && TypeDeBorneSv[Var] != VARIABLE_BORNEE_SUPERIEUREMENT ) {
			  if ( BorneInfConnue[Var] != NON_PNE ) {						
		    if ( ValeurDeBorneInf[Var] - Marge > UminSv[Var] ) {						
		        UminAmeliore[Var] = ValeurDeBorneInf[Var];
          }
				}
			}
    # endif    	

    # if RECUPERER_XMAX_DANS_LE_REDUCED_COST_FIXING_AU_NOEUD_RACINE == OUI_PNE
		  if ( TypeDeBorneSv[Var] == VARIABLE_NON_BORNEE ) continue;			
	  	if ( BorneSupConnue[Var] == NON_PNE ) continue;			
		  if ( ValeurDeBorneSup[Var] + Marge < UmaxSv[Var] ) {
			  Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */			
 	      UmaxSv[Var] = ValeurDeBorneSup[Var] + Marge;				
				if ( UmaxSv[Var] < UminSv[Var] ) UmaxSv[Var] = UminSv[Var];
        # if UTILISER_UMIN_AMELIORE == OUI_PNE
				  if ( UminAmeliore[Var] != VALEUR_NON_INITIALISEE ) {
				    if ( UmaxSv[Var] < UminAmeliore[Var] ) UmaxSv[Var] = UminAmeliore[Var];
					}
				# endif	
        /* Precaution */
			  if ( UmaxSv[Var] < UminSv[Var] ) {
			    UmaxSv[Var] = UminSv[Var]; /* Si le pb est vraiment infaisable on s'en apercevra ensuite */
			  }				
        if ( TypeDeBorneSv[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {
			    if ( UmaxSv[Var] - UminSv[Var] < 1.e+9 ) {
            TypeDeBorneSv[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;
            TypeDeBorne[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;
				  }
			  }				
		  }
		# endif
	}
}

/* On recalcule Bmin Bmax avec le contenu de Umin et Umax */
for ( i = 0 ; i < Pne->NombreDeVariablesNonFixes ; i++ ) {
  Var = NumeroDesVariablesNonFixes[i];
	Umin[Var] = UminSv[Var];
	Umax[Var] = UmaxSv[Var];
}

# if UTILISER_UMIN_AMELIORE == OUI_PNE
  for ( Var = 0 ; Var < NombreDeVariables; Var++ ) {
    if ( UminAmeliore[Var] != VALEUR_NON_INITIALISEE ) Umin[Var] = UminAmeliore[Var];
  } 
# endif


# if TEST_RELANCE_BRANCH_AND_BOUND == 1
  /* On regarde si on a pu fixer des variables ou abaisser des bornes afin de lancer un presolve plus
     complet si c'est prometteur */
	if ( NbFixBin > 0 || NbFix > 10 ) {
    Bb = (BB *) Pne->ProblemeBbDuSolveur;
    if ( Pne->Controls == NULL ) {	
		  PNE_BranchAndBoundIntermediare( Pne, Bb->ValeurDuMeilleurMinorant );
 	    exit(0);
		}
  }
# endif

# if UTILISER_LES_COUPES_DE_COUTS_REDUITS == OUI_PNE
  /*printf("Attention PNE_ReducedCostFixingAuNoeudRacine appel de PNE_EmpilementDesCoutsReduitsDesVariablesBinairesNonFixees\n");*/
  PNE_EmpilementDesCoutsReduitsDesVariablesBinairesNonFixees( Pne, CoutsReduitsALaResolutionDuNoeudRacine, UminSv, UmaxSv,
																				                      PositionDeLaVariableALaResolutionDuNoeudRacine, Pne->CritereAuNoeudRacine );
# endif

PNE_InitBorneInfBorneSupDesVariables( Pne );
PNE_CalculMinEtMaxDesContraintes( Pne, &Faisabilite );
/* Et on sauvegarde le resultat comme point de depart pour les noeuds suivants */
memcpy( (char *) BminSv, (char *) Bmin, NombreDeContraintes * sizeof( double ) );
memcpy( (char *) BmaxSv, (char *) Bmax, NombreDeContraintes * sizeof( double ) );	

/* Contraintes inactives */

NbCntRelax = 0;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  CntActiv = ContrainteActivable[Cnt];
  # if UTILISER_UMIN_AMELIORE == NON_PNE
    ContrainteActivable[Cnt] = OUI_PNE; 
	# endif	
	if ( SensContrainte[Cnt] == '<' ) {
		if ( BmaxValide[Cnt] == OUI_PNE ) {
      if ( Bmax[Cnt] <= B[Cnt] + MARGE_POUR_RELAXATION ) {
				if ( CntActiv == OUI_PNE ) {
          Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */							
				}
				ContrainteActivable[Cnt] = NON_PNE;
				NbCntRelax++;
			}			
		}		
	}
	else {
		if ( BminValide[Cnt] == OUI_PNE && BmaxValide[Cnt] == OUI_PNE ) {
      if ( fabs( Bmax[Cnt] - Bmin[Cnt] ) < MARGE_POUR_RELAXATION ) {
				if ( fabs( Bmax[Cnt] - B[Cnt] ) < MARGE_POUR_RELAXATION ) {
				  if ( CntActiv == OUI_PNE ) {
            Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */							
				  }				
				  ContrainteActivable[Cnt] = NON_PNE;
				  NbCntRelax++;
				}
      }
    }		
	}
}

if ( Pne->AffichageDesTraces == OUI_PNE ) {
  if ( NbFix != 0 || NbCntRelax != 0 || NbBranchesAjoutees != 0 ) {
	  printf("Reduced cost fixing at root node: \n");
    if ( NbFix != 0 ) printf("        Bounds tightened: %d of which %d binarie(s)\n",NbFix,NbFixBin);
  	if ( NbBranchesAjoutees != 0 ) printf("        %d implications added. Conflict graph has %d edges\n",NbBranchesAjoutees,Pne->ConflictGraph->NbEdges);
  	if ( NbCntRelax != 0 ) printf("        Useless constraints: %d over %d\n",NbCntRelax,NombreDeContraintes);
	}
}

# if PRISE_EN_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTS == OUI_PNE
  PNE_ColonnesColineaires( Pne );
# endif

# if TENIR_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTES_POUR_LE_BRANCHING == OUI_PNE
  /* On desalloue la structure des groupes de variables equivalents */
  free( Pne->NumeroDeGroupeDeVariablesEquivalentes );
  if ( Pne->Groupe != NULL ) {
    for ( i = 0 ; i < Pne->NombreDeGroupesDeVariablesEquivalentes ; i++ ) {
	    free( Pne->Groupe[i]->VariablesDuGroupe );
	    free( Pne->Groupe[i] );
	  }
    free( Pne->Groupe );		
  }
	Pne->Groupe = NULL;
	Pne->NumeroDeGroupeDeVariablesEquivalentes = NULL;
  Pne->NombreDeGroupesDeVariablesEquivalentes = 0;

	PNE_RechercherLesGroupesDeVariablesEquivalentes( Pne );
	
# endif

/* Attention ce n'est utile que si on modifie le second membre */
/*
SPX_ModifierLeVecteurSecondMembre( (PROBLEME_SPX *) Pne->ProblemeSpxDuSolveur, Pne->BTrav, Pne->SensContrainteTrav, Pne->NombreDeContraintesTrav );
*/

/*
PNE_TransformerCliquesEnEgalites( Pne );
exit(0);
*/

/*
PNE_EcrirePresolvedMPS( Pne );
exit(0);
*/

return;
}

/*----------------------------------------------------------------------------*/


