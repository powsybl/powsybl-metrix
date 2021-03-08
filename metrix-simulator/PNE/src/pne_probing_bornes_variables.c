/***********************************************************************

   FONCTION: Detection des contraintes de borne variable pendant le
	           variable probing.
						 Soient y la variable binaire instanciee et x la variable
						 continue. On genere des contraintes de type/
						 x <= xmax + (xmaxNew-xmax) * y si la borne change pour y = 1
						 x <= xmaxNew + (xmax-xmaxNew) * y si la borne change pour y = 0
						 x >= xmin + (xminNew-xmin) * y si la borne change pour y = 1
						 x >= xminNew + (xmin-xminNew) * y si la borne change pour y = 0
                
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

# define VERBOSE_CONTRAINTES_DE_BORNE  NON_PNE

# define MAX_CONTRAINTES_DE_BORNE_VARIABLE  1000000 /* Pas de limite */ /*100000*/
# define EGALITE 0
# define BORNE_SUP 1
# define BORNE_INF 2
# define MARGE 1 
# define MARGE_SUR_NOUVELLE_BORNE 0.e-10  /* Il vaut mieux mettre 0 car si le B vaut 0 ca fait un Epsilon qui perturbe le calcul */
# define SEUIL_DE_REDUCTION_DE_PLAGE 0.1 

# define SIZE_ALLOC_CONTRAINTES 1000 /* Nombre de contraintes allouees */
# define SIZE_ALLOC_TERMES_CONTRAINTES (SIZE_ALLOC_CONTRAINTES*2)

# if CONSTRUIRE_BORNES_VARIABLES == OUI_PNE

void PNE_ProbingAllocContraintesDeBorneVariable( PROBLEME_PNE * );
void PNE_AugmenterNombreDeContraintesDeBorneVariable( PROBLEME_PNE * );
void PNE_AugmenterLaTailleDesContraintesDeBorneVariable( PROBLEME_PNE * );
void PNE_ProbingEtablirContraineDeBornes( PROBLEME_PNE * , int , int , double , double , double , char , char );
																					
/*----------------------------------------------------------------------------*/
/* On cherche simplement a savoir s'il y a une contrainte de borne variable
   sur la variable continue */
void PNE_ProbingInitDetectionDesBornesVariables( PROBLEME_PNE * Pne )
{
int Cnt; int il; int ilMax; int Var; int NbBin; int NombreDeContraintes; int NbCont;
int VarCont; double CoeffCont; int * Mdeb; int * NbTerm; int NombreDeVariables;
int * Nuvar; int * TypeDeVariable; double * A;  int * CntDeBorneSupVariable;
int * CntDeBorneInfVariable; char * SensContrainte; int * TypeDeBorne; double * B; 

# if PROBING_JUSTE_APRES_LE_PRESOLVE == NON_PNE		  
  return;
# endif

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

if ( Pne->CntDeBorneSupVariable == NULL ) {   
  Pne->CntDeBorneSupVariable  = (int *) malloc( NombreDeVariables * sizeof( int ) );
  if ( Pne->CntDeBorneSupVariable == NULL ) {
    printf(" Solveur PNE , memoire insuffisante. Sous-programme: PNE_ProbingDetectionDesBornesVariables \n");
    Pne->AnomalieDetectee = OUI_PNE;
    longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
  }	
}
if ( Pne->CntDeBorneInfVariable == NULL ) {   
  Pne->CntDeBorneInfVariable  = (int *) malloc( NombreDeVariables * sizeof( int ) );
  if ( Pne->CntDeBorneInfVariable == NULL ) {
    printf(" Solveur PNE , memoire insuffisante. Sous-programme: PNE_ProbingDetectionDesBornesVariables \n");
    Pne->AnomalieDetectee = OUI_PNE;
    longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
  }	
}

CntDeBorneSupVariable = Pne->CntDeBorneSupVariable;
CntDeBorneInfVariable = Pne->CntDeBorneInfVariable;

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) { CntDeBorneSupVariable[Var] = -1; CntDeBorneInfVariable[Var] = -1; }

SensContrainte = Pne->SensContrainteTrav;
NbTerm = Pne->NbTermTrav;   
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
Nuvar = Pne->NuvarTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
A = Pne->ATrav;

/* Marquage des variables continues soumises a une borne sup ou inf variable */
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  if ( SensContrainte[Cnt] != '<' ) continue;
  if ( NbTerm[Cnt] != 2 ) continue;
	NbCont = 0;
  CoeffCont = 0.0;
	NbBin = 0;
	VarCont = -1;
  il = Mdeb[Cnt];
  ilMax = il + 2;	
  while ( il < ilMax ) { 
    Var = Nuvar[il];
    if ( TypeDeBorne[Var] != VARIABLE_FIXE ) {
      if ( TypeDeVariable[Var] == ENTIER ) {
			  NbBin++;
				if ( NbBin > 1 ) break;
			}
      else {
	      NbCont++;
				VarCont = Var;
				CoeffCont = A[il];
				if ( NbCont > 1 ) break;
			}
		}
    il++;
  }
	if ( NbCont == 1 && NbBin == 1 && VarCont != -1 ) {		
		/* C'est une contrainte de borne variable */
		if ( CoeffCont > 0.0 ) CntDeBorneSupVariable[VarCont] = Cnt;
		else CntDeBorneInfVariable[VarCont] = Cnt;
	}
}

return;
}
/*----------------------------------------------------------------------------*/
void PNE_ProbingCloseDetectionDesBornesVariables( PROBLEME_PNE * Pne )
{
# if PROBING_JUSTE_APRES_LE_PRESOLVE == NON_PNE		  
  return;
# endif
free( Pne->CntDeBorneSupVariable );
free( Pne->CntDeBorneInfVariable );
Pne->CntDeBorneSupVariable = NULL;
Pne->CntDeBorneInfVariable = NULL;
return;
}

