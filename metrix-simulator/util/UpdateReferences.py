#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

#!/usr/bin/env python3

import os
import re
import shutil

out_regex = re.compile(r"out_s\d+")


def must_copy_file(basename):
    return basename == "metrixOut" or (out_regex.match(basename) != None)

if __name__ == "__main__":
    root_dir = os.path.realpath(os.path.join(os.path.dirname(__file__), ".."))
    root_dir_tests = os.path.realpath(os.path.join(
        os.path.dirname(__file__), "..", "tests"))

    for root, dir, files in os.walk(root_dir_tests):
        dest_root = root.replace("tests", "tests_reference")
        for file in files:
            file_without_ext = os.path.splitext(file)[0]
            if must_copy_file(file_without_ext):
                shutil.copyfile(os.path.join(root, file), os.path.join(dest_root, file))
