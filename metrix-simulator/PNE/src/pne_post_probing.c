/***********************************************************************

   FONCTION: En fin de probing, si des variables binaires ont ete fixees
	           on regarde si des contraintes sont de venue des contraintes
						 de type "forcing constraints".
						 Plus tard on regardera si des contraintes ne sont pas devenues
						 redondantes afin de les eliminer.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# include "prs_define.h"
# include "prs_fonctions.h"

# define TRACES 0

# define EPSILON_FORCING_CONSTRAINT 1.e-8 /*1.e-7*/
# define FORCING_BMIN 1
# define FORCING_BMAX 2
# define PAS_DE_FORCING 128

/*# define MARGE_DINFAISABILITE 1.e-7*/

# define TEST_DES_CONTRAINTES_A_UNE_VARIABLE  OUI_PNE /*OUI_PNE*/

# define EPS_ENTIER  MARGE_INITIALE_VARIABLES_ENTIERES   /*1.e-3*/
# define EPS_CONTINU 0  /*1.e-3*/ /* En mettant 0 peut se permettre de supprimer toutes les lignes singleton contenant une variable continue */

# define ZERFIX 1.e-6

# define INFAISABILITE 1.e-3

/*****************************/ # define MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES 1.e-4 /*1.e-2*/

# define APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES OUI_PNE /*OUI_PNE*/
# define SUPPRIMER_LES_BORNES_VARIABLES_EXPLOITEES NON_PNE /*NON_PNE*/

# if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == NON_PNE
  # undef SUPPRIMER_LES_BORNES_VARIABLES_EXPLOITEES 
  # define SUPPRIMER_LES_BORNES_VARIABLES_EXPLOITEES OUI_PNE
  # undef MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES  
  # define MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES 0.0
# endif

# define REBOUCLAGE OUI_PNE /*OUI_PNE*/
# define NOMBRE_MAX_DITERATIONS 10 /* 5 */

void PNE_PostProbingCliques( PROBLEME_PNE * , int , double );
void PNE_PostProbingContraintesDeBornesVariables( PROBLEME_PNE * , char * );
void PNE_PostProbingInvaliderToutesLesBornesVariableDuneVariableFixee( PROBLEME_PNE * , int );
void PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( PROBLEME_PNE * , int , double );
void PNE_PostProbingContraintesDeBornesVariablesSansMarges( PROBLEME_PNE * , double * , double * , int * , char * );
void PNE_PostProbingContraintesDeBornesVariablesAvecMarges( PROBLEME_PNE * , char * );

/*----------------------------------------------------------------------------*/

