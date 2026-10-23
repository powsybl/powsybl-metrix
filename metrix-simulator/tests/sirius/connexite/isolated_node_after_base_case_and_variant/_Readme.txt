Isolated node after connectedness lost in base variant and N-2 lines in 1st variant: 
-------------------------------------------------------------------------------------

The N-2 lines contingency "ARCOML31SSFL8 + MARGEL31ZSSAP" creates a lost network containing the 'ARCOM' and 'ZSSAP' nodes. 
One of the node (ZSSAP) becomes isolated when metrix creates the modified contingencies' list (the modified contingency being the N-1 line ARCOMSSFL8), because the SSFL7L31SSFL8 line has been opened in the base variant, but Metrix was doing the modified contingency assessment only on the N case network.

This test allows to verify the bug fix which assess the modified contingencies' list on the -1 base case network after the network topology modification and hence after the lost load network creation and the connectedness analysis. 

This bug fix then allows to solve the singular matrix problem due to the isolated node.
