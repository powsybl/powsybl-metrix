/***********************************************************************

   FONCTION: 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"
# include "prs_define.h"

# include "pne_define.h"
		
# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	  
  # include "prs_memoire.h"
# endif
  
# define ZERO_COLINEAIRE  1.e-10

void PRS_CreuxComparerVariables( PROBLEME_PNE * , PRESOLVE * , int * , int , double * , char * , int , double * , int * );

/*----------------------------------------------------------------------------*/

void PRS_AugmenterLeCreuxDesColonnes( PRESOLVE * Presolve, int * NbModifications )     
{
int NombreDeVariables; int NombreDeContraintes; int * Mdeb; int * NbTerm;
int * Nuvar; double * A; int Cnt; int il1; char InitV; int Var1; int ic;
int * Cdeb; int * Csui; int * NumContrainte; int ilMax; char * SensContrainte;
double * B; int NbVarDeCnt; int il; char * Buffer; int * TypeDeVariable;
int * TypeDeBornePourPresolve; int NbT; int Var; int * NumVarDeCnt; int VarCnt;
double * V; int LallocTas; char * ContrainteInactive; char * T; char * pt;
int CntDeVar; int * ParLignePremiereContrainte; int * ParLigneContrainteSuivante;
int * TypeDeBorneNative; double * BorneInfPourPresolve; char TypeBrn;
double * BorneSupPourPresolve; double * BorneInfNative; double * BorneSupNative;
char * ConserverLaBorneInfDuPresolve; char * ConserverLaBorneSupDuPresolve;
int * NbTermesUtilesDeVar; double CoutDeVar; int NbModif; char TypeBrnDeVar; 
int icPrec; PROBLEME_PNE * Pne; double * CoutLineaire;
char MinSommeDesAiFoisLambdaValide; double MinSommeDesAiFoisLambda; double CBarre;
char MaxSommeDesAiFoisLambdaValide; double MaxSommeDesAiFoisLambda; double CBarreMin;
char SommeDesAiFoisLambdaValide; double SommeDesAiFoisLambda; double CBarreMax;
char * VariableSource;

*NbModifications = 0;

return;

printf("AugmenterLeCreuxDesColonnes\n");

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorneNative = Pne->TypeDeBorneTrav;
BorneInfNative = Pne->UminTrav;
BorneSupNative = Pne->UmaxTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;
B = Pne->BTrav;
SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
Cdeb = Pne->CdebTrav;   
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
CoutLineaire = Pne->LTrav;

ParLignePremiereContrainte = Presolve->ParLignePremiereContrainte;
ParLigneContrainteSuivante = Presolve->ParLigneContrainteSuivante;
TypeDeBornePourPresolve = Presolve->TypeDeBornePourPresolve;
BorneInfPourPresolve = Presolve->BorneInfPourPresolve;
BorneSupPourPresolve = Presolve->BorneSupPourPresolve;
ConserverLaBorneInfDuPresolve = Presolve->ConserverLaBorneInfDuPresolve;
ConserverLaBorneSupDuPresolve = Presolve->ConserverLaBorneSupDuPresolve;
ContrainteInactive = Presolve->ContrainteInactive;

LallocTas = 0;
LallocTas += NombreDeContraintes * sizeof( double ); /* V */
LallocTas += NombreDeContraintes * sizeof( char ); /* T */
LallocTas += NombreDeVariables * sizeof( int ); /* NumVarDeCnt */
LallocTas += NombreDeVariables * sizeof( int ); /* NbTermesUtilesDeVar */
LallocTas += NombreDeVariables * sizeof( char ); /* VariableSource */

Buffer = (char *) malloc( LallocTas );
if ( Buffer == NULL ) {
  printf(" Solveur PNE , memoire insuffisante dans le presolve. Sous-programme: PRS_ColonnesColineaires \n");
	return;
}

pt = Buffer;
V = (double *) pt;
pt += NombreDeContraintes * sizeof( double );
T = (char *) pt;
pt +=  NombreDeContraintes * sizeof( char );
NumVarDeCnt = (int *) pt;
pt += NombreDeVariables * sizeof( int ); 
NbTermesUtilesDeVar = (int *) pt;
pt += NombreDeVariables * sizeof( int );
VariableSource = (char *) pt;
pt += NombreDeVariables * sizeof( char );

memset( (char *) T, 0, NombreDeContraintes * sizeof( char ) );
memset( (char *) VariableSource, OUI_PNE, NombreDeVariables * sizeof( char ) );

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
	/* On en profite pour dechainer les lignes qui correspondent a des contraintes inactives */
  NbT = 0;
	ic = Cdeb[Var]; 
	icPrec = -1;
  while ( ic >= 0 ) {
	  Cnt = NumContrainte[ic];
    if ( ContrainteInactive[Cnt] != OUI_PNE && A[ic] != 0.0 ) {
		  NbT++;
		  icPrec = ic;			
		  ic = Csui[ic];
		}
		else {
	    /* On en profite pour dechainer les lignes qui correspondent a des contraintes inactives */						
      ic = Csui[ic];			
			if ( icPrec >= 0 ) {
			  Csui[icPrec] = ic;				
			}
			else {
			  Cdeb[Var] = ic;
				if ( ic < 0 ) break; /* Attention s'il ne reste plus rien dans la ligne il faut sortir */				
			}
		}		
  }
	NbTermesUtilesDeVar[Var] = NbT;
}