/*----------------------------------------------------------------------------*/
void PNE_ProbingAllocContraintesDeBorneVariable( PROBLEME_PNE * Pne )
{
CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
ContraintesDeBorneVariable = (CONTRAITES_DE_BORNE_VARIABLE *) malloc( sizeof( CONTRAITES_DE_BORNE_VARIABLE ) );
if ( ContraintesDeBorneVariable == NULL ) return;

ContraintesDeBorneVariable->SecondMembre = (double *) malloc( SIZE_ALLOC_CONTRAINTES * sizeof( double ) );
if ( ContraintesDeBorneVariable->SecondMembre == NULL ) {
  return;
}
ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool = (char *) malloc( SIZE_ALLOC_CONTRAINTES * sizeof( char ) );
if ( ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool == NULL ) {
  free( ContraintesDeBorneVariable->SecondMembre );
	return;
}
ContraintesDeBorneVariable->AppliquerUneMargeEventuelle = (char *) malloc( SIZE_ALLOC_CONTRAINTES * sizeof( char ) );
if ( ContraintesDeBorneVariable->AppliquerUneMargeEventuelle == NULL ) {
  free( ContraintesDeBorneVariable->SecondMembre );
  free( ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool );
	return;
}
ContraintesDeBorneVariable->First = (int *) malloc( SIZE_ALLOC_CONTRAINTES * sizeof( int ) );
if ( ContraintesDeBorneVariable->First == NULL ) {
  free( ContraintesDeBorneVariable->SecondMembre );	
	free( ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool ); 
  free( ContraintesDeBorneVariable->AppliquerUneMargeEventuelle );
  return;
}
ContraintesDeBorneVariable->NombreDeContraintesDeBorneAlloue = SIZE_ALLOC_CONTRAINTES;

ContraintesDeBorneVariable->Colonne = (int *) malloc( SIZE_ALLOC_TERMES_CONTRAINTES * sizeof( int ) );
if ( ContraintesDeBorneVariable->Colonne == NULL ) {
  free( ContraintesDeBorneVariable->SecondMembre );	
	free( ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool ); 
  free( ContraintesDeBorneVariable->AppliquerUneMargeEventuelle );
	free( ContraintesDeBorneVariable->First ); 
  return;
}
ContraintesDeBorneVariable->Coefficient = (double *) malloc( SIZE_ALLOC_TERMES_CONTRAINTES * sizeof( double ) );
if ( ContraintesDeBorneVariable->Coefficient == NULL ) {
  free( ContraintesDeBorneVariable->SecondMembre );	
	free( ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool ); 
  free( ContraintesDeBorneVariable->AppliquerUneMargeEventuelle );
	free( ContraintesDeBorneVariable->First ); 
	free( ContraintesDeBorneVariable->Colonne ); 
  return;
}
ContraintesDeBorneVariable->TailleContraintesDeBorneAllouee = SIZE_ALLOC_TERMES_CONTRAINTES;
ContraintesDeBorneVariable->IndexLibre = 0;
ContraintesDeBorneVariable->NombreDeContraintesDeBorne = 0;
ContraintesDeBorneVariable->Full = NON_PNE;
Pne->ContraintesDeBorneVariable = ContraintesDeBorneVariable;
return;
}
/*----------------------------------------------------------------------------*/
void PNE_AugmenterNombreDeContraintesDeBorneVariable( PROBLEME_PNE * Pne )
{
CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable; int Size; double * SecondMembre;
char * LaContrainteDeBorneVariableEstDansLePool; int * First; char * AppliquerUneMargeEventuelle;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;
Size = ContraintesDeBorneVariable->NombreDeContraintesDeBorne + SIZE_ALLOC_CONTRAINTES;
SecondMembre = (double *) realloc( ContraintesDeBorneVariable->SecondMembre, Size * sizeof( double ) );
if ( SecondMembre == NULL ) {
	ContraintesDeBorneVariable->Full = OUI_PNE;
  return;
}
LaContrainteDeBorneVariableEstDansLePool = (char *) realloc( ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool, Size * sizeof( char ) );
if ( LaContrainteDeBorneVariableEstDansLePool == NULL ) {
  free( SecondMembre );
	ContraintesDeBorneVariable->Full = OUI_PNE;
	return;
}
AppliquerUneMargeEventuelle = (char *) realloc( ContraintesDeBorneVariable->AppliquerUneMargeEventuelle, Size * sizeof( char ) );
if ( AppliquerUneMargeEventuelle == NULL ) {
  free( SecondMembre );
	free( LaContrainteDeBorneVariableEstDansLePool ); 
	ContraintesDeBorneVariable->Full = OUI_PNE;
	return;
}
First = (int *) realloc( ContraintesDeBorneVariable->First, Size * sizeof( int ) );
if ( First == NULL ) {
  free( SecondMembre );	
	free( LaContrainteDeBorneVariableEstDansLePool ); 
	free( AppliquerUneMargeEventuelle ); 
	ContraintesDeBorneVariable->Full = OUI_PNE;
  return;
}
ContraintesDeBorneVariable->NombreDeContraintesDeBorneAlloue = Size;
ContraintesDeBorneVariable->SecondMembre = SecondMembre;
ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool = LaContrainteDeBorneVariableEstDansLePool;
ContraintesDeBorneVariable->AppliquerUneMargeEventuelle = AppliquerUneMargeEventuelle;
ContraintesDeBorneVariable->First = First;
return;
}
/*----------------------------------------------------------------------------*/
void PNE_AugmenterLaTailleDesContraintesDeBorneVariable( PROBLEME_PNE * Pne )
{
CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable; int Size; int * Colonne; double * Coefficient;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;
Size = ContraintesDeBorneVariable->TailleContraintesDeBorneAllouee + SIZE_ALLOC_TERMES_CONTRAINTES;

Colonne = (int *) realloc( ContraintesDeBorneVariable->Colonne, Size * sizeof( int ) );
if ( Colonne == NULL ) {
	ContraintesDeBorneVariable->Full = OUI_PNE;
  return;
}
Coefficient = (double *) realloc( ContraintesDeBorneVariable->Coefficient, Size * sizeof( double ) );
if ( Coefficient == NULL ) {
	free( Colonne ); 
	ContraintesDeBorneVariable->Full = OUI_PNE;
  return;
}
ContraintesDeBorneVariable->TailleContraintesDeBorneAllouee = Size;
ContraintesDeBorneVariable->Colonne = Colonne;
ContraintesDeBorneVariable->Coefficient = Coefficient;
return;
}
/*----------------------------------------------------------------------------*/
void PNE_ProbingEtablirContraineDeBornes( PROBLEME_PNE * Pne, int Var, int VariableInstanciee,
                                          double a0, double a, double b, char Borne, char Fiable )
{  
CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable; int il; int Cnt;
/* On regarde si la variable entiere servait deja a construite une contrainte de borne */

if ( Borne == BORNE_SUP ) Cnt = Pne->CntDeBorneSupVariable[Var];
else if ( Borne == BORNE_INF ) Cnt = Pne->CntDeBorneInfVariable[Var];
else Cnt = -1;

# if TENIR_COMPTE_DE_LA_PRESENCE_DE_BORNES_VARIABLES_NATIVE_DANS_CALCUL_DES_BORNES_VARIABLE == NON_PNE
  Cnt = -1;
# endif

if ( Cnt >= 0 ) {
  il = Pne->MdebTrav[Cnt];
  if ( Pne->NuvarTrav[il] == VariableInstanciee || Pne->NuvarTrav[il+1] == VariableInstanciee ) return;
}
if ( Pne->ContraintesDeBorneVariable == NULL ) {			
  PNE_ProbingAllocContraintesDeBorneVariable( Pne );
  if ( Pne->ContraintesDeBorneVariable == NULL ) return; /* Saturation memoire */		
}

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

/* Place suffisante pour les contraintes */
if ( ContraintesDeBorneVariable->NombreDeContraintesDeBorne >= ContraintesDeBorneVariable->NombreDeContraintesDeBorneAlloue ) {
  PNE_AugmenterNombreDeContraintesDeBorneVariable( Pne );		
  if ( Pne->ContraintesDeBorneVariable == NULL ) return;
  if ( ContraintesDeBorneVariable->Full == OUI_PNE ) return;
}
/* Place suffisante pour les termes */
if ( ContraintesDeBorneVariable->IndexLibre + 2 >= ContraintesDeBorneVariable->TailleContraintesDeBorneAllouee ) {
  PNE_AugmenterLaTailleDesContraintesDeBorneVariable( Pne );			 
  if ( Pne->ContraintesDeBorneVariable == NULL ) return;	   
  if ( ContraintesDeBorneVariable->Full == OUI_PNE ) return;
}
il = ContraintesDeBorneVariable->IndexLibre;
ContraintesDeBorneVariable->First[ContraintesDeBorneVariable->NombreDeContraintesDeBorne] = il;
/* La variable continue en premier */
ContraintesDeBorneVariable->Coefficient[il] = a0;
ContraintesDeBorneVariable->Colonne[il] = Var;
il++;
ContraintesDeBorneVariable->Coefficient[il] = a;
ContraintesDeBorneVariable->Colonne[il] = VariableInstanciee;
il++;
ContraintesDeBorneVariable->IndexLibre = il;
ContraintesDeBorneVariable->SecondMembre[ContraintesDeBorneVariable->NombreDeContraintesDeBorne] = b;
ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool[ContraintesDeBorneVariable->NombreDeContraintesDeBorne] = NON_PNE;
if ( Fiable == OUI_PNE ) {
  ContraintesDeBorneVariable->AppliquerUneMargeEventuelle[ContraintesDeBorneVariable->NombreDeContraintesDeBorne] = NON_PNE /*NON_PNE*/;
}
else {
  ContraintesDeBorneVariable->AppliquerUneMargeEventuelle[ContraintesDeBorneVariable->NombreDeContraintesDeBorne] = OUI_PNE;
}
ContraintesDeBorneVariable->NombreDeContraintesDeBorne += 1;			
return;
}
/*----------------------------------------------------------------------------*/

