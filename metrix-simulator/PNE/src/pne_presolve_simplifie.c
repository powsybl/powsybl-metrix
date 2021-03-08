/***********************************************************************

   FONCTION: Presolve simplifie. Appele par le reduced cost fixing au noeud
	           racine et par le node presolve.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"

# include "bb_define.h"
# include "bb_fonctions.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define MX_CYCLES_REDUCED_COST_FIXING_AU_NOEUD_RACINE     10 /*10*/
# define MX_CYCLES_REDUCED_COST_FIXING_POUR_NODE_PRESOLVE  5  /*5*/

# define TRACES 0

# define PRISE_EN_COMPTE_DES_CONTRAINTES_DE_BORNES_VARIABLES OUI_PNE /*NON_PNE*/

# define POURCENT_LIMITE 0.01
# define SEUIL_LIMITE    50 /*10*/

# define MARGE_POUR_RELAXATION 1.e-7 /*1.e-8*/
 
# define NOUVEAU_STOCKAGE_COUPES NON_PNE

# define PETITE_MARGE_SUR_BORNES 1.e-6 /*1.e-6*/ 
# define GRANDE_MARGE_SUR_BORNES 1.e-3 /*1.e-3*/
# define NBTERMES_POUR_APPLICATION_DE_LA_MARGE 10
# define ECART_MIN_XI_XS_POUR_MARGE_SUR_BORNES 1.e-6

/********************************************************/ # define FAIRE_DU_PRESOLVE_SUR_LES_COUPES NON_PNE /*OUI_PNE*/

void PNE_PresolveSimplifieContrainteDEgaliteAUneSeuleVariable( PROBLEME_PNE * , int , double , char * , double * , char * , double * , char * );						
void PNE_PresolveSimplifieExaminerLesCoupes( PROBLEME_PNE * );

# if TENIR_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTES_POUR_LE_BRANCHING == OUI_PNE
  void PNE_PresolveSimplifieGroupesDeVariablesEquivalentes( PROBLEME_PNE *, char * );
# endif

