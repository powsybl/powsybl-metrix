/***********************************************************************

   FONCTION: Substitutions de variables dans la matrice des contrainte.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES 1

/*----------------------------------------------------------------------------*/

int PNE_SubstituerUneVariableDansLaMatrice( PROBLEME_PNE * Pne, int Mode, int VarSubstituee, int VarRestante, double ATilde, double BTilde,
                                            int Simulation, int Remplacement, int SubstitutionPossible, int SubstitutionImpossible ) 
{
int il; double Ai; int Cnt; int ilCnt; int ilMaxCnt; char VarRestanteTrouvee; 
int ilR; int ilPrec; int * Cdeb; int * NumContrainte; int * Mdeb; int * NbTerm; 
int * Nuvar; int * Csui; double * A; double * B; double PlusPetitTerme; double PlusGrandTerme;
double X;

PlusPetitTerme = Pne->PlusPetitTerme;
PlusGrandTerme = Pne->PlusGrandTerme;

Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
B = Pne->BTrav;

/* On balaye la colonne de la variable substituee */

if ( Mode == Simulation ) {

  il  = Cdeb[VarSubstituee];
  while ( il >= 0 ) {
    Cnt = NumContrainte[il];	
    VarRestanteTrouvee = NON_PNE;	
    Ai = A[il];		
    if ( Ai != 0.0 ) {	
      /* On balaye la contrainte pour faire les actions necessaires au cas ou 
         la variable VarRestante y serait deja presente */
      ilCnt    = Mdeb[Cnt];
      ilMaxCnt = ilCnt + NbTerm[Cnt];
      while ( ilCnt < ilMaxCnt ) {
        if ( Nuvar[ilCnt] == VarRestante ) {
				  X = A[ilCnt] - ( Ai * ATilde );					
          if ( X != 0.0 ) {
				    if ( fabs( X ) > PlusGrandTerme ) return( SubstitutionImpossible );
				    else if ( fabs( X ) < PlusPetitTerme ) return( SubstitutionImpossible );
          }				 
          VarRestanteTrouvee = OUI_PNE;  
          break;
				}       
        ilCnt++;
			}   
    }
		
    ilPrec = il;
    il = Csui[il];

    if ( VarRestanteTrouvee == NON_PNE && Ai != 0.0 ) {
      X = -Ai * ATilde;
			if ( X != 0.0 ) {
		    if ( fabs( X ) > PlusGrandTerme ) return( SubstitutionImpossible );
		    else if ( fabs( X ) < PlusPetitTerme ) return( SubstitutionImpossible );
			}      
		}
	}
}
else if ( Mode == Remplacement ) { 
  il  = Cdeb[VarSubstituee];
  while ( il >= 0 ) {
    Cnt = NumContrainte[il];		
    VarRestanteTrouvee = NON_PNE;	
    Ai = A[il];		
    if ( Ai != 0.0 ) {	
      /* On balaye la contrainte pour faire les actions necessaires au cas ou 
         la variable VarRestante y serait deja presente */
      ilCnt    = Mdeb[Cnt];
      ilMaxCnt = ilCnt + NbTerm[Cnt];
      while ( ilCnt < ilMaxCnt ) {
        if ( Nuvar[ilCnt] == VarRestante ) {				
          A[ilCnt] -= Ai * ATilde;

					/*
				  if ( fabs( A[ilCnt] ) < 1.e-8 && A[ilCnt] != 0 ) {
						printf("A[ilCnt] %e Sens %c\n",A[ilCnt],Pne->SensContrainteTrav[Cnt]);
					}
					*/
				  if ( fabs( A[ilCnt] ) < 1.e-8 && A[ilCnt] != 0 ) {
						/*printf("Annulation A[ilCnt] %e Sens %c\n",A[ilCnt],Pne->SensContrainteTrav[Cnt]);*/
						A[ilCnt] = 0;
					}
				  
						           															
          VarRestanteTrouvee = OUI_PNE;  
          break;
        } 
       ilCnt++;
      }			
		  B[Cnt] -= Ai * (long double) BTilde;			
    }

    ilPrec = il;
    il = Csui[il];

    if ( VarRestanteTrouvee == NON_PNE && Ai != 0.0 ) {     		  
      Nuvar[ilPrec] = VarRestante;				
      A[ilPrec] = -Ai * ATilde;			
      /* Attention comme on a change la colonne d'un terme il faut modifier le chainage 
         de la colonne VarRestante */
      ilR = Cdeb[VarRestante];
      Cdeb[VarRestante] = ilPrec; 
      Csui[ilPrec] = ilR;		
    }
    else {
      /* On peut mettre le coefficient de la variable substituee a 0 s'il y avait la 
         variable restante dans la contrainte */
      A[ilPrec] = 0.;  
    }		
  }
}
else {
  printf("BUG dans PNE_SubstituerUneVariableDansLaMatrice\n");
}

return( SubstitutionPossible );

}

/*----------------------------------------------------------------------------*/


