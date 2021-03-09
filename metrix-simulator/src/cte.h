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

#include <array>
#include <cstdio>
#include <string>

/**
 * @brief Namespace containing c format fonctions
 */
namespace cte
{
/**
 * @brief Format a string in "printf" format into a std::string
 *
 * The formatted string must be at most 1000 characters.
 */
template<class... Args>
std::string c_fmt(const char* format, Args... args)
{
    constexpr size_t max = 1000;
    std::array<char, max> buf;
    buf.fill('\0');

    snprintf(buf.data(), max, format, args...);

    return std::string(buf.data());
}
} // namespace cte