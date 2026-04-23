"""
Parse the metrix-simulator logs to extract MIP statistics.
Usage: python parse_mip_logs.py <fichier.log> [-o output.csv]
"""

import argparse
import csv
import re
import sys


def parse_log(filepath):
    """Parse the log file and extract the MIP stats."""

    # Pattern for the line of stats
    stats_pattern = re.compile(
        r"MIP stats: variante (\d+), micro-iteration (\d+), "
        r"nb variables = (\d+), nb contraintes = (\d+), "
        r"nb variables entieres actives = (\d+)"
    )

    # Pattern for the time line
    time_pattern = re.compile(
        r"MIP solve time: variante (\d+), micro-iteration (\d+), "
        r"(\d+) ms"
    )

    results = {}  # key = (variant, micro-it), value = data dict

    with open(filepath, "r") as f:
        for line in f:
            # Match stats
            match = stats_pattern.search(line)
            if match:
                variant = int(match.group(1))
                micro_it = int(match.group(2))
                key = (variant, micro_it)

                results[key] = {
                    "variant": variant,
                    "micro_iteration": micro_it,
                    "nb_variables": int(match.group(3)),
                    "nb_constraints": int(match.group(4)),
                    "nb_integer_vars": int(match.group(5)),
                    "time_ms": None,
                }

            # Match time
            match = time_pattern.search(line)
            if match:
                variant = int(match.group(1))
                micro_it = int(match.group(2))
                key = (variant, micro_it)

                if key in results:
                    results[key]["time_ms"] = int(match.group(3))

    return list(results.values())


def write_csv(data, output_path):
    """Writes the data to a CSV file."""

    if not data:
        print("No MIP data found in the log.", file=sys.stderr)
        return

    fieldnames = [
        "variant",
        "micro_iteration",
        "nb_variables",
        "nb_constraints",
        "nb_integer_vars",
        "time_ms",
    ]

    with open(output_path, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, delimiter=";")
        writer.writeheader()
        writer.writerows(data)

    print(f"Write {len(data)} lines in {output_path}")


def main():
    parser = argparse.ArgumentParser(
        description="Parse the metrix-simulator logs to extract MIP stats"
    )
    parser.add_argument("logfile", help="Log file to parse")
    parser.add_argument(
        "-o",
        "--output",
        default=None,
        help="Output CSV file (default: <logfile>.csv)",
    )

    args = parser.parse_args()

    output = args.output or args.logfile.rsplit(".", 1)[0] + ".csv"

    data = parse_log(args.logfile)
    write_csv(data, output)


if __name__ == "__main__":
    main()
