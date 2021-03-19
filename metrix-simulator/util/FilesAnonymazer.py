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

import argparse
import sys
import os
import shutil
import re
import json

elem_reg = re.compile(r"([\w \._]+)")


class Mapping:
    """
    @brief Mapping class holding the anonymising mapping
    """

    def __init__(self):
        self.__mapping = dict()

    @staticmethod
    def __get_alpha__(num):
        """
        @brief Convert non-zero positive integer to string, equivalent to convert to 26-based representation

        @param num unique id of the element to convert

        @returns result of conversion
        """
        result = ""
        n = num
        while n > 0:
            n = n - 1
            remainder = int(n % 26)
            digit = chr(remainder + ord("A"))
            result = digit + result
            n = int((n - remainder) / 26)
        return result

    def has_key(self, key):
        return (key in self.__mapping.keys())

    def get_value(self, key, default):
        """
        @brief Retrieves value associated with key, with a default value returned in case key is not added
        """
        return self.__mapping.get(key, default)

    def get_create_value(self, prefix, key):
        """
        @brief Retrieves and creates if doesn't exist the value associated to a key

        calls __get_alpha__ with the current size of the mapping to generate the new key.
        This create a incremental sequence of items in the map

        @returns anonymized value associated with key
        """
        if not key:
            return ""
        return self.__mapping.setdefault(key, prefix + Mapping.__get_alpha__(len(self.__mapping.keys()) + 1))


class FileProcessor:
    """
    @brief Class holding the file processing
    """

    def __init__(self, mapping, in_place):
        self.__mapping = mapping
        self.__in_place = in_place

    @staticmethod
    def __isNumber__(str):
        try:
            float(str)
            return True
        except:
            return False

    def process_fort_file(self,  fortfilepath):
        """
        @brief Process fort.json file

        Since this file is a json, native json support is used

        @param fortfilepath the filepath
        """

        print("processing " + fortfilepath)
        output_dir = os.path.join(os.path.dirname(
            fortfilepath), "output")
        output_file = os.path.join(output_dir, "fort.json.tmp")
        keys_replace = {
            "CQNOMQUA": "QUAD_",   # Quads
            "DCNOMQUA": "LCC_",    # LCC
            "CGNOMREG": "REG_",    # regions
            "TRNOMGTH": "GROUP_",  # groups
            "TNNOMNOE": "CONSO_",  # consumptions
            "SECTNOMS": "SECT_",   # sections
            "DMNOMDEK": "INC_",    # incidents name
            "GBINDNOM": "GVAR_",   # coupled variables for groups
            "LBINDNOM": "CVAR_"    # coupled variables for consos
        }

        if not os.path.isdir(output_dir):
            os.mkdir(output_dir)

        with open(fortfilepath, 'r') as file:
            with open(output_file, 'w') as output:
                data = json.load(file)
                for idxf, f in enumerate(data["files"]):
                    for idxa, attr in enumerate(f["attributes"]):
                        if attr["name"].strip() in keys_replace.keys():
                            for idxv, val in enumerate(attr["values"]):
                                if not FileProcessor.__isNumber__(val.strip()):
                                    data["files"][idxf]["attributes"][idxa][
                                        "values"][idxv] = self.__mapping.get_create_value(keys_replace[attr["name"].strip()], val.strip())
                json.dump(data, output, indent=2)

        if self.__in_place:
            shutil.move(output_file, fortfilepath)

    def process_csv(self, filepath):
        """
        @brief Process file

        This will replace any found key in the file by their anonymized value. Only already created pairs will be used

        @param filepath the file path to process
        """

        print("processing " + filepath)

        if not os.path.exists(filepath):
            print(filepath + " doesn't exist: skip")
            return

        output_dir = os.path.join(os.path.dirname(
            filepath), "output")
        output_file = os.path.join(
            output_dir, os.path.basename(filepath) + ".tmp")

        if not os.path.isdir(output_dir):
            os.mkdir(output_dir)

        with open(filepath, 'r') as file:
            with open(output_file, 'w') as output:
                lines = file.readlines()
                for line in lines:
                    elems = line.split(";")
                    new_elems = []
                    for elem in elems:
                        if FileProcessor.__isNumber__(elem):
                            new_elems.append(str(elem))
                        else:
                            if elem.strip().startswith("+"):
                                # case only use in parades csv file
                                elem = elem[1:]
                                new_elems.append("+" + self.__mapping.get_value(
                                    elem.strip(), str(elem)))
                            else:
                                new_elems.append(self.__mapping.get_value(
                                    elem.strip(), str(elem)))
                    output.write(';'.join(new_elems))

        if self.__in_place:
            shutil.move(output_file, filepath)

    def process_file(self, filepath):
        """
        @brief Process file

        This will replace any found key in the file by their anonymized value. Only already created pairs will be used

        @param filepath the file path to process
        """
        def replace_mapping(match):
            if match.group(1) and mapping.has_key(match.group(1).strip()):
                return self.__mapping.get_value(match.group(1).strip(), match.group(1))
            else:
                return match.group(1)

        print("processing " + filepath)

        if not os.path.exists(filepath):
            print(filepath + " doesn't exist: skip")
            return

        output_dir = os.path.join(os.path.dirname(
            filepath), "output")
        output_file = os.path.join(
            output_dir, os.path.basename(filepath) + ".tmp")

        if not os.path.isdir(output_dir):
            os.mkdir(output_dir)

        with open(filepath, 'r') as file:
            with open(output_file, 'w') as output:
                lines = file.readlines()
                for line in lines:
                    line = elem_reg.sub(replace_mapping, line)
                    output.write(line)

        if self.__in_place:
            shutil.move(output_file, filepath)


def convert_to_reference_path(path):
    """
    @brief Build the reference path associated with current path

    @param path the path to convert

    Used for in place modification
    """
    return path.replace("tests", "tests_reference")


def createParser():
    """
    Create argument parser
    """
    parser = argparse.ArgumentParser()
    parser.add_argument("dir", help="directory to process")
    parser.add_argument(
        "-i", "--in-place", help="Modify the files in place", action="store_true")
    return parser

if __name__ == "__main__":
    parser = createParser()
    options = parser.parse_args()

    mapping = Mapping()

    ## Inputs ##
    processor = FileProcessor(mapping, options.in_place)

    # process_fort_file MUST be the first file to be processed as it is this
    # process that will fill the mapping
    processor.process_fort_file(os.path.join(options.dir, "fort.json"))
    #
    processor.process_csv(os.path.join(options.dir, "parades.csv"))
    processor.process_csv(os.path.join(options.dir, "VariantSet.csv"))

    ## Outputs ##
    reference_path = convert_to_reference_path(options.dir)

    processor.process_file(os.path.join(
        reference_path, "metrixOut.txt"))
    for file in os.listdir(reference_path):
        filename = os.fsdecode(file)
        if re.search(r"out_s\d+", filename):
            processor.process_csv(os.path.join(reference_path, filename))
