/***********************************************************************

   FONCTION: Determination des variables a valeur fractionnaire
                            
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"
							     
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
# include "spx_fonctions.h"

# include "bb_define.h"
# include "bb_fonctions.h"

# define SEUILB 1.e-6

void PNE_ClasserLesVariableFractionnairesSteepestEdge( PROBLEME_PNE * , int , double , double * );
void PNE_EssayerDeFixerDeVariablesEntieres( PROBLEME_PNE * );
void PNE_EssayerDeFixerDeVariablesEntieresFractionnaliteCroissante( PROBLEME_PNE * , char *, int , int * , char * , double * , double , int * , int * , double * );
void PNE_EssayerDeFixerDeVariablesEntieresFractionnaliteDeCroissante( PROBLEME_PNE * , char *, int , int * , char * , double * , double , int * , int * , double * );
void PNE_MettreAJourLesMarges( int , int * , int * , int * , char * , double * , double , double * );

/*----------------------------------------------------------------------------*/

void PNE_DeterminerLesVariablesFractionnaires( PROBLEME_PNE * Pne, int * PositionDeLaVariable )
{
int Var; int LaValeurEstEntiere; double Sigma; double U1; double U2; double * X; double * Xmin; double * Xmax;
double Milieu; double Moyenne; double ValeurDeFractionnaliteNulle; int * LaVariableAUneValeurFractionnaire;
double U; double * SeuilDeFractionnalite; int * TypeDeVariable; int * TypeDeBorne; char SolveurPourLeProblemeRelaxe;
int NombreDeVariables; int * Cdeb; int * Csui; int * NumContrainte; int ic; double * ValeurDeB; double SeuilB;
int NombreDeContraintes; int Cnt; int * Mdeb; int * NbTerm; int * Nuvar; char * SensContrainte; double * A;
double * B; int il; int ilMax; char * ContrainteActivable; char OnAccepte; double NouvelleValeurdeB; 
double * SauvegardeDeX; double b; BB * Bb;

Bb = (BB *) Pne->ProblemeBbDuSolveur;

Pne->CestTermine                           = OUI_PNE;
Pne->VariableLaPlusFractionnaire           = -1;
Milieu                                     = 0.5;
Pne->NombreDeVariablesAValeurFractionnaire = 0;
Pne->NormeDeFractionnalite                 = 0.0; 
Pne->PremFrac                              = -1;

Moyenne = 0.0;
Sigma   = 0.0;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
NombreDeVariables = Pne->NombreDeVariablesTrav;
SolveurPourLeProblemeRelaxe = Pne->SolveurPourLeProblemeRelaxe;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
SeuilDeFractionnalite = Pne->SeuilDeFractionnalite;
X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
LaVariableAUneValeurFractionnaire = Pne->LaVariableAUneValeurFractionnaire;

/* Verification des valeurs entieres et choix de branchement. Pour finioler on peut boucler sur les seules variables entieres
   car il y existe maintenant une liste de ces variables. On peut acceder au type de variable par TypeDeVariable. Ceci dit
	 il n'y a pas grande difference. */
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  LaVariableAUneValeurFractionnaire[Var] = NON_PNE;
	if ( Xmin[Var] == Xmax[Var] ) continue;
  if ( TypeDeVariable[Var] == ENTIER && TypeDeBorne[Var] != VARIABLE_FIXE ) {
    LaValeurEstEntiere = OUI_PNE;
    U = X[Var];		
		if ( U > Xmax[Var] ) U = Xmax[Var];
		else if ( U < Xmin[Var] ) U = Xmin[Var];
		
		ValeurDeFractionnaliteNulle = SeuilDeFractionnalite[Var];		
    if ( SolveurPourLeProblemeRelaxe == SIMPLEXE ) {
      /* Si la variable est basique sa valeur est potentiellement fractionnaire */
      if ( PositionDeLaVariable[Var] == EN_BASE ) {
        /* Ceci permet de resserer la tolerance */
				U1 = U - floor( U );
				U2 = ceil( U ) - U;				
        if( U1 > ValeurDeFractionnaliteNulle && U2 > ValeurDeFractionnaliteNulle ) {				
          /*
	        if ( U1 < U2 ) printf("%d ValeurDeFractionnaliteNulle %e fractionnalite %e  \n",Var,ValeurDeFractionnaliteNulle,U1);
          else printf("%d ValeurDeFractionnaliteNulle %10.17e fractionnalite %10.17e  \n",Var,ValeurDeFractionnaliteNulle,U2);
				  */
          Pne->CestTermine   = NON_PNE;
          LaValeurEstEntiere = NON_PNE;
          LaVariableAUneValeurFractionnaire[Var] = OUI_PNE;
          Pne->NombreDeVariablesAValeurFractionnaire++;
					
	        if ( U1 < U2 ) { Pne->NormeDeFractionnalite+= U1; Sigma+= U1 * U1; }
	        else { Pne->NormeDeFractionnalite+= U2; Sigma+= U2 * U2;}
					
          /* Classement de la variable en fonction de sa position par rapport au milieu */	  	  
          PNE_ClasserLesVariableFractionnaires( Pne, Var, Milieu );					
					
        }
      }      
    }
    else if( fabs( U - floor( U ) ) > ValeurDeFractionnaliteNulle && fabs( U - ceil ( U ) ) > ValeurDeFractionnaliteNulle ) {      
			Pne->CestTermine = NON_PNE;
      LaValeurEstEntiere = NON_PNE;
      LaVariableAUneValeurFractionnaire[Var] = OUI_PNE;
      Pne->NombreDeVariablesAValeurFractionnaire++;
      PNE_ClasserLesVariableFractionnaires( Pne, Var, Milieu );			
		}
  }
}

