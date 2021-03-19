//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "converter.h"

#include <err/IoDico.h>
#include <err/error.h>
#include <metrix/log.h>

namespace config
{
namespace convert
{
int toInt(const std::string& str)
{
    try {
        return std::stoi(str);
    } catch (const std::invalid_argument& e) {
        LOG(error) << e.what();
        throw ErrorI(err::ioDico().msg("ERRFormatNombre", str));
    }
}

double toDouble(const std::string& str)
{
    try {
        return std::stod(str);
    } catch (const std::invalid_argument& e) {
        LOG(error) << e.what();
        throw ErrorI(err::ioDico().msg("ERRFormatNombre", str));
    }
}
} // namespace convert
} // namespace config