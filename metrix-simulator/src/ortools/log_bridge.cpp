//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "log_bridge.h"

#include <metrix/log.h>

namespace ortools
{
void logWarning(const char* file, int line, std::string_view message)
{
    metrix::log::Logger::instance().log(
        metrix::log::severity::warning, file, line, metrix::log::Logger::TargetOutput::ALL)
        << message;
}
} // namespace ortools
