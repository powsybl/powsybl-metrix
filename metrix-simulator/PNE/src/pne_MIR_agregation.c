/***********************************************************************

   FONCTION: Heuristique Marchand-Wolsey pour faire des MIR sur des
	           contraintes natives agregees.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
# include "spx_fonctions.h"

# include "bb_define.h"
# include "bb_fonctions.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define MAX_FOIS_SUCCESS_MIR 10

# define Z0 1.e-7
# define Z1 0.9999999

# define VALEUR_NULLE_DUN_COEFF 1.e-15 /*1.e-12*/

# define RAPPORT_MAX_POUR_AGREGATION 1.e+4 /*1.e+4*/ 

# define MAX_AGREG_AU_NOEUD_RACINE   10 /*5*/    
# define MAX_AGREG_AU_NIVEAU_UN      5 /*5*/   
# define MAX_AGREG_AU_AUTRES_NIVEAU  5 /*5*/   
# define MAX_AGREG_SI_BEAUCOUP_DE_TERMES 2
		
# define LOW_VARBIN   1
# define HIGH_VARBIN  2
# define LOW   3
# define HIGH  4

/*
# undef BORNES_INF_AUXILIAIRES
# define BORNES_INF_AUXILIAIRES  NON_PNE ***  11/2016: on prefere laisser la valeur de constantes_internes car l'avantage ou le desavantage de faire cela n'est pas net.
*/

void PNE_VariableAvecContrainteSelectionnable ( PROBLEME_PNE * , int , double , int * , int * , int * , char * , double * , char * , char * , int * , 
                                                char , int * , double * , int );
																																										
/*----------------------------------------------------------------------------*/
double PNE_G_de_D( double d, double f )
{ double X; double fd;
X = floor( d );
/*fd = d - floor( d );*/
fd = d - X;
if ( fd > f ) {
  X += ( fd - f ) / ( 1. - f );
}
return( X );
}
/*----------------------------------------------------------------------------*/
double PNE_TesterUneMIR( int NombreDeVariablesBinaires, int * NumeroDeLaVariableBinaire,
                         double * CoeffDeLaVariableBinaire, double * UTrav,
												 double SecondMembreDeLaMIR, double Delta, double f, double SeuilPourC,
												 double * bMIR, char ModeTest )
{
int i; int Var; double ViolationMIR; double MembreDeGauche; double CoeffMir; double U;

MembreDeGauche = 0.0;
/* Calcul de chaque coeff de la MIR et de la violation */
goto NouveauCode;
for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
  Var = NumeroDeLaVariableBinaire[i];			
  if ( UTrav[Var] > SeuilPourC ) {
		/* La variable est dans C */
		CoeffMir = PNE_G_de_D( -CoeffDeLaVariableBinaire[i] / Delta, f );
	  MembreDeGauche += CoeffMir * ( 1. - UTrav[Var] );
	  if ( ModeTest == NON_PNE ) {
		  CoeffDeLaVariableBinaire[i] = -CoeffMir;
			*bMIR -= CoeffMir;
		}		
  }
	else {
	  CoeffMir = PNE_G_de_D( CoeffDeLaVariableBinaire[i] / Delta, f );
	  MembreDeGauche += CoeffMir * UTrav[Var];
	  if ( ModeTest == NON_PNE ) CoeffDeLaVariableBinaire[i] = CoeffMir;
	}
}
NouveauCode:
if ( ModeTest == NON_PNE ) {
  for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
    Var = NumeroDeLaVariableBinaire[i];
		U = UTrav[Var];
    if ( U > SeuilPourC ) {
		  /* La variable est dans C */
		  CoeffMir = PNE_G_de_D( -CoeffDeLaVariableBinaire[i] / Delta, f );
	    MembreDeGauche += CoeffMir * ( 1. - U );
		  CoeffDeLaVariableBinaire[i] = -CoeffMir;
			*bMIR -= CoeffMir;
    }
	  else {
	    CoeffMir = PNE_G_de_D( CoeffDeLaVariableBinaire[i] / Delta, f );
	    MembreDeGauche += CoeffMir * U;
	    CoeffDeLaVariableBinaire[i] = CoeffMir;
	  }
  }
}
else {
  for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
    Var = NumeroDeLaVariableBinaire[i];			
		U = UTrav[Var];
    if ( U > SeuilPourC ) {
		  /* La variable est dans C */
		  CoeffMir = PNE_G_de_D( -CoeffDeLaVariableBinaire[i] / Delta, f );
	    MembreDeGauche += CoeffMir * ( 1. - U );
    }
 	  else {
	    CoeffMir = PNE_G_de_D( CoeffDeLaVariableBinaire[i] / Delta, f );
	    MembreDeGauche += CoeffMir * U;
	  }
  }
}

ViolationMIR = MembreDeGauche - SecondMembreDeLaMIR;

if ( ViolationMIR > 0 && 0 ) {
	printf("Pendant tests ViolationMIR = %e  MembreDeGauche %e SecondMembreDeLaMIR %e ",ViolationMIR,MembreDeGauche,SecondMembreDeLaMIR);
	if ( ModeTest == NON_PNE ) printf("Final bMIR %e\n",*bMIR);
	printf("\n");
}

return( ViolationMIR );
}
/*----------------------------------------------------------------------------*/
void PNE_TesterUneMIRpourUneValeurDeDelta( PROBLEME_PNE * Pne,
                                           double Delta, int NombreDeVariablesBinaires,
                                           double SeuilPourC, int * NumeroDeLaVariableBinaire,
                                           double * CoeffDeLaVariableBinaire, double b,
																					 double * CoeffDeLaVariableContinue,
								                           double ValeurDeLaVariableContinue, 
																					 double * UTrav, double * ViolationMaxMIR,
																					 double * DeltaOpt, char * MIRviolee,
																					 double * bMIR, char ModeTest )
{
double CoeffVarCont; double Beta; double f; double SecondMembreDeLaMIR;
double ViolationMIR; double Seuil;

if ( Delta < Z0 ) return;
Beta = b / Delta;
f = Beta - floor( Beta );

if ( f > Z1 ) return;

/* On fait une MIR sur la contrainte divisee par Delta */
CoeffVarCont = 1 / ( Delta * ( 1. - f ) );
SecondMembreDeLaMIR = floor( Beta ) + ( CoeffVarCont * ValeurDeLaVariableContinue );

if ( ModeTest == NON_PNE ) {
  *CoeffDeLaVariableContinue = CoeffVarCont;
	*bMIR = floor( Beta );
}

ViolationMIR = PNE_TesterUneMIR( NombreDeVariablesBinaires, NumeroDeLaVariableBinaire,
                                 CoeffDeLaVariableBinaire, UTrav, SecondMembreDeLaMIR,
																 Delta, f, SeuilPourC, bMIR ,ModeTest );

PNE_MiseAJourSeuilCoupes( Pne, COUPE_MIR_MARCHAND_WOLSEY, &Seuil );

if ( ViolationMIR > Seuil ) {
  Pne->SommeViolationsMIR_MARCHAND_WOLSEY += ViolationMIR;
  Pne->NombreDeMIR_MARCHAND_WOLSEY++;
}
																 
if ( ViolationMIR > Pne->SeuilDeViolationMIR_MARCHAND_WOLSEY && ViolationMIR > *ViolationMaxMIR ) {
	*MIRviolee = OUI_PNE; 
  *ViolationMaxMIR = ViolationMIR;
	*DeltaOpt = Delta;	
	/*printf("Violation MIR    ViolationMIR = %e\n",ViolationMIR); */ 	
}

return;
}

/*----------------------------------------------------------------------------*/
/* La contrainte mixte recue est du type Somme ai * yi <= b + s ou les yi
   sont des variables binaires et s est une variable continue >= 0 */
/* On suppose que les variables entieres sont des binaires donc leur borne
   sup est 1. */

char PNE_C_MIR( PROBLEME_PNE * Pne, int NombreDeVariablesBinaires, int * NumeroDeLaVariableBinaire,
                double * CoeffDeLaVariableBinaire, double * b, double * CoeffDeLaVariableContinue,
								double ValeurDeLaVariableContinue )
{
int i; int Var; double * UTrav; double Delta; double SeuilPourC; char MIRviolee;
double ViolationMaxMIR; double DeltaOpt; double Delta0; char ModeTest; double bTest;
double bMIR; double SeuilPourCRetenu; double Beta; double f; double S; 

SeuilPourC = 0.5;
MIRviolee = NON_PNE;
ViolationMaxMIR = -1.0;
UTrav = Pne->UTrav;

/* Au depart, on a toujours CoeffDeLaVariableContinue = 1 */
/* Recherche de l'ensemble C que l'on va complementer */
bTest = *b;
for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
  if ( UTrav[NumeroDeLaVariableBinaire[i]] > SeuilPourC ) {
    /* On complemente la variable */
		bTest -= CoeffDeLaVariableBinaire[i]; /* Borne sur des variables binaires = 1 */
	}
}

/* Choix des valeurs de Delta */
ModeTest = OUI_PNE;

