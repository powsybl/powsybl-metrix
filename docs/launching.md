The following environments variables must be defined in order to run metrix properly:
METRIX_ETC: location of the .dic files for language region (some of these dictionnaries are exported in 'etc' directory in install directory)

All options are detailed in the helper
```
$> ./metrix-simulator --help
Usage:
 metrix-simulator <errorFilepath> <variantFilepath> <resultsFilepath> <firstVariantIndex> <numberVariants> <paradesFilepath> 
Such as:
 - <errorFilepath>: a string representing the log file path
 - <variantFilepath>: a string representing the variants file path 
 - <resultsFilepath>: a string representing the prefix path for the results files
 - <firstVariantIndex>: an integer representing the index of the first variant (described in the <variantFilepath> file) to be considered
 - <numberVariants>: an integer representing the number of variants (described in the <variantFilepath> file) to be considered from the <firstVariantIndex>
 - <paradesFilepath> a string representing the parades file path (= "parades.csv" by default)

[options] 
Metrix options:
  -h [ --help ]                 Display help message
  --log-level arg               Logger level (allowed values are critical, 
                                error, warning, info, debug, trace): default is
                                info
  -p [ --print-log ]            Print developer log in standard output
  --verbose-config              Activate debug/trace logs relative to 
                                configuration
  --verbose-constraints         Activate debug/trace logs relative to 
                                constraint detection
  --write-constraints           Write the constraints in a dedicated file
  --print-constraints           Trace in logs the constraints matrix (time 
                                consuming even if trace logs are not active), 
                                log level at trace is required
  --write-PTDF                  Write the power transfer distribution factors 
                                matrix in a dedicated file
  --write-LODF                  Write the line outage distribution factors 
                                matrix report in a dedicated file
  --check-constraints-level arg Check adding constraints:
                                0: no check (default)
                                1: When adding a constraint, perform a load 
                                flow to check transit (more time consuming)
                                2: When adding a constraint, run every incident
                                to check that we didn't forget a constraint 
                                (even more time consuming
  --compare-reports             Compare load flow reports after application of 
                                report factors to check trigger of coupling
  --no-incident-group           Ignore incident if a group of N-K is not 
                                available
  --all-outputs                 Display all values in results files
  --mps-file                    Export MPS file
```
