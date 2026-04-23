"""
Compare two MIP stats CSV files from different solvers.
Usage: python compare_mip_solvers.py <solver_a.csv> <solver_b.csv> [-o output.csv]

OUTPUT EXPLANATION
==================

Number of MIP solves compared:
    Number of (variant, micro_iteration) pairs present in BOTH CSV files.
    If different from input counts, some cases have no match (e.g., a variant
    failed or behaved differently with one solver).

Total time <solver>:
    Sum of all MIP solve times for each solver across all compared calls.

Overall speedup (A/B):
    Ratio of total times: total_time_A / total_time_B
    - > 1 means B is faster overall
    - < 1 means A is faster overall
    This metric is dominated by the longest MIP solves.

Average speedup per solve:
    Mean of individual speedups: for each MIP call, compute time_A / time_B,
    then average all values.
    Unlike overall speedup, small MIPs have equal weight as large ones.

Correlation (nb_integer_vars, speedup):
    Pearson correlation coefficient between the number of integer variables
    and the speedup (time_A / time_B).
    - 0       : No linear relationship
    - 0.3-0.5 : Moderate correlation
    - 0.5-0.7 : Significant correlation
    - 0.7-1.0 : Strong correlation
    A positive value means: more integer variables -> higher speedup for B.

Speedup by number of integer variables:
    Breakdown by buckets of integer variable count.
    - Bucket: Range of nb_integer_vars
    - Count: Number of MIP solves in this bucket
    - <solver> (ms): Total time spent by this solver on this bucket
    - Speedup: total_time_A / total_time_B for this bucket
      - > 1 means B is faster for this bucket
      - < 1 means A is faster for this bucket
"""

import argparse
import csv
import sys
from typing import Optional


def read_csv(filepath):
    """Read a CSV file and return a dict keyed by (variant, micro_iteration)."""
    results = {}
    with open(filepath, "r", newline="") as f:
        reader = csv.DictReader(f, delimiter=";")
        for row in reader:
            key = (int(row["variant"]), int(row["micro_iteration"]))
            results[key] = {
                "nb_variables": int(row["nb_variables"]),
                "nb_constraints": int(row["nb_constraints"]),
                "nb_integer_vars": int(row["nb_integer_vars"]),
                "time_ms": int(row["time_ms"]) if row["time_ms"] else None,
            }
    return results


def compute_comparison(data_a, data_b, name_a, name_b):
    """Compute comparison metrics between two solver runs."""
    results = []
    mismatches = []

    # Find common keys
    common_keys = set(data_a.keys()) & set(data_b.keys())

    if not common_keys:
        print("Warning: No common (variant, micro_iteration) found.", file=sys.stderr)
        return results, mismatches

    for key in sorted(common_keys):
        variant, micro_it = key
        a = data_a[key]
        b = data_b[key]

        # Check model consistency
        if (
            a["nb_variables"] != b["nb_variables"]
            or a["nb_constraints"] != b["nb_constraints"]
            or a["nb_integer_vars"] != b["nb_integer_vars"]
        ):
            mismatches.append(
                {
                    "variant": variant,
                    "micro_iteration": micro_it,
                    "nb_variables_a": a["nb_variables"],
                    "nb_variables_b": b["nb_variables"],
                    "nb_constraints_a": a["nb_constraints"],
                    "nb_constraints_b": b["nb_constraints"],
                    "nb_integer_vars_a": a["nb_integer_vars"],
                    "nb_integer_vars_b": b["nb_integer_vars"],
                }
            )

        time_a = a["time_ms"]
        time_b = b["time_ms"]

        # Skip if time is missing
        if time_a is None or time_b is None:
            continue

        # Compute metrics
        diff_ms = time_a - time_b
        speedup = time_a / time_b if time_b > 0 else None

        results.append(
            {
                "variant": variant,
                "micro_iteration": micro_it,
                "nb_variables": a["nb_variables"],
                "nb_constraints": a["nb_constraints"],
                "nb_integer_vars": a["nb_integer_vars"],
                f"time_ms_{name_a}": time_a,
                f"time_ms_{name_b}": time_b,
                "diff_ms": diff_ms,
                "speedup": round(speedup, 3) if speedup else None,
            }
        )

    return results, mismatches