/*----------------------------------------------------------------------------*/
/* Appele a chaque fois qu'on trouve une solution entiere */
void PNE_PresolveSimplifie( PROBLEME_PNE * Pne, char * ContrainteActivable, char Mode, int * Faisabilite )														
{
int Var; int * TypeDeBorne; int * TypeDeVariable; double S; double * Bmin; double * Bmax;
char * BminValide; char * BmaxValide; char BmnValide; char BmxValide; char SensCnt;
double Bmn; double Bmx; double BCnt; double Ai; double * A; double Xs; double Xi;
double Xs0; double Xi0; int NombreDeVariables; double BminNew; double BmaxNew; int i;
double NouvelleValeur; char * SensContrainte; double * B; int * Cdeb; int * Csui; int ic;
int * NumContrainte; int Cnt; int NombreDeContraintes;  char ForcingConstraint;
char * BorneSupConnue; double * ValeurDeBorneSup; char * BorneInfConnue; double * ValeurDeBorneInf;
char BrnInfConnue; char BorneMiseAJour; char UneVariableAEteFixee; char RefaireUnCycle;
char XsValide; char XiValide; int NbCycles; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve;
char SigneCoeff; double * L; int NombreDeVariablesNonFixes; int * NumeroDesVariablesNonFixes;
double CoutVar; int MxCycles; int * CNbTerm; int Limite; char VariableFixee; BB * Bb; NOEUD * Noeud;
int * NbTerm; double MargeSurBorne; char YaUneSolutionEntiere; int ProfondeurDuNoeud; 
double EcartBorneInf; double SeuilSurEcartBorneInf;

# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
  double * UminAmeliorePourCoupes;
# endif

/*printf("PresolveSimplifie Mode = %d\n",Mode);*/

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;

if ( ProbingOuNodePresolve == NULL ) return;

Bb = Pne->ProblemeBbDuSolveur; 
Noeud = Bb->NoeudEnExamen;

YaUneSolutionEntiere = Pne->YaUneSolutionEntiere;
ProfondeurDuNoeud = Noeud->ProfondeurDuNoeud;
SeuilSurEcartBorneInf = 0.01;
EcartBorneInf = Bb->EcartBorneInf;
if ( YaUneSolutionEntiere != OUI_PNE ) EcartBorneInf = LINFINI_PNE;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;
NombreDeVariablesNonFixes = Pne->NombreDeVariablesNonFixes;
NumeroDesVariablesNonFixes = Pne->NumeroDesVariablesNonFixes;

L = Pne->LTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;

BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

Bmin = ProbingOuNodePresolve->Bmin;
Bmax = ProbingOuNodePresolve->Bmax;
BminValide = ProbingOuNodePresolve->BminValide;
BmaxValide = ProbingOuNodePresolve->BmaxValide;

SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;
NbTerm = Pne->NbTermTrav;

Cdeb = Pne->CdebTrav;
CNbTerm = Pne->CNbTermTrav;
Csui = Pne->CsuiTrav;
A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;

if ( Mode == PRESOLVE_SIMPLIFIE_POUR_NODE_PRESOLVE ) MxCycles = MX_CYCLES_REDUCED_COST_FIXING_POUR_NODE_PRESOLVE;
else MxCycles = MX_CYCLES_REDUCED_COST_FIXING_AU_NOEUD_RACINE;

Pne->ProbingOuNodePresolve->Faisabilite = OUI_PNE;

# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
  UminAmeliorePourCoupes = Pne->UminAmeliorePourCoupes;
	if ( UminAmeliorePourCoupes != NULL ) {
		UneVariableAEteFixee = NON_PNE;
    BorneMiseAJour = MODIF_BORNE_INF;
    for ( i = 0 ; i < NombreDeVariablesNonFixes ; i++ ) {
      Var = NumeroDesVariablesNonFixes[i];
      if ( UminAmeliorePourCoupes[Var] == VALEUR_NON_INITIALISEE ) continue;
      if ( BorneInfConnue[Var] == FIXE_AU_DEPART ) continue;
 	    if ( BorneInfConnue[Var] == NON_PNE ) continue;
			if ( ValeurDeBorneInf[Var] >= UminAmeliorePourCoupes[Var] ) continue;
			NouvelleValeur = UminAmeliorePourCoupes[Var];			
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  	  if ( *Faisabilite == NON_PNE ) {
		    # if TRACES == 1
			    printf("Pas de solution dans le presolve simplifie\n");
			  # endif									
		    return;
			}					  
	  }											 				
	}  
# endif

NbCycles = 0;
DebutDeCycle:
	
RefaireUnCycle = NON_PNE;

/* Forcing constraints et contraintes inactives */

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {

 	if ( ContrainteActivable[Cnt] == NON_PNE ) continue;			
  ForcingConstraint = PNE_DeterminerForcingConstraint( Pne, ProbingOuNodePresolve, Cnt, SensContrainte[Cnt],
	                                                     BminValide[Cnt], BmaxValide[Cnt], Bmin[Cnt], Bmax[Cnt], B[Cnt] );						
  if ( ForcingConstraint == OUI_PNE ) {
	  ContrainteActivable[Cnt] = NON_PNE;
 		continue;
 	}
 	if ( SensContrainte[Cnt] == '<' ) {
 		if ( BmaxValide[Cnt] == OUI_PNE ) {
      if ( Bmax[Cnt] <= B[Cnt] + MARGE_POUR_RELAXATION ) {
	 		  ContrainteActivable[Cnt] = NON_PNE;
  			continue;
		 	}
		}
	}
	else {
	  /* Contrainte d'egalite */
		if ( BminValide[Cnt] == OUI_PNE && BmaxValide[Cnt] == OUI_PNE ) {
      if ( fabs( Bmax[Cnt] - Bmin[Cnt] ) < MARGE_POUR_RELAXATION ) {
				if ( fabs( Bmax[Cnt] - B[Cnt] ) < MARGE_POUR_RELAXATION ) {
				  ContrainteActivable[Cnt] = NON_PNE;
  			  continue;
				}
      }
    }		
	}	
}
	 
/* Essayer aussi les contraintes a une seule variable pour fixer des variables avec Bmin Bmax non calculables cf le Node Presolve REF */

Limite = (int) ( POURCENT_LIMITE * NombreDeContraintes );
if ( Limite < SEUIL_LIMITE ) Limite = SEUIL_LIMITE;

for ( i = 0 ; i < NombreDeVariablesNonFixes ; i++ ) {

  Var = NumeroDesVariablesNonFixes[i];
	
	if ( Mode == PRESOLVE_SIMPLIFIE_POUR_NODE_PRESOLVE && TypeDeVariable[Var] != ENTIER ) {
    if ( CNbTerm[Var] > Limite ) {
		  continue;
		}
	}
  
	/*
	if ( Mode == PRESOLVE_SIMPLIFIE_POUR_NODE_PRESOLVE && TypeDeVariable[Var] != ENTIER ) {
    if ( YaUneSolutionEntiere == OUI_PNE ) {
      if ( EcartBorneInf > SeuilSurEcartBorneInf ) {
        if ( ProfondeurDuNoeud > 10 ) {
          if ( CNbTerm[Var] > Limite ) continue;
			  }
		  }
	  }
	}
	*/
	
  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;
			 
	CoutVar = L[Var];
	SigneCoeff = '?';

  UneVariableAEteFixee = NON_PNE; /* Car peut ne pas etre initialise si on ne passe pas dans le corps du while */
  ic = Cdeb[Var];
  while ( ic >= 0 ) {
    Ai = A[ic];
    if ( Ai == 0.0 ) goto NextIc;		
    Cnt = NumContrainte[ic];
    if ( ContrainteActivable[Cnt] == NON_PNE ) goto NextIc;
		
	  SensCnt = SensContrainte[Cnt];

		/* Pour la fixation sur critere */
	  /* On ne fait la fixation sur critere que si le cout de la variable est nul */
    if ( CoutVar == 0.0 && SigneCoeff != 'X' ) {				 
      if ( SensCnt == '=' ) SigneCoeff = 'X';
			else {
		    /* Si le coeff est trop petit on prefere ne rien conclure */
		    if ( fabs( Ai ) < 1.e-6 && 0 ) {
          SigneCoeff = 'X';
		    }			
		    if ( Ai > 0.0 ) {
          if ( SigneCoeff == '?' ) SigneCoeff = '+';
			    else if ( SigneCoeff == '-' ) SigneCoeff = 'X';				
			  }		  
		    else {
          if ( SigneCoeff == '?' ) SigneCoeff = '-';
			    else if ( SigneCoeff == '+' ) SigneCoeff = 'X';
			  }
		  }		
    }
		
	  BmnValide = BminValide[Cnt];
	  BmxValide = BmaxValide[Cnt];
		if ( BmnValide == NON_PNE && BmnValide == NON_PNE ) goto NextIc;

	  Bmn = Bmin[Cnt];
	  Bmx = Bmax[Cnt];
	  BCnt = B[Cnt];

		XsValide = NON_PNE;
		XiValide = NON_PNE;		
		Xs = ValeurDeBorneSup[Var];
		Xi = ValeurDeBorneInf[Var];
		Xs0 = Xs;
		Xi0 = Xi;
		
    UneVariableAEteFixee = NON_PNE;
    BorneMiseAJour = NON_PNE;
						
		VariableFixee = NON_PNE;
    if ( SensCnt == '=' ) {
		  /*VariableFixee = NON_PNE;*/ /* 29/9/2016: Deplace avant le test car teste plus loin et peut ne pas etre initialise */
			/* Est inutile sur Var est bornee des 2 cotes */
			if ( TypeDeBorne[Var] != VARIABLE_BORNEE_DES_DEUX_COTES ) {
        PNE_PresolveSimplifieContrainteDEgaliteAUneSeuleVariable( Pne, Cnt, BCnt, &XiValide, &Xi , &XsValide, &Xs, &VariableFixee );					
        if ( VariableFixee == OUI_PNE ) goto SyntheseXiXs;
			}					
	    /* On regarde le min et le max */
	    if ( BmnValide == OUI_PNE ) {
		    BminNew = Bmn;
        if ( Ai > 0.0 ) BminNew -= Ai * Xi0; /* On avait pris le min */
        else BminNew -= Ai * Xs0; /* On avait pris le max */		
		    S = BCnt - BminNew;				
		    if ( Ai > 0 ) { Xs = S / Ai; XsValide = OUI_PNE; }
	    	else { Xi = -S / fabs( Ai ); XiValide = OUI_PNE; }		
	    }
	    if ( BmxValide == OUI_PNE ) {	
        BmaxNew = Bmx;
		    if ( Ai > 0.0 ) BmaxNew -= Ai * Xs0; /* On avait pris le max */
        else BmaxNew -= Ai * Xi0; /* On avait pris le min */
        S = BCnt - BmaxNew;				
		    if ( Ai > 0 ) { Xi = S / Ai; XiValide = OUI_PNE; }	
		    else { Xs = -S / fabs( Ai ); XsValide = OUI_PNE; }				
	    }	
    }
    else { /* SensContrainte est '<' */
      /* On peut calculer un majorant */			
	    if ( BmnValide == OUI_PNE ) {						  
		    BminNew = Bmn;								
        if ( Ai > 0.0 ) BminNew -= Ai * Xi0; /* On avait pris le min */				
        else BminNew -= Ai * Xs0; /* On avait pris le max */				
		    S = BCnt - BminNew;				
		    if ( Ai > 0 ) { Xs = S / Ai; XsValide = OUI_PNE; }
		    else { Xi = -S / fabs( Ai ); XiValide = OUI_PNE; }					
	    }			 
    }		
		/* Que si Xi ou Xs sont valides et si une des 2 bornes est plus petite ou plus grande */
		SyntheseXiXs:		
		if ( XiValide != OUI_PNE && XsValide != OUI_PNE ) goto NextIc;

		if ( VariableFixee != OUI_PNE && TypeDeVariable[Var] != ENTIER ) {
		
	    if ( NbTerm[Cnt] > NBTERMES_POUR_APPLICATION_DE_LA_MARGE ) MargeSurBorne = GRANDE_MARGE_SUR_BORNES;
			else MargeSurBorne = PETITE_MARGE_SUR_BORNES;
			
		  /* Ne pas empecher la fixation des variables */
		  if (  XiValide == OUI_PNE && XsValide == OUI_PNE ) {
        if ( fabs( Xs - Xi ) > ECART_MIN_XI_XS_POUR_MARGE_SUR_BORNES ) {
		      Xi -= MargeSurBorne;
		      Xs += MargeSurBorne;
				}
			}
			else {		
		    if ( XiValide == OUI_PNE ) Xi -= MargeSurBorne;
		    if ( XsValide == OUI_PNE ) Xs += MargeSurBorne;
			}
		}
		
		if ( Xi <= Xi0 && Xs >= Xs0 ) goto NextIc; /* Attention s'il s'agit d'une variable fixee a une valeur infaisable */		
    PNE_ModifierLaBorneDUneVariable( Pne, Var, SensCnt, XsValide, Xs, XiValide, Xi, &NouvelleValeur,
		                                 &BorneMiseAJour, &UneVariableAEteFixee, Faisabilite );
    if ( *Faisabilite == NON_PNE ) {
		  # if TRACES == 1
			  printf("Pas de solution dans le presolve simplifie\n");
			# endif			
		  return;
		}		
	  /* Si la variable a ete fixee ou une borne mise a jour on modifie les bornes des contraintes */																	 
	  if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE  ) {		  
		  # if TRACES == 1
			  printf("Variable %d  NouvelleValeur de borne %e\n",Var,NouvelleValeur);
			# endif							
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  	  if ( *Faisabilite == NON_PNE ) {
		    # if TRACES == 1
			    printf("Pas de solution dans le presolve simplifie\n");
			  # endif				
		    return;
			}			
		  RefaireUnCycle = OUI_PNE;
			if ( UneVariableAEteFixee != NON_PNE ) {
			  break;
			}      			
	  }											 		
	  NextIc:
    ic = Csui[ic];
  }

	if ( UneVariableAEteFixee != NON_PNE ) continue;

	/* Fixation sur critere */
	if ( CoutVar != 0.0 ) continue;
		
	if ( SigneCoeff == 'X' ) continue;	

  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;
			 
	NouvelleValeur = -1;
	BorneMiseAJour = NON_PNE;
  UneVariableAEteFixee = NON_PNE;				 

	if ( SigneCoeff == '+' ) {
	  if ( CoutVar >= 0.0 ) {
		  /* On fixe a Umin */
			if ( BorneInfConnue[Var] == OUI_PNE ) {				
		    # if TRACES == 1
			    printf("Fixation de la variable %d a %e\n",Var,ValeurDeBorneInf[Var]);
	      # endif								
        NouvelleValeur = ValeurDeBorneInf[Var];
				UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				
			}
		}  
	}	
	else if ( SigneCoeff == '-' ) {
	  if ( CoutVar <= 0.0 ) {
			/* On fixe a Umax */
			if ( BorneSupConnue[Var] == OUI_PNE ) {				
		    # if TRACES == 1
			    printf("Fixation de la variable %d a %e\n",Var,ValeurDeBorneSup[Var]);
	      # endif				
        NouvelleValeur = ValeurDeBorneSup[Var];
				UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;				      									
			}
		}
	}
	else if ( SigneCoeff == '?' ) {
    /* La variable n'apparait pas dans les contraintes */
	  if ( CoutVar >= 0.0 ) {
		  /* On fixe a Umin */
			if ( BorneInfConnue[Var] == OUI_PNE ) {
		    # if TRACES == 1
			    printf("Fixation de la variable %d a %e\n",Var,ValeurDeBorneInf[Var]);
	      # endif
        NouvelleValeur = ValeurDeBorneInf[Var];
				UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				       
			}
		}
		else {
			if ( BorneSupConnue[Var] == OUI_PNE ) {
		    # if TRACES == 1
			    printf("Fixation de la variable %d a %e\n",Var,ValeurDeBorneSup[Var]);
	      # endif
        NouvelleValeur = ValeurDeBorneSup[Var];
			  UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;				  
			}
		}
  }
	if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE ) {
		# if TRACES == 1
			printf("Variable %d  NouvelleValeur de borne %e\n",Var,NouvelleValeur);
		# endif			
    PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
    if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  	if ( *Faisabilite == NON_PNE ) {
		  # if TRACES == 1
			  printf("Pas de solution dans le presolve simplifie\n");
			# endif			
		  return;
		} 		
	  RefaireUnCycle = OUI_PNE;	
  }			
	
}

