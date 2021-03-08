/***********************************************************************

   FONCTION: Amelioration des coefficients des variables entieres 
             (cas des variables binaires uniquement).
						 La description de la methode se trouve dans l'article:
						 "Computational Integer Programming and cutting planes"
             Armin Fugenschuh & Alexander Martin, page 6, 2001.
						 Mais on peut aussi la trouver dans d'autres articles.
						 Bien que ce calcul soit fait dans le presolve, on le fait
						 aussi apres le variable probing.
						 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "prs_define.h"
# include "prs_fonctions.h"
  
# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES 0      

# define ZERO 1.e-10

# define GRAND_COEFFICIENT 0. /* 10 */

# define MODIFICATION_MINIMALE 1 
/*********************************/ # define IGNORER_LES_COEFFS_NON_NULS_TROP_PETITS NON_PNE /*OUI_PNE*/

# define MARGE_SI_MODE_PRESOLVE 1.e-8 /*1.e-8*/
# define MARGE_SI_MODE_PNE      1.e-4 /*1.e-4*/
# define BRUITER_LA_MARGE       NON_PNE
# define COEFF_MARGE_A_BRUITER  0.1

# define APPLIQUER_UNE_MARGE_QUE_SI_VALEUR_FRACTIONNAIRE OUI_PNE

void PNE_ReCalculMaxApresAmeliorationCoeff( int * , int * , int * , double * , double * , double * , double * , double * , int ,
																	  				char , int * , char * , char * , int * , int * );
																					
/*----------------------------------------------------------------------------*/

