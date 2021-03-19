//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "version_def.h"

#include <sstream>

namespace config
{
std::ostream& operator<<(std::ostream& os, const VersionDef& def)
{
    os << " v" << def.major << "." << def.minor << "." << def.minor_bis << " (build " << def.date << " at " << def.time
       << ")";
    return os;
}

std::string VersionDef::toString() const
{
    std::ostringstream ss;
    ss << *this;
    return ss.str();
}
} // namespace config