Pne->VariableLaPlusFractionnaire = Pne->PremFrac;

#if VERBOSE_PNE
  printf(" Nombre de variables a valeur fractionnaire: %d \n",Pne->NombreDeVariablesAValeurFractionnaire); fflush(stdout);
#endif
 
/*
if ( Pne->PremFrac >= 0 ) printf(" Profondeur %d nombre de variables a valeur fractionnaire: %d %e Critere %e\n",
                                   Bb->NoeudEnExamen->ProfondeurDuNoeud,Pne->NombreDeVariablesAValeurFractionnaire,Pne->UTrav[Pne->PremFrac],Pne->Critere);
*/

if (  Pne->NombreDeVariablesAValeurFractionnaire > 0 ) {
  PNE_EssayerDeFixerDeVariablesEntieres( Pne );
}

/* Si on a trouve une solution entiere, on place les valeurs optimales des variables entieres sur la bonne borne
   pour ne pas avoir de probleme par la suite */
if ( Pne->NombreDeVariablesAValeurFractionnaire == 0 && Pne->YaDesVariablesEntieres == OUI_PNE ) {

  ValeurDeB = NULL;
  SauvegardeDeX = NULL;
	
  ValeurDeB = (double *) malloc( NombreDeContraintes * sizeof( double ) );
	if ( ValeurDeB == NULL ) goto Fin;

  SauvegardeDeX = (double *) malloc( NombreDeVariables * sizeof( double ) );
	if ( SauvegardeDeX == NULL ) goto Fin;

	memcpy( (char *) SauvegardeDeX, (char *) X , NombreDeVariables * sizeof( double ) );
	
	/*SeuilB = 10 * SEUIL_DADMISSIBILITE;*/
  /* Calcul du membre de gauche */
	ContrainteActivable = Pne->ContrainteActivable;
  Mdeb = Pne->MdebTrav;
  NbTerm = Pne->NbTermTrav;
  A = Pne->ATrav;
  Nuvar = Pne->NuvarTrav;
  SensContrainte = Pne->SensContrainteTrav;
  B = Pne->BTrav;	
  for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
	  if ( ContrainteActivable[Cnt] == NON_PNE ) continue;
    il = Mdeb[Cnt];
    ilMax = il + NbTerm[Cnt];
    U = 0.;  
    while ( il < ilMax ) {
		  U += A[il] * X[Nuvar[il]];
			il++;
		}
    ValeurDeB[Cnt] = U;
  }
	
  Cdeb = Pne->CdebTrav;
	Csui = Pne->CsuiTrav;
  NumContrainte = Pne->NumContrainteTrav;
  for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
    if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
    if ( TypeDeVariable[Var] != ENTIER ) continue;
    U = X[Var];

//	if ( LaVariableAUneValeurFractionnaire[Var] == OUI_PNE ) {
		/* on ne doit pas remettre en cause les variables dont la valeur n'a pas ete
	    decidee comme fractionnaire a l'etape de detection (correction du 21/12/2017) */	
		if ( U > Xmax[Var] ) U = Xmax[Var];
		else if ( U < Xmin[Var] ) U = Xmin[Var];		
		U1 = U - floor( U );
		U2 = ceil( U ) - U;						
		if ( U1 < U2 ) U = floor( U );
		else U = ceil( U );