if ( Mode == PRESOLVE_SIMPLIFIE_POUR_NODE_PRESOLVE ) goto FIN_FORCING;

/* Forcing constraints et contraintes inactives */

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
 	if ( ContrainteActivable[Cnt] == NON_PNE ) continue;
  ForcingConstraint = PNE_DeterminerForcingConstraint( Pne, ProbingOuNodePresolve, Cnt, SensContrainte[Cnt],
	                                                     BminValide[Cnt], BmaxValide[Cnt], Bmin[Cnt], Bmax[Cnt], B[Cnt] );						
  if ( ForcingConstraint == OUI_PNE ) {
	  ContrainteActivable[Cnt] = NON_PNE;
 	  RefaireUnCycle = OUI_PNE;
 		continue;
 	}
 	if ( SensContrainte[Cnt] == '<' ) {
 		if ( BmaxValide[Cnt] == OUI_PNE ) {
      if ( Bmax[Cnt] <= B[Cnt] + MARGE_POUR_RELAXATION ) {
	 		  ContrainteActivable[Cnt] = NON_PNE;
 		    RefaireUnCycle = OUI_PNE;
  			continue;
		 	}
		}
	}
	else {
	  /* Contrainte d'egalite */
		if ( BminValide[Cnt] == OUI_PNE && BmaxValide[Cnt] == OUI_PNE ) {
      if ( fabs( Bmax[Cnt] - Bmin[Cnt] ) < MARGE_POUR_RELAXATION ) {
				if ( fabs( Bmax[Cnt] - B[Cnt] ) < MARGE_POUR_RELAXATION ) {
				  ContrainteActivable[Cnt] = NON_PNE;
 		      RefaireUnCycle = OUI_PNE;
  			  continue;
				}
      }
    }		
	}		
}

