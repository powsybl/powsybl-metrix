Use of ITAM threshold only test :
-----------------------------------------------------------------

In this 6 nodes network test launched in OPF mode (we use the network and actions from section 5.A of the Metrix workshop for timestep 1 only), we give metrix remedial actions (SS1 SS1 DJ OMN ; SOO1 SOO1 DJ OMN ; SS1 SS1 DJ OMN + SOO1 SOO1 DJ OMN and S SO 2) and preventive generation units (SO_G1, SE_G, SO_G2, N_G). We define however the following threshold for  branch 'S_SO_2' (no N threshold, seuilAM = 480 for the three timesteps):
   baseCaseFlowResults true // résultats en N
   maxThreatFlowResults true // résultats sur N-k
   branchRatingsBeforeCurative 'seuilAM' // seuil en N-k before curative action

The branchRatingsOnContingency threshold is not defined, which can be seen by a 99999 value of QATI5MNS in the variantSet file. 

In the general behavior, the Metrix OPF would not detect any constraint for the beforecurative threshold as this threshold depends on the Aftercontingency threshold detection. We can see that no constraint is detected.

In the new Metrix version, this ITAM threshold is well detected but the Aftercontingency one (branchRatingsOnContingency) is not; which leads to the use of preventive actions such as preventive generation but no curative actions such as remedial actions. The behavior of ITAM detection without curative action is validated.





