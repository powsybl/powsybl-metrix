/***********************************************************************

   FONCTION: On etudie les domaines de variation des variables entieres
	           dans le but d'en fixer.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
  
# include "bb_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define VERBOSE_FIXATIONS_DE_VARIABLES NON_PNE
# define ZERO 1.e-10

# define TRACES_CONTRAINTES_DE_BORNE_VARIABLE NON_PNE
  
# define EPSILON_FORCING_CONSTRAINT 1.e-8 /*1.e-7*/
# define FORCING_BMIN 1
# define FORCING_BMAX 2
# define PAS_DE_FORCING 128

/*----------------------------------------------------------------------------*/

void PNE_CalculMinEtMaxDesContraintes( PROBLEME_PNE * Pne, int * Faisabilite )
{
double A; int il; int ilMax; int Var; char MinValide; char MaxValide; int * MdebTrav;
int * NbTermTrav; int * NuvarTrav; double * ATrav; double Smin; int Nb;
double Smax; int NombreDeContraintesTrav; double * Bmin; double * Bmax; char * BminValide;
char * BmaxValide; double * ValeurDeBorneInf; double * ValeurDeBorneSup; char * BorneSupConnue;
char * BorneInfConnue;  int Cnt; char BrnInfConnue; char BrnSupConnue; double * B;
char * SensContrainte; int ilDeb; 

*Faisabilite = OUI_PNE;
BminValide = Pne->ProbingOuNodePresolve->BminValide;
Bmin = Pne->ProbingOuNodePresolve->Bmin;
BmaxValide = Pne->ProbingOuNodePresolve->BmaxValide;
Bmax = Pne->ProbingOuNodePresolve->Bmax;
BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;

Smin = 0.0;
Smax = 0.0;
MinValide = OUI_PNE;
MaxValide = OUI_PNE;
													
NombreDeContraintesTrav = Pne->NombreDeContraintesTrav;
MdebTrav = Pne->MdebTrav;
NbTermTrav = Pne->NbTermTrav;
ATrav = Pne->ATrav;
NuvarTrav = Pne->NuvarTrav;
B = Pne->BTrav;
SensContrainte = Pne->SensContrainteTrav;

for ( Cnt = 0 ; Cnt < NombreDeContraintesTrav ; Cnt++ ) {	
  MinValide = OUI_PNE;
  MaxValide = OUI_PNE;	
  Smin = 0.0;
  Smax = 0.0;
	Nb = 0;
  ilDeb = MdebTrav[Cnt];
  ilMax = ilDeb + NbTermTrav[Cnt];
	il = ilDeb;
  while ( il < ilMax ) {
    Var = NuvarTrav[il];		
	  A = ATrav[il];
	  if ( A == 0.0 ) goto NextElement;
    BrnInfConnue = BorneInfConnue[Var];
	  BrnSupConnue = BorneSupConnue[Var];		
    if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	       BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {
	    Smin += A * ValeurDeBorneInf[Var];
	    Smax += A * ValeurDeBorneInf[Var];						
		  goto NextElement;
	  }
		Nb++;
	  if ( A > 0.0 ) {
		  /* Calcul de min */
      if ( BrnInfConnue == OUI_PNE && MinValide != NON_PNE ) Smin += A * ValeurDeBorneInf[Var];				       								
		  else {
        MinValide = NON_PNE;
			  if ( MaxValide == NON_PNE ) goto ConclusionValiditeBminBmax;
		  }
		  /* Calcul de max */
      if ( BrnSupConnue == OUI_PNE && MaxValide != NON_PNE ) Smax += A * ValeurDeBorneSup[Var];			
		  else {
        MaxValide = NON_PNE;
			  if ( MinValide == NON_PNE ) goto ConclusionValiditeBminBmax;
		  }
	  }
	  else {
		  /* Calcul de min */
      if ( BrnSupConnue == OUI_PNE && MinValide != NON_PNE ) Smin += A * ValeurDeBorneSup[Var];			
		  else {
        MinValide = NON_PNE;
			  if ( MaxValide == NON_PNE ) goto ConclusionValiditeBminBmax;
		  }
		  /* Calcul de max */
      if ( BrnInfConnue == OUI_PNE && MaxValide != NON_PNE ) Smax += A * ValeurDeBorneInf[Var];			
		  else {
        MaxValide = NON_PNE;
			  if ( MinValide == NON_PNE ) goto ConclusionValiditeBminBmax;
		  }						
	  }
	  NextElement:
	  il++;
  }
  ConclusionValiditeBminBmax:
	
  Bmin[Cnt] = Smin;
  Bmax[Cnt] = Smax;
  BminValide[Cnt] = MinValide;		
  BmaxValide[Cnt] = MaxValide;
		
  if ( Nb == 0 ) {
    if ( SensContrainte[Cnt] == '<' ) {
      /* On verifie que la contrainte est satisfaite */
		  if ( Smin > B[Cnt] + SEUIL_DADMISSIBILITE && MinValide == OUI_PNE ) {			
			  *Faisabilite = NON_PNE;
			  return;
		  }
	  }
	  else {
	    /* Contrainte d'egalite */
      if ( ( fabs( Smax - Smin ) > SEUIL_DADMISSIBILITE && MaxValide == OUI_PNE && MinValide == OUI_PNE ) ||
			     ( fabs( Smax - B[Cnt] ) > SEUIL_DADMISSIBILITE && MaxValide == OUI_PNE ) ) {					 
			  *Faisabilite = NON_PNE;								
			  return;			
		  }
    }
  }
  else {
    if ( SensContrainte[Cnt] == '<' ) {
		  if ( MinValide == OUI_PNE && Smin > B[Cnt] + SEUIL_DADMISSIBILITE ) {
			  *Faisabilite = NON_PNE;
			  return;			
		  }
	  }
		else {
		  /* Contrainte d'egalite */
	    if ( MaxValide == OUI_PNE && Smax < B[Cnt] - SEUIL_DADMISSIBILITE ) {			
			  *Faisabilite = NON_PNE;
			  return;							
		  }
	    if ( MinValide == OUI_PNE && Smin > B[Cnt] + SEUIL_DADMISSIBILITE ) {			
			  *Faisabilite = NON_PNE;
			  return;							
		  }			
		}
  }

}

return;
}

/*----------------------------------------------------------------------------*/

