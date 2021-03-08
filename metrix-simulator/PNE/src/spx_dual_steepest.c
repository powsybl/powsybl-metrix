/***********************************************************************

   FONCTION: 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"  

# include "lu_define.h"
# include "lu_fonctions.h"

# define SEUIL_CARRE_DE_NORME_TROP_FAIBLE  1.e-4 /*1.e-4*/
# define DUAL_POIDS_INITIAL 1.0

/*----------------------------------------------------------------------------*/
void SPX_InitDualPoids( PROBLEME_SPX * Spx )
{
int Var; int Cnt; char * PositionDeLaVariable; char * InDualFramework;
double * DualPoids; double * Tau;

PositionDeLaVariable = Spx->PositionDeLaVariable;
InDualFramework      = Spx->InDualFramework;

for ( Var = 0 ; Var < Spx->NombreDeVariables ; Var++ ) {
  if ( PositionDeLaVariable[Var] == EN_BASE_LIBRE ) InDualFramework[Var] = OUI_SPX;
  else                                              InDualFramework[Var] = NON_SPX;
}

DualPoids = Spx->DualPoids;
Tau       = Spx->Tau ;
for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) { 
  DualPoids[Cnt] = DUAL_POIDS_INITIAL;
  Tau      [Cnt] = 0.0;
}

Spx->LeSteepestEdgeEstInitilise = OUI_SPX;

Spx->CalculTauEnHyperCreux         = OUI_SPX;
Spx->CalculTauEnHyperCreuxPossible = OUI_SPX;
Spx->CountEchecsTau                = 0;                
Spx->AvertissementsEchecsTau       = 0;
Spx->NbEchecsTau                   = 0;
 
return;
}

/*----------------------------------------------------------------------------*/
void SPX_ResetReferenceSpace( PROBLEME_SPX * Spx ) 
{
char A1; char A2;

A1 = Spx->PositionDeLaVariable[Spx->VariableSortante];
A2 = Spx->PositionDeLaVariable[Spx->VariableEntrante];

Spx->PositionDeLaVariable[Spx->VariableSortante] = HORS_BASE_SUR_BORNE_INF;
Spx->PositionDeLaVariable[Spx->VariableEntrante] = EN_BASE_LIBRE;

SPX_InitDualPoids( Spx );

Spx->PositionDeLaVariable[Spx->VariableSortante] = A1;
Spx->PositionDeLaVariable[Spx->VariableEntrante] = A2;

return;
}

/*----------------------------------------------------------------------------*/
/*        Attention il s'agit de la methode "Projected Steepest Edge"         */

