//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "margin_variations_compute.h"

#include <metrix/log.h>

#include <algorithm>

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
    numericMatrix_(nullptr),
    BMatrixTerms_(nbConstraints * nbConstraints, 0.)
{
    BStartingIndexColumns_.reserve(nbConstraints);
    BLineIndex_.reserve(nbConstraints * nbConstraints);

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
    if (numericMatrix_ != nullptr) {
        klu_free_numeric(&numericMatrix_, &commonParameters_);
    }
}

// ConvertConstraintIndex method aims at calculating the new index of the constraint in the constraint matrices for the
// post treatment of marginal variations
int MarginVariationMatrix::convertConstraintIndex(int num) const
{
    int idx = num;
    for (unsigned int i = 0; i < constraintsToIgnore_.size() && constraintsToIgnore_[i] < num; ++i) {
        idx--;
    }
    return idx;
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
    // Determine the baseSize
    int baseSize = nbComplementaryVar;
    for (int i = 0; i < nbVars && baseSize < nbConstraints; ++i) {
        if (varPosition[i] == EN_BASE) {
            baseSize++;
        }
    }
    // Determine the constraint to be ignored when not all constraints are in base
    if (baseSize > 0 && nbConstraints != baseSize) {
        // We look for the contraints that can be ignored (meaning that do not involve basic variables)
        for (int i = 0; i < nbConstraints; ++i) {
            if (sens[i] == '=') {
                bool ignore = true;
                for (int j = startLineIndexes[i]; j < startLineIndexes[i] + nbTermesLine[i]; ++j) {
                    if (varPosition[columnIndexes[j]] == EN_BASE && constraintsMatrixCoeffs[j] != 0) {
                        ignore = false;
                        break;
                    }
                }
                if (ignore) {
                    LOG(info) << "Constraint " << i << "is ignored for base size calulation";
                    constraintsToIgnore_.push_back(i);
                }
            }
        }
        // Throw exception when new number of constraints doesn't match baseSize
        if (nbConstraints - static_cast<int>(constraintsToIgnore_.size()) != baseSize) {
            throw Exception(baseSize);
        }
    }

    // Building a B sub matrix of the constraints matrix with the real basic constraint only
    //-------------------------------------------------------------------------------------
    int cnt = -1;
    for (int i = 0; i < nbConstraints; ++i) {
        if (std::find(constraintsToIgnore_.begin(), constraintsToIgnore_.end(), i) != constraintsToIgnore_.end()) {
            continue; // this constraint has to be ignored
        }
        BStartingIndexColumns_.push_back(++cnt * baseSize);
        // i : B line index
        // j : column index in constraint matrix
        // j1 : B column index in B
        int ideb = startLineIndexes[i];
        for (int k = 0; k < nbTermesLine[i]; ++k) {
            int j = columnIndexes[ideb + k];
            if (varPosition[j] == EN_BASE) { // we found a basic element
                int j1 = numVarEnBaseDansB[j];
                BMatrixTerms_[j1 * baseSize + cnt] = constraintsMatrixCoeffs[ideb + k];
            }
        }
    }
    // Treatment of the base complement
    int cpmBase = -1;
    for (int i = baseSize - nbComplementaryVar; i < baseSize; ++i) {
        cpmBase++;
        int ideb = BStartingIndexColumns_[i];
        for (int j = 0; j < baseSize; ++j) {
            BMatrixTerms_[ideb + j] = convertConstraintIndex(baseComplement[cpmBase]) == j ? (sens[j] == '>' ? -1 : 1)
                                                                                           : 0;
        }
    }

    // Building the tables from Bmatrix for the C KLU methods for the factorization:
    // inputs are:
    // - n the size of the matrix called here nz
    // - Ax table contains the non zero elements of the matrix, which is here BMatrixNonZeroTerms
    // - Ap table contains for each first element of each matrix colunm the value of the index of this element in Ax
    // , which is here BNonZeroStartingIndexColumns. Its size is n+1, starts always with 0 and end with Ax.size().
    // - Ai table contains for each non zero element in Ax the row index of this element in the matrix. We call this
    // table here BLineIndex_.
    std::vector<double> BMatrixNonZeroTerms;
    std::vector<int> BNonZeroStartingIndexColumns;
    int count = 0;
    BNonZeroStartingIndexColumns.push_back(0);
    for (int i = 0; i < baseSize; ++i) {
        for (int j = 0; j < baseSize; ++j) {
            if (BMatrixTerms_[i * baseSize + j] != 0) {
                BMatrixNonZeroTerms.push_back(BMatrixTerms_[i * baseSize + j]);
                count++;
                BLineIndex_.push_back(j);
            }
        }
        BNonZeroStartingIndexColumns.push_back(count);
    }

    nz_ = static_cast<int>(BNonZeroStartingIndexColumns.size()) - 1;
    // Callling klu methods:
    klu_defaults(&commonParameters_);
    // call to klu_analyze: klu_analyze(n, Ap, Ai,common);
    symbolicMatrix_ = klu_analyze(nz_, BNonZeroStartingIndexColumns.data(), BLineIndex_.data(), &commonParameters_);
    // call to klu_factor(Ap, Ai, Ax, Symbolic, &Common)
    numericMatrix_ = klu_factor(BNonZeroStartingIndexColumns.data(),
                                BLineIndex_.data(),
                                BMatrixNonZeroTerms.data(),
                                symbolicMatrix_,
                                &commonParameters_);
}