for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
  Var = NumeroDeLaVariableBinaire[i];
  if ( UTrav[Var] < Z0 || UTrav[Var] > Z1 ) continue;
	Delta = fabs( CoeffDeLaVariableBinaire[i] );
	
  Beta = bTest / Delta;
  f = Beta - floor( Beta );
	
  if ( f < Z0 ) {
	  Delta *= 0.9 /*1.5*/;
	}
		
  PNE_TesterUneMIRpourUneValeurDeDelta( Pne, Delta, NombreDeVariablesBinaires, SeuilPourC,
	                                      NumeroDeLaVariableBinaire, CoeffDeLaVariableBinaire,
																				bTest, CoeffDeLaVariableContinue,
								                        ValeurDeLaVariableContinue, UTrav, &ViolationMaxMIR,
																				&DeltaOpt, &MIRviolee, &bMIR, ModeTest );																			
}
if ( MIRviolee == OUI_PNE ) {
  
  Delta0 = DeltaOpt;
  /* On essaie d'ameliorer la violation */
	Delta = Delta0 / 2;	
  PNE_TesterUneMIRpourUneValeurDeDelta( Pne, Delta, NombreDeVariablesBinaires, SeuilPourC,
	                                      NumeroDeLaVariableBinaire, CoeffDeLaVariableBinaire,
																				bTest, CoeffDeLaVariableContinue,
								                        ValeurDeLaVariableContinue, UTrav, &ViolationMaxMIR,
																				&DeltaOpt, &MIRviolee, &bMIR, ModeTest );																					
	Delta = Delta0 / 4;
  PNE_TesterUneMIRpourUneValeurDeDelta( Pne, Delta, NombreDeVariablesBinaires, SeuilPourC,
	                                      NumeroDeLaVariableBinaire, CoeffDeLaVariableBinaire,
																				bTest, CoeffDeLaVariableContinue,
								                        ValeurDeLaVariableContinue, UTrav, &ViolationMaxMIR,
																				&DeltaOpt, &MIRviolee, &bMIR, ModeTest ); 	
	Delta = Delta0 / 8;
  PNE_TesterUneMIRpourUneValeurDeDelta( Pne, Delta, NombreDeVariablesBinaires, SeuilPourC,
	                                      NumeroDeLaVariableBinaire, CoeffDeLaVariableBinaire,
																				bTest, CoeffDeLaVariableContinue,
								                        ValeurDeLaVariableContinue, UTrav, &ViolationMaxMIR,
																				&DeltaOpt, &MIRviolee, &bMIR, ModeTest );																					
}

/* Il manque la derniere etape d'amelioration de la mir decrite dans l'article */

if ( MIRviolee == OUI_PNE ) {	
  SeuilPourCRetenu = SeuilPourC;	
  /* On a trouve un DeltaOpt */
	/* On essaie d'augmenter la violation de la MIR en essayant SeuilPourC a 0.4 puis SeuilPourC 0.6 */
	Delta = DeltaOpt;
  SeuilPourC = 0.4;	
	BaisseSeuilC:
  bTest = *b;
  for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
    if ( UTrav[NumeroDeLaVariableBinaire[i]] > SeuilPourC ) {
      /* On complemente la variable */
		  bTest -= CoeffDeLaVariableBinaire[i];
	  }
  }
	MIRviolee = NON_PNE;
  PNE_TesterUneMIRpourUneValeurDeDelta( Pne, Delta, NombreDeVariablesBinaires, SeuilPourC,
	                                      NumeroDeLaVariableBinaire, CoeffDeLaVariableBinaire,
																				bTest, CoeffDeLaVariableContinue,
								                        ValeurDeLaVariableContinue, UTrav, &ViolationMaxMIR,
																				&DeltaOpt, &MIRviolee, &bMIR, ModeTest ); 
	if ( MIRviolee == OUI_PNE ) {
	  /*printf("MIR amelioree en modifiant SeuilPourC a la baisse\n");*/
	  SeuilPourCRetenu = SeuilPourC;
		SeuilPourC = SeuilPourC - 0.1;
		if ( SeuilPourC > 0.0 && 0 ) goto BaisseSeuilC;
	}
	
	Delta = DeltaOpt;
  SeuilPourC = 0.6;
	HausseeSeuilC:	
  bTest = *b;
  for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
    if ( UTrav[NumeroDeLaVariableBinaire[i]] > SeuilPourC ) {
      /* On complemente la variable */
		  bTest -= CoeffDeLaVariableBinaire[i];
	  }
  }
	MIRviolee = NON_PNE;
  PNE_TesterUneMIRpourUneValeurDeDelta( Pne, Delta, NombreDeVariablesBinaires, SeuilPourC,
	                                      NumeroDeLaVariableBinaire, CoeffDeLaVariableBinaire,
																				bTest, CoeffDeLaVariableContinue,
								                        ValeurDeLaVariableContinue, UTrav, &ViolationMaxMIR,
																				&DeltaOpt, &MIRviolee, &bMIR, ModeTest ); 	
	if ( MIRviolee == OUI_PNE ) {
	  /*printf("MIR amelioree en modifiant SeuilPourC a la hausse\n");*/
	  SeuilPourCRetenu = SeuilPourC;
		SeuilPourC = SeuilPourC + 0.1;
		if ( SeuilPourC < 1.0 && 0 ) goto HausseeSeuilC;		
	}
	
  /* Stockage de la MIR */
	Delta = DeltaOpt;
  SeuilPourC = SeuilPourCRetenu;
  bTest = *b;
  for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
    if ( UTrav[NumeroDeLaVariableBinaire[i]] > SeuilPourC ) {
      /* On complemente la variable */
		  bTest -= CoeffDeLaVariableBinaire[i];
	  }
  }	
	ModeTest = NON_PNE;
  PNE_TesterUneMIRpourUneValeurDeDelta( Pne, Delta, NombreDeVariablesBinaires, SeuilPourC,
	                                      NumeroDeLaVariableBinaire, CoeffDeLaVariableBinaire,
																				bTest, CoeffDeLaVariableContinue,
								                        ValeurDeLaVariableContinue, UTrav, &ViolationMaxMIR,
																				&DeltaOpt, &MIRviolee, b, ModeTest );																					
  MIRviolee = OUI_PNE;
	/*
	printf("b de la MIR %e  CoeffDeLaVariableContinue %e ValeurDeLaVariableContinue %e\n",*b,*CoeffDeLaVariableContinue,ValeurDeLaVariableContinue);
	*/
	S = -(*CoeffDeLaVariableContinue) * ValeurDeLaVariableContinue;
	/*printf("La MIR avant reconstitution de S\n");*/
  for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
    S += CoeffDeLaVariableBinaire[i] * Pne->UTrav[NumeroDeLaVariableBinaire[i]];
		/*printf(" %e (%d) ",CoeffDeLaVariableBinaire[i],NumeroDeLaVariableBinaire[i]);*/
  }
	/*printf("\n");*/
	if ( S < *b ) {
	  /*printf("Erreur des la fin de la MIR  S = %e  < b = %e\n",S,*b);*/
	}
	else {
	  /*printf("violation de cette mir S %e b %e violation %e\n",S,*b,S-*b);*/
	}
	/*printf("On va maintenant revenir aux variables qui constituent S\n");*/
	
}

return( MIRviolee );
}
/*----------------------------------------------------------------------------*/
void PNE_SyntheseEtStockageMIR_New( PROBLEME_PNE * Pne, int NombreDeVariablesBinaires, int * NumeroDeLaVariableBinaire,
                                    double * CoeffDeLaVariableBinaire, double b, double CoeffDeLaVariableContinue,
															    	int NombreDeVariablesSubstituees, int * NumeroDesVariablesSubstituees,
														        char * TypeDeSubsitution, double * CoefficientDeLaVariableSubstituee, char * MirAcceptee )																
{
int i; int Var; int il; int VarBin; int * NuVarCoupe; double * CoeffCoupe; int NbTermes; char TypeSubst;
double Violation; double * Umax; double * Umin; int * Mdeb; double * A; int * Nuvar; double l; double u;
int * CntDeBorneInfVariable; int * CntDeBorneSupVariable; double * U; double bBorne; double S; double * B;
int * TypeDeBorne; double Cmin; double Cmax; double SeuilZero; double SeuilInvalidation; int NbGarbage;

/*
printf(" Synthese de la MIR \n");
printf(" b = %e  CoeffDeLaVariableContinue = %e\n",b,CoeffDeLaVariableContinue);
*/

*MirAcceptee = NON_PNE;
 
Mdeb = Pne->MdebTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;
Umax = Pne->UmaxTravSv;
Umin = Pne->UminTravSv;

# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Umin = Pne->XminAuxiliaire;
# endif

U = Pne->UTrav;
CntDeBorneInfVariable = Pne->CntDeBorneInfVariable;
CntDeBorneSupVariable = Pne->CntDeBorneSupVariable;
TypeDeBorne = Pne->TypeDeBorneTrav;

NuVarCoupe = Pne->IndiceDeLaVariable_CG;
CoeffCoupe = Pne->Coefficient_CG;

NbTermes = 0;
memset( (char *) CoeffCoupe, 0, Pne->NombreDeVariablesTrav * sizeof( double) );
for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
	CoeffCoupe[NumeroDeLaVariableBinaire[i]] = CoeffDeLaVariableBinaire[i];
}
for ( i = 0 ; i < NombreDeVariablesSubstituees ; i++ ) {
  Var = NumeroDesVariablesSubstituees[i];
	/* On reconstitue Umin Umax */	
	TypeSubst = TypeDeSubsitution[i];
	if ( TypeDeSubsitution[i] == LOW ) {
	  CoeffCoupe[Var] = CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i];
    b += CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i] * Umin[Var];
	}	
	else if ( TypeDeSubsitution[i] == LOW_VARBIN ) {
	  CoeffCoupe[Var] = CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i];		
	  il = Mdeb[CntDeBorneInfVariable[Var]];				
		VarBin = Nuvar[il];
	  l = A[il];
	  CoeffCoupe[VarBin] -= CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i] * l;
		
		bBorne = -B[CntDeBorneInfVariable[Var]];
    b += CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i] * bBorne;
		
	}
	
	else if ( TypeDeSubsitution[i] == HIGH ) {
	  CoeffCoupe[Var] = -CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i];
    b -= CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i] * Umax[Var];	
	}		
	else if ( TypeDeSubsitution[i] == HIGH_VARBIN ) {
	  CoeffCoupe[Var] = -CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i];
		il = Mdeb[CntDeBorneSupVariable[Var]];		
		VarBin = Nuvar[il];
	  u = -A[il];
	  CoeffCoupe[VarBin] += CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i] * u;

		bBorne = B[CntDeBorneSupVariable[Var]];
    b -= CoeffDeLaVariableContinue * CoefficientDeLaVariableSubstituee[i] * bBorne;
		
	}
	else {
	  printf("BUG: TypeDeSubsitution %d inconnu i %d\n",TypeDeSubsitution[i],i);
		exit(0);
	}
}

