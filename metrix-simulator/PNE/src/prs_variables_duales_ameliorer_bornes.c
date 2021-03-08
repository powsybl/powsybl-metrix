/***********************************************************************

   FONCTION: Amelioration des bornes sur les variables duales.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "prs_sys.h"

# include "prs_fonctions.h"

# include "pne_define.h"

# include "prs_define.h"

# define VALEUR_NULLE 1.e-8
# define TRACES 0

# define UTILISER_LE_FORCING NON_PNE

# define PAS_DE_FORCING  0
# define FORCING_SUR_MIN_DE_LA_SOMME 1
# define FORCING_SUR_MAX_DE_LA_SOMME 2
# define FIXER_NON_INITIALISE 0
# define FIXER_AU_MIN 1
# define FIXER_AU_MAX 2

/*----------------------------------------------------------------------------*/

void PRS_AmeliorerBornesDesVariablesDuales( PRESOLVE * Presolve,
                                            int Var,
																						char TypeBrn,
																						double MinSommeDesAiFoisLambda,
																						char MinSommeDesAiFoisLambdaValide,
																						double MaxSommeDesAiFoisLambda,
																						char MaxSommeDesAiFoisLambdaValide,
																						int * NbModifications )
{
int il; int Cnt; double CoeffDeVar; PROBLEME_PNE * Pne; double * LambdaMin;
double * LambdaMax; double * Lambda; int * Cdeb; int * Csui; int * NumContrainte;
char * ContrainteInactive; char * ConnaissanceDeLambda; double * A; double X;
char SensCoutReduit; double CBarreMin; double CBarreMax;
# if UTILISER_LE_FORCING == OUI_PNE
  char Forcing; char Fixer;
# endif

if ( TypeBrn == VARIABLE_BORNEE_INFERIEUREMENT ) SensCoutReduit = '>';
else if ( TypeBrn == VARIABLE_BORNEE_SUPERIEUREMENT ) SensCoutReduit = '<';
else if ( TypeBrn == VARIABLE_NON_BORNEE ) SensCoutReduit = '=';
else return;

Pne = (PROBLEME_PNE *) Presolve->ProblemePneDuPresolve;    

Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
A = Pne->ATrav;

CBarreMin = Pne->LTrav[Var] - MaxSommeDesAiFoisLambda;
CBarreMax = Pne->LTrav[Var] - MinSommeDesAiFoisLambda;

LambdaMin = Presolve->LambdaMin;
LambdaMax = Presolve->LambdaMax;
Lambda = Presolve->Lambda;
ConnaissanceDeLambda = Presolve->ConnaissanceDeLambda;
ContrainteInactive = Presolve->ContrainteInactive;

# if UTILISER_LE_FORCING == OUI_PNE
  Forcing = PAS_DE_FORCING;
  if ( SensCoutReduit == '>' ) {
    if ( MinSommeDesAiFoisLambdaValide == OUI_PNE ) {
      if ( fabs( MinSommeDesAiFoisLambda - Pne->LTrav[Var] ) < VALEUR_NULLE ) {
	    	Forcing = FORCING_SUR_MIN_DE_LA_SOMME; goto SyntheseDuForcing;
		  }
	  }
  }
  else if ( SensCoutReduit == '<' ) {
    if ( MaxSommeDesAiFoisLambdaValide == OUI_PNE ) {
      if ( fabs( MaxSommeDesAiFoisLambda - Pne->LTrav[Var] ) < VALEUR_NULLE ) {
	  	  Forcing = FORCING_SUR_MAX_DE_LA_SOMME; goto SyntheseDuForcing;
		  }
  	}
  }
  else if ( SensCoutReduit == '=' ) {
    if ( MinSommeDesAiFoisLambdaValide == OUI_PNE ) {
      if ( fabs( MinSommeDesAiFoisLambda - Pne->LTrav[Var] ) < VALEUR_NULLE ) {
	  	  Forcing = FORCING_SUR_MIN_DE_LA_SOMME; goto SyntheseDuForcing;
		  }
	  }
    if ( MaxSommeDesAiFoisLambdaValide == OUI_PNE ) {
      if ( fabs( MaxSommeDesAiFoisLambda - Pne->LTrav[Var] ) < VALEUR_NULLE ) {
	  	  Forcing = FORCING_SUR_MAX_DE_LA_SOMME; goto SyntheseDuForcing;
		  }
	  }
  }
  SyntheseDuForcing:
  if ( Forcing != PAS_DE_FORCING ) {
    il = Cdeb[Var];
    while ( il >= 0 ) {
      Cnt = NumContrainte[il];
      if ( ContrainteInactive[Cnt] == OUI_PNE ) goto ContrainteSuivante_0;
	    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_CONNU ) goto ContrainteSuivante_0;
      CoeffDeVar = A[il];
		  Fixer = FIXER_NON_INITIALISE;
      if ( Forcing == FORCING_SUR_MIN_DE_LA_SOMME ) {
        if ( CoeffDeVar > 0 ) Fixer = FIXER_AU_MIN;									
			  else if ( CoeffDeVar < 0 ) Fixer = FIXER_AU_MAX;												
		  }
		  else if ( Forcing == FORCING_SUR_MAX_DE_LA_SOMME ) {
        if ( CoeffDeVar > 0 ) Fixer = FIXER_AU_MAX;									
		  	else if ( CoeffDeVar < 0 ) Fixer = FIXER_AU_MIN;												
      }
		  if ( Fixer == FIXER_AU_MIN ) { /* On fixe sur le min */
	      Lambda[Cnt] = LambdaMin[Cnt];
		    LambdaMin[Cnt] = Lambda[Cnt];
		    LambdaMax[Cnt] = Lambda[Cnt];
        ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;
		  	*NbModifications = *NbModifications + 1;				
      }
		  else if ( Fixer == FIXER_AU_MAX ) { /* On fixe sur le max */
	      Lambda[Cnt] = LambdaMax[Cnt];
		    LambdaMin[Cnt] = Lambda[Cnt];
		    LambdaMax[Cnt] = Lambda[Cnt];
        ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;
			  *NbModifications = *NbModifications + 1;					
      }		
      ContrainteSuivante_0:
      il = Csui[il];
    }
    return;
  }