void PNE_PostProbing( PROBLEME_PNE * Pne )
{
int Cnt; int NombreDeContraintes; int il; int ilMax; int Var; char SminValide;
char SmaxValide; int * TypeDeVariable; int Nb; double Smin; double Smax; double a;
double b; double NouvelleValeur; int * TypeDeBorne; int * Mdeb; int * NbTerm;
int * Nuvar; double * X; double * Xmin; double * Xmax; double * B; double * A;
char * SensContrainte; char SensCnt; char TypeDeForcing; char TypeBorne;
int DerniereContrainte; int * CorrespondanceCntPneCntEntree; double Ai; int NbCntElim;
int NbVarFixees; int TypeBrn; double * CoutLineaire; char Signe; int NombreDeVariables; 
char SuppressionPossible; char DernierSigneTrouve; char SupprimerLaVariable;
int * Cdeb; int * Csui; int * NumContrainte; char SupprimerLaContrainte;
int Var1; double S; double Xi; double Xs; int NbIt; char Reboucler; double Eps;

# if FAIRE_DU_POST_PROBING == NON_PNE 
  return;
# endif

if ( Pne->FaireDuPresolve == NON_PNE ) return;

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

NombreDeContraintes = Pne->NombreDeContraintesTrav;
X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;
CorrespondanceCntPneCntEntree = Pne->CorrespondanceCntPneCntEntree;

NbCntElim = 0;
NbVarFixees = 0;
NbIt = 0;

Debut:

NbIt++;
Reboucler = NON_PNE;

NombreDeContraintes = Pne->NombreDeContraintesTrav;

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {

  SminValide = OUI_PNE;
	SmaxValide = OUI_PNE;
  Smin = 0.;
  Smax = 0.;
	SensCnt = SensContrainte[Cnt];
	b = B[Cnt];
	TypeDeForcing = PAS_DE_FORCING;
	Nb = 0;
	SupprimerLaContrainte = NON_PNE;

  il    = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
	  Var = Nuvar[il];
		TypeBorne = TypeDeBorne[Var];
		if ( TypeBorne == VARIABLE_NON_BORNEE ) {
      SminValide = NON_PNE;
	    SmaxValide = NON_PNE;
		  goto FinAnalyseContrainte;		
		}
		a = A[il];
		
	  if ( TypeBorne == VARIABLE_FIXE ) {
      Smin += a * X[Var];
      Smax += a * X[Var];			
		}
		else {		
		  Nb++;
			if ( a > 0.0 ) {
		    /* Calcul de min */
				if ( SminValide == OUI_PNE ) {
				  if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {				
            Smin += a * Xmin[Var];
					}
		      else {
            SminValide = NON_PNE;
			      if ( SmaxValide == NON_PNE ) break;						
					}
		    }
		    /* Calcul de max */
				if ( SmaxValide == OUI_PNE ) {
				  if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {				
            Smax += a * Xmax[Var];
					}
		      else {
            SmaxValide = NON_PNE;
			      if ( SminValide == NON_PNE ) break;
					}
		    }
	    }
      else {
		    /* Calcul de min */
				if ( SminValide == OUI_PNE ) {
				  if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {				
            Smin += a * Xmax[Var];
					}
		      else {
            SminValide = NON_PNE;
			      if ( SmaxValide == NON_PNE ) break;					 
					}
				}
		    /* Calcul de max */
				if ( SmaxValide == OUI_PNE ) {
				  if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {				
            Smax += a * Xmin[Var];
					}
		      else {
            SmaxValide = NON_PNE;
			      if ( SminValide == NON_PNE ) break;
					}
				}
			}					
		}		
    il++;
  }
	if ( Nb == 0 ) {
		SupprimerLaContrainte = OUI_PNE;
	  goto FinAnalyseContrainte;
	}		
	if ( SensCnt == '<' ) {
		if ( SmaxValide == OUI_PNE ) {
		  if ( Smax <= b + MARGE_DINFAISABILITE ) {
			  # if TRACES == 1
			    printf("Contrainte %d redondante Smx = %e < %e   Smin %e SminValide %d\n",Cnt,Smax,b,Smin,SminValide);
        # endif
	      SupprimerLaContrainte = OUI_PNE;								
			}
		}
	}	
  if ( SminValide == OUI_PNE ) {	
	  if ( fabs( Smin - b ) < EPSILON_FORCING_CONSTRAINT ) {
		  # if TRACES == 1
	 	    if ( SensCnt == '<' ) printf("Forcing constraint pendant le variable probing sur contrainte d'inegalite %d\n",Cnt);
		    else printf("Forcing constraint pendant le variable probing sur contrainte d'egalite %d\n",Cnt);
        printf("Bmin %e B %e sens %c\n",Smin,b,SensCnt);
      # endif			
		  TypeDeForcing = FORCING_BMIN;
	    SupprimerLaContrainte = OUI_PNE;			
		}
	}
  else if ( SmaxValide == OUI_PNE ) {
    if ( SensCnt == '=' ) {		
	    if ( fabs( Smax - b ) < EPSILON_FORCING_CONSTRAINT ) {
		    # if TRACES == 1
			    printf("Forcing constraint pendant le variable probing sur contrainte d'egalite %d\n",Cnt);          
          printf("Bmax %e B %e sens %c\n",Smax,b,SensCnt);
			  # endif
			  TypeDeForcing = FORCING_BMAX;
	      SupprimerLaContrainte = OUI_PNE;			
      }
	  }				
  }
  if ( TypeDeForcing != FORCING_BMIN && TypeDeForcing != FORCING_BMAX ) goto FinAnalyseContrainte;

	# if TRACES == 1
	  if ( TypeDeForcing == FORCING_BMIN ) printf("Contraintes %d FORCING_BMIN\n",Cnt);
	  if ( TypeDeForcing == FORCING_BMAX ) printf("Contraintes %d FORCING_BMAX\n",Cnt); 
	# endif

	Reboucler = OUI_PNE;
	
  il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
    Var = Nuvar[il];
		TypeBorne = TypeDeBorne[Var];
		if ( TypeBorne == VARIABLE_FIXE ) goto NextIlDoForcing; 			 
    a = A[il];
	  if ( a == 0.0 ) goto NextIlDoForcing;
	  if ( TypeDeForcing == FORCING_BMIN ) {
      if ( a > 0.0 ) {
		    # if TRACES == 1
		      printf("Forcing constraint contrainte %d variables %d a fixer au min %e\n",Cnt,Var,Xmin[Var]);		
			  # endif
			  NouvelleValeur = Xmin[Var];
		  }
		  else {
		    # if TRACES == 1
		      printf("Forcing constraint contrainte %d variables %d a fixer au max %e\n",Cnt,Var,Xmax[Var]);
		    # endif
        NouvelleValeur = Xmax[Var];
		  }
	  }
	  else { /* TypeForcing = FORCING_BMAX */
      if ( a > 0.0 ) {
		    # if TRACES == 1
		      printf("Forcing constraint contrainte %d variables %d a fixer au max %e\n",Cnt,Var,Xmax[Var]);
			  # endif
        NouvelleValeur = Xmax[Var];
		  }
		  else {
		    # if TRACES == 1
		      printf("Forcing constraint contrainte %d variables %d a fixer au min %e\n",Cnt,Var,Xmin[Var]);	
		    # endif
		    NouvelleValeur = Xmin[Var];			
		  }
	  }
	  /* Si la variable a ete fixee on met a jour la liste des contraintes a examiner au prochain coup */

/*
if ( 	TypeDeVariable[Var] == ENTIER ) {
  printf("0- Suppression de la variable entiere %d\n",Var);
  PNE_PostProbingCliques( Pne, Var, NouvelleValeur );
}
*/
		
  	X[Var] = NouvelleValeur;      
	  Xmin[Var] = NouvelleValeur;
	  Xmax[Var] = NouvelleValeur;
	  TypeDeBorne[Var] = VARIABLE_FIXE;		
	  TypeDeVariable[Var] = REEL;
		NbVarFixees++;
	  NextIlDoForcing:
    il++;
  }

	FinAnalyseContrainte:
			
	if ( SupprimerLaContrainte == OUI_PNE ) {
	
		/* On inverse avec la derniere contrainte */
		
    Pne->NumeroDesContraintesInactives[Pne->NombreDeContraintesInactives] = CorrespondanceCntPneCntEntree[Cnt];
    Pne->NombreDeContraintesInactives++;

 	  DerniereContrainte = NombreDeContraintes - 1;				
 	  Mdeb[Cnt]   = Mdeb[DerniereContrainte];
    NbTerm[Cnt] = NbTerm[DerniereContrainte];				
    B[Cnt]      = B[DerniereContrainte];
    SensContrainte[Cnt] = SensContrainte[DerniereContrainte];
    CorrespondanceCntPneCntEntree[Cnt] = CorrespondanceCntPneCntEntree[DerniereContrainte];
    Pne->ChainageTransposeeExploitable = NON_PNE;
	  Cnt--;
	  NombreDeContraintes--;
	  Pne->NombreDeContraintesTrav = NombreDeContraintes;
	  NbCntElim++;
		Reboucler = OUI_PNE;
	}
}

# if TEST_DES_CONTRAINTES_A_UNE_VARIABLE == OUI_PNE

/* Contraintes a une variable */

