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

#include <set>
#include <string>
#include <vector>

namespace config
{
class ParadesConfiguration
{
public:
    struct ParadeDef {
        std::string incident_name;          ///< Incident for which the parade is allowed
        std::set<std::string> constraints;  ///< Quadripol name for constraints
        std::vector<std::string> couplings; ///< Contain the coupling in raw format
    };

public:
    explicit ParadesConfiguration(const std::string& pathname);

    const std::vector<ParadeDef>& parades() const { return parades_; }

private:
    std::vector<ParadeDef> parades_;
};
} // namespace config