S = 0.;
Cmin = LINFINI_PNE;
Cmax = -LINFINI_PNE;
SeuilZero = 1.e-12;

if ( Pne->PlusPetitTerme > 1.e-2 ) SeuilInvalidation = 1.e-4;
else if ( Pne->PlusPetitTerme > 1.e-3 ) SeuilInvalidation = 1.e-5;
else if ( Pne->PlusPetitTerme > 1.e-4 ) SeuilInvalidation = 1.e-6;
else SeuilInvalidation = 1.e-7;

NbGarbage = 0;

for ( Var = 0 ; Var < Pne->NombreDeVariablesTrav ; Var++ ) {
  if ( CoeffCoupe[Var] != 0.0 ) {
		if ( fabs( CoeffCoupe[Var] ) <  VALEUR_NULLE_DUN_COEFF && 0 ) {
			NbGarbage++;			
			continue;
		}
		if ( fabs( CoeffCoupe[Var] ) < SeuilInvalidation ) {
		  if ( CoeffCoupe[Var] > 0 ) {
        /* On supprime la variable et on fait comme ci la variable etait sur borne inf */
		    if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {				
          b -= CoeffCoupe[Var] * Umin[Var];
				}
				else goto FinSyntheseEtStockageMIR;
			}
			else {
        /* On supprime la variable et on fait comme ci la variable etait sur borne sup */
		    if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) {				
          b -= CoeffCoupe[Var] * Umax[Var];
				}
				else goto FinSyntheseEtStockageMIR;
			}		
			continue;
		}
				
	  NuVarCoupe[NbTermes] = Var;
	  CoeffCoupe[NbTermes] = CoeffCoupe[Var];
  
		if ( fabs( CoeffCoupe[NbTermes] ) > Cmax ) Cmax =  fabs( CoeffCoupe[NbTermes] );
		if ( fabs( CoeffCoupe[NbTermes] ) < Cmin ) Cmin =  fabs( CoeffCoupe[NbTermes] );
		
	  S += CoeffCoupe[NbTermes] * U[NuVarCoupe[NbTermes]];
		
		NbTermes++;
	}	
}

if ( NbGarbage >= 10 /*|| NbGarbage > ceil( 0.1 * NbTermes ) + 1*/ ) {
	printf("             3- MIR refusee\n");
  goto FinSyntheseEtStockageMIR;
}

if ( Cmax / Cmin > Pne->PlusGrandTerme / Pne->PlusPetitTerme ) {
	/*printf("4- MIR refusee\n");*/
  goto FinSyntheseEtStockageMIR;
}

Violation = S-b;  

if ( Violation < Pne->SeuilDeViolationMIR_MARCHAND_WOLSEY || NbTermes <= 0 ) {
  goto FinSyntheseEtStockageMIR;
}

if ( PNE_LaCoupeEstColineaire( Pne, CoeffCoupe, NuVarCoupe, b, NbTermes ) == OUI_PNE ) goto FinSyntheseEtStockageMIR;
/*
printf("MIR Marchand Wolsey Violation = %e NbTermes = %d  b = %e Cmin %e Cmax %e PlusPetitTerme %e PlusGrandTerme %e NbGarbage %d\n",
        Violation,NbTermes,b,Cmin,Cmax,Pne->PlusPetitTerme,Pne->PlusGrandTerme,NbGarbage);
*/

/*printf("                 MIR acceptee NbTermes %d\n",NbTermes);*/

/* Le 31/08/2015 on change le type de coupe: K devient G */
/* Mais attention car dans SPX_CalculMIRPourCoupeDeGomoryOuIntersection cela risque d'invalider des
   coupes de gomory. Pour e pas risque d'invalider il faut mettre K */
*MirAcceptee = OUI_PNE;
PNE_EnrichirLeProblemeCourantAvecUneCoupe( Pne, 'G', NbTermes, b, Violation, CoeffCoupe, NuVarCoupe );

FinSyntheseEtStockageMIR:

memset( (char *) CoeffCoupe, 0, Pne->NombreDeVariablesTrav * sizeof( double ) );
memset( (char *) NuVarCoupe, 0, Pne->NombreDeVariablesTrav * sizeof( int ) );

