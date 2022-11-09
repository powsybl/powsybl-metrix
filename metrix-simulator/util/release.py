#!/usr/bin/env python3

import os
import argparse
import git
import re
import fileinput

version_reg = re.compile(
    r"^\s*(?P<major>\d+)\.(?P<minor>\d+)\.(?P<patch>\d+)\s*$")
cmakelist_regex = re.compile(r"^(.*VERSION)\s*\d+\.\d+\.\d+")
release_note_regex = re.compile(r"(Release note for Version)\s*\d+\.\d+\.\d+")
doxyfile_regex = re.compile(r"(PROJECT_NUMBER\s*=)\s*\d+\.\d+\.\d+")


def get_parser():
    parser = argparse.ArgumentParser(
        description="Performs a release commit with the corresponding version")

    parser.add_argument("version", help="Version to release")

    return parser


def process_file(filename, regex, version_str):
    with fileinput.FileInput(filename, inplace=True) as f:
        for line in f:
            if re.match(regex, line) != None:
                line = re.sub(regex, r"\1 " + version_str, line)
            print(line, end='')

if __name__ == "__main__":
    parser = get_parser()
    options = parser.parse_args()
    version_match = re.match(version_reg, options.version)
    if version_match == None:
        raise Exception("Version format incorrect")

    repo_path = os.path.realpath(os.path.join(os.path.dirname(__file__), ".."))
    repo = git.Repo(repo_path)
    new_version_str = version_match.group(
        "major") + "." + version_match.group("minor") + "." + version_match.group("patch")
    version_tag = "v" + new_version_str
    if version_tag in repo.tags:
        raise Exception("Target tag already exists")

    print("Release version " + version_tag)

    release_branch = repo.create_head("release/" + version_tag)
    release_branch.checkout()

    # process files
    # process main cmakelist
    process_file(os.path.join(repo_path, "CMakeLists.txt"),
                 cmakelist_regex, new_version_str)

    # process release note
    process_file(os.path.join(repo_path, "release_note.txt"),
                 release_note_regex, new_version_str)

    # process doxyfile
    process_file(os.path.join(repo_path, "Doxyfile"),
                 doxyfile_regex, new_version_str)

    # commit and tag the changes
    repo.index.add([
        os.path.join(repo_path, "CMakeLists.txt"),
        os.path.join(repo_path, "release_note.txt"),
        os.path.join(repo_path, "Doxyfile")
    ])
    repo.index.commit("[automatic commit] Prepare for release " + version_tag)
    repo.create_tag(version_tag)
