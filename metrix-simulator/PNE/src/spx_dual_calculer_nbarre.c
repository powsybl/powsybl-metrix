/***********************************************************************

   FONCTION: Calcul de NBarre pour la variable sortante
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

/*----------------------------------------------------------------------------*/
void SPX_TenterRestaurationCalculErBMoinsEnHyperCreux( PROBLEME_SPX * Spx )
{
int Cnt; double * ErBMoinsUn; int Count;
	
if ( Spx->CountEchecsErBMoins == 0 ) {
  if ( Spx->Iteration % CYCLE_TENTATIVE_HYPER_CREUX == 0 ) {
		Spx->NbEchecsErBMoins    = SEUIL_REUSSITE_CREUX;
		Spx->CountEchecsErBMoins = SEUIL_REUSSITE_CREUX + 2;
	}
}
if ( Spx->CountEchecsErBMoins == 0 ) return;

Spx->CountEchecsErBMoins--;		
/* On compte le nombre de termes non nuls du resultat */
ErBMoinsUn = Spx->ErBMoinsUn;
Count = 0;
for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) if ( ErBMoinsUn[Cnt] != 0.0 ) Count++;
if ( Count < 0.1 * Spx->NombreDeContraintes ) Spx->NbEchecsErBMoins--;
if ( Spx->NbEchecsErBMoins <= 0 ) {
  # if VERBOSE_SPX
    printf("Remise en service de l'hyper creux pour le calcul de la ligne pivot, iteration %d\n",Spx->Iteration);
	# endif
  Spx->AvertissementsEchecsErBMoins = 0;
  Spx->CountEchecsErBMoins = 0;
	Spx->CalculErBMoinsUnEnHyperCreux = OUI_SPX;
}
else if ( Spx->CountEchecsErBMoins <= 0 ) {
  Spx->CountEchecsErBMoins = 0;
  if ( Spx->CalculErBMoinsUnEnHyperCreux == NON_SPX ) Spx->AvertissementsEchecsErBMoins++;
  if ( Spx->AvertissementsEchecsErBMoins >= SEUIL_ABANDON_HYPER_CREUX ) {
    # if VERBOSE_SPX
      printf("Arret prolonge de l'hyper creux pour le calcul de la ligne pivot, iteration %d\n",Spx->Iteration);
	  # endif
		Spx->CalculErBMoinsEnHyperCreuxPossible = NON_SPX;
	}
}

return;
}
/*----------------------------------------------------------------------------*/