return;
}
/*----------------------------------------------------------------------------*/
char PNE_BoundSubstitution( PROBLEME_PNE * Pne,int NbVarContinues, int NbVarEntieres,
                            int * NumeroDesVariables, double * CoeffDesVariables,
														double SecondMembreContrainteAgregee,
														/* En sortie, la contrainte pour y faire une MIR */                            
														int * NombreDeVariablesBinaires, int * NumeroDeLaVariableBinaire,
                            double * CoeffDeLaVariableBinaire, double * b, double * CoeffDeLaVariableContinue,
								            double * ValeurDeLaVariableContinue,
														int * NombreDeVariablesSubstituees, int * NumeroDesVariablesSubstituees,
														char * TypeDeSubsitution, double * CoefficientDeLaVariableSubstituee,
														char * YaUneVariableS )
{
int i; int Var; int il; double Beta; int * CntDeBorneInfVariable; int * CntDeBorneSupVariable;
char l_valide; char u_valide; char VarEstSurBorneInf; char VarEstSurBorneSup; int VarBin_u; int VarBin_l;
double AlphaJ; int * Mdeb; int * Nuvar; int Nb; int * T; char CodeRet;	double bBorne_l; double	bBorne_u;
double * A; double * U; double * Umin; double * Umax; double l; double u; int TypeBorne; int * TypeDeBorne;
int Index; int IndexFin; double ValeurDeS; double * B; double DeltaJ; double ValeurDeTJ; int NbVarB;
int NbVarBinaires; int NombreDeVariables; int VarBin;

CntDeBorneInfVariable = Pne->CntDeBorneInfVariable;
CntDeBorneSupVariable = Pne->CntDeBorneSupVariable;
Mdeb = Pne->MdebTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;
U = Pne->UTrav;
Umin = Pne->UminTravSv;
Umax = Pne->UmaxTravSv;

# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Umin = Pne->XminAuxiliaire;
# endif

TypeDeBorne = Pne->TypeDeBorneTrav;
ValeurDeS = 0.0;
*NombreDeVariablesSubstituees = 0;
*YaUneVariableS = NON_PNE;

NombreDeVariables = Pne->NombreDeVariablesTrav;

/* A ce stade Coefficient_CG est nul et IndiceDeLaVariable_CG est nul */
T = Pne->IndiceDeLaVariable_CG;

/* Si la variable n'a pas de borne sup ou est sur sa borne inf on remplace par l'expression de sa borne inf */
/* Si la variable est sur sa borne sup on remplace par l'expression de sa borne sup */
/* Si AlphJ < 0 on remplace par l'expression de sa borne sup */
/* Si AlphJ > 0 on remplace par l'expression de sa borne inf */

Beta = SecondMembreContrainteAgregee;
NbVarB = 0;
CodeRet = NON_PNE;
l = 0.0;
u = 0.0;

/* Remarque: telles que sont construits CntDeBorneInfVariable et CntDeBorneSupVariable, bBorne_l et bBorne_u ne peuvent etre que nulles */

NbVarBinaires = 0;
for ( i = 0 ; i < NbVarContinues ; i++ ) {
  Var = NumeroDesVariables[i];
	AlphaJ = CoeffDesVariables[i];
  TypeBorne	= TypeDeBorne[Var];

	if ( TypeBorne == VARIABLE_FIXE ) {
	  /* La variable continue est fixe: on la passe dans le second membre */
    Beta -= CoeffDesVariables[i] * U[Var]; 
		continue;		
	}
	 
	/* A ce stade on n'a jamais de variables bornees superieurement */
	if ( TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT) {
	  printf("Attention BUG: a ce stade du calcul (les MIR) il ne doit pas exister de VARIABLE_BORNEE_SUPERIEUREMENT\n");
	}
	if ( TypeBorne == VARIABLE_NON_BORNEE ) {
    /* Substitution impossible et on ne peut pas non plus la passer dans s */
		/*printf("Bound substitution impossible car variable non bornee\n");*/
		goto FinBoundSubstitution;	
	}

	/* Presence d'une borne inf ou sup variable */	
	l_valide = NON_PNE;
	u_valide = NON_PNE;
	VarEstSurBorneInf = NON_PNE;
	VarEstSurBorneSup = NON_PNE;

	/* 29/9/2016: on separe VarBin et bBorne en VarBin_l VarBin_u bBorne_l bBorne_u */
	VarBin_l = -1;
	VarBin_u = -1;
	bBorne_l = 0.0;
	bBorne_u = 0.0;

  if ( CntDeBorneInfVariable != NULL ) {
    if ( CntDeBorneInfVariable[Var] >= 0 ) {				
		  il = Mdeb[CntDeBorneInfVariable[Var]];
			bBorne_l = -B[CntDeBorneInfVariable[Var]];
		  VarBin_l = Nuvar[il];
	    l = A[il];
		  l_valide = OUI_PNE;
		  if ( fabs( U[Var] - ( l * U[VarBin_l] ) ) < 1.e-9 ) VarEstSurBorneInf = OUI_PNE;
			if ( TypeDeBorne[VarBin_l] == VARIABLE_FIXE ) {			
			  l_valide = NON_PNE; 
				l = bBorne_l + ( l * U[VarBin_l] );
			}
	  }
	}
	else if ( fabs( U[Var] - Umin[Var] ) < 1.e-9 ) VarEstSurBorneInf = OUI_PNE;  
	
  if ( CntDeBorneSupVariable != NULL ) {
    if ( CntDeBorneSupVariable[Var] >= 0 ) {		
		  il = Mdeb[CntDeBorneSupVariable[Var]];
			bBorne_u = B[CntDeBorneSupVariable[Var]];
		  VarBin_u = Nuvar[il];
	    u = -A[il];
		  u_valide = OUI_PNE;
		  if ( fabs( U[Var] - ( u * U[VarBin_u] ) ) < 1.e-9 ) VarEstSurBorneSup = OUI_PNE;
			if ( TypeDeBorne[VarBin_u] == VARIABLE_FIXE ) {			
			  u_valide = NON_PNE; 
				u = bBorne_u + ( u * U[VarBin_u] );
			}			
	  }
	}
	else if ( fabs( U[Var] - Umax[Var] ) < 1.e-9 ) VarEstSurBorneSup = OUI_PNE;
	
  /* Attention aux variables binaires de substitution qui sont fixes */
	
  /* Si la variable n'a pas de borne sup ou est sur sa borne inf on remplace par l'expression de sa borne inf */
  if ( (TypeDeBorne[Var] == VARIABLE_BORNEE_INFERIEUREMENT && u_valide == NON_PNE ) || VarEstSurBorneInf == OUI_PNE ) {
    if ( l_valide == OUI_PNE ) {
		  /* On fait x = l * y + t */
						
      if ( T[VarBin_l] == 0 ) {
			  NumeroDeLaVariableBinaire[NbVarBinaires] = VarBin_l;
		    CoeffDeLaVariableBinaire[NbVarBinaires] = AlphaJ * l;
        T[VarBin_l] = NombreDeVariables + NbVarBinaires;
        NbVarBinaires++;
			}
      else {
			  Nb = T[VarBin_l] - NombreDeVariables;
		    CoeffDeLaVariableBinaire[Nb] += AlphaJ * l;
			}

			Beta -= AlphaJ * bBorne_l;
			
		  ValeurDeTJ = U[Var] - ( l * U[VarBin_l] ) - bBorne_l;						
		}
		else {
		  /* Pas de borne inf variable */
			Beta -= AlphaJ * Umin[Var];
		  ValeurDeTJ = U[Var] - Umin[Var];
		}
		DeltaJ = AlphaJ;
		if ( DeltaJ < 0 ) {
		  ValeurDeS += -DeltaJ * ValeurDeTJ;
      *YaUneVariableS = OUI_PNE;			 
			NumeroDesVariablesSubstituees[*NombreDeVariablesSubstituees] = Var;
			if ( l_valide == OUI_PNE ) TypeDeSubsitution[*NombreDeVariablesSubstituees] = LOW_VARBIN;
			else TypeDeSubsitution[*NombreDeVariablesSubstituees] = LOW;
			CoefficientDeLaVariableSubstituee[*NombreDeVariablesSubstituees] = DeltaJ;
			*NombreDeVariablesSubstituees = *NombreDeVariablesSubstituees + 1;
		}		
		continue;
	}
  /* Si la variable est sur sa borne sup on remplace par l'expression de sa borne sup */
  if ( VarEstSurBorneSup == OUI_PNE ) {
    if ( u_valide == OUI_PNE ) {
		  /* On fait x = u * y - t */
						
      if ( T[VarBin_u] == 0 ) {
			  NumeroDeLaVariableBinaire[NbVarBinaires] = VarBin_u;
		    CoeffDeLaVariableBinaire[NbVarBinaires] = AlphaJ * u;
        T[VarBin_u] = NombreDeVariables + NbVarBinaires;
        NbVarBinaires++;
			}
      else {
			  Nb = T[VarBin_u] - NombreDeVariables;
		    CoeffDeLaVariableBinaire[Nb] += AlphaJ * u;
			}
			
			Beta -= AlphaJ * bBorne_u;
									
		  ValeurDeTJ = bBorne_u + ( u * U[VarBin_u] ) - U[Var];			
		}
		else {
		  /* Pas de borne sup variable */
			Beta -= AlphaJ * Umax[Var];
		  ValeurDeTJ = Umax[Var] - U[Var];
		}
		DeltaJ = -AlphaJ;
		if ( DeltaJ < 0 ) {
		  ValeurDeS += -DeltaJ * ValeurDeTJ;
      *YaUneVariableS = OUI_PNE;			 			
			NumeroDesVariablesSubstituees[*NombreDeVariablesSubstituees] = Var;
			if ( u_valide == OUI_PNE ) TypeDeSubsitution[*NombreDeVariablesSubstituees] = HIGH_VARBIN;
			else TypeDeSubsitution[*NombreDeVariablesSubstituees] = HIGH;			
			CoefficientDeLaVariableSubstituee[*NombreDeVariablesSubstituees] = DeltaJ;
			*NombreDeVariablesSubstituees = *NombreDeVariablesSubstituees + 1;			
		}
		continue;
	}
  /* Si AlphaJ < 0 on remplace par l'expression de sa borne sup */
	if ( AlphaJ < 0.0 ) {
    if ( u_valide == OUI_PNE ) {
		  /* On fait x = u * y - t */
						
      if ( T[VarBin_u] == 0 ) {
			  NumeroDeLaVariableBinaire[NbVarBinaires] = VarBin_u;
		    CoeffDeLaVariableBinaire[NbVarBinaires] = AlphaJ * u;
        T[VarBin_u] = NombreDeVariables + NbVarBinaires;
        NbVarBinaires++;
			}
      else {
			  Nb = T[VarBin_u] - NombreDeVariables;
		    CoeffDeLaVariableBinaire[Nb] += AlphaJ * u;
			}
			
			Beta -= AlphaJ * bBorne_u;
			
		  ValeurDeTJ = bBorne_u + ( u * U[VarBin_u] ) - U[Var];						
		}
		else {  
		  /* Pas de borne sup variable */
			Beta -= AlphaJ * Umax[Var];
		  ValeurDeTJ = Umax[Var] - U[Var];			
		}
		DeltaJ = -AlphaJ;
		if ( DeltaJ < 0 ) { /* Mais la c'est jamais negatif puisque AlphaJ est negatif */
		  ValeurDeS += -DeltaJ * ValeurDeTJ;
      *YaUneVariableS = OUI_PNE;			 						
			NumeroDesVariablesSubstituees[*NombreDeVariablesSubstituees] = Var;
			if ( u_valide == OUI_PNE ) TypeDeSubsitution[*NombreDeVariablesSubstituees] = HIGH_VARBIN;
			else TypeDeSubsitution[*NombreDeVariablesSubstituees] = HIGH;				
			CoefficientDeLaVariableSubstituee[*NombreDeVariablesSubstituees] = DeltaJ;
			*NombreDeVariablesSubstituees = *NombreDeVariablesSubstituees + 1;						
		}
		continue;
	}
  else {
    /* Si AlphaJ > 0 on remplace par l'expression de sa borne inf */
    if ( l_valide == OUI_PNE ) {
		  /* On fait x = l * y + t */
						
      if ( T[VarBin_l] == 0 ) {
			  NumeroDeLaVariableBinaire[NbVarBinaires] = VarBin_l;
		    CoeffDeLaVariableBinaire[NbVarBinaires] = AlphaJ * l;
        T[VarBin_l] = NombreDeVariables + NbVarBinaires;
        NbVarBinaires++;
			}
      else {
			  Nb = T[VarBin_l] - NombreDeVariables;
		    CoeffDeLaVariableBinaire[Nb] += AlphaJ * l;
			}
			
			Beta -= AlphaJ * bBorne_l;
					 					
		  ValeurDeTJ = U[Var] - ( l * U[VarBin_l] ) - bBorne_l;
		}
		else {
		  /* Pas de borne inf variable */
			Beta -= AlphaJ * Umin[Var];
		  ValeurDeTJ = U[Var] - Umin[Var];			
		}
		DeltaJ = AlphaJ;
		if ( DeltaJ < 0 ) { /* Mais la c'est jamais negatif puisque AlphaJ est positif */
		  ValeurDeS += -DeltaJ * ValeurDeTJ;
      *YaUneVariableS = OUI_PNE;			 									
			NumeroDesVariablesSubstituees[*NombreDeVariablesSubstituees] = Var;
			if ( l_valide == OUI_PNE ) TypeDeSubsitution[*NombreDeVariablesSubstituees] = LOW_VARBIN;
			else TypeDeSubsitution[*NombreDeVariablesSubstituees] = LOW;							
			CoefficientDeLaVariableSubstituee[*NombreDeVariablesSubstituees] = DeltaJ;
			*NombreDeVariablesSubstituees = *NombreDeVariablesSubstituees + 1;					
		}
		continue;
	}
}

*b = Beta;
*CoeffDeLaVariableContinue = 1.0;
*ValeurDeLaVariableContinue = ValeurDeS;

/* On complete avec les variables binaires existantes */
IndexFin = NombreDeVariables - 1;
for ( i = 0 , Index = IndexFin ; i < NbVarEntieres ; i++ , Index-- ) {
  VarBin = NumeroDesVariables[Index];
  if ( T[VarBin] == 0 ) {
		NumeroDeLaVariableBinaire[NbVarBinaires] = VarBin;
		CoeffDeLaVariableBinaire[NbVarBinaires] = CoeffDesVariables[Index];
    T[VarBin] = NombreDeVariables + NbVarBinaires;
    NbVarBinaires++;
	}
  else {
		Nb = T[VarBin] - NombreDeVariables;
		CoeffDeLaVariableBinaire[Nb] += CoeffDesVariables[Index];
	}			
}

*NombreDeVariablesBinaires = NbVarBinaires;
CodeRet = OUI_PNE;

FinBoundSubstitution:

/* RAZ de T */
for ( i = 0 ; i < NbVarBinaires ; i++ ) T[NumeroDeLaVariableBinaire[i]] = 0;

return( CodeRet );
}