# endif

if ( MinSommeDesAiFoisLambdaValide == OUI_PNE ) {
  if ( SensCoutReduit == '>' || SensCoutReduit == '=' ) {
	  /* Le cout reduit est soit positif ou nul, soit nul */
    il = Cdeb[Var];
    while ( il >= 0 ) {
      Cnt = NumContrainte[il];
      if ( ContrainteInactive[Cnt] == OUI_PNE ) goto ContrainteSuivante_1;
	    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_CONNU ) goto ContrainteSuivante_1;
      CoeffDeVar = A[il];
	    if ( fabs( CoeffDeVar ) < VALEUR_NULLE ) goto ContrainteSuivante_1;			
	    if ( CoeffDeVar > 0.0 ) {
		    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {					 
		      X = ( CBarreMax / CoeffDeVar ) + LambdaMin[Cnt];									
			    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
			      if ( X < LambdaMax[Cnt] ) {
              LambdaMax[Cnt] = X;
							*NbModifications = *NbModifications + 1;
						  # if TRACES == 1
                printf("LambdaMax ameliore sur la contrainte %d: %e\n",Cnt,LambdaMax[Cnt]);
						  # endif
			      }
				  }
				  else {
				    /* Borne sur LambdaMax */
            LambdaMax[Cnt] = X;
						*NbModifications = *NbModifications + 1;
					  # if TRACES == 1
              printf("LambdaMax ameliore sur la contrainte %d: %e\n",Cnt,LambdaMax[Cnt]);
					  # endif
            if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
			      else ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;					
				  }
			  }						
		  }
		  else {
		    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {					 		
		      X = ( CBarreMax / CoeffDeVar ) + LambdaMax[Cnt];							
			    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {				
			      if ( X > LambdaMin[Cnt] ) {
              LambdaMin[Cnt] = X;
						  *NbModifications = *NbModifications + 1;
						  # if TRACES == 1
                printf("LambdaMin ameliore sur la contrainte %d: %e\n",Cnt,LambdaMax[Cnt]);
					    # endif
			      }
				  }
				  else {
				    /* Borne sur LambdaMin */
            LambdaMin[Cnt] = X;
						*NbModifications = *NbModifications + 1;
					  # if TRACES == 1
              printf("LambdaMin ameliore sur la contrainte %d: %e\n",Cnt,LambdaMax[Cnt]);
					  # endif
            if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
			      else ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_CONNU;
				  }
			  }
		  }
      if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
        if ( fabs( LambdaMin[Cnt] - LambdaMax[Cnt] ) < VALEUR_NULLE ) {	
	        Lambda[Cnt] = 0.5 * ( LambdaMin[Cnt] + LambdaMax[Cnt] );
		      LambdaMin[Cnt] = Lambda[Cnt];
		      LambdaMax[Cnt] = Lambda[Cnt];
          ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;
					*NbModifications = *NbModifications + 1;
	      }
      }		
      ContrainteSuivante_1:	
      il = Csui[il];
	  }
  }
}

