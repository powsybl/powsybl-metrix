=================================
Redispatching offset test case :
=================================

In the general case, the adequacy and the rediqpatch cost offset are defined to ensure the user that no opportunity cost will lead metrix to choose generation means instead of network means. Indeed, when downward cost are negative on the balancing mechanism, it can happen that it is more interesting to decrease this geenration unit and increase another upward offer, for economical reason only. For example, if a generation unit produces at minimal power for 15 euros/MW, and another generation unit produces at maximal power at -25 euros/MW, Metrix will increase the first one and decrease the second as it allows a gain of 10 euros/MW.
Hence, the two options adequacyCostOffset and redispatchingCostOffset allows to define a translation cost for all generation units, in order to prevent opportunity while keeping the Metrix final cost indicators identical. 

In this particular case, we aims to validate this "no opportunity" behavior in the special case of negative load (demand side response). 
- when load is positive (load shedding) the cost offset is positive and added to the load cost. 
- when load is negative, the cost offset is negative and withdrawn to the load cost (which is also negative).
By doing this in the adequacy phase and redispatch phase, we ensure that Metrix will not use the load as first means for constraint.
In this non regression test, which is the 6 nodes network, negative load is defined but shouldn't be used, which proves that the cost offset is well set.
