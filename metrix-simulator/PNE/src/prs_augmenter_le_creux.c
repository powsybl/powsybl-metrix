/***********************************************************************

   FONCTION: 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"

# include "pne_define.h"

# include "prs_define.h"

# define TRACES 0
# define TRACES_MODIFS_BORNES 0

# define ZERO_COLINEAIRE  1.e-10 /*1.e-10*/
# define SEUIL_FORCING_CONSTRAINT  1.e-7 /*1.e-7*/
# define FORCING_AU_MAX 1
# define FORCING_AU_MIN 2
  
# define CONTRAINTE_DE_BORNE_INF 1
# define CONTRAINTE_DE_BORNE_SUP 2
# define CONTRAINTE_EGAL         3

# define NOMBRE_MAX_DE_COMPARAISONS_PAR_CONTRAINTE 1000

void PRS_AugmenterLeCreuxComparerContraintes( PROBLEME_PNE * , PRESOLVE * , int , int , double * , double * , char * , int , int * );
void PRS_CalculMinMaxDeCnt1PourTEgal0 ( PROBLEME_PNE * , PRESOLVE * , int ,double * , char * ,double * , char * , double * , char * );
void PRS_AjusterBornesDesVariablesPourTEgal0 ( PROBLEME_PNE * , PRESOLVE * , int , char , double , char , double , char , double , char * , int * );
void PRS_AugmenterLeCreuxFaireLeForcing( PRESOLVE * , char , int , int * , int * , int * , double * , int * , double * , double * , char * );
																				 
/*----------------------------------------------------------------------------*/

