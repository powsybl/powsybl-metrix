//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "cte.h"

#include "err/error.h"

#include <array>
#include <cstdarg>
#include <cstring>

namespace cte
{
std::string c_fmt(const char* format, ...)
{
    const int max = 1000;
    std::array<char, max> buf;
    buf.fill('\0');
    va_list ap;
    va_start(ap, format);
    vsnprintf(buf.data(), max, format, ap);

    return std::string(buf.data());
}
} // namespace cte