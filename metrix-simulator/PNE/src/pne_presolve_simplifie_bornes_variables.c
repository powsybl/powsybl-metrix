/***********************************************************************

   FONCTION: Prise en compte des contraintes de borne variable dans le
	           presolve simplifie.
                 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
# include "spx_fonctions.h"
 
# include "bb_define.h"
  
# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES NON_PNE

# define ZERFIX 1.e-6

# define INFAISABILITE 1.e-3

/*****************************/ # define MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES 1.e-4 /*1.e-2*/

# define APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES OUI_PNE /*OUI_PNE*/

# if CONSTRUIRE_BORNES_VARIABLES == OUI_PNE

/*----------------------------------------------------------------------------*/

void PNE_PresolveSimplifieContraintesDeBornesVariables( PROBLEME_PNE *  Pne, int * Faisabilite,
                                                        char * RefaireUnCycle ) 
{
int Cnt; int * First; int ilbin; int ilcont; double B; int Varcont; int * Colonne; double * SecondMembre;
double * Coefficient; int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
int NombreDeContraintesDeBorne; int * TypeDeVariable; char * BorneSupConnue; char * BorneInfConnue;
double * ValeurDeBorneSup; double * ValeurDeBorneInf; char BrnInfConnue; double S0; double S1;
PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; char UneVariableAEteFixee; char BorneMiseAJour;
double NouvelleValeur; char * AppliquerUneMargeEventuelle; 

*RefaireUnCycle = NON_PNE;   
if ( *Faisabilite == NON_PNE ) return;

# if UTILISER_LE_GRAPHE_DE_CONFLITS == OUI_PNE
  # if PROBING_JUSTE_APRES_LE_PRESOLVE == NON_PNE  
    return;
  # endif
# endif

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

if ( Pne->ProbingOuNodePresolve == NULL ) return;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

TypeDeVariable = Pne->TypeDeVariableTrav;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;
AppliquerUneMargeEventuelle = ContraintesDeBorneVariable->AppliquerUneMargeEventuelle;

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;

/* La variable continue est toujours placee en premier */

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {

  if ( First[Cnt] < 0 ) continue;
	
  ilcont = First[Cnt];
	ilbin = ilcont + 1;
	B = SecondMembre[Cnt];

	Varcont = Colonne[ilcont];	
	Varbin = Colonne[ilbin];
	
  BrnInfConnue = BorneInfConnue[Varbin];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) {			 			 
    /* La variable binaire est fixee */
		NouvelleValeur = ( B - ( Coefficient[ilbin] * ValeurDeBorneInf[Varbin] ) ) / Coefficient[ilcont];				
    if ( Coefficient[ilcont] > 0 ) {
		  /* La contrainte est une contrainte de borne sup */

      # if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == OUI_PNE			
			  if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			    if ( PNE_LaValeurEstEntiere( &NouvelleValeur ) == NON_PNE ) NouvelleValeur += MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES;
				}
			# endif
			
			if ( NouvelleValeur < ValeurDeBorneSup[Varcont] ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable %d on peut abaisser la borne sup de %e a %e\n",Varcont,ValeurDeBorneSup[Varcont],NouvelleValeur);
				# endif
        UneVariableAEteFixee = NON_PNE;
        BorneMiseAJour = MODIF_BORNE_SUP;
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varcont, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
			}			
			if ( ValeurDeBorneInf[Varcont] > NouvelleValeur + INFAISABILITE ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable %d pas de solution car NouvelleValeur = %e et ValeurDeBorneInf = %e\n",Varcont,NouvelleValeur,ValeurDeBorneInf[Varcont]);
				# endif
        *Faisabilite = NON_PNE;
		    return;		      				
			}
			else if ( fabs( ValeurDeBorneInf[Varcont] - NouvelleValeur ) < ZERFIX ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable %d fixee a ValeurDeBorneInf = %e\n",Varcont,ValeurDeBorneInf[Varcont]);
				# endif
				NouvelleValeur = 0.5 * ( ValeurDeBorneInf[Varcont] + NouvelleValeur );
        UneVariableAEteFixee = FIXATION_A_UNE_VALEUR;
        BorneMiseAJour = NON_PNE;
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varcont, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
			}			
		}		
		else {
		  /* La contrainte est une contrainte de borne inf */
			
      # if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == OUI_PNE			
			  if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			    if ( PNE_LaValeurEstEntiere( &NouvelleValeur ) == NON_PNE ) NouvelleValeur -= MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES;
				}
			# endif
			
			if ( NouvelleValeur > ValeurDeBorneInf[Varcont] ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable %d on peut remonter la borne inf de %e a %e\n",Varcont,ValeurDeBorneInf[Varcont],NouvelleValeur);
				# endif						
        UneVariableAEteFixee = NON_PNE;
        BorneMiseAJour = MODIF_BORNE_INF;
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varcont, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
      }					
			if ( ValeurDeBorneSup[Varcont] < NouvelleValeur - INFAISABILITE ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable %d pas de solution car NouvelleValeur = %e et ValeurDeBorneSup = %e\n",Varcont,NouvelleValeur,ValeurDeBorneSup[Varcont]);
				# endif						
        *Faisabilite = NON_PNE;
		    return;		      								
			}
			else if ( fabs( NouvelleValeur - ValeurDeBorneSup[Varcont] ) < ZERFIX ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable %d fixee a ValeurDeBorneSup = %e\n",Varcont,ValeurDeBorneSup[Varcont]);
				# endif
				NouvelleValeur = 0.5 * ( ValeurDeBorneSup[Varcont] + NouvelleValeur );
        UneVariableAEteFixee = FIXATION_A_UNE_VALEUR;
        BorneMiseAJour = NON_PNE;								
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varcont, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
			}			
		}
	}	
	else {
	
    /* La variable binaire n'est pas fixee */
		/* Fixation a 0 */
	  S0 = ( B - ( Coefficient[ilbin] * ValeurDeBorneInf[Varbin] ) ) / Coefficient[ilcont];
		/* Fixation a 1 */
	  S1 = ( B - ( Coefficient[ilbin] * ValeurDeBorneSup[Varbin] ) ) / Coefficient[ilcont];
		
    if ( Coefficient[ilcont] > 0 ) {
		  /* La contrainte est une contrainte de borne sup */
			/* Fixation a 0 */			
			if ( ValeurDeBorneInf[Varcont] > S0 + INFAISABILITE ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable entiere %d valeur 0 interdite\n",Varbin);
				# endif				
				/* La variable entiere est fixee a 1 */
        UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;
        BorneMiseAJour = NON_PNE;								
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varbin, ValeurDeBorneSup[Varbin], BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
				/* Passer a la contrainte suivante */
				continue;				
			}						
			/* Fixation a 1 */			
			if ( ValeurDeBorneInf[Varcont] > S1 + INFAISABILITE ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable entiere %d valeur 1 interdite\n",Varbin);
				# endif				
				/* La variable entiere est fixee a 0 */
        UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;
        BorneMiseAJour = NON_PNE;								
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varbin, ValeurDeBorneInf[Varbin], BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
				continue;				
			}
						
      if ( S0 < S1 ) NouvelleValeur = S1;
			else NouvelleValeur = S0;
			 
			# if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == OUI_PNE			
			  if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
          if ( PNE_LaValeurEstEntiere( &NouvelleValeur ) == NON_PNE ) NouvelleValeur += MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES;
				}
      # endif
		
			if ( NouvelleValeur < ValeurDeBorneSup[Varcont] ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable %d on peut abaisser la borne sup de %e a %e\n",Varcont,ValeurDeBorneSup[Varcont],NouvelleValeur);
				# endif				
				UneVariableAEteFixee = NON_PNE;
        BorneMiseAJour = MODIF_BORNE_SUP;
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varcont, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
			}			
						
		}
		else {
		  /* La contrainte est une contrainte de borne inf */
			/* Fixation a 0 */
			if ( ValeurDeBorneSup[Varcont] < S0 - INFAISABILITE ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable entiere %d valeur 0 interdite\n",Varbin);
				# endif				
 				/* La variable entiere est fixee a 1 */
        UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;
        BorneMiseAJour = NON_PNE;								
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varbin, ValeurDeBorneSup[Varbin], BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}			
				/* Passer a la contrainte suivante */
				continue;						
			}			
			/* Fixation a 1 */
			if ( ValeurDeBorneSup[Varcont] < S1 - INFAISABILITE ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable entiere %d valeur 1 interdite\n",Varbin);
				# endif				
 				/* La variable entiere est fixee a 0 */
        UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;
        BorneMiseAJour = NON_PNE;								
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varbin, ValeurDeBorneInf[Varbin], BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
				continue;						
			}
						
      if ( S0 < S1 ) NouvelleValeur = S0;
			else NouvelleValeur = S1;

			# if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == OUI_PNE			
			  if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			    if ( PNE_LaValeurEstEntiere( &NouvelleValeur ) == NON_PNE ) NouvelleValeur -= MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES;
				}
			# endif
			
			if ( NouvelleValeur > ValeurDeBorneInf[Varcont] ) {
				# if TRACES == OUI_PNE 
				  printf("Contrainte de borne variable: variable %d on peut remonter la borne inf de %e a %e\n",Varcont,ValeurDeBorneInf[Varcont],NouvelleValeur);
				# endif				
        UneVariableAEteFixee = NON_PNE;
        BorneMiseAJour = MODIF_BORNE_INF;
				*RefaireUnCycle = OUI_PNE;
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Varcont, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
					*Faisabilite = NON_PNE;
		      return;		        
				}				
      }
			
		}		
	}			 	
}

return;
}

/*----------------------------------------------------------------------------*/

# endif
