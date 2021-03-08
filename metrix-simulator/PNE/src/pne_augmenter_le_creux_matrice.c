/***********************************************************************

   FONCTION: On essai de remplacer des groupes de variables a l'aide de
	           contraintes d'egalite. On essai aussi de detecter la
						 redondance de certaines contraintes.
                 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
  
# include "bb_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define ZERO_COLINEAIRE  1.e-10

void PNE_CreuxMatriceComparerContraintes( PROBLEME_PNE * , int , int , double , double * , char * , int , char * , char * );


int NbCntRedondantes = 0;
/*----------------------------------------------------------------------------*/

void PNE_AugmenterLeCreuxDeLaMatrice( PROBLEME_PNE * Pne )
{
int il; int ic; int ilDebCnt; int ilMaxCnt; int ic1;  int NombreDeContraintes;
int NombreDeVariables; int Cnt; int Cnt1; double * V; char * T; int LallocTas;
double BCnt; double * B; int * Mdeb; int * NbTerm; int * Nuvar; double * A;
int * NumContrainte; int NbTermesUtilesDeCnt; double S; char * pt;
int * Cdeb; int * Csui; int * TypeDeBorne; double * X; double * Xmin; double * Xmax;
int Var; int NbT; int * ParColonnePremiereVariable; int * ParColonneVariableSuivante;
char * Buffer; char * SensContrainte; char Modifications; char * CntRedondante;
char InitV; int NbCntDeVar; int * NumCntDeVar; int VarCnt;
int NbMaxTermesDesColonnes; int MxTrm;

return;

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

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

TypeDeBorne = Pne->TypeDeBorneTrav;
X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;

LallocTas = 0;
LallocTas += NombreDeVariables * sizeof( double ); /* V */
LallocTas += NombreDeVariables * sizeof( char ); /* T */
LallocTas += NombreDeContraintes * sizeof( int ); /* NumCntDeVar */
LallocTas += (NombreDeContraintes+1) * sizeof( int ); /* ParColonnePremiereVariable */
LallocTas += NombreDeVariables * sizeof( int ); /* ParColonneVariableSuivante */
LallocTas += NombreDeContraintes * sizeof( char ); /* CntRedondante */

Buffer = (char *) malloc( LallocTas );
if ( Buffer == NULL ) {
  printf(" Solveur PNE , memoire insuffisante dans le presolve. Sous-programme: PNE_ContraintesColineaires \n");
	return;
}

pt = Buffer;
V = (double *) pt;
pt += NombreDeVariables * sizeof( double );
T = (char *) pt;
pt +=  NombreDeVariables * sizeof( char );
NumCntDeVar = (int *) pt;
pt += NombreDeContraintes * sizeof( int );
ParColonnePremiereVariable = (int *) pt;
pt += (NombreDeContraintes+1) * sizeof( int );
ParColonneVariableSuivante = (int *) pt;
pt += NombreDeVariables * sizeof( int );
CntRedondante = (char *) pt;
pt += NombreDeContraintes * sizeof( char );

memset( (char *) T, 0, NombreDeVariables * sizeof( char ) );
memset( (char *) CntRedondante, NON_PNE, NombreDeContraintes * sizeof( char ) );

for ( NbT = 0 ; NbT <= NombreDeContraintes ; NbT++ ) ParColonnePremiereVariable[NbT] = -1;
MxTrm = -1;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
	if ( Xmin[Var] == Xmax[Var] ) continue;
  NbT = 0;
	ic = Cdeb[Var];
  while ( ic >= 0 ) {
		if ( A[ic] != 0 ) NbT++;					
		ic = Csui[ic];
  }
	if ( NbT > MxTrm ) MxTrm = NbT;	
	ic = ParColonnePremiereVariable[NbT];	
	ParColonnePremiereVariable[NbT] = Var;	  
	ParColonneVariableSuivante[Var] = ic;	
}
NbMaxTermesDesColonnes = MxTrm;

Modifications = NON_PNE;

/* On balaye les colonnes dans l'ordre croissant du nombre de termes et on ne compare que les
   les lignes qui ont un terme dans cette colonne */
	 
