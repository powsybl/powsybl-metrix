/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

for (branch_component in network.branches) {
  branch(branch_component.id) {
   baseCaseFlowResults true
   maxThreatFlowResults true
 }     
}
