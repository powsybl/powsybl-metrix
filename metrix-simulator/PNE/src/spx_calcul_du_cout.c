/***********************************************************************

   FONCTION: Utilisable surtout dans un contexte de Branch and Bound.
             On calcule le cout de la solution primale et on le compare 
             au seuil fourni en entrée (qui dans un contexte de 
             Branch and Bound est le cout de la meilleure solution 
             entiere deja trouvee). Si le cout calculé est superieur
             au seuil fourni en entree, on arrete les calculs et on 
             sort avec le verdict: pas de solution. En effet le cout
             courant est un minorant du cout optimal.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_define.h"
# include "spx_fonctions.h"

# include "pne_define.h"

/*----------------------------------------------------------------------------*/

void SPX_CalculDuCout( PROBLEME_SPX * Spx )
{
int i; double Cout; double * C; double * X;  
PROBLEME_PNE * Pne; 

SPX_FixerXEnFonctionDeSaPosition( Spx );
C = Spx->C; 
X = Spx->X;
Cout = 0.0;
for ( i = 0 ; i < Spx->NombreDeVariables ; i++ ) {	 
  Cout+= C[i] * X[i]; 
} 

Cout/= Spx->ScaleLigneDesCouts;
Cout+= Spx->PartieFixeDuCout;

Spx->Cout = Cout;

Pne = (PROBLEME_PNE *) Spx->ProblemePneDeSpx;
if ( Pne != NULL ) {
  Spx->Cout += Pne->Z0;	
}

#if VERBOSE_SPX
  if ( Spx->StrongBranchingEnCours != OUI_SPX ) {  
    if ( Spx->UtiliserCoutMax == OUI_SPX ) { 
      printf("Iteration %5d Cout %20.6lf Infaisabilites primales %20.6lf PartieFixeDuCout %20.6lf CoutMax %20.6lf\n",
              Spx->Iteration,Spx->Cout,Spx->SommeDesInfaisabilitesPrimales,Spx->PartieFixeDuCout,Spx->CoutMax);
    }
    else {
      printf("Iteration %5d Cout %20.6lf Infaisabilites primales %20.6lf PartieFixeDuCout %20.6lf\n",
              Spx->Iteration,Spx->Cout,Spx->SommeDesInfaisabilitesPrimales,Spx->PartieFixeDuCout);
    }
  }
#else
  /* Cas non verbose */		 
  if ( Spx->LaBaseDeDepartEstFournie == NON_SPX && Spx->AffichageDesTraces == OUI_SPX ) { /* Premier simplexe */
    if ( Spx->EcrireLegendePhase2 == OUI_SPX ) {
      Spx->EcrireLegendePhase1 = OUI_SPX;   
      Spx->EcrireLegendePhase2 = NON_SPX;   
      printf(" ");
      printf(" | Phase |");
      printf(" Iteration |");
      printf("         Objective        |");
      printf("          Primal infeas.       |");
      printf("     Primal infeas. count   |");			
      printf("\n");               
    }          
    printf(" ");
    printf(" |   II  |");
    printf("   %6d  |",Spx->Iteration);
    printf("      %16.9e    |",Spx->Cout);
    printf("        %15.8e        |",Spx->SommeDesInfaisabilitesPrimales);    
    printf("         %10d         |",Spx->NombreDeContraintesASurveiller);    
    printf("\n");            
  }
#endif

return;
}

