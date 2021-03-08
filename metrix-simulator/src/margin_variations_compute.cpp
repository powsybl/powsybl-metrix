#include "margin_variations_compute.h"

MarginVariationMatrix::MarginVariationMatrix(int nbConstraints,
                                             int nbComplementaryVar,
                                             int nbVars,
                                             const std::vector<int>& startLineIndexes,
                                             const std::vector<int>& columnIndexes,
                                             const std::vector<int>& varPosition,
                                             const std::vector<int>& nbTermesLine,
                                             const std::vector<int>& numVarEnBaseDansB,
                                             const std::vector<double>& constraintsMatrixCoeffs,
                                             const std::vector<int>& baseComplement,
                                             const std::vector<char>& sens) :
    pmatrix_{nullptr},
    BValeurDesTermesDeLaMatrice_(nbConstraints * nbConstraints, 0.)
{
    BIndexDebutDesColonnes_.reserve(nbConstraints);
    BNbTermesDesColonnes_.reserve(nbConstraints);
    BIndicesDeLigne_.reserve(nbConstraints * nbConstraints);

    init(nbConstraints,
         nbComplementaryVar,
         nbVars,
         startLineIndexes,
         columnIndexes,
         varPosition,
         nbTermesLine,
         numVarEnBaseDansB,
         constraintsMatrixCoeffs,
         baseComplement,
         sens);
}

MarginVariationMatrix::~MarginVariationMatrix()
{
    if (pmatrix_ != nullptr) {
        LU_LibererMemoireLU(pmatrix_);
    }
}

void MarginVariationMatrix::init(int nbConstraints,
                                 int nbComplementaryVar,
                                 int nbVars,
                                 const std::vector<int>& startLineIndexes,
                                 const std::vector<int>& columnIndexes,
                                 const std::vector<int>& varPosition,
                                 const std::vector<int>& nbTermesLine,
                                 const std::vector<int>& numVarEnBaseDansB,
                                 const std::vector<double>& constraintsMatrixCoeffs,
                                 const std::vector<int>& baseComplement,
                                 const std::vector<char>& sens)
{
    int baseSize = nbComplementaryVar;
    for (int i = 0; i < nbVars && baseSize < nbConstraints; ++i) {
        if (varPosition[i] == EN_BASE) {
            baseSize++;
        }
    }

    if (baseSize > 0 && nbConstraints != baseSize) {
        throw Exception(Exception::Location::BASE_SIZE, baseSize);
    }

    // Init de la matrice Indices de lignes
    // En effet, il y a aussi des termes qui n'interviennt pas mais on les a chaines,
    // il faut donc bien initialiser BIndicesDeLigne_
    for (int i = 0; i < nbConstraints; ++i) {
        for (int j = 0; j < nbConstraints; ++j) {
            BIndicesDeLigne_.push_back(j);
        }
    }

    for (int i = 0; i < nbConstraints; ++i) {
        BIndexDebutDesColonnes_.push_back(i * baseSize);
        BNbTermesDesColonnes_.push_back(baseSize);
        // i : indice de la ligne de B
        // j : indice de la colonne de dans la matrice des contraintes
        // j1 : indice de la colonne de B
        int ideb = startLineIndexes[i];
        for (int k = 0; k < nbTermesLine[i]; ++k) {
            int j = columnIndexes[ideb + k];
            if (varPosition[j] == EN_BASE) { // on  a trouve un element
                int j1 = numVarEnBaseDansB[j];
                BValeurDesTermesDeLaMatrice_[j1 * baseSize + i] = constraintsMatrixCoeffs[ideb + k];
                // BIndicesDeLigne_[j1*nbConstraints+i]              = i;
            }
        }
    }
    // traitement du complement de la base
    int cpmBase = -1;
    for (int i = baseSize - nbComplementaryVar; i < baseSize; ++i) {
        cpmBase++;
        int ideb = BIndexDebutDesColonnes_[i];
        for (int j = 0; j < baseSize; ++j) {
            BValeurDesTermesDeLaMatrice_[ideb + j] = baseComplement[cpmBase] == j ? (sens[j] == '>' ? -1 : 1) : 0;
        }
    }


    B_.NombreDeColonnes = baseSize;
    B_.UtiliserLesSuperLignes = NON_LU;
    B_.ContexteDeLaFactorisation = LU_GENERAL;
    B_.FaireScalingDeLaMatrice = NON_LU;
    B_.UtiliserLesValeursDePivotNulParDefaut = OUI_LU;
    B_.LaMatriceEstSymetrique = NON_LU;
    B_.LaMatriceEstSymetriqueEnStructure = NON_LU;
    B_.FaireDuPivotageDiagonal = NON_LU;
    B_.SeuilPivotMarkowitzParDefaut = OUI_LU;
    B_.IndexDebutDesColonnes = &BIndexDebutDesColonnes_[0];
    B_.NbTermesDesColonnes = &BNbTermesDesColonnes_[0];
    B_.ValeurDesTermesDeLaMatrice = &BValeurDesTermesDeLaMatrice_[0];
    B_.IndicesDeLigne = &BIndicesDeLigne_[0];

    pmatrix_ = LU_Factorisation(&B_);

    if (B_.ProblemeDeFactorisation != NON_LU) {
        B_.FaireScalingDeLaMatrice = OUI_LU;
        pmatrix_ = LU_Factorisation(&B_);

        double pivot = 1.e-10;
        while (B_.ProblemeDeFactorisation != NON_LU && pivot > 1.e-15) {
            B_.UtiliserLesValeursDePivotNulParDefaut = NON_LU;
            B_.ValeurDuPivotMin = pivot;
            B_.ValeurDuPivotMinExtreme = pivot * 0.1;

            pmatrix_ = LU_Factorisation(&B_);

            pivot = pivot * 0.1;
        }

        if (B_.ProblemeDeFactorisation != NON_LU) {
            throw Exception(Exception::Location::FACTORIZATION, B_.ProblemeDeFactorisation);
        }
    }
}