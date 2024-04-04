
parameters{
    computationType OPF
    overloadResultsOnly false
    withAdequacyResults true
    withRedispatchingResults true 
	adequacyCostOffset 0
}


generator('SO_G1') {
    adequacyDownCosts (100)
    adequacyUpCosts 100
    redispatchingDownCosts (120)
    redispatchingUpCosts 100
}

generator('SO_G2') {
    adequacyDownCosts (120)
    adequacyUpCosts 120
    redispatchingDownCosts (130)
    redispatchingUpCosts 120
}

generator('SE_G') {
    adequacyDownCosts (130)
    adequacyUpCosts 130
    redispatchingDownCosts (140)
    redispatchingUpCosts 130
}

generator('N_G') {
    adequacyDownCosts (140)
    adequacyUpCosts 140
    redispatchingDownCosts (150)
    redispatchingUpCosts 140
}

generator('new_gen ') {
    adequacyDownCosts (90)
    adequacyUpCosts 90
    redispatchingDownCosts (150)
    redispatchingUpCosts 150
}