void PRS_AugmenterLeCreuxDesContraintes( PRESOLVE * Presolve, int * NbContraintesModifees ) 
{
int il; int ic; int ilDebCnt; int ilMaxCnt; int ic1;  int NombreDeContraintes;
int NombreDeVariables; int Cnt; int Cnt1; double * V; char * T; int LallocTas;
double BCnt; double * B; int * Mdeb; int * NbTerm; int * Nuvar; double * A;
int * NumContrainte; int NbTermesUtilesDeCnt; char * pt; char * ContrainteInactive;
int * Cdeb; int * Csui; int * TypeDeBornePourPresolve; int Var; int NbT;
int * ParColonnePremiereVariable; int * ParColonneVariableSuivante; char * ContrainteSource;
char * Buffer; char * SensContrainte; double * ValeurDeXPourPresolve;
char InitV; int NbCntDeVar; int * NumCntDeVar; int VarCnt; double BFixeDeCnt;
double * BorneInfPourPresolve; double * BorneSupPourPresolve; PROBLEME_PNE * Pne;
int NombreDeComparaisons;

*NbContraintesModifees = 0;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;
   
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
SensContrainte = Pne->SensContrainteTrav;   
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;

ContrainteInactive = Presolve->ContrainteInactive;
ValeurDeXPourPresolve = Presolve->ValeurDeXPourPresolve;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;

ParColonnePremiereVariable = Presolve->ParColonnePremiereVariable;	
ParColonneVariableSuivante = Presolve->ParColonneVariableSuivante;

LallocTas = 0;
LallocTas += NombreDeVariables * sizeof( double ); /* V */
LallocTas += NombreDeVariables * sizeof( char ); /* T */
LallocTas += NombreDeContraintes * sizeof( int ); /* NumCntDeVar */
LallocTas += NombreDeContraintes * sizeof( char ); /* ContrainteSource */

Buffer = (char *) malloc( LallocTas );
if ( Buffer == NULL ) {
  printf(" Solveur PNE , memoire insuffisante dans le presolve. Sous-programme: PRS_ContraintesColineaires \n");
	return;
}

pt = Buffer;
V = (double *) pt;
pt += NombreDeVariables * sizeof( double );
T = (char *) pt;
pt += NombreDeVariables * sizeof( char );
NumCntDeVar = (int *) pt;
pt += NombreDeContraintes * sizeof( int );
ContrainteSource = (char *) pt;
pt += NombreDeContraintes * sizeof( char );

memset( (char *) T, 0, NombreDeVariables * sizeof( char ) );
memset( (char *) ContrainteSource, OUI_PNE, NombreDeContraintes * sizeof( char ) );

/* On balaye les colonnes dans l'ordre croissant du nombre de termes et on ne compare que les
   les lignes qui ont un terme dans cette colonne */
	 
for ( NbT = 2 ; NbT <= Presolve->NbMaxTermesDesColonnes ; NbT++ ) {
  Var = ParColonnePremiereVariable[NbT];
	while ( Var >= 0 ) {
	  /*********************************************/
	  if ( TypeDeBornePourPresolve[Var] == VARIABLE_FIXE ) goto NextVar;
	  if ( BorneInfPourPresolve[Var] == BorneSupPourPresolve[Var] ) goto NextVar;
	  /* On prepare la table des contraintes a explorer */
 	  NbCntDeVar = 0;
	  ic = Cdeb[Var];
    while ( ic >= 0 ) {
		  if ( A[ic] != 0 ) {
		    Cnt = NumContrainte[ic];
        if ( ContrainteInactive[Cnt] != OUI_PNE ) {
			    NumCntDeVar[NbCntDeVar] = Cnt;
				  NbCntDeVar++;
			  }
      }
	    ic = Csui[ic];
	  }

		BFixeDeCnt = 0.0; /* A cause des warning de compilation */
	  for ( ic = 0 ; ic < NbCntDeVar ; ic++ ) {
	    Cnt = NumCntDeVar[ic];
      if ( ContrainteInactive[Cnt] == OUI_PNE ) continue;
      if ( ContrainteSource[Cnt] == NON_PNE ) continue;			
      ilDebCnt = 0;
		  InitV = NON_PNE;
			ilMaxCnt = -1;
			BCnt = LINFINI_PNE;
			NbTermesUtilesDeCnt = 0;
      /* On compare a Cnt toutes les contraintes de la colonne (celles qui sont reperees sont activables) */
		  NombreDeComparaisons = 0;		
	    for ( ic1 = 0 ; ic1 < NbCntDeVar ; ic1++ ) {
	      Cnt1 = NumCntDeVar[ic1];
        if ( ContrainteInactive[Cnt1] == OUI_PNE ) continue;
				if ( Cnt1 == Cnt ) continue;				
			  /* Comparaison de Cnt a Cnt1 */
			  if ( InitV == NON_PNE ) {
		      /* Preparation des tables pour la contrainte Cnt */
					BFixeDeCnt = 0.0;
          ilDebCnt = Mdeb[Cnt];
	        ilMaxCnt = ilDebCnt + NbTerm[Cnt];
	        il = ilDebCnt;
	        while ( il < ilMaxCnt ) {
			      if ( A[il] != 0.0 ) {
					    VarCnt = Nuvar[il];
				      if ( TypeDeBornePourPresolve[VarCnt] == VARIABLE_FIXE ) BFixeDeCnt += A[il] * ValeurDeXPourPresolve[VarCnt];
              else if ( BorneInfPourPresolve[Var] == BorneSupPourPresolve[Var] ) BFixeDeCnt += A[il] * BorneInfPourPresolve[VarCnt];							
				      else {
		            V[VarCnt] = A[il];
		            T[VarCnt] = 1;
						    NbTermesUtilesDeCnt++;
					    }
				    }  
		        il++;
	        }					
		      BCnt = B[Cnt] - BFixeDeCnt;
          InitV = OUI_PNE;
					if ( NbTermesUtilesDeCnt <= 1 ) break;	 				
					if ( NbTermesUtilesDeCnt > 10 ) break;					
			  }

        if ( NbTerm[Cnt1] >= NbTermesUtilesDeCnt && NbTerm[Cnt1] < 100 ) {				
          PRS_AugmenterLeCreuxComparerContraintes( Pne, Presolve, Cnt, NbTermesUtilesDeCnt, &BCnt, V, T,
				                                           Cnt1, NbContraintesModifees );
          if ( ContrainteInactive[Cnt] == OUI_PNE ) break;
					NombreDeComparaisons++;
					if ( NombreDeComparaisons > NOMBRE_MAX_DE_COMPARAISONS_PAR_CONTRAINTE ) break;
        }
																								 
	    }
      /* RAZ de V et T avant de passer a la contrainte suivante */
		  if ( InitV == OUI_PNE ) {
        il = ilDebCnt;
 	      while ( il < ilMaxCnt ) {
		      V[Nuvar[il]] = 0.0;
		      T[Nuvar[il]] = 0;
		      il++;
	      }
		  }
			ContrainteSource[Cnt] = NON_PNE;
  	}
	  /*********************************************/
		NextVar:
	  Var = ParColonneVariableSuivante[Var];
	}
}

free( Buffer );

# if VERBOSE_PRS == 1 || TRACES == 1
  printf("-> PRS_AugmenterLeCreuxDesContraintes: Nombre de modifications %d\n",*NbContraintesModifees);
	fflush(stdout);
# endif
	
return;
}

/*----------------------------------------------------------------------------*/

