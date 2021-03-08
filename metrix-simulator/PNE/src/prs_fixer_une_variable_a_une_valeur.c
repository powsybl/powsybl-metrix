/***********************************************************************

   FONCTION: Init des indicateurs de fixation d'une variable.					 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_define.h"  
		
# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "prs_memoire.h"
# endif

# define TRACES_DUAL 1

# define MIN 1
# define MAX 2

void PRS_DualCalculDesSommesMinOuMax( int * , int * , int * , double * , double * , double * , 
									  double * , char * , char * , int , int , char , double * , 
									  char * );
void PRS_AmeliorerLesBornesDesVariablesDualesPourUneVariableFixee( PROBLEME_PNE * , PRESOLVE * , int , char  );

/*----------------------------------------------------------------------------*/

void PRS_FixerUneVariableAUneValeur( PRESOLVE * Presolve, int Var, double X )   
{
PROBLEME_PNE * Pne; char TypeBrn; char SigneCoutReduit;
Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

/* Attention, il faut en plus savoir si la fixation est borne sup ou inf et pas simplement 
   une substitution */
goto CaSertARien; /* Apres tests, ca sert a rien */  
TypeBrn = Presolve->TypeDeBornePourPresolve[Var];
if ( X == Presolve->BorneInfPourPresolve[Var] ) {
  if ( TypeBrn == VARIABLE_BORNEE_INFERIEUREMENT || TypeBrn == VARIABLE_BORNEE_DES_DEUX_COTES ) {
    /* Fixation sur borne inf => cout reduit >= */
    SigneCoutReduit = '>';
    PRS_AmeliorerLesBornesDesVariablesDualesPourUneVariableFixee( Pne, Presolve, Var, SigneCoutReduit );
  }
}
else if ( X == Presolve->BorneSupPourPresolve[Var] ) {
  if ( TypeBrn == VARIABLE_BORNEE_SUPERIEUREMENT || TypeBrn == VARIABLE_BORNEE_DES_DEUX_COTES ) {
    /* Fixation sur borne sup => cout reduit <= */
    SigneCoutReduit = '<';
    PRS_AmeliorerLesBornesDesVariablesDualesPourUneVariableFixee( Pne, Presolve, Var, SigneCoutReduit );
  }
}
CaSertARien:

Pne->TypeDeVariableTrav[Var] = REEL;
Presolve->ValeurDeXPourPresolve[Var] = X;
/* Car on se sert de ces bornes dans le postsolve 
Presolve->BorneInfPourPresolve[Var] = X;
Presolve->BorneSupPourPresolve[Var] = X;							
*/
Presolve->TypeDeBornePourPresolve[Var] = VARIABLE_FIXE;
Presolve->TypeDeValeurDeBorneInf[Var] = VALEUR_IMPLICITE;
Presolve->TypeDeValeurDeBorneSup[Var] = VALEUR_IMPLICITE;

/* On desactive les eventuelles contraintes de bornes variables de cette variable */
PRS_DesactiverContraintesDeBorneVariable( Pne, Var, -1 );

return;
}

/*----------------------------------------------------------------------------*/
/* Si la variable duale a ete fixee on connait le signe de son cout reduit */

