# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

include(CMakeParseArguments)

function(get_url)
  set(options NO_OPTIONS)
  set(oneValueArgs NAME URL NNI PASSWORD)
  set(multiValueArgs NO_MULTI_VALUE_OPTIONS)
  cmake_parse_arguments(GetUrl "${options}" "${oneValueArgs}" "${multiValueArgs}" ${ARGN})

  if (NOT GetUrl_URL)
    message(FATAL_ERROR "URL not specified")
  endif()
  if (NOT GetUrl_NAME)
    message(FATAL_ERROR "NAME not specified")
  endif()
  string(TOUPPER ${GetUrl_NAME} upperName)

  string(LENGTH ${GetUrl_URL} length)

  # Get the protocol
  string(FIND ${GetUrl_URL} "://" pos)
  string(SUBSTRING ${GetUrl_URL} 0 ${pos} protocol)

  # Get the server
  MATH(EXPR start "${pos} + 3")
  MATH(EXPR end "${length} - ${start}")
  string(SUBSTRING "${GetUrl_URL}" ${start} ${end} server)

  if (GetUrl_NNI AND GetUrl_PASSWORD)
    set(${upperName}_URL "${protocol}://${GetUrl_NNI}:${GetUrl_PASSWORD}@${server}" PARENT_SCOPE)
  elseif (GetUrl_NNI AND NOT GetUrl_PASSWORD)
    set(${upperName}_URL "${protocol}://${GetUrl_NNI}@${server}" PARENT_SCOPE)
  else()
    set(${upperName}_URL ${GetUrl_URL} PARENT_SCOPE)
  endif()

endfunction()