NombreDeContraintes = Pne->NombreDeContraintesTrav;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  S = 0.;
	Nb = 0;
	SupprimerLaContrainte = NON_PNE;
  il    = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
	Ai = 1.;
	Var1 = -1;
  while ( il < ilMax ) {
	  Var = Nuvar[il];
		TypeBorne = TypeDeBorne[Var];
		a = A[il];
	  if ( TypeBorne == VARIABLE_FIXE ) { S += a * X[Var]; goto NextElement; }
		else if ( Xmin[Var] == Xmax[Var] ) { S += a * Xmin[Var]; goto NextElement; }
    else {
		  /* Modif 3/11/2016: test par rapport a 0 pour eviter les divisions par 0 et le cas echeant pouvoir supprimer une contrainte sasns variable */
		  if ( a != 0 ) { 
  	    Nb++;
		    if ( Nb > 1 ) goto Synthese;
		    Var1 = Var;
		    Ai = A[il];
			}
		}
		NextElement:
    il++;
  }
	Synthese:
	if ( Var1 < 0 ) continue;
	if ( Nb == 0 ) {
	  # if TRACES == 1 
		  printf("Contrainte %d sans variable \n",Cnt);
		# endif		
		SupprimerLaContrainte = OUI_PNE;
	}
  if ( Nb == 1 ) {
	
	  Var = Var1;
		TypeBorne = TypeDeBorne[Var];

    if ( TypeDeVariable[Var] == ENTIER ) Eps = EPS_ENTIER;
		else {
		  Eps = EPS_CONTINU;
      if ( Eps == 0 ) SupprimerLaContrainte = OUI_PNE; /* Dans tous le cas on peut supprimer la contrainte */
		}
		
    S = B[Cnt] - S;
    if ( SensContrainte[Cnt] == '=' ) {
			NouvelleValeur = S / Ai;
			  			
      if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES ) { 
        if ( NouvelleValeur < Xmin[Var] - SEUIL_DADMISSIBILITE ) {
			    Pne->YaUneSolution = PROBLEME_INFAISABLE; return;
        }
        if ( NouvelleValeur > Xmax[Var] + SEUIL_DADMISSIBILITE ) {	
          Pne->YaUneSolution = PROBLEME_INFAISABLE; return;
        }				
      }
      else if ( TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) { 
        if ( NouvelleValeur < Xmin[Var] - SEUIL_DADMISSIBILITE ) {			
			    Pne->YaUneSolution = PROBLEME_INFAISABLE; return;
        }
      }			
      else if ( TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) { 
        if ( NouvelleValeur > Xmax[Var] + SEUIL_DADMISSIBILITE ) {					
          Pne->YaUneSolution = PROBLEME_INFAISABLE; return;
        }
      }
			
		  if ( TypeDeVariable[Var] == ENTIER ) {
		    if ( fabs( NouvelleValeur ) > MARGE_DINFAISABILITE && fabs( 1 - NouvelleValeur ) > MARGE_DINFAISABILITE ) {						
			    Pne->YaUneSolution = PROBLEME_INFAISABLE; return;
			  }
      }
			/* On fixe la variable */
				
			# if TRACES == 1
			  if ( TypeDeVariable[Var] == ENTIER ) {
		      printf("Post probing contrainte d'egalite a une seule variable: on fixe la variable entiere %d a %e (Xmin = %e Xmax = %e)\n",Var,NouvelleValeur,Xmin[Var],Xmax[Var]);
				}
				else {
		      printf("Post probing contrainte d'egalite a une seule variable: on fixe la variable %d a %e (Xmin = %e Xmax = %e)\n",Var,NouvelleValeur,Xmin[Var],Xmax[Var]);
				}
			# endif

/*
if ( 	TypeDeVariable[Var] == ENTIER ) {
  printf("1- Suppression de la variable entiere %d\n",Var);
  PNE_PostProbingCliques( Pne, Var, NouvelleValeur );
}
*/			
					  						
  	  X[Var] = NouvelleValeur;      
	    Xmin[Var] = NouvelleValeur;
	    Xmax[Var] = NouvelleValeur;
	    TypeDeBorne[Var] = VARIABLE_FIXE;			
	    TypeDeVariable[Var] = REEL;
		  NbVarFixees++;
		  SupprimerLaContrainte = OUI_PNE;			
		  Reboucler = OUI_PNE;			
    }		
    else {
		  /* La contrainte est de sens < */
			/* Attention, on peut fixer la variable si elle est entiere */
	    if ( Ai > 0 ) {
			  Xs = S / Ai;
				/* Eventuellement on change la borne sup */
        if ( Xs < Xmax[Var] - Eps ) {
				
				  # if TRACES == 1
					  printf("Post probing contrainte d'inegalite a une seule variable: Xmax[%d] = %e devient %e (Xmin = %e)\n",Var,Xmax[Var],Xs,Xmin[Var]);
					# endif
					
          Xmax[Var] = Xs;
		      SupprimerLaContrainte = OUI_PNE;			
		      Reboucler = OUI_PNE;					
          if ( TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) TypeBorne = VARIABLE_BORNEE_DES_DEUX_COTES;
					else if ( TypeBorne == VARIABLE_NON_BORNEE ) TypeBorne = VARIABLE_BORNEE_SUPERIEUREMENT;

          /* Modif 2/9/2016 */
					TypeDeBorne[Var] = TypeBorne; 
				 
					if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES ) {
            if ( TypeDeVariable[Var] == ENTIER ) {
/*						
if ( 	TypeDeVariable[Var] == ENTIER ) {
  printf("1- Suppression de la variable entiere %d\n",Var);
  PNE_PostProbingCliques( Pne, Var, Xmin[Var] );
}
*/						
              /* On fixe la variable a Xmin */							
 	            X[Var] = Xmin[Var];      
	            Xmax[Var] = Xmin[Var];
	            TypeDeBorne[Var] = VARIABLE_FIXE;							
	            TypeDeVariable[Var] = REEL;
		          NbVarFixees++;												
						}						
					  else if ( fabs( Xmax[Var] - Xmin[Var] ) < ZERO_VARFIXE ) {						
						  /* On fixe la variable */
 	            X[Var] = Xs;      
	            Xmin[Var] = Xs;							
	            TypeDeBorne[Var] = VARIABLE_FIXE;							
	            TypeDeVariable[Var] = REEL;
		          NbVarFixees++;													
						}
					}
					/* Bug car TypeDeBorne peut etre modifie juste au dessus pour fixer la variable => instruction deplacee au dessus */
					/* TypeDeBorne[Var] = TypeBorne;*/
				}		 			
			}
	    else {			
			  Xi = -S / fabs( Ai ); 								 
				/* Eventuellement on change la borne inf */
        if ( Xi > Xmin[Var] + Eps ) {
				
				  # if TRACES == 1
					  printf("Post probing contrainte d'inegalite a une seule variable: Xmin[%d] = %e devient %e (Xmax = %e)\n",Var,Xmin[Var],Xi,Xmax[Var]);
					# endif
					
          Xmin[Var] = Xi;				
		      SupprimerLaContrainte = OUI_PNE;			
		      Reboucler = OUI_PNE;
          if ( TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeBorne = VARIABLE_BORNEE_DES_DEUX_COTES;
          else if ( TypeBorne == VARIABLE_NON_BORNEE ) TypeBorne = VARIABLE_BORNEE_INFERIEUREMENT;

          /* Modif 2/9/2016 */
					TypeDeBorne[Var] = TypeBorne; 
					
					if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES ) {
           if ( TypeDeVariable[Var] == ENTIER ) {
/*					 
if ( 	TypeDeVariable[Var] == ENTIER ) {
  printf("1- Suppression de la variable entiere %d\n",Var);
  PNE_PostProbingCliques( Pne, Var, Xmax[Var] );
}
*/
              /* On fixe la variable a Xmax */							
 	            X[Var] = Xmax[Var];      
	            Xmin[Var] = Xmax[Var];
	            TypeDeBorne[Var] = VARIABLE_FIXE;							
	            TypeDeVariable[Var] = REEL;
		          NbVarFixees++;						 
					  }				
					  else if ( fabs( Xmax[Var] - Xmin[Var] ) < ZERO_VARFIXE ) {
						  /* On fixe la variable */
 	            X[Var] = Xi;      
	            Xmax[Var] = Xi;
	            TypeDeBorne[Var] = VARIABLE_FIXE;						
	            TypeDeVariable[Var] = REEL;
		          NbVarFixees++;													
						}
					}					
					/* Bug car TypeDeBorne peut etre modifie juste au dessus pour fixer la variable => instruction deplacee au dessus */
					/* TypeDeBorne[Var] = TypeBorne; */
					
        }		
	    }
		}
	}
	
	if ( SupprimerLaContrainte == OUI_PNE ) {
	
		/* On inverse avec la derniere contrainte */
		
    Pne->NumeroDesContraintesInactives[Pne->NombreDeContraintesInactives] = CorrespondanceCntPneCntEntree[Cnt];
    Pne->NombreDeContraintesInactives++;

 	  DerniereContrainte = NombreDeContraintes - 1;				
 	  Mdeb[Cnt]   = Mdeb[DerniereContrainte];
    NbTerm[Cnt] = NbTerm[DerniereContrainte];				
    B[Cnt]      = B[DerniereContrainte];
    SensContrainte[Cnt] = SensContrainte[DerniereContrainte];
    CorrespondanceCntPneCntEntree[Cnt] = CorrespondanceCntPneCntEntree[DerniereContrainte];
    Pne->ChainageTransposeeExploitable = NON_PNE;
	  Cnt--;
	  NombreDeContraintes--;
	  Pne->NombreDeContraintesTrav = NombreDeContraintes;
	  NbCntElim++;
		Reboucler = OUI_PNE;
	}	
}
# endif

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );
 