FIN_FORCING:

/* Transfere a la fin pour ne pas reboucler */
/*
if ( Mode == PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE && 0 ) { 
  PNE_TesterLaDominanceDesVariablesEntieres( Pne, Faisabilite, &RefaireUnCycle );
}
*/
  
/* Test */
if ( Mode == PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE && 0 ) { 
  /*printf("Test PNE_PresolveSimplifieVariableProbin  en cours de mise au point il suffit de ne pas l'appeler si pas bon\n");*/
  /*PNE_PresolveSimplifieVariableProbing( Pne, Faisabilite, &RefaireUnCycle );*/
}
/* Fin test */

/* Test */
# if PRISE_EN_COMPTE_DES_CONTRAINTES_DE_BORNES_VARIABLES == OUI_PNE
  # if CONSTRUIRE_BORNES_VARIABLES == OUI_PNE	
    if ( Mode == PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE || Pne->YaUneSolutionEntiere == NON_PNE ||
		     Bb->NoeudEnExamen->ProfondeurDuNoeud < 10 || Bb->NoeudEnExamen->ProfondeurDuNoeud > 100 ) {		
      PNE_PresolveSimplifieContraintesDeBornesVariables( Pne, Faisabilite, &RefaireUnCycle );
  	  if ( *Faisabilite == NON_PNE ) {		
		    return;
      }
	  }		
  # endif
