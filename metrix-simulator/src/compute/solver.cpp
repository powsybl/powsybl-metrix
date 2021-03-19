//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "solver.h"

#include <metrix/log.h>

namespace compute
{
void Solver::solve(PROBLEME_A_RESOUDRE* pne_problem) { PNE_Solveur(pne_problem); }

void Solver::solve(PROBLEME_SIMPLEXE* spx_problem)
{
    if (problem_ != nullptr) {
        LOG(warning) << "Solving a SIMPLEX problem without releasing the previous one: possible memory leak";
    }

    problem_ = SPX_Simplexe(spx_problem, nullptr);
}

void Solver::free()
{
    if (problem_ != nullptr) {
        SPX_LibererProbleme(problem_);
        problem_ = nullptr;
    }
}

} // namespace compute