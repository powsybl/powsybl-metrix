/***********************************************************************

   FONCTION: On substitutions de variables dans les coupes de probing.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
  
# include "bb_define.h"

# include "prs_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

/*----------------------------------------------------------------------------*/
/* Peut etre ameliore en exploitant un chainage par colonne */

int PNE_SubstituerUneVariableDansCoupesDeProbing ( PROBLEME_PNE * Pne, int Mode, int VarSubstituee, int VarRestante, double ATilde, double BTilde,
                                                   int Simulation, int Remplacement, int SubstitutionPossible, int SubstitutionImpossible )																					
{
int ilCnt; int ilMaxCnt;  COUPES_DE_PROBING * CoupesDeProbing; int NumCoupe; int ilVarRestante;
int ilVarSubstituee; int * First; int * NbElements; int * Colonne; double * Coefficient;
double * SecondMembre; double PlusPetitTerme; double PlusGrandTerme; double X;

CoupesDeProbing = Pne->CoupesDeProbing;

if ( CoupesDeProbing == NULL ) return( SubstitutionPossible );
if ( CoupesDeProbing->NombreDeCoupesDeProbing <= 0 ) return( SubstitutionPossible );

PlusPetitTerme = Pne->PlusPetitTerme;
PlusGrandTerme = Pne->PlusGrandTerme;

First = CoupesDeProbing->First;
NbElements = CoupesDeProbing->NbElements;
SecondMembre = CoupesDeProbing->SecondMembre;
Colonne = CoupesDeProbing->Colonne;
Coefficient = CoupesDeProbing->Coefficient;	

for ( NumCoupe = 0 ; NumCoupe < CoupesDeProbing->NombreDeCoupesDeProbing ; NumCoupe++ ) {
  ilCnt    = First[NumCoupe];
  ilMaxCnt = ilCnt + NbElements[NumCoupe];
	ilVarRestante = -1;
	ilVarSubstituee = -1;
  while ( ilCnt < ilMaxCnt ) {
		if ( Colonne[ilCnt] == VarRestante ) ilVarRestante = ilCnt;	
		if ( Colonne[ilCnt] == VarSubstituee ) ilVarSubstituee = ilCnt;	      
    ilCnt++;
  }  
  if ( Mode == Simulation ) {
    if ( ilVarSubstituee >= 0 ) {
      if ( ilVarRestante >= 0 ) {
        X = Coefficient[ilVarRestante] - ( Coefficient[ilVarSubstituee] * (long double) ATilde );
				if ( X != 0.0 ) {
			    if ( fabs( X ) > PlusGrandTerme ) return( SubstitutionImpossible );
			    else if ( fabs( X ) < PlusPetitTerme ) return( SubstitutionImpossible );
				}
			}		  		  		
		  else {
        X = -Coefficient[ilVarSubstituee] * (long double) ATilde;
			  if ( X != 0.0 ) {
			    if ( fabs( X ) > PlusGrandTerme ) return( SubstitutionImpossible );
			    else if ( fabs( X ) < PlusPetitTerme ) return( SubstitutionImpossible );
				}
			}
		}
	}
  else if ( Mode == Remplacement ) { 	
    if ( ilVarSubstituee >= 0 ) {
      SecondMembre[NumCoupe] -= Coefficient[ilVarSubstituee] * (long double) BTilde;		
      if ( ilVarRestante >= 0 ) {        	  
        Coefficient[ilVarRestante] -= Coefficient[ilVarSubstituee] * (long double) ATilde;			 
			  /* Il faut enlever un terme */
        Coefficient[ilVarSubstituee] = Coefficient[ilMaxCnt-1];			 			 
			  Colonne[ilVarSubstituee] = Colonne[ilMaxCnt-1];
			  NbElements[NumCoupe]--;			
		  }
		  else {		  
        Coefficient[ilVarSubstituee] = -Coefficient[ilVarSubstituee] * (long double) ATilde;			 
        Colonne[ilVarSubstituee] = VarRestante;			
		  }
	  }
  }
  else {
    printf("BUG dans PNE_SubstituerUneVariableDansCoupesDeProbing\n");
  }	
}

return( SubstitutionPossible );

}

/*************************************************************************/