CoutLineaire = Pne->LTrav;
NombreDeVariables = Pne->NombreDeVariablesTrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {

  TypeBrn = TypeDeBorne[Var];
  if ( TypeBrn == VARIABLE_FIXE ) continue;

  if ( CoutLineaire[Var] > 0.0 )      Signe = '+';
  else if ( CoutLineaire[Var] < 0.0 ) Signe = '-';
  else Signe = '|';

  SuppressionPossible = OUI_PNE;
  DernierSigneTrouve  = '|';
	
  il = Cdeb[Var];
  while ( il >= 0 ) {
    Ai  = A[il];
    if ( Ai == 0.0 ) goto ContrainteSuivante;

		/* Si le coeff est trop petit on prefere ne rien conclure */
		if ( fabs( Ai ) < 1.e-6 && 0 ) {
		  /* On prefere ne rien conclure */
      SuppressionPossible = NON_PNE;
      break; 			
		}
		
    Cnt = NumContrainte[il];		
    if ( SensContrainte[Cnt] == '=' ) { 
      SuppressionPossible = NON_PNE;
      break; 
    }
    if ( Signe == '+' ) {
      if ( Ai < 0.0 ) {
        SuppressionPossible = NON_PNE;
        break;
      } 
    }
    else if ( Signe == '-' ) {
      if ( Ai > 0.0 ) {
        SuppressionPossible = NON_PNE;
        break;
      }
    }
    else { /* Alors Signe = '|' ) */        
      if ( Ai > 0.0 ) {	
        if ( DernierSigneTrouve == '-' ) {
          SuppressionPossible = NON_PNE;
          break;
	      }
	      else DernierSigneTrouve = '+';	
      }
      else if ( Ai < 0.0 ) {
        if ( DernierSigneTrouve == '+' ) {
          SuppressionPossible = NON_PNE;
          break;
	      }	
	      else DernierSigneTrouve = '-';	
      }
    }  
    ContrainteSuivante:
    il = Csui[il];
  }
  SupprimerLaVariable = NON_PNE;	
  if ( SuppressionPossible == OUI_PNE ) {			
    if ( Signe == '+' ) {            
      if ( TypeBrn == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBrn == VARIABLE_BORNEE_INFERIEUREMENT ) {			
			  # if TRACES == 1
          printf("Variable %d cout %e fixee a sa borne inf: %e \n",Var,CoutLineaire[Var],Xmin[Var]);
			  # endif
        SupprimerLaVariable = OUI_PNE;							
			  NouvelleValeur = Xmin[Var];				     
			}			
    }
    else if ( Signe == '-' ) {        
      if ( TypeBrn == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBrn == VARIABLE_BORNEE_SUPERIEUREMENT ) {						
			  # if TRACES == 1
          printf("Variable %d cout %e fixee a sa borne sup: %e \n",Var,CoutLineaire[Var],Xmax[Var]);
			  # endif   			
        SupprimerLaVariable = OUI_PNE;						
			  NouvelleValeur = Xmax[Var];				     
			}
    }
    else { /* Alors Signe = '|' ) */		
      if ( DernierSigneTrouve == '+' ) {
        /* La variable n'intervient que dans des contraintes d'inegalite, avec un coefficient positif et 
           de plus elle n'a pas de cout => on la fixe a Xmin */       
				if ( TypeBrn == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBrn == VARIABLE_BORNEE_INFERIEUREMENT ) {        
				  # if TRACES == 1
            printf("Variable %d cout %e fixee a sa borne inf: %e \n",Var,CoutLineaire[Var],Xmin[Var]);
          # endif				
          SupprimerLaVariable = OUI_PNE;
			    NouvelleValeur = Xmin[Var];				     					
				}				
      }
      else if ( DernierSigneTrouve == '-' ) {			
				if ( TypeBrn == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBrn == VARIABLE_BORNEE_SUPERIEUREMENT ) {
				  # if TRACES == 1
            printf("Variable %d cout %e fixee a sa borne sup: %e \n",Var,CoutLineaire[Var],Xmax[Var]);
          # endif 				
				  SupprimerLaVariable = OUI_PNE;
			    NouvelleValeur = Xmax[Var];				     					
				}				
      }
			else {						
        /* DernierSigneTrouve = '|' : la variable n'intervient pas dans les contraintes et son cout est nul */
				# if TRACES == 1
          printf("Variable %d a cout nul et n'intervenant dans aucune contrainte\n",Var);
        # endif				
			}
    }
    if ( SupprimerLaVariable == OUI_PNE ) {		
			# if TRACES == 1
			  printf("Suppression de la variable %d\n",Var);
      # endif

/*
if ( 	TypeDeVariable[Var] == ENTIER ) {
  printf("2- Suppression de la variable entiere %d\n",Var);
  PNE_PostProbingCliques( Pne, Var, NouvelleValeur );
}
*/
			
  	  X[Var] = NouvelleValeur;      
	    Xmin[Var] = NouvelleValeur;
	    Xmax[Var] = NouvelleValeur;			
	    TypeDeBorne[Var] = VARIABLE_FIXE;			
	    TypeDeVariable[Var] = REEL;
		  NbVarFixees++;
			Reboucler = OUI_PNE;
    }    
  }
}