for ( NbT = 2 ; NbT <= NbMaxTermesDesColonnes ; NbT++ ) {
  Var = ParColonnePremiereVariable[NbT];
	while ( Var >= 0 ) {
	  /*********************************************/
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) goto NextVar;
	  if ( Xmin[Var] == Xmax[Var] ) goto NextVar;
	  /* On prepare la table des contraintes a explorer */
 	  NbCntDeVar = 0;
	  ic = Cdeb[Var];
    while ( ic >= 0 ) {
		  if ( A[ic] != 0 ) {
		    Cnt = NumContrainte[ic];
			  NumCntDeVar[NbCntDeVar] = Cnt;
				NbCntDeVar++;			  
      }
	    ic = Csui[ic];
	  }

	  for ( ic = 0 ; ic < NbCntDeVar ; ic++ ) {
	    Cnt = NumCntDeVar[ic];
			if ( CntRedondante[Cnt] == OUI_PNE ) continue;
			if ( SensContrainte[Cnt] != '=' ) continue;
      ilDebCnt = 0;
		  InitV = NON_PNE;
			ilMaxCnt = -1;
			BCnt = LINFINI_PNE;
			NbTermesUtilesDeCnt = 0;
      /* On compare a Cnt toutes les contraintes d'inegalite de la colonne */
	    for ( ic1 = 0 ; ic1 < NbCntDeVar ; ic1++ ) {
	      Cnt1 = NumCntDeVar[ic1];
				if ( CntRedondante[Cnt1] == OUI_PNE ) continue;
				if ( Cnt1 == Cnt ) continue;
			  /* Comparaison de Cnt a Cnt1 */
			  if ( InitV == NON_PNE ) {
		      /* Preparation des tables pour la contrainte Cnt */
			    S = 0.0;
          ilDebCnt = Mdeb[Cnt];
	        ilMaxCnt = ilDebCnt + NbTerm[Cnt];
	        il = ilDebCnt;
	        while ( il < ilMaxCnt ) {
			      if ( A[il] != 0.0 ) {
					    VarCnt = Nuvar[il];
				      if ( TypeDeBorne[VarCnt] == VARIABLE_FIXE ) S += A[il] * X[VarCnt];
              else if ( Xmin[VarCnt] == Xmax[VarCnt] ) S += A[il] * Xmin[VarCnt];							
				      else {
		            V[VarCnt] = A[il];
		            T[VarCnt] = 1;
						    NbTermesUtilesDeCnt++;
					    }							
				    }  
		        il++;
	        }
		      BCnt = B[Cnt] - S;
          InitV = OUI_PNE;
					if ( NbTermesUtilesDeCnt <= 0 ) break;					
			  }
				
        if ( NbTerm[Cnt1] >= NbTermesUtilesDeCnt ) {
				  Modifications = NON_PNE;
				  PNE_CreuxMatriceComparerContraintes( Pne, Cnt, NbTermesUtilesDeCnt, BCnt, V, T, Cnt1, CntRedondante, &Modifications );
					if ( Modifications == OUI_PNE && 0 ) {
            /* Recalcul de BCnt, V, T ????????????????? */
            il = ilDebCnt;
 	          while ( il < ilMaxCnt ) {
		          V[Nuvar[il]] = 0.0;
		          T[Nuvar[il]] = 0;
		          il++;
	          }						
			      S = 0.0;
						NbTermesUtilesDeCnt = 0;
	          il = ilDebCnt;
	          while ( il < ilMaxCnt ) {
			        if ( A[il] != 0.0 ) {
					      VarCnt = Nuvar[il];
				        if ( TypeDeBorne[VarCnt] == VARIABLE_FIXE ) S += A[il] * X[VarCnt];
                else if ( Xmin[VarCnt] == Xmax[VarCnt] ) S += A[il] * Xmin[VarCnt];							
				        else {
		              V[VarCnt] = A[il];
		              T[VarCnt] = 1;
						      NbTermesUtilesDeCnt++;
					      }							
				      }  
		          il++;
	          }
		        BCnt = B[Cnt] - S;
					  if ( NbTermesUtilesDeCnt <= 0 ) break;										
					}
				}
				
	    }			
      /* RAZ de V et T avant de passer a la contrainte suivante */
		  /*Flag[Cnt] = 1;*/
		  if ( InitV == OUI_PNE ) {
        il = ilDebCnt;
 	      while ( il < ilMaxCnt ) {
		      V[Nuvar[il]] = 0.0;
		      T[Nuvar[il]] = 0;
		      il++;
	      }
		  }
  	}
	  /*********************************************/
		NextVar:
	  Var = ParColonneVariableSuivante[Var];
	}
}

