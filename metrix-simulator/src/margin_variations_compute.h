//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#pragma once

#include "pne.h"

#include <amd.h>
#include <exception>
#include <klu.h>
#include <sstream>
#include <vector>

/**
 * @brief Embed all elements required to build optimization problem for margin variations
 */
class MarginVariationMatrix
{
public:
    MarginVariationMatrix(int nbConstraints,
                          int nbComplementaryVar,
                          int nbVars,
                          const std::vector<int>& lineIndexes,
                          const std::vector<int>& columnIndexes,
                          const std::vector<int>& varPosition,
                          const std::vector<int>& nbTermesLine,
                          const std::vector<int>& numVarEnBaseDansB,
                          const std::vector<double>& constraintsMatrixCoeffs,
                          const std::vector<int>& baseComplement,
                          const std::vector<char>& sens);
    ~MarginVariationMatrix();

    klu_numeric* nMatrix() const { return numericMatrix_; }
    klu_symbolic* sMatrix() const { return symbolicMatrix_; }
    const klu_common& cParameters() const { return commonParameters_; }
    int nzz() const { return nz_; }


    /**
     * @brief Special exceptions thrown in constructor
     */
    struct Exception : public std::exception {
        explicit Exception(int info) : info{info} {}

        const char* what() const noexcept final
        {
            std::stringstream ss;
            ss << "Number of constraints is different from base size " << info;
            return ss.str().c_str();
        }

        int info;
    };

    int convertConstraintIndex(int num) const;

private:
    void init(int nbConstraints,
              int nbComplementaryVar,
              int nbVars,
              const std::vector<int>& lineIndexes,
              const std::vector<int>& columnIndexes,
              const std::vector<int>& varPosition,
              const std::vector<int>& nbTermesLine,
              const std::vector<int>& numVarEnBaseDansB,
              const std::vector<double>& constraintsMatrixCoeffs,
              const std::vector<int>& baseComplement,
              const std::vector<char>& sens);

private:
    klu_numeric* numericMatrix_;
    klu_symbolic* symbolicMatrix_;
    klu_common commonParameters_;
    int nz_;
    std::vector<int> BStartingIndexColumns_;
    std::vector<int> BLineIndex_;
    std::vector<double> BMatrixTerms_;
    std::vector<int> constraintsToIgnore_;
};