void SPX_DualCalculerNBarreR( PROBLEME_SPX * Spx, char CalculDeCBarreSurNBarre, char * ControlerAdmissibiliteDuale )
{
int Var; double S; char PositionDeLaVariable_x; double X; int i; char * PositionDeLaVariable; 
double * NBarreR; int SortSurXmaxOuSurXmin; double * C; double * CBarre; char CalculEnHyperCreux;
double * CBarreSurNBarreR; double * CBarreSurNBarreRAvecTolerance; int * NumeroDesVariableATester;
char * CorrectionDuale; char FaireTestDegenerescence; double NBarreR_x; int j; double SeuilDePivot;
double UnSurNBarreR; int iLimite; int * NumerosDesVariables; double EpsHarris; int NbDeg0; int NbDeg;
double MinAbsCBarreNonDeg; double Delta; char ToutModifier; double Cb; int Iter;
double * SeuilDAmissibiliteDuale; double SeuilModifCout;

Spx->AdmissibilitePossible = NON_SPX;
*ControlerAdmissibiliteDuale = NON_SPX;

if ( Spx->TypeDeCreuxDeLaBase == BASE_HYPER_CREUSE && Spx->CalculErBMoinsUnEnHyperCreux == OUI_SPX &&
     Spx->FaireDuRaffinementIteratif <= 0 ) { 
  CalculEnHyperCreux = OUI_SPX;
  Spx->TypeDeStockageDeErBMoinsUn = COMPACT_SPX;	
}
else {
	CalculEnHyperCreux = NON_SPX;
  Spx->TypeDeStockageDeErBMoinsUn = VECTEUR_SPX;
}

SPX_CalculerErBMoins1( Spx, CalculEnHyperCreux );

/* Si on est pas en hyper creux, on essaie d'y revenir */
if ( Spx->CalculErBMoinsUnEnHyperCreux == NON_SPX ) {
  if ( Spx->CalculErBMoinsEnHyperCreuxPossible == OUI_SPX ) {
    SPX_TenterRestaurationCalculErBMoinsEnHyperCreux( Spx );
	}
}   

if ( Spx->TypeDeStockageDeErBMoinsUn == COMPACT_SPX ) { 
  SPX_DualCalculerNBarreRHyperCreux( Spx );	
}
else {
  SPX_DualCalculerNBarreRStandard( Spx );  
}

if ( CalculDeCBarreSurNBarre == NON_SPX ) return;
  
SeuilDePivot = Spx->SeuilDePivotDual;

PositionDeLaVariable   = Spx->PositionDeLaVariable;  
NBarreR                = Spx->NBarreR;   
SortSurXmaxOuSurXmin   = Spx->SortSurXmaxOuSurXmin;

C      = Spx->C; 
CBarre = Spx->CBarre;

CBarreSurNBarreR              = Spx->CBarreSurNBarreR;
CBarreSurNBarreRAvecTolerance = Spx->CBarreSurNBarreRAvecTolerance;
NumeroDesVariableATester      = Spx->NumeroDesVariableATester;

CorrectionDuale = Spx->CorrectionDuale;

FaireTestDegenerescence = NON_SPX;
if ( Spx->NbCyclesSansControleDeDegenerescence >= Spx->CycleDeControleDeDegenerescence ) {
  Spx->NbCyclesSansControleDeDegenerescence = 0;
  if ( Spx->ModifCoutsAutorisee == OUI_SPX ) FaireTestDegenerescence = OUI_SPX;
}
else {
  Spx->NbCyclesSansControleDeDegenerescence++;
}

/***************************** Preparation du test du ratio ***********************************/
Iter = 1;
Debut:

SeuilDAmissibiliteDuale = Spx->SeuilDAmissibiliteDuale1;

Spx->NombreDeVariablesATester = 0;

j = 0;

if ( Spx->TypeDeStockageDeNBarreR == VECTEUR_SPX ) {
  iLimite = Spx->NombreDeVariablesHorsBase;
	NumerosDesVariables = Spx->NumerosDesVariablesHorsBase;
}
else {
  iLimite = Spx->NombreDeValeursNonNullesDeNBarreR;
	NumerosDesVariables = Spx->NumVarNBarreRNonNul;
}

for ( i = 0 ; i < iLimite ; i++ ) {
   
  Var = NumerosDesVariables[i];	
  if ( fabs( NBarreR[Var] ) < SeuilDePivot ) continue;  
    
  NBarreR_x = NBarreR[Var];
	Cb = CBarre[Var];	
  PositionDeLaVariable_x = PositionDeLaVariable[Var];
  /* Classement des CBarreSurNBarreR qui vont servir au choix de la variables
     entrante dans l'algorithme dual.

  Si la variable sortante sort sur XMIN
  ------------------------------------- 
  Cas 1 - la variable hors base est sur BORNE INF: 
          alors son CBarre est >= 0 
          on est interesses que par les valeurs de NBarreR < 0 
          le rapport CBarre / NBarreR est <= 0
  Cas 2 - la variable hors base est sur BORNE SUP:
          alors son CBarre est <= 0 
          on est interesses que par les valeurs de NBarreR > 0 
          le rapport CBarre / NBarreR est <= 0
  Cas 3 - la variable hors base est sur HORS_BASE_A_ZERO (i.e. non bornée):
          alors son CBarre est = 0 quel que soit le signe de NBarreR,
					la variable devra entrer en base 
                                                 
  Si la variable sortante sort sur XMAX
  ------------------------------------- 
  Cas 1 - la variable hors base est sur BORNE INF: 
          alors son CBarre est >= 0 
          on est interesses que par les valeurs de NBarreR > 0 
          les rapport CBarre / NBarreR est >= 0
  Cas 2 - la variable hors base est sur BORNE SUP:
          alors son CBarre est <= 0 
          on est interesses que par les valeurs de NBarreR < 0 
          le rapport CBarre / NBarreR est >= 0
  Cas 3 - la variable hors base est sur HORS_BASE_A_ZERO (i.e. non bornée):
          alors son CBarre est = 0 quel que soit le signe de NBarreR,
					la variable devra entrer en base 
  */
  if ( SortSurXmaxOuSurXmin == SORT_SUR_XMIN ) {
    /* Pour les variables autres que non bornees on est interesses que par les rapports negatifs */
    if ( PositionDeLaVariable_x == HORS_BASE_SUR_BORNE_INF ) {
      /* Si la variable est HORS_BASE_SUR_BORNE_INF et si NBarreR est positif, la variable ne 
         quittera pas la base */
		  if ( NBarreR_x > 0.0 ) continue;
		}		
    else if ( PositionDeLaVariable_x == HORS_BASE_SUR_BORNE_SUP ) {
	    /* Si la variable est HORS_BASE_SUR_BORNE_SUP et si NBarreR est negatif, la variable ne 
         quittera pas la base */
		  if ( NBarreR_x < 0.0 ) continue;
		}
	  /* Si on arrive la c'est que c'est une variable non bornee */
  }
  else { /* La variable sort sur XMAX */
    /* Pour les variables autres que non bornees on est interesses que par les rapports positifs */
    if ( PositionDeLaVariable_x == HORS_BASE_SUR_BORNE_INF ) {
      /* Si la variable est HORS_BASE_SUR_BORNE_INF et si NBarreR est negatif, la variable ne 
         quittera pas la base */
		  if ( NBarreR_x < 0.0 ) continue;
		}	
    else if ( PositionDeLaVariable_x == HORS_BASE_SUR_BORNE_SUP ) {
      /* Si la variable est HORS_BASE_SUR_BORNE_INF et si NBarreR est positif, la variable ne 
         quittera pas la base */
		  if ( NBarreR_x > 0.0 ) continue;
		}
	  /* Si on arrive la c'est que c'est une variable non bornee */
  }

  /* Calcul de CBarre sur NBarreR */
  UnSurNBarreR = 1. / NBarreR_x;	
  X = Cb * UnSurNBarreR;	
  EpsHarris	= COEFF_TOLERANCE_POUR_LE_TEST_DE_HARRIS * SeuilDAmissibiliteDuale[Var] * UnSurNBarreR;
	
  if ( PositionDeLaVariable_x == HORS_BASE_SUR_BORNE_INF ) {		
    /* Le cout reduit est positif (sauf s'il y a des infaisabilites duales) */				
    if ( SortSurXmaxOuSurXmin != SORT_SUR_XMIN ) {		  
      CBarreSurNBarreR             [j] = X;
      CBarreSurNBarreRAvecTolerance[j] = X + EpsHarris;      			
    }
		else {		  
      CBarreSurNBarreR             [j] = -X;
      CBarreSurNBarreRAvecTolerance[j] = -(X + EpsHarris);			 			
		}		
    NumeroDesVariableATester[j] = i;				
    j++;      
  }
  else if ( PositionDeLaVariable_x == HORS_BASE_SUR_BORNE_SUP ) {	
    /* Le cout reduit est negatif (sauf s'il y a des infaisabilites duales) */		
    if ( SortSurXmaxOuSurXmin != SORT_SUR_XMIN ) {		  
      CBarreSurNBarreR             [j] = X;
      CBarreSurNBarreRAvecTolerance[j] = X - EpsHarris;									
		}
		else {		  
      CBarreSurNBarreR[j] = -X;	
      CBarreSurNBarreRAvecTolerance[j] = -(X - EpsHarris);			    			
		}		
    NumeroDesVariableATester[j] = i; 		
    j++;     
  }
  else {
    /* On est dans la cas d'une variable non bornee */      
    /* Comme on fait entrer en priorite les variables non bornees, des qu'on en trouve une 
       on arrete */		
    CBarreSurNBarreR             [0] = 0.;
    CBarreSurNBarreRAvecTolerance[0] = 0.;
    NumeroDesVariableATester     [0] = i;		   
		Spx->NombreDeVariablesATester = 1;		
    return;	  				
  }
		
}

Spx->NombreDeVariablesATester = j;

/* S'il n'y a pas de variable entrante on diminue un peu le seuil pour en trouver
   au cas ou il serait trop grand */
if ( Spx->NombreDeVariablesATester <= 0 && Iter == 1 /*&& 0*/ /* Reactivation du test le 8/9/2016 pour faire passer certains JDD optimate */ ) {
  Iter = 2;  
  /*SeuilDePivot /= 10;*/
	SeuilDePivot = DERNIERE_VALEUR_DE_PIVOT_ACCEPTABLE;
  /*printf("DualCalculerNBarreR   R  SeuilDePivot %e\n",SeuilDePivot);*/	
	goto Debut;
}

if ( FaireTestDegenerescence == NON_SPX ) return;

/* S'il y a risque de degenerescence on modifie les couts */
NbDeg0 = 0;
NbDeg  = 0;
MinAbsCBarreNonDeg = LINFINI_SPX;
for ( j = 0 ; j < Spx->NombreDeVariablesATester ; j++ ) {
  Var = NumerosDesVariables[NumeroDesVariableATester[j]];	
  if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_INF ) {
	  if ( CBarre[Var] < COEFF_TOLERANCE_POUR_LE_TEST_DE_HARRIS * SeuilDAmissibiliteDuale[Var] ) {
			NbDeg0++;
	    if ( CorrectionDuale[Var] != 0 ) NbDeg++;
		}
		else if ( fabs( CBarre[Var] ) < MinAbsCBarreNonDeg ) MinAbsCBarreNonDeg = fabs( CBarre[Var] );
	}
	else if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_SUP ) {
	  if ( CBarre[Var] > -COEFF_SEUIL_POUR_MODIFICATION_DE_COUT * SeuilDAmissibiliteDuale[Var] ) {
			NbDeg0++;
	    if ( CorrectionDuale[Var] != 0 ) NbDeg++;
		}
		else if ( fabs( CBarre[Var] ) < MinAbsCBarreNonDeg ) MinAbsCBarreNonDeg = fabs( CBarre[Var] );
	}
}