# endif
/* Fin test */

PNE_PresolveSimplifieColonnesColineaires( Pne, ContrainteActivable, Mode );

if ( Mode == PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE ) {
  PNE_ComparerLesContraintes( Pne, ContrainteActivable, Mode, &RefaireUnCycle );
  if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  if ( *Faisabilite == NON_PNE ) {
		# if TRACES == 1
		  printf("Pas de solution dans le presolve simplifie\n");
		# endif				
	  return;
	}	
}

# if TENIR_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTES_POUR_LE_BRANCHING == OUI_PNE
  PNE_PresolveSimplifieGroupesDeVariablesEquivalentes( Pne, &RefaireUnCycle );
  if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) *Faisabilite = NON_PNE;
  if ( *Faisabilite == NON_PNE ) {
		# if TRACES == 1
		  printf("Pas de solution dans le presolve simplifie\n");
		# endif				
	  return;
	}		
# endif

if ( RefaireUnCycle == OUI_PNE ) { 
	NbCycles++;
  if ( NbCycles < MxCycles ) {			
    # if TRACES == 1
	    printf("-> Rebouclage dans le presolve simplifie\n");
	  # endif
	  goto DebutDeCycle;
	}
}

if ( Mode == PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE || Bb->NoeudEnExamen == Bb->NoeudRacine ) {
  PNE_TesterLaDominanceDesVariablesEntieres( Pne, Faisabilite, ContrainteActivable, &RefaireUnCycle );
  if ( *Faisabilite == NON_PNE ) {		
		return;
  }
  if ( RefaireUnCycle == OUI_PNE ) {	
	  NbCycles++;
    if ( NbCycles < MxCycles ) {
      # if TRACES == 1
	      printf("-> Rebouclage dans le presolve simplifie\n");
	    # endif
	    goto DebutDeCycle;
	  }			
  }	
}