void PRS_AugmenterLeCreuxComparerContraintes( PROBLEME_PNE * Pne, PRESOLVE * Presolve, int Cnt, int NbTermesUtilesDeCnt,
                                              double * ValeurNetteDeBCnt_E_S,
                                              double * V, char * T, int Cnt1, int * NbContraintesModifees )
{
int Nb; int il1; int il1Max; int * Mdeb; int * NbTerm; double * A; int * Nuvar; char * SensContrainte; int Var1; double * B;
double Rapport; double * ValeurDeXPourPresolve; int * TypeDeBornePourPresolve; char TypeDeContrainteDeBorne; double X;
double * BorneInfPourPresolve; double * BorneSupPourPresolve; double ValeurDeLaContraintedeBorne; double Delta;
double Smax; char SmaxValide; double Smin; char SminValide; double ValeurNetteDeBCnt_1; char TypeBorne; int Reste;
char SensCnt; char SensCnt1; char * ContrainteInactive; double ValeurNetteDeBCnt; 

ValeurNetteDeBCnt = *ValeurNetteDeBCnt_E_S;

SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

ValeurDeXPourPresolve = Presolve->ValeurDeXPourPresolve;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;

Nb = NbTermesUtilesDeCnt;
SensCnt = SensContrainte[Cnt];
SensCnt1 = SensContrainte[Cnt1];

il1 = Mdeb[Cnt1];
il1Max = il1 + NbTerm[Cnt1];
/* Determination du rapport */
Rapport = 1.0;
while ( il1 < il1Max ) {
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
		TypeBorne = TypeDeBornePourPresolve[Var1];
	  if ( TypeBorne == VARIABLE_FIXE ) goto NextIl1_0;
	  if ( BorneInfPourPresolve[Var1] == BorneSupPourPresolve[Var1] ) goto NextIl1_0;		
    if ( T[Var1] != 0 ) {				
			/* Terme correspondant dans la colonne */				
		  Rapport = V[Var1] / A[il1];
      Nb--;
			il1++;
			break;		 			
	  }
	}
	NextIl1_0:
  il1++;
}

/* On poursuit l'analyse de la contrainte Cnt1 */
while ( il1 < il1Max ) {
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
		TypeBorne = TypeDeBornePourPresolve[Var1];
	  if ( TypeBorne == VARIABLE_FIXE ) goto NextIl1_1;
	  if ( BorneInfPourPresolve[Var1] == BorneSupPourPresolve[Var1] ) goto NextIl1_1;				
		if ( T[Var1] != 0 ) {			 
			/* Terme correspondant dans la colonne */				
			X = fabs( V[Var1] - (Rapport * A[il1]) );      
			if ( X > ZERO_COLINEAIRE ) return;				
	    Nb--;			
		}
	}
	NextIl1_1:
	il1++;
}

if ( Nb != 0 ) return;

ContrainteInactive = Presolve->ContrainteInactive;
  
/* Si contrainte d'egalite on fait une substitution */

if ( SensCnt == '=' && 0 ) {
  Reste = 0;
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    Var1 = Nuvar[il1];
    if ( T[Var1] != 0 ) A[il1] = 0.0;
		else if ( A[il1] != 0.0 ) Reste++;		
    il1++;
  }	
  B[Cnt1] -= ValeurNetteDeBCnt / Rapport;		
  *NbContraintesModifees = *NbContraintesModifees + 1;
  # if TRACES == 1
    printf("Remplacement de termes de la contrainte %d grace a la contrainte d'egalite %d  Rapport %e Reste %d Sur %d\n",Cnt1,Cnt,Rapport,Reste,NbTerm[Cnt1]);
    if ( Reste != 0 ) {
      il1 = Mdeb[Cnt1];
      il1Max = il1 + NbTerm[Cnt1];
      while ( il1 < il1Max ) {
        Var1 = Nuvar[il1];
        if ( A[il1] != 0.0 ) printf(" %e ",A[il1]);
        il1++;
      }
      printf(" %c %e\n",SensCnt1,B[Cnt1]);
    }
	# endif	
	if ( Reste == 0 ) PRS_DesactiverContrainte( Presolve, Cnt1 ); 	
	return;  
}

PRS_CalculMinMaxDeCnt1PourTEgal0 ( Pne, Presolve, Cnt1, &Smin, &SminValide, &Smax, &SmaxValide, &ValeurNetteDeBCnt_1, T );

/* Si Rapport est positif, alors Cnt peut etre vue comme une contrainte de borne sup sur un groupe de variables */
/* Si Rapport est negatif, alors Cnt peut etre vue comme une contrainte de borne inf sur un groupe de variables */

ValeurDeLaContraintedeBorne = ValeurNetteDeBCnt / Rapport;

if ( Rapport > 0 ) TypeDeContrainteDeBorne = CONTRAINTE_DE_BORNE_SUP;		
else TypeDeContrainteDeBorne = CONTRAINTE_DE_BORNE_INF;
if ( SensCnt == '=' ) TypeDeContrainteDeBorne = CONTRAINTE_EGAL;	