def compute_stats(results, name_a, name_b):
    """Compute and return summary statistics as a string."""
    lines = []

    if not results:
        lines.append("No data to compute statistics.")
        return "\n".join(lines)

    n = len(results)
    total_time_a = sum(r[f"time_ms_{name_a}"] for r in results)
    total_time_b = sum(r[f"time_ms_{name_b}"] for r in results)

    avg_speedup = sum(r["speedup"] for r in results if r["speedup"]) / n

    # Correlation between nb_integer_vars and speedup
    int_vars = [r["nb_integer_vars"] for r in results]
    speedups = [r["speedup"] for r in results if r["speedup"]]

    if len(int_vars) > 1:
        mean_iv = sum(int_vars) / len(int_vars)
        mean_sp = sum(speedups) / len(speedups)

        cov = sum(
            (iv - mean_iv) * (sp - mean_sp) for iv, sp in zip(int_vars, speedups)
        ) / len(int_vars)
        std_iv = (sum((iv - mean_iv) ** 2 for iv in int_vars) / len(int_vars)) ** 0.5
        std_sp = (sum((sp - mean_sp) ** 2 for sp in speedups) / len(speedups)) ** 0.5

        correlation = cov / (std_iv * std_sp) if std_iv > 0 and std_sp > 0 else None
    else:
        correlation = None

    # Stats by nb_integer_vars buckets
    buckets = {}
    for r in results:
        iv = r["nb_integer_vars"]
        if iv == 0:
            bucket = "0"
        elif iv <= 5:
            bucket = "1-5"
        elif iv <= 10:
            bucket = "6-10"
        elif iv <= 20:
            bucket = "11-20"
        elif iv <= 50:
            bucket = "21-50"
        elif iv <= 100:
            bucket = "51-100"
        elif iv <= 200:
            bucket = "101-200"
        elif iv <= 500:
            bucket = "201-500"
        elif iv <= 1000:
            bucket = "501-1000"
        elif iv <= 1500:
            bucket = "1001-1500"
        elif iv <= 2000:
            bucket = "1501-2000"
        elif iv <= 3000:
            bucket = "2001-3000"
        elif iv <= 4000:
            bucket = "3001-4000"
        elif iv <= 5000:
            bucket = "4001-5000"
        elif iv <= 7500:
            bucket = "5001-7500"
        elif iv <= 10000:
            bucket = "7501-10000"
        else:
            bucket = ">10000"

        if bucket not in buckets:
            buckets[bucket] = {"count": 0, "total_a": 0, "total_b": 0}
        buckets[bucket]["count"] += 1
        buckets[bucket]["total_a"] += r[f"time_ms_{name_a}"]
        buckets[bucket]["total_b"] += r[f"time_ms_{name_b}"]

    # Build summary
    lines.append("")
    lines.append("=" * 60)
    lines.append("SUMMARY")
    lines.append("=" * 60)
    lines.append(f"Number of MIP solves compared: {n}")
    lines.append(f"Total time {name_a}: {total_time_a} ms")
    lines.append(f"Total time {name_b}: {total_time_b} ms")
    lines.append(
        f"Overall speedup ({name_a}/{name_b}): {total_time_a / total_time_b:.3f}"
    )
    lines.append(f"Average speedup per solve: {avg_speedup:.3f}")
    if correlation is not None:
        lines.append(f"Correlation (nb_integer_vars, speedup): {correlation:.3f}")
    lines.append("")

    lines.append("Speedup by number of integer variables:")
    lines.append("-" * 60)
    lines.append(
        f"{'Bucket':<10} {'Count':<8} {name_a + ' (ms)':<14} {name_b + ' (ms)':<14} {'Speedup':<10}"
    )
    lines.append("-" * 60)
    for bucket in [
        "0",
        "1-5",
        "6-10",
        "11-20",
        "21-50",
        "51-100",
        "101-200",
        "201-500",
        "501-1000",
        "1001-1500",
        "1501-2000",
        "2001-3000",
        "3001-4000",
        "4001-5000",
        "5001-7500",
        "7501-10000",
        ">10000",
    ]:
        if bucket in buckets:
            b = buckets[bucket]
            sp = b["total_a"] / b["total_b"] if b["total_b"] > 0 else 0
            lines.append(
                f"{bucket:<10} {b['count']:<8} {b['total_a']:<14} {b['total_b']:<14} {sp:<10.3f}"
            )
    lines.append("=" * 60)

    return "\n".join(lines)