char PNE_DeterminerForcingConstraint( PROBLEME_PNE * Pne, PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve,
                                      int Cnt, char SensCnt, char BmnValide, char BmxValide,
																			double Bmn, double Bmx, double BCnt )																		
{
char TypeDeForcing; int il; int ilMax; int Var; int * Mdeb; int * NbTerm; int * Nuvar;
double * A; double Ai; char * BorneInfConnue; char * BorneSupConnue; char BrnInfConnue;
double * ValeurDeBorneInf; double * ValeurDeBorneSup; char BorneMiseAJour; 
char UneVariableAEteFixee; double NouvelleValeur; int * TypeDeVariable; char CodeRet;

TypeDeForcing = PAS_DE_FORCING;

if ( BmnValide == OUI_PNE ) {	
	if ( fabs( Bmn - BCnt ) < EPSILON_FORCING_CONSTRAINT ) {
		/*
	 	if ( SensCnt == '<' ) printf("Forcing constraint pendant le variable probing sur contrainte d'inegalite %d\n",Cnt);
		else printf("Forcing constraint pendant le variable probing sur contrainte d'egalite %d\n",Cnt);
    printf("Bmin %e B %e sens %c\n",Bmn,BCnt,SensCnt);
    */		 			
		TypeDeForcing = FORCING_BMIN;
	}    
}
else if ( BmxValide == OUI_PNE ) {
  if ( SensCnt == '=' ) {		
	  if ( fabs( Bmx - BCnt ) < EPSILON_FORCING_CONSTRAINT ) {
      /*
			printf("Forcing constraint pendant le variable probing sur contrainte d'egalite %d\n",Cnt);          
      printf("Bmax %e B %e sens %c\n",Bmx,BCnt,SensCnt);
			*/
			TypeDeForcing = FORCING_BMAX;
    }
	}						
}
if ( TypeDeForcing != FORCING_BMIN && TypeDeForcing != FORCING_BMAX ) return( NON_PNE );
   
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;  
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneMiseAJour = NON_PNE;

CodeRet = NON_PNE;
il = Mdeb[Cnt];
ilMax = il + NbTerm[Cnt];
while ( il < ilMax ) {
  Var = Nuvar[il];	
	BrnInfConnue = BorneInfConnue[Var];	
	if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
		   BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIlDoForcing;		
  Ai = A[il];
	if ( Ai == 0.0 ) goto NextIlDoForcing;
		
  UneVariableAEteFixee = NON_PNE;
  BorneMiseAJour = NON_PNE;
	
	if ( TypeDeForcing == FORCING_BMIN ) {
    if ( Ai > 0.0 ) {
		  /*printf("Forcing constraint contrainte %d variables %d a fixer au min %e\n",Cnt,Var,ValeurDeBorneInf[Var]);*/		
			NouvelleValeur = ValeurDeBorneInf[Var];
			UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				
		}
		else {
		  /*printf("Forcing constraint contrainte %d variables %d a fixer au max %e\n",Cnt,Var,ValeurDeBorneSup[Var]);*/
      NouvelleValeur = ValeurDeBorneSup[Var];
			UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;								
		}
	}
	else { /* TypeForcing = FORCING_BMAX */
    if ( Ai > 0.0 ) {
		  /*printf("Forcing constraint contrainte %d variables %d a fixer au max %e\n",Cnt,Var,ValeurDeBorneSup[Var]);*/
      NouvelleValeur = ValeurDeBorneSup[Var];
			UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;				
		}
		else {
		  /*printf("Forcing constraint contrainte %d variables %d a fixer au min %e\n",Cnt,Var,ValeurDeBorneInf[Var]);*/	
			NouvelleValeur = ValeurDeBorneInf[Var];			
			UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;								
		}
	}
	/* Si la variable a ete fixee on met a jour la liste des contraintes a examiner au prochain coup */
	if ( UneVariableAEteFixee != NON_PNE ) {
	  CodeRet = OUI_PNE;
    PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return( NON_PNE );
	}
	
	NextIlDoForcing:
  il++;
}
	
return( CodeRet );
}  

/*----------------------------------------------------------------------------*/

void PNE_InitBorneInfBorneSupDesVariables( PROBLEME_PNE * Pne )
{
int NombreDeVariables; int Var; int TypeBorne; int * TypeDeBorne; double * Xmin;
double * Xmax; double * X; char * BorneSupConnue; char * BorneInfConnue;
double * ValeurDeBorneInf; double * ValeurDeBorneSup;
 
BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;

NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;  
X = Pne->UTrav;

# if BORNES_SPECIFIQUES_POUR_CALCUL_BMIN_BMAX == OUI_PNE
  Xmin = Pne->XminPourLeCalculDeBminBmax;
  Xmax = Pne->XmaxPourLeCalculDeBminBmax;
# else
  Xmin = Pne->UminTrav;
  Xmax = Pne->UmaxTrav;
# endif

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  BorneInfConnue[Var] = NON_PNE;
  BorneSupConnue[Var] = NON_PNE;
	ValeurDeBorneInf[Var] = -LINFINI_PNE;
	ValeurDeBorneSup[Var] = LINFINI_PNE;
  TypeBorne = TypeDeBorne[Var];	
	if ( TypeBorne == VARIABLE_FIXE ) {
    BorneInfConnue[Var] = FIXE_AU_DEPART;
    BorneSupConnue[Var] = FIXE_AU_DEPART;		
	  ValeurDeBorneInf[Var] = X[Var];
	  ValeurDeBorneSup[Var] = X[Var];
		continue;
	}
  if ( Xmin[Var] == Xmax[Var] ) {
    BorneInfConnue[Var] = FIXE_AU_DEPART;
    BorneSupConnue[Var] = FIXE_AU_DEPART;		
	  ValeurDeBorneInf[Var] = Xmin[Var];
	  ValeurDeBorneSup[Var] = Xmin[Var];
		continue;
	}	
	if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
	  BorneSupConnue[Var] = OUI_PNE;
		ValeurDeBorneSup[Var] = Xmax[Var];		
	}		
  if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {
	  BorneInfConnue[Var] = OUI_PNE;
		ValeurDeBorneInf[Var] = Xmin[Var];		
	}		
}

PNE_AppliquerToutesLesContraintesDeBorneVariable( Pne );

return;
}

/*----------------------------------------------------------------------------*/