free( Buffer );

printf("NbCntRedondantes %d\n",NbCntRedondantes);

return;
}

/*----------------------------------------------------------------------------*/

void PNE_CreuxMatriceComparerContraintes( PROBLEME_PNE * Pne, int Cnt, int NbTermesUtilesDeCnt, double BCnt,
                                          double * V, char * T, int Cnt1, char * CntRedondante, char * Modifications )
{
int Nb; int il1; int il1Max; int * Mdeb; int * NbTerm; double * A; int * Nuvar; int Var1;
double * B; double Rapport; char * SensContrainte; int * TypeDeBorne; double * X;
double * Xmin; double * Xmax; double R; char TypeDeContrainteDeBorne; double ValeurDeLaContraintedeBorne;
char ContrainteDeBorneInf; char ContrainteDeBorneSup; char ContrainteEgal; 

SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

TypeDeBorne = Pne->TypeDeBorneTrav;
X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;

Nb = NbTermesUtilesDeCnt;

il1 = Mdeb[Cnt1];
il1Max = il1 + NbTerm[Cnt1];
/* Determination du rapport */
Rapport = 1.0;
while ( il1 < il1Max ) {
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
	  if ( TypeDeBorne[Var1] == VARIABLE_FIXE ) goto NextIl1_0;
		if ( Xmin[Var1] == Xmax[Var1] ) goto NextIl1_0;
    if ( T[Var1] != 0 ) {
			/* Terme correspondant dans la colonne */				
		  Rapport = V[Var1] / A[il1];
			if ( fabs( Rapport ) < 1.e-6 || fabs( Rapport ) > 1.e+6 ) return;
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
	  if ( TypeDeBorne[Var1] == VARIABLE_FIXE ) goto NextIl1_1;
		if ( Xmin[Var1] == Xmax[Var1] ) goto NextIl1_1;
		if ( T[Var1] != 0 ) {			 
			/* Terme correspondant dans la colonne */				
			R = fabs( V[Var1] - (Rapport * A[il1]) );
      if ( R > ZERO_COLINEAIRE ) return;				
	    Nb--;
		}		
	}
	NextIl1_1:
	il1++;
}

if ( Nb != 0 ) return;

/*printf("Les termes de la contrainte %d sont contenus dans la contrainte %d rapport %e\n",Cnt,Cnt1,Rapport);*/

if ( SensContrainte[Cnt] == '=' ) {
  printf("Amelioration du creux avec une contrainte d'egalite\n");
  /* Ameliore le creux */
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    Var1 = Nuvar[il1];
    if ( T[Var1] != 0 ) A[il1] = 0.0;
    il1++;
  }
  B[Cnt1] -= BCnt / Rapport;
  *Modifications = OUI_PNE;
	return;
}

return;

/* A partir d'ici SensContrainte[Cnt] est < */

/* Si Rapport est positif, alors Cnt peut etre vue comme une contrainte de borne sup sur un groupe de variables */
/* Si Rapport est egati, alors Cnt peut etre vue comme une contrainte de borne inf sur un groupe de variables */

ContrainteDeBorneInf = 1;
ContrainteDeBorneSup = 2;
ContrainteEgal = 3;
if ( Rapport > 0 ) TypeDeContrainteDeBorne = ContrainteDeBorneSup;		
else TypeDeContrainteDeBorne = ContrainteDeBorneInf;				 
ValeurDeLaContraintedeBorne = BCnt / Rapport;

if ( SensContrainte[Cnt] == '=' ) TypeDeContrainteDeBorne = ContrainteEgal;

/* A partir d'ici SensContrainte[Cnt] est < */

