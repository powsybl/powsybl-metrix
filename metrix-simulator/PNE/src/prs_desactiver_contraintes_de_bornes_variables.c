
/***********************************************************************

   FONCTION: Desactivation de contraintes de bornes variables quand
	           on fixe ou quand on supprime une variable.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_define.h"
		
# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "prs_memoire.h"
# endif

# define ABCS 0

/*----------------------------------------------------------------------------*/

void PRS_DesactiverContraintesDeBorneVariable( void * PneE, int Var, int Var1 )
{
int Cnt; int ilVarcont; int ilVarBin; int * First; int * Colonne; PROBLEME_PNE * Pne;

Pne = (PROBLEME_PNE *) PneE;

# if ABCS == 1

if ( Pne->VariableDansContrainteDeBorneVariable != NULL ) { /* car VariableDansContrainteDeBorneVariable n'existe pas */
  if ( Pne->VariableDansContrainteDeBorneVariable[Var] == OUI_PNE ) {
    /* On desactive toutes les contraintes concernees */
    First = Pne->ContraintesDeBorneVariable->First;
    Colonne = Pne->ContraintesDeBorneVariable->Colonne;
    for ( Cnt = 0 ; Cnt < Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne ; Cnt++ ) {
      if ( First[Cnt] < 0 ) continue;	
      ilVarcont = First[Cnt];
	    ilVarBin = ilVarcont + 1;
			if ( Colonne[ilVarBin] == Var || Colonne[ilVarcont] == Var ) First[Cnt] = -1;
			else if ( Colonne[ilVarBin] == Var1 || Colonne[ilVarcont] == Var1 ) First[Cnt] = -1;
    }				 		
	}
}

# endif

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

First = Pne->ContraintesDeBorneVariable->First;
Colonne = Pne->ContraintesDeBorneVariable->Colonne;

for ( Cnt = 0 ; Cnt <  Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;	
  ilVarcont = First[Cnt];
	ilVarBin = ilVarcont + 1;
	if ( Colonne[ilVarBin] == Var || Colonne[ilVarcont] == Var ) First[Cnt] = -1;
	else if ( Colonne[ilVarBin] == Var1 || Colonne[ilVarcont] == Var1 ) First[Cnt] = -1;
}
	
return;
}