/* On balaye les lignes dans l'ordre croissant du nombre de termes et on ne compare que les
   les colonnes qui ont un terme dans cette ligne */

for ( NbT = 2 ; NbT <= Presolve->NbMaxTermesDesLignes ; NbT++ ) {	
  Cnt = ParLignePremiereContrainte[NbT];
	while ( Cnt >= 0 ) {
	  if ( ContrainteInactive[Cnt] != NON_PNE ) goto NextCnt;
	  /* On prepare la table des colonnes a explorer */
		il = Mdeb[Cnt];
		ilMax = il + NbTerm[Cnt];
		NbVarDeCnt = 0;
		while ( il < ilMax ) {
		  if ( A[il] != 0 ) {
			  VarCnt = Nuvar[il];

        TypeBrnDeVar = PRS_ChoisirLaBorneLaPlusContraignantePourLesVariablesDuales( 
                        Pne->TypeDeBorneTrav[VarCnt], Presolve->TypeDeBornePourPresolve[VarCnt],
										    Presolve->BorneInfPourPresolve[VarCnt], Pne->UminTrav[VarCnt],
										    Presolve->BorneSupPourPresolve[VarCnt], Pne->UmaxTrav[VarCnt],
										    Presolve->ConserverLaBorneInfDuPresolve[VarCnt], Presolve->ConserverLaBorneSupDuPresolve[VarCnt],
										    Presolve->TypeDeValeurDeBorneInf[VarCnt], Presolve->TypeDeValeurDeBorneSup[VarCnt] );

				/* Inutile d'etudier les variables fixes et les variables bornees des 2 cotes */
				if ( TypeDeBornePourPresolve[VarCnt] != VARIABLE_FIXE && TypeBrnDeVar != VARIABLE_BORNEE_DES_DEUX_COTES ) {
				  NumVarDeCnt[NbVarDeCnt] = VarCnt;
					NbVarDeCnt++;
				}
		  }
	    il++;
	  }		
	  for ( il = 0 ; il < NbVarDeCnt ; il++ ) {
      Var = NumVarDeCnt[il];
			if ( VariableSource[Var] == NON_PNE ) continue;
			
			/*if ( NbTermesUtilesDeVar[Var] < 1 ) continue;*/

      TypeBrnDeVar = PRS_ChoisirLaBorneLaPlusContraignantePourLesVariablesDuales( 
                        Pne->TypeDeBorneTrav[Var], Presolve->TypeDeBornePourPresolve[Var],
										    Presolve->BorneInfPourPresolve[Var], Pne->UminTrav[Var],
										    Presolve->BorneSupPourPresolve[Var], Pne->UmaxTrav[Var],
										    Presolve->ConserverLaBorneInfDuPresolve[Var], Presolve->ConserverLaBorneSupDuPresolve[Var],
										    Presolve->TypeDeValeurDeBorneInf[Var], Presolve->TypeDeValeurDeBorneSup[Var] );
				

      printf("Variable Var %d NbTermesUtilesDeVar %d ",Var,NbTermesUtilesDeVar[Var]);
      if ( TypeBrnDeVar == VARIABLE_NON_BORNEE ) printf(" VARIABLE_NON_BORNEE\n"); 
			else if ( TypeBrnDeVar == VARIABLE_BORNEE_INFERIEUREMENT ) printf(" VARIABLE_BORNEE_INFERIEUREMENT\n");
			else if ( TypeBrnDeVar == VARIABLE_BORNEE_SUPERIEUREMENT ) printf(" VARIABLE_BORNEE_SUPERIEUREMENT\n");
			else if ( TypeBrnDeVar == VARIABLE_FIXE ) { printf(" VARIABLE_FIXE\n"); exit(0); }
			else if ( TypeBrnDeVar == VARIABLE_BORNEE_DES_DEUX_COTES ) { printf(" VARIABLE_BORNEE_DES_DEUX_COTES\n"); exit(0); }
      else { printf(" code inconnu\n"); exit(0); }
			
			CoutDeVar = CoutLineaire[Var];
			NbModif = 0;
		  InitV = NON_PNE;		
      /* On compare a Var toutes les variables suivantes de la contrainte */
	    for ( il1 = 0 ; il1 < NbVarDeCnt ; il1++ ) {
        Var1 = NumVarDeCnt[il1];
				if ( Var1 == Var ) continue;

        TypeBrn = PRS_ChoisirLaBorneLaPlusContraignantePourLesVariablesDuales( 
                      Pne->TypeDeBorneTrav[Var1], Presolve->TypeDeBornePourPresolve[Var1],
										  Presolve->BorneInfPourPresolve[Var1], Pne->UminTrav[Var1],
										  Presolve->BorneSupPourPresolve[Var1], Pne->UmaxTrav[Var1],
										  Presolve->ConserverLaBorneInfDuPresolve[Var1], Presolve->ConserverLaBorneSupDuPresolve[Var1],
										  Presolve->TypeDeValeurDeBorneInf[Var1], Presolve->TypeDeValeurDeBorneSup[Var1] );

        /*if ( TypeBrn != VARIABLE_NON_BORNEE ) continue;*/		
				
			  /* Comparaison de Var a Var1 */
			  if ( InitV == NON_PNE ) {
		      /* Preparation des tables pour la variable Var */
					/* Les contraintes inactives ont ete dechainees */
					NbT = 0;
	        ic = Cdeb[Var];
	        while ( ic >= 0 ) {
					  if ( A[ic] != 0.0 ) {
						
              if ( ContrainteInactive[NumContrainte[ic]] == OUI_PNE ) {
						    printf("BUG sur les contraintes inactives\n");
							  exit(0);
							}
						
					    CntDeVar = NumContrainte[ic];						
		          V[CntDeVar] = A[ic];
		          T[CntDeVar] = 1;
							NbT++;
						}
		        ic = Csui[ic];
	        }
          InitV = OUI_PNE;
					if ( NbTermesUtilesDeVar[Var] != NbT ) {
            printf("BUG  Var %d NbTermesUtilesDeVar %d NbT %d\n",Var,NbTermesUtilesDeVar[Var],NbT);
						exit(0);
					}
			  }
				if ( NbTermesUtilesDeVar[Var] > NbTermesUtilesDeVar[Var1] ) continue;


				/*
        printf("      comparaison a  Var1 %d NbTermesUtilesDeVar1 %d ",Var1,NbTermesUtilesDeVar[Var1]);
        if ( TypeBrn == VARIABLE_NON_BORNEE ) printf(" VARIABLE_NON_BORNEE\n");
			  else if ( TypeBrn == VARIABLE_BORNEE_INFERIEUREMENT ) printf(" VARIABLE_BORNEE_INFERIEUREMENT\n");
			  else if ( TypeBrn == VARIABLE_BORNEE_SUPERIEUREMENT ) printf(" VARIABLE_BORNEE_SUPERIEUREMENT\n");
			  else if ( TypeBrn == VARIABLE_FIXE ) { printf(" VARIABLE_FIXE\n"); exit(0); }
			  else if ( TypeBrn == VARIABLE_BORNEE_DES_DEUX_COTES ) { printf(" VARIABLE_BORNEE_DES_DEUX_COTES\n"); exit(0); }
        else { printf(" code inconnu\n"); exit(0); }
			  */

        PRS_CreuxComparerVariables( Pne, Presolve, NbTermesUtilesDeVar, Var, V, T, Var1, &CoutDeVar, &NbModif );

				*NbModifications = *NbModifications + NbModif;
				
	    }
	    /* Synthese */
			
# if ABC == 1 
      if ( NbModif > 0 ) {
			  printf("NbModif %d\n",NbModif);
        /* Calcul des produits scalaires sur les termes restants */
        PRS_CalculSommeDeAiFoisLambda( Pne, Presolve, Var, T,
                                       &MinSommeDesAiFoisLambdaValide, &MinSommeDesAiFoisLambda,
																	   	 &MaxSommeDesAiFoisLambdaValide, &MaxSommeDesAiFoisLambda,
																		   &SommeDesAiFoisLambdaValide, &SommeDesAiFoisLambda );     
        if ( SommeDesAiFoisLambdaValide == OUI_PNE ) {
			    CBarre = CoutDeVar - SommeDesAiFoisLambda;
          if ( CBarre > ZERO_CBARRE ) {
					  printf("CBarre %e\n",CBarre);
            /* On fixe la variable su la borne inf. Attention verifier que c'est possible. */
						printf("Variable %d fixee sur borne inf \n",Var);
            PRS_FixerUneVariableAUneValeur( Presolve, Var, BorneInfPourPresolve[Var] );   						
					}
					else if ( CBarre < -ZERO_CBARRE ) {
            /* On fixe la variable sur la borne inf */
						printf("Variable %d fixee sur borne sup \n",Var);
            PRS_FixerUneVariableAUneValeur( Presolve, Var, BorneSupPourPresolve[Var] );   
					}
					goto RazDeT;
        }
				if ( MinSommeDesAiFoisLambdaValide == OUI_PNE ) {
          CBarreMax = CoutDeVar - MinSommeDesAiFoisLambda;
				  printf("CBarreMax %e\n",CBarreMax);
					if ( CBarreMax < -ZERO_CBARRE ) {
						printf("Variable %d fixee sur borne sup \n",Var);
            PRS_FixerUneVariableAUneValeur( Presolve, Var, BorneSupPourPresolve[Var] );   
					  goto RazDeT;
					}
				}
				if ( MaxSommeDesAiFoisLambdaValide == OUI_PNE ) {
          CBarreMin = CoutDeVar - MaxSommeDesAiFoisLambda;
				  printf("CBarreMin %e\n",CBarreMin);
					if ( CBarreMin > ZERO_CBARRE ) {
						printf("Variable %d fixee sur borne inf \n",Var);
            PRS_FixerUneVariableAUneValeur( Presolve, Var, BorneInfPourPresolve[Var] );   
					  goto RazDeT;
					}
				}																			 
			}
# endif

			/* Raz de T */
			RazDeT:
		  if ( InitV == OUI_PNE ) {
	      ic = Cdeb[Var];
 	      while ( ic >= 0 ) {
		      V[NumContrainte[ic]] = 0.0;
		      T[NumContrainte[ic]] = 0;
		      ic = Csui[ic];
	      }
		  }
			VariableSource[Var] = NON_PNE;
  	}
    NextCnt:
		Cnt = ParLigneContrainteSuivante[Cnt];
	}	
}