/* On regarde si la contrainte Cnt1 devient redondante */
{
  double b; double Smax; double Smin; int NbAFixer;
  b = B[Cnt1];
  Smax = 0;
	Smin = 0;
	NbAFixer = 0;
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    Var1 = Nuvar[il1];
    if ( T[Var1] == 0 ) {
      if ( TypeDeBorne[Var1] == VARIABLE_FIXE ) b -= A[il1] * X[Var1];
      else if ( Xmin[Var1] == Xmax[Var1] ) b -= A[il1] * Xmin[Var1];
		  else {
			  NbAFixer++;
        if ( A[il1] > 0 ) {
          if ( TypeDeBorne[Var1] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var1] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
				    Smax += A[il1] * Xmax[Var1];
				  }
				  else return;
          if ( TypeDeBorne[Var1] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var1] == VARIABLE_BORNEE_INFERIEUREMENT ) {
				    Smin += A[il1] * Xmin[Var1];
				  }
				  else return;					
			  }
			  else if ( A[il1] < 0 ) {
          if ( TypeDeBorne[Var1] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var1] == VARIABLE_BORNEE_INFERIEUREMENT ) {
				    Smax += A[il1] * Xmin[Var1];
				  }
				  else return;
          if ( TypeDeBorne[Var1] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var1] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
				    Smin += A[il1] * Xmax[Var1];
				  }
				  else return;						
			  }
		  }
	  }
    il1++;
  }
	
	b -= ValeurDeLaContraintedeBorne;
	
  if ( SensContrainte[Cnt1] == '<' ) {
	  if ( TypeDeContrainteDeBorne == ContrainteDeBorneInf || TypeDeContrainteDeBorne == ContrainteEgal ) {
      if ( fabs( Smin - b ) < 1.e-7 ) {
        printf("Forcing contrainte %d grace a la contrainte %d  Smin %e  b %e NbAFixer %d\n",Cnt1,Cnt,Smin,b,NbAFixer);
        /* On fixe les variables pour obtenir Smin */
        il1 = Mdeb[Cnt1];
        il1Max = il1 + NbTerm[Cnt1];
        while ( il1 < il1Max ) {
          Var1 = Nuvar[il1];
          if ( T[Var1] == 0 ) {
            if ( TypeDeBorne[Var1] != VARIABLE_FIXE && Xmin[Var1] != Xmax[Var1] ) {
              if ( A[il1] > 0 ) {
							  X[Var1] = Xmin[Var1]; Xmax[Var1] = Xmin[Var1];
							  if ( Pne->TypeDeVariableTrav[Var1] == ENTIER ) {
                  PNE_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Pne, Var1 + Pne->ConflictGraph->Pivot );						
							  }										
							}			   
			        else if ( A[il1] < 0 ) {
							  X[Var1] = Xmax[Var1]; Xmin[Var1] = Xmax[Var1];
							  if ( Pne->TypeDeVariableTrav[Var1] == ENTIER ) {
                  PNE_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Pne, Var1 );						
							  }							
							}
	            TypeDeBorne[Var1] = VARIABLE_FIXE;
	            Pne->TypeDeVariableTrav[Var1] = REEL;
					  }
				  }					  	    
          il1++;
        }
        *Modifications = OUI_PNE;			
		  	return;
		  }
		}
	
	  if ( TypeDeContrainteDeBorne == ContrainteDeBorneSup || TypeDeContrainteDeBorne == ContrainteEgal ) {
      if ( Smax <= b ) {
        printf("Contrainte %d redondante grace a la contrainte %d  Smax %e  b %e  marge %e  B[Cnt1] %e NbAFixer %d \n",Cnt1,Cnt,Smax,b,b-Smax,B[Cnt1],NbAFixer);
        NbCntRedondantes++;
        B[Cnt1] += 100000000.;
		    CntRedondante[Cnt1] = OUI_PNE;	
        *Modifications = OUI_PNE;
		    return;
	    }		
		}
		return;
  }
	else {
	  /* Cnt1 est une contrainte d'egalite */
	  if ( TypeDeContrainteDeBorne == ContrainteDeBorneSup || TypeDeContrainteDeBorne == ContrainteEgal ) {
      if ( fabs( Smax - b ) < 1.e-7 ) {
        printf("Forcing contrainte %d grace a la contrainte %d  Smax %e  b %e B[Cnt1] %e NbAFixer %d SensContrainte[%d] %c\n",Cnt1,Cnt,Smax,b,B[Cnt1],NbAFixer,Cnt1,SensContrainte[Cnt1]);
        /* On fixe les variables pour obtenir Smax */
        il1 = Mdeb[Cnt1];
        il1Max = il1 + NbTerm[Cnt1];
        while ( il1 < il1Max ) {
          Var1 = Nuvar[il1];
				  printf("Var1 %d T %d Xmin %e Xmax %e A %e \n",Var1,T[Var1],Xmin[Var1],Xmax[Var1],A[il1]);
          if ( T[Var1] == 0 ) {
            if ( TypeDeBorne[Var1] != VARIABLE_FIXE && Xmin[Var1] != Xmax[Var1] ) {
              if ( A[il1] > 0 ) {
							  X[Var1] = Xmax[Var1]; Xmin[Var1] = Xmax[Var1];
							  if ( Pne->TypeDeVariableTrav[Var1] == ENTIER ) {
                  PNE_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Pne, Var1 );						
							  }					
						 	}			   
			        else if ( A[il1] < 0 ) {
							  X[Var1] = Xmin[Var1]; Xmax[Var1] = Xmin[Var1];
							  if ( Pne->TypeDeVariableTrav[Var1] == ENTIER ) {
                  PNE_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Pne, Var1 + Pne->ConflictGraph->Pivot );						
							  }								
							}
	            TypeDeBorne[Var1] = VARIABLE_FIXE;
	            Pne->TypeDeVariableTrav[Var1] = REEL;						
					  } 
				  }					  	    
          il1++;
        }
        *Modifications = OUI_PNE;			
			  return;			
		  }
		}
	  if ( TypeDeContrainteDeBorne == ContrainteDeBorneInf || TypeDeContrainteDeBorne == ContrainteEgal ) {
      if ( fabs( Smin - b ) < 1.e-7 ) {
        printf("Forcing contrainte %d grace a la contrainte %d  Smin %e  b %e B[Cnt1] %e NbAFixer %d SensContrainte[%d] %c NbTermesUtilesDeCnt %d\n",
			          Cnt1,Cnt,Smin,b,B[Cnt1],NbAFixer,Cnt1,SensContrainte[Cnt1],NbTermesUtilesDeCnt);			
        /* On fixe les variables pour obtenir Smin */
        il1 = Mdeb[Cnt1];
        il1Max = il1 + NbTerm[Cnt1];
        while ( il1 < il1Max ) {
          Var1 = Nuvar[il1];
				  printf("Var1 %d T %d Xmin %e Xmax %e A %e \n",Var1,T[Var1],Xmin[Var1],Xmax[Var1],A[il1]);
          if ( T[Var1] == 0 ) {
            if ( TypeDeBorne[Var1] != VARIABLE_FIXE && Xmin[Var1] != Xmax[Var1] ) {
              if ( A[il1] > 0 ) {
  	            X[Var1] = Xmin[Var1];
	              Xmax[Var1] = Xmin[Var1];
							  if ( Pne->TypeDeVariableTrav[Var1] == ENTIER ) {
                  PNE_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Pne, Var1 + Pne->ConflictGraph->Pivot );						
							  }										
				      }
			        else if ( A[il1] < 0 ) {
  	            X[Var1] = Xmax[Var1];
	              Xmin[Var1] = Xmax[Var1];
							  if ( Pne->TypeDeVariableTrav[Var1] == ENTIER ) {
                  PNE_ConflictGraphFixerLesNoeudsVoisinsDunNoeud( Pne, Var1 );						
							  }											
						  }
	            TypeDeBorne[Var1] = VARIABLE_FIXE;
	            Pne->TypeDeVariableTrav[Var1] = REEL;
					  }
				  }					  	    
          il1++;
        }
        *Modifications = OUI_PNE;
			
			  return;			
		  }
		}
	}
	
}

return;
}
