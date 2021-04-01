# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

function(GetPatchCommand dir projectName)
  if(UNIX)
    set(nullFile /dev/null)
  else()
    set(nullFile nul)
  endif()
  set(patchFile "${dir}/${projectName}/patch/${projectName}.patch")
  if(EXISTS ${patchFile})
      set(patchCommand git apply -p1 ${patchFile} --reverse --check 2> ${nullFile} || git apply --ignore-whitespace --whitespace=nowarn -p1 ${patchFile})
  endif()
  if(DEFINED patchCommand)
    set(${projectName}_patch ${patchCommand} PARENT_SCOPE)
  else()
    set(${projectName}_patch ${CMAKE_COMMAND} -E echo No common patch. PARENT_SCOPE)
  endif()
endfunction()