if ( SensCnt1 == '<' ) {
  if ( TypeDeContrainteDeBorne == CONTRAINTE_DE_BORNE_SUP || TypeDeContrainteDeBorne == CONTRAINTE_EGAL ) {
    if ( SmaxValide == OUI_PNE && ContrainteInactive[Cnt1] == NON_PNE ) {
      if ( ValeurDeLaContraintedeBorne + Smax < ValeurNetteDeBCnt_1 + (0.05 * SEUIL_DADMISSIBILITE) ) {        
        # if TRACES == 1
				  printf("Contrainte %d redondante grace a la contrainte %d  Max %e  ValeurNetteDeBCnt_1 %e\n",
					        Cnt1,Cnt,ValeurDeLaContraintedeBorne+Smax,ValeurNetteDeBCnt_1);        
			  # endif								
				PRS_DesactiverContrainte( Presolve, Cnt1 );																					
        *NbContraintesModifees = *NbContraintesModifees + 1;
	    }
		}
  }
  if ( TypeDeContrainteDeBorne == CONTRAINTE_DE_BORNE_INF || TypeDeContrainteDeBorne == CONTRAINTE_EGAL ) {
    if ( SminValide == OUI_PNE && ContrainteInactive[Cnt1] == NON_PNE ) {
      if ( ValeurDeLaContraintedeBorne + Smin > ValeurNetteDeBCnt_1 + SEUIL_DADMISSIBILITE ) {
        # if TRACES == 1
				  printf("Contrainte %d infaisable: %e > %e\n",Cnt1,ValeurDeLaContraintedeBorne+Smin,ValeurNetteDeBCnt_1);
			  # endif
			  return; /* Attention, pour aller plus loin il faut dire pas de solution */
	    }	
		  if ( fabs( (ValeurDeLaContraintedeBorne+Smin) - ValeurNetteDeBCnt_1 ) < SEUIL_FORCING_CONSTRAINT ) {        
        # if TRACES == 1
			    printf("Forcing contrainte %d grace a la contrainte %d\n",Cnt1,Cnt);
			  # endif
        /* On fixe les variables pour obtenir Smin */			 
        PRS_AugmenterLeCreuxFaireLeForcing( Presolve, FORCING_AU_MIN, Cnt1, Mdeb, NbTerm, Nuvar, A,
								 											      TypeDeBornePourPresolve, BorneInfPourPresolve, BorneSupPourPresolve, T );
				if ( SensCnt == '=' ) PRS_DesactiverContrainte( Presolve, Cnt1 );																		
        *NbContraintesModifees = *NbContraintesModifees + 1;						  
		  }			
		  else {
		    /* Tentative d'amelioration de bornes des variables de Cnt1 pour les T = 0 */
		    /* On dispose d'une nouvelle borne sup */
				if ( ContrainteInactive[Cnt1] != OUI_PNE ) { 
          PRS_AjusterBornesDesVariablesPourTEgal0 ( Pne, Presolve, Cnt1, '<', Smin, SminValide, Smax, SmaxValide,
				                                            ValeurNetteDeBCnt_1 - ValeurDeLaContraintedeBorne, T, NbContraintesModifees );
        }
		  }
		}
	}
}
else {
  /* Sens de Cnt1 est = */
  if ( TypeDeContrainteDeBorne == CONTRAINTE_DE_BORNE_SUP || TypeDeContrainteDeBorne == CONTRAINTE_EGAL ) {  
    if ( SmaxValide == OUI_PNE && ContrainteInactive[Cnt1] == NON_PNE ) {
	    if (  ValeurDeLaContraintedeBorne + Smax < ValeurNetteDeBCnt_1 - SEUIL_DADMISSIBILITE ) {
        # if TRACES == 1
				  printf("Contrainte %d infaisable: %e < %e\n",Cnt1,ValeurDeLaContraintedeBorne+Smax,ValeurDeLaContraintedeBorne);
			  # endif
			  return; /* Attention, pour aller plus loin il faut dire pas de solution */
		  }
		  if ( fabs( (ValeurDeLaContraintedeBorne+Smax) - ValeurNetteDeBCnt_1 ) < SEUIL_FORCING_CONSTRAINT ) {      
        # if TRACES == 1
			    printf("Forcing contrainte %d grace a la contrainte %d\n",Cnt1,Cnt);
			  # endif
        /* On fixe les variables pour obtenir Smax */			 
        PRS_AugmenterLeCreuxFaireLeForcing( Presolve, FORCING_AU_MAX, Cnt1, Mdeb, NbTerm, Nuvar, A,
								 											      TypeDeBornePourPresolve, BorneInfPourPresolve, BorneSupPourPresolve, T );
				if ( SensCnt == '=' ) PRS_DesactiverContrainte( Presolve, Cnt1 );																		
        *NbContraintesModifees = *NbContraintesModifees + 1;						  
		  }
		  /* Tentative d'amelioration de bornes des variables de Cnt1 pour les T = 0 */
		  /* On dispose d'une nouvelle borne inf */
			if ( ContrainteInactive[Cnt1] != OUI_PNE ) { 
        PRS_AjusterBornesDesVariablesPourTEgal0 ( Pne, Presolve, Cnt1, '>', Smin, SminValide, Smax, SmaxValide,
				                                          ValeurNetteDeBCnt_1 - ValeurDeLaContraintedeBorne, T, NbContraintesModifees );				
      }
		}
  }
	/* Ci dessous: il est important de ne pas mettre else if */
  if ( TypeDeContrainteDeBorne == CONTRAINTE_DE_BORNE_INF || TypeDeContrainteDeBorne == CONTRAINTE_EGAL ) {
    if ( SminValide == OUI_PNE && ContrainteInactive[Cnt1] == NON_PNE ) {
      if ( ValeurDeLaContraintedeBorne + Smin > ValeurNetteDeBCnt_1 + SEUIL_DADMISSIBILITE ) {
        # if TRACES == 1
				  printf("Contrainte %d infaisable: %e > %e\n",Cnt1,ValeurDeLaContraintedeBorne+Smin,ValeurNetteDeBCnt_1);
			  # endif
			  return; /* Attention, pour aller plus loin il faut dire pas de solution */
		  }  
      if ( fabs( (ValeurNetteDeBCnt_1-ValeurDeLaContraintedeBorne) - Smin ) < SEUIL_FORCING_CONSTRAINT ) {	  		
        # if TRACES == 1
			    printf("Forcing contrainte %d grace a la contrainte %d\n",Cnt1,Cnt);
			  # endif
        /* On fixe les variables pour obtenir Smin */			 
        PRS_AugmenterLeCreuxFaireLeForcing( Presolve, FORCING_AU_MIN, Cnt1, Mdeb, NbTerm, Nuvar, A,
								 											      TypeDeBornePourPresolve, BorneInfPourPresolve, BorneSupPourPresolve, T );
				if ( SensCnt == '=' ) PRS_DesactiverContrainte( Presolve, Cnt1 );																																								
        *NbContraintesModifees = *NbContraintesModifees + 1;						  
		  }		
		  /* Tentative d'amelioration de bornes des variables de Cnt1 pour les T = 0 */
		  /* On dispose d'une nouvelle borne sup */
			if ( ContrainteInactive[Cnt1] != OUI_PNE ) { 			
        PRS_AjusterBornesDesVariablesPourTEgal0 ( Pne, Presolve, Cnt1, '<', Smin, SminValide, Smax, SmaxValide, 
				                                          ValeurNetteDeBCnt_1 - ValeurDeLaContraintedeBorne, T, NbContraintesModifees );						    
      }
		}
  }
}