if ( Reboucler == OUI_PNE ) {
	if ( NbIt < NOMBRE_MAX_DITERATIONS && REBOUCLAGE == OUI_PNE ) {	
	  goto Debut;
	}
	else printf("Post probing: max iterations count reached, limit is %d\n",NOMBRE_MAX_DITERATIONS);
}

PNE_PostProbingContraintesDeBornesVariables( Pne, &Reboucler );

/*********** NOUVEAU *****************/
PNE_ContraintesColineaires( Pne, &NbCntElim, &Reboucler );

if ( Pne->AffichageDesTraces == OUI_PNE ) {
  if ( NbCntElim != 0 || NbVarFixees != 0 ) {
    printf("Post probing: %d constraint(s) removed - %d variable(s) fixed\n",NbCntElim,NbVarFixees);
	}
}

# if PRISE_EN_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTS == OUI_PNE
  PNE_ColonnesColineaires( Pne );
# endif

# if RELATION_DORDRE_DANS_LE_PROBING == OUI_PNE
  # if METHODE_DE_PRISE_EN_COMPTE_DES_CONTRAINTES_DORDRE == CONTRAINTES_DORDRE_DANS_LES_COUPES  
    PNE_ConstruireLesContraintesDOrdre( Pne );
  # endif
# endif

# if TENIR_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTES_POUR_LE_BRANCHING == OUI_PNE 
  PNE_InitDonneesPourLaRechercheDesGroupesDeVariablesEquivalentes( Pne );	
	if ( Pne->ContrainteActivable != NULL ) {
    PNE_RechercherLesGroupesDeVariablesEquivalentes( Pne );
    free( Pne->ContrainteActivable );
    Pne->ContrainteActivable = NULL;
	}
# endif

return;
}

/*----------------------------------------------------------------------------*/