//	}

	OnAccepte = OUI_PNE;	 
    if ( SolveurPourLeProblemeRelaxe == SIMPLEXE ) {
      if ( PositionDeLaVariable[Var] != EN_BASE ) goto ValeurEntiereAcceptee;
	}
		
    /* Avant de dire que la variable est entiere, on verifie que si on la place sur la borne,
		   elle n'entraine pas de violation de contrainte */
		ic = Cdeb[Var];
		while ( ic >= 0 ) {
		  Cnt = NumContrainte[ic];
	    if ( ContrainteActivable[Cnt] == OUI_PNE ) {
        NouvelleValeurdeB = ValeurDeB[Cnt] - ( A[ic] * X[Var] ) + ( A[ic] * U );

				b = fabs( B[Cnt] );
        if ( b < 1 ) SeuilB = 5 * SEUILB;
				else if ( b < 1.e+1 ) SeuilB = 1.e+1 * SEUILB;
				else if ( b < 1.e+2 ) SeuilB = 1.e+2 * SEUILB;
				else if ( b < 1.e+3 ) SeuilB = 1.e+3 * SEUILB;
				else if ( b < 1.e+4 ) SeuilB = 1.e+4 * SEUILB;
				else if ( b < 1.e+5 ) SeuilB = 1.e+5 * SEUILB;
				else if ( b < 1.e+6 ) SeuilB = 1.e+6 * SEUILB;
				else if ( b < 1.e+7 ) SeuilB = 1.e+7 * SEUILB;
        else SeuilB = 1.e+7 * SEUILB;
				
        if ( SensContrainte[Cnt] == '=' ) {
					if ( fabs( NouvelleValeurdeB - B[Cnt] ) > SeuilB ) {            						
            /* On n'accepte pas */						
						/*
						printf("   On refuse Cnt type = num %d NouvelleValeurdeB %e B %e   Valeur de la variable binaire X[%d] = %e\n",Cnt,NouvelleValeurdeB,B[Cnt],Var,X[Var]);	 	        
						*/
						OnAccepte = NON_PNE;
						break;
					}
				}
				else {
				  /* La contrainte est forcement <= */
					if ( NouvelleValeurdeB > B[Cnt] + SeuilB ) {
						/*
						printf("   On refuse Cnt type < num %d NouvelleValeurdeB %e B %e   Valeur de la variable binaire X[%d] = %e\n",Cnt,NouvelleValeurdeB,B[Cnt],Var,X[Var]);
						*/
						/* On n'accepte pas */
	 	        OnAccepte = NON_PNE;
						break;						
					}
				}				
			}

		  ic = Csui[ic];
		}
		ValeurEntiereAcceptee:
		if ( OnAccepte == OUI_PNE ) X[Var] = U;
		else {
      Pne->CestTermine   = NON_PNE;
      LaValeurEstEntiere = NON_PNE;
      LaVariableAUneValeurFractionnaire[Var] = OUI_PNE;
      Pne->NombreDeVariablesAValeurFractionnaire++;					
	    if ( U1 < U2 ) { Pne->NormeDeFractionnalite += U1; Sigma += U1 * U1; }
	    else { Pne->NormeDeFractionnalite += U2; Sigma += U2 * U2;}					
      /* Classement de la variable en fonction de sa position par rapport au milieu */	  	  
      PNE_ClasserLesVariableFractionnaires( Pne, Var, Milieu );		
		}
  }
	Fin:
	
	/* Si on a refuse la solution entiere, on reprend les valeurs initiales de X */
  if ( Pne->NombreDeVariablesAValeurFractionnaire != 0 ) {
	  memcpy( (char *) X, (char *) SauvegardeDeX, NombreDeVariables * sizeof( double ) );
  }

	free( ValeurDeB );
	free( SauvegardeDeX );	
	
	Pne->VariableLaPlusFractionnaire = Pne->PremFrac;
	/* Certaines variables ont pu etre ajustees */
	PNE_CalculerLaValeurDuCritere( Pne );
}					        

Pne->FaireDuStrongBranching = OUI_PNE;
 
return;
}

/*----------------------------------------------------------------------------*/
/*                Classe les variables entieres en fonction 
                  de leur valeur fractionnaire                                */

