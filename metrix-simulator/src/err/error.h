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

#include <exception>
#include <string>

/**
 * @brief Namespace managing the errors, exceptions and messages sent to the user of the programm
 */
namespace err
{
/**
 * @brief Main exception of metrix
 *
 * This class carries the filepath and line number in which it was thrown through the use of the @a ErrorI macro
 */
class Error : public std::exception
{
public:
    explicit Error(const std::string& msg, const std::string& file, int line) noexcept;

    const char* what() const noexcept final { return msg_.c_str(); }

private:
    void formatMsg(const std::string& msg, const std::string& file, int line);

private:
    std::string msg_;
};

} // namespace err

#define ErrorI(msg) err::Error(msg, __FILE__, __LINE__)
