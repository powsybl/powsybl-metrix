Generation at zero cost test (section 5.A of the Metrix workshop):
-----------------------------------------------------------------


in this 6 nodes network test launched in OPF mode, we give metrix remedial actions (SS1 SS1 DJ OMN ; SOO1 SOO1 DJ OMN ; SS1 SS1 DJ OMN + SOO1 SOO1 DJ OMN and S SO 2) and preventive generation units (SO_G1, SE_G, SO_G2, N_G) with a zero price cost.

In the general behavior, the Metrix OPF would select the preventive generation unit as there are the cheapest option to respect the basecase and N-1 threshold. Metrix doesn't use the remedial actions, and use the generation units at 960 MW.

In the new Metrix version, with a noise cost of 0.5 euros/MW, the generation units with a zero price are given the preventive and curative cost of 0.5. This prevents the use of free redispatching, in order to be more realistic. 0.5 is a default value that ensures the use of TSO's free remedial actions first. 

In this example, the remedial actions are well selected for timestep 1 and 2, and some generation units are activated to complete the actions of the remedial action "S_SO_2" for timestep 3. In this timestep, we see that the global generation cost is hence : total_activated_generation_volume * 0.5 = 670.3 * 2 * 0.5 = 670.3 euros.