void PNE_AppliquerToutesLesContraintesDeBorneVariable( PROBLEME_PNE * Pne )
{
int Cnt; int * First; double Xborne; int * Colonne; double * SecondMembre; double * Coefficient;
char * LaContrainteDeBorneVariableEstDansLePool; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
int NombreDeContraintesDeBorne; char * BorneSupConnue; double * ValeurDeBorneInf; char * BorneInfConnue;
double * ValeurDeBorneSup; char BrnInfConnue; int ilVarCont; int ilVarBin; int VarCont; int VarBin;
double B; double MargeSurNouvellesBornes; char * AppliquerUneMargeEventuelle;

/*return;*/ /* Inutilise pour l'instant car il semble, d'apres les essais numeriques, que ca n'apporte rien */
/* Finalement de nouveaux jeux de donnees ont montre que c'etait utile */

/* On peut accelerer le calcul car seules sont utiles les contraintes de bornes variables
   dans lesquelles figurent des variables entieres qui ont ete fixees */

if ( Pne->ContraintesDeBorneVariable == NULL ) return;
ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
LaContrainteDeBorneVariableEstDansLePool = ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;
AppliquerUneMargeEventuelle = ContraintesDeBorneVariable->AppliquerUneMargeEventuelle;

MargeSurNouvellesBornes = 1.e-2;

/* La variable continue est toujours en premier */

/* Peut etre ameliore: on met des marges pour les imprecisions numeriques mais ce n'est pas completement satifaisant.
   En effet, s'il existe 2 contraintes qui fixent une variable, on risque de ne pas le voir a cause de la marge ajoutee.
	 Pour pallier a ce manque je fais un test sur l'integralite de la borne en me disant que si on fixe une variable meme
	 continue, la borne sera plutot une valeur entiere qu'une valeur avec plein de decimale.
	 Ce qu'il faudrait faire c'est, dans une premiere passe identifer les contraintes qui fixent une variable.
	 Puis dans une 2eme passe mettre des marges sur les autres bornes */

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( LaContrainteDeBorneVariableEstDansLePool[Cnt] == OUI_PNE ) continue;	
  if ( First[Cnt] < 0 ) continue;	
  ilVarCont = First[Cnt];
	ilVarBin = ilVarCont + 1;	
	VarBin = Colonne[ilVarBin];	
	VarCont = Colonne[ilVarCont];
	BrnInfConnue = BorneInfConnue[VarBin];
	B = SecondMembre[Cnt];	
	/* Dans le cas ou la variable a ete fixee, BorneInfConnue et BorneSupConnue ont la meme valeur */		
	if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
		   BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {
	  BrnInfConnue = BorneInfConnue[VarCont];
	  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
		     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;  
		Xborne = ( B - ( Coefficient[ilVarBin] * ValeurDeBorneInf[VarBin] ) ) / Coefficient[ilVarCont];		
		if ( Coefficient[ilVarCont] > 0 ) {
      /* Contrainte de borne sup */
			
			if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			  if ( PNE_LaValeurEstEntiere( &Xborne ) == NON_PNE ) Xborne += MargeSurNouvellesBornes;
			}
			
			if ( Xborne < ValeurDeBorneSup[VarCont] ) {
        if ( Xborne < ValeurDeBorneInf[VarCont] - MARGE_INITIALE_VARIABLES_CONTINUES  /******************************************************/  ) {
          # if TRACES_CONTRAINTES_DE_BORNE_VARIABLE == OUI_PNE
            printf("Pas de solution a cause de la contrainte de borne sup variable %d: Nouvelle borne sup = %e ValeurDeBorneInf %e ValeurDeBorneSup %e\n",
						        Cnt,Xborne,ValeurDeBorneInf[VarCont],ValeurDeBorneSup[VarCont]);
          # endif					
          /* Pas de solution */
          Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;
          return;						
				}

        # if TRACES_CONTRAINTES_DE_BORNE_VARIABLE == OUI_PNE
          printf("Contrainte de borne sup variable %d: VarCont %d Nouvelle borne sup = %e  ValeurDeBorneSup %e\n",Cnt,VarCont,Xborne,ValeurDeBorneSup[VarCont]);
        # endif
				
				BorneSupConnue[VarCont] = OUI_PNE;

	      if ( fabs( Xborne - ValeurDeBorneInf[VarCont] ) < 1.e-8 ) {
		      ValeurDeBorneSup[VarCont] = 0.5 * ( Xborne + ValeurDeBorneInf[VarCont] );			
			    ValeurDeBorneInf[VarCont] = ValeurDeBorneSup[VarCont];
		    	BorneSupConnue[VarCont] = FIXATION_A_UNE_VALEUR;
			    BorneInfConnue[VarCont] = FIXATION_A_UNE_VALEUR;
          # if TRACES_CONTRAINTES_DE_BORNE_VARIABLE == OUI_PNE
            printf("VarCont %d fixee a %e grace aux contraintes de bornes variables\n",VarCont,ValeurDeBorneInf[VarCont]);
			    # endif			
		    }
				else {				
				  ValeurDeBorneSup[VarCont] = Xborne;
				}
				continue;
      }
		}
		else if ( Coefficient[ilVarCont] < 0 ) {
      /* Contrainte de borne inf */

			if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			  if ( PNE_LaValeurEstEntiere( &Xborne ) == NON_PNE ) Xborne -= MargeSurNouvellesBornes;
			}
			
			if ( Xborne > ValeurDeBorneInf[VarCont] ) {
        if ( Xborne > ValeurDeBorneSup[VarCont] + MARGE_INITIALE_VARIABLES_CONTINUES  /******************************************************/  ) {
          # if TRACES_CONTRAINTES_DE_BORNE_VARIABLE == OUI_PNE
            printf("Pas de solution a cause de la contrainte de borne inf variable %d: Nouvelle borne inf = %e ValeurDeBorneInf %e ValeurDeBorneSup %e\n",
						        Cnt,Xborne,ValeurDeBorneInf[VarCont],ValeurDeBorneSup[VarCont]);
          # endif        															
          /* Pas de solution */
          Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;
          return;						
				}			
        # if TRACES_CONTRAINTES_DE_BORNE_VARIABLE == OUI_PNE
          printf("Contrainte de borne inf variable %d: VarCont %d Nouvelle borne inf = %e  ValeurDeBorneInf %e\n",Cnt,VarCont,Xborne,ValeurDeBorneInf[VarCont]);
        # endif

				BorneInfConnue[VarCont] = OUI_PNE;
				
	      if ( fabs( ValeurDeBorneSup[VarCont] - Xborne ) < 1.e-8 ) {
		      ValeurDeBorneSup[VarCont] = 0.5 * ( ValeurDeBorneSup[VarCont] + Xborne );			
			    ValeurDeBorneInf[VarCont] = ValeurDeBorneSup[VarCont];
			    BorneSupConnue[VarCont] = FIXATION_A_UNE_VALEUR;
			    BorneInfConnue[VarCont] = FIXATION_A_UNE_VALEUR;
          # if TRACES_CONTRAINTES_DE_BORNE_VARIABLE == OUI_PNE
            printf("VarCont %d fixee a %e grace aux contraintes de bornes variables\n",VarCont,ValeurDeBorneInf[VarCont]);
			    # endif			
		    }
        else {				
				  ValeurDeBorneInf[VarCont] = Xborne;
				}
				continue;				
			}
		}		
	}	
}
				 
return;
}

/*----------------------------------------------------------------------------*/
/* Ce module ne peut etre appele qui si a ete fixee VariableEntiere */