ToutModifier = NON_SPX;
if ( NbDeg == 0 ) {
  if ( NbDeg0 == 0 ) return;  
	else ToutModifier = OUI_SPX;
}

Spx->LesCoutsOntEteModifies = OUI_SPX;

S = VALEUR_PERTURBATION_COUT_A_POSTERIORI;
X = 0.5; 
if ( S > X * MinAbsCBarreNonDeg ) S = X * MinAbsCBarreNonDeg;

if ( ToutModifier == NON_SPX ) X = (double) NbDeg;
else X = (double) NbDeg0;

Delta = S / X;
/*if ( Delta < SEUIL_ADMISSIBILITE_DUALE_1 / X ) Delta = SEUIL_ADMISSIBILITE_DUALE_1 / X;*/
if ( Delta < SEUIL_ADMISSIBILITE_DUALE_2 / X ) Delta = SEUIL_ADMISSIBILITE_DUALE_2 / X;
S = Delta;

/* Maintenant on modifie les couts */

for ( j = 0 ; j < Spx->NombreDeVariablesATester ; j++ ) {
  i = NumeroDesVariableATester[j];
  Var = NumerosDesVariables[i];
	SeuilModifCout = COEFF_SEUIL_POUR_MODIFICATION_DE_COUT * SeuilDAmissibiliteDuale[Var];
  if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_INF ) {
	  if ( CBarre[Var] < SeuilModifCout ) {

		  if ( CorrectionDuale[Var] == 0 && ToutModifier == NON_SPX ) continue;
			
		  CorrectionDuale[Var]-= 1;				
	    if ( CorrectionDuale[Var] <= 0 ) CorrectionDuale[Var] = 0;
			  
      C[Var] += S; 
      CBarre[Var] += S;			
		  S += Delta;
						
			X = CBarre[Var] / NBarreR[Var];			
      EpsHarris	= COEFF_TOLERANCE_POUR_LE_TEST_DE_HARRIS * SeuilDAmissibiliteDuale[Var] / NBarreR[Var];
			
      if ( SortSurXmaxOuSurXmin != SORT_SUR_XMIN ) {			
        CBarreSurNBarreR             [j] = X;
        CBarreSurNBarreRAvecTolerance[j] = X + EpsHarris;
      }
		  else {
        CBarreSurNBarreR             [j] = -X;
        CBarreSurNBarreRAvecTolerance[j] = -(X + EpsHarris);
		  }					
		}
	}
	else if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_SUP ) {
	  if ( CBarre[Var] > -SeuilModifCout ) {
		
		  if ( CorrectionDuale[Var] == 0 && ToutModifier == NON_SPX ) continue;
			
		  CorrectionDuale[Var]-= 1;				
	    if ( CorrectionDuale[Var] <= 0 ) CorrectionDuale[Var] = 0;
			  
      C[Var] -= S;  
      CBarre[Var] -= S;			
		  S += Delta;			
			
			X = CBarre[Var] / NBarreR[Var];				 			
      EpsHarris	= COEFF_TOLERANCE_POUR_LE_TEST_DE_HARRIS * SeuilDAmissibiliteDuale[Var] / NBarreR[Var];	
      if ( SortSurXmaxOuSurXmin != SORT_SUR_XMIN ) {
        CBarreSurNBarreR             [j] = X;
        CBarreSurNBarreRAvecTolerance[j] = X - EpsHarris;		
		  }
		  else {
        CBarreSurNBarreR             [j] = -X;	
        CBarreSurNBarreRAvecTolerance[j] = -(X - EpsHarris);			
		  }								
		}
	}					     	
}

return;
}