/*----------------------------------------------------------------------------*/

void PNE_VariableAvecContrainteSelectionnable ( PROBLEME_PNE * Pne, int Kappa, double AlfaJ, int * Cdeb, int * Csui, int * NumContrainte, char * SensContrainte,
                                                double * A, char * ContrainteMixte, char * ContrainteAgregee, int * ContrainteSaturee, 
                                                char NUtiliserQueLesContraintesSaturees, int * CntChoix, double * AlfaKappaChoix, int MxSelections )
{
int Cnt; int Cont; double AlfaKappa; int ic; int NbSelections; int NbVisites; int MxVisites; double a; int ic0;
char * ContrainteActivable;

ContrainteActivable = Pne->ContrainteActivable;
MxVisites = Pne->CNbTermTrav[Kappa];
ic0 = 0;
/*
if ( MxVisites > 10 ) {
  Pne->A1 = PNE_Rand( Pne->A1 );
  MxVisites = (int) floor( Pne->A1 * Pne->CNbTermTrav[Kappa] );
  if ( MxVisites > Pne->CNbTermTrav[Kappa] ) MxVisites = Pne->CNbTermTrav[Kappa];
  if ( MxVisites <= 0 ) MxVisites = 1;
}
*/

/*
Pne->A1 = PNE_Rand( Pne->A1 );
MxSelections = (int) floor( Pne->A1 * 10 );
if ( MxSelections > 10 ) MxSelections = 10;
if ( MxSelections <= 0 ) MxSelections = 1;
*/

NbVisites = 0;
NbSelections = 0;
Cnt = -1;
AlfaKappa = 0.;

ic = Cdeb[Kappa];
while ( ic >= 0 ) {
  if ( ic0 == 0 ) break;
	ic0--;
  ic = Csui[ic];	
}

if ( NUtiliserQueLesContraintesSaturees == OUI_PNE ) {
  while ( ic >= 0 ) {
	  if ( NbVisites >= MxVisites ) break;
	  if ( A[ic] != 0.0 && ContrainteActivable[NumContrainte[ic]] == OUI_PNE ) {
	    Cont = NumContrainte[ic];
	    if ( SensContrainte[Cont] == '=' ) {
        if ( ContrainteMixte[Cont] == OUI_PNE || 1 /*********/ ) {
				  if ( ContrainteAgregee[Cont] == NON ) {
					  if ( fabs( AlfaJ / A[ic] ) < RAPPORT_MAX_POUR_AGREGATION ) {
	            Cnt = Cont;
              AlfaKappa = A[ic];
					    NbSelections++;
		          if ( NbSelections >= MxSelections ) break;
            }
					}
				}
	    }    
		  else if ( ContrainteSaturee[Cont] == OUI_PNE ) {
        /* Dans le cas d'une contrainte d'inegalite le produit AlfaJ * AlfaKappa doit etre negatif */
				a = AlfaJ / A[ic];
        if ( a < 0.0 ) { 
          if ( ContrainteMixte[Cont] == OUI_PNE || 1 /*********/ ) {
					  if ( ContrainteAgregee[Cont] == NON ) {
						  if ( fabs( a ) < RAPPORT_MAX_POUR_AGREGATION ) {	
	              Cnt = Cont;
                AlfaKappa = A[ic];			
                NbSelections++;
		            if ( NbSelections >= MxSelections ) break;
							}
						}
          }
        }
      }
		  NbVisites++;
	  }
    ic = Csui[ic];
	}
}
else {
  while ( ic >= 0 ) {
	  if ( NbVisites >= MxVisites ) break;
	  if ( A[ic] != 0.0  && ContrainteActivable[NumContrainte[ic]] == OUI_PNE ) {
	    Cont = NumContrainte[ic];			
	    if ( SensContrainte[Cont] == '=' ) {
        if ( ContrainteMixte[Cont] == OUI_PNE || 1 /*********/ ) {
				  if ( ContrainteAgregee[Cont] == NON ) {
					  if ( fabs( AlfaJ / A[ic] ) < RAPPORT_MAX_POUR_AGREGATION ) {
	            Cnt = Cont;
              AlfaKappa = A[ic];
					    NbSelections++;
		          if ( NbSelections >= MxSelections ) break;
						}
					}
        }   
	    }   
	    else {
				a = AlfaJ / A[ic];
        if ( a < 0.0 ) {     
          if ( ContrainteMixte[Cont] == OUI_PNE || 1 /*********/ ) {
					  if ( ContrainteAgregee[Cont] == NON ) {
						  if ( fabs( a ) < RAPPORT_MAX_POUR_AGREGATION ) {	
	              Cnt = Cont;
                AlfaKappa = A[ic];
						    NbSelections++;
		            if ( NbSelections >= MxSelections ) break;
							}
						}
					}
        }
      }
		  NbVisites++;
    }
    ic = Csui[ic];
  }
}

*CntChoix = Cnt;
*AlfaKappaChoix = AlfaKappa;
 
return;
}

/*----------------------------------------------------------------------------*/