void PNE_ProbingConstruireContraintesDeBornes( PROBLEME_PNE * Pne, PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve,
                                               double * Xmin0, double * Xmax0 )
{
int NombreDeVariables; int Var; int VariableInstanciee; double ValeurDeLaVariableInstanciee; double ReductionDePlage;
char * BorneInfConnue; int * TypeDeBorne; int * TypeDeVariable;double * ValeurDeBorneInf; int i; char Fiable;
double * ValeurDeBorneSup; double a; double b; double a0; char Borne; char CreerUneContrainte; double ZTest;
double PlageInitiale; double PlageFinale; double Alpha; double PlusGrandTerme; double PlusPetitTerme;
double BorneSupCandidate; double BorneInfCandidate; char ContrainteCree; int NombreMaxDeContaintesDeBorneVariable;
int * NumeroDeVariableModifiee; char OnReboucle; double * ValeurDeBorneInfSv; double * ValeurDeBorneSupSv;

# if PROBING_JUSTE_APRES_LE_PRESOLVE == NON_PNE		  
  return;
# endif

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreMaxDeContaintesDeBorneVariable = 2 * NombreDeVariables;
if ( NombreMaxDeContaintesDeBorneVariable < MAX_CONTRAINTES_DE_BORNE_VARIABLE ) {
  NombreMaxDeContaintesDeBorneVariable = MAX_CONTRAINTES_DE_BORNE_VARIABLE;
}

if ( ProbingOuNodePresolve->VariableInstanciee < 0 ) return;

if ( Pne->ContraintesDeBorneVariable != NULL ) {
  if ( Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne >= NombreMaxDeContaintesDeBorneVariable ) return;
}

VariableInstanciee = ProbingOuNodePresolve->VariableInstanciee;
ValeurDeLaVariableInstanciee = ProbingOuNodePresolve->ValeurDeLaVariableInstanciee;

BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;

ValeurDeBorneInfSv = ProbingOuNodePresolve->ValeurDeBorneInfSv;
ValeurDeBorneSupSv = ProbingOuNodePresolve->ValeurDeBorneSupSv;

TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;

PlusGrandTerme = Pne->PlusGrandTerme;
PlusPetitTerme = Pne->PlusPetitTerme;

NumeroDeVariableModifiee = ProbingOuNodePresolve->NumeroDeVariableModifiee;

/*for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {*/

OnReboucle = OUI_PNE;
while ( OnReboucle == OUI_PNE ) {

break;

  OnReboucle = NON_PNE;
  for ( i = 0 ; i < ProbingOuNodePresolve->NbVariablesModifiees - 1 ; i++ ) {
    if ( NumeroDeVariableModifiee[i+1] < NumeroDeVariableModifiee[i] ) {
      OnReboucle = OUI_PNE;
      Var = NumeroDeVariableModifiee[i+1];
			NumeroDeVariableModifiee[i+1] = NumeroDeVariableModifiee[i];
			NumeroDeVariableModifiee[i] = Var;
		}
	}
}

ZTest = 2.e-8;

for ( i = 0 ; i < ProbingOuNodePresolve->NbVariablesModifiees ; i++ ) {
  Var = NumeroDeVariableModifiee[i];

	if ( TypeDeVariable[Var] != REEL ) continue;	
	if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
  /*
	printf("Variable continue %d xmin %e xmax %e\n",Var,ValeurDeBorneInf[Var],ValeurDeBorneSup[Var]);
  */	
  /* Recherche d'une egalite */
	/*
	if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES ) {	  
    if ( ValeurDeBorneSup[Var] == ValeurDeBorneInf[Var] ) {
      if ( ValeurDeBorneSup[Var] != Xmax0[Var] && ValeurDeBorneSup[Var] != Xmin0[Var] ) {
			  ContrainteCree = OUI_PNE;
			  Alpha = ValeurDeBorneSup[Var];
			
		*/

	if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES &&  Xmax0[Var] != Xmin0[Var] ) {
    if ( fabs( ValeurDeBorneSup[Var] - ValeurDeBorneInf[Var] ) <= ZTest ) {
      if ( ValeurDeBorneSup[Var] != Xmax0[Var] || ValeurDeBorneInf[Var] != Xmin0[Var] ) {
		
			  ContrainteCree = OUI_PNE;
			  Alpha = 0.5 * ( ValeurDeBorneSup[Var] + ValeurDeBorneInf[Var] );
				
       /* Quand on fixe une variable a une tres petite valeur, il vaut mieux mettre 0 car souvent la valeur
          tres petite (mais non nulle ) est du soit aux effets des toutes petites marges, soit aux effet des
          imprecisions numeriques */

        Fiable = NON_PNE;
					
        if ( fabs( Alpha ) <= ZTest ) {
				  Alpha = 0;								
          Fiable = OUI_PNE;
				}
				if (  fabs( Alpha - ValeurDeBorneInfSv[Var] ) < ZTest ) {
				  Alpha = ValeurDeBorneInfSv[Var];
          Fiable = OUI_PNE;
				}
				else if ( fabs( Alpha - ValeurDeBorneSupSv[Var] ) < ZTest ) {
				  Alpha = ValeurDeBorneSupSv[Var];
          Fiable = OUI_PNE;
				}
        Fiable = OUI_PNE; /* On met OUI quand meme ... bof ... */
								
			  Borne = EGALITE;
			  /* On cree 2 contraintes pour construire l'egalite a la valeur Alpha */
				/* Cas y = 1: 
				x <= xmax - (xmax-Alpha) * y  i.e  x + (xmax-Alpha) * y <= xmax
				x >= xmin + (Alpha - xmin) * y i.e. -x + (Alpha - xmin) * y <= -xmin
           Cas y = 0:
				x - (xmax-Alpha) * y <= Alpha
				-x - (Alpha - xmin) * y <= -Alpha */
				
        a0 = 1;
        if ( ValeurDeLaVariableInstanciee == 1 ) { a = Xmax0[Var] - Alpha; b = Xmax0[Var]; }
				else if ( ValeurDeLaVariableInstanciee == 0 ) { a = -(Xmax0[Var] - Alpha); b = Alpha; }
				else {
				  printf("Bug dans PNE_ProbingConstruireContraintesDeBornes ValeurDeLaVariableInstanciee %e\n",ValeurDeLaVariableInstanciee);
					continue;
				}
				
				if ( fabs( a ) >= PlusPetitTerme && fabs( a ) <= PlusGrandTerme ) {
			    Borne = EGALITE;
          PNE_ProbingEtablirContraineDeBornes( Pne, Var, VariableInstanciee, a0, a, b, Borne, Fiable );
          if ( Pne->ContraintesDeBorneVariable != NULL ) {
            if ( Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne >= NombreMaxDeContaintesDeBorneVariable ) return;
          }
				}
				else ContrainteCree = NON_PNE;
				
        a0 = -1;
        if ( ValeurDeLaVariableInstanciee == 1 ) { a = Alpha - Xmin0[Var]; b = -Xmin0[Var]; }
				else if ( ValeurDeLaVariableInstanciee == 0 ) { a = -(Alpha - Xmin0[Var]); b = -Alpha; }
				else {
				  printf("Bug dans PNE_ProbingConstruireContraintesDeBornes ValeurDeLaVariableInstanciee %e\n",ValeurDeLaVariableInstanciee);
					continue;
				}
				if ( fabs( a ) >= PlusPetitTerme && fabs( a ) <= PlusGrandTerme ) {													
			    Borne = EGALITE;
          PNE_ProbingEtablirContraineDeBornes( Pne, Var, VariableInstanciee, a0, a, b, Borne, Fiable );
          if ( Pne->ContraintesDeBorneVariable != NULL ) {
            if ( Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne >= NombreMaxDeContaintesDeBorneVariable ) return;
          }						
				}
				else ContrainteCree = NON_PNE;

				if ( ContrainteCree == OUI_PNE ) {
	        # if VERBOSE_CONTRAINTES_DE_BORNE == OUI_PNE			
            printf("Contrainte pour fixer la variable %d a %e  (xmax: %e xmin: %e) si la variable entier %d passe a %e\n",
					          Var,Alpha,Xmax0[Var],Xmin0[Var],VariableInstanciee,ValeurDeLaVariableInstanciee);									 
	        # endif
				  continue;
				}
				
	    }
    }
	}	
	/* Recherche d'une borne sup */
	if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
	  CreerUneContrainte = NON_PNE;
		BorneSupCandidate = ValeurDeBorneSup[Var] + MARGE_SUR_NOUVELLE_BORNE;
		
    /* Quand on fixe une variable a une tres petite valeur, il vaut mieux mettre 0 car souvent la valeur
       tres petite (mais non nulle ) est du soit aux effets des toutes petites marges, soit aux effet des
       imprecisions numeriques */				
    if ( fabs( BorneSupCandidate ) <= ZTest ) BorneSupCandidate = 0;
		
	  if ( BorneSupCandidate < Xmax0[Var] - MARGE ) CreerUneContrainte = OUI_PNE;

		if ( CreerUneContrainte == NON_PNE && TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES ) {
      PlageInitiale = Xmax0[Var] - Xmin0[Var];
      PlageFinale = BorneSupCandidate - Xmin0[Var];			
			ReductionDePlage = SEUIL_DE_REDUCTION_DE_PLAGE * PlageInitiale;
			if ( ReductionDePlage > MARGE ) ReductionDePlage = MARGE;			
		  if ( PlageFinale - PlageInitiale > ReductionDePlage ) CreerUneContrainte = OUI_PNE;		
		}		
	  if ( CreerUneContrainte == OUI_PNE ) {
						
      Fiable = NON_PNE; 
			if (  fabs( BorneSupCandidate - ValeurDeBorneInfSv[Var] ) < ZTest ) {
        Fiable = OUI_PNE; 
			  BorneSupCandidate = ValeurDeBorneInfSv[Var]; /* On la colle sur borne inf */
			}
		
		  a0 = 1;
      if ( ValeurDeLaVariableInstanciee == 1 ) {
			  b = Xmax0[Var];
				a = -(BorneSupCandidate - Xmax0[Var]);					
			}
			else if ( ValeurDeLaVariableInstanciee == 0 ) {				
			  b = BorneSupCandidate;
				a = -(Xmax0[Var] - BorneSupCandidate);							
			}
			else {
			  printf("Bug dans PNE_ProbingConstruireContraintesDeBornes ValeurDeLaVariableInstanciee %e\n",ValeurDeLaVariableInstanciee);
				continue;
			}
			/* Pour ne pas trop detruire le conditionnement */
			if ( fabs( a ) < PlusPetitTerme || fabs( a ) > PlusGrandTerme ) CreerUneContrainte = NON_PNE;						
			if ( CreerUneContrainte == OUI_PNE ) {
	      # if VERBOSE_CONTRAINTES_DE_BORNE == OUI_PNE
          printf("Contrainte de borne sup variable sur %d xmax: %e -> %e (xmin = %e) variable instancies %d a %e\n",
			            Var,Xmax0[Var],BorneSupCandidate,Xmin0[Var],VariableInstanciee,ValeurDeLaVariableInstanciee);
	      # endif
			  Borne = BORNE_SUP;
        PNE_ProbingEtablirContraineDeBornes( Pne, Var, VariableInstanciee, a0, a, b, Borne, Fiable );
        if ( Pne->ContraintesDeBorneVariable != NULL ) {
          if ( Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne >= NombreMaxDeContaintesDeBorneVariable ) return;
        }
			} 
    }
	}
	/* Recherche d'une borne inf */
	if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {
	  CreerUneContrainte = NON_PNE;
		BorneInfCandidate = ValeurDeBorneInf[Var] - MARGE_SUR_NOUVELLE_BORNE;

    /* Quand on fixe une variable a une tres petite valeur, il vaut mieux mettre 0 car souvent la valeur
       tres petite (mais non nulle ) est du soit aux effets des toutes petites marges, soit aux effet des
       imprecisions numeriques */				
    if ( fabs( BorneInfCandidate ) <= ZTest ) BorneInfCandidate = 0;		
		
    if ( BorneInfCandidate > Xmin0[Var] + MARGE ) CreerUneContrainte = OUI_PNE;
		if ( CreerUneContrainte == NON_PNE && TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES ) {
      PlageInitiale = Xmax0[Var] - Xmin0[Var];
      PlageFinale = Xmax0[Var] - BorneInfCandidate;			
			ReductionDePlage = SEUIL_DE_REDUCTION_DE_PLAGE * PlageInitiale;
			if ( ReductionDePlage > MARGE ) ReductionDePlage = MARGE;			
		  if ( PlageFinale - PlageInitiale > ReductionDePlage ) CreerUneContrainte = OUI_PNE;		
    }		
	  if ( CreerUneContrainte == OUI_PNE ) {
					
      Fiable = NON_PNE; 
			if (  fabs( BorneInfCandidate - ValeurDeBorneSupSv[Var] ) < ZTest ) {
        Fiable = OUI_PNE; 
			  BorneInfCandidate = ValeurDeBorneSupSv[Var]; /* On la colle sur borne sup */
			}
				
	    /* On regarde si la variable entiere servait deja a construite une contrainte de borne */
		  a0 = -1;
      if ( ValeurDeLaVariableInstanciee == 1 ) {
		    b = -Xmin0[Var];
			  a = BorneInfCandidate - Xmin0[Var];												
		  }
		  else if ( ValeurDeLaVariableInstanciee == 0 ) {
		    b = -BorneInfCandidate;
			  a = Xmin0[Var] - BorneInfCandidate;
		  }
			else {
			  printf("Bug dans PNE_ProbingConstruireContraintesDeBornes ValeurDeLaVariableInstanciee %e\n",ValeurDeLaVariableInstanciee);
				continue;
			}
			/* Pour ne pas trop detruire le conditionnement */
			if ( fabs( a ) < PlusPetitTerme || fabs( a ) > PlusGrandTerme ) CreerUneContrainte = NON_PNE;									
			if ( CreerUneContrainte == OUI_PNE ) {			
	      # if VERBOSE_CONTRAINTES_DE_BORNE == OUI_PNE
          printf("Contrainte de borne inf variable sur %d xmin: %e -> %e (xmax = %e) variable instancies %d a %e\n",
			            Var,Xmin0[Var],BorneInfCandidate,Xmax0[Var],VariableInstanciee,ValeurDeLaVariableInstanciee);
	 	    # endif
			  Borne = BORNE_INF;
        PNE_ProbingEtablirContraineDeBornes( Pne, Var, VariableInstanciee, a0, a, b, Borne, Fiable );			      	
        if ( Pne->ContraintesDeBorneVariable != NULL ) {
          if ( Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne >= NombreMaxDeContaintesDeBorneVariable ) return;
        }
			}
	  }
  }
}