if ( MaxSommeDesAiFoisLambdaValide == OUI_PNE ) { 
  if ( SensCoutReduit == '<' || SensCoutReduit == '=' ) {
	  /* Le cout reduit est soit negatif ou nul, soit nul */
    il = Cdeb[Var];
    while ( il >= 0 ) {
      Cnt = NumContrainte[il];
      if ( ContrainteInactive[Cnt] == OUI_PNE ) goto ContrainteSuivante_2;
	    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_CONNU ) goto ContrainteSuivante_2;
      CoeffDeVar = A[il];
	    if ( fabs( CoeffDeVar ) < VALEUR_NULLE ) goto ContrainteSuivante_2;			
	    if ( CoeffDeVar > 0.0 ) {
		    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {					 		
			    X = ( CBarreMin / CoeffDeVar ) + LambdaMax[Cnt];						
			    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {							
			      if ( X > LambdaMin[Cnt] ) {
              LambdaMin[Cnt] = X;
				     	*NbModifications = *NbModifications + 1;
					    # if TRACES == 1
                printf("LambdaMin ameliore sur la contrainte %d: %e\n",Cnt,LambdaMax[Cnt]);
					    # endif
				    }
			    }
				  else {
				    /* Borne sur LambdaMin */
            LambdaMin[Cnt] = X;
				    *NbModifications = *NbModifications + 1;
					  # if TRACES == 1
              printf("LambdaMin ameliore sur la contrainte %d: %e\n",Cnt,LambdaMax[Cnt]);
				    # endif
            if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
			      else ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_CONNU;
				  }
			  }
		  }
		  else {
		    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {					 		
			    X = ( CBarreMin / CoeffDeVar ) + LambdaMin[Cnt];						
			    if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MAX_CONNU || ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {				
				    if ( X < LambdaMax[Cnt] ) {
              LambdaMax[Cnt] = X;
				      *NbModifications = *NbModifications + 1;
					    # if TRACES == 1
                printf("LambdaMax ameliore sur la contrainte %d: %e\n",Cnt,LambdaMax[Cnt]);
				      # endif
				    }
				  }
				  else {
				    /* Borne sur LambdaMax */
            LambdaMax[Cnt] = X;
				    *NbModifications = *NbModifications + 1;
					  # if TRACES == 1
              printf("LambdaMax ameliore sur la contrainte %d: %e\n",Cnt,LambdaMax[Cnt]);
				    # endif
            if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_CONNU ) ConnaissanceDeLambda[Cnt] = LAMBDA_MIN_ET_MAX_CONNU;
			      else ConnaissanceDeLambda[Cnt] = LAMBDA_MAX_CONNU;					
				  }
			  }
		  }
      if ( ConnaissanceDeLambda[Cnt] == LAMBDA_MIN_ET_MAX_CONNU ) {
        if ( fabs( LambdaMin[Cnt] - LambdaMax[Cnt] ) < VALEUR_NULLE ) {	
	        Lambda[Cnt] = 0.5 * ( LambdaMin[Cnt] + LambdaMax[Cnt] );
		      LambdaMin[Cnt] = Lambda[Cnt];
		      LambdaMax[Cnt] = Lambda[Cnt];
          ConnaissanceDeLambda[Cnt] = LAMBDA_CONNU;
				  *NbModifications = *NbModifications + 1;
	      }
      }				
      ContrainteSuivante_2:
      il = Csui[il];
	  }
  }
}
						
return;

}