char PNE_Agregation( PROBLEME_PNE * Pne, char * VariableDansContrainteMixte, int * NbVarCont, int * NbVarEnt,
                     int * NumeroDesVariables, double * CoeffDesVariables,
										 double * SecondMembreContrainteAgregee, char * ContrainteAgregee,
										 char * VariableSelectionable, int * NbV, int * NumV, int * NbC, int * NumC	)
{
int IndexFin; int NombreDeVariables; int i; int Var; int * CntDeBorneSupVariable; int * CntDeBorneInfVariable;
double * X; char * ContrainteMixte; double b; double * B; int * Mdeb; int * NbTerm; double * A; 
double * Xmin; double * Xmax; int * Nuvar; double f; int VarBin; int il; int ilMax; double Fmax; int Kappa; double AlfaKappa;
int * Cdeb; int * Csui; int * NumContrainte; int Cnt; double AlfaJ; double AlfaKappaChoisi;
double Gamma; double * V; int * T; int Index; int * TypeDeVariable; double l; double u; char l_valide; char u_valide;
int * TypeDeBorne; int TypeBorne; char * SensContrainte; double bBorne; int CntChoisie; int NbVarContinues;
int NbVarEntieres; int Nn; int j; int * ContrainteSaturee; char NUtiliserQueLesContraintesSaturees;
int MxSelections; int NbSelections; int NbVisites; int MxVisites; int Offset; int Nselect; int Nvisites;

NbVarContinues = *NbVarCont;
NbVarEntieres = *NbVarEnt;

/* Attention a la restriction: pour l'instant on a 1 seule variable bound par variable */

NombreDeVariables = Pne->NombreDeVariablesTrav;
IndexFin = NombreDeVariables - 1;
CntDeBorneSupVariable = Pne->CntDeBorneSupVariable;
CntDeBorneInfVariable = Pne->CntDeBorneInfVariable;
X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;

# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

SensContrainte = Pne->SensContrainteTrav;

B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;   
Nuvar = Pne->NuvarTrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
ContrainteMixte = Pne->ContrainteMixte;

ContrainteSaturee = Pne->ContrainteSaturee;

if ( Pne->NombreDeContraintesTrav > Pne->NombreDeVariablesTrav && Pne->NombreDeContraintesTrav > 20000 ) NUtiliserQueLesContraintesSaturees = OUI_PNE;
else NUtiliserQueLesContraintesSaturees = NON_PNE;

/* La contrainte est rangee dans les vecteurs NumeroDesVariables CoeffDesVariables.
   Pour les variables continues on commence a 0. Pour les variables entieres on commence
	 a NombreDeVariablesTrav - 1 */

/* A essayer:
balayer les variables du graphe de conflit reliees aux variables entieres de la contrainte
on ne choisi les contraintes a concatener que parmi ces contraintes
*/

/*
Pne->A1 = PNE_Rand( Pne->A1 );
MxVisites = (int) floor( Pne->A1 * NbVarContinues );
if ( MxVisites > NbVarContinues ) MxVisites = NbVarContinues;
if ( MxVisites <= 0 ) MxVisites = 1;
*/

MxVisites = NbVarContinues;

Offset = 0;
Nvisites = 100;
if ( MxVisites > Nvisites ) {
  Offset = (int) floor( Pne->A1 * NbVarContinues );
  if ( Offset > NbVarContinues - Nvisites ) Offset = NbVarContinues - Nvisites;
  if ( Offset <= 0 ) Offset = 0;
  /*MxVisites	= Nvisites;*/
}

Nselect = 10;
/*
Pne->A1 = PNE_Rand( Pne->A1 );
MxSelections = (int) floor( Pne->A1 * Nselect );
if ( MxSelections > Nselect ) MxSelections = Nselect;
if ( MxSelections <= 0 ) MxSelections = 1;
*/

MxSelections = 1;

NbVisites = 0;
NbSelections = 0;

Fmax = -1.;
Kappa = -1;
AlfaJ = 0.0;
l = 0.0;
u = 0.0;
f = 0.0;

CntChoisie = -1;
AlfaKappaChoisi = VALEUR_NON_INITIALISEE;

/* Choix de la variable continue P* et de la contrainte a ajouter */

for ( i = Offset ; i < NbVarContinues ; i++ ) {
  Var = NumeroDesVariables[i];

  if ( VariableSelectionable[Var] == NON_PNE ) continue;
	
	/* Remarque: la seule contrainte mixte de la variable a pu etre agregee mais comme
	   c'est long a tester on ne le fait pas */
	if ( VariableDansContrainteMixte[Var] == 0 && 0 ) continue;

  if ( Xmin[Var] == Xmax[Var] ) continue;

	if ( NbVisites >= MxVisites ) break;	
	
	/* Presence d'une borne inf ou sup variable */
	TypeBorne = TypeDeBorne[Var];		
	l_valide = NON_PNE;
	u_valide = NON_PNE;  
	bBorne = 0.0;	
	if ( CntDeBorneInfVariable != NULL ) {
    if ( CntDeBorneInfVariable[Var] >= 0 ) {
		  Cnt = CntDeBorneInfVariable[Var];			
			bBorne = -B[Cnt];			
		  il = Mdeb[Cnt];
		  VarBin = Nuvar[il]; /* Ne sert plus a rien */
	    l = A[il];
		  l_valide = OUI_PNE;
	  }
	}
	if ( CntDeBorneSupVariable != NULL ) {
    if ( CntDeBorneSupVariable[Var] >= 0 ) {
		  Cnt = CntDeBorneSupVariable[Var];			
			bBorne = B[Cnt];
		  il = Mdeb[Cnt];			
	    u = -A[il];
		  u_valide = OUI_PNE;
	  }  
	}
  if ( l_valide == NON_PNE ) {
    if ( TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT || TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES ) {
	    l = Xmin[Var];
		  l_valide = OUI_PNE;
		}
	}
  if ( u_valide == NON_PNE ) {
    if ( TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT || TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES ) {
	    u = Xmax[Var];
		  u_valide = OUI_PNE;
	  }
  }
	
	if ( u_valide == NON_PNE && l_valide == NON_PNE ) {
    f = LINFINI_PNE;
  }	
	else if ( u_valide == OUI_PNE ) {
	  f = fabs( u - X[Var] );
    if ( l_valide ) {
	    if ( f > fabs( X[Var] - l ) ) f = fabs( X[Var] - l );
		}
	}
	else if ( l_valide == OUI_PNE ) f = fabs( X[Var] - l );
	
	/* La variable ne doit pas etre sur borne */
	if ( f < Z0 ) {
	  if ( VariableSelectionable[Var] == OUI_PNE ) {
	    VariableSelectionable[Var] = NON_PNE;
		  NumV[*NbV] = Var;
		  *NbV = *NbV + 1;
		}
		continue;
	}
	if ( f > Fmax ) {

	  NbVisites++;
	
    PNE_VariableAvecContrainteSelectionnable ( Pne, Var, CoeffDesVariables[i], Cdeb, Csui, NumContrainte, SensContrainte,
                                               A, ContrainteMixte, ContrainteAgregee , ContrainteSaturee, 
                                               NUtiliserQueLesContraintesSaturees, &Cnt, &AlfaKappa, MxSelections );  
    if ( Cnt >= 0 ) {		
      Fmax = f;
		  Kappa = Var;
		  AlfaJ = CoeffDesVariables[i]; 
      CntChoisie = Cnt;
      AlfaKappaChoisi = AlfaKappa;
      NbSelections++;
			if ( NbSelections >= MxSelections ) break;
    }  
	  else {
	    VariableSelectionable[Var] = NON_PNE;
		  NumV[*NbV] = Var;
		  *NbV = *NbV + 1;
		}   
	}
}

if ( Kappa < 0 || CntChoisie < 0 || AlfaKappaChoisi == VALEUR_NON_INITIALISEE ) {
  /* Rien a agreger */
	return( NON_PNE );
}

Cnt = CntChoisie;
AlfaKappa = AlfaKappaChoisi;

VariableSelectionable[Kappa] = NON_PNE;
NumV[*NbV] = Kappa;
*NbV = *NbV + 1;	 

ContrainteAgregee[Cnt] = OUI_PNE;	
NumC[*NbC] = Cnt;
*NbC = *NbC + 1;

Gamma = -( AlfaJ / AlfaKappa );

V = (double *) Pne->Coefficient_CG;
T = (int *) Pne->IndiceDeLaVariable_CG;

/* Expand de la contrainte dans V */
b = B[Cnt];
il = Mdeb[Cnt];
ilMax = il + NbTerm[Cnt];
while ( il < ilMax ) {
  Var = Nuvar[il];
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) {
    b -= A[il] * X[Var];
	}	
  else {
	  if ( A[il] != 0.0 ) {
      V[Var] = Gamma * A[il];
	    T[Var] = 1;
		}
	}	
  il++;
}
b *= Gamma;

/* Combinaision avec l'existant */
for ( i = 0 ; i < NbVarContinues ; i++ ) {
  Var = NumeroDesVariables[i];
	if ( T[Var] == 1 ) {
	  CoeffDesVariables[i] += V[Var];		
		V[Var] = 0.0;
		T[Var] = 0;
	}	
}
for ( i = 0 , Index = IndexFin ; i < NbVarEntieres ; i++ , Index-- ) {
  Var = NumeroDesVariables[Index];	
	if ( T[Var] == 1 ) {
    CoeffDesVariables[Index] += V[Var];
		V[Var] = 0.0;
		T[Var] = 0;			
  }	
}

/* On complete avec la partie de la contrainte ajoutee qui n'a pas pu etre prise en compte */
il = Mdeb[Cnt];
ilMax = il + NbTerm[Cnt];
while ( il < ilMax ) {
  Var = Nuvar[il];		
	if ( T[Var] != 0 ) {
    if ( TypeDeVariable[Var] == ENTIER ) {
      CoeffDesVariables[Index] = V[Var];
		  NumeroDesVariables[Index] = Var;
      Index--;
			NbVarEntieres++;
		}
		else {
	    CoeffDesVariables[NbVarContinues] = V[Var];
		  NumeroDesVariables[NbVarContinues] = Var;
		  NbVarContinues++;	
		}
		V[Var] = 0.0;
		T[Var] = 0;		
	}
  il++;
}

/* On enleve tous les coeff de variables continues qui sont a 0 et au passage la 
   variable continue Kappa dont le coefficient est devenu nul */
Nn = 0;
for ( i = 0 ; i < NbVarContinues ; i++ ) {
	if ( fabs( CoeffDesVariables[i] ) < VALEUR_NULLE_DUN_COEFF ) {	
	  for ( j = i + 1 ; j < NbVarContinues ; j++ ) {
		  if ( fabs( CoeffDesVariables[j] ) >= VALEUR_NULLE_DUN_COEFF ) {
		    CoeffDesVariables[i] = CoeffDesVariables[j];
				CoeffDesVariables[j] = 0.0;
			  NumeroDesVariables[i] = NumeroDesVariables[j];
				Nn++;
				break;
			}
		}
	}
	else Nn++;
} 
NbVarContinues = Nn;