if ( Mode == PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE ) { 
  PNE_PresolveSimplifieExaminerLesCoupes( Pne );
}

return;
}

/*-------------------------------------------------------------------------------*/
# if TENIR_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTES_POUR_LE_BRANCHING == OUI_PNE
void PNE_PresolveSimplifieGroupesDeVariablesEquivalentes( PROBLEME_PNE * Pne, char * RefaireUnCycle )
{
int i; int NombreDeVariablesNonFixes; int * NumeroDesVariablesNonFixes; int * TypeDeVariable;
double * ValeurDeBorneSup; double * ValeurDeBorneInf; char * BorneSupConnue; char * BorneInfConnue; 
int Var; int * NumeroDeGroupeDeVariablesEquivalentes; GROUPE * G; GROUPE ** Groupe; int j;
int Var1; double NouvelleValeur; char BorneMiseAJour; char UneVariableAEteFixee;

return;

NombreDeVariablesNonFixes = Pne->NombreDeVariablesNonFixes;
NumeroDesVariablesNonFixes = Pne->NumeroDesVariablesNonFixes;

TypeDeVariable = Pne->TypeDeVariableTrav;

BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;

NumeroDeGroupeDeVariablesEquivalentes = Pne->NumeroDeGroupeDeVariablesEquivalentes;
Groupe = Pne->Groupe;

for ( j = 0 ; j < NombreDeVariablesNonFixes ; j++ ) {
  Var = NumeroDesVariablesNonFixes[j];
	if ( NumeroDeGroupeDeVariablesEquivalentes[Var] < 0 ) continue;
  if ( BorneInfConnue[Var] == FIXATION_SUR_BORNE_INF ) {
    /* On fixe a 0 toutes les variable a droite */
		G = Groupe[NumeroDeGroupeDeVariablesEquivalentes[Var]];
    for ( i = 0 ; i < G->NombreDeVariablesDuGroupe ; i++ ) {
      if ( G->VariablesDuGroupe[i] == Var ) {
			  i++;
        for ( ; i < G->NombreDeVariablesDuGroupe ; i++ ) {
          Var1 = G->VariablesDuGroupe[i];	
          if ( BorneInfConnue[Var1] == FIXE_AU_DEPART || BorneInfConnue[Var1] == FIXATION_SUR_BORNE_INF ||
	             BorneInfConnue[Var1] == FIXATION_SUR_BORNE_SUP || BorneInfConnue[Var1] == FIXATION_A_UNE_VALEUR ) continue;					
     
 	        BorneMiseAJour = NON_PNE;
          NouvelleValeur = ValeurDeBorneInf[Var1];
				  UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				       
			    *RefaireUnCycle = OUI_PNE;
					printf("*************** on fixe %d a %e\n",Var1,NouvelleValeur);
					
          PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );					 
				}
				break;
			}
	  }		
	}
	else if ( BorneInfConnue[Var] == FIXATION_SUR_BORNE_SUP ) {
    /* On fixe a 1 toutes les variable a gauche */
		G = Groupe[NumeroDeGroupeDeVariablesEquivalentes[Var]];
    for ( i = 0 ; i < G->NombreDeVariablesDuGroupe ; i++ ) {
      if ( G->VariablesDuGroupe[i] == Var ) break;
      Var1 = G->VariablesDuGroupe[i];	
      if ( BorneInfConnue[Var1] == FIXE_AU_DEPART || BorneInfConnue[Var1] == FIXATION_SUR_BORNE_INF ||
	         BorneInfConnue[Var1] == FIXATION_SUR_BORNE_SUP || BorneInfConnue[Var1] == FIXATION_A_UNE_VALEUR ) continue;					
     
 	    BorneMiseAJour = NON_PNE;
      NouvelleValeur = ValeurDeBorneSup[Var1];
			UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;				       
			*RefaireUnCycle = OUI_PNE;
			printf("*************** on fixe %d a %e\n",Var1,NouvelleValeur);
					
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );			
		
    }
	}
}
			 