void SPX_MajPoidsDualSteepestEdge( PROBLEME_SPX * Spx )
{
int Var; int Cnt; int CntBase   ; int il ; int ilMax; char Save      ;  
double BetaP      ; double Rapport ; double X; double UnSurABarreSCntBase ; char ResoudreLeSysteme; 
char SecondMembreCreux; int * Cdeb; int * CNbTerm; double * NBarreR; 
int * ContrainteDeLaVariableEnBase; char *   InDualFramework             ; 
int * NumerosDesVariablesHorsBase ; int * NumeroDeContrainte            ;
int i; double *ArrayABarreS       ; int VariableEntrante                ;
double ABarreS; double * ACol      ; double * DualPoids; double * Tau     ;
int VariableSortante              ; char * OrigineDeLaVariable           ;
int * CntDeABarreSNonNuls         ; char ResolutionEnHyperCreux          ;
char TypeDEntree; char TypeDeSortie; int j; int k                       ;
int * NumVarNBarreRNonNul         ; char * T; int * IndexTermesNonNuls  ;
int NbTermesNonNuls               ; char StockageDeTau                   ;
char HyperCreuxInitial             ;
 
if ( Spx->TypeDePricing == PRICING_DANTZIG ) return;

T = Spx->T;
IndexTermesNonNuls = (int *) Spx->ErBMoinsUn;  	

VariableEntrante = Spx->VariableEntrante;
VariableSortante = Spx->VariableSortante;

ContrainteDeLaVariableEnBase = Spx->ContrainteDeLaVariableEnBase;
InDualFramework              = Spx->InDualFramework;
NumerosDesVariablesHorsBase  = Spx->NumerosDesVariablesHorsBase;
NBarreR                      = Spx->NBarreR;
ArrayABarreS                 = Spx->ABarreS;  
Tau                          = Spx->Tau;
DualPoids                    = Spx->DualPoids;   
OrigineDeLaVariable          = Spx->OrigineDeLaVariable;

Cdeb               = Spx->Cdeb;
CNbTerm            = Spx->CNbTerm;
NumeroDeContrainte = Spx->NumeroDeContrainte;
ACol               = Spx->ACol;

CntBase = ContrainteDeLaVariableEnBase[VariableSortante];

/* Calcul de BetaP */

BetaP = 0.;
if ( InDualFramework[VariableSortante] == OUI_SPX ) BetaP = DUAL_POIDS_INITIAL;

/* Multiplier N par NBarreR la ou il faut */
/* Boucle sur les variables hors base qui sont dans le framework */
ResoudreLeSysteme = NON_SPX;

if ( Spx->CalculABarreSEnHyperCreux == OUI_SPX && Spx->CalculTauEnHyperCreux == OUI_SPX ) ResolutionEnHyperCreux = OUI_SPX;
else ResolutionEnHyperCreux = NON_SPX;

if ( Spx->TypeDeStockageDeNBarreR == ADRESSAGE_INDIRECT_SPX ) {
	NumVarNBarreRNonNul = Spx->NumVarNBarreRNonNul;
  if ( ResolutionEnHyperCreux == OUI_SPX ) {		
    NbTermesNonNuls = 0 ;
    for ( j = 0 ; j < Spx->NombreDeValeursNonNullesDeNBarreR ; j++ ) {
      Var = NumVarNBarreRNonNul[j];
      if ( InDualFramework[Var] == NON_SPX ) continue;			
      X = NBarreR[Var];
		  if ( X == 0.0 ) continue;
      /* Calcul sur les variables hors base qui sont dans le framework */
      ResoudreLeSysteme = OUI_SPX;
      BetaP+= X * X; 
      il    = Cdeb[Var];
      ilMax = il + CNbTerm[Var];
      while ( il < ilMax ) {
		    k = NumeroDeContrainte[il];
        Tau[k]+= X * ACol[il];
			  if ( T[k] == 0 ) {
			    T[k] = 1;
				  IndexTermesNonNuls[NbTermesNonNuls] = k;
				  NbTermesNonNuls++;				
			  }
	      il++;
      }				  
    }
	  if ( NbTermesNonNuls != 0 ) ResoudreLeSysteme = OUI_SPX;
    for ( j = 0 ; j < NbTermesNonNuls ; j++ ) T[IndexTermesNonNuls[j]] = 0;
 	  StockageDeTau = ADRESSAGE_INDIRECT_SPX;
  }
	else {
    for ( j = 0 ; j < Spx->NombreDeValeursNonNullesDeNBarreR ; j++ ) {
      Var = NumVarNBarreRNonNul[j];
      if ( InDualFramework[Var] == NON_SPX ) continue;			
      X = NBarreR[Var];
		  if ( X == 0.0 ) continue;
      /* Calcul sur les variables hors base qui sont dans le framework */
      ResoudreLeSysteme = OUI_SPX;
      BetaP+= X * X; 
      il    = Cdeb[Var];
      ilMax = il + CNbTerm[Var];
      while ( il < ilMax ) {
        Tau[NumeroDeContrainte[il]]+= X * ACol[il];
	      il++;
      }				  
    }
 	  StockageDeTau = VECTEUR_SPX;	
	  ResolutionEnHyperCreux = NON_SPX;
	}	
}
else {
  for ( i = 0 ; i < Spx->NombreDeVariablesHorsBase ; i++ ) {
    Var = NumerosDesVariablesHorsBase[i];
    if ( InDualFramework[Var] == NON_SPX ) continue;	
    if ( NBarreR[Var] == 0.0 ) continue;	
    X = NBarreR[Var];
    /* Calcul sur les variables hors base qui sont dans le framework */
    ResoudreLeSysteme = OUI_SPX;
    BetaP+= X * X; 
    /*        */
    il    = Cdeb[Var];
    ilMax = il + CNbTerm[Var];
    while ( il < ilMax ) {
      Tau[NumeroDeContrainte[il]]+= X * ACol[il];
      il++;
    }
  }
 	StockageDeTau = VECTEUR_SPX;
	ResolutionEnHyperCreux = NON_SPX;
}
HyperCreuxInitial = ResolutionEnHyperCreux;

if ( DualPoids[CntBase] > /*2.*/ 4. * BetaP || DualPoids[CntBase] < /*0.5*/ 0.25 * BetaP ) {
  #if VERBOSE_SPX
    printf("Iteration %d BetaP %e Spx->DualPoids[CntBase] %e CntBase %d\n",Spx->Iteration,BetaP,Spx->DualPoids[CntBase],CntBase); 
    printf("VariableSortante %d VariableEntrante %d\n",Spx->VariableSortante,Spx->VariableEntrante); 
    printf("Iteration %d RESET REFERENCE SPACE par derive excessive des poids\n",Spx->Iteration);
  #endif		
  SPX_ResetReferenceSpace( Spx );	
  return;
}

/* Dynamic steepest edge: si la variable sortante est une variable basique artificielle on l'enleve de 
   l'espace de reference */
#ifdef ENLEVER_LES_VARIABLES_XMIN_EGAL_XMAX_DE_LA_LISTE_HORS_BASE
  if ( Spx->Xmin[VariableSortante] == Spx->Xmax[VariableSortante] ) {
    if ( InDualFramework[VariableSortante] == OUI_SPX ) { 
      InDualFramework[VariableSortante] = NON_SPX;
      BetaP-= DUAL_POIDS_INITIAL;
    }
  }
#else
  if (  OrigineDeLaVariable[VariableSortante] == BASIQUE_ARTIFICIELLE ) {
    if ( InDualFramework[VariableSortante] == OUI_SPX ) { 
      InDualFramework[VariableSortante] = NON_SPX;
      BetaP-= DUAL_POIDS_INITIAL;
    }
  }
#endif
   
/* Resolution du systeme Tau = B^{-1} Tau */
if ( ResoudreLeSysteme == OUI_SPX ) {
  Save = NON_LU;
  SecondMembreCreux = OUI_LU;
	TypeDEntree  = ADRESSAGE_INDIRECT_LU;
	TypeDeSortie = ADRESSAGE_INDIRECT_LU;
	
  if ( ResolutionEnHyperCreux == OUI_SPX ) {
	  if ( NbTermesNonNuls >= TAUX_DE_REMPLISSAGE_POUR_VECTEUR_HYPER_CREUX * Spx->NombreDeContraintes ) {
		  ResolutionEnHyperCreux = NON_SPX;
 	    StockageDeTau = VECTEUR_SPX;
		}
	}
	
  SPX_ResoudreBYegalA( Spx, TypeDEntree, Tau, IndexTermesNonNuls, &NbTermesNonNuls, &TypeDeSortie, 												
                       ResolutionEnHyperCreux, Save, SecondMembreCreux );
											 
  if ( ResolutionEnHyperCreux == OUI_SPX ) {
    if ( TypeDeSortie == VECTEUR_LU ) {		
      StockageDeTau = VECTEUR_SPX;
      /* Ca s'est pas bien passe et on s'est forcement retrouve en VECTEUR_LU */
		  Spx->NbEchecsTau++;			
		  /*printf("Echec hyper creux Tau iteration %d\n",Spx->Iteration);*/
		  if ( Spx->NbEchecsTau >= SEUIL_ECHEC_CREUX_STEEPEST  ) {
        # if VERBOSE_SPX
		      printf("Arret de l'hyper creux pour le steepest edge, iteration %d\n",Spx->Iteration);
	      # endif
		    Spx->CalculTauEnHyperCreux = NON_SPX;
		    Spx->CountEchecsTau = 0;
		  }
			
    }
	  else Spx->NbEchecsTau = 0;
  }
  /* Si on est pas en hyper creux, on essaie d'y revenir */
	if ( HyperCreuxInitial == NON_SPX ) {
    if ( Spx->CalculABarreSEnHyperCreuxPossible == OUI_SPX ) {
      if ( Spx->CalculTauEnHyperCreuxPossible == OUI_SPX ) {
        if ( Spx->CalculTauEnHyperCreux == NON_SPX ) {
          SPX_TenterRestaurationCalculTauEnHyperCreux( Spx );
          /* Test: on n'arrive plus a faire de l'hyper creux alors que ce serait possible: on fait un reset
					   de l'espace de reference pour ameliorer le creux du vecteur Tau */
					
					if ( Spx->CalculTauEnHyperCreuxPossible == NON_SPX ) {
					  if ( Spx->TypeDeCreuxDeLaBase == BASE_HYPER_CREUSE &&
						     Spx->CalculABarreSEnHyperCreux == OUI_SPX &&
								 Spx->CalculErBMoinsUnEnHyperCreux == OUI_SPX ) {
							/* On compte les variable HB dans le framework. S'il y en a beaucoup alors il y a peu de chances
							   qu'on puisse faire de l'hyper creux dans le steepest edge */
              for ( il = 0 , i = 0 ; i < Spx->NombreDeVariablesHorsBase ; i++ ) {
                if ( InDualFramework[NumerosDesVariablesHorsBase[i]] == OUI_SPX ) il++;
							}
							if ( il > ceil ( 0.2 * Spx->NombreDeVariablesHorsBase ) )  {							
                SPX_ResetReferenceSpace( Spx );
                # if VERBOSE_SPX
							    printf("Iteration %d reset reference space car on peut plus faire d'hyper creux\n",Spx->Iteration);
	              # endif
							  return;
							}
					  }								 
					}
					
          /* Fin test */				
	      }
			}
    }   
	}
	   
}

UnSurABarreSCntBase = 1. / Spx->ABarreSCntBase;

/* On n'aurait pas besoin de calculer DualPoids des contraintes dont la  variable en base est
   non bornee car ces variables ne sortent jamais de la base */
 
if ( ResoudreLeSysteme == OUI_SPX ) {
  if ( Spx->TypeDeStockageDeABarreS == VECTEUR_SPX ) {
		if ( StockageDeTau == VECTEUR_SPX ) {
      for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {    
        ABarreS = ArrayABarreS[Cnt];      
        if ( ABarreS != 0.0 ) {
		      Rapport = ABarreS * UnSurABarreSCntBase;									
          X = DualPoids[Cnt] + ( ( ( -2. * Tau[Cnt] ) + ( Rapport * BetaP ) ) * Rapport );			
          if ( X < SEUIL_CARRE_DE_NORME_TROP_FAIBLE ) {			  
	          #if VERBOSE_SPX
              printf("Iteration %d Cnt %d X %e => RESET REFERENCE SPACE par carre de norme trop faible\n",Spx->Iteration,Cnt,X);
            #endif				
            SPX_ResetReferenceSpace( Spx ); 
            return;				
          }										
          DualPoids[Cnt] = X;    
        }    
        Tau[Cnt] = 0.0;
      }
	  }
		else {
      for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {    
        ABarreS = ArrayABarreS[Cnt];      
        if ( ABarreS != 0.0 ) {
		      Rapport = ABarreS * UnSurABarreSCntBase;									
          X = DualPoids[Cnt] + ( ( ( -2. * Tau[Cnt] ) + ( Rapport * BetaP ) ) * Rapport );			
          if ( X < SEUIL_CARRE_DE_NORME_TROP_FAIBLE ) {			  
	          #if VERBOSE_SPX
              printf("Iteration %d Cnt %d X %e => RESET REFERENCE SPACE par carre de norme trop faible\n",Spx->Iteration,Cnt,X);
            #endif				
            SPX_ResetReferenceSpace( Spx ); 
            return;				
          }									
          DualPoids[Cnt] = X;    
        }    
      }
			for ( i = 0 ; i < NbTermesNonNuls ; i++ ) Tau[IndexTermesNonNuls[i]] = 0.0;
		}
	}
	else {
    CntDeABarreSNonNuls = Spx->CntDeABarreSNonNuls;	
    for ( i = 0 ; i < Spx->NbABarreSNonNuls ; i++ ) {
		  Rapport = ArrayABarreS[i] * UnSurABarreSCntBase;		
      Cnt = CntDeABarreSNonNuls[i];
      DualPoids[Cnt] +=  ( ( -2. * Tau[Cnt] ) + ( Rapport * BetaP ) ) * Rapport;			
      if ( DualPoids[Cnt] < SEUIL_CARRE_DE_NORME_TROP_FAIBLE ) {			  
	      #if VERBOSE_SPX
          printf("Iteration %d Cnt %d X %e => RESET REFERENCE SPACE par carre de norme trop faible\n",
					        Spx->Iteration,Cnt,DualPoids[Cnt]);
        #endif					
        SPX_ResetReferenceSpace( Spx ); 
        return;				
      }						
    }
		if ( StockageDeTau != VECTEUR_SPX ) {
		  for ( i = 0 ; i < NbTermesNonNuls ; i++ ) Tau[IndexTermesNonNuls[i]] = 0.0;
		}
		else {
		  /*memset( (char *) Tau, 0, Spx->NombreDeContraintes * sizeof( double ) );*/
		  for ( i = 0 ; i < Spx->NombreDeContraintes ; i++ ) Tau[i] = 0.0;			
		}
	}
}
else {
  if ( Spx->TypeDeStockageDeABarreS == VECTEUR_SPX ) {
    for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
      ABarreS = ArrayABarreS[Cnt];
      if ( ABarreS != 0.0 ) {		  
        Rapport = ABarreS * UnSurABarreSCntBase;						
        X = DualPoids[Cnt] + ( Rapport * BetaP * Rapport );						
        if ( X < SEUIL_CARRE_DE_NORME_TROP_FAIBLE ) {			  
	        #if VERBOSE_SPX
            printf("Iteration %d Cnt %d X %e => RESET REFERENCE SPACE par carre de norme trop faible\n",Spx->Iteration,Cnt,X);
          #endif				
          SPX_ResetReferenceSpace( Spx ); 
          return;				
        }								
        DualPoids[Cnt] = X;
      }
    }
	}
	else {
    CntDeABarreSNonNuls = Spx->CntDeABarreSNonNuls;	
    for ( i = 0 ; i < Spx->NbABarreSNonNuls ; i++ ) {
		  Rapport = ArrayABarreS[i] * UnSurABarreSCntBase;		
      Cnt = CntDeABarreSNonNuls[i];
      X = DualPoids[Cnt] + ( Rapport * BetaP * Rapport );									
      if ( X < SEUIL_CARRE_DE_NORME_TROP_FAIBLE ) {			  
	      #if VERBOSE_SPX
          printf("Iteration %d Cnt %d X %e => RESET REFERENCE SPACE par carre de norme trop faible\n",Spx->Iteration,Cnt,X);
        #endif				
        SPX_ResetReferenceSpace( Spx ); 
        return;				
      }			
      DualPoids[Cnt] = X;    
    }
	}
}

