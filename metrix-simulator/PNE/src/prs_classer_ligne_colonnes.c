/***********************************************************************

   FONCTION: Classement des lignes et des colonnes en fonction du nombre
             de termes.
                
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

void PRS_ClasserLignesEtColonnesEnFonctionDuNombreDeTermes( PRESOLVE * Presolve )
{		      
int Var; PROBLEME_PNE * Pne; int NombreDeVariables; int NombreDeContraintes;
int NbT; int ic; int * ParColonnePremiereVariable; int * ParColonneVariableSuivante; int * Cdeb;
int * Csui; double * A; int * NumContrainte; char * ContrainteInactive; int * TypeDeBornePourPresolve;
int MxTrm; int il; int ilMax; int Cnt; int * ParLignePremiereContrainte; int * ParLigneContrainteSuivante;
int * Mdeb; int * NbTerm; int * Nuvar; double * BorneInfPourPresolve; double * BorneSupPourPresolve;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

/* Classement des colonnes en fonction du nombre de termes */
ParColonnePremiereVariable = Presolve->ParColonnePremiereVariable;
ParColonneVariableSuivante = Presolve->ParColonneVariableSuivante;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;
ContrainteInactive = Presolve->ContrainteInactive;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;  
for ( NbT = 0 ; NbT <= NombreDeContraintes ; NbT++ ) ParColonnePremiereVariable[NbT] = -1;
MxTrm = -1;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) continue;
	if ( BorneInfPourPresolve[Var] == BorneSupPourPresolve[Var] ) continue;	
  NbT = 0;
	ic = Cdeb[Var];
  while ( ic >= 0 ) {
		if ( A[ic] != 0 ) {
      if ( ContrainteInactive[NumContrainte[ic]] != OUI_PNE ) NbT++;			
		}
		ic = Csui[ic];
  }
	if ( NbT > MxTrm ) MxTrm = NbT;	
	ic = ParColonnePremiereVariable[NbT];	
	ParColonnePremiereVariable[NbT] = Var;	  
	ParColonneVariableSuivante[Var] = ic;	
}
Presolve->NbMaxTermesDesColonnes = MxTrm;

/* Classement des lignes en fonction du nombre de termes */
ParLignePremiereContrainte = Presolve->ParLignePremiereContrainte;
ParLigneContrainteSuivante = Presolve->ParLigneContrainteSuivante;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
for ( NbT = 0 ; NbT <= NombreDeVariables ; NbT++ ) ParLignePremiereContrainte[NbT] = -1;
MxTrm = -1;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  if ( ContrainteInactive[Cnt] == OUI_PNE ) continue;
  NbT = 0;
	il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
		if ( A[il] != 0 ) {
		  Var = Nuvar[il];
      if ( TypeDeBornePourPresolve[Var] != VARIABLE_FIXE && BorneInfPourPresolve[Var] != BorneSupPourPresolve[Var] ) NbT++;
		}
	  il++;
	}
	if ( NbT > MxTrm ) MxTrm = NbT;	
	il = ParLignePremiereContrainte[NbT]; 
	ParLignePremiereContrainte[NbT] = Cnt;	
	ParLigneContrainteSuivante[Cnt] = il;		
	
}
Presolve->NbMaxTermesDesLignes = MxTrm;

return;
}
