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
#include <ostream>
#include <string>

namespace config
{
/**
 * @brief Wrapper for programm definition, including version
 */
struct VersionDef {
    const char* date;
    const char* time;
    unsigned int major;
    unsigned int minor;
    unsigned int minor_bis;

    /**
     * @brief constexpr constructor to be able to call it at compile time
     */
    constexpr VersionDef(const char* date_build,
                         const char* time_build,
                         unsigned int major_version,
                         unsigned int minor_version,
                         unsigned int patch_version) :
        date(date_build),
        time(time_build),
        major{major_version},
        minor{minor_version},
        minor_bis{patch_version}
    {
    }

    /**
     * @brief Format the version display
     *
     * @returns the formatted version
     */
    std::string toString() const;
};

std::ostream& operator<<(std::ostream& os, const VersionDef& def);

} // namespace config