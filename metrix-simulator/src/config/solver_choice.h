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

namespace config
{
/**
 * @brief Solver selected by SOLVERCH / PCSOLVER
 *
 * Kept in its own header, free of any metrix logging include, so that it can
 * be used by translation units that include OR-Tools (whose absl/log defines a
 * `LOG` macro colliding with the one of <metrix/log.h>).
 */
enum class SolverChoice {
    SIRIUS = 5,
    XPRESS = 6, // Must always be the last of the list
};
} // namespace config