/* Repercussions sur Cnt */
if ( SensCnt == '<' ) {
  if ( SminValide == OUI_PNE ) {
	  if ( Rapport > 0.0 ) {
		  if ( ValeurNetteDeBCnt > Rapport * (ValeurNetteDeBCnt_1 - Smin) ) { 
        # if TRACES == 1        				
			    printf("Rapport = %e on peut faire passer le second membre de la contrainte %d de %e a %e\n",Rapport,Cnt,B[Cnt],Rapport * (ValeurNetteDeBCnt_1 - Smin));
			  # endif
				Delta = ValeurNetteDeBCnt - ( Rapport * (ValeurNetteDeBCnt_1 - Smin) );
				ValeurNetteDeBCnt -= Delta;
				*ValeurNetteDeBCnt_E_S = ValeurNetteDeBCnt;
        B[Cnt] -= Delta;
        *NbContraintesModifees = *NbContraintesModifees + 1;
		  }
	  }
    else {
	    /* Rapport negatif */
      if ( fabs( ValeurNetteDeBCnt - ( Rapport * ( ValeurNetteDeBCnt_1 - Smin ) ) ) < SEUIL_FORCING_CONSTRAINT ) {			
        # if TRACES == 1        				
		      printf("Rapport = %e on peut transformer la contrainte d'inegalite %d en contrainte d'egalite\n",Rapport,Cnt);
			  # endif				
				SensContrainte[Cnt] = '=';
				/* Comme on transforme le sens de la contrainte, la variable duale que l'on avait n'est plus exploitable
				   puisque cette nouvelle contrainte en regroupe 2 */			  
				Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_NON_INITIALISE;		 
        Presolve->LambdaMin[Cnt] = -LINFINI_PNE;
	      Presolve->LambdaMax[Cnt] = LINFINI_PNE;						
        *NbContraintesModifees = *NbContraintesModifees + 1;
			}
		}					
	}
	if ( SensCnt1 == '=' ) {
    if ( SmaxValide == OUI_PNE ) {
	    if ( Rapport > 0.0 ) {
        if ( fabs( ValeurNetteDeBCnt - ( Rapport * ( ValeurNetteDeBCnt_1 - Smax ) ) ) < SEUIL_FORCING_CONSTRAINT ) {		 
          # if TRACES == 1        				
		        printf("Rapport = %e on peut transformer la contrainte d'inegalite %d en contrainte d'egalite\n",Rapport,Cnt);
			    # endif						
          SensContrainte[Cnt] = '=';
				  /* Comme on transforme le sens de la contrainte, la variable duale que l'on avait n'est plus exploitable
				     puisque cette nouvelle contrainte en regroupe 2 */			  
				  Presolve->ConnaissanceDeLambda[Cnt] = LAMBDA_NON_INITIALISE;		 
          Presolve->LambdaMin[Cnt] = -LINFINI_PNE;
	        Presolve->LambdaMax[Cnt] = LINFINI_PNE;											
          *NbContraintesModifees = *NbContraintesModifees + 1;
			  }
			}
			else {
	      /* Rapport negatif */
		    if ( ValeurNetteDeBCnt > Rapport * (ValeurNetteDeBCnt_1 - Smax) ) {  
          # if TRACES == 1        				
			      printf("Rapport = %e on peut faire passer le second membre de la contrainte %d de %e a %e\n",Rapport,Cnt,B[Cnt],Rapport * (ValeurNetteDeBCnt_1 - Smax));
			    # endif
				  Delta = ValeurNetteDeBCnt - ( Rapport * (ValeurNetteDeBCnt_1 - Smax) );
				  ValeurNetteDeBCnt -= Delta;
				  *ValeurNetteDeBCnt_E_S = ValeurNetteDeBCnt;
          B[Cnt] -= Delta;					
          *NbContraintesModifees = *NbContraintesModifees + 1;
				}
		  }	  			
		}
	}
}
else {
  /* Sens de Cnt est = */
  if ( SminValide == OUI_PNE ) {
		if ( ValeurNetteDeBCnt / Rapport > (ValeurNetteDeBCnt_1 - Smin) + SEUIL_DADMISSIBILITE ) {
      # if TRACES == 1        				
			  printf("Pas de solution pour la contrainte %d\n",Cnt);
			# endif
      return;		  
	  }
	}
	if ( SensCnt1 == '=' ) {
	  if ( SmaxValide == OUI_PNE ) {
	 	  if ( ValeurNetteDeBCnt / Rapport < (ValeurNetteDeBCnt_1 - Smax) - SEUIL_DADMISSIBILITE ) {
        # if TRACES == 1        				
			    printf("Pas de solution pour la contrainte %d\n",Cnt);
			  # endif
        return;		  
	    }
		}
	}
}

