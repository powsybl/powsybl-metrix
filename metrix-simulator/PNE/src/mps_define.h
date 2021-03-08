//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

# define OUI_MPS  1
# define NON_MPS  0

# define REEL    1
# define ENTIER  2

# define NON_DEFINI 128

typedef struct {

int NentreesVar;
int NentreesCnt;

int NbVar;
int NbCnt;

int NbCntRange;

int CoeffHaschCodeContraintes;
int SeuilHaschCodeContraintes;
int CoeffHaschCodeVariables;
int SeuilHaschCodeVariables; 

int   *  Mdeb;
double *  A;      
int   *  Nuvar;  
int   *  Msui;   
int   *  Mder;
int   *  NbTerm;
double *  B;
char   *  SensDeLaContrainte;
double *  BRange; /* Pas nul si contrainte range */
double *  VariablesDualesDesContraintes;

char   ** LabelDeLaContrainte;
char   ** LabelDuSecondMembre;
char   *  LabelDeLObjectif;

char   ** LabelDeLaVariable; 

int   *  TypeDeVariable;  
int   *  TypeDeBorneDeLaVariable;  
double *  U;	     
double *  L; 	      
double *  Umin;	     
double *  Umax;	

int * FirstNomCnt;
int * NomCntSuivant;

int * FirstNomVar;
int * NomVarSuivant;

} PROBLEME_MPS;