def write_csv(data, output_path, name_a, name_b):
    """Write comparison data to CSV."""
    if not data:
        print("No data to write.", file=sys.stderr)
        return

    fieldnames = [
        "variant",
        "micro_iteration",
        "nb_variables",
        "nb_constraints",
        "nb_integer_vars",
        f"time_ms_{name_a}",
        f"time_ms_{name_b}",
        "diff_ms",
        "speedup",
    ]

    with open(output_path, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, delimiter=";")
        writer.writeheader()
        writer.writerows(data)

    print(f"Written {len(data)} lines to {output_path}")


def write_mismatches(mismatches, output_path):
    """Write model mismatches to a CSV file."""
    if not mismatches:
        return

    fieldnames = [
        "variant",
        "micro_iteration",
        "nb_variables_a",
        "nb_variables_b",
        "nb_constraints_a",
        "nb_constraints_b",
        "nb_integer_vars_a",
        "nb_integer_vars_b",
    ]

    with open(output_path, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, delimiter=";")
        writer.writeheader()
        writer.writerows(mismatches)

    print(f"Written {len(mismatches)} mismatches to {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Compare MIP stats from two solvers")
    parser.add_argument("csv_a", help="CSV file from solver A")
    parser.add_argument("csv_b", help="CSV file from solver B")
    parser.add_argument(
        "-o",
        "--output",
        default="comparison.csv",
        help="Output CSV file (default: comparison.csv)",
    )
    parser.add_argument(
        "--name-a",
        default="solver_a",
        help="Name for solver A (default: solver_a)",
    )
    parser.add_argument(
        "--name-b",
        default="solver_b",
        help="Name for solver B (default: solver_b)",
    )

    args = parser.parse_args()

    data_a = read_csv(args.csv_a)
    data_b = read_csv(args.csv_b)

    print(f"Loaded {len(data_a)} entries from {args.csv_a}")
    print(f"Loaded {len(data_b)} entries from {args.csv_b}")

    results, mismatches = compute_comparison(data_a, data_b, args.name_a, args.name_b)

    # Report mismatches
    if mismatches:
        print(
            f"\nWARNING: {len(mismatches)} model mismatches detected!", file=sys.stderr
        )
        for m in mismatches[:5]:
            print(
                f"  Variant {m['variant']}, micro-it {m['micro_iteration']}: "
                f"vars ({m['nb_variables_a']} vs {m['nb_variables_b']}), "
                f"constraints ({m['nb_constraints_a']} vs {m['nb_constraints_b']}), "
                f"int vars ({m['nb_integer_vars_a']} vs {m['nb_integer_vars_b']})",
                file=sys.stderr,
            )
        if len(mismatches) > 5:
            print(f"  ... and {len(mismatches) - 5} more", file=sys.stderr)
        print(file=sys.stderr)

        # Export mismatches to CSV
        mismatches_path = args.output.rsplit(".", 1)[0] + "_mismatches.csv"
        write_mismatches(mismatches, mismatches_path)

    write_csv(results, args.output, args.name_a, args.name_b)

    # Generate and display summary
    summary = compute_stats(results, args.name_a, args.name_b)
    print(summary)

    # Write summary to file
    summary_path = args.output.rsplit(".", 1)[0] + "_summary.txt"
    with open(summary_path, "w") as f:
        f.write(f"Comparison: {args.csv_a} vs {args.csv_b}\n")
        f.write(f"Solver A: {args.name_a}\n")
        f.write(f"Solver B: {args.name_b}\n")
        if mismatches:
            f.write(f"\nWARNING: {len(mismatches)} model mismatches detected!\n")
        f.write(summary)
    print(f"\nSummary written to {summary_path}")


if __name__ == "__main__":
    main()