if ( SensCnt == '=' && ContrainteInactive[Cnt1] == NON_PNE ) {
  Reste = 0;
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    Var1 = Nuvar[il1];
    if ( T[Var1] != 0 ) A[il1] = 0.0;
		else if ( A[il1] != 0.0 ) Reste++;		
    il1++;
  }	
  B[Cnt1] -= ValeurNetteDeBCnt / Rapport;		
  *NbContraintesModifees = *NbContraintesModifees + 1;
  # if TRACES == 1
    printf("Remplacement de termes de la contrainte %d grace a la contrainte d'egalite %d  Rapport %e Reste %d Sur %d\n",Cnt1,Cnt,Rapport,Reste,NbTerm[Cnt1]);
	# endif	
	if ( Reste == 0 ) PRS_DesactiverContrainte( Presolve, Cnt1 ); 	
	return;  
}

return;
}

/*----------------------------------------------------------------------------*/

void PRS_CalculMinMaxDeCnt1PourTEgal0( PROBLEME_PNE * Pne, PRESOLVE * Presolve, int Cnt1,
                                       double * SminS, char * SminValideS,
																			 double * SmaxS, char * SmaxValideS,
                                       double * ValeurNetteDeBCnt1,
																			 char * T )
{

double SFixe; int il1; int il1Max; int * Mdeb; int * NbTerm; double * A; int * Nuvar;
int Var1; double * B; double * ValeurDeXPourPresolve; int * TypeDeBornePourPresolve; 
double * BorneInfPourPresolve; double * BorneSupPourPresolve; double Smax; char SmaxValide;
double Smin; char SminValide; double a1; char TypeBorne;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

ValeurDeXPourPresolve = Presolve->ValeurDeXPourPresolve;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;

SminValide = OUI_PNE;
SmaxValide = OUI_PNE;
Smax = 0;
Smin = 0;
SFixe = 0.0;
 
il1 = Mdeb[Cnt1];
il1Max = il1 + NbTerm[Cnt1];
while ( il1 < il1Max ) {
  if ( SminValide == NON_PNE && SmaxValide == NON_PNE ) break;
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
		if ( T[Var1] == 0 ) {
	    a1 = A[il1];
		  TypeBorne = TypeDeBornePourPresolve[Var1];
	    if ( TypeBorne == VARIABLE_FIXE ) SFixe += a1 * ValeurDeXPourPresolve[Var1];
		  else if ( BorneInfPourPresolve[Var1] == BorneSupPourPresolve[Var1] ) SFixe += a1 * BorneInfPourPresolve[Var1];
			else {
        if ( a1 > 0 ) {
          if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
				    Smax += a1 * BorneSupPourPresolve[Var1];
				  }
				  else SmaxValide = NON_PNE;
          if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {
				    Smin += a1 * BorneInfPourPresolve[Var1];
				  }
			    else SminValide = NON_PNE;					
			  }
			  else {
          if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {
			      Smax += a1 * BorneInfPourPresolve[Var1];
			    }
			    else SmaxValide = NON_PNE;
          if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
			      Smin += a1 * BorneSupPourPresolve[Var1];
				  }
				  else SminValide = NON_PNE;
				}
			}
	  }
	}
  il1++;
}

