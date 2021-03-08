/***********************************************************************

   FONCTION: Mise a jour de l'indicateur d'activite d'une contrainte.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# define TRACES 0

/*----------------------------------------------------------------------------*/
              
void PRS_DesactiverContrainte( PRESOLVE * Presolve, int Cnt )
{
int il; int ilMax; int Var; int * Nuvar; int * Mdeb; int * NbTerm; PROBLEME_PNE * Pne;
char * ConserverLaBorneSupDuPresolve; char * ConserverLaBorneInfDuPresolve;
int * ContrainteBornanteSuperieurement; int * ContrainteBornanteInferieurement;
char * ContrainteInactive;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

ConserverLaBorneSupDuPresolve = Presolve->ConserverLaBorneSupDuPresolve;
ConserverLaBorneInfDuPresolve = Presolve->ConserverLaBorneInfDuPresolve;
ContrainteBornanteSuperieurement = Presolve->ContrainteBornanteSuperieurement;
ContrainteBornanteInferieurement = Presolve->ContrainteBornanteInferieurement;
ContrainteInactive = Presolve->ContrainteInactive;

Nuvar = Pne->NuvarTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;

il = Mdeb[Cnt];
ilMax = il + NbTerm[Cnt];
while ( il < ilMax ) {	
  Var = Nuvar[il];	
  /* Si la contrainte etait bornante, on impose de conserver les bornes du presolve */
	/* Remarque: si la variable est a nouveau bornee par une contrainte activable, cet
	   indicateur repasse a NON_PNE */
  if ( ContrainteBornanteSuperieurement[Var] == Cnt ) {
	  ConserverLaBorneSupDuPresolve[Var] = OUI_PNE;
		ContrainteBornanteSuperieurement[Var] = -1;		
	}
  if ( ContrainteBornanteInferieurement[Var] == Cnt ) {
	  ConserverLaBorneInfDuPresolve[Var] = OUI_PNE;
		ContrainteBornanteInferieurement[Var] = -1;
	}
  il++;		
}
# if TRACES == 1
  printf("PRS_DesactiverContrainte: contrainte %d desactivee sens %c second membre %e\n",Cnt,Pne->SensContrainteTrav[Cnt],Pne->BTrav[Cnt]);  
# endif

ContrainteInactive[Cnt] = OUI_PNE;
Pne->NumeroDesContraintesInactives[Pne->NombreDeContraintesInactives] = Pne->CorrespondanceCntPneCntEntree[Cnt];
Pne->NombreDeContraintesInactives++;

return;
}