DualPoids[CntBase] = BetaP * UnSurABarreSCntBase * UnSurABarreSCntBase;  

/* Dynamic steepest edge: on ajoute la variable entrante dans l'espace de reference */
if ( InDualFramework[VariableEntrante] == NON_SPX ) {
  InDualFramework[VariableEntrante] = OUI_SPX;
  DualPoids[CntBase]+= DUAL_POIDS_INITIAL;
}

return;
}

/*----------------------------------------------------------------------------*/

void SPX_TenterRestaurationCalculTauEnHyperCreux( PROBLEME_SPX * Spx )
{
int Cnt; double * Tau; int Count;
	
if ( Spx->CountEchecsTau == 0 ) {
  if ( Spx->Iteration % CYCLE_TENTATIVE_HYPER_CREUX == 0 ) {
		Spx->NbEchecsTau    = SEUIL_REUSSITE_CREUX;
		Spx->CountEchecsTau = SEUIL_REUSSITE_CREUX + 2;
	}
}
if ( Spx->CountEchecsTau == 0 ) return;

Spx->CountEchecsTau--;		
/* On compte le nombre de termes non nuls du resultat */
Tau = Spx->Tau;
Count = 0;
for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) if ( Tau[Cnt] != 0.0 ) Count++;
if ( Count < 0.1 * Spx->NombreDeContraintes ) Spx->NbEchecsTau--;
if ( Spx->NbEchecsTau <= 0 ) {
  # if VERBOSE_SPX
    printf("Remise en service de l'hyper creux pour le steepest edge, iteration %d\n",Spx->Iteration);
	# endif
  Spx->AvertissementsEchecsTau = 0;
  Spx->CountEchecsTau = 0;
	Spx->CalculTauEnHyperCreux = OUI_SPX;
}
else if ( Spx->CountEchecsTau <= 0 ) {
  Spx->CountEchecsTau = 0;
  if ( Spx->CalculTauEnHyperCreux == NON_SPX ) Spx->AvertissementsEchecsTau++;
  if ( Spx->AvertissementsEchecsTau >= SEUIL_ABANDON_HYPER_CREUX ) {
	  # if VERBOSE_SPX
      printf("Arret prolonge de l'hyper creux pour le steepest edge, iteration %d\n",Spx->Iteration);
	  # endif
	  Spx->CalculTauEnHyperCreuxPossible = NON_SPX;
	}
}

return;
}
  
  