if ( SminValide == NON_PNE ) Smin = -LINFINI_PNE;
if ( SmaxValide == NON_PNE ) Smax = LINFINI_PNE;

*SminS = Smin;
*SminValideS = SminValide;
*SmaxS = Smax;
*SmaxValideS = SmaxValide;

*ValeurNetteDeBCnt1 = B[Cnt1] - SFixe;

return;
}

/*----------------------------------------------------------------------------*/

void PRS_AjusterBornesDesVariablesPourTEgal0 ( PROBLEME_PNE * Pne, PRESOLVE * Presolve, int Cnt1,
                                               char SensCnt1, double Smin, char SminValide,
																				       double Smax, char SmaxValide, double b,
																				       char * T, int * NbModifications )
{
int il1; int il1Max; int * Mdeb; int * NbTerm; double * A; int * Nuvar; int Var1;
int * TypeDeBornePourPresolve; char BorneInfCalculee; char BorneSupCalculee;
double * BorneInfPourPresolve; double * BorneSupPourPresolve; double a1; char TypeBorne;
double S; int ContrainteBornanteSup; int ContrainteBornanteInf; double Xinf; double Xsup;
char BorneAmelioree; int NbModifs;
# if TRACES_MODIFS_BORNES == 1
  double OldBorne;
# endif

NbModifs = 0;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;

if ( SensCnt1 == '<' ) {
  if ( SminValide == NON_PNE ) return;
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    if ( A[il1] != 0.0 ) {
      Var1 = Nuvar[il1];
		  if ( T[Var1] == 0 ) {
	      a1 = A[il1];
		    TypeBorne = TypeDeBornePourPresolve[Var1];
	      if ( TypeBorne != VARIABLE_FIXE && BorneInfPourPresolve[Var1] != BorneSupPourPresolve[Var1] ) {			
          if ( a1 > 0 ) {
            if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {
              S = Smin - ( a1 * BorneInfPourPresolve[Var1] );
							
							BorneInfCalculee = NON_PNE;
							Xinf = BorneInfPourPresolve[Var1];
	            ContrainteBornanteInf = -1;
							
							BorneSupCalculee = OUI_PNE;
						 	Xsup = ( b - S ) / a1;							
              # if TRACES_MODIFS_BORNES == 1
								OldBorne = BorneSupPourPresolve[Var1];
              # endif								
	            ContrainteBornanteSup = Cnt1;
							
              PRS_MettreAJourLesBornesDUneVariable( Presolve, Var1, BorneInfCalculee, Xinf, ContrainteBornanteInf,
																		                BorneSupCalculee, Xsup, ContrainteBornanteSup, &BorneAmelioree );
							if ( BorneAmelioree == OUI_PNE ) NbModifs++;																								
              # if TRACES_MODIFS_BORNES == 1
								if ( BorneAmelioree == OUI_PNE ) {
                  printf("1- Type <, variable %d borne sup passe de %e a %e\n",Var1,OldBorne,Xsup);
								}
              # endif																																							
				    }
			    }
			    else {
            if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
              S = Smin - ( a1 * BorneSupPourPresolve[Var1] );
							
							BorneInfCalculee = OUI_PNE;
							Xinf = ( b - S ) / a1;
              # if TRACES_MODIFS_BORNES == 1
								OldBorne = BorneInfPourPresolve[Var1];
              # endif
	            ContrainteBornanteInf = Cnt1;
							
							BorneSupCalculee = NON_PNE;
							Xsup = BorneSupPourPresolve[Var1];
	            ContrainteBornanteSup = -1;
							
              PRS_MettreAJourLesBornesDUneVariable( Presolve, Var1, BorneInfCalculee, Xinf, ContrainteBornanteInf,
																		                BorneSupCalculee, Xsup, ContrainteBornanteSup, &BorneAmelioree );
							if ( BorneAmelioree == OUI_PNE ) NbModifs++;																								
              # if TRACES_MODIFS_BORNES == 1
								if ( BorneAmelioree == OUI_PNE ) {
                  printf("2- Type <, variable %d borne inf passe de %e a %e\n",Var1,OldBorne,Xinf);
								}
              # endif																																													
			      }
				  }
			  }
	    }
	  }
    il1++;
  }
}
else if ( SensCnt1 == '>' ) {
  if ( SmaxValide == NON_PNE ) return;	
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    if ( A[il1] != 0.0 ) {
      Var1 = Nuvar[il1];
		  if ( T[Var1] == 0 ) {
	      a1 = A[il1];
		    TypeBorne = TypeDeBornePourPresolve[Var1];
	      if ( TypeBorne != VARIABLE_FIXE && BorneInfPourPresolve[Var1] != BorneSupPourPresolve[Var1] ) {			
          if ( a1 > 0 ) {
            if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_SUPERIEUREMENT ) {
              S = Smax - ( a1 * BorneSupPourPresolve[Var1] );
							
							BorneInfCalculee = OUI_PNE;
							Xinf = ( b - S ) / a1;
              # if TRACES_MODIFS_BORNES == 1
								OldBorne = BorneInfPourPresolve[Var1];
              # endif																			
	            ContrainteBornanteInf = Cnt1;
							
							BorneSupCalculee = NON_PNE;
							Xsup = BorneSupPourPresolve[Var1];
	            ContrainteBornanteSup = -1;
							
              PRS_MettreAJourLesBornesDUneVariable( Presolve, Var1, BorneInfCalculee, Xinf, ContrainteBornanteInf,
																		                BorneSupCalculee, Xsup, ContrainteBornanteSup, &BorneAmelioree );
							if ( BorneAmelioree == OUI_PNE ) NbModifs++;																								
              # if TRACES_MODIFS_BORNES == 1
								if ( BorneAmelioree == OUI_PNE ) {
                  printf("3- Type >, variable %d borne inf passe de %e a %e\n",Var1,OldBorne,Xinf);
								}
              # endif																																										
				    }
			    }
			    else {
            if ( TypeBorne == VARIABLE_BORNEE_DES_DEUX_COTES || TypeBorne == VARIABLE_BORNEE_INFERIEUREMENT ) {
              S = Smax - ( a1 * BorneInfPourPresolve[Var1] );

							BorneInfCalculee = NON_PNE;
							Xinf = BorneInfPourPresolve[Var1];
	            ContrainteBornanteInf = -1;
							
							BorneSupCalculee = OUI_PNE;
							Xsup = ( b - S ) / a1;
              # if TRACES_MODIFS_BORNES == 1
								OldBorne = BorneSupPourPresolve[Var1];
              # endif
						  ContrainteBornanteSup = Cnt1;
							
              PRS_MettreAJourLesBornesDUneVariable( Presolve, Var1, BorneInfCalculee, Xinf, ContrainteBornanteInf,
																		                BorneSupCalculee, Xsup, ContrainteBornanteSup, &BorneAmelioree );																											
							if ( BorneAmelioree == OUI_PNE ) NbModifs++;																								
              # if TRACES_MODIFS_BORNES == 1
								if ( BorneAmelioree == OUI_PNE ) {
                  printf("4- Type >, variable %d borne sup passe de %e a %e\n",Var1,OldBorne,Xsup);
								}
              # endif																																													
			      }
				  }
			  }
	    }
	  }
    il1++;
  }
}

