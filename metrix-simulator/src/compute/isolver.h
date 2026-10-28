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

#include <pne.h>

namespace compute
{
class ISolver
{
public:
    virtual ~ISolver() = default;

    virtual void solve(PROBLEME_A_RESOUDRE* pne_problem) = 0;
    virtual void solve(PROBLEME_SIMPLEXE* spx_problem) = 0;

    virtual void free() = 0;
};
} // namespace compute