/* On enleve tous les coeff de variables entieres qui sont a 0 */
Nn = 0;
for ( i = 0 , Index = IndexFin ; i < NbVarEntieres ; i++ , Index-- ) {
	if ( fabs( CoeffDesVariables[Index] ) < VALEUR_NULLE_DUN_COEFF ) {	
	  for ( j = Index - 1 ; j >= IndexFin - NbVarEntieres + 1 ; j-- ) {		
	    if ( fabs( CoeffDesVariables[j] ) >= VALEUR_NULLE_DUN_COEFF ) { 
		    CoeffDesVariables[Index] = CoeffDesVariables[j];
				CoeffDesVariables[j] = 0.0;
			  NumeroDesVariables[Index] = NumeroDesVariables[j];
				Nn++;
				break;
	 		}
    }
	}
	else Nn++;
}
NbVarEntieres = Nn;

*SecondMembreContrainteAgregee = *SecondMembreContrainteAgregee + b;  

*NbVarCont = NbVarContinues;
*NbVarEnt = NbVarEntieres;

/* Construction de la constrainte sur laquelle on va faire la MIR */
return( OUI_PNE );
}

/*----------------------------------------------------------------------------*/
  
void PNE_MIRMarchandWolsey( PROBLEME_PNE * Pne )
{
int Cnt; int Var; char * ContrainteMixte; int il; int ilMax; int * Mdeb; int * NbTerm;
int * NumeroDesVariables; double * CoeffDesVariables; int NbVarContinues; int NbVarEntieres;
double * A; int * Nuvar; int IndexFin; int Index; char * ContrainteAgregee; int * TypeDeVariable; 
char Agreg; char OK; double * B; double * X; int NombreDeVariablesBinaires; int * NumeroDeLaVariableBinaire; 
double * CoeffDeLaVariableBinaire; double b; double CoeffDeLaVariableContinue; double ValeurDeLaVariableContinue;
double SecondMembreContrainteAgregee; int NbAgr; char MIRviolee; int NombreDeVariablesSubstituees;
int * NumeroDesVariablesSubstituees; double * CoefficientDeLaVariableSubstituee; char * TypeDeSubsitution; 
int NombreDeVariables; int NombreDeContraintes; int * TypeDeBorne; int * TesterMirMarchandWolseySurLaContrainte;
char * VariableDansContrainteMixte; int NbMIRCreees; char YaUneVariableS; double * L; BB * Bb; 
char * VariableSelectionable; double * Xmin; double * Xmax; int NbV; int * NumV; int NbC; 
int * NumC; int NbCntTestees; int MaxAgreg; char MirSurLaContrainte; char MirAcceptee;
int * Cdeb; int * Csui; int * NumContrainte;  int * Ic; int Nb; int i; int ic; char OnReboucle;
int ProfondeurDuNoeud; char * ContrainteActivable;


/******************** test 31/05/2016 ***********************/
/*
Bb = (BB *) Pne->ProblemeBbDuSolveur;
if ( Pne->YaUneSolutionEntiere == NON_PNE ) {
  if ( Bb->NoeudEnExamen != Bb->NoeudRacine ) return;
}
else if ( Bb->EcartBorneInf > 2 ) return;
*/
/********************* fin test et ca marche pas mal d'apres les essais **********************/


if ( Pne->ContrainteMixte == NULL ) return;
/*printf("     PNE_MIRMarchandWolsey\n");*/

Bb = (BB *) Pne->ProblemeBbDuSolveur;

if ( Bb->NoeudEnExamen->ProfondeurDuNoeud > 100 && Pne->YaUneSolutionEntiere == OUI_PNE ) return; 

ProfondeurDuNoeud = Bb->NoeudEnExamen->ProfondeurDuNoeud;

if ( ProfondeurDuNoeud > 1 ) MaxAgreg = MAX_AGREG_AU_AUTRES_NIVEAU;
else if ( ProfondeurDuNoeud == 1 ) MaxAgreg = MAX_AGREG_AU_NIVEAU_UN;
else MaxAgreg = MAX_AGREG_AU_NOEUD_RACINE;

NbMIRCreees = 0;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

CoefficientDeLaVariableSubstituee = Pne->FlottantBanaliseEnNombreDeVariables_1;
CoeffDeLaVariableBinaire = Pne->FlottantBanaliseEnNombreDeVariables_2;

NumeroDesVariablesSubstituees = Pne->EntierBanaliseEnNombreDeVariables_1;
NumeroDeLaVariableBinaire = Pne->EntierBanaliseEnNombreDeVariables_2;
NumV = Pne->EntierBanaliseEnNombreDeVariables_3;

TypeDeSubsitution = Pne->CharBanaliseEnNombreDeVariables_1;
VariableDansContrainteMixte = Pne->CharBanaliseEnNombreDeVariables_2;
VariableSelectionable = Pne->CharBanaliseEnNombreDeVariables_3;

NumC = Pne->EntierBanaliseEnNombreDeContraintes_1;

ContrainteAgregee = Pne->CharBanaliseEnNombreDeContraintes_1;

L = Pne->LTrav;
X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;

# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;

ContrainteActivable = Pne->ContrainteActivable;
ContrainteMixte = Pne->ContrainteMixte;
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

TesterMirMarchandWolseySurLaContrainte = Pne->TesterMirMarchandWolseySurLaContrainte;
  
CoeffDesVariables  = Pne->ValeurLocale;
NumeroDesVariables = Pne->IndiceLocal;

IndexFin = NombreDeVariables - 1;

memset( (char *) VariableDansContrainteMixte, 0, NombreDeVariables * sizeof( char ) );

memset( (char *) ContrainteAgregee, NON_PNE, NombreDeContraintes * sizeof( char ) );
memset( (char *) VariableSelectionable, OUI_PNE, NombreDeVariables * sizeof( char ) );

/* RAZ de Coefficient_CG et IndiceDeLaVariable_CG car utilises et razes ensuite
   dans la phase d'agregation */
memset( (char *) Pne->Coefficient_CG, 0, NombreDeVariables * sizeof( double ) );
memset( (char *) Pne->IndiceDeLaVariable_CG, 0, NombreDeVariables * sizeof( int ) );

/* Classement du chainage des colonnes dans l'ordre croissant du nombre de termes des contraintes.
   On le fait seulement au noeud racine et c'est valable pour toute la suite.
	 De cet fait on n'impose plus d'avoir les contraintes egalite en premier */	 
if ( Bb->NoeudEnExamen->ProfondeurDuNoeud == 0 && 0 ) {
  /* On pourra tranferer ca dans le calcul du stockage par colonne car ici on le fait intulement plusieurs fois */
  Cdeb = Pne->CdebTrav;
  Csui = Pne->CsuiTrav;
  NumContrainte = Pne->NumContrainteTrav;

  Ic = (int *) malloc( NombreDeContraintes * sizeof( int ) );
  if ( Ic == NULL ) {
      printf(" Solveur PNE , memoire insuffisante. Sous-programme: PNE_MIRMarchandWolsey \n");
      Pne->AnomalieDetectee = OUI_PNE;
      longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
  }

  for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
    ic = Cdeb[Var];
	  Nb = 0;
	  while ( ic >= 0 ) {
	    Cnt = NumContrainte[ic];
      NumC[Nb] = NumContrainte[ic];
		  Ic[Nb] = ic;
		  Nb++;
		  ic = Csui[ic];
	  }
    /* Tri bulle (c'est pas le plus rapide mais on ne le fera pas a chaque fois */
    OnReboucle = OUI_PNE;
    while ( OnReboucle == OUI_PNE ) {
      OnReboucle = NON_PNE;
      for ( i = 0 ; i < Nb - 1 ; i++ ) {
        if ( NbTerm[NumC[i]] > NbTerm[NumC[i+1]] ) {
          OnReboucle = OUI_PNE;
          ic = NumC[i+1];
			    NumC[i+1] = NumC[i];
			    NumC[i] = ic;
		      ic = Ic[i+1];
			    Ic[i+1] = Ic[i];
			    Ic[i] = ic;						
		    }
	    }
    }
	  /* On refait le chainage */
    Cdeb[Var] = -1;
	  if ( Nb > 0 ) {
	    i = 0;
      ic = Ic[i];
		  Cnt = NumC[i];
		  Cdeb[Var] = ic;
		  NumContrainte[ic] = Cnt;
		  i++;
		  for ( ; i < Nb ; i++ ) {
        Csui[ic] = Ic[i];
			  ic = Ic[i];
		    Cnt = NumC[i];
		    NumContrainte[ic] = Cnt;
		  }
		  Csui[ic] = -1;
	  }	
  }
  free( Ic );
}

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  if ( ContrainteMixte[Cnt] == NON_PNE ) continue;
  il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
	  VariableDansContrainteMixte[Nuvar[il]] = 1;
		il++;
  }	
}

