/***********************************************************************

   FONCTION: Initialisations pour le presolve
                
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

void PRS_InitPresolve( PRESOLVE * Presolve )
{		      
int Var; int NbCntAlloc; PROBLEME_PNE * Pne; int NombreDeVariables; int NombreDeContraintes;
int NbT; int ic; int * ParColonnePremiereVariable; int * ParColonneVariableSuivante; int * Cdeb;
int * Csui; double * A; int * NumContrainte; char * ContrainteInactive; int * TypeDeBornePourPresolve;
int MxTrm; int il; int ilMax; int Cnt; int * ParLignePremiereContrainte; int * ParLigneContrainteSuivante;
int * Mdeb; int * NbTerm; int * Nuvar; char * SensContrainte;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

NbCntAlloc = Pne->NombreDeContraintesAllouees;
NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

memset( (char *) Presolve->ConnaissanceDeLambda , LAMBDA_NON_INITIALISE, NombreDeContraintes * sizeof( char ) );	
memset( (char *) Presolve->ContrainteInactive, NON_PNE, NombreDeContraintes * sizeof( char ) );

SensContrainte = Pne->SensContrainteTrav;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  Presolve->LambdaMin[Cnt] = -LINFINI_PNE;
	Presolve->LambdaMax[Cnt] = LINFINI_PNE;
	if ( SensContrainte[Cnt] == '<' ) {	
    Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;
	  Presolve->LambdaMax[Cnt] = 0;
	}
}

memset( (char *) Presolve->ConserverLaBorneSupDuPresolve, NON_PNE, NombreDeVariables * sizeof( char ) );
memset( (char *) Presolve->ConserverLaBorneInfDuPresolve, NON_PNE, NombreDeVariables * sizeof( char ) );

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  Presolve->ContrainteBornanteSuperieurement[Var] = -1;
  Presolve->ContrainteBornanteInferieurement[Var] = -1;	
}

memcpy( (char *) Presolve->ValeurDeXPourPresolve, (char *) Pne->UTrav, NombreDeVariables * sizeof( double ) );
memcpy( (char *) Presolve->BorneInfPourPresolve, (char *) Pne->UminTrav, NombreDeVariables * sizeof( double ) );
memcpy( (char *) Presolve->BorneSupPourPresolve, (char *) Pne->UmaxTrav, NombreDeVariables * sizeof( double ) );
memcpy( (char *) Presolve->TypeDeBornePourPresolve, (char *) Pne->TypeDeBorneTrav, NombreDeVariables * sizeof( int ) );

memset( (char *) Presolve->TypeDeValeurDeBorneInf, VALEUR_NATIVE, NombreDeVariables * sizeof( char ) );
memset( (char *) Presolve->TypeDeValeurDeBorneSup, VALEUR_NATIVE, NombreDeVariables * sizeof( char ) );

if ( Pne->ContraintesDeBorneVariable != NULL && 0 ) {
 /* Ca marche pas et je sais pas pourquoi meme si on l'applique aux variables natives plutot qu'a celle du presolve */
  PRS_AppliquerLeContraintesDeBornesVariables( Presolve );
}

/* Classement des colonnes en fonction du nombre de termes */
ParColonnePremiereVariable = Presolve->ParColonnePremiereVariable;
ParColonneVariableSuivante = Presolve->ParColonneVariableSuivante;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
ContrainteInactive = Presolve->ContrainteInactive;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;  
for ( NbT = 0 ; NbT <= NombreDeContraintes ; NbT++ ) ParColonnePremiereVariable[NbT] = -1;
MxTrm = -1;
ic = -1; 
/*
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) continue;
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
*/

/* Classement des lignes en fonction du nombre de termes */
ParLignePremiereContrainte = Presolve->ParLignePremiereContrainte;
ParLigneContrainteSuivante = Presolve->ParLigneContrainteSuivante;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
for ( NbT = 0 ; NbT <= NombreDeVariables ; NbT++ ) ParLignePremiereContrainte[NbT] = -1;
MxTrm = -1;
il = -1;
ilMax = -1;
/*
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  if ( ContrainteInactive[Cnt] == OUI_PNE ) continue;
  NbT = 0;
	il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
		if ( A[il] != 0 ) {
      if ( TypeDeBornePourPresolve[Nuvar[il]] != VARIABLE_FIXE ) NbT++;
		}
	  il++;
	}
	if ( NbT > MxTrm ) MxTrm = NbT;	
	il = ParLignePremiereContrainte[NbT]; 
	ParLignePremiereContrainte[NbT] = Cnt;	
	ParLigneContrainteSuivante[Cnt] = il;			
}
Presolve->NbMaxTermesDesLignes = MxTrm;
*/
return;
}
