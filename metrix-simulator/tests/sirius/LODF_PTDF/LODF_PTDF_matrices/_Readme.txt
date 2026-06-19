Test based on the 6 nodes network from the Metrix tutorial (5A action):
------------------------------------------------------------------------

In this test, we launch the OPF mode on the 6 nodes network with remedial action and preventive generation means.

Some thresholds for the flow are imposed on S_SO_2 in N case and in N-1 case for S_SO_1 contingency.
We give some of the following topological actions:
S SO 1 ; 1 ; SS1 SS1 DJ OMN ; // 2 nodes in S substation
S SO 1 ; 1 ; SOO1 SOO1 DJ OMN ; // 2 nodes in SO substation
S SO 1 ; 2 ; SS1 SS1 DJ OMN ; SOO1 SOO1 DJ OMN ; // combination of two previous actions
S SO 1 ; 1 ; S SO 2 // open S_SO_2 line
We also authorize metrix to use the following generation units (SO_G1, SE_G, SO_G2, N_G) with a upward cost of 100 as preventive action only.

We aim here to export the PTDF matrix for all generation units and PSTs and the LODF matrix for each combinaison of incidents and branch/topological actions.
Two additional output files containing the matrices are printed.

The LODF matrix gives here the Line Outage Distribution Factor of the contingencies (S_SO_1)  for each monitored branch (S_SO_1 and S_SO_2) and each selected remedial action (SS1_SS1_DJ_OMN).

The PDTF matrix gives here the power transfer distribution factor of change in the generation units (N_G, SE_G, SO_G1, SO_G2), load (SO_L, SE_L1, SE_L2), PST (NA here) and HVDC (HVDC1, HVDC2) for each monitored branch (S_SO_1 and S_SO_2) and each selected remedial action (SS1_SS1_DJ_OMN). 


