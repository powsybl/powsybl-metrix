/***********************************************************************

   FONCTION: On applique les contraintes de bornes variables dans le cas
	           ou le presolve a ete appele apres le probing.
                
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
																				
/*----------------------------------------------------------------------------*/
/* On cherche simplement a savoir s'il y a une contrainte de borne variable
   sur la variable continue */
	 
void PRS_AppliquerLeContraintesDeBornesVariables( PRESOLVE * Presolve ) 
{
int Cnt; int * First; int Varcont; int * Colonne; double Zero; double * SecondMembre;
double * Coefficient; int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
int NombreDeContraintesDeBorne; double ValeurDeVarBin; double NouvelleValeur;
int ilVarBin; int ilVarcont; double * Xmax; double * Xmin; double * X; int * TypeDeBorne; 
PROBLEME_PNE * Pne;

return; /* Ca marche pas et je sais pas pourquoi meme si on l'applique aux variables natives plutot
           qu'a celle du presolve */

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
if ( Pne->ContraintesDeBorneVariable == NULL ) return;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;

TypeDeBorne = Presolve->TypeDeBornePourPresolve;
X = Presolve->ValeurDeXPourPresolve;
Xmin = Presolve->BorneInfPourPresolve;
Xmax = Presolve->BorneSupPourPresolve;

Zero = 1.e-8;

/* La variable continue est toujours placee en premier */

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;	
  ilVarcont = First[Cnt];
	ilVarBin = ilVarcont + 1;
	
	Varcont = Colonne[ilVarcont];
	if ( TypeDeBorne[Varcont] == VARIABLE_FIXE || Xmin[Varcont] == Xmax[Varcont] ) continue;
	
	Varbin = Colonne[ilVarBin];
	if ( TypeDeBorne[Varbin] == VARIABLE_FIXE ) { ValeurDeVarBin = X[Varbin]; goto TestDeLaContrainteDeBorneVariable; }
	if ( Xmin[Varbin] == Xmax[Varbin] ) { ValeurDeVarBin = Xmin[Varbin]; goto TestDeLaContrainteDeBorneVariable; }
			
  continue;
	
	TestDeLaContrainteDeBorneVariable:
		
	NouvelleValeur = ( SecondMembre[Cnt] - ( Coefficient[ilVarBin] * ValeurDeVarBin ) ) / Coefficient[ilVarcont];
	
  /* La variable binaire est fixee */
  if ( Coefficient[ilVarcont] > 0 ) {
		/* La contrainte est une contrainte de borne sup */
		if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
		  if ( NouvelleValeur < Xmax[Varcont] ) {
			  printf("Variable %d Xmax %e -> %e\n",Varcont,Xmax[Varcont],NouvelleValeur);
			  Xmax[Varcont] = NouvelleValeur;
				First[Cnt] = -1;
			}
		}
		else {
		  /*
			printf("Variable %d Xmax %e -> %e\n",Varcont,Xmax[Varcont],NouvelleValeur);	
      Xmax[Varcont] = NouvelleValeur;
			if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_SUPERIEUREMENT;
			else if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
			*/
		}												  
	}
	else {
		/* La contrainte est une contrainte de borne inf */
		if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) {
		  if ( NouvelleValeur > Xmin[Varcont] ) {
			  printf("Variable %d Xmin %e -> %e\n",Varcont,Xmin[Varcont],NouvelleValeur);				
		    Xmin[Varcont] = NouvelleValeur;
				First[Cnt] = -1;
			}
		}
		else {
		  /*
			printf("Variable %d Xmin %e -> %e\n",Varcont,Xmin[Varcont],NouvelleValeur);					
      Xmin[Varcont] = NouvelleValeur;
			if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_INFERIEUREMENT;
			else if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES ;
			*/
		}				    					
	}
	
	if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES ) {	
	  if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < Zero ) {
      Xmin[Varcont] = 0.5 * ( Xmax[Varcont] + Xmin[Varcont] );
			Xmax[Varcont] = Xmin[Varcont];
			X[Varcont] = Xmin[Varcont];
			/*
			printf("Variable %d fixee a %e\n",Varcont,Xmin[Varcont]);								
			TypeDeBorne[Varcont] = VARIABLE_FIXE;
			*/
			First[Cnt] = -1;
		}
	}
  	 				 			
}

return;
}