void PNE_ClasserLesVariableFractionnaires( PROBLEME_PNE * Pne, int i , double Milieu )
{
int ik; int ikPrec;

if ( Pne->PremFrac == -1 ) { /* C'est la premiere variable */
  Pne->PremFrac    =  i;
  Pne->SuivFrac[i] = -1;
  return;
}

/* C'est pas la premiere variable: on lui cherche un emplacement */
ik = Pne->PremFrac;
if ( fabs( Pne->UTrav[i ] - ( Milieu * Pne->UmaxTrav[i ] ) ) < 
     fabs( Pne->UTrav[ik] - ( Milieu * Pne->UmaxTrav[ik] ) ) ) {
  Pne->PremFrac    = i;
  Pne->SuivFrac[i] = ik; 
  return; 
}

/* C'est pas le meilleur */
ikPrec = ik;  
ik     = Pne->SuivFrac[ik];
while ( ik >= 0 ) {
  if ( fabs( Pne->UTrav[i ] - ( Milieu * Pne->UmaxTrav[i ] ) ) < 
       fabs( Pne->UTrav[ik] - ( Milieu * Pne->UmaxTrav[ik] ) ) ) {
    /* Emplacement trouve */
    Pne->SuivFrac[ikPrec] = i;
    Pne->SuivFrac[i]      = ik;
    return; 
  }
  ikPrec = ik;
  ik     = Pne->SuivFrac[ik];
}

/* C'est la plus mauvaise: classement de la variable a la fin de la liste */
Pne->SuivFrac[ikPrec] =  i;
Pne->SuivFrac[i]      = -1;

return;
}

/*----------------------------------------------------------------------------*/
/*                Classe les variables entieres en fonction                   */
/*                de leur valeur fractionnaire ponderee par                   */
/*								le steepest edge du simplexe                                */