void PNE_AppliquerToutesLesContraintesDeBorneVariablePourUneVariableEntiere( PROBLEME_PNE * Pne, int VariableEntiere )
{
int Cnt; int * First; double Xborne; int * Colonne; double * SecondMembre; double * Coefficient;
char * LaContrainteDeBorneVariableEstDansLePool; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
int NombreDeContraintesDeBorne; char * BorneSupConnue; double * ValeurDeBorneInf; char * BorneInfConnue;
double * ValeurDeBorneSup; char BrnInfConnue; int ilVarcont; int ilVarBin; int VarCont; int VarBin;
char UneVariableAEteFixee; char BorneMiseAJour; double NouvelleValeur;

if ( Pne->ContraintesDeBorneVariable == NULL ) return;
ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

printf("AppliquerToutesLesContraintesDeBorneVariablePourUneVariableEntiere attention, il faut aussi modifier ce sp pour tenir compte de AppliquerUneMargeEventuelle\n");
printf("Donc exit tant que c'est pas fait\n");
exit(0);

BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
LaContrainteDeBorneVariableEstDansLePool = ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;

/* La variable continue est toujours en premier */
 
UneVariableAEteFixee = NON_PNE; /* Pour eviter les warning de compilation */
BorneMiseAJour = MODIF_BORNE_SUP; /* Pour eviter les warning de compilation */
NouvelleValeur = 0; /* Pour eviter les warning de compilation */

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( LaContrainteDeBorneVariableEstDansLePool[Cnt] == OUI_PNE ) continue;	
  if ( First[Cnt] < 0 ) continue;	
  ilVarcont = First[Cnt];
	ilVarBin = ilVarcont + 1;	
	VarBin = Colonne[ilVarBin];
	
  if ( VarBin != VariableEntiere ) continue;
				 
	VarCont = Colonne[ilVarcont];
	BrnInfConnue = BorneInfConnue[VarCont];
	if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
		   BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;
	Xborne = ( SecondMembre[Cnt] - ( Coefficient[ilVarBin] * ValeurDeBorneInf[VarBin] ) ) / Coefficient[ilVarcont];		
	if ( Coefficient[ilVarcont] > 0 ) {
    /* Contrainte de borne sup */
		if ( Xborne < ValeurDeBorneSup[VarCont] - MARGE_INITIALE_VARIABLES_CONTINUES ) {
      if ( Xborne < ValeurDeBorneInf[VarCont] - MARGE_INITIALE_VARIABLES_CONTINUES ) {
        /* Pas de solution */
        Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;
        return;						
			}
		  UneVariableAEteFixee = NON_PNE;
      BorneMiseAJour = MODIF_BORNE_SUP;
			NouvelleValeur = Xborne;
			if ( NouvelleValeur == ValeurDeBorneInf[VarCont] ) UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;
			goto MajBminBmaxEtIndicateurs;
    }
	}
	else if ( Coefficient[ilVarcont] < 0 ) {
    /* Contrainte de borne inf */
		if ( Xborne > ValeurDeBorneInf[VarCont] + MARGE_INITIALE_VARIABLES_CONTINUES ) {
      if ( Xborne > ValeurDeBorneSup[VarCont] + MARGE_INITIALE_VARIABLES_CONTINUES ) {
        /* Pas de solution */
        Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;
        return;						
			}			
		  UneVariableAEteFixee = NON_PNE;
      BorneMiseAJour = MODIF_BORNE_INF;
			NouvelleValeur = Xborne;
			if ( NouvelleValeur == ValeurDeBorneSup[VarCont] ) UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;
			goto MajBminBmaxEtIndicateurs;								
		}
	}
  continue;
	MajBminBmaxEtIndicateurs:

  PNE_InitListeDesContraintesAExaminer( Pne, VarCont, NouvelleValeur, BorneMiseAJour );
	
  /*PNE_ProbingMajBminBmax( Pne, VarCont, NouvelleValeur, BorneMiseAJour );*/
  if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
  	
  PNE_MajIndicateursDeBornes( Pne, ValeurDeBorneInf, ValeurDeBorneSup, BorneInfConnue, BorneSupConnue,
                              NouvelleValeur, VarCont, UneVariableAEteFixee, BorneMiseAJour );					
}
				 
return;
}

/*----------------------------------------------------------------------------*/

void PNE_CalculMinContrainte( PROBLEME_PNE * Pne, double Bmin, double A, int Var, double * Min )																	
{
if ( A > 0.0 ) {
  /* On avait pris Umin */
	Bmin -= A * Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var];
}
else {
  /* On avait pris Umax */
	Bmin -= A * Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var];
}
*Min = Bmin;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_CalculMaxContrainte( PROBLEME_PNE * Pne, double Bmax, double A, int Var, double * Max )																	
{
if ( A > 0.0 ) {
  /* On avait pris Umax */
	Bmax -= A * Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var];
}
else {
  /* On avait pris Umin */
	Bmax -= A * Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var];
}
*Max = Bmax;
return;
}

/*----------------------------------------------------------------------------*/
/* Remarque: dans le calcul des bornes, on ajuste Bmin Bmax avec les bornes de
la variable. Rq: plus iutilise. */

void PNE_CalculXiXs( PROBLEME_PNE * Pne, double Ai, int Var, int Cnt, 
                     char * XiValide, char * XsValide, double * Xi , double * Xs ) 
{
double BminNew; double BmaxNew; double S; char * BminValide; double * Bmin;
char * BmaxValide; double * Bmax; double * B;

BminValide = Pne->ProbingOuNodePresolve->BminValide;
Bmin = Pne->ProbingOuNodePresolve->Bmin;
BmaxValide = Pne->ProbingOuNodePresolve->BmaxValide;
Bmax = Pne->ProbingOuNodePresolve->Bmax;
B = Pne->BTrav;

if ( Pne->SensContrainteTrav[Cnt] == '=' ) {	   
	/* On regarde le min et le max */
	if ( BminValide[Cnt] == OUI_PNE ) {																																		
    /*PNE_CalculMinContrainte( Pne, Bmin[Cnt], Ai, Var, &BminNew );*/
		BminNew = Bmin[Cnt];
    if ( Ai > 0.0 ) BminNew -= Ai * Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var]; /* On avait pris Umin */
    else BminNew -= Ai * Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var]; /* On avait pris Umax */		
		S = B[Cnt] - BminNew;
		if ( Ai > 0 ) { *Xs = S / Ai; *XsValide = OUI_PNE; }
		else { *Xi = -S / fabs( Ai ); *XiValide = OUI_PNE; }		
	}
	if ( BmaxValide[Cnt] == OUI_PNE ) {	
    /*PNE_CalculMaxContrainte( Pne, Bmax[Cnt], Ai, Var, &BmaxNew );*/
    BmaxNew = Bmax[Cnt];
		if ( Ai > 0.0 ) BmaxNew -= Ai * Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var]; /* On avait pris Umax */ 
    else BmaxNew -= Ai * Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var]; /* On avait pris Umin */	
    S = B[Cnt] - BmaxNew;
		if ( Ai > 0 ) { *Xi = S / Ai; *XiValide = OUI_PNE; }	
		else { *Xs = -S / fabs( Ai ); *XsValide = OUI_PNE; }				
	}	
}
else if ( Pne->SensContrainteTrav[Cnt] == '<' ) {
  /* On peut calculer un majorant */			
	if ( BminValide[Cnt] == OUI_PNE ) {						
    /*PNE_CalculMinContrainte( Pne, Bmin[Cnt], Ai, Var, &BminNew );*/
		BminNew = Bmin[Cnt];
    if ( Ai > 0.0 ) BminNew -= Ai * Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var]; /* On avait pris Umin */
    else BminNew -= Ai * Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var]; /* On avait pris Umax */				
		S = B[Cnt] - BminNew;
		if ( Ai > 0 ) { *Xs = S / Ai; *XsValide = OUI_PNE; }
		else { *Xi = -S / fabs( Ai ); *XiValide = OUI_PNE; }					
	}			 
}
return;
}

