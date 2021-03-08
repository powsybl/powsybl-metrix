/***********************************************************************

   FONCTION: On compacte la matrice des contraintes en enlevant les
             termes qui correspondent a des variables fixes ou de
             borne inf et sup identiques.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif     

/*----------------------------------------------------------------------------*/
   
void PNE_CompacterLaMatriceDesContraintes( PROBLEME_PNE * Pne )
{
int Cnt; double S; int il; int ilMax; int Var; int NombreDeContraintes; int NbT; 
int * Mdeb; int * NbTerm; int * Nuvar; int * TypeDeBorne; char ChainageTransposeeExploitable;
double * A; double * X; double * Xmin ; double * Xmax; double * B; int * CntDeBorneSupVariable;
int * CntDeBorneInfVariable; int NombreDeVariables; int DerniereContrainte; char * SensContrainte;
int * CorrespondanceCntPneCntEntree;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
B = Pne->BTrav;
SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
ChainageTransposeeExploitable = Pne->ChainageTransposeeExploitable;
CorrespondanceCntPneCntEntree = Pne->CorrespondanceCntPneCntEntree;

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
	S = 0;
	il = Mdeb[Cnt];	
	NbT = NbTerm[Cnt];
	ilMax = il + NbT;	
	while ( il < ilMax ) {
	  /* Pour eviter de supprimer une contrainte en entier */
		/*if ( NbT <= 1 ) break;*/  /* Modif du 3/11/2016: on s'autorise a supprimer les contraintes sans variables */
		Var = Nuvar[il];			
		if ( TypeDeBorne[Var] == VARIABLE_FIXE ) {		
		  S += A[il] * X[Var];
			Nuvar[il] = Nuvar[ilMax-1];			
			A[il] = A[ilMax-1];
			il--;
			ilMax--;
			NbT--;			
		}
		else if ( Xmin[Var] == Xmax[Var] ) {		
		  S += A[il] * Xmin[Var];
			Nuvar[il] = Nuvar[ilMax-1];
			A[il] = A[ilMax-1];
			il--;
			ilMax--;
		 	NbT--;			
		}
		else if ( A[il] == 0.0 ) {		
			Nuvar[il] = Nuvar[ilMax-1];
			A[il] = A[ilMax-1];
			il--;
			ilMax--;
		 	NbT--;			
		}		
    il++;
  }
	B[Cnt] -= S;
	if ( NbT != NbTerm[Cnt] ) ChainageTransposeeExploitable = NON_PNE;
	NbTerm[Cnt] = NbT;

  if ( NbTerm[Cnt] <= 0 ) {	
    /* Test d'infaisabilite */
		if ( SensContrainte[Cnt] == '=' ) {
      if ( fabs( B[Cnt] ) > SEUIL_DADMISSIBILITE ) {				
        Pne->YaUneSolution = PROBLEME_INFAISABLE;
			  return;
		  }
		}
		else { /* Sens < */		
      if ( B[Cnt] < -SEUIL_DADMISSIBILITE ) {			
        Pne->YaUneSolution = PROBLEME_INFAISABLE;
			  return;
			}
		}
	
    /* On supprime la contrainte */
		/* On inverse avec la derniere contrainte */

		/*printf("******* PNE_CompacterLaMatriceDesContraintes suppression Cnt %d\n",Cnt);*/
		
    Pne->NumeroDesContraintesInactives[Pne->NombreDeContraintesInactives] = CorrespondanceCntPneCntEntree[Cnt];
    Pne->NombreDeContraintesInactives++;

 	  DerniereContrainte = NombreDeContraintes - 1;				
 	  Mdeb[Cnt]   = Mdeb[DerniereContrainte];
    NbTerm[Cnt] = NbTerm[DerniereContrainte];				
    B[Cnt]      = B[DerniereContrainte];
    SensContrainte[Cnt] = SensContrainte[DerniereContrainte];
    CorrespondanceCntPneCntEntree[Cnt] = CorrespondanceCntPneCntEntree[DerniereContrainte];
    ChainageTransposeeExploitable = NON_PNE;
	  Cnt--;
	  NombreDeContraintes--;
	  Pne->NombreDeContraintesTrav = NombreDeContraintes;

	}
	
}

Pne->ChainageTransposeeExploitable = ChainageTransposeeExploitable;

/* On cree la transposee si besoin */
if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

CntDeBorneSupVariable = Pne->CntDeBorneSupVariable;
CntDeBorneInfVariable = Pne->CntDeBorneInfVariable;
if ( CntDeBorneSupVariable == NULL && CntDeBorneInfVariable == NULL ) goto Fin;

/* Ci dessous: ne sert plus a rien */

NombreDeVariables = Pne->NombreDeVariablesTrav;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
	if ( CntDeBorneSupVariable != NULL ) {	
    Cnt = CntDeBorneSupVariable[Var];
		if ( Cnt >= 0 ) {
		  if ( NbTerm[Cnt] != 2 ) CntDeBorneSupVariable[Var] = -1;
		}
	}
	if ( CntDeBorneInfVariable != NULL ) {	
    Cnt = CntDeBorneInfVariable[Var];
		if ( Cnt >= 0 ) {
		  if ( NbTerm[Cnt] != 2 ) CntDeBorneInfVariable[Var] = -1;
		}
	}
}

Fin:

return;
}