void PRS_AmeliorerLesBornesDesVariablesDualesPourUneVariableFixee( PROBLEME_PNE * Pne,
																   PRESOLVE * Presolve, 
																   int Var, 
																   char SigneCoutReduit ) 
{
int ic; double C; double CoeffDeCntTest; int Cnt; int * Cdeb; int * Csui; int * NumContrainte; 
double * A; double * LambdaMin; double * LambdaMax; double * Lambda; char * ConnaissanceDeLambda; 
char * ContrainteInactive; double Borne; char TypeDeSomme; double Somme; char SommeValide;

C = Pne->LTrav[Var];
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
A = Pne->ATrav;

Lambda = Presolve->Lambda;
LambdaMin = Presolve->LambdaMin;
LambdaMax = Presolve->LambdaMax;
ConnaissanceDeLambda = Presolve->ConnaissanceDeLambda;
ContrainteInactive = Presolve->ContrainteInactive;
 
CoeffDeCntTest = 1.;

ic = Cdeb[Var];
while ( ic >= 0 ) {
  if ( A[ic] == 0.0 ) goto NextIc;
  Cnt = NumContrainte[ic];	
  if ( ContrainteInactive[Cnt] == OUI_PNE ) goto NextIc;
  if ( ConnaissanceDeLambda[Cnt] == LAMBDA_CONNU ) goto NextIc;
  /* On essaie d'ameliorer la variable duale de Cnt */
  /* Si le signe du cout reduit est > on doit calculer des min de ua car ua <= c */
  /* Si le signe du cout reduit est < on doit calculer des max de ua car ua >= c */
  CoeffDeCntTest = A[ic];
  if ( SigneCoutReduit == '>' ) {
    TypeDeSomme = MIN;
    PRS_DualCalculDesSommesMinOuMax( Cdeb, Csui, NumContrainte, A, LambdaMin, LambdaMax, Lambda, 
                                     ConnaissanceDeLambda, ContrainteInactive, Var, Cnt,
								     TypeDeSomme, &Somme, &SommeValide );
    if ( SommeValide == OUI_PNE ) {		
      /* On doit avoir c-uA >= 0 <=> uA <= c */
	  Borne = ( C - Somme ) / CoeffDeCntTest;
      if ( CoeffDeCntTest > 0.0 ) {		
		if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
		  if ( Borne < LambdaMax[Cnt] ) {
            # if TRACES_DUAL == 1		
			        printf("Contrainte %d LambdaMax: %e -> %e\n",Cnt,LambdaMax[Cnt],Borne);
            # endif
			LambdaMax[Cnt] = Borne;
		  }
		}
		else {
          # if TRACES_DUAL == 1		
			      printf("Contrainte %d LambdaMax: %e -> %e\n",Cnt,LambdaMax[Cnt],Borne);
          # endif
          LambdaMax[Cnt] = Borne;
          if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
		  else ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;					
		}	
	  }
	  else {
		if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
		  if ( Borne > LambdaMin[Cnt] ) {
            # if TRACES_DUAL == 1		
			        printf("Contrainte %d LambdaMin: %e -> %e\n",Cnt,LambdaMin[Cnt],Borne);
            # endif
			LambdaMin[Cnt] = Borne;
		  }
		}
		else {
          # if TRACES_DUAL == 1		
			      printf("Contrainte %d LambdaMin: %e -> %e\n",Cnt,LambdaMin[Cnt],Borne);
          # endif
          LambdaMin[Cnt] = Borne;
          if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
		  else ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_CONNU;		
		}		
	  }
	}  
  }
  else {
	/* SigneCoutReduit < */
    TypeDeSomme = MAX;
    PRS_DualCalculDesSommesMinOuMax( Cdeb, Csui, NumContrainte, A, LambdaMin, LambdaMax, Lambda, 
                                     ConnaissanceDeLambda, ContrainteInactive, Var, Cnt,
								     TypeDeSomme, &Somme, &SommeValide );

    if ( SommeValide == OUI_PNE ) {				
      /* On doit avoir c-uA <= 0 <=> uA >= c */
	  Borne = ( C - Somme ) / CoeffDeCntTest;
      if ( CoeffDeCntTest > 0.0 ) {		
		if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
		  if ( Borne > LambdaMin[Cnt] ) {
            # if TRACES_DUAL == 1		
			        printf("Contrainte %d LambdaMin: %e -> %e\n",Cnt,LambdaMin[Cnt],Borne);
            # endif
			LambdaMin[Cnt] = Borne;
		  }
		}
		else {
          # if TRACES_DUAL == 1		
			      printf("Contrainte %d LambdaMin: %e -> %e\n",Cnt,LambdaMin[Cnt],Borne);
          # endif
          LambdaMin[Cnt] = Borne;
          if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
		  else ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_CONNU;					
		}	
	  }
	  else {
		if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
		  if ( Borne < LambdaMax[Cnt] ) {
            # if TRACES_DUAL == 1		
			        printf("Contrainte %d LambdaMax: %e -> %e\n",Cnt,LambdaMax[Cnt],Borne);
            # endif
			LambdaMax[Cnt] = Borne;
		  }
		}
		else {
          # if TRACES_DUAL == 1		
			      printf("Contrainte %d LambdaMax: %e -> %e\n",Cnt,LambdaMax[Cnt],Borne);
          # endif
          LambdaMax[Cnt] = Borne;
          if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
		  else ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;		
		}		
	  }
	}
  }
  if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
    if ( fabs( LambdaMax[Cnt] - LambdaMin[Cnt] ) < 1.e-8 ) {
	  Borne = 0.5 * ( LambdaMax[Cnt] + LambdaMin[Cnt] );
      # if TRACES_DUAL == 1		
	      printf("Contrainte %d onfixe Lambda a %e\n",Cnt,Borne);
      # endif		
      Lambda[Cnt] = Borne;
      LambdaMin[Cnt] = Borne;
      LambdaMax[Cnt] = Borne;
      ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;	  
	}
  }
  NextIc:
  ic = Csui[ic];
}

return;
}

/*----------------------------------------------------------------------------*/

void PRS_DualCalculDesSommesMinOuMax( int * Cdeb, int * Csui, int * NumContrainte,
									  double * A, double * LambdaMin, double * LambdaMax,
									  double * Lambda, char * ConnaissanceDeLambda,
									  char * ContrainteInactive, int Var, int Cnt0,
								      char TypeDeSomme, double * Somme, char * SommeValide ) 
{
int ic; int Cnt; char Valide; double S;

Valide = OUI_PNE;
S = 0;
ic = Cdeb[Var];
while ( ic >= 0 ) {
  if ( A[ic] == 0.0 ) goto NextIc;
  Cnt = NumContrainte[ic];	
  if ( Cnt == Cnt0 ) goto NextIc;
  if ( ContrainteInactive[Cnt] == OUI_PNE ) goto NextIc;
  if ( ConnaissanceDeLambda[Cnt] == LAMBDA_CONNU ) S += A[ic] * Lambda[Cnt]; 
  else {
	if ( TypeDeSomme == MAX ) {
      if ( A[ic] > 0.0 ) {
	    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
		  S += A[ic] * LambdaMax[Cnt];
		}
		else { Valide = NON_PNE; break; }
	  }
	  else {
        /* A[ic] < 0 */		
		if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
		  S += A[ic] * LambdaMin[Cnt];
		}
		else { Valide = NON_PNE; break; }
	  }
	} 
    else {
	  /* Calcul de Min */
      if ( A[ic] > 0.0 ) {
		if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
		  S += A[ic] * LambdaMin[Cnt];
		}
		else { Valide = NON_PNE; break; }
	  }
	  else {
        /* A[ic] < 0 */				  
	    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
		  S += A[ic] * LambdaMax[Cnt];
	    }
	    else { Valide = NON_PNE; break; }
	  }
	}
  }
  NextIc:	
  ic = Csui[ic];
}

*Somme = S;
*SommeValide = Valide;

return;
}