free( Buffer );

# if VERBOSE_PRS == 1 
  printf("-> Nombre de modifications de colonnes %d\n",*NbModifications );
	fflush(stdout);
# endif

  printf("-> Nombre de modifications de colonnes %d\n",*NbModifications );


	

return;
}

/*----------------------------------------------------------------------------*/

void PRS_CreuxComparerVariables( PROBLEME_PNE * Pne, PRESOLVE * Presolve, int * NbTermesUtilesDeVar, int Var,
                                 double * V, char * T, int Var1, double * CDeVar, int * NbModifications )
{
int ic1; int * Cdeb; int * Csui; double Nu; int Cnt1; int * NumContrainte; double * A; int Nb; double X;
double Rapport; char TypeBrnVar1; int Reste;

Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
A = Pne->ATrav;

Nu = 1.; /* Juste pour eviter les warning de compilation */
Nb = NbTermesUtilesDeVar[Var];
Rapport = 1.0;
/* Determination du rapport */
/* Les contraintes inactives ont ete dechainees */
ic1 = Cdeb[Var1];
while ( ic1 >= 0 ) {
	if ( A[ic1] != 0 ) {
    Cnt1 = NumContrainte[ic1];
    if ( T[Cnt1] != 0 ) {
		  Rapport = V[Cnt1] / A[ic1];
		  Nb--;
      ic1 = Csui[ic1];
		  break;
		}
	}
  ic1 = Csui[ic1];	
}

/* On poursuit l'analyse de la variable Var1 */		
while ( ic1 >= 0 ) {
	if ( A[ic1] != 0 ) {
    Cnt1 = NumContrainte[ic1];
    if ( T[Cnt1] != 0 ) {
	    X = fabs( V[Cnt1] - (Rapport * A[ic1]) );
      if ( X > ZERO_COLINEAIRE ) {
		    return;
		  }
		  Nb--;
	  }
	}
  ic1 = Csui[ic1];
}

if ( Nb != 0 ) {
  /*printf("NbTermesUtilesDeVar[Var] %d Nb %d\n",NbTermesUtilesDeVar[Var],Nb);*/
  return;
}


printf("             possibilite de qq chose Var %d Var1 %d  Rapport %e  L[%d] = %e\n",Var,Var1,Rapport,Var1,Pne->LTrav[Var1]);
TypeBrnVar1 = PRS_ChoisirLaBorneLaPlusContraignantePourLesVariablesDuales( 
                      Pne->TypeDeBorneTrav[Var1], Presolve->TypeDeBornePourPresolve[Var1],
										  Presolve->BorneInfPourPresolve[Var1], Pne->UminTrav[Var1],
										  Presolve->BorneSupPourPresolve[Var1], Pne->UmaxTrav[Var1],
										  Presolve->ConserverLaBorneInfDuPresolve[Var1], Presolve->ConserverLaBorneSupDuPresolve[Var1],
										  Presolve->TypeDeValeurDeBorneInf[Var1], Presolve->TypeDeValeurDeBorneSup[Var1] );
if ( TypeBrnVar1 == VARIABLE_NON_BORNEE ) printf(" VARIABLE_NON_BORNEE\n");
else if ( TypeBrnVar1 == VARIABLE_BORNEE_INFERIEUREMENT ) printf(" VARIABLE_BORNEE_INFERIEUREMENT\n");
else if ( TypeBrnVar1 == VARIABLE_BORNEE_SUPERIEUREMENT ) printf(" VARIABLE_BORNEE_SUPERIEUREMENT\n");
else if ( TypeBrnVar1 == VARIABLE_FIXE ) { printf(" VARIABLE_FIXE\n"); exit(0); }
else if ( TypeBrnVar1 == VARIABLE_BORNEE_DES_DEUX_COTES ) { printf(" VARIABLE_BORNEE_DES_DEUX_COTES\n"); exit(0); }
else { printf(" code inconnu\n"); exit(0); }
											
Reste = 0;
ic1 = Cdeb[Var1];
while ( ic1 >= 0 ) {
  if ( A[ic1] != 0.0 ) {
    Cnt1 = NumContrainte[ic1];	
    if ( T[Cnt1] != 0 ) printf(" %e(T=1 %d  %e/%e) + ",A[ic1],Cnt1,Presolve->LambdaMin[Cnt1],Presolve->LambdaMax[Cnt1]);
		else {						
      printf(" %e(T=0 %d  %e/%e) + ",A[ic1],Cnt1,Presolve->LambdaMin[Cnt1],Presolve->LambdaMax[Cnt1]);
		  Reste++;
		}
	}
  ic1 = Csui[ic1];
}
printf(" < %e   Reste %d\n",Pne->LTrav[Var1]/*-Pne->LTrav[Var]*/,Reste);


*NbModifications = *NbModifications + 1;
return;


/*printf("Les termes de la colonne %d sont contenus dans la colonne %d rapport %e\n",Var,Var1,Rapport);*/

/* On remplace les termes concernes de la colonne Var1 par 0, on modifie les valeurs des autres termes
   et on modifie le cout de la variable Var1 */

TypeBrnVar1 = PRS_ChoisirLaBorneLaPlusContraignantePourLesVariablesDuales( 
                      Pne->TypeDeBorneTrav[Var1], Presolve->TypeDeBornePourPresolve[Var1],
										  Presolve->BorneInfPourPresolve[Var1], Pne->UminTrav[Var1],
										  Presolve->BorneSupPourPresolve[Var1], Pne->UmaxTrav[Var1],
										  Presolve->ConserverLaBorneInfDuPresolve[Var1], Presolve->ConserverLaBorneSupDuPresolve[Var1],
										  Presolve->TypeDeValeurDeBorneInf[Var1], Presolve->TypeDeValeurDeBorneSup[Var1] );

if ( TypeBrnVar1 == VARIABLE_NON_BORNEE || 1 ) {
  Reste = 0;
  ic1 = Cdeb[Var1];
  while ( ic1 >= 0 ) {
    Cnt1 = NumContrainte[ic1];
    if ( T[Cnt1] != 0 ) A[ic1] = 0.0;
		else Reste++;  
    ic1 = Csui[ic1];
  }
	NbTermesUtilesDeVar[Var1] = Reste;
	printf("NbTermesUtilesDeVar %d Reste %d\n",NbTermesUtilesDeVar[Var],Reste);
  Pne->LTrav[Var1] = Pne->LTrav[Var1] - ( Pne->LTrav[Var] / Rapport );
 *NbModifications = *NbModifications + 1;

}
											
return;
}

/*----------------------------------------------------------------------------*/