return;
}

/*----------------------------------------------------------------------------*/
/* Indispensable si on dit VARIABLE_FIXE pour les variables entieres qu'on fixe dans le variable probing */

void PNE_ProbingSupprimerCntDeBornesVariablesSiVariableEntiereFixee( PROBLEME_PNE * Pne )

{
int Cnt; int ilVarBin; int * First; int * Colonne; int Var; int * TypeDeBorne; int * TypeDeVariable; int NombreDeContraintesDeBornes;
double * Coefficient; double * SecondMembre; char * LaContrainteDeBorneVariableEstDansLePool; int IndexLibre;
int ilVarcont; int CntDer; int ilVarcontDer; int ilVarBinDer;

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

printf("ProbingSupprimerCntDeBornesVariablesSiVariableEntiereFixee attention, il faut aussi modifier ce sp pour tenir compte de AppliquerUneMargeEventuelle\n");
printf("Donc exit tant que c'est pas fait\n");
exit(0);

NombreDeContraintesDeBornes = Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = Pne->ContraintesDeBorneVariable->First;
Colonne = Pne->ContraintesDeBorneVariable->Colonne;
Coefficient = Pne->ContraintesDeBorneVariable->Coefficient;
SecondMembre = Pne->ContraintesDeBorneVariable->SecondMembre;
LaContrainteDeBorneVariableEstDansLePool = Pne->ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool;
IndexLibre = Pne->ContraintesDeBorneVariable->IndexLibre;

TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBornes ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;	
  ilVarcont = First[Cnt];
	ilVarBin = ilVarcont + 1;
	Var = Colonne[ilVarBin];
	if ( TypeDeBorne[Var] == VARIABLE_FIXE || TypeDeVariable[Var] == REEL ) {
		/* On supprime la contrainte */
    CntDer = NombreDeContraintesDeBornes - 1;
		ilVarcontDer = First[CntDer];
	  ilVarBinDer =  ilVarcontDer + 1;
		Colonne[ilVarcont] = Colonne[ilVarcontDer];
    Coefficient[ilVarcont] = Coefficient[ilVarcontDer];
		Colonne[ilVarBin] = Colonne[ilVarBinDer];
    Coefficient[ilVarBin] = Coefficient[ilVarBinDer];
	  SecondMembre[Cnt] = SecondMembre[CntDer];	
    LaContrainteDeBorneVariableEstDansLePool[Cnt] = LaContrainteDeBorneVariableEstDansLePool[CntDer];
		NombreDeContraintesDeBornes--;
		IndexLibre -= 2;
		Cnt--;   
  }				 			
}   

Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne = NombreDeContraintesDeBornes;
Pne->ContraintesDeBorneVariable->IndexLibre = IndexLibre;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ProbingAppliquerCntDeBornesVariablesSiVariableEntiereFixee( PROBLEME_PNE *  Pne, int VarBinFixee ) 
{
int Cnt; int * First; int ilbin; int ilcont; double B; int Varcont; int * Colonne; double Zero;
double * SecondMembre; double * Coefficient; int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
int NombreDeContraintesDeBorne; char * BorneSupConnue; char * BorneInfConnue; double ValeurDeVarBin;
double XmaxVarcont; double * ValeurDeBorneSup; double * ValeurDeBorneInf; double XminVarcont;
PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; 
char UneVariableAEteFixee; char BorneMiseAJour; double NouvelleValeur;

return;

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

printf("ProbingAppliquerCntDeBornesVariablesSiVariableEntiereFixee attention, il faut aussi modifier ce sp pour tenir compte de AppliquerUneMargeEventuelle\n");
printf("Donc exit tant que c'est pas fait\n");
exit(0);

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;

Zero = 1.e-8;

ValeurDeVarBin = ValeurDeBorneInf[VarBinFixee];

/* La variable continue est toujours placee en premier */

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;	
  ilcont = First[Cnt];
	ilbin = ilcont + 1;
	
	Varcont = Colonne[ilcont];
	Varbin = Colonne[ilbin];
	if ( Varbin != VarBinFixee ) continue;
	
	if ( BorneInfConnue[Varcont] == FIXE_AU_DEPART || BorneInfConnue[Varcont] == FIXATION_SUR_BORNE_INF ||
	     BorneInfConnue[Varcont] == FIXATION_SUR_BORNE_SUP || BorneInfConnue[Varcont] == FIXATION_A_UNE_VALEUR ) {
	  continue;
	}	
	
	B = SecondMembre[Cnt];

  UneVariableAEteFixee = NON_PNE;
  BorneMiseAJour = NON_PNE;	
				 
  /* La variable binaire est fixee */
  if ( Coefficient[ilcont] > 0 ) {
		/* La contrainte est une contrainte de borne sup */
		XmaxVarcont = ( B - ( Coefficient[ilbin] * ValeurDeVarBin ) ) / Coefficient[ilcont];		
		if ( XmaxVarcont < ValeurDeBorneSup[Varcont] ) {
 			
      NouvelleValeur = XmaxVarcont;
      BorneMiseAJour = MODIF_BORNE_SUP;

			if ( fabs( XmaxVarcont -  ValeurDeBorneInf[Varcont] ) < Zero ) {
        BorneMiseAJour = NON_PNE;
        UneVariableAEteFixee = FIXE_AU_DEPART;				
			}

			/*printf("Changement de borne sup %e -> %e\n",ValeurDeBorneSup[Varcont],NouvelleValeur);*/
      
			PNE_ProbingMajBminBmax( Pne, Varcont, NouvelleValeur, BorneMiseAJour );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;      
			
      PNE_MajIndicateursDeBornes( Pne, ValeurDeBorneInf, ValeurDeBorneSup,
                                  BorneInfConnue, BorneSupConnue,
                                  NouvelleValeur, Varcont, UneVariableAEteFixee, BorneMiseAJour );

			/*First[Cnt] = -1;*/												
		}								  
	}
	else {
		/* La contrainte est une contrainte de borne inf */
		XminVarcont = ( -B + ( Coefficient[ilbin] * ValeurDeVarBin ) ) / fabs( Coefficient[ilcont] );			
		if ( XminVarcont > ValeurDeBorneInf[Varcont] ) {
						  					
      NouvelleValeur = XminVarcont;
      BorneMiseAJour = MODIF_BORNE_INF;
			
			if ( fabs( XminVarcont -  ValeurDeBorneSup[Varcont] ) < Zero ) {
        BorneMiseAJour = NON_PNE;
        UneVariableAEteFixee = FIXE_AU_DEPART;				
      }


			/*printf("Changement de borne inf %e -> %e\n",ValeurDeBorneInf[Varcont],NouvelleValeur);*/

			
			PNE_ProbingMajBminBmax( Pne, Varcont, NouvelleValeur, BorneMiseAJour );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;			
			
      PNE_MajIndicateursDeBornes( Pne, ValeurDeBorneInf, ValeurDeBorneSup,
                                  BorneInfConnue, BorneSupConnue,
                                  NouvelleValeur, Varcont, UneVariableAEteFixee, BorneMiseAJour );			
					
		  /*First[Cnt] = -1;*/													
    }						
	}			 	
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ContraintesDeBornesVariablesInitListeDesContraintesAExaminer( PROBLEME_PNE * Pne, int VarBinFixee ) 
{
int Cnt; int * First; int ilbin; int ilcont; double B; int Varcont; int * Colonne; double Zero;
double * SecondMembre; double * Coefficient; int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
int NombreDeContraintesDeBorne; char * BorneSupConnue; char * BorneInfConnue; double ValeurDeVarBin;
double * ValeurDeBorneSup; double * ValeurDeBorneInf;
PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; char UneVariableAEteFixee; char BorneMiseAJour; double NouvelleValeur;

return; 

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

printf("ContraintesDeBornesVariablesInitListeDesContraintesAExaminer attention, il faut aussi modifier ce sp pour tenir compte de AppliquerUneMargeEventuelle\n");
printf("Donc exit tant que c'est pas fait\n");
exit(0);

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;

Zero = 1.e-8;

ValeurDeVarBin = ValeurDeBorneInf[VarBinFixee];

/* La variable continue est toujours placee en premier */

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;	
  ilcont = First[Cnt];
	ilbin = ilcont + 1;
	
	Varcont = Colonne[ilcont];
	Varbin = Colonne[ilbin];
	if ( Varbin != VarBinFixee ) continue;
	
	if ( BorneInfConnue[Varcont] == FIXE_AU_DEPART || BorneInfConnue[Varcont] == FIXATION_SUR_BORNE_INF ||
	     BorneInfConnue[Varcont] == FIXATION_SUR_BORNE_SUP || BorneInfConnue[Varcont] == FIXATION_A_UNE_VALEUR ) {
	  continue;
	}	
	
	B = SecondMembre[Cnt];

  UneVariableAEteFixee = NON_PNE;
  BorneMiseAJour = NON_PNE;	
				 
  /* La variable binaire est fixee */
  if ( Coefficient[ilcont] > 0 ) {
		/* La contrainte est une contrainte de borne sup */
		NouvelleValeur = ( B - ( Coefficient[ilbin] * ValeurDeVarBin ) ) / Coefficient[ilcont];		
		if ( NouvelleValeur < ValeurDeBorneSup[Varcont] ) {
 			
      BorneMiseAJour = MODIF_BORNE_SUP;

			if ( fabs( NouvelleValeur -  ValeurDeBorneInf[Varcont] ) < Zero ) {
        BorneMiseAJour = NON_PNE;
        UneVariableAEteFixee = FIXE_AU_DEPART;				
			}
      
      PNE_InitListeDesContraintesAExaminer( Pne, Varcont, NouvelleValeur, BorneMiseAJour );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;

      PNE_MajIndicateursDeBornes( Pne, ValeurDeBorneInf, ValeurDeBorneSup,
                                  BorneInfConnue, BorneSupConnue,
                                  NouvelleValeur, Varcont, UneVariableAEteFixee, BorneMiseAJour );																	

		}								  
	}
	else {
		/* La contrainte est une contrainte de borne inf */
		NouvelleValeur = ( -B + ( Coefficient[ilbin] * ValeurDeVarBin ) ) / fabs( Coefficient[ilcont] );			
		if ( NouvelleValeur > ValeurDeBorneInf[Varcont] ) {
						  					
      BorneMiseAJour = MODIF_BORNE_INF;
			
			if ( fabs( NouvelleValeur -  ValeurDeBorneSup[Varcont] ) < Zero ) {
        BorneMiseAJour = NON_PNE;
        UneVariableAEteFixee = FIXE_AU_DEPART;				
      }
			          
      PNE_InitListeDesContraintesAExaminer( Pne, Varcont, NouvelleValeur, BorneMiseAJour );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;			
			
      PNE_MajIndicateursDeBornes( Pne, ValeurDeBorneInf, ValeurDeBorneSup,
                                  BorneInfConnue, BorneSupConnue,
                                  NouvelleValeur, Varcont, UneVariableAEteFixee, BorneMiseAJour );			
					
    }						
	}			 	
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ProbingAppliquerCntDeBornesVariablesEnFinDeVariableProbing( PROBLEME_PNE * Pne, char * BornesModifiees )    
{
int Cnt; int * First; int Varcont; int * Colonne; double Zero; double * SecondMembre;
double * Coefficient; int Varbin; CONTRAITES_DE_BORNE_VARIABLE * ContraintesDeBorneVariable;
int NombreDeContraintesDeBorne; double ValeurDeVarBin; char SupprimerLaContrainteDeBorneVariable;
double NouvelleValeur; int ilVarBin; int ilVarcont; int ilVarBinDer; int ilVarcontDer; int CntDer;
double * Xmax; double * Xmin; double * X; int * TypeDeVariable; int * TypeDeBorne; int IndexLibre; 
char * LaContrainteDeBorneVariableEstDansLePool; double ValeurDeVarbin; char VarbinFixee;
double B; double S0; double S1;

*BornesModifiees = NON_PNE;
if ( Pne->ContraintesDeBorneVariable == NULL ) return;

return;

/* Test */
ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

printf("ProbingAppliquerCntDeBornesVariablesEnFinDeVariableProbing attention, il faut aussi modifier ce sp pour tenir compte de AppliquerUneMargeEventuelle\n");
printf("Donc exit tant que c'est pas fait\n");
exit(0);

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;

TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;
	
  ilVarcont = First[Cnt];
	ilVarBin = ilVarcont + 1;
	B = SecondMembre[Cnt];

	Varcont = Colonne[ilVarcont];	
	Varbin = Colonne[ilVarBin];
	
  VarbinFixee = NON_PNE;
	
	if ( TypeDeBorne[Varbin] == VARIABLE_FIXE ) { ValeurDeVarbin = X[Varbin]; VarbinFixee = OUI_PNE; }
	else if ( Xmin[Varbin] == Xmax[Varbin] ) { ValeurDeVarbin = Xmin[Varbin]; VarbinFixee = OUI_PNE; }
		
  if ( VarbinFixee == OUI_PNE ) {			 			 
    /* La variable binaire est fixee */
		NouvelleValeur = ( B - ( Coefficient[ilVarBin] * ValeurDeVarbin ) ) / Coefficient[ilVarcont];				
    if ( Coefficient[ilVarcont] > 0 ) {
		  /* La contrainte est une contrainte de borne sup */
			/*NouvelleValeur += 1.e-9;*/
			if ( NouvelleValeur < Xmax[Varcont] ) {
	      printf("1- Xmax[%d]: %e -> %e\n",Varcont,Xmax[Varcont],NouvelleValeur);
        Xmax[Varcont] = NouvelleValeur;
				if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_SUPERIEUREMENT;
				if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < 1.e-7 ) {
          X[Varcont] = 0.5 * ( Xmax[Varcont] + Xmin[Varcont] ) ;
          Xmin[Varcont] = X[Varcont];
          Xmax[Varcont] = X[Varcont];									
				}				
			}			
		}		
		else {
		  /* La contrainte est une contrainte de borne inf */		
			/*NouvelleValeur -= 1.e-9;*/
			if ( NouvelleValeur > Xmin[Varcont] ) {
	      printf("1- Xmin[%d]: %e -> %e\n",Varcont,Xmin[Varcont],NouvelleValeur);
        Xmin[Varcont] = NouvelleValeur;
				if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_INFERIEUREMENT;
				if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < 1.e-7 ) {
          X[Varcont] = 0.5 * ( Xmax[Varcont] + Xmin[Varcont] ) ;
          Xmin[Varcont] = X[Varcont];
          Xmax[Varcont] = X[Varcont];									
				}								
			}
		}
	}	
	else {
    /* La variable binaire n'est pas fixee */
		/* Fixation a 0 */
	  S0 = ( B - ( Coefficient[ilVarBin] * Xmin[Varbin] ) ) / Coefficient[ilVarcont];
		/* Fixation a 1 */
	  S1 = ( B - ( Coefficient[ilVarBin] * Xmax[Varbin] ) ) / Coefficient[ilVarcont];
		
    if ( Coefficient[ilVarcont] > 0 ) {
			/*NouvelleValeur += 1.e-9;*/
		  /* La contrainte est une contrainte de borne sup */
			NouvelleValeur = S0;
			if ( S1 > S0 ) NouvelleValeur = S1;
			if ( NouvelleValeur < Xmax[Varcont] ) {
	      printf("2- Xmax[%d]: %e -> %e\n",Varcont,Xmax[Varcont],NouvelleValeur);
        Xmax[Varcont] = NouvelleValeur;
				if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_SUPERIEUREMENT;
				if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < 1.e-7 ) {
          X[Varcont] = 0.5 * ( Xmax[Varcont] + Xmin[Varcont] ) ;
          Xmin[Varcont] = X[Varcont];
          Xmax[Varcont] = X[Varcont];									
				}							
			}									
		}
		else {
		  /* La contrainte est une contrainte de borne inf */
			NouvelleValeur = S0;
			if ( S1 < S0 ) NouvelleValeur = S1;
			/*NouvelleValeur -= 1.e-9;*/
			if ( NouvelleValeur > Xmin[Varcont] ) {
	      printf("2- Xmin[%d]: %e -> %e\n",Varcont,Xmin[Varcont],NouvelleValeur);
        Xmin[Varcont] = NouvelleValeur;
				if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;
				else if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_INFERIEUREMENT;
				if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < 1.e-7 ) {
          X[Varcont] = 0.5 * ( Xmax[Varcont] + Xmin[Varcont] ) ;
          Xmin[Varcont] = X[Varcont];
          Xmax[Varcont] = X[Varcont];									
				}					
			}							
		}		
	}			 	
}