void PNE_ClasserLesVariableFractionnairesSteepestEdge( PROBLEME_PNE * Pne, int Var,
                                                      double Frac,
                                                      double * Fractionnalite )
{
int ik; int ikPrec;

Fractionnalite[Var] = Frac;
if ( Pne->PremFrac == -1 ) { /* C'est la premiere variable */
  Pne->PremFrac    =  Var;
  Pne->SuivFrac[Var] = -1;
  return;
}

/* C'est pas la premiere variable: on lui cherche un emplacement */
ik = Pne->PremFrac;
if ( Frac > Fractionnalite[ik] ) {
  Pne->PremFrac    = Var;
  Pne->SuivFrac[Var] = ik;	
  return; 
}

/* C'est pas le meilleur */
ikPrec = ik;  
ik     = Pne->SuivFrac[ik];
while ( ik >= 0 ) {
  if ( Frac >  Fractionnalite[ik] ) {
    /* Emplacement trouve */
    Pne->SuivFrac[ikPrec] = Var;
    Pne->SuivFrac[Var] = ik;
    return; 
  }
  ikPrec = ik;
  ik = Pne->SuivFrac[ik];
}

/* C'est la plus mauvaise: classement de la variable a la fin de la liste */
Pne->SuivFrac[ikPrec] =  Var;
Pne->SuivFrac[Var] = -1;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_EssayerDeFixerDeVariablesEntieres( PROBLEME_PNE * Pne )
{
int * SuivFrac; int NombreDeVariablesAValeurFractionnaire; int Cnt; int il; int NbFrac; double * Xmin; double * Xmax;
int * NumeroDeVariableFractionnaire; int NombreDeVariables; int i; int Var; int * Cdeb; int ilMax; double Smax;
int * Csui; int * NumeroDeContrainte; char * ContrainteActivable; char * SensContrainte; double S;
int * Mdeb; int * NbTerm; double * B; double * A; int * Nuvar; int VarBin; int ic; int NombreDeContraintes;
double * CoutLineaire; double * X; double * MargeDisponible; double MargeLimite; int DerniereVarBin;
int * LaVariableAUneValeurFractionnaire;  char VariablesEntieresACoutNul; char * VariableEntiereDansContrainteEgalite;
double * MargeDisponibleAuxiliaire; int NombreDeVariablesFixeesSiFractionnaliteDecroissante; int NombreDeVariablesFixeesSiFractionnaliteCroissante;
int * VariablesFixeesSiFractionnaliteDecroissante; double * ValeurDeFixationSiFractionnaliteDecroissante;
int * VariablesFixeesSiFractionnaliteCroissante; double * ValeurDeFixationSiFractionnaliteCroissante;
char NbFracResiduel;

MargeLimite = -SEUIL_DADMISSIBILITE;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

/* Les variables sont classes dans l'ordre decroissant de fractionnalite on les classe dans l'autre sens */
NombreDeVariablesAValeurFractionnaire = Pne->NombreDeVariablesAValeurFractionnaire;

NumeroDeVariableFractionnaire = NULL;
MargeDisponible = NULL;
MargeDisponibleAuxiliaire = NULL;
VariableEntiereDansContrainteEgalite = NULL;

VariablesFixeesSiFractionnaliteDecroissante = NULL;
ValeurDeFixationSiFractionnaliteDecroissante = NULL;
VariablesFixeesSiFractionnaliteCroissante = NULL;
ValeurDeFixationSiFractionnaliteCroissante = NULL;

ContrainteActivable = NULL;

NumeroDeVariableFractionnaire = (int *) malloc( NombreDeVariablesAValeurFractionnaire * sizeof( int ) );
if ( NumeroDeVariableFractionnaire == NULL ) goto Fin;
MargeDisponible = (double *) malloc( NombreDeContraintes * sizeof( double ) );
if ( MargeDisponible == NULL ) goto Fin;
MargeDisponibleAuxiliaire = (double *) malloc( NombreDeContraintes * sizeof( double ) );
if ( MargeDisponibleAuxiliaire == NULL ) goto Fin;

VariablesFixeesSiFractionnaliteDecroissante = (int *) malloc( NombreDeVariablesAValeurFractionnaire * sizeof( int ) );
if ( VariablesFixeesSiFractionnaliteDecroissante == NULL ) goto Fin;
ValeurDeFixationSiFractionnaliteDecroissante = (double *) malloc( NombreDeVariablesAValeurFractionnaire * sizeof( double ) );
if ( ValeurDeFixationSiFractionnaliteDecroissante == NULL ) goto Fin;
VariablesFixeesSiFractionnaliteCroissante = (int *) malloc( NombreDeVariablesAValeurFractionnaire * sizeof( int ) );
if ( VariablesFixeesSiFractionnaliteCroissante == NULL ) goto Fin;
ValeurDeFixationSiFractionnaliteCroissante = (double *) malloc( NombreDeVariablesAValeurFractionnaire * sizeof( double ) );
if ( ValeurDeFixationSiFractionnaliteCroissante == NULL ) goto Fin;

ContrainteActivable = (char *) malloc( NombreDeContraintes * sizeof( char ) );
if ( ContrainteActivable == NULL ) goto Fin;

VariableEntiereDansContrainteEgalite = (char *) malloc( NombreDeVariablesAValeurFractionnaire * sizeof( char ) );
if ( VariableEntiereDansContrainteEgalite == NULL ) goto Fin;

CoutLineaire = Pne->LTrav;
X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;

# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

LaVariableAUneValeurFractionnaire = Pne->LaVariableAUneValeurFractionnaire;
SuivFrac = Pne->SuivFrac;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumeroDeContrainte = Pne->NumContrainteTrav;
SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

VariablesEntieresACoutNul = NON_PNE;
i = NombreDeVariablesAValeurFractionnaire - 1;
Var = Pne->PremFrac;
while ( Var >= 0 ) {
  if ( CoutLineaire[Var] == 0 ) VariablesEntieresACoutNul = OUI_PNE;
  NumeroDeVariableFractionnaire[i] = Var;
	i--;
  Var = SuivFrac[Var];
}
if ( VariablesEntieresACoutNul == NON_PNE ) goto Fin;

/* Calcul de la marge disponible par contrainte d'inegalite */

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  MargeDisponible[Cnt] = 0;
	ContrainteActivable[Cnt] = OUI_PNE;
	if ( SensContrainte[Cnt] == '=' ) continue;
  il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];
	S = 0;
	Smax = 0;
	while ( il < ilMax ) {
    S += A[il] * X[Nuvar[il]];
		if ( A[il] > 0 ) Smax += A[il] * Pne->UmaxTravSv[Nuvar[il]];
		else Smax += A[il] * Pne->UminTravSv[Nuvar[il]];
	  il++;
	}
	MargeDisponible[Cnt] = B[Cnt] - S;
	if ( Smax < B[Cnt] - MargeLimite ) { /* Car MargeLimite est negatif */
	  ContrainteActivable[Cnt] = NON_PNE;
	}
}

for ( i = 0 ; i < NombreDeVariablesAValeurFractionnaire ; i++ ) {
  VarBin = NumeroDeVariableFractionnaire[i];
	VariableEntiereDansContrainteEgalite[i] = NON_PNE;
  ic = Cdeb[VarBin];
  while ( ic >= 0 ) {
    Cnt = NumeroDeContrainte[ic];
	  if ( ContrainteActivable[Cnt] == OUI_PNE ) {
	    if ( SensContrainte[Cnt] == '=' ) {
			  VariableEntiereDansContrainteEgalite[i] = OUI_PNE;
				break;
			} 
	  }
	  ic = Csui[ic];
	}
}