/*----------------------------------------------------------------------------*/

void PNE_CalculXiXsContrainteAUneSeuleVariable( PROBLEME_PNE * Pne, int * VarSelect, int Cnt,
                                                int ilDeb, int ilMax, double * A, int * Nuvar,
																								char * BorneInfConnue, double * ValeurDeBorneInf, 
                                                char * XiValide, char * XsValide,
																								double * Xi, double * Xs ) 
{
int il; int Nb; int Var; double S; double Ai; char BrnInfConnue;

*XsValide = NON_PNE; 
*XiValide = NON_PNE; 
S = 0;
il = ilDeb;
ilMax = ilMax;
Nb = 0;
*VarSelect = -1;
Ai = 1.0;
while ( il < ilMax ) {
	if ( A[il] == 0.0 ) goto NextIl;
	Var = Nuvar[il];		
  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {
	  S += A[il] * ValeurDeBorneInf[Var];
		goto NextIl;   
	}
	Nb++;
	*VarSelect = Var;
	Ai = A[il];
  NextIl:
	il++;
}
if ( Nb != 1 ) {
  printf("Probleme dans PNE_CalculXiXsContrainteAUneSeuleVariable sur la contrainte %d :\n",Cnt);
  printf("   La contrainte est sensee contenir une seule variable non fixee or elles sont deja toutes fixees\n");
	*VarSelect = -1;
	return; 
}
S = Pne->BTrav[Cnt] - S;
if ( Pne->SensContrainteTrav[Cnt] == '=' ) {
  *XiValide = OUI_PNE;
  *XsValide = OUI_PNE;
  *Xi = S / Ai;
	*Xs = *Xi;
}
else {
	if ( Ai > 0 ) { *Xs = S / Ai; *XsValide = OUI_PNE; }
	else { *Xi = -S / fabs( Ai ); *XiValide = OUI_PNE; }									 
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_MajIndicateursDeBornes( PROBLEME_PNE * Pne,
                                 double * ValeurDeBorneInf, double * ValeurDeBorneSup,
                                 char * BorneInfConnue, char * BorneSupConnue,
																 double NouvelleValeur, int Var, char UneVariableAEteFixee,
																 char BorneMiseAJour )
{
PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; int * TypeDeVariable;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
TypeDeVariable = Pne->TypeDeVariableTrav;

if ( ProbingOuNodePresolve->VariableModifiee[Var] == NON_PNE ) {
  ProbingOuNodePresolve->NumeroDeVariableModifiee[ProbingOuNodePresolve->NbVariablesModifiees] = Var;
	ProbingOuNodePresolve->NbVariablesModifiees++;
	ProbingOuNodePresolve->VariableModifiee[Var] = OUI_PNE;
}

if ( TypeDeVariable[Var] == ENTIER ) {
  /* Pour les variables entieres on incremente le compteur sauf si la variable est fixe au depart car alors
	   cela correspond a une arete existante et si on le prenait en compte on creerait un arete contradictoire ! */
  if ( UneVariableAEteFixee != FIXE_AU_DEPART ) {		 
    ProbingOuNodePresolve->NumeroDesVariablesFixees[ProbingOuNodePresolve->NombreDeVariablesFixees] = Var;
    ProbingOuNodePresolve->NombreDeVariablesFixees += 1;
	}
}

if ( UneVariableAEteFixee == FIXATION_SUR_BORNE_INF ) {
  ValeurDeBorneSup[Var] = NouvelleValeur;					
  BorneInfConnue[Var] = FIXATION_SUR_BORNE_INF;
  BorneSupConnue[Var] = FIXATION_SUR_BORNE_INF;								
}  
else if ( UneVariableAEteFixee == FIXATION_SUR_BORNE_SUP ) {
  ValeurDeBorneInf[Var] = NouvelleValeur;					
  BorneInfConnue[Var] = FIXATION_SUR_BORNE_SUP;
  BorneSupConnue[Var] = FIXATION_SUR_BORNE_SUP;							
}
else if ( UneVariableAEteFixee == FIXATION_A_UNE_VALEUR ) {
  ValeurDeBorneInf[Var] = NouvelleValeur;				
  ValeurDeBorneSup[Var] = NouvelleValeur;
  BorneInfConnue[Var] = FIXATION_A_UNE_VALEUR;
  BorneSupConnue[Var] = FIXATION_A_UNE_VALEUR;
  if ( Pne->TypeDeVariableTrav[Var] == ENTIER ) {
	  printf("Bug dans PNE_MajIndicateursDeBornes, une variable entiere ne prend jamais le qualificatif: FIXATION_A_UNE_VALEUR\n");
	}	
}
else if ( UneVariableAEteFixee == FIXE_AU_DEPART ) {
  ValeurDeBorneInf[Var] = NouvelleValeur;				
  ValeurDeBorneSup[Var] = NouvelleValeur;
  BorneInfConnue[Var] = FIXE_AU_DEPART;
  BorneSupConnue[Var] = FIXE_AU_DEPART;									
}
else if ( BorneMiseAJour == MODIF_BORNE_INF ) {
  ValeurDeBorneInf[Var] = NouvelleValeur;
  BorneInfConnue[Var] = OUI_PNE;
  if ( Pne->TypeDeVariableTrav[Var] == ENTIER ) {
	  printf("Bug dans PNE_MajIndicateursDeBornes, une variable entiere ne prend jamais le qualificatif: MODIF_BORNE_INF\n");
	}		
}
else if ( BorneMiseAJour == MODIF_BORNE_SUP ) {
  ValeurDeBorneSup[Var] = NouvelleValeur;
  BorneSupConnue[Var] = OUI_PNE;
  if ( Pne->TypeDeVariableTrav[Var] == ENTIER ) {
	  printf("Bug dans PNE_MajIndicateursDeBornes, une variable entiere ne prend jamais le qualificatif: MODIF_BORNE_SUP\n");
	}		
}
else {
  printf("Bug dans PNE_MajIndicateursDeBornes, indicateur UneVariableAEteFixee ou BorneMiseAJour incorrects\n");
  printf("Variable %d UneVariableAEteFixee = %d   BorneMiseAJour = %d\n",Var,UneVariableAEteFixee,BorneMiseAJour);
	exit(0);
}
return;
}

/*----------------------------------------------------------------------------*/

void PNE_ModifierLaBorneDUneVariable( PROBLEME_PNE * Pne, int Var, char SensContrainte,
                                      char XsValide, double Xs, char XiValide, double Xi,																			
                                      double * NouvelleValeur, char * BorneMiseAJour,
																			char * VariableFixee, int * Faisabilite )
{
int TypeDeVariable; char BorneInfConnue; double Marge; double ValeurDeBorneInf; double InfaisabiliteVariableContinue;
char BorneSupConnue; double ValeurDeBorneSup; double MargeBorneVariableContinue; char Sens;

/* Remarque: NombreDeVariablesFixees ne concerne que les variables entieres */

Sens = SensContrainte; /* Pour ne pas avoir de warning a la compilation */

BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue[Var];
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf[Var];
BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue[Var];
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup[Var];

TypeDeVariable = Pne->TypeDeVariableTrav[Var];

Marge = MARGE_INITIALE_VARIABLES_CONTINUES /*MARGE_INITIALE*/;
if ( TypeDeVariable == ENTIER ) Marge = MARGE_INITIALE_VARIABLES_ENTIERES /*VALEUR_DE_FRACTIONNALITE_NULLE * 10.*/;

/*
MargeBorneVariableContinue = 0.1;
if ( BorneInfConnue != NON_PNE && BorneSupConnue != NON_PNE ) {
  if ( 0.1 * (ValeurDeBorneSup - ValeurDeBorneInf) < MargeBorneVariableContinue ) MargeBorneVariableContinue = 0.1 * (ValeurDeBorneSup - ValeurDeBorneInf);
}
*/

InfaisabiliteVariableContinue = 1.e-7;
MargeBorneVariableContinue = 1.0;
if ( BorneInfConnue != NON_PNE && BorneSupConnue != NON_PNE ) {
  if ( 0.1 * (ValeurDeBorneSup - ValeurDeBorneInf) < MargeBorneVariableContinue ) MargeBorneVariableContinue = 0.1 * (ValeurDeBorneSup - ValeurDeBorneInf);
}

/* Si c'est une variable entiere elle ne peut pas etre entre les 2 bornes */
if ( XsValide == OUI_PNE ) {
	/* Attention il ne faut pas avoir Xs < Umin */
	if ( BorneInfConnue == OUI_PNE && Xs < ValeurDeBorneInf - Marge ) {
		*Faisabilite = NON_PNE;
    # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
	 	  printf("Non realisable variable %d Xs %e ValeurDeBorneInf %e XsValide %d\n",Var,Xs,ValeurDeBorneInf,XsValide);
		# endif			
		return;
	}
}
if ( XiValide == OUI_PNE ) {
	/* Attention il ne faut pas avoir Xi > Umax */
	if ( BorneSupConnue == OUI_PNE && Xi > ValeurDeBorneSup + Marge ) {
		*Faisabilite = NON_PNE;
    # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
		  printf("Non realisable variable %d Xi %e ValeurDeBorneSup %e XiValide %d\n",Var,Xi,ValeurDeBorneSup,XiValide);
		# endif					
		return;
	}
}
/* Tentative de fixation a une valeur */
if ( XsValide == OUI_PNE && XiValide == OUI_PNE ) {	
	if ( fabs( Xs - Xi ) < ZERO_NP_PROB ) {
		Xi = 0.5 * ( Xs + Xi );
		if ( Xi < ValeurDeBorneInf - Marge || Xi > ValeurDeBorneSup + Marge ) {
			/* Par precaution mais normalement ca ne passe pas les 2 tests qui precedent */
		  *Faisabilite = NON_PNE;
      # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
			  printf("Non realisable Xi %e ValeurDeBorneInf %e ValeurDeBorneSup %e\n",Xi,ValeurDeBorneInf,ValeurDeBorneSup);
		  # endif							
			return;			
    }			
		if ( TypeDeVariable == ENTIER ) {
			/* Variable entiere: la valeur ne doit pas se trouver entre min et max */
			if ( fabs( ValeurDeBorneInf - Xi ) > Marge && fabs( ValeurDeBorneSup - Xi ) > Marge ) {
			  *Faisabilite = NON_PNE;
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
		 	    printf("Non realisable Xi %e ValeurDeBorneSup %e\n",Xi,ValeurDeBorneSup);					
		    # endif				
			  return;
			}
      if ( fabs( Xi - ValeurDeBorneInf ) < fabs( ValeurDeBorneSup - Xi ) ) {
				/* Fixation de la variable entiere a Xmin */
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
				  printf("Fixation de la variable entiere %d a %e sur contrainte d'egalite Xi %e\n",Var,ValeurDeBorneInf,Xi);
		    # endif				
				*NouvelleValeur = ValeurDeBorneInf;				
				*VariableFixee = FIXATION_SUR_BORNE_INF;
				return;
			}	
			else {
				/* Fixation de la variable entiere a Xmax */
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
				  printf("Fixation de la variable entiere %d a %e sur contrainte d'egalite Xi %e\n",Var,ValeurDeBorneSup,Xi);		
		    # endif				
				*NouvelleValeur = ValeurDeBorneSup;
				*VariableFixee = FIXATION_SUR_BORNE_SUP;																		
			  return;
		  }
		}		
		else {			  
		  /* Variable continue */
      # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
			  printf("Fixation de la variable continue %d a %e ValeurDeBorneInf %e ValeurDeBorneSup %e par contrainte d'egalite\n",Var,Xi,
			          ValeurDeBorneInf,ValeurDeBorneSup);					
		  # endif
			
			if ( BorneInfConnue != NON_PNE && Xi < ValeurDeBorneInf - InfaisabiliteVariableContinue ) { 
		    *Faisabilite = NON_PNE;
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
		      printf("Non realisable variable %d On vaut la fixer a %e or ValeurDeBorneInf = %e\n",Var,Xi,ValeurDeBorneInf);
		    # endif					
		    return;
			}
			else if ( BorneSupConnue != NON_PNE && Xi > ValeurDeBorneSup + InfaisabiliteVariableContinue ) {
		    *Faisabilite = NON_PNE;
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
		      printf("Non realisable variable %d On vaut la fixer a %e or ValeurDeBorneSup = %e\n",Var,Xi,ValeurDeBorneSup);
		    # endif					
		    return;
			}
			if ( BorneInfConnue != NON_PNE && Xi < ValeurDeBorneInf ) Xi = ValeurDeBorneInf;
		  if ( BorneSupConnue != NON_PNE && Xi > ValeurDeBorneSup ) Xi = ValeurDeBorneSup;

			/* Fixation de la variable a une valeur */				
			*NouvelleValeur = Xi;				
			*VariableFixee = FIXATION_A_UNE_VALEUR;						
			return;
	  }
		return;
  }
}

/* Xi et Xs sont differents ou bien une seule des 2 valeurs est valide */

/* Remarque: on ne peut jamais mettre a jour a la fois Xi et Xs donc ca reste a faire */

if ( XsValide == OUI_PNE ) {
	if ( TypeDeVariable == ENTIER ) {
		/* Variable entiere */
		if ( Xs < ValeurDeBorneSup - Marge ) {
			/* On fixe la variable a Xmin */
      # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
			  printf("Fixation de la variable entiere %d a %e sur contrainte d'inegalite Xs %e\n",Var,ValeurDeBorneInf,Xs);						
		  # endif		
			*NouvelleValeur = ValeurDeBorneInf;				
			*VariableFixee = FIXATION_SUR_BORNE_INF;				
			return;
    }						
	}
	else {
	  /* Variable continue */
    /* Tentative de fixation */
	  if ( fabs( Xs - ValeurDeBorneInf ) < ZERO_NP_PROB ) {
		  Xi = 0.5 * ( Xs + ValeurDeBorneInf );
			
			if ( BorneInfConnue != NON_PNE && Xi < ValeurDeBorneInf - InfaisabiliteVariableContinue ) { 
		    *Faisabilite = NON_PNE;
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
		      printf("Non realisable variable %d On vaut la fixer a %e or ValeurDeBorneInf = %e\n",Var,Xi,ValeurDeBorneInf);
		    # endif					
		    return;
			}
			else if ( BorneSupConnue != NON_PNE && Xi > ValeurDeBorneSup + InfaisabiliteVariableContinue ) {
		    *Faisabilite = NON_PNE;
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
		      printf("Non realisable variable %d On vaut la fixer a %e or ValeurDeBorneSup = %e\n",Var,Xi,ValeurDeBorneSup);
		    # endif					
		    return;
			}
			if ( BorneInfConnue != NON_PNE && Xi < ValeurDeBorneInf ) Xi = ValeurDeBorneInf;
		  if ( BorneSupConnue != NON_PNE && Xi > ValeurDeBorneSup ) Xi = ValeurDeBorneSup;
						
			/* Fixation de la variable a une valeur */				
			*NouvelleValeur = Xi;				
			*VariableFixee = FIXATION_A_UNE_VALEUR;
      # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
        # if BORNES_SPECIFIQUES_POUR_CALCUL_BMIN_BMAX == OUI_PNE
          printf("Fixation d'une variable continue sur une borne %d  %e  Umin %e  Umax %e\n",Var,Xi,
					        Pne->XminPourLeCalculDeBminBmax[Var],Pne->XmaxPourLeCalculDeBminBmax[Var]);					
			  # else 
          printf("Fixation d'une variable continue sur une borne %d  %e  Umin %e  Umax %e\n",Var,Xi,Pne->UminTrav[Var],Pne->UmaxTrav[Var]);
				# endif
		  # endif		
			return;			
		}		
		/* Tentative d'amelioration de borne sup */		
		if ( Xs < ValeurDeBorneSup - MargeBorneVariableContinue ) {		
			/* Precaution */			
			if ( Xs < ValeurDeBorneInf + MargeBorneVariableContinue ) return;			
			/* On abaisse la borne sup */				
      # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
			  printf("On abaisse la borne sup de la variable continue %d de %e a %e (borne inf = %e connue %d)\n",
					      Var,ValeurDeBorneSup,Xs,ValeurDeBorneInf,BorneInfConnue);							
		  # endif
			
			*NouvelleValeur = Xs;			
			*BorneMiseAJour = MODIF_BORNE_SUP;
			return;
    }						
	}
}	
if ( XiValide == OUI_PNE ) {
	if ( TypeDeVariable == ENTIER ) {
	  if ( Xi > ValeurDeBorneInf + Marge ) {		
			/* On fixe la variable a Xmax */
      # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
			  printf("Fixation de la variable entiere %d a %e sur contrainte d'inegalite Xi %e\n",Var,ValeurDeBorneSup,Xi);		 									
		  # endif			
			*NouvelleValeur = ValeurDeBorneSup;								
			*VariableFixee = FIXATION_SUR_BORNE_SUP;
			return;
    }
	}
	else {
	  /* Variable continue */
    /* Tentative de fixation */
	  if ( fabs( ValeurDeBorneSup - Xi ) < ZERO_NP_PROB ) {
		  Xi = 0.5 * ( ValeurDeBorneSup + Xi );
			
			if ( BorneInfConnue != NON_PNE && Xi < ValeurDeBorneInf - InfaisabiliteVariableContinue ) { 
		    *Faisabilite = NON_PNE;
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
		      printf("Non realisable variable %d On vaut la fixer a %e or ValeurDeBorneInf = %e\n",Var,Xi,ValeurDeBorneInf);
		    # endif					
		    return;
			}
			else if ( BorneSupConnue != NON_PNE && Xi > ValeurDeBorneSup + InfaisabiliteVariableContinue ) {
		    *Faisabilite = NON_PNE;
        # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
		      printf("Non realisable variable %d On vaut la fixer a %e or ValeurDeBorneSup = %e\n",Var,Xi,ValeurDeBorneSup);
		    # endif					
		    return;
			}
			if ( BorneInfConnue != NON_PNE && Xi < ValeurDeBorneInf ) Xi = ValeurDeBorneInf;
		  if ( BorneSupConnue != NON_PNE && Xi > ValeurDeBorneSup ) Xi = ValeurDeBorneSup;
									
			/* Fixation de la variable a une valeur */				
			*NouvelleValeur = Xi;				
			*VariableFixee = FIXATION_A_UNE_VALEUR;
      # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
        # if BORNES_SPECIFIQUES_POUR_CALCUL_BMIN_BMAX == OUI_PNE
          printf("Fixation d'une variable continue sur une borne %d  %e  Umin %e  Umax %e\n",Var,Xi,
					        Pne->XminPourLeCalculDeBminBmax[Var],Pne->XmaxPourLeCalculDeBminBmax[Var]);
			  # else				
          printf("Fixation d'une variable continue sur une borne %d  %e  Umin %e  Umax %e\n",Var,Xi,Pne->UminTrav[Var],Pne->UmaxTrav[Var]);				
		    # endif			
		  # endif			
			return;			
	  }
		/* Tentative d'amelioration de borne inf */		
		if ( Xi > ValeurDeBorneInf + MargeBorneVariableContinue ) {		
			/* Precaution */		
			if ( Xi > ValeurDeBorneSup - MargeBorneVariableContinue ) return;			
			/* On remonte la borne inf */
      # if VERBOSE_FIXATIONS_DE_VARIABLES == OUI_PNE
			  printf("On remonte la borne inf de la variable continue %d de %e a %e (borne sup = %e connue %d)\n",
					      Var,ValeurDeBorneInf,Xi,ValeurDeBorneSup,BorneSupConnue);							
		  # endif			
			*NouvelleValeur = Xi;		
			*BorneMiseAJour = MODIF_BORNE_INF;
			return;
    }
	}
}						
			
return;
}

/*----------------------------------------------------------------------------*/
/* Tentative de fixation sur critere */
/* Si une variable n'est pas seule dans sa contrainte on regarde si on peut la fixer pour desaturer
   une contrainte d'inegalite. Si elle intervient dans d'autres egalites ou des inegalites avec un
	 coeff de signe different mais qu'elle est seule (les autres sont fixees alors on peut la fixer) */
	 
void PNE_ProbingNodePresolveFixerVariablesSurCritere( PROBLEME_PNE * Pne, char * DesVariablesOntEteFixees )
{
int Var; int * TypeDeVariable; int NombreDeVariables; double * L;
int * Cdeb; int * Csui; double * A; double Ai; int Cnt; char * SensContrainte;
int * NumContrainte; int ic; double SigneCoeff;  int NombreDeContraintes;
char * BorneSupConnue; double * ValeurDeBorneSup; double NouvelleValeur; char BorneMiseAJour;  
char * BorneInfConnue; double * ValeurDeBorneInf; 
double * B; double * Bmax; char * BmaxValide; char UneVariableAEteFixee;
char BrnInfConnue; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; CONFLICT_GRAPH * ConflictGraph;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
L = Pne->LTrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
A = Pne->ATrav;
SensContrainte = Pne->SensContrainteTrav;
NumContrainte = Pne->NumContrainteTrav;
B = Pne->BTrav;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
ConflictGraph = Pne->ConflictGraph;

BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;
Bmax = ProbingOuNodePresolve->Bmax;
BmaxValide = ProbingOuNodePresolve->BmaxValide;

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
	
  if ( TypeDeVariable[Var] != ENTIER ) continue;
	
	BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;

	NouvelleValeur = -1;
	BorneMiseAJour = NON_PNE;
  UneVariableAEteFixee = NON_PNE;
	
	SigneCoeff = '?';
  ic = Cdeb[Var];
  while ( ic >= 0 ) {	
    Ai = A[ic];
    if ( Ai == 0.0 ) goto NextIc_1;		
    Cnt = NumContrainte[ic];
		
    if ( SensContrainte[Cnt] == '<' ) {
		  /* Contrainte toujours satisafaite ? */
		  if ( Bmax[Cnt] <= B[Cnt] && BmaxValide[Cnt] == OUI_PNE ) goto NextIc_1;		   
    }
				 
    if ( SensContrainte[Cnt] == '=' ) {			
			SigneCoeff = 'X';
		  goto NextVar;
		}		
		if ( Ai > 0.0 ) {
      if ( SigneCoeff == '?' ) SigneCoeff = '+';
			else if ( SigneCoeff == '-' ) {				
				SigneCoeff = 'X';				
				goto NextVar;
			}
		}
		else {
      if ( SigneCoeff == '?' ) SigneCoeff = '-';
			else if ( SigneCoeff == '+' ) {				
				SigneCoeff = 'X';
				goto NextVar;
			}
		}		
		NextIc_1:
    ic = Csui[ic];
  }
  NextVar:
	if ( SigneCoeff == 'X' ) continue;	
	if ( SigneCoeff == '+' ) {
	  if ( L[Var] >= 0.0 ) {
		  /* On fixe a Umin */
			if ( BorneInfConnue[Var] == OUI_PNE ) {
		    *DesVariablesOntEteFixees = OUI_PNE;
				/*
			  printf("Fixation de la variable %d a %e\n",Var,ValeurDeBorneInf[Var]);
				*/
        NouvelleValeur = ValeurDeBorneInf[Var];
				UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				
			}
		}  
	}	
	else if ( SigneCoeff == '-' ) {
	  if ( L[Var] <= 0.0 ) {
			/* On fixe a Umax */
			if ( BorneSupConnue[Var] == OUI_PNE ) {
		    *DesVariablesOntEteFixees = OUI_PNE;
				/*
			  printf("Fixation de la variable %d a %e\n",Var,ValeurDeBorneSup[Var]);
				*/
        NouvelleValeur = ValeurDeBorneSup[Var];
				UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;				      									
			}
		}
	}
	else if ( SigneCoeff == '?' ) {
		/*printf("Fixation de la variable %d car elle n'apparait pas dans les contraintes\n",Var);*/
    /* La variable n'apparait pas dans les contraintes */
	  if ( L[Var] >= 0.0 ) {
		  /* On fixe a Umin */
			if ( BorneInfConnue[Var] == OUI_PNE ) {
		    *DesVariablesOntEteFixees = OUI_PNE;
				/*
			  printf("Fixation de la variable %d a %e\n",Var,ValeurDeBorneInf[Var]);
				*/
        NouvelleValeur = ValeurDeBorneInf[Var];
				UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;				       
			}
		}
		else {
			if ( BorneSupConnue[Var] == OUI_PNE ) {
		    *DesVariablesOntEteFixees = OUI_PNE;
				/*
			  printf("Fixation de la variable %d a %e\n",Var,ValeurDeBorneSup[Var]);
				*/
        NouvelleValeur = ValeurDeBorneSup[Var];
			  UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;				  
			}
		}
	  if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE ) {      
			/*
			PNE_MajIndicateursDeBornes( Pne, ValeurDeBorneInf, ValeurDeBorneSup, BorneInfConnue, BorneSupConnue,
															 	  NouvelleValeur, Var, UneVariableAEteFixee, BorneMiseAJour );		  
			*/
			
      PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
			
    }		
  }	
}

return;
}