/* Choix d'une contrainte mixte pour demarrer le processus d'agregation */
NbCntTestees = 0;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {

  if ( ContrainteActivable[Cnt] == NON_PNE ) continue;
	
  if ( TesterMirMarchandWolseySurLaContrainte[Cnt] <= 0 && 0 ) continue;

  if ( ContrainteMixte[Cnt] == NON_PNE && 0 ) continue;

  if ( Pne->FoisCntSuccesMirMarchandWolseyTrouvees[Cnt] > MAX_FOIS_SUCCESS_MIR && 0 ) continue;	
	
	/* On tente Marchand-Wolsey */
  /*
	printf(" Tentative Marchand Wolsey sur Cnt %d \n",Cnt);  	
	*/
	NbVarContinues = 0;
	NbVarEntieres = 0; 
	
	if ( NbTerm[Cnt] > 10000 ) { 
	  /*printf(" Tentative Marchand Wolsey rejetee car NbTerm %d \n",Pne->NbTermTrav[Cnt]);*/	
	  continue;
	}
  else if ( ProfondeurDuNoeud > 0 ) {
	  if ( NbTerm[Cnt] > 1000 ) { 
	    /* Lorsque la contrainte a trop de termes le calcul peut etre tres couteux */	
	    continue;
    }
    else if ( ProfondeurDuNoeud > 1 ) {
      if ( NbTerm[Cnt] > 100 ) {
        MaxAgreg = MAX_AGREG_SI_BEAUCOUP_DE_TERMES;
      }
    }
  }
	
	SecondMembreContrainteAgregee = B[Cnt];
	il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
	Index = IndexFin;
  while ( il < ilMax ) {
	  if ( A[il] == 0.0 ) goto NextIl;
	  Var = Nuvar[il];
    if ( TypeDeBorne[Var] == VARIABLE_FIXE ) {
      SecondMembreContrainteAgregee -= A[il] * X[Var];
			goto NextIl;
		}
		else if ( Xmin[Var] == Xmax[Var] ) {
      SecondMembreContrainteAgregee -= A[il] * Xmin[Var];
			goto NextIl;
		}		
    if ( TypeDeVariable[Var] == ENTIER ) {		
	    CoeffDesVariables[Index] = A[il];
		  NumeroDesVariables[Index] = Var;
		  Index--;
		  NbVarEntieres++;
    }
	  else {		
	    CoeffDesVariables[NbVarContinues] = A[il];
		  NumeroDesVariables[NbVarContinues] = Var;
		  NbVarContinues++;	
	  }
		NextIl:
		il++;
	}

/*if ( NbVarContinues == 0 || NbVarEntieres == 0 ) continue;*/ /* Si que des variables entieres alors on a essaye une knapsack dessus */

  NbAgr = 1;
	NbC = 0;
	NbV = 0;

  ContrainteAgregee[Cnt] = OUI_PNE;
  NumC[NbC] = Cnt;
  NbC = NbC + 1;

  NbCntTestees++;
	MirSurLaContrainte = NON_PNE;
	
  DEBUT_MIR:
		
	if ( NbVarContinues + NbVarEntieres == 0 ) goto RAZ;
		
  OK = PNE_BoundSubstitution( Pne, NbVarContinues, NbVarEntieres, NumeroDesVariables, CoeffDesVariables,
                            	SecondMembreContrainteAgregee,
	                            /* En sortie, la contrainte pour y faire une MIR */                             
															&NombreDeVariablesBinaires, NumeroDeLaVariableBinaire,
                              CoeffDeLaVariableBinaire, &b, &CoeffDeLaVariableContinue,
								              &ValeurDeLaVariableContinue,
															&NombreDeVariablesSubstituees, NumeroDesVariablesSubstituees, TypeDeSubsitution,
														  CoefficientDeLaVariableSubstituee, &YaUneVariableS );
															
	if ( OK == OUI_PNE && NombreDeVariablesBinaires >= 1 ) {
	  /* Si la valeur de la variable continue est negative, c'est a cause des imprecisions et on evite de faire un calcul */
		if ( ValeurDeLaVariableContinue < 0.0 ) goto RAZ;
 	
    /* cMIR separation */		
	  /*printf("2- Avant CMIR NombreDeVariablesBinaires %d b = %e ValeurDeLaVariableContinue = %e\n",NombreDeVariablesBinaires,b,ValeurDeLaVariableContinue);*/		
		
    MIRviolee = PNE_C_MIR( Pne, NombreDeVariablesBinaires, NumeroDeLaVariableBinaire,
                           CoeffDeLaVariableBinaire, &b, &CoeffDeLaVariableContinue,
							             ValeurDeLaVariableContinue );

    if ( MIRviolee == OUI_PNE ) {      
		  /*
      printf("MIR violee, contrainte de depart: %d - nb termes de la contrainte: %d - NbAgr: %d FoisCntSuccesMirMarchandWolseyTrouvees %d\n",
              Cnt,NbTerm[Cnt],NbAgr,Pne->FoisCntSuccesMirMarchandWolseyTrouvees[Cnt]);
			*/
      			
	    PNE_SyntheseEtStockageMIR_New( Pne, NombreDeVariablesBinaires, NumeroDeLaVariableBinaire,
                                     CoeffDeLaVariableBinaire, b, CoeffDeLaVariableContinue,
								                     NombreDeVariablesSubstituees,
																     NumeroDesVariablesSubstituees, TypeDeSubsitution, 
																     CoefficientDeLaVariableSubstituee, &MirAcceptee );

      if ( MirAcceptee == OUI_PNE ) {
		    NbMIRCreees++;
    	  MirSurLaContrainte = OUI_PNE;
/*
printf("Contrainte MIR sur contrainte %d avec %d termes NbVarEntieres %d\n",Cnt,Pne->NbTermTrav[Cnt],NbVarEntieres);
*/
			  if ( NbAgr == 1 ) Pne->FoisCntSuccesMirMarchandWolseyTrouvees[Cnt]++;
			}	
																 
		  goto RAZ;
	  }
	}
		
	/*printf("3- Avant Agregation\n");*/

	/* Attention. Dans l'article Marchand-Wolsey il est dit qu'on peut aller jusqu'a 5. Mais experimentalement
	   on constate qu'il ne faut aller aussi loin a cause des imprecisions */
	if ( NbAgr < MaxAgreg ) {
    /* memset( (char *) VariableSelectionable, OUI_PNE, NombreDeVariables * sizeof( char ) ); */	
	  /*printf("3- PNE_Agregation\n");*/		
    Agreg = PNE_Agregation( Pne, VariableDansContrainteMixte, &NbVarContinues, &NbVarEntieres, NumeroDesVariables,
		                        CoeffDesVariables, &SecondMembreContrainteAgregee, ContrainteAgregee, VariableSelectionable,
														&NbV, NumV, &NbC, NumC );														
		if ( Agreg == NON_PNE || NbVarContinues + NbVarEntieres == 0 ) {
		  /*printf("Echec agregation NbAgr %d\n",NbAgr);*/		
		  goto RAZ;
		}		
	  NbAgr++;
		
	  goto DEBUT_MIR;
	}
	else {	  
	  /*printf(" Marchand Wolsey arretee car NbAgr > MAX_AGREG \n");*/		
	}

	RAZ:

  if ( MirSurLaContrainte == NON_PNE ) TesterMirMarchandWolseySurLaContrainte[Cnt] -= 1;
  else TesterMirMarchandWolseySurLaContrainte[Cnt] = NOMBRE_MAX_ECHECS_MIR_MARCHAND_WOLSEY_SUR_UNE_CONTRAINTE;

  for ( il = 0 ; il < NbV ; il++ ) VariableSelectionable[NumV[il]] = OUI_PNE;
  for ( il = 0 ; il < NbC ; il++ ) {
	  if ( NumC[il] < 0 || NumC[il] >= NombreDeContraintes ) {
      printf("Bug NumC[%d] = %d\n",il,NumC[il]);
			exit(0);
		}
	  ContrainteAgregee[NumC[il]] = NON_PNE;
	}
	
}

if ( NbMIRCreees == 0 ) {	
  Pne->NbEchecsConsecutifsDeCalculsMIRmarchandWolsey++;
	if ( Pne->NbEchecsConsecutifsDeCalculsMIRmarchandWolsey >= NB_ECHECS_INHIB_MIR ) {
    Pne->CalculDeMIRmarchandWolsey = NON_PNE;
	}
}
else {
  /* On a cree une ou plusieurs MIR => on remet le compteur a 0 */
	Pne->NbEchecsConsecutifsDeCalculsMIRmarchandWolsey = 0;
	if ( Bb->NoeudEnExamen->ProfondeurDuNoeud + 1 > Pne->ProfondeurMirMarchandWolseTrouvees ) {
    Pne->ProfondeurMirMarchandWolseTrouvees = Bb->NoeudEnExamen->ProfondeurDuNoeud + 1;
  }
  if ( Pne->AffichageDesTraces == OUI_PNE ) {
	  /*
		if ( NbMIRCreees != 0 )printf("Mir cuts found %d at depth %d\n",NbMIRCreees,Bb->NoeudEnExamen->ProfondeurDuNoeud);
	  fflush( stdout );
		*/
  }				
}

		/*
		if ( NbMIRCreees != 0 ) printf("Mir cuts found %d at depth %d     NbCntTestees %d\n",NbMIRCreees,Bb->NoeudEnExamen->ProfondeurDuNoeud,NbCntTestees);
    */

return;
}

/*----------------------------------------------------------------------------*/