memcpy( (char *) MargeDisponibleAuxiliaire, (char *) MargeDisponible, NombreDeContraintes * sizeof( double ) );

NombreDeVariablesFixeesSiFractionnaliteCroissante = 0;
PNE_EssayerDeFixerDeVariablesEntieresFractionnaliteCroissante( Pne, ContrainteActivable, NombreDeVariablesAValeurFractionnaire, NumeroDeVariableFractionnaire,
                                                               VariableEntiereDansContrainteEgalite, MargeDisponible, MargeLimite,
																															 &NombreDeVariablesFixeesSiFractionnaliteCroissante,
																															 VariablesFixeesSiFractionnaliteCroissante, ValeurDeFixationSiFractionnaliteCroissante );
																														 
NombreDeVariablesFixeesSiFractionnaliteDecroissante = 0;
PNE_EssayerDeFixerDeVariablesEntieresFractionnaliteDeCroissante( Pne, ContrainteActivable, NombreDeVariablesAValeurFractionnaire, NumeroDeVariableFractionnaire,
                                                                 VariableEntiereDansContrainteEgalite, MargeDisponibleAuxiliaire, MargeLimite,
								 	 																			 				 &NombreDeVariablesFixeesSiFractionnaliteDecroissante,
																																 VariablesFixeesSiFractionnaliteDecroissante, ValeurDeFixationSiFractionnaliteDecroissante );

if ( NombreDeVariablesFixeesSiFractionnaliteCroissante >= NombreDeVariablesFixeesSiFractionnaliteDecroissante ) {	
  /* On prend le resultat de la fractionnalite croissante */
	NbFracResiduel = NombreDeVariablesAValeurFractionnaire - NombreDeVariablesFixeesSiFractionnaliteCroissante;
  for ( i = 0 ; i < NombreDeVariablesFixeesSiFractionnaliteCroissante ; i++ ) {
    VarBin = VariablesFixeesSiFractionnaliteCroissante[i];
		if ( NbFracResiduel == 0 ) {
		  X[VarBin] = ValeurDeFixationSiFractionnaliteCroissante[i];
		}
    LaVariableAUneValeurFractionnaire[VarBin] = NON_PNE;
	}
}
else {
  /* On prend le resultat de la fractionnalite decroissante */
	NbFracResiduel = NombreDeVariablesAValeurFractionnaire - NombreDeVariablesFixeesSiFractionnaliteDecroissante;
  for ( i = 0 ; i < NombreDeVariablesFixeesSiFractionnaliteDecroissante ; i++ ) {
    VarBin = VariablesFixeesSiFractionnaliteDecroissante[i];
		if ( NbFracResiduel == 0 ) {
		  X[VarBin] = ValeurDeFixationSiFractionnaliteDecroissante[i];
		}
    LaVariableAUneValeurFractionnaire[VarBin] = NON_PNE;		
	}	
}
																																 
Pne->PremFrac = -1;
NbFrac = 0;
DerniereVarBin = -1;
/* On reclasse */
for ( i = NombreDeVariablesAValeurFractionnaire - 1 ; i >= 0 ; i-- ) {
  VarBin = NumeroDeVariableFractionnaire[i];
  if ( LaVariableAUneValeurFractionnaire[VarBin] == NON_PNE ) {
	  continue;
	}
  NbFrac++;
	if ( Pne->PremFrac < 0 ) {
	  Pne->PremFrac = VarBin;
	}
	else {
		SuivFrac[DerniereVarBin] = VarBin;
	}
	DerniereVarBin = VarBin;
	SuivFrac[DerniereVarBin] = -1;	
}

/*printf("NombreDeVariablesAValeurFractionnaire %d -> %d\n",NombreDeVariablesAValeurFractionnaire,NombreDeVariablesAValeurFractionnaire-NbFix);*/

Pne->NombreDeVariablesAValeurFractionnaire = NbFrac;
Pne->VariableLaPlusFractionnaire = Pne->PremFrac;

if( Pne->NombreDeVariablesAValeurFractionnaire == 0 ) Pne->CestTermine = OUI_PNE;	

Fin:

free( NumeroDeVariableFractionnaire );
free( MargeDisponible );
free( MargeDisponibleAuxiliaire );
free( VariableEntiereDansContrainteEgalite );
free( VariablesFixeesSiFractionnaliteDecroissante );
free( ValeurDeFixationSiFractionnaliteDecroissante );
free( VariablesFixeesSiFractionnaliteCroissante );
free( ValeurDeFixationSiFractionnaliteCroissante );
free( ContrainteActivable );

return;
}

/*----------------------------------------------------------------------------*/

