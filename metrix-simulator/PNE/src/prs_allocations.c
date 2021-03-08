/***********************************************************************

   FONCTION: Allocation des structures pour le Presolve 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"   

# include "pne_define.h"

# include "prs_define.h"

# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "prs_memoire.h"
# endif

/*----------------------------------------------------------------------------*/

void PRS_AllocationsStructure( PRESOLVE * Presolve )       
{
PROBLEME_PNE * Pne; int NombreDeVariables; int NombreDeContraintes; int NbCntAlloc;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;

NbCntAlloc = Pne->NombreDeContraintesAllouees;
NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

Presolve->MinContrainte = (double *) malloc( NbCntAlloc * sizeof( double ) );
Presolve->MaxContrainte = (double *) malloc( NbCntAlloc * sizeof( double ) );
Presolve->MinContrainteCalcule = (char *)malloc( NbCntAlloc * sizeof( char   ) );
Presolve->MaxContrainteCalcule = (char *)malloc( NbCntAlloc * sizeof( char   ) );

Presolve->Lambda = (double *) malloc( NbCntAlloc * sizeof( double ) );
Presolve->LambdaMin = (double *) malloc( NbCntAlloc * sizeof( double ) );
Presolve->LambdaMax = (double *) malloc( NbCntAlloc * sizeof( double ) );
Presolve->ConnaissanceDeLambda = (char *) malloc( NbCntAlloc * sizeof( char ) );

Presolve->ContrainteInactive = (char *) malloc( NbCntAlloc * sizeof( char ) );

Presolve->ContrainteBornanteSuperieurement = (int *) malloc( NombreDeVariables * sizeof( int  ) );
Presolve->ContrainteBornanteInferieurement = (int *) malloc( NombreDeVariables * sizeof( int  ) );
/* Indicateurs pour obliger a conserver les bornes du presolve. Cela peut etre necessaire lorsqu'on supprime une
   contrainte bornante */
Presolve->ConserverLaBorneSupDuPresolve = (char *) malloc( NombreDeVariables * sizeof( char ) );
Presolve->ConserverLaBorneInfDuPresolve = (char *) malloc( NombreDeVariables * sizeof( char ) );

Presolve->ParColonnePremiereVariable = (int *) malloc( (NombreDeContraintes+1) * sizeof( int ) );
Presolve->ParColonneVariableSuivante = (int *) malloc( NombreDeVariables * sizeof( int ) );

Presolve->ParLignePremiereContrainte = (int *) malloc( (NombreDeVariables +1) * sizeof( int ) );
Presolve->ParLigneContrainteSuivante = (int *) malloc( NombreDeContraintes * sizeof( int ) );

Presolve->VariableEquivalente = (int *) malloc( NombreDeVariables * sizeof( int ) );

/* Les bornes inf pour presolve et type de borne pour presolve sont toujours egales ou plus
   resserees que les valeurs natives */
Presolve->ValeurDeXPourPresolve = (double *) malloc( NombreDeVariables * sizeof( double ) );   
Presolve->BorneInfPourPresolve = (double *) malloc( NombreDeVariables * sizeof( double ) );   
Presolve->BorneSupPourPresolve = (double *) malloc( NombreDeVariables * sizeof( double ) );   
Presolve->TypeDeBornePourPresolve = (int *) malloc( NombreDeVariables * sizeof( int ) );   
Presolve->TypeDeValeurDeBorneInf = (char *) malloc( NombreDeVariables * sizeof( int ) );   
Presolve->TypeDeValeurDeBorneSup = (char *) malloc( NombreDeVariables * sizeof( int ) );

Presolve->NombreDeVariablesQueLaVariableSubstitue = (int *) malloc( NombreDeVariables * sizeof( int ) );

if ( Presolve->MinContrainte                    == NULL || Presolve->MaxContrainte                    == NULL || 
     Presolve->MinContrainteCalcule             == NULL || Presolve->MaxContrainteCalcule             == NULL ||
     Presolve->Lambda                           == NULL || Presolve->LambdaMin                        == NULL || 
     Presolve->LambdaMax                        == NULL || Presolve->ConnaissanceDeLambda             == NULL || 
     Presolve->ContrainteInactive               == NULL || Presolve->ContrainteBornanteSuperieurement == NULL ||
     Presolve->ParColonnePremiereVariable       == NULL || Presolve->ParColonneVariableSuivante       == NULL ||
     Presolve->ParLignePremiereContrainte       == NULL || Presolve->ParLigneContrainteSuivante       == NULL ||
     Presolve->VariableEquivalente              == NULL ||		 
		 Presolve->ContrainteBornanteInferieurement == NULL || Presolve->ConserverLaBorneSupDuPresolve    == NULL ||
     Presolve->ConserverLaBorneInfDuPresolve    == NULL || Presolve->ValeurDeXPourPresolve            == NULL ||
     Presolve->BorneInfPourPresolve             == NULL || Presolve->BorneSupPourPresolve             == NULL ||
     Presolve->TypeDeBornePourPresolve          == NULL || Presolve->TypeDeValeurDeBorneInf           == NULL || 
		 Presolve->TypeDeValeurDeBorneSup           == NULL || Presolve->NombreDeVariablesQueLaVariableSubstitue == NULL ) {
  printf(" Solveur PNE , memoire insuffisante. Sous-programme: PRS_AllocationsStructure \n");
  Pne->AnomalieDetectee = OUI_PNE;
  longjmp( Pne->Env , Pne->AnomalieDetectee ); 
}

memset( (char *) Presolve->VariableEquivalente, 0, NombreDeVariables * sizeof( int ) );

memset( (char *) Presolve->NombreDeVariablesQueLaVariableSubstitue, 0, NombreDeVariables * sizeof( int ) );

Presolve->PremierPassageDansLePresolve = OUI_PNE;

return;
}

/*----------------------------------------------------------------------------*/

void PRS_LiberationStructure( PRESOLVE * Presolve )
{
PROBLEME_PNE * Pne; 

if ( Presolve == NULL ) return;

# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE
	Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
  if ( Pne != NULL ) Pne->ProblemePrsDuSolveur = NULL;
  MEM_Quit( Presolve->Tas );
	return;
# endif

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;
if ( Pne != NULL ) Pne->ProblemePrsDuSolveur = NULL;
else return;

free( Presolve->MinContrainte );
free( Presolve->MaxContrainte );
free( Presolve->MinContrainteCalcule );
free( Presolve->MaxContrainteCalcule );
free( Presolve->Lambda );
free( Presolve->LambdaMin );
free( Presolve->LambdaMax );
free( Presolve->ConnaissanceDeLambda );
free( Presolve->ContrainteInactive );
free( Presolve->ContrainteBornanteSuperieurement );
free( Presolve->ContrainteBornanteInferieurement );
free( Presolve->ConserverLaBorneSupDuPresolve );
free( Presolve->ConserverLaBorneInfDuPresolve );
free( Presolve->ParColonnePremiereVariable );
free( Presolve->ParColonneVariableSuivante );
free( Presolve->ParLignePremiereContrainte );
free( Presolve->ParLigneContrainteSuivante );
free( Presolve->VariableEquivalente );
free( Presolve->ValeurDeXPourPresolve );
free( Presolve->BorneInfPourPresolve );
free( Presolve->BorneSupPourPresolve );
free( Presolve->TypeDeBornePourPresolve );
free( Presolve->TypeDeValeurDeBorneInf );
free( Presolve->TypeDeValeurDeBorneSup );
free( Presolve->NombreDeVariablesQueLaVariableSubstitue );

free( Presolve );

return;
}


