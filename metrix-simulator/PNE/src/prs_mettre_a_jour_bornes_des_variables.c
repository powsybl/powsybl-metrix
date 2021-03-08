/***********************************************************************

   FONCTION: Mise a jour des bornes d'une variable a chaque fois qu'on
	           calcule une nouvelle borne.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_fonctions.h"  
# include "pne_define.h"

# define TRACES_BORNES_IDENTIQUES 0
# define TRACES 0

/*----------------------------------------------------------------------------*/
/* Il se peut qu'en ameliorant les bornes, la variable en soit fixee */
void PRS_VariablesDeBornesIdentiques( PRESOLVE * Presolve, int Var,																		
																		  double BorneInf, double BorneSup,
                                      char * BorneAmelioree )
{
PROBLEME_PNE * Pne;
*BorneAmelioree = NON_PNE;
Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

if ( Pne->TypeDeVariableTrav[Var] == ENTIER ) return;
if ( Presolve->TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) return;
/* Il se peut qu'en ameliorant les bornes, la variable en soit fixee */
if ( fabs( BorneSup - BorneInf ) < MARGE_EGALITE_BORNE_MIN_ET_MAX ) {
  # if TRACES_BORNES_IDENTIQUES == 1
    printf("Fixation de la variable %d car bornes identiques BorneInf %e BorneSup %e Ecart %e\n",
            Var,BorneInf,BorneSup,BorneSup-BorneInf);
  # endif			
  *BorneAmelioree = OUI_PNE;
  PRS_FixerUneVariableAUneValeur( Presolve, Var, 0.5 * ( BorneSup + BorneInf ) );	
}
return;  
}

/*----------------------------------------------------------------------------*/

