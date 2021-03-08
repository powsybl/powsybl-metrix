# ifdef __cplusplus
  extern "C"
	{
# endif
# ifndef FONCTIONS_PI_DEJA_DEFINIES
/*-----------------------------------------------------------------------------------------*/

# include "pi_definition_arguments.h"
# include "pi_define.h"

void PI_AugmenterLaTailleDeLaMatrice( PROBLEME_PI * );
   
void PI_Cremat( PROBLEME_PI * ); 

void PI_Crebis( PROBLEME_PI * ); 

void PI_CrematSystemeAugmente( PROBLEME_PI * ); 

void PI_CrebisSystemeAugmente( PROBLEME_PI * ); 

void PI_Sos1s2( PROBLEME_PI * ); 

void PI_Calmuk( PROBLEME_PI * );

void PI_Calcent( PROBLEME_PI * );

void PI_Caldel( PROBLEME_PI * );

void PI_Calstaf( PROBLEME_PI * );

void PI_Incrementation( PROBLEME_PI * );

void PI_CalculDesTeta( PROBLEME_PI * , double * , double * );

void PI_InitXS( PROBLEME_PI * );

void PI_SplitColonnes( PROBLEME_PI * );  

void PI_InverseDeMiseALEchelle( PROBLEME_PI * );

void PI_CalculerLeScaling( PROBLEME_PI * );      

void PI_Scaling( PROBLEME_PI * );

void PI_ArrondiEnPuissanceDe2( double * );

void PI_UnScaling( PROBLEME_PI * );

void PI_RestitutionDesResultats( PROBLEME_PI * , int , double * , double * , double * , int , double * );

void PI_Joptimise( PROBLEME_PI * );
 
void PI_InitATransposee( PROBLEME_PI * , int );

void PI_Qinit( PROBLEME_PI *, double , int , double , int , double , int );

void PI_MdEqua( PROBLEME_PI * , int   , double * , double * , double * , double * ,
                       int * , char   * , double * , int     , int   * , int * ,
		       int * , double * , double * , char   * );		       

void PI_MettreLaContrainteSousFormeStandard( PROBLEME_PI * , int ); 		       

void PI_SplitContraintes( PROBLEME_PI * );

void PI_AllocProbleme( PROBLEME_PI * , int , int , int * , char * , char * , int * ); 
	       
void PI_LibereProbleme( PROBLEME_PI * );

void PI_AllocMatrice( PROBLEME_PI * );

void PI_InitMatriceLU( PROBLEME_PI * , int * , int * , double * , int * );

void PI_LibereMatrice( PROBLEME_PI * );

void PI_Resolution( PROBLEME_PI * );  

void PI_ReconstruireLaMatriceDuSystemeAResoudre( PROBLEME_PI * Pi );

void PI_ResolutionSystemeAugmente( PROBLEME_PI * );

void PI_QuaminCalculs( PROBLEME_POINT_INTERIEUR * , PROBLEME_PI * );  
                                   	       
void PI_Quamin( PROBLEME_POINT_INTERIEUR * );

/***********************************************************/
double PI_Armijo( PROBLEME_PI * , double );
	  
void PI_CalculDesVariables( PROBLEME_PI * , double , double * , double * , double * , double * );

double PI_CalculNormeSecondMembre( PROBLEME_PI * , double , double * , double * , double * , double * , double * );
 
double PI_CalculDeLaDeriveeDuCritereEnZero( PROBLEME_PI * , double , double * , double * , double * , double * , double * , double * );

void PI_GradientConjugue( PROBLEME_PI * );
	
/*-----------------------------------------------------------------------------------------*/
# define FONCTIONS_PI_DEJA_DEFINIES
# endif
# ifdef __cplusplus
  }
# endif
