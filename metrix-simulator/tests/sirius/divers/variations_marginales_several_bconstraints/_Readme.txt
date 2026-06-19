Tests for marginal variations detailed information:

In this test, the network is divided into 3 different connex zones, two of them are already balanced and without constraints.
However the main one gets two constraints due to a specific contingency ('LESPAL32SANIL'), and the marginal variations are asked for those two monitored branches (LESPAL31SANIL).

In the optimization problem, there are hence 5 constraints : 3 balancing constraints et 2 power flow constraints, but the optimization base matrix returned by the solver is of dimension 2.
This is explained by the fact balancing constraints 2 and 3 are useless (as the baalnce is already zero).
And when we solve the first power flow constraint, the second is already solved as the two lines are in parallel.
The problem is that the solver should theoretically returned 3 basic complementary variables and it only returns 1 (for the second power flow constraint).

The complementary basic variables are the gap variables that the solved adds to the inequality constraints to make them become equality constraints.
Yet the balancing constraints are already equality constraints hence no gap variables.

The proposed solution in the code, and that is illustrated through this example is that the equality constraints of such kind should be ignored
When this is a gap between the number of constraints and the size of the base matrix, we look for the equality constraints that doesn't involve basic variable and we ignore them.

In this example, only the detailed marginal variations related to the constrained monitored branch LESPAL31SANIL are finally printed.