void PNE_PostProbingCliques( PROBLEME_PNE * Pne, int VarFixee, double Valeur )
{
int il; int * First; int * NbElements; int Noeud; int Complement;
int * NoeudDeClique; CLIQUES * Cliques; int c; int Pivot; int ilMax; int Var;

return;

if ( Pne->Cliques == NULL ) return;

Cliques = Pne->Cliques;
First = Cliques->First;  
NbElements = Cliques->NbElements;
NoeudDeClique = Cliques->Noeud;
Pivot = Pne->ConflictGraph->Pivot;
				
for ( c = 0 ; c < Cliques->NombreDeCliques ; c++ ) {				
  il = First[c];
  if ( il < 0 ) continue; /* On ne sait jamais ... */	
  ilMax = il + NbElements[c];	
  while ( il < ilMax ) {
	  if ( NoeudDeClique[il] < Pivot ) Var = NoeudDeClique[il];
		else Var = NoeudDeClique[il] - Pivot;				
    if ( Var == VarFixee ) {		
			if ( NoeudDeClique[il] < Pivot ) printf("La variable est dans une clique  Valeur %e  coeff 1\n",Valeur);
      else printf("La variable est dans une clique  Valeur %e  coeff -1\n",Valeur);
      il = First[c];
      ilMax = il + NbElements[c];	
      while ( il < ilMax ) {
			  if ( NoeudDeClique[il] < Pivot ) printf(" + %d ",NoeudDeClique[il]);
				else printf(" + 1 - %d ",NoeudDeClique[il] - Pivot);	
			  il++;
      }
			printf("\n");

      /* On modifie la clique */
      il = First[c];
      ilMax = il + NbElements[c];	
      while ( il < ilMax ) {
	      if ( NoeudDeClique[il] < Pivot ) Var = NoeudDeClique[il];
		    else Var = NoeudDeClique[il] - Pivot;				
        if ( Var == VarFixee ) {

				if ( Pne->UminTrav[Var] == Pne->UmaxTrav[Var] ) printf("Var %d fixee \n",Var);
				else if ( Pne->TypeDeBorneTrav[Var] == VARIABLE_FIXE ) printf("Var %d fixee \n",Var);
				
          NoeudDeClique[il] = NoeudDeClique[ilMax-1];
		   	  NbElements[c]--;
					break;
			  }
			  il++;
      }
						
			break;
		}			
		il++;
	}
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_PostProbingContraintesDeBornesVariables( PROBLEME_PNE * Pne, char * Reboucler )
{

double * Xmin; double * Xmax; int * TypeDeBorne; double * XminTmp; double * XmaxTmp;
int * TypeDeBorneTmp;

/*return;*/ /* D'apres les essais numeriques il n'est pas interessant de le faire a cause des imprecisions numerique */

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;

XminTmp = (double *) malloc( Pne->NombreDeVariablesTrav * sizeof( double ) );
XmaxTmp = (double *) malloc( Pne->NombreDeVariablesTrav * sizeof( double ) );
TypeDeBorneTmp = (int *) malloc( Pne->NombreDeVariablesTrav * sizeof( int ) );
if ( XminTmp == NULL || XmaxTmp == NULL || TypeDeBorneTmp == NULL ) {
  free( XminTmp ); free( XmaxTmp ); free( TypeDeBorneTmp );
  return;
}
memcpy( (char *) XminTmp, (char *) Xmin, Pne->NombreDeVariablesTrav * sizeof( double ) );
memcpy( (char *) XmaxTmp, (char *) Xmax, Pne->NombreDeVariablesTrav * sizeof( double ) );
memcpy( (char *) TypeDeBorneTmp, (char *) TypeDeBorne, Pne->NombreDeVariablesTrav * sizeof( int ) );

/* Premiere passe on regarde si les contraintes de bornes variables conduisent a fixer des variables */
PNE_PostProbingContraintesDeBornesVariablesSansMarges( Pne, XminTmp, XmaxTmp, TypeDeBorneTmp, Reboucler );
free( XminTmp ); free( XmaxTmp ); free( TypeDeBorneTmp );

/* Deuxieme passe, on ameliore les bornes sur les variables non fixees par les contraintes de bornes variables */
PNE_PostProbingContraintesDeBornesVariablesAvecMarges( Pne, Reboucler );

return;
}

/*----------------------------------------------------------------------------*/

void PNE_PostProbingInvaliderToutesLesBornesVariableDuneVariableFixee( PROBLEME_PNE * Pne,
                                                                       int VariableFixee )
{
CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable; int NombreDeContraintesDeBorne;
int * First; int * Colonne; int ilVarCont; int Varcont; int Cnt;
ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;	
NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
Colonne = ContraintesDeBorneVariable->Colonne;
/* La variable continue est toujours placee en premier */
for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;	
  ilVarCont = First[Cnt];
	Varcont = Colonne[ilVarCont];
	if ( Varcont == VariableFixee ) {
	  First[Cnt] = -1;
	}	
}	
return;
}

/*----------------------------------------------------------------------------*/

void PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( PROBLEME_PNE * Pne,
                                                             int VariableFixee,
																														 double NouvelleValeur )
{
Pne->UTrav[VariableFixee] = NouvelleValeur;      
Pne->UminTrav[VariableFixee] = NouvelleValeur;
Pne->UmaxTrav[VariableFixee] = NouvelleValeur;			
Pne->TypeDeBorneTrav[VariableFixee] = VARIABLE_FIXE;	
Pne->TypeDeVariableTrav[VariableFixee] = REEL;
return;
}

/*----------------------------------------------------------------------------*/
/* On ne se sert de cette premiere etape que pour fixer des variables */

void PNE_PostProbingContraintesDeBornesVariablesSansMarges( PROBLEME_PNE * Pne,
                                                            double * Xmin, double * Xmax,
																														int * TypeDeBorne,
																														char * Reboucler )
{
int Cnt; int * First; int * Colonne; double * Coefficient; double * SecondMembre;
int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable; int ilVarBin; 
int NombreDeContraintesDeBorne; int ilVarCont; int Varcont; double NouvelleValeur;
double S0; double S1; double B; double * XRef; double ValeurDeVarbin; int * TypeDeVariable;
double * XminRef; double * XmaxRef; int * TypeDeBorneRef; char NouveauTest; int NbIterations;
int MxIterations;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;	
NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;

XRef = Pne->UTrav;
XminRef = Pne->UminTrav;
XmaxRef = Pne->UmaxTrav;
TypeDeBorneRef = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;

NbIterations = 0;
MxIterations = 3;

Debut:
NouveauTest = NON_PNE;

/* La variable continue est toujours placee en premier */
for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;
	
  ilVarCont = First[Cnt];
	ilVarBin = ilVarCont + 1;
	
	B = SecondMembre[Cnt];

	Varcont = Colonne[ilVarCont];
	if ( TypeDeBorne[Varcont] == VARIABLE_FIXE ) continue;
	if ( TypeDeBorneRef[Varcont] == VARIABLE_FIXE ) continue;
	if ( XminRef[Varcont] == XmaxRef[Varcont] ) continue;
	
	Varbin = Colonne[ilVarBin];	

  if ( TypeDeBorne[Varbin] == VARIABLE_FIXE || Xmin[Varbin] == Xmax[Varbin] ) {
    if ( TypeDeBorne[Varbin] == VARIABLE_FIXE ) ValeurDeVarbin = XRef[Varbin];
		else ValeurDeVarbin = Xmin[Varbin];
		
    /* La variable binaire est fixee */
		NouvelleValeur = ( B - ( Coefficient[ilVarBin] * ValeurDeVarbin ) ) / Coefficient[ilVarCont];

    if ( Coefficient[ilVarCont] > 0 ) {
		  /* La contrainte est une contrainte de borne sup */			
		  if ( NouvelleValeur < Xmax[Varcont] ) {
	      Xmax[Varcont] = NouvelleValeur;				
	      if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_SUPERIEUREMENT;												      
	      if ( fabs( Xmin[Varcont] - NouvelleValeur ) < ZERFIX ) {
			    /*printf("Contrainte de borne variable: variable %d fixee a Xmin = %e (Xmax = %e)\n",Varcont,Xmin[Varcont],Xmax[Varcont]);*/
		  	  NouvelleValeur = 0.5 * ( Xmin[Varcont] + NouvelleValeur );			  	      
          PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varcont, NouvelleValeur );
          PNE_PostProbingInvaliderToutesLesBornesVariableDuneVariableFixee( Pne, Varcont );
					NouveauTest = OUI_PNE;
				}
      }
		}
		else {
		  /* La contrainte est une contrainte de borne inf */			
			if ( NouvelleValeur > Xmin[Varcont] ) {
	      Xmin[Varcont] = NouvelleValeur;				
	      if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_INFERIEUREMENT;								      
		    if ( fabs( NouvelleValeur - Xmax[Varcont] ) < ZERFIX ) {
				  /*printf("Contrainte de borne variable: variable %d fixee a Xmax = %e (Xmin = %e)\n",Varcont,Xmax[Varcont],Xmin[Varcont]);*/
				  NouvelleValeur = 0.5 * ( Xmax[Varcont] + NouvelleValeur );  	      				
          PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varcont, NouvelleValeur );
          PNE_PostProbingInvaliderToutesLesBornesVariableDuneVariableFixee( Pne, Varcont );				
					NouveauTest = OUI_PNE;
				}
			}						
		}
	}
	else {
    /* La variable binaire n'est pas fixee */
		/* Fixation a 0 */
	  S0 = ( B - ( Coefficient[ilVarBin] * Xmin[Varbin] ) ) / Coefficient[ilVarCont];
		/* Fixation a 1 */
	  S1 = ( B - ( Coefficient[ilVarBin] * Xmax[Varbin] ) ) / Coefficient[ilVarCont];
		
    if ( Coefficient[ilVarCont] > 0 ) {
		  /* La contrainte est une contrainte de borne sup */
			/* Fixation a 0 */
			if ( Xmin[Varcont] > S0 + INFAISABILITE ) {
				/*printf("Contrainte de borne variable: variable entiere %d valeur 0 interdite\n",Varbin);*/						
				NouvelleValeur = Xmax[Varbin];
        PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varbin, NouvelleValeur );				
				First[Cnt] = -1;
				NouveauTest = OUI_PNE;
				continue;				
			}						
			/* Fixation a 1 */
			if ( Xmin[Varcont] > S1 + INFAISABILITE ) {
				/*printf("Contrainte de borne variable: variable entiere %d valeur 1 interdite\n",Varbin);*/ 	    				
        NouvelleValeur = Xmin[Varbin];  
        PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varbin, NouvelleValeur );								
				First[Cnt] = -1;
				NouveauTest = OUI_PNE;
				continue;				
			}			
      if ( S0 < S1 ) NouvelleValeur = S1;
			else NouvelleValeur = S0;			
			if ( NouvelleValeur < Xmax[Varcont] ) {
	      Xmax[Varcont] = NouvelleValeur;				
	      if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_SUPERIEUREMENT;								
	      if ( fabs( Xmin[Varcont] - NouvelleValeur ) < ZERFIX ) {
			    /*printf("Contrainte de borne variable: variable %d fixee a Xmin = %e (Xmax = %e)\n",Varcont,Xmin[Varcont],Xmax[Varcont]);*/
		  	  NouvelleValeur = 0.5 * ( Xmin[Varcont] + NouvelleValeur );					  	     					
          PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varcont, NouvelleValeur );					
          PNE_PostProbingInvaliderToutesLesBornesVariableDuneVariableFixee( Pne, Varcont );				
				  NouveauTest = OUI_PNE;
        }				
			}
		}
		else {
		  /* La contrainte est une contrainte de borne inf */
			/* Fixation a 0 */
			if ( Xmax[Varcont] < S0 - INFAISABILITE ) {
				/*printf("Contrainte de borne variable: variable entiere %d valeur 0 interdite\n",Varbin);*/  	   				
				NouvelleValeur = Xmax[Varbin];
        PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varbin, NouvelleValeur );							
				First[Cnt] = -1;
				NouveauTest = OUI_PNE;
				continue;				
			}		
			/* Fixation a 1 */
			else if ( Xmax[Varcont] < S1 - INFAISABILITE ) {
				/*printf("Contrainte de borne variable: variable entiere %d valeur 1 interdite\n",Varbin);*/	   
				NouvelleValeur = Xmin[Varbin];
        PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varbin, NouvelleValeur );							
				First[Cnt] = -1;				
				NouveauTest = OUI_PNE;
				continue;								
			}			
      if ( S0 < S1 ) NouvelleValeur = S0;
			else NouvelleValeur = S1;
			if ( NouvelleValeur > Xmin[Varcont] ) {
	      Xmin[Varcont] = NouvelleValeur;				
	      if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_INFERIEUREMENT;
		    if ( fabs( NouvelleValeur - Xmax[Varcont] ) < ZERFIX ) {
				  /*printf("Contrainte de borne variable: variable %d fixee a Xmax = %e (Xmin = %e)\n",Varcont,Xmax[Varcont],Xmin[Varcont]);*/
				  NouvelleValeur = 0.5 * ( Xmax[Varcont] + NouvelleValeur );					 	      					
          PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varcont, NouvelleValeur );										
          PNE_PostProbingInvaliderToutesLesBornesVariableDuneVariableFixee( Pne, Varcont );				
				  NouveauTest = OUI_PNE;
			  }															
      }			
		}		
	}	
}