void PNE_ReCalculMaxApresAmeliorationCoeff( int * Mdeb, int * NbTerm, int * Nuvar, double * A,
                                            double * X, double * Xmin, double * Xmax, double * Bmax, int Cnt,
																				 	  char Mode, int * TypeDeBornePourPresolve,
																						char * ConserverLaBorneInfDuPresolve, char * ConserverLaBorneSupDuPresolve,
																						int * ContrainteBornanteInferieurement, int * ContrainteBornanteSuperieurement )
{ 
double Smax; int il; int ilMax; int Var;
/* Pas utile si on ne fait qu'une seul iteration */
/* Smax est toujours calculable quand on appelle ce module */

Smax = 0;
il = Mdeb[Cnt];
ilMax = il + NbTerm[Cnt];
if ( Mode == MODE_PRESOLVE ) {
  while ( il < ilMax ) {
	  Var = Nuvar[il];
	  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) Smax += A[il] * X[Var];	
    if ( A[il] > 0.0 ) {		
		  Smax += A[il] * Xmax[Var];
		  /* Si on est en mode presolve il faut conserver la borne de la variable */
      if ( ContrainteBornanteSuperieurement[Var] == Cnt ) {
	      ConserverLaBorneSupDuPresolve[Var] = OUI_PNE;
			}
		}			
    else {		
		  Smax += A[il] * Xmin[Var];
		  /* Si on est en mode presolve il faut conserver la borne de la variable */
      if ( ContrainteBornanteInferieurement[Var] == Cnt ) {
	      ConserverLaBorneInfDuPresolve[Var] = OUI_PNE;
			}
	  }		
    il++;
  }
}
else {
  while ( il < ilMax ) {
    if ( A[il] > 0.0 ) Smax += A[il] * Xmax[Nuvar[il]];					
    else if ( A[il] < 0.0 ) Smax += A[il] * Xmin[Nuvar[il]];					
    il++;
  }
}
Bmax[Cnt] = Smax;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_AmeliorerLesCoefficientsDesVariablesBinaires( PROBLEME_PNE * Pne,
                                                       void * Prs,
																											 char Mode ) 
{
int Var; int Cnt; int ic; double Smax; int icEntier; int NombreDeVariables; double Coeff;
int * TypeDeVariable; int * Cdeb; int * Csui; int * NumContrainte; char * SensContrainte;
int * Mdeb; int * NbTerm; int * Nuvar; double NouveauCoeff; double * B; double * A; double PlusPetitTerme;
double * Xmin; double * Xmax; int NbIter; char CoeffModifie; double * Bmax; char * BmaxValide;
char * BorneInfConnue; int * TypeDeBornePourPresolve; char * ContrainteInactive; int Faisabilite;
double Marge; int NbModifications; int NbC; PRESOLVE * Presolve; double EcartRelatif; 
double EcartAbsolu; double C; double * X; char * ConserverLaBorneInfDuPresolve; char * ConserverLaBorneSupDuPresolve;
int * ContrainteBornanteInferieurement; int * ContrainteBornanteSuperieurement;

if ( Pne->YaDesVariablesEntieres != OUI_PNE ) return;
   
if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

Presolve = (PRESOLVE *) Prs;

if ( Mode == MODE_PNE ) {
  # if TRACES == 1
	  printf("AmeliorerLesCoefficientsDesVariablesBinaires: MODE_PNE\n");
	# endif	
  PNE_InitBorneInfBorneSupDesVariables( Pne );	
  PNE_CalculMinEtMaxDesContraintes( Pne, &Faisabilite );	
}
else if ( Mode == MODE_PRESOLVE ) {
  # if TRACES == 1
	  printf("AmeliorerLesCoefficientsDesVariablesBinaires: MODE_PRESOLVE\n");
	# endif	
  PRS_CalculerLesBornesDeToutesLesContraintes( Presolve, &NbModifications );	
}
else return;

Marge = MARGE_REDUCTION; /* Rq: MARGE_REDUCTION ne sert plus */

NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
PlusPetitTerme = Pne->PlusPetitTerme;
NbC = 0;

X = NULL;
Xmin = NULL;
Xmax = NULL;
BorneInfConnue = NULL;
BmaxValide = NULL;
Bmax = NULL;
TypeDeBornePourPresolve = NULL;
ContrainteInactive = NULL;
ConserverLaBorneInfDuPresolve = NULL;
ConserverLaBorneSupDuPresolve = NULL;
ContrainteBornanteInferieurement = NULL;
ContrainteBornanteSuperieurement = NULL;																						 

if ( Mode == MODE_PNE ) {
  Xmin = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;
  Xmax = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
  BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
  BmaxValide = Pne->ProbingOuNodePresolve->BmaxValide;
  Bmax = Pne->ProbingOuNodePresolve->Bmax;
}
else {
  X = Presolve->ValeurDeXPourPresolve;
  Xmin = Presolve->BorneInfPourPresolve;
  Xmax = Presolve->BorneSupPourPresolve;
  TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
  ContrainteInactive = Presolve->ContrainteInactive;
  BmaxValide = Presolve->MaxContrainteCalcule;
  Bmax = Presolve->MaxContrainte;
  ConserverLaBorneInfDuPresolve = Presolve->ConserverLaBorneInfDuPresolve;
  ConserverLaBorneSupDuPresolve	= Presolve->ConserverLaBorneSupDuPresolve;
  ContrainteBornanteInferieurement = Presolve->ContrainteBornanteInferieurement;
  ContrainteBornanteSuperieurement = Presolve->ContrainteBornanteSuperieurement;																					 
}

NbIter = 0;
Debut:
CoeffModifie = NON_PNE;
		
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( TypeDeVariable[Var] != ENTIER ) continue;
	if ( Mode == MODE_PNE ) {
    if ( BorneInfConnue[Var] == FIXE_AU_DEPART ) continue;
	}
	else {
    if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) continue;
	}  
  ic = Cdeb[Var];
  while ( ic >= 0 ) {
    Cnt = NumContrainte[ic];
    if ( SensContrainte[Cnt] == '=' ) goto ContrainteSuivante;    		   
	  if ( Mode == MODE_PRESOLVE ) {
      if ( ContrainteInactive[Cnt] == OUI_PNE ) goto ContrainteSuivante;
		}		
    /* La contrainte est donc de type < , calcul du max du membre de gauche */
		icEntier = ic;
		Coeff = A[icEntier];
    if ( Coeff == 0.0 ) goto ContrainteSuivante;

    if ( fabs( Coeff ) < GRAND_COEFFICIENT ) goto ContrainteSuivante;
				
		if ( BmaxValide[Cnt] == OUI_PNE ) Smax = Bmax[Cnt];		
		else goto ContrainteSuivante;
		
		if ( Smax <= B[Cnt] ) goto ContrainteSuivante; /* Contrainte redondante */    
		
    if ( Coeff < 0.0 ) {					
      if ( Smax + Coeff < B[Cnt] - EPS_COEFF_REDUCTION ) {  
        /* On peut diminuer le coeff de la variable entiere */
				NouveauCoeff = B[Cnt] - Smax;

        /* Modifs du 23/8/16 : on met a jour dynamiquement la marge */				
				if ( Mode == MODE_PRESOLVE ) Marge = MARGE_SI_MODE_PRESOLVE;
        else Marge = MARGE_SI_MODE_PNE;

        # if APPLIQUER_UNE_MARGE_QUE_SI_VALEUR_FRACTIONNAIRE == OUI_PNE				
			    if ( PNE_LaValeurEstEntiere( &NouveauCoeff ) == OUI_PNE ) {
					  /*printf("Pas de marge\n");*/
					  Marge = 0;
					}
				# endif				

        # if BRUITER_LA_MARGE == OUI_PNE
          Pne->A1 = PNE_Rand( Pne->A1 );
					Marge -= Pne->A1 * ( COEFF_MARGE_A_BRUITER * Marge );
				# endif				

        /* Modifs du 23/8/16 : on ne tient compte de la marge que si on ne tombe pas sur un coeff nul */
        if ( NouveauCoeff != 0 ) NouveauCoeff -= Marge;
				
				if ( fabs( NouveauCoeff ) < ZERO ) NouveauCoeff = 0;				

				if ( NouveauCoeff != 0 ) {				
          if ( fabs( NouveauCoeff ) < PlusPetitTerme ) {
            # if IGNORER_LES_COEFFS_NON_NULS_TROP_PETITS == OUI_PNE					
					    goto ContrainteSuivante;
						# endif						
						NouveauCoeff = -PlusPetitTerme;
					}
				}							

				/* C'est con c'est fait juste apres if ( fabs( NouveauCoeff ) > fabs( Coeff ) ) goto ContrainteSuivante; */
								
        EcartAbsolu	= fabs( Coeff - NouveauCoeff );
				if ( EcartAbsolu < MODIFICATION_MINIMALE ) {
				  /*printf("Echec EcartAbsolu %e\n",EcartAbsolu);*/
				  goto ContrainteSuivante;
				}
        EcartRelatif = fabs( EcartAbsolu / Coeff );
			  if ( EcartRelatif < ECART_RELATIF_MIN ) {
				  /*printf("Echec EcartRelatif %e\n",EcartRelatif);*/
				  goto ContrainteSuivante;
				}
				
				# if TRACES == 1
	        printf("1- Variable entiere %d contrainte %d, remplacement de son coefficient %e par %e \n",Var,Cnt,Coeff,NouveauCoeff);
			  # endif
				
				/* Modif du coefficient */
        A[icEntier] = NouveauCoeff;
				CoeffModifie = OUI_PNE;
				
				/* En mode presolve on doit invalider la connaissance des variables duales puisque ce n'est plus la meme matrice */
        if ( Mode != MODE_PNE ) {
				  Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_NON_INITIALISE;		 
          Presolve->LambdaMin[Cnt] = -LINFINI_PNE;
	        Presolve->LambdaMax[Cnt] = LINFINI_PNE;							
				  if ( SensContrainte[Cnt] == '<' ) {	
            Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;
	          Presolve->LambdaMax[Cnt] = 0;
	        }							
				}
				
				/* Mise a jour des limites des contraintes */
        PNE_ReCalculMaxApresAmeliorationCoeff( Mdeb, NbTerm, Nuvar, A, X, Xmin, Xmax, Bmax, Cnt,
																					     Mode, TypeDeBornePourPresolve,
																						   ConserverLaBorneInfDuPresolve, ConserverLaBorneSupDuPresolve,
																						   ContrainteBornanteInferieurement, ContrainteBornanteSuperieurement );																							 
				NbC++;				
			}
		}
		else if ( Coeff > 0.0 ) {
							
      if ( Smax - Coeff < B[Cnt] - EPS_COEFF_REDUCTION ) {
        /* On peut diminuer le coeff de la variable entiere */
				NouveauCoeff = Smax - B[Cnt];
			
        /* Modifs du 23/8/16 : on met a jour dynamiquement la marge */
				if ( Mode == MODE_PRESOLVE ) Marge = MARGE_SI_MODE_PRESOLVE;
        else Marge = MARGE_SI_MODE_PNE;

        # if APPLIQUER_UNE_MARGE_QUE_SI_VALEUR_FRACTIONNAIRE == OUI_PNE
			    if ( PNE_LaValeurEstEntiere( &NouveauCoeff ) == OUI_PNE ) {
						/*printf("Pas de marge\n");*/
					  Marge = 0;
					}
				# endif							

        # if BRUITER_LA_MARGE == OUI_PNE
          Pne->A1 = PNE_Rand( Pne->A1 );
					Marge -= Pne->A1 * ( COEFF_MARGE_A_BRUITER * Marge );
				# endif				

        /* Modifs du 23/8/16 : on ne tient compte de la marge que si on ne tombe pas sur un coeff nul */
        if ( NouveauCoeff != 0 ) NouveauCoeff += Marge;
        else Marge = 0;
				
				if ( fabs( NouveauCoeff ) < ZERO ) {
				  NouveauCoeff = 0;
					Marge = 0;
				}
				
				C = Coeff - NouveauCoeff;																

				if ( NouveauCoeff != 0 /*&& 0*/ ) {	 /* Inhibe pour l'instant car pour la mise a jour de B on est revenus sur B = Smax - Coeff */			
          if ( fabs( NouveauCoeff ) < PlusPetitTerme ) {					
            # if IGNORER_LES_COEFFS_NON_NULS_TROP_PETITS == OUI_PNE					
					    goto ContrainteSuivante;
						# endif						

           /* Modifs du 23/8/16 : mise a jour de la marge puisqu'on modifie le coeff par rapport a ce qu'il devrait etre */
            Marge = PlusPetitTerme - NouveauCoeff;

					  NouveauCoeff = PlusPetitTerme;

					  C = Coeff - NouveauCoeff;																	
					}
				}				

				/* C'est con c'est fait juste apres if ( fabs( NouveauCoeff ) > fabs( Coeff ) ) goto ContrainteSuivante; */		
				
        EcartAbsolu	= fabs( Coeff - NouveauCoeff );				
				if ( EcartAbsolu < MODIFICATION_MINIMALE ) {
				  /*printf("Echec EcartAbsolu %e\n",EcartAbsolu);*/
				  goto ContrainteSuivante;
        }
				EcartRelatif = fabs( EcartAbsolu / Coeff );
				if ( EcartRelatif < ECART_RELATIF_MIN ) {
				  /*printf("Echec EcartRelatif %e\n",EcartRelatif);*/  
				  goto ContrainteSuivante;
				}
				
				# if TRACES == 1					
			    printf("2- Variable entiere %d contrainte %d, remplacement de son coefficient %e par %e et B %e par %e\n",Var,Cnt,Coeff,NouveauCoeff,
				             B[Cnt],Smax-Coeff+Marge);					
			  # endif
				
				/* Modif du coefficient */					
				A[icEntier] = NouveauCoeff;
				CoeffModifie = OUI_PNE;			
				B[Cnt] = Smax - Coeff + Marge; /* Il vaut mieux faire ce calcul car cela fait moins d'operations (a part le cas de marge) qu'en faisant B = B - C.
				                                  En effet si B = B - C font au total 2 operations ( C provient deja d'une operation ) or B = Smax - Coeff
																					fait 1 operation.
																					Aussi surprenant que cela puisse paraitre, en prenant le calcul ci-dessus on est tombes sur des cas
																					d'infaisabilite alors que le probleme etait faisable */
				/*B[Cnt] -= C;*/

				/* En mode presolve on doit invalider la connaissance des variables duales puisque ce n'est plus la meme matrice */
        if ( Mode != MODE_PNE ) {
				  Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_NON_INITIALISE;		 
          Presolve->LambdaMin[Cnt] = -LINFINI_PNE;
	        Presolve->LambdaMax[Cnt] = LINFINI_PNE;
				  if ( SensContrainte[Cnt] == '<' ) {	
            Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;
	          Presolve->LambdaMax[Cnt] = 0;
	        }								
				}
				
				/* Mise a jour des limites des contraintes */
        PNE_ReCalculMaxApresAmeliorationCoeff( Mdeb, NbTerm, Nuvar, A, X, Xmin, Xmax, Bmax, Cnt,
																					     Mode, TypeDeBornePourPresolve,
																						   ConserverLaBorneInfDuPresolve, ConserverLaBorneSupDuPresolve,
																						   ContrainteBornanteInferieurement, ContrainteBornanteSuperieurement );																							 
				NbC++;
		  }
		}		
    ContrainteSuivante:
    ic = Csui[ic];
  }
}
if ( CoeffModifie == OUI_PNE ) {
	# if TRACES == 1					
    printf("AmeliorerLesCoefficientsDesVariablesBinaires : NbIter %d\n",NbIter);
	# endif
  NbIter++;
  if ( NbIter < NB_ITER_MX_REDUCTION ) goto Debut;	
}

if ( Pne->AffichageDesTraces == OUI_PNE ) {
  if ( NbC != 0 ) printf("%d binary coefficient(s) reduced\n",NbC);
}

return;
}   

