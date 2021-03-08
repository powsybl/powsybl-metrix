/***********************************************************************

   FONCTION: Detection des contraintes toujours inactives.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# define TRACES 0

/*----------------------------------------------------------------------------*/

void PRS_ContraintesToujoursInactives( PRESOLVE * Presolve, int * NbCntInact )
{
int Cnt; int Nb; double Seuil; PROBLEME_PNE * Pne; int NombreDeContraintes; 
double * MaxContrainte; double * B; double * MinContrainte; int * Nuvar;
char * ContrainteInactive; char * SensContrainte; char * MaxContrainteCalcule;
char * MinContrainteCalcule; int * Mdeb; int * NbTerm;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

NombreDeContraintes = Pne->NombreDeContraintesTrav;

ContrainteInactive = Presolve->ContrainteInactive;
SensContrainte = Pne->SensContrainteTrav;

MaxContrainte = Presolve->MaxContrainte;
MinContrainte = Presolve->MinContrainte;
B         = Pne->BTrav;
MaxContrainteCalcule = Presolve->MaxContrainteCalcule;
MinContrainteCalcule = Presolve->MinContrainteCalcule;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;

/* On regarde si certaines contraintes sont toujours satisfaites */
Seuil = 0.05 * SEUIL_DADMISSIBILITE;

for ( Nb = 0 , Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {

  if ( ContrainteInactive[Cnt] == OUI_PNE ) continue;	
	
  /* A ce stade toutes les contraintes sont soit en = soit en < */

  if ( SensContrainte[Cnt] == '<' ) {
    /*if ( MaxContrainte[Cnt] <= B[Cnt] && MaxContrainteCalcule[Cnt] == OUI_PNE ) {*/
    if ( MaxContrainte[Cnt] < B[Cnt] + Seuil && MaxContrainteCalcule[Cnt] == OUI_PNE ) {		
		  # if TRACES == 1
			  printf("Contrainte d'inegalite %d desactivee car ne peut etre jamais atteinte\n",Cnt);
			# endif
      Nb++ ;
      PRS_DesactiverContrainte( Presolve, Cnt );			
    }		
  }
  else { /* Contrainte d'egalite */	  
    if ( fabs( MaxContrainte[Cnt] - B[Cnt] ) < Seuil && fabs( MinContrainte[Cnt] - B[Cnt] ) < Seuil && 
			   MaxContrainteCalcule[Cnt] == OUI_PNE        && MinContrainteCalcule[Cnt] == OUI_PNE ) {
		  # if TRACES == 1
			  printf("Contrainte d'egalite %d desactivee car ne peut etre jamais atteinte\n",Cnt);
			# endif				 
      Nb++ ;
      PRS_DesactiverContrainte( Presolve, Cnt );	
    }       
  }
	
}

#if VERBOSE_PRS == 1
  printf("-> Nombre de contraintes supprimees car jamais actives %d\n",Nb);
  fflush(stdout);	
#endif

*NbCntInact = Nb;

return;
}  