if ( NouveauTest == OUI_PNE ) {
  *Reboucler = OUI_PNE;
  NbIterations++;
	if ( NbIterations < MxIterations && 0 )  goto Debut;
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_PostProbingContraintesDeBornesVariablesAvecMarges( PROBLEME_PNE * Pne, char * Reboucler )
{
int Cnt; int * First; int * Colonne; double * Coefficient; double * SecondMembre;
int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable; int ilVarBin; 
int NombreDeContraintesDeBorne; int ilVarCont; int Varcont; double NouvelleValeur;
double S0; double S1; double B; double * X; double * Xmin; double * Xmax; int * TypeDeBorne;
double ValeurDeVarbin; int * TypeDeVariable; char * AppliquerUneMargeEventuelle;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;	
NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;
AppliquerUneMargeEventuelle = ContraintesDeBorneVariable->AppliquerUneMargeEventuelle;

X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;

/* La variable continue est toujours placee en premier */
for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {

  if ( First[Cnt] < 0 ) continue;
	
  ilVarCont = First[Cnt];
	ilVarBin = ilVarCont + 1;
	
	B = SecondMembre[Cnt];

	Varcont = Colonne[ilVarCont];
	if ( TypeDeBorne[Varcont] == VARIABLE_FIXE ) continue;
	
	Varbin = Colonne[ilVarBin];	

  if ( TypeDeBorne[Varbin] == VARIABLE_FIXE || Xmin[Varbin] == Xmax[Varbin] ) {
    if ( TypeDeBorne[Varbin] == VARIABLE_FIXE ) ValeurDeVarbin = X[Varbin];
		else ValeurDeVarbin = Xmin[Varbin];
    /* La variable binaire est fixee */
		NouvelleValeur = ( B - ( Coefficient[ilVarBin] * ValeurDeVarbin ) ) / Coefficient[ilVarCont];

    if ( Coefficient[ilVarCont] > 0 ) {
		  /* La contrainte est une contrainte de borne sup */

			# if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == OUI_PNE
			  if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			    if ( PNE_LaValeurEstEntiere( &NouvelleValeur ) == NON_PNE ) NouvelleValeur += MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES;
				}
			# endif
			
		  if ( NouvelleValeur < Xmax[Varcont] ) {
			  /*printf("1- Contrainte de borne variable %d: variable %d on abaisse la borne sup de %e a %e\n",Cnt,Varcont,Xmax[Varcont],NouvelleValeur);*/
	      Xmax[Varcont] = NouvelleValeur;				
	      if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_SUPERIEUREMENT;
				
				# if SUPPRIMER_LES_BORNES_VARIABLES_EXPLOITEES == OUI_PNE
				  First[Cnt] = -1;
				# endif

        *Reboucler = OUI_PNE;

        if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES ) {
	        if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < ZERFIX ) {
		  	    NouvelleValeur = 0.5 * ( Xmin[Varcont] + Xmax[Varcont] );			  	      
			      /*printf("Variable %d fixee a %e grace a la contrainte de borne variable %d\n",Varcont,NouvelleValeur,Cnt);*/
            PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varcont, NouvelleValeur );
				  }
				}
				
				continue;				
      }
		}
		else {
		  /* La contrainte est une contrainte de borne inf */
			
			# if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == OUI_PNE			
			  if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			    if ( PNE_LaValeurEstEntiere( &NouvelleValeur ) == NON_PNE ) NouvelleValeur -= MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES;
				}
			# endif
			
			if ( NouvelleValeur > Xmin[Varcont] ) {
				/*printf("1- Contrainte de borne variable %d: variable %d on remonte la borne inf de %e a %e\n",Cnt,Varcont,Xmin[Varcont],NouvelleValeur);*/
	      Xmin[Varcont] = NouvelleValeur;				
	      if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_INFERIEUREMENT;

				# if SUPPRIMER_LES_BORNES_VARIABLES_EXPLOITEES == OUI_PNE
				  First[Cnt] = -1;
				# endif
				
        *Reboucler = OUI_PNE;

        if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES ) {
	        if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < ZERFIX ) {
		  	    NouvelleValeur = 0.5 * ( Xmin[Varcont] + Xmax[Varcont] );			  	      
			      /*printf("Variable %d fixee a %e grace a la contrainte de borne variable %d\n",Varcont,NouvelleValeur,Cnt);*/
            PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varcont, NouvelleValeur );
				  }
				}				
				
				continue;				
      }		 
		}
	}
	else {
    /* La variable binaire n'est pas fixee */
		/* Fixation a 0 */
	  S0 = ( B - ( Coefficient[ilVarBin] * Xmin[Varbin] ) ) / Coefficient[ilVarCont];
		/* Fixation a 1 */
	  S1 = ( B - ( Coefficient[ilVarBin] * Xmax[Varbin] ) ) / Coefficient[ilVarCont];
		
    if ( Coefficient[ilVarCont] > 0 ) {
		  /* La contrainte est une contrainte de borne sup */
									
      if ( S0 < S1 ) NouvelleValeur = S1;
			else NouvelleValeur = S0;

			# if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == OUI_PNE
			  if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			    if ( PNE_LaValeurEstEntiere( &NouvelleValeur ) == NON_PNE ) NouvelleValeur += MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES;
				}
			# endif			
			
			if ( NouvelleValeur < Xmax[Varcont] ) {
				/*printf("2- Contrainte de borne variable: variable %d on peut abaisser la borne sup de %e a %e\n",Varcont,Xmax[Varcont],NouvelleValeur);*/
	      Xmax[Varcont] = NouvelleValeur;				
	      if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_SUPERIEUREMENT;

				# if SUPPRIMER_LES_BORNES_VARIABLES_EXPLOITEES == OUI_PNE				
				  First[Cnt] = -1;
        # endif
				
        *Reboucler = OUI_PNE;

        if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES ) {
	        if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < ZERFIX ) {
		  	    NouvelleValeur = 0.5 * ( Xmin[Varcont] + Xmax[Varcont] );			  	      
			      /*printf("Variable %d fixee a %e grace a la contrainte de borne variable %d\n",Varcont,NouvelleValeur,Cnt);*/
            PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varcont, NouvelleValeur );
				  }
				}							
				
				continue;				
			}
		}
		else {
		  /* La contrainte est une contrainte de borne inf */
			
      if ( S0 < S1 ) NouvelleValeur = S0;
			else NouvelleValeur = S1;		

			# if APPLIQUER_UNE_MARGE_SUR_LES_NOUVELLES_BORNES == OUI_PNE
			  if ( AppliquerUneMargeEventuelle[Cnt] == OUI_PNE ) {
			    if ( PNE_LaValeurEstEntiere( &NouvelleValeur ) == NON_PNE ) NouvelleValeur -= MARGE_A_APPLIQUER_SUR_LES_NOUVELLES_BORNES;
				}
			# endif						
			
			if ( NouvelleValeur > Xmin[Varcont] ) {
				/*printf("2- Contrainte de borne variable: variable %d on peut remonter la borne inf de %e a %e\n",Varcont,Xmin[Varcont],NouvelleValeur);*/
	      Xmin[Varcont] = NouvelleValeur;				
	      if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_INFERIEUREMENT;

				# if SUPPRIMER_LES_BORNES_VARIABLES_EXPLOITEES == OUI_PNE				
				  First[Cnt] = -1;
        # endif
				
		    *Reboucler = OUI_PNE;

        if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES ) {
	        if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < ZERFIX ) {
		  	    NouvelleValeur = 0.5 * ( Xmin[Varcont] + Xmax[Varcont] );			  	      
			      /*printf("Variable %d fixee a %e grace a la contrainte de borne variable %d\n",Varcont,NouvelleValeur,Cnt);*/
            PNE_PostProbingFixerUneVariableGraceAuxBornesVariables( Pne, Varcont, NouvelleValeur );
				  }
				}				
						
				continue;				 				
      }										
		}		
	}	
}

return;
}

/*----------------------------------------------------------------------------*/