return;
}
# endif
/*-------------------------------------------------------------------------------*/

void PNE_PresolveSimplifieContrainteDEgaliteAUneSeuleVariable( PROBLEME_PNE * Pne, int Cnt, double BCnt,
                                                               char * XiValide, double * Xi ,
																											         char * XsValide, double * Xs, char * VariableFixee )														
{
int il; int ilMax; int Var; int NbVarLibres; double B; double Coeff; double * A; int * Nuvar; double * ValeurDeBorneInf;
char * BorneInfConnue; char BrnInfConnue; 

*VariableFixee = NON_PNE;

if ( Pne->NbTermTrav[Cnt] > 100 ) return;

Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;

BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;

B = BCnt;
NbVarLibres = 0;
Coeff = 1;
il = Pne->MdebTrav[Cnt];
ilMax = il + Pne->NbTermTrav[Cnt];
while ( il < ilMax ) {
  Var = Nuvar[il];
  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {
    B -= A[il] * ValeurDeBorneInf[Var];
		goto NextIl;			 
	}
  NbVarLibres++;
	if ( NbVarLibres > 1 ) break;
  Coeff = A[il];
  NextIl:
  il++;
}
if ( NbVarLibres == 1 ) {
  /* On peut fixer la variable */
  *Xi = B / Coeff;
	*Xs = *Xi;			
  *XiValide = OUI_PNE;
	*XsValide = OUI_PNE;  
  *VariableFixee = OUI_PNE;	
}

return;
}

/*---------------------------------------------------------------------------------*/
/* On examine le pool des coupes pour voir si on peut fixer des variables entieres */

