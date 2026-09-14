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

#include <string_view>

namespace ortools
{
/**
 * @brief Emit a warning through metrix::log without exposing its macros
 *
 * The OR-Tools translation unit cannot include <metrix/log.h>: absl/log
 * (pulled by ortools/linear_solver/linear_solver.h) defines a `LOG` macro that
 * collides with metrix's, and the build runs with -Werror. This function is
 * implemented in a translation unit that includes only metrix headers.
 *
 * @param file  __FILE__ of the caller
 * @param line  __LINE__ of the caller
 * @param message text of the warning (dev log and result file)
 */
void logWarning(const char* file, int line, std::string_view message);
} // namespace ortools