void PNE_MettreAJourLesMarges( int VarBin, int * Cdeb, int * Csui, int * NumeroDeContrainte, char * ContrainteActivable,
                               double * A, double Delta, double * MargeDisponible )
{
int ic; int Cnt;
ic = Cdeb[VarBin];
while ( ic >= 0 ) {
  Cnt = NumeroDeContrainte[ic];
  if ( ContrainteActivable[Cnt] == OUI_PNE ) {
    MargeDisponible[Cnt] = MargeDisponible[Cnt] + ( A[ic] * Delta );
  }
  ic = Csui[ic];
}
return;
}

/*----------------------------------------------------------------------------*/

void PNE_EssayerDeFixerDeVariablesEntieresFractionnaliteCroissante( PROBLEME_PNE * Pne, char * ContrainteActivable, int NombreDeVariablesAValeurFractionnaire,
                                                                    int * NumeroDeVariableFractionnaire, char * VariableEntiereDansContrainteEgalite,
																																		double * MargeDisponible, double MargeLimite, int * NombreDeVariablesFixees,
																																		int * VariablesFixees, double * ValeurDeFixation )
{
int i; int VarBin; double * CoutLineaire; double ValeurTest0; double ValeurTest1; double MargeMaximale0;
double MargeMaximale1; double Delta0; double Delta1; int Cnt; int ic; double NouvelleMarge; char Valeur0Possible;
char Valeur1Possible; double * X; int * Cdeb; int * Csui; int * NumeroDeContrainte; char * SensContrainte;
int * NbTerm; int * Mdeb; double * B; double * A; int * Nuvar; int NbFix; BB * Bb;

Bb = (BB *) Pne->ProblemeBbDuSolveur;

CoutLineaire = Pne->LTrav;
X = Pne->UTrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumeroDeContrainte = Pne->NumContrainteTrav;

SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

NbFix = *NombreDeVariablesFixees;

for ( i = 0 ; i < NombreDeVariablesAValeurFractionnaire ; i++ ) {
  VarBin = NumeroDeVariableFractionnaire[i];
	if ( VarBin < 0 ) continue;	
  if ( CoutLineaire[VarBin] != 0 ) continue; 	
	if ( VariableEntiereDansContrainteEgalite[i] == OUI_PNE ) continue;
	/* Test a 0 */
	ValeurTest0 = 0;
	ValeurTest1 = 1;
  Valeur0Possible = OUI_PNE;
	Valeur1Possible = OUI_PNE;
	MargeMaximale0 = -1;
	MargeMaximale1 = -1;
	Delta0 = X[VarBin] - ValeurTest0;
	Delta1 = X[VarBin] - ValeurTest1;
  ic = Cdeb[VarBin];
	while ( ic >= 0 ) {	
	  if ( Valeur0Possible == NON_PNE && Valeur1Possible == NON_PNE ) break;
    Cnt = NumeroDeContrainte[ic];
		if ( ContrainteActivable[Cnt] == NON_PNE ) goto NextIc;		
		NouvelleMarge = MargeDisponible[Cnt] + ( A[ic] * Delta0 );
		if ( NouvelleMarge < MargeLimite ) Valeur0Possible = NON_PNE;
		else if ( NouvelleMarge > MargeMaximale0 ) MargeMaximale0 = NouvelleMarge;
		NouvelleMarge = MargeDisponible[Cnt] + ( A[ic] * Delta1 );
		if ( NouvelleMarge < MargeLimite ) Valeur1Possible = NON_PNE;
		else if ( NouvelleMarge > MargeMaximale1 ) MargeMaximale1 = NouvelleMarge;
		NextIc:
	  ic = Csui[ic];
	}
	if ( Valeur0Possible == OUI_PNE && Valeur1Possible == OUI_PNE ) {
    if ( MargeMaximale0 > MargeMaximale1 ) Valeur1Possible = NON_PNE; /* Pour fixer la variable a 0 */
		else Valeur0Possible = NON_PNE; 
	}
	if ( Valeur0Possible == OUI_PNE ) {	
		Delta0 = X[VarBin] - ValeurTest0;
    VariablesFixees[NbFix] = VarBin;
    ValeurDeFixation[NbFix] = ValeurTest0;
	  NbFix++;		
    PNE_MettreAJourLesMarges( VarBin, Cdeb, Csui, NumeroDeContrainte, ContrainteActivable, A, Delta0, MargeDisponible );			 				
	}
	else if ( Valeur1Possible == OUI_PNE ) {
		Delta1 = X[VarBin] - ValeurTest1;
    VariablesFixees[NbFix] = VarBin;
    ValeurDeFixation[NbFix] = ValeurTest1;
	  NbFix++;			
    PNE_MettreAJourLesMarges( VarBin, Cdeb, Csui, NumeroDeContrainte, ContrainteActivable, A, Delta1, MargeDisponible );				 		
	}	
}
*NombreDeVariablesFixees = NbFix;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_EssayerDeFixerDeVariablesEntieresFractionnaliteDeCroissante( PROBLEME_PNE * Pne, char * ContrainteActivable, int NombreDeVariablesAValeurFractionnaire, int * NumeroDeVariableFractionnaire,
                                                                      char * VariableEntiereDansContrainteEgalite, double * MargeDisponible, double MargeLimite,
																																	  	int * NombreDeVariablesFixees, int * VariablesFixees, double * ValeurDeFixation )
{
int i; int VarBin; double * CoutLineaire; double ValeurTest0; double ValeurTest1; double MargeMaximale0;
double MargeMaximale1; double Delta0; double Delta1; int Cnt; int ic; double NouvelleMarge; char Valeur0Possible;
char Valeur1Possible; double * X; int * Cdeb; int * Csui; int * NumeroDeContrainte; 
char * SensContrainte; int * NbTerm; int * Mdeb; double * B; double * A; int * Nuvar; int NbFix;

CoutLineaire = Pne->LTrav;
X = Pne->UTrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumeroDeContrainte = Pne->NumContrainteTrav;

SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

NbFix = *NombreDeVariablesFixees;

for ( i = NombreDeVariablesAValeurFractionnaire - 1 ; i >= 0  ; i-- ) {
  VarBin = NumeroDeVariableFractionnaire[i];
	if ( VarBin < 0 ) continue;
  if ( CoutLineaire[VarBin] != 0 ) continue; 	
	if ( VariableEntiereDansContrainteEgalite[i] == OUI_PNE ) continue;
	/* Test a 0 */
	ValeurTest0 = 0;
	ValeurTest1 = 1;
  Valeur0Possible = OUI_PNE;
	Valeur1Possible = OUI_PNE;
	MargeMaximale0 = -1;
	MargeMaximale1 = -1;
	Delta0 = X[VarBin] - ValeurTest0;
	Delta1 = X[VarBin] - ValeurTest1;
  ic = Cdeb[VarBin];
	while ( ic >= 0 ) {	
	  if ( Valeur0Possible == NON_PNE && Valeur1Possible == NON_PNE ) break;
    Cnt = NumeroDeContrainte[ic];
		if ( ContrainteActivable[Cnt] == NON_PNE ) goto NextIc;		
		NouvelleMarge = MargeDisponible[Cnt] + ( A[ic] * Delta0 );
		if ( NouvelleMarge < MargeLimite ) Valeur0Possible = NON_PNE;
		else if ( NouvelleMarge > MargeMaximale0 ) MargeMaximale0 = NouvelleMarge;
		NouvelleMarge = MargeDisponible[Cnt] + ( A[ic] * Delta1 );
		if ( NouvelleMarge < MargeLimite ) Valeur1Possible = NON_PNE;
		else if ( NouvelleMarge > MargeMaximale1 ) MargeMaximale1 = NouvelleMarge;
		NextIc:
	  ic = Csui[ic];
	}
	if ( Valeur0Possible == OUI_PNE && Valeur1Possible == OUI_PNE ) {
    if ( MargeMaximale0 > MargeMaximale1 ) Valeur1Possible = NON_PNE; /* Pour fixer la variable a 0 */
		else Valeur0Possible = NON_PNE; 
	}
	if ( Valeur0Possible == OUI_PNE ) {	
		Delta0 = X[VarBin] - ValeurTest0;
    VariablesFixees[NbFix] = VarBin;
    ValeurDeFixation[NbFix] = ValeurTest0;
	  NbFix++;			
    PNE_MettreAJourLesMarges( VarBin, Cdeb, Csui, NumeroDeContrainte, ContrainteActivable, A, Delta0, MargeDisponible );			 				
	}
	else if ( Valeur1Possible == OUI_PNE ) {
		Delta1 = X[VarBin] - ValeurTest1;
    VariablesFixees[NbFix] = VarBin;
    ValeurDeFixation[NbFix] = ValeurTest1;
	  NbFix++;		
    PNE_MettreAJourLesMarges( VarBin, Cdeb, Csui, NumeroDeContrainte, ContrainteActivable, A, Delta1, MargeDisponible );				 		
	}	
}
*NombreDeVariablesFixees = NbFix;

return;
}