*NbModifications = *NbModifications + NbModifs;

return;
}

/*----------------------------------------------------------------------------*/

void PRS_AugmenterLeCreuxFaireLeForcing( PRESOLVE * Presolve, char TypeDeForcing, int Cnt1,
                                         int * Mdeb, int * NbTerm, int * Nuvar, double * A,
																				 int * TypeDeBornePourPresolve, double * BorneInfPourPresolve,
                                         double * BorneSupPourPresolve, char * T )
{
int il1; int il1Max; int Var1; 

if ( TypeDeForcing == FORCING_AU_MIN ) {
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    Var1 = Nuvar[il1];
    if ( T[Var1] == 0 ) {
      if ( TypeDeBornePourPresolve[Var1] != VARIABLE_FIXE && BorneInfPourPresolve[Var1] != BorneSupPourPresolve[Var1] ) {
        if ( A[il1] > 0 ) PRS_FixerUneVariableAUneValeur( Presolve, Var1, BorneInfPourPresolve[Var1] );
			  else if ( A[il1] < 0 ) PRS_FixerUneVariableAUneValeur( Presolve, Var1, BorneSupPourPresolve[Var1] );   														  					 
			}
		}
    il1++;
  }
}
else {
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    Var1 = Nuvar[il1];
    if ( T[Var1] == 0 ) {
      if ( TypeDeBornePourPresolve[Var1] != VARIABLE_FIXE && BorneInfPourPresolve[Var1] != BorneSupPourPresolve[Var1] ) {
        if ( A[il1] > 0 ) PRS_FixerUneVariableAUneValeur( Presolve, Var1, BorneSupPourPresolve[Var1] );
		    else if ( A[il1] < 0 ) PRS_FixerUneVariableAUneValeur( Presolve, Var1, BorneInfPourPresolve[Var1] );
			}
		} 		 					  	    
    il1++;
  }
}
return;
}
