/***********************************************************************

   FONCTION: Appele par le branch and bound pour verifier si la solution
	           entiere trouvee satisfait toutes les contraintes.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "bb_define.h"
# include "bb_fonctions.h"

# define TOLERANCE_SUR_LES_CONTRAINTES_EN_VALEUR_ABSOLUE  1.e-3

# define TRACES 1

/*----------------------------------------------------------------------------*/

void PNE_VerifierAdmissibiliteDeLaSolutionCourante( PROBLEME_PNE * Pne, int * LaSolutionEstAdmissible ) 
{
int Cnt; int NombreDeContraintes; double S; double * L; double * U; char * SensContrainte;
double * B; int * Mdeb; int * NbTerm; int * Nuvar; double * A; int il; int ilMax;
double ToleranceViolation;

*LaSolutionEstAdmissible = OUI;

return; /* A faire: toujours accepter si la variable duale de la contrainte violee est nulle */

NombreDeContraintes = Pne->NombreDeContraintesTrav;
 
L = Pne->LTrav;
U = Pne->UTrav;

SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

ToleranceViolation = TOLERANCE_SUR_LES_CONTRAINTES_EN_VALEUR_ABSOLUE;

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  S = 0.;
  il    = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
    S += A[il] * U[Nuvar[il]];		
    il++;
  }	
  if ( SensContrainte[Cnt] == '=' ) {
    if ( fabs( S - B[Cnt] ) > ToleranceViolation ) {		
      *LaSolutionEstAdmissible = NON;
			# if TRACES == 1
			  printf("La solution entiere n'est pas admissible pour la contrainte d'egalite %d (entree %d):\n",Cnt,Pne->CorrespondanceCntPneCntEntree[Cnt]);
				printf(" Second membre %e  valeur calculee %e  ecart %e  Lamdba*ecart = %e\n",B[Cnt],S,fabs( S - B[Cnt] ),
				         Pne->VariablesDualesDesContraintesTravEtDesCoupes[Cnt] * fabs( S - B[Cnt] ) );
			# endif			
		  break;
    }   
  }
  else if ( SensContrainte[Cnt] == '<' ) {
    if ( S > B[Cnt] + ToleranceViolation ) {
      *LaSolutionEstAdmissible = NON;
			# if TRACES == 1
			  printf("La solution entiere n'est pas admissible pour la contrainte d'inegalite %d de type < (entree %d):\n",Cnt,Pne->CorrespondanceCntPneCntEntree[Cnt]);
				printf(" Second membre %e  valeur calculee %e  ecart %e  Lamdba*ecart = %e\n",B[Cnt],S,S-B[Cnt],
				         Pne->VariablesDualesDesContraintesTravEtDesCoupes[Cnt] * (S-B[Cnt]));				
			# endif			
		  break;
    }
  }
  else if ( SensContrainte[Cnt] == '>' ) {
    if ( S < B[Cnt] - ToleranceViolation ) {		
      *LaSolutionEstAdmissible = NON;
			# if TRACES == 1
			  printf("La solution entiere n'est pas admissible pour la contrainte d'inegalite %d de type > (entree %d):\n",Cnt,Pne->CorrespondanceCntPneCntEntree[Cnt]);
				printf(" Second membre %e  valeur calculee %e  ecart %e  Lamdba*ecart = %e\n",B[Cnt],S,B[Cnt]-S,
				         Pne->VariablesDualesDesContraintesTravEtDesCoupes[Cnt] * (B[Cnt]-S));
			# endif			
		  break;				
    }
  }
}

return;
}