void PNE_PresolveSimplifieExaminerLesCoupes( PROBLEME_PNE * Pne )
{
int NumeroDeCoupe; double * Coefficient; int * IndiceDeLaVariable; int NbTermes;
double Smin; char * BorneSupConnue; char * BorneInfConnue; char YaDesVariablesEntieres;
double * ValeurDeBorneInf; double * ValeurDeBorneSup; int il; int Var; double a;
char MinValide; int * TypeDeVariable; char BrnInfConnue; char BrnSupConnue; int Faisabilite;
double NouvelleValeur; double S; double SminNew; double b; double Xi; double Xs; double Xi0;
double Xs0; char UneVariableAEteFixee; char BorneMiseAJour; int ilMax; BB * Bb; double MargeSurBorne;

# if NOUVEAU_STOCKAGE_COUPES == NON_PNE
  COUPE ** Coupe;
# endif

# if FAIRE_DU_PRESOLVE_SUR_LES_COUPES == NON_PNE
  return;
# endif

Bb = (BB *) Pne->ProblemeBbDuSolveur;
if ( Bb == NULL ) return;

Faisabilite = OUI_PNE;

TypeDeVariable = Pne->TypeDeVariableTrav;
BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;

# if NOUVEAU_STOCKAGE_COUPES == OUI_PNE
  if ( Bb->Pool == NULL ) return;
  for ( NumeroDeCoupe = 0 ; NumeroDeCoupe < Bb->Pool->NombreDeCoupes ; NumeroDeCoupe++ ) {
    if ( Bb->Pool->PresenceDeVariableEntiereDansLaCoupe[NumeroDeCoupe] == NON_PNE ) continue;
# else 
  Coupe = Bb->NoeudRacine->CoupesGenereesAuNoeud;  
  for ( NumeroDeCoupe = 0 ; NumeroDeCoupe < Bb->NoeudRacine->NombreDeCoupesGenereesAuNoeud ; NumeroDeCoupe++ ) {
    if ( Coupe[NumeroDeCoupe]->PresenceDeVariableEntiereDansLaCoupe == NON_PNE ) continue;
# endif

  /* Recalcul du second membre de la coupe */
  # if NOUVEAU_STOCKAGE_COUPES == OUI_PNE
    Coefficient = Bb->Pool->Coefficient;
    IndiceDeLaVariable = Bb->Pool->IndiceDeLaVariable;
		il = Bb->Pool->IndexDebut[NumeroDeCoupe];
		NbTermes = Bb->Pool->NombreDeTermes[NumeroDeCoupe];
		ilMax = il + NbTermes;
	# else 
    Coefficient = Coupe[NumeroDeCoupe]->Coefficient;
    IndiceDeLaVariable = Coupe[NumeroDeCoupe]->IndiceDeLaVariable;
		il = 0;
		NbTermes = Coupe[NumeroDeCoupe]->NombreDeTermes;
		ilMax = il + NbTermes;
	# endif
	
  YaDesVariablesEntieres = NON_PNE;
  MinValide = OUI_PNE;
  Smin = 0.0;

  while ( il < ilMax ) {	
    Var = IndiceDeLaVariable[il];		
	  a = Coefficient[il];
	  if ( a == 0.0 ) goto NextIl_0;
    BrnInfConnue = BorneInfConnue[Var];
	  BrnSupConnue = BorneSupConnue[Var];		
    if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	       BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {
	    Smin += a * ValeurDeBorneInf[Var];
			goto NextIl_0;
	  }
		if (  TypeDeVariable[Var] == ENTIER ) YaDesVariablesEntieres = OUI_PNE;
	  if ( a > 0.0 ) {
		  /* Calcul de min */
      if ( BrnInfConnue == OUI_PNE ) Smin += a * ValeurDeBorneInf[Var];				       								
		  else { MinValide = NON_PNE; goto ConclusionValiditeBminBmax; }      
	  }
	  else {
		  /* Calcul de min */
      if ( BrnSupConnue == OUI_PNE ) Smin += a * ValeurDeBorneSup[Var];			
		  else { MinValide = NON_PNE; goto ConclusionValiditeBminBmax; }      				
	  }
    NextIl_0:
		il++;
  }  
  ConclusionValiditeBminBmax:
	if ( MinValide == NON_PNE ) continue;
  if ( YaDesVariablesEntieres == NON_PNE ) continue; 
	
  # if NOUVEAU_STOCKAGE_COUPES == OUI_PNE
	  b = Bb->Pool->SecondMembre[NumeroDeCoupe];
		il = Bb->Pool->IndexDebut[NumeroDeCoupe];
		ilMax = il + Bb->Pool->NombreDeTermes[NumeroDeCoupe];		
	# else 
	  b = Coupe[NumeroDeCoupe]->SecondMembre;
		il = 0;
		ilMax = il + Coupe[NumeroDeCoupe]->NombreDeTermes;		
	# endif

  while ( il < ilMax ) {	
    Var = IndiceDeLaVariable[il];		
	  a = Coefficient[il];
	  if ( a == 0.0 ) goto NextIl_1;
    BrnInfConnue = BorneInfConnue[Var];
	  BrnSupConnue = BorneSupConnue[Var];		
    if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	       BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIl_1;

		Xs = ValeurDeBorneSup[Var];
		Xi = ValeurDeBorneInf[Var];
		Xs0 = Xs;
		Xi0 = Xi;
		
    UneVariableAEteFixee = NON_PNE;
    BorneMiseAJour = NON_PNE;
						
    /* On peut calculer un majorant */			
		SminNew = Smin;								
    if ( a > 0.0 ) SminNew -= a * Xi0; /* On avait pris le min */				
    else SminNew -= a * Xs0; /* On avait pris le max */				
		S = b - SminNew;
		if ( a > 0 ) Xs = S / a;  
		else Xi = -S / fabs( a );

		/* Ne pas empecher la fixation des variables */
    if ( fabs( Xs - Xi ) > ECART_MIN_XI_XS_POUR_MARGE_SUR_BORNES && TypeDeVariable[Var] != ENTIER ) {
      /*
		  if ( NbTermes > NBTERMES_POUR_APPLICATION_DE_LA_MARGE ) MargeSurBorne = GRANDE_MARGE_SUR_BORNES;
			else MargeSurBorne = PETITE_MARGE_SUR_BORNES;
			*/
		  MargeSurBorne = GRANDE_MARGE_SUR_BORNES;
		
		  Xi -= MargeSurBorne;
		}		
    		
		if ( Xi <= Xi0 && Xs >= Xs0 ) goto NextIl_1;
		
    PNE_ModifierLaBorneDUneVariable( Pne, Var, '<', OUI_PNE, Xs, OUI_PNE, Xi, &NouvelleValeur,
		                                 &BorneMiseAJour, &UneVariableAEteFixee, &Faisabilite );
    if ( Faisabilite == NON_PNE ) {
		  # if TRACES == 1
			  printf("Pas de solution dans le presolve simplifie\n");
			# endif			
		  return;
		}		
	  /* Si la variable a ete fixee ou une borne mise a jour on modifie les bornes des contraintes */																	 
	  if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE  ) {		  
		  # if TRACES == 1
			  printf("Variable %d  NouvelleValeur de borne %e\n",Var,NouvelleValeur);
			# endif							
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) Faisabilite = NON_PNE;
  	  if ( Faisabilite == NON_PNE ) {
		    # if TRACES == 1
			    printf("Pas de solution dans le presolve simplifie\n");
			  # endif				
		    return;
			}
		}
    NextIl_1:
    il++;		
	}	
}

return;
}