void PRS_MettreAJourLesBornesDUneVariable( PRESOLVE * Presolve, int Var,
																		       char BorneInfCalculee, double BorneInf,
																		       int ContrainteBornanteInf,
																		       char BorneSupCalculee, double BorneSup,
																	 	       int ContrainteBornanteSup,
                                           char * BorneAmelioree )
{
int * TypeDeVariable; int * TypeDeBornePourPresolve; double * BorneInfPourPresolve;
double * BorneSupPourPresolve; int TypeBrn; int * ContrainteBornanteInferieurement;
int * ContrainteBornanteSuperieurement; char * TypeDeValeurDeBorneInf;
char * TypeDeValeurDeBorneSup; char * ConserverLaBorneInfDuPresolve;
char * ConserverLaBorneSupDuPresolve; double MargeSurModifDeBorne;
double SeuilPourAmeliorationDeBorne; PROBLEME_PNE * Pne;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

*BorneAmelioree = NON_PNE;

TypeDeVariable = Pne->TypeDeVariableTrav;

TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;
TypeDeValeurDeBorneInf = Presolve->TypeDeValeurDeBorneInf;
TypeDeValeurDeBorneSup = Presolve->TypeDeValeurDeBorneSup;
ContrainteBornanteInferieurement = Presolve->ContrainteBornanteInferieurement;
ContrainteBornanteSuperieurement = Presolve->ContrainteBornanteSuperieurement;
ConserverLaBorneInfDuPresolve = Presolve->ConserverLaBorneInfDuPresolve;
ConserverLaBorneSupDuPresolve = Presolve->ConserverLaBorneSupDuPresolve;

TypeBrn = TypeDeBornePourPresolve[Var];

MargeSurModifDeBorne = MARGE_SUR_MODIF_DE_BORNE;
SeuilPourAmeliorationDeBorne = SEUIL_POUR_AMELIORATION_DE_BORNE;

if ( BorneInfCalculee == OUI_PNE ) {
  
  PRS_VariablesDeBornesIdentiques( Presolve, Var, BorneInf, BorneSupPourPresolve[Var], BorneAmelioree );
	if ( *BorneAmelioree == OUI_PNE ) return;

	/*if ( ConserverLaBorneInfDuPresolve[Var] == OUI_PNE || TypeDeValeurDeBorneInf[Var] == VALEUR_NATIVE ) SeuilPourAmeliorationDeBorne = 0.0;*/
	/*if ( TypeDeValeurDeBorneInf[Var] == VALEUR_NATIVE ) SeuilPourAmeliorationDeBorne = 0.0;*/

  if ( BorneInf - MargeSurModifDeBorne >= BorneInfPourPresolve[Var] + SeuilPourAmeliorationDeBorne || 
       TypeBrn == VARIABLE_BORNEE_SUPERIEUREMENT || TypeBrn == VARIABLE_NON_BORNEE ) {     			 
    # if TRACES == 1
      printf("-> variable %d borne inf amelioree valeur %e nouvelle valeur %e\n",Var,BorneInfPourPresolve[Var],BorneInf); 
    # endif						    		
    *BorneAmelioree = OUI_PNE;	
		BorneInfPourPresolve[Var] = BorneInf - MargeSurModifDeBorne;
    ConserverLaBorneInfDuPresolve[Var] = NON_PNE; /* Car elle a pu etre mise a OUI_PNE lors d'une desactivation de contrainte */	
    ContrainteBornanteInferieurement[Var] = ContrainteBornanteInf;
    TypeDeValeurDeBorneInf[Var] = VALEUR_IMPLICITE;
		
		/* Maj des bornes pour le presolve */		
    if ( TypeBrn == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBornePourPresolve[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;
    else if ( TypeBrn == VARIABLE_NON_BORNEE ) TypeDeBornePourPresolve[Var] = VARIABLE_BORNEE_INFERIEUREMENT;     
  }
}

if ( TypeDeVariable[Var] == ENTIER && BorneInfPourPresolve[Var] > SEUIL_INF_POUR_FIXATION_BINAIRE ) {
  if ( BorneSupPourPresolve[Var] == 1.0 ) {
    # if TRACES == 1
      printf("-> variable entiere %d fixee a 1\n",Var); 
    # endif		
    *BorneAmelioree = OUI_PNE;		
    PRS_FixerUneVariableAUneValeur( Presolve, Var, 1.0 );   
		return;
	}
	else {	
    # if TRACES == 1
      printf("-> Probleme infaisable: variable entiere %d BorneSup %e BorneInf %e\n",Var,BorneSupPourPresolve[Var],BorneInfPourPresolve[Var]); 		
		# endif
	  Pne->YaUneSolution = PROBLEME_INFAISABLE;
		return;
	}
}
  
if ( BorneSupCalculee == OUI_PNE ) {

  PRS_VariablesDeBornesIdentiques( Presolve, Var, BorneInfPourPresolve[Var], BorneSup, BorneAmelioree );
	if ( *BorneAmelioree == OUI_PNE ) return;

	/*if ( ConserverLaBorneSupDuPresolve[Var] == OUI_PNE || TypeDeValeurDeBorneSup[Var] == VALEUR_NATIVE ) SeuilPourAmeliorationDeBorne = 0.0;*/	
	/*if ( TypeDeValeurDeBorneSup[Var] == VALEUR_NATIVE ) SeuilPourAmeliorationDeBorne = 0.0;*/
	
  if ( BorneSup + MargeSurModifDeBorne <= BorneSupPourPresolve[Var] - SeuilPourAmeliorationDeBorne || 
       TypeBrn == VARIABLE_BORNEE_INFERIEUREMENT || TypeBrn == VARIABLE_NON_BORNEE ) {
			 
    # if TRACES == 1
		  printf("-> variable %d borne sup amelioree valeur %e nouvelle valeur %e\n",Var,BorneSupPourPresolve[Var],BorneSup);
      if ( TypeBrn == VARIABLE_BORNEE_DES_DEUX_COTES ) printf("VARIABLE_BORNEE_DES_DEUX_COTES \n");
      if ( TypeBrn == VARIABLE_BORNEE_INFERIEUREMENT ) printf("VARIABLE_BORNEE_INFERIEUREMENT \n");
      if ( TypeBrn == VARIABLE_BORNEE_SUPERIEUREMENT ) printf("VARIABLE_BORNEE_SUPERIEUREMENT \n");
      if ( TypeBrn == VARIABLE_NON_BORNEE ) printf("VARIABLE_NON_BORNEE \n");
    # endif
		
    *BorneAmelioree = OUI_PNE;		
		BorneSupPourPresolve[Var] = BorneSup + MargeSurModifDeBorne;
    ConserverLaBorneSupDuPresolve[Var] = NON_PNE; /* Car elle a pu etre mise a OUI_PNE lors d'une desactivation de contrainte */	
    ContrainteBornanteSuperieurement[Var] = ContrainteBornanteSup;
    TypeDeValeurDeBorneSup[Var] = VALEUR_IMPLICITE;
		
		/* Maj des bornes pour le presolve */		
    if ( TypeBrn == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBornePourPresolve[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;
    else if ( TypeBrn == VARIABLE_NON_BORNEE ) TypeDeBornePourPresolve[Var] = VARIABLE_BORNEE_SUPERIEUREMENT;
		
    # if TRACES == 1
      if ( TypeDeBornePourPresolve[Var] == VARIABLE_BORNEE_DES_DEUX_COTES ) printf("La variable devient VARIABLE_BORNEE_DES_DEUX_COTES \n");
      if ( TypeDeBornePourPresolve[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) printf("La variable devient VARIABLE_BORNEE_INFERIEUREMENT \n");
      if ( TypeDeBornePourPresolve[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) printf("La variable devient VARIABLE_BORNEE_SUPERIEUREMENT \n");
      if ( TypeDeBornePourPresolve[Var] == VARIABLE_NON_BORNEE ) printf("La variable devient VARIABLE_NON_BORNEE \n");
		  printf("BorneInfPourPresolve %e BorneSupPourPresolve %e Ecart %e\n",BorneInfPourPresolve[Var],BorneSupPourPresolve[Var],
			        BorneSupPourPresolve[Var]-BorneInfPourPresolve[Var]);
    # endif
		
  }
}

if ( TypeDeVariable[Var] == ENTIER && BorneSupPourPresolve[Var] < SEUIL_SUP_POUR_FIXATION_BINAIRE ) {
  if ( BorneInfPourPresolve[Var] == 0.0 ) {	
    # if TRACES == 1
    printf("-> variable entiere %d fixee a 0\n",Var); 
    # endif		
    *BorneAmelioree = OUI_PNE;
    PRS_FixerUneVariableAUneValeur( Presolve, Var, 0.0 );   			
		return;		
	}
	else {
    # if TRACES == 1
      printf("-> Probleme infaisable: variable entiere %d BorneSup %e BorneInf %e\n",Var,BorneSupPourPresolve[Var],BorneInfPourPresolve[Var]); 		
		# endif	
	  Pne->YaUneSolution = PROBLEME_INFAISABLE;
		return;
	}
}

if ( TypeDeBornePourPresolve[Var] == VARIABLE_BORNEE_DES_DEUX_COTES &&
		 BorneInfPourPresolve[Var] > BorneSupPourPresolve[Var] + MARGE_DINFAISABILITE ) {     
  # if TRACES == 1
    printf("** Phase de Presolve, resserage des bornes des variables:\n"); 
    printf("   la variable %d a une borne inf %lf plus grande que la borne sup %lf\n",Var,
				       BorneInfPourPresolve[Var],BorneSupPourPresolve[Var]);
		printf("   Valeur min en entree %e  Valeur max en entree %e\n",Pne->UminTrav[Var],Pne->UmaxTrav[Var]);
  # endif
  Pne->YaUneSolution = PROBLEME_INFAISABLE;
	return;  
}

return;
}




