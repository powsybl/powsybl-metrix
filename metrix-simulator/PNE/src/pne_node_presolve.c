/***********************************************************************

   FONCTION: On etudie les domaines de variation des variables entieres
	           dans le but d'en fixer.
                
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

# define TRACES 0

# define MARGE_POUR_RELAXATION 1.e-8 

/*----------------------------------------------------------------------------*/

void PNE_NodePresolve( PROBLEME_PNE * Pne, int * Faisabilite )   
{
int Var; double * Umin; double * Umax; int NombreDeVariables; double Ai; int * TypeDeBorne;
int il; double * U; int NombreDeContraintes; double * UminSv; double * UmaxSv; int * TypeDeBorneSv;
char * BorneInfConnue; double * ValeurDeBorneInf; double * Bmin; double * Bmax; int * NumeroDesVariablesFixees;
int i; char CodeRet; BB * Bb; NOEUD * Noeud; char * T; int NombreDeBornesModifiees; char CntActiv;
int * NumeroDeLaVariableModifiee; char Mode; char * TypeDeBorneModifiee; double * NouvelleValeurDeBorne;
CONFLICT_GRAPH * ConflictGraph; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; char * ContrainteActivable; 
double * ValeurDeBorneSup; char * BorneSupConnue; int * TypeDeVariable; int * NumeroDesVariablesNonFixes;
double * ValeurDeUminARestaurer; double Marge; char * BminValide; char * BmaxValide; char * SensContrainte; double * B; int Cnt;
# if UTILISER_UMIN_AMELIORE == OUI_PNE
  double * UminAmeliore;
# endif
# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
  double * UminAmeliorePourCoupes;
# endif

Bb = Pne->ProblemeBbDuSolveur; 
Noeud = Bb->NoeudEnExamen;

if ( Noeud->ProfondeurDuNoeud < PROFONDEUR_MIN_POUR_NODE_PRESOLVE && Noeud->ProfondeurDuNoeud > 1 ) return;

if ( Noeud->ProfondeurDuNoeud % CYCLE_NODE_PRESOLVE != 0 ) return;
      
/*printf("PNE_NodePresolve \n");*/

/* Dans la suite: node presolve simplifie */

ConflictGraph = Pne->ConflictGraph;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
NombreDeVariables = Pne->NombreDeVariablesTrav;
Umin = Pne->UminTrav;
Umax = Pne->UmaxTrav;
U = Pne->UTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;

# if UTILISER_UMIN_AMELIORE == OUI_PNE
  UminAmeliore = Pne->UminAmeliore;
# endif

# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
  UminAmeliorePourCoupes = Pne->UminAmeliorePourCoupes;
# endif

NumeroDesVariablesNonFixes = Pne->NumeroDesVariablesNonFixes;

Mode = PRESOLVE_SIMPLIFIE_POUR_NODE_PRESOLVE;

if ( Pne->ProbingOuNodePresolve == NULL ) {
  PNE_ProbingNodePresolveAlloc( Pne, &CodeRet );
  if ( CodeRet == NON_PNE ) return;
}

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;

ProbingOuNodePresolve->Faisabilite = OUI_PNE;
ProbingOuNodePresolve->VariableInstanciee = -1;
ProbingOuNodePresolve->NbVariablesModifiees = 0;
ProbingOuNodePresolve->NbContraintesModifiees = 0;

BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;
BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;

Bmin = ProbingOuNodePresolve->Bmin;
Bmax = ProbingOuNodePresolve->Bmax;

NumeroDesVariablesFixees = ProbingOuNodePresolve->NumeroDesVariablesFixees;
ProbingOuNodePresolve->NombreDeVariablesFixees = 0;

T = NULL;
ContrainteActivable = NULL;

ContrainteActivable = (char *) malloc( NombreDeContraintes * sizeof( char ) );
if ( ContrainteActivable == NULL ) return;
memcpy( (char *) ContrainteActivable, (char *) Pne->ContrainteActivable, NombreDeContraintes * sizeof( char ) );

ValeurDeUminARestaurer = (double *) malloc( NombreDeVariables * sizeof( double ) );
if ( ValeurDeUminARestaurer == NULL ) {
  free( ContrainteActivable );
  return;
}
memcpy( (char *) ValeurDeUminARestaurer, (char *) Umin, NombreDeVariables * sizeof( double ) );

# if UTILISER_UMIN_AMELIORE == OUI_PNE
  for ( Var = 0 ; Var < NombreDeVariables; Var++ ) {
    if ( UminAmeliore[Var] != VALEUR_NON_INITIALISEE ) Umin[Var] = UminAmeliore[Var];
  }
# endif

PNE_InitBorneInfBorneSupDesVariables( Pne );
																	
/* Calcul min et max des contraintes */
if ( Bb->NoeudEnExamen == Bb->NoeudRacine ) {
  /* Au noeud racine on part "from scratch" et on archive le resultat */
  PNE_CalculMinEtMaxDesContraintes( Pne, Faisabilite );
  if ( *Faisabilite == NON_PNE ) {	
 	  # if TRACES == 1
		  printf("Pas de solution dans le presolve apres CalculMinEtMaxDesContraintes\n");
	  # endif 
    goto Fin;
  }	
	/* Et on sauvegarde le resultat comme point de depart pour les noeuds suivants */
  memcpy( (char *) ProbingOuNodePresolve->BminSv, (char *) ProbingOuNodePresolve->Bmin, NombreDeContraintes * sizeof( double ) );
  memcpy( (char *) ProbingOuNodePresolve->BmaxSv, (char *) ProbingOuNodePresolve->Bmax, NombreDeContraintes * sizeof( double ) );	
}
else {
  /* Aux autres noeuds, on part des valeurs au noeud racine et on tient compte des instanciations
     et des nouvelles bornes. Attention: peut modifier Umin et Umax de certaines variables entieres */
  PNE_NodePresolveInitBornesDesContraintes( Pne, Faisabilite );
  if ( *Faisabilite == NON_PNE ) {	
 	  # if TRACES == 1
		  printf("Pas de solution dans le presolve apres NodePresolveInitBornesDesContraintes\n");
	  # endif 
    goto Fin;
  }	
}

PNE_PresolveSimplifie( Pne, ContrainteActivable, Mode, Faisabilite );

Fin:

if ( *Faisabilite == NON_PNE ) {
  # if TRACES == 1
    printf("Pas de solution dans le node presolve\n");
  # endif	
  goto FreeMemoire;
}

if ( Bb->NoeudEnExamen == Bb->NoeudRacine ) goto ModifsBornesAuNoeudRacine;

/* Il faut restaurer les valeurs de Umin des variables non entieres au cas ou elles auraient ete modifiees avec UminAmeliore */
# if UTILISER_UMIN_AMELIORE == OUI_PNE
  memcpy( (char *) Umin, (char *) ValeurDeUminARestaurer, NombreDeVariables * sizeof( double ) );
# endif
/*free( ValeurDeUminARestaurer );*/ /* 11/10/2016: deplace a l'etiquette FreeMemoire car sinon c'est pas libere lorsque Faisabilite est egal a NON_PNE */

if ( ProbingOuNodePresolve->NombreDeVariablesFixees <= 0 ) goto FreeMemoire;

T = (char *) malloc( ( Bb->NombreDeVariablesDuProbleme ) * sizeof(char) );
if ( T == NULL ) goto FreeMemoire;
memset( (char *) T, 0, Bb->NombreDeVariablesDuProbleme * sizeof( char ) );
NombreDeBornesModifiees = Noeud->NombreDeBornesModifiees;
NumeroDeLaVariableModifiee = Noeud->NumeroDeLaVariableModifiee;
for( i = 0 ; i < NombreDeBornesModifiees ; i++ ) T[NumeroDeLaVariableModifiee[i]] = 1;

il = NombreDeBornesModifiees + ProbingOuNodePresolve->NombreDeVariablesFixees;
Noeud->NumeroDeLaVariableModifiee = (int *) realloc( Noeud->NumeroDeLaVariableModifiee, il * sizeof( int ) );
if ( Noeud->NumeroDeLaVariableModifiee == NULL ) {
  free( Noeud->TypeDeBorneModifiee );
  free( Noeud->NouvelleValeurDeBorne );
	Noeud->TypeDeBorneModifiee = NULL;
	Noeud->NouvelleValeurDeBorne = NULL;
	Noeud->NombreDeBornesModifiees = 0;
  goto FreeMemoire;
}
Noeud->TypeDeBorneModifiee = (char *) realloc( Noeud->TypeDeBorneModifiee, il * sizeof( char ) );
if ( Noeud->TypeDeBorneModifiee == NULL ) {
  free( Noeud->NumeroDeLaVariableModifiee );
  free( Noeud->NouvelleValeurDeBorne );
	Noeud->NumeroDeLaVariableModifiee = NULL;
	Noeud->NouvelleValeurDeBorne = NULL;
	Noeud->NombreDeBornesModifiees = 0;
  goto FreeMemoire;
}
Noeud->NouvelleValeurDeBorne = (double *) realloc( Noeud->NouvelleValeurDeBorne, il * sizeof( double ) );
if ( Noeud->NouvelleValeurDeBorne == NULL ) {
  free( Noeud->NumeroDeLaVariableModifiee );
  free( Noeud->TypeDeBorneModifiee );   
	Noeud->NumeroDeLaVariableModifiee = NULL;
	Noeud->TypeDeBorneModifiee = NULL;
	Noeud->NombreDeBornesModifiees = 0;	
  goto FreeMemoire;
}

NumeroDeLaVariableModifiee = Noeud->NumeroDeLaVariableModifiee;
TypeDeBorneModifiee = Noeud->TypeDeBorneModifiee;
NouvelleValeurDeBorne = Noeud->NouvelleValeurDeBorne;

/* De plus on initialise les bornes pour le simplexe qui va suivre. Mais on ne le fait que pour les
   variables entieres */

for ( i = 0 ; i < ProbingOuNodePresolve->NombreDeVariablesFixees ; i++ ) {
  /* Ne contient que les variables entieres fixees */
  Var = NumeroDesVariablesFixees[i];	
	Ai = ValeurDeBorneInf[Var];
	U[Var] = Ai;
  Umin[Var] = Ai;
	Umax[Var] = Ai;	
	if ( BorneInfConnue[Var] == FIXATION_SUR_BORNE_SUP ) {	
    NumeroDeLaVariableModifiee[NombreDeBornesModifiees] = Var;
    TypeDeBorneModifiee[NombreDeBornesModifiees] = BORNE_INF;
		NouvelleValeurDeBorne[NombreDeBornesModifiees] = Umin[Var];
	  NombreDeBornesModifiees++;
		T[Var] = 2;
	}
	else if ( BorneInfConnue[Var] == FIXATION_SUR_BORNE_INF ) {	
    NumeroDeLaVariableModifiee[NombreDeBornesModifiees] = Var;
    TypeDeBorneModifiee[NombreDeBornesModifiees] = BORNE_SUP;
		NouvelleValeurDeBorne[NombreDeBornesModifiees] = Umax[Var];
	  NombreDeBornesModifiees++;
		T[Var] = 2;
	}
	else {
	  printf("BUG dans le node presolve: code BorneInfConnue[%d] = %d incorrect\n",Var,BorneInfConnue[Var]);
		exit(0);
	}
}
Noeud->NombreDeBornesModifiees = NombreDeBornesModifiees;

FreeMemoire:

free( T );
free( ContrainteActivable );
free( ValeurDeUminARestaurer );

return;

ModifsBornesAuNoeudRacine:

UminSv = Pne->UminTravSv;
UmaxSv = Pne->UmaxTravSv;
TypeDeBorneSv = Pne->TypeDeBorneTravSv;

Marge = MARGE_SUR_LA_MODIFICATION_DE_BORNE;

for ( i = 0 ; i < ProbingOuNodePresolve->NombreDeVariablesFixees ; i++ ) {
  /* Ne concerne que les variables entieres */
  Var = NumeroDesVariablesFixees[i];
	Ai = ValeurDeBorneInf[Var];
	U[Var] = Ai;
	UminSv[Var] = Ai;
	
  # if BORNES_INF_AUXILIAIRES == OUI_PNE
    Pne->XminAuxiliaire[Var] = Ai;
    Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */
  # endif
	
	UmaxSv[Var] = Ai;	
}

# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
	if ( UminAmeliorePourCoupes != NULL ) {
    for ( i = 0 ; i < Pne->NombreDeVariablesNonFixes ; i++ ) {
      Var = NumeroDesVariablesNonFixes[i];
	    if ( TypeDeVariable[Var] == ENTIER ) continue;
		  if ( TypeDeBorneSv[Var] == VARIABLE_NON_BORNEE ) continue;
		  if ( TypeDeBorneSv[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) continue;
      if ( BorneInfConnue[Var] == FIXE_AU_DEPART ) continue;
 	    if ( BorneInfConnue[Var] == NON_PNE ) continue;						
		  if ( UminAmeliorePourCoupes[Var] == VALEUR_NON_INITIALISEE ) {
		    if ( ValeurDeBorneInf[Var] - Marge > UminSv[Var] ) UminAmeliorePourCoupes[Var] = ValeurDeBorneInf[Var];
      }		
		  else if ( ValeurDeBorneInf[Var] - Marge > UminAmeliorePourCoupes[Var] ) UminAmeliorePourCoupes[Var] = ValeurDeBorneInf[Var];
		}		
  }	
# endif

# if UTILISER_UMIN_AMELIORE == OUI_PNE
  for ( i = 0 ; i < Pne->NombreDeVariablesNonFixes ; i++ ) {
    Var = NumeroDesVariablesNonFixes[i];
	  if ( TypeDeVariable[Var] == ENTIER ) continue;
		if ( TypeDeBorneSv[Var] == VARIABLE_NON_BORNEE ) continue;
		if ( TypeDeBorneSv[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) continue;
    if ( BorneInfConnue[Var] == FIXE_AU_DEPART ) continue;
 	  if ( BorneInfConnue[Var] == NON_PNE ) continue;
						
		if ( ValeurDeBorneInf[Var] - Marge > Umin[Var] ) {		
		  UminAmeliore[Var] = ValeurDeBorneInf[Var];
		  Umin[Var] = UminAmeliore[Var] - Marge; /* Pour le calcul PNE_InitBorneInfBorneSupDesVariables qui suit juste apres */
		  /*UminSv[Var] = Umin[Var];*/ /* Au noeud racine on peut recuperer Umin car il n'y a pas eu de simplexe */
    }	
  }
# endif

# if RECUPERER_XMAX_AU_NOEUD_RACINE_DANS_LE_NODE_PRESOLVE == OUI_PNE
  /* On recupere Umax */
  for ( i = 0 ; i < Pne->NombreDeVariablesNonFixes ; i++ ) {
    Var = NumeroDesVariablesNonFixes[i];
    if ( TypeDeVariable[Var] == ENTIER ) continue;
    if ( BorneInfConnue[Var] == FIXE_AU_DEPART ) continue;
		if ( TypeDeBorneSv[Var] == VARIABLE_NON_BORNEE ) continue;
 	  if ( BorneSupConnue[Var] == NON_PNE ) continue;
		/*if ( TypeDeBorneSv[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) continue;*/	

    # if BORNES_INF_AUXILIAIRES == OUI_PNE
			if ( BorneInfConnue[Var] != NON_PNE ) {						
        if ( ValeurDeBorneInf[Var] > Pne->XminAuxiliaire[Var] + Marge ) {
          Pne->XminAuxiliaire[Var] = ValeurDeBorneInf[Var] - (0.1*Marge);
          Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */		
        }
			}
    # endif
	
 	  if ( ValeurDeBorneSup[Var] + Marge < UmaxSv[Var] ) {
			Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */			
 	    UmaxSv[Var] = ValeurDeBorneSup[Var] + Marge;				
			if ( UmaxSv[Var] < UminSv[Var] ) UmaxSv[Var] = UminSv[Var];
      # if UTILISER_UMIN_AMELIORE == OUI_PNE
				if ( UminAmeliore[Var] != VALEUR_NON_INITIALISEE ) {
				  if ( UmaxSv[Var] < UminAmeliore[Var] ) UmaxSv[Var] = UminAmeliore[Var];
				}
			# endif					
      /* Precaution */
			if ( UmaxSv[Var] < UminSv[Var] ) {
			  UmaxSv[Var] = UminSv[Var]; /* Si le pb est vraiment infaisable on s'en apercevra ensuite */
			}
      if ( TypeDeBorneSv[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {
			  if ( UmaxSv[Var] - UminSv[Var] < 1.e+9 ) {
          TypeDeBorneSv[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;
          TypeDeBorne[Var] = VARIABLE_BORNEE_DES_DEUX_COTES;
				}
			}			
	  }
  }
# endif

for ( i = 0 ; i < Pne->NombreDeVariablesNonFixes ; i++ ) {
  Var = NumeroDesVariablesNonFixes[i];
	Umin[Var] = UminSv[Var];
	Umax[Var] = UmaxSv[Var];
}

/* Attention: il faut recalculer les Min et Max des contraintes si des bornes sont modifiees */
PNE_InitBorneInfBorneSupDesVariables( Pne );
PNE_CalculMinEtMaxDesContraintes( Pne, Faisabilite );
if ( *Faisabilite == NON_PNE ) {	
 	# if TRACES == 1
		printf("Pas de solution apres le presolve du noeud racine\n");
	# endif 
  goto Fin;
}	
/* Et on sauvegarde le resultat comme point de depart pour les noeuds suivants */
memcpy( (char *) ProbingOuNodePresolve->BminSv, (char *) ProbingOuNodePresolve->Bmin, NombreDeContraintes * sizeof( double ) );
memcpy( (char *) ProbingOuNodePresolve->BmaxSv, (char *) ProbingOuNodePresolve->Bmax, NombreDeContraintes * sizeof( double ) );

BminValide = ProbingOuNodePresolve->BminValide;
BmaxValide = ProbingOuNodePresolve->BmaxValide;
SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  CntActiv = ContrainteActivable[Cnt];
  # if UTILISER_UMIN_AMELIORE == NON_PNE
    ContrainteActivable[Cnt] = OUI_PNE; 
	# endif	
	if ( SensContrainte[Cnt] == '<' ) {
		if ( BmaxValide[Cnt] == OUI_PNE ) {
      if ( Bmax[Cnt] <= B[Cnt] + MARGE_POUR_RELAXATION ) {
				if ( CntActiv == OUI_PNE ) {
          Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */							
				}
				ContrainteActivable[Cnt] = NON_PNE;
			}			
		}		
	}
	else {
		if ( BminValide[Cnt] == OUI_PNE && BmaxValide[Cnt] == OUI_PNE ) {
      if ( fabs( Bmax[Cnt] - Bmin[Cnt] ) < MARGE_POUR_RELAXATION ) {
				if ( fabs( Bmax[Cnt] - B[Cnt] ) < MARGE_POUR_RELAXATION ) {
				  if ( CntActiv == OUI_PNE ) {
            Pne->CreerContraintesPourK = OUI_PNE; /* Car il y a eu un changement */							
				  }				
				  ContrainteActivable[Cnt] = NON_PNE;
				}
      }
    }		
	}
}

free( T );
free( ContrainteActivable );
free( ValeurDeUminARestaurer );

return;
}


