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

#include "compute/isolver.h"
#include "config/solver_choice.h"

#include <memory>
#include <string_view>

namespace ortools
{
/**
 * @brief Build the OR-Tools backed solver for the given choice
 *
 * This is the only entry point the rest of metrix needs: it keeps every
 * OR-Tools include confined to src/ortools/solver.cpp, so callers can include
 * <metrix/log.h> in any order.
 *
 * @param solver_choice backend to use (must not be SIRIUS)
 * @param specific_params content of SOLVPARA, forwarded to the backend
 */
std::shared_ptr<compute::ISolver> makeSolver(config::SolverChoice solver_choice, std::string_view specific_params);
} // namespace ortools