return;

if ( Pne->ContraintesDeBorneVariable == NULL ) return;

ContraintesDeBorneVariable = Pne->ContraintesDeBorneVariable;

NombreDeContraintesDeBorne = ContraintesDeBorneVariable->NombreDeContraintesDeBorne;
First = ContraintesDeBorneVariable->First;
SecondMembre = ContraintesDeBorneVariable->SecondMembre;
Colonne = ContraintesDeBorneVariable->Colonne;
Coefficient = ContraintesDeBorneVariable->Coefficient;
LaContrainteDeBorneVariableEstDansLePool = Pne->ContraintesDeBorneVariable->LaContrainteDeBorneVariableEstDansLePool;
IndexLibre = Pne->ContraintesDeBorneVariable->IndexLibre;

TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;

Zero = 1.e-8;

/* La variable continue est toujours placee en premier */

for ( Cnt = 0 ; Cnt < NombreDeContraintesDeBorne ; Cnt++ ) {
  if ( First[Cnt] < 0 ) continue;	
  ilVarcont = First[Cnt];
	ilVarBin = ilVarcont + 1;
	
	Varcont = Colonne[ilVarcont];
	Varbin = Colonne[ilVarBin];

	if ( TypeDeBorne[Varbin] == VARIABLE_FIXE ) { ValeurDeVarBin = X[Varbin]; goto TestDeLaContrainteDeBorneVariable; }
	if ( Xmin[Varbin] == Xmax[Varbin] ) { ValeurDeVarBin = Xmin[Varbin]; goto TestDeLaContrainteDeBorneVariable; }
			
  continue;
	
	TestDeLaContrainteDeBorneVariable:
	
	if ( TypeDeBorne[Varcont] == VARIABLE_FIXE || Xmin[Varcont] == Xmax[Varcont] ) goto SupprimerLaContrainte;			
	
	SupprimerLaContrainteDeBorneVariable = NON_PNE;
	NouvelleValeur = ( SecondMembre[Cnt] - ( Coefficient[ilVarBin] * ValeurDeVarBin ) ) / Coefficient[ilVarcont];
	
  /* La variable binaire est fixee */
  if ( Coefficient[ilVarcont] > 0 ) {
		/* La contrainte est une contrainte de borne sup */
		if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
		  if ( NouvelleValeur < Xmax[Varcont] ) {
			  /*printf("Variable %d Xmax %e -> %e\n",Varcont,Xmax[Varcont],NouvelleValeur);*/
			  Xmax[Varcont] = NouvelleValeur;
				SupprimerLaContrainteDeBorneVariable = OUI_PNE;
			}
		}
		else {
			/*printf("Variable %d Xmax %e -> %e\n",Varcont,Xmax[Varcont],NouvelleValeur);*/		
      Xmax[Varcont] = NouvelleValeur;
			SupprimerLaContrainteDeBorneVariable = OUI_PNE;
			if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_SUPERIEUREMENT;
			else if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES;	
		}												  
	}
	else {
		/* La contrainte est une contrainte de borne inf */
		if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Varcont] == VARIABLE_BORNEE_INFERIEUREMENT ) {
		  if ( NouvelleValeur > Xmin[Varcont] ) {
			  /*printf("Variable %d Xmin %e -> %e\n",Varcont,Xmin[Varcont],NouvelleValeur);*/					
		    Xmin[Varcont] = NouvelleValeur;
				SupprimerLaContrainteDeBorneVariable = OUI_PNE;
			}
		}
		else {
			/*printf("Variable %d Xmin %e -> %e\n",Varcont,Xmin[Varcont],NouvelleValeur);*/						
      Xmin[Varcont] = NouvelleValeur;
			SupprimerLaContrainteDeBorneVariable = OUI_PNE;
			if ( TypeDeBorne[Varcont] == VARIABLE_NON_BORNEE ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_INFERIEUREMENT;
			else if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_SUPERIEUREMENT ) TypeDeBorne[Varcont] = VARIABLE_BORNEE_DES_DEUX_COTES ;							
		}				    					
	}

  if ( SupprimerLaContrainteDeBorneVariable == NON_PNE ) continue;
	
	if ( TypeDeBorne[Varcont] == VARIABLE_BORNEE_DES_DEUX_COTES ) {	
	  if ( fabs( Xmax[Varcont] - Xmin[Varcont] ) < Zero ) {
      Xmin[Varcont] = 0.5 * ( Xmax[Varcont] + Xmin[Varcont] );
			Xmax[Varcont] = Xmin[Varcont];
			X[Varcont] = Xmin[Varcont];
			/*printf("Variable %d fixee a %e\n",Varcont,Xmin[Varcont]);*/									
			TypeDeBorne[Varcont] = VARIABLE_FIXE;
		}
	}
  	
	SupprimerLaContrainte:

	/* On supprime la contrainte */
  CntDer = NombreDeContraintesDeBorne - 1;
	ilVarcontDer = First[CntDer];	
	ilVarBinDer =  ilVarcontDer + 1;
	Colonne[ilVarcont] = Colonne[ilVarcontDer];
  Coefficient[ilVarcont] = Coefficient[ilVarcontDer];
	Colonne[ilVarBin] = Colonne[ilVarBinDer];
  Coefficient[ilVarBin] = Coefficient[ilVarBinDer];
	SecondMembre[Cnt] = SecondMembre[CntDer];	
  LaContrainteDeBorneVariableEstDansLePool[Cnt] = LaContrainteDeBorneVariableEstDansLePool[CntDer];
	NombreDeContraintesDeBorne--;
	IndexLibre -= 2;
	Cnt--;   
  				 			
}

if ( NombreDeContraintesDeBorne != Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne ) *BornesModifiees = OUI_PNE;

Pne->ContraintesDeBorneVariable->NombreDeContraintesDeBorne = NombreDeContraintesDeBorne;
Pne->ContraintesDeBorneVariable->IndexLibre = IndexLibre;

return;
}

/*----------------------------------------------------------------------------*/

# endif
