# MultiCase Configuration (mapping)

A multi case configuration is made of 3 objects : 
- an iidm situation case
- a timeserie store (csv)
- a mapping groovy script using the [mapping dsl](#mapping-dsl) described below

The mapping process is mainly to be used within the metrix tool but it may be executed separatly
in order to check this temporary result.

With option `--mapping-synthesis-dir`, it will output a [synthesis](#synthesis) of the mapping, allowing you
to check the consistency of the produced multi case.\
If you want in depth mapping detail, the option `--check-equipment-time-series` will produce the full timeseries mapping.\
Lastly, the option `--network-output-dir` will output the network case with value mapped for the steps asked.


## Mapping dsl

The purpose of the mapping is to map time serie values to network elements. As it is a groovy
script, it can also be used to create composite time series or modify the network.

### General parameters

The general parameters can be used to configure globally the mapping.

```groovy
parameters {
    toleranceThreshold // Tolerance threshold when comparing the mapped power to the limit defined by the network element (default : 0.0001) 
}
``` 

### Mapping timeseries to network

There is one mapping function for each type of network elements

```groovy
mapToGenerators {...}
mapToLoads {...}
mapToHvdcLines {...}
mapToBoundaryLines {...}
mapToBreakers {...}
mapToPsts {...}
```

the general syntax is 

```groovy
mapToXXX {
 variable variableName //optional, the name of the element variable to map the timeseries with. Each element have a default mapped variable (defined below)
 timeSeriesName 'timeseriesName' // name of the timeseries to map
 filter {...} // a filter predicate allowing to select which item of the element type we wish to map
 distributionKey {...} // optional, a repartition key that will define how the timeserie values will be distributed for each selected items (default is equipartition : val / N)
}   
```

For the `mapToBreakers` function, the `variable` and `distributionKey` are always ignored.


#### Time series

In the `timeSeriesName` variable you can map any time series by referring to its name.\
Time series available must exist within the input data or created within the script. For more details about time series manipulation, refer to the [Time series section](https://www.powsybl.org/pages/documentation/data/timeseries).

Note that if the same time series (refered by its name) is used in multiple mapToXXX of same type, the
mapping behavior will be as if it was applied once on all of the elements selected by theses mapToXXX instructions.\
For instance, if we map a constant time series of value 100, to a group A and in another mapTo, to a group B,
then, with the default distributionKey, it will map the value 50 to each group. 


#### Variable

The `variable` allow to specify which attribute of the network element will be overriden by its mapped time series value.
It is an optional variable which values depends on the element type (default is in bold) :

- mapToGenerators : **targetP**, minP, maxP, targetQ
- mapToLoads : **p0**, fixedActivePower, variableActivePower, q0, fixedReactivePower, variableReactivePower
- mapToHvdcLines : **activePowerSetpoint**, minP, maxP
- mapToBoundaryLines : **p0**
- mapToBreakers : **open** (with 1 corresponding to a closed switch and 0 to open)
- mapToPsts: **currentTap**

For the loads, it is forbidden to map `p0` and either `fixedActivePower` or `variableActivePower`, as theses variables are linked (`p0 = Pfixed + Pvar`).
It is restricted to prevent incoherent mapping.\
If only one of `fixedActivePower` or `variableActivePower` is mapped, then the value of the other unmapped one will default to 0. 

### Filter

The `filter` variable is evaluated for every network item (of the requested type) and only those which
match will be mapped.

The filter must a groovy statement returning `true` or `false`. It has access to the same variables that of the main script plus 3 variables
related to the current object filtered. One of the three depends on the `mapToXXX` type:
- generator OR hvdcLine OR load OR boundaryLine OR pst OR breaker
- voltageLevel
- substation

Be careful of the syntax of the groovy statement as an assignation `load.id = 'conso'` instead of the comparison `load.id == 'conso'` would return something truth equal to true and then allowing all equipments to be mapped.

#### Examples

To filter the generators of the country 'FR' of energy source type 'THERMAL', we can define the following filter:
```groovy
filter {generator.terminal.voltageLevel.substation.country == FR && generator.energySource == EnergySource.THERMAL}
```
or 
```groovy
filter {substation.country == FR && generator.energySource == EnergySource.THERMAL}
```

To filter the generators connected to a list of substations :
```groovy
filter {['D.BURP7', 'D.BUXP7'].contains(generator.terminal.voltageLevel.id)}
```

To filter the loads of a particular region (assuming the iidm model has been extended with a custom property `region`):  
```groovy
filter {    ['05', '08', '09', '13', '14', '17', '24', '25', '28'].contains(load.voltageLevel.substation.region)}
```

To filter generators that belong to the main connected subnetwork : 
```groovy
filter {generator.terminal.busView.bus?.inMainConnectedComponent}
```


### Distribution key

The distribution key `distributionKey` allows to set a distribution weight for each item selected. Its content is a
groovy statement returning either a time series name or a integer. The value is then normalized so that the total sum for
the current mapToXXX filter is equal to 1.\
The additionnal variable accessible from the groovy statement is the same as the one found in the [filter](#filter) : the equipment variable (which name depends on the mapTo type).

Note that the `mapToBreakers` function does not support a distribution key as the mapped value will be either 1 or 0 (close/open) and will be applied to all filtered breakers.

#### Examples

To create a distribution key relative to the Pmax of each group :
```groovy
mapToGenerators {
    ...
    distributionKey {generator.maxP}
}
```

To create a distribution key relative to the power target:
```groovy
distributionKey {generator.targetP}
```

To create a distribution key defined by time series (assuming we have or created the time series SO_G1_key and SO_G2_key) :
```groovy
mapToGenerators {
    timeSeriesName 'SO_G' 
    variable targetP 
    filter { generator.id == 'SO_G1' || generator.id == 'SO_G2' } 
    distributionKey { generator.id + '_key' } 
}
```

To create a distribution key relative to the base load :
```groovy
mapToLoads {
    ...
    distributionKey {load.p0}    
}
``` 

### Unmapping

If we want to "unmap" items so that their value original static value. The main purpose (aside unmapping previously mapped items due to too broad filter maybe) is to prevent the selected items to appear in the "not mapped" section in the mapping synthesis that we will see later.
```groovy
unmappedXXX { // unmappedGenerators, unmappedLoads, …
   filter { … } // same usage as a normale mapToXXX
}
```

### Generators and HVDC limits

When mapping a time series to a generator or a HVDC, a time series value may break a threshold defined in the network case 
(a max or min power for instance). To handle this case we can choose to ignore the limit with either :
- a global parameter : use `--ignore-limits` in the CLI parameter
- a per time series configuration : add `ignoreLimits { "timeSeriesName" }` for each timeseries wanted

If no option is set to ignore the limits, then if it happens, the program will exit with an exception. Otherwise it will log a warning message.

## Examples

Time series mapping on a generator :
```groovy
mapToGenerators {
    timeSeriesName 'myTimeSeries'
    filter {generator.id=='GROUP_1'}
}
```

Time series mapping on the variable active power of a load :
```groovy
mapToLoads {
    variable variableActivePower
    timeSeriesName 'myTimeSeries'
    filter {load.id=='CONSO_1'}
}
```

Mapping on the load of a whole region (with custom iidm properties `region`):
```groovy
mapToLoads {
    timeSeriesName '15_fr_LOAD'
    filter {substation.region=='15_fr'}
    distributionKey { load.p0}
}
```


Mapping on breakers:
```groovy
mapToBreakers {
  timeSeriesName 'ARD_CT2018_PRATCP6'
  filter {breaker.id == 'PRATCP6_PRATC   6COUPL    DJ'}
}
```

Mapping on Phase Tap Shifters:
```groovy
mapToPsts {
    timeSeriesName 'N1_TD'
    filter {'TD_1'} 
}
```

With added properties in iidm we can, using the groovy language, map multiple configurations :
```groovy
for (area in ['05', '08', '09', '13', '14', '17', '24', '25', '28']) {
    // on définit des couples "AA" / "BB" pour mapper pour chaque zone, les chroniques contenant "AA" sur les générateurs de la zone de type "BB"
    for (prod_type in ['SOLAR', 'WIND', 'HYDRO', 'PSP', 'MISC', 'COAL', 'NUCLEAR', 'GAS']) {
        //on sélectionne la chronique de nom "nomDelaZone_AA"
        mapToGenerators {
            timeSeriesName area + '_fr_' + prod_type
            filter {
                // on mappe chaque chronique sélectionnée sur les générateurs qui sont dans la bonne région DI et de type "BB"
                substation.region == area && generator.genre == prod_type
            }
            distributionKey {
                generator.maxP
            }
        }  
    }
}
```

Mapping on multiple items (HVDC)
```groovy
mapToHvdcLines {
    timeSeriesName 'HVDC.DE32DE34'
    filter {hvdcLine.id == 'BRUN_GROS' || hvdcLine.id == 'WILS_GRAFEN'}
    distributionKey {1}
}
```


## Outputs

### Synthesis

L'onglet synthèse donne un aperçu des éléments de la situation réseau qui ont reçu ou non une chronique.

Les première lignes du tableau fournissent des indications précieuses pour corriger/valider le script :

    Le nombre de composants « mapped » : ce sont les éléments dont la puissance active est mappée par une et une seule chronique. Le nombre indiqué dans le tableau est utile dans le cas où on connaît le nombre visé (c’est souvent le cas pour les « HVDC lines » par exemple) ou si on connaît un ordre de grandeur du nombre recherché.
    Le nombre de composants « unmapped » et "ignored unmapped" : ce sont les éléments dont la puissance active est non mappée. Le non mapping de certains composants peut être voulu, par exemple une chronique de bilan appliquée sur les consommations laisse les productions de la zone inchangées. Dans ce cas, on préférera les indiquer explicitement dans le script avec la syntaxe "unmappedXXX", ils apparaissent alors comme "ignored unmapped". Il peut aussi s'agir d'un oubli ou d'une erreur.
    Le nombre de composants « multi-mapped » : Un multi-mapping peut être volontaire, par exemple on fait un premier mapping sur les chroniques de nucléaires agrégées puis on particularise certains sites. C’est la dernière TS utilisée qui mappe le composant, car il y a écrasement successif.
    Le nombre de composants « disconnected » : Les fonctions de mapping (mapToLoads,mapToGenerators...) ne s'appliquent jamais aux éléments déconnectés. En revanche, ils sont bien contenus dans les listes network.generators ou network.loads, ce qui peut entrainer des erreurs dans certains cas (par exemple le calcul de la production totale initiale de la situation initiale). Il est alors recommandé de filtrer les éléments déconnectés avec la syntaxe précisée sur cette page. Si on souhaite reconnecter des éléments, il faut le faire dans un script dédié et pas dans le script de mapping, cela ne fonctionnera pas correctement.
    Le nombre de composants « out of main connex composant » : Les fonctions de mapping (mapToLoads,mapToGenerators...) ne s'appliquent jamais aux éléments hors de la composante connexe principale. Comme pour les éléments déconnectés, ils sont bien contenus dans les liste network.generators ou network.loads, ce qui peut entrainer des erreurs dans certains cas (par exemple le calcul de la production totale initiale de la situation initiale). Il est alors recommandé de filtrer les éléments hors composante connexe principale avec la syntaxe précisée sur cette page. Si on souhaite reconnecter des éléments, il faut le faire dans un script dédié et pas dans le script de mapping, cela ne fonctionnera pas correctement.

Les lignes suivantes donnent le détail des éléments mappés pour les autres variables variable par variable


Les composants « multi-mapped » : Les fichiers : « loadToTimeSeriesMapping.csv », « generatorToTimeSeriesMapping.csv », etc … donnent : Colonne “generator” / colonne “Time series” : pour chaque générateur, nom de la(les) série(s) qui ont servi à le mapper. Quand un composant est mappé plusieurs fois, son nom apparaît une fois dans la colonne de gauche, et plusieurs fois de suite dans la colonne de droite. Donc une recherche de blancs dans la colonne de gauche permet de détecter et de vérifier que les TS avec lesquels il a été mappé sont légitimes. Un multi-mapping peut être volontaire, par exemple on fait un premier mapping sur les chroniques de nucléaires agrégées puis on particularise certains sites. C’est la dernière TS utilisée qui mappe le composant, car il y a écrasement successif.

    Les composants « disconnected » : Les fichiers « disconnectedLoads.csv », “disconnectedGenerators.csv”, etc… donnent la liste des consos, groupes etc… déconnectés. Les fonctions de mapping (mapToLoads,mapToGenerators...) ne s'appliquent jamais aux éléments déconnectés. En revanche, ils sont bien contenus dans les liste network.generators ou network.loads, ce qui peut entrainer des erreurs dans certains cas (par exemple le calcul de la production totale initiale de la situation initiale). Il est alors recommandé de filtrer les éléments déconnectés avec la syntaxe précisée sur cette page. Si on souhaite reconnecter des éléments, il faut le faire dans un script dédié et pas dans le script de mapping, cela ne fonctionnera pas correctement.

    Les composants « out of main connex composant » : Le tableau de synthèse fournit le nombre de composants trouvés hors de la composante connexe principale (NB: les composantes reliées uniquement par des HVDC sont considérées comme connexes contrairement à Convergence). Les fonctions de mapping (mapToLoads,mapToGenerators...) ne s'appliquent jamais aux éléments hors de la composante connexe principale. Comme pour les éléments déconnectés, ils sont bien contenus dans les liste network.generators ou network.loads, ce qui peut entrainer des erreurs dans certains cas (par exemple le calcul de la production totale initiale de la situation initiale). Il est alors recommandé de filtrer les éléments hors composante connexe principale avec la syntaxe précisée sur cette page. Si on souhaite reconnecter des éléments, il faut le faire dans un script dédié et pas dans le script de mapping, cela ne fonctionnera pas correctement.

Onglets Time series -> XXX

Pour vérifier les éléments affectés à une chronique : les onglets "Time series -> Charges", "Time series -> Générateurs" etc. donnent :

    le nom de la chronique
    la variable sur laquelle on l'a appliquée
    l'ensemble des éléments de la situation réseau sur lesquels on l'a appliquée

Onglets charges, générateurs, liaisons

Pour vérifier le ou les chroniques affectées à un élément : Les onglets "Charges", "Générateurs"  et "Laisons" donnent pour chaque élément de la situation réseau :

    ses caractéristiques principales dans la situation initiale : pays, poste, regionDI, P0...
    la variable qu'on a écrasé par une chronique
    le nom de la chronique appliquée. S'il y a "multi-mapping", plusieurs chroniques apparaissent. Le première de la liste est celle qui l'importe (c'est la dernière chronique appliquée dans le script)
    le statut : vide sauf pour les éléments hors composante connexe principale, déconnecté ou non mappés

Ces onglets permettent de vérifier sur des cas précis que le mapping s’applique bien comme souhaité : par exemple que la syntaxe de certains filtres complexes est la bonne.
Onglet bilan

Si le calcul du bilan est demandé lors de l'analyse de la muli-situations (le calcul est alors plus long), il est accessible dans cet onglet. La convention adoptée est bilan = consommation-production (attention signe inversé par rapport à CVG ou Antares). Un bilan de l'ordre de quelques MW traduit une multi-situations très bien calée.
Onglet Logs

Cet onglet n'est présent qu'en cas de calcul de bilan. Y sont indiqués d'éventuels conflits de Pmax/Pmin sur les groupes ou HVDC. Les lois de mapping entrainant des violations de Pmax sur certains pas de temps ne sont pas appliquées sur ces pas de temps, sauf si on coche l'option "ignore limits" dans le paramétrage de la multi-situations. Cette option permet d'augmenter automatiquement les Pmax des éléments dont ce paramètre n'a pas été mappé dans le script de configuration de multi-situations. Les messages sur les HVDC et les Pmin sont informatifs uniquement, si rien n'est fait, les valeurs seront écrêtées par Metrix.
### Chroniques à la maille équipement

Le script de mapping permet de demander la mise à disposition (pour export et analyse) de chroniques à la maille équipement (maille la plus fine : groupes, consommations, HVDC, disjoncteurs). Pour en disposer, il faut ajouter une instruction reprenant le format des mapTo... : indiquer les variables desquelles on souhaite récupérer les chroniques ainsi que les équipements via le filtrage. Ci-dessous, un exemple pour récupérer les consignes de production et les Pmax des groupes raccordés au poste #VERFEP3# et la puissance totale (p0) des consommations du même poste.
provideTsGenerators {
    variables targetP, maxP
    filter {
        generator.terminal.voltageLevel.id == "VERFEP3"
    }
}

provideTsLoads {
    variables p0
    filter {
        load.terminal.voltageLevel.id == "VERFEP3"
    }
}

Les différentes instructions possibles sont :
provideTsGenerators  {...}
provideTsLoads {...}
provideTsHvdcLines  {...}
provideTsBoundaryLines  {...}
provideTsBreakers  {...}
provideTsPsts {...}

Il est possible, de façon symétrique à mapTo[Equipement] d'omettre le champ variables : c'est alors la variable par défaut qui est retenue.
Pour récupérer les chroniques à la maille équipement, il est nécessaire de lancer un calcul de mapping avec calcul de bilan. Sinon, ces chroniques ne sont pas calculées.
Aide au calage d'un mapping
Le script n’aboutit pas :

- Erreur de syntaxe groovy : voir les erreurs courantes ici

- Erreurs de mapping, elles sont tracées dans l'onglet log:

    Une chronique non nulle est mappée sur un ensemble non nul d’éléments.

    "scaling down error" = Problème de Pmax ou Pmin sur un groupe ou une HVDC. Si l'application d'une chronique est impossible sans violer une Pmax/Pmin, la chronique n'est pas appliquée. L'option --ignore-limits est disponible pour ignorer ce contrôle et augmenter/diminuer automatiquement les pmax/pmin.

    "range warning" = Problème de Pmin. Si l'application d'une chronique implique une consigne nulle et non comprise entre Pmin et Pmax, un warning rest renvoyé dans le mapping-logs.

Vérification du tableau de synthèse
Des éléments sont déconnectés/hors de la composante connexe principale

Vérifier la liste grâce à la colonne statut des onglets Charges, Générateurs et Liaisons.

NB : les fonctions mapToXXX ne s’appliquent jamais à ces éléments, si on souhaite leur affecter une chronique et les connecter sur certains pas de temps, il faut les connecter dans la situation initiale.
Des éléments ne sont pas mappés

Est-ce voulu ? Vérifier grâce à la colonne statut des onglets Charges, Générateurs et Liaisons.

- oui c'est voulu :

    Ces éléments doivent-ils bien rester à leur valeur dans le cas de base (c'est ce qui sera fait par défaut)?
    Ex : une chronique de bilan appliquée sur les consommations laisse les productions de la zone inchangée
    Ces éléments doivent-ils être mis à zéro?

    On peut soit le faire en modifiant le fichier réseau soit en affectant une chronique nulle (ou fonction qui le fait tout seul à développer)

    Ex : un groupe qu’on ne souhaite pas prendre en compte

- non, dans ce cas :

    La loi de mapping a-t-elle était prévue ?
    Si oui, y-a-t-il une erreur qui fait que le filtre n’est pas appliqué ?

        Vérifier dans le fichier Time Series -> XXX si la chronique est appliquée à un autre élément

        -> si non, vérifier le filtre de la loi de mapping

        -> si oui, vérifier les caractéristiques de l'élément pour voir pourquoi il n’est pas pris dans le filtre (ex : mauvais typage d’un groupe)


Des éléments sont mappés plusieurs fois

Est-ce voulu ? Vérifier les éléments qui ont plusieurs chroniques dans les onglets Charges, Générateurs et Liaisons.

- oui c’est voulu. Vérifier l’ordre des lois de mapping, seule la dernière sera prise en compte
Ex : Une boucle permet de mapper toutes les consos comme dans Antares puis une seconde permet de particulariser quelques conso de la zone d’étude

- non c'est une erreur :

    Vérifier les filtres
    cas particulier où tous les éléments du réseau semblent retenu dans un filtre : il y a vraisemblablement une erreur de syntaxe qui fait que le filtre renvoie "vrai" pour tout, cas typique :generator.id='toto' au lieu de generator.id=='toto'
    Vérifier les caractéristiques de l’élément pour voir pourquoi il est pris dans le filtre (ex : mauvais typage d’un groupe)

Vérification du bilan

La chronique de bilan est-elle quasi nulle ?
- Non mais c’est voulu. Dans ce cas, est-elle bien ce qu’elle devrait être ? Penser à paramétrer Metrix correctement si on ne souhaite pas que l’équilibrage soit fait sur n’importe quels groupes.
Ex : un mélange de données Antares et historique. On peut choisir d’équilibrer sur un groupe loin de la zone d’étude

- Non et les listes d’éléments non mappés et mappés deux fois sont bonnes :

    Vérifier l'onglet logs
    Le fichier mapping.logs ne relève-t-il pas de problèmes de Pmax? Si une chronique n'est pas applicable sans violer une Pmax, elle n'est pas appliquée du tout.
    Quelle est l’allure de la chronique de bilan ?
    Une chronique quasiment constante est souvent due à des éléments de la situation initiale non mappés ou alors à un oubli de filtrer sur les éléments déconnectés lors d'une boucle sur les listes network.generators ou network.loads
    Pour une chronique de bilan non constante, il est très utile de regarder sa corrélation aux chroniques de la base d'entrée. Cela peut permettre de détecter des problèmes comme un oubli d'une chronique ou une erreur de signe.
    Si on a corrigé les chroniques importées, ont-elles les bonnes valeurs après ces corrections ? On peut les vérifier avec l’option XXX.
    Export vers CVG
    On peut exporter un ou des pas de temps vers Convergence si cela peut aider à comprendre le problème

Erreurs invisibles sur le tableau de synthèse et le bilan

- HVDC :
Des HVDC peuvent être modélisées par des prod ou des consos, on peut donc les mapper par erreur avec une chronique de prod ou de conso.
L’orientation des HVDC dans le fichier réseau et dans les chroniques peut être différente, il faut bien la vérifier.

Le bilan est réalisé pour toute la composante connexe principale sur toutes les zones synchrones d'un coup. Des erreurs sur les HVDC entre composantes synchrones ne sont donc pas vues. Elles le seront par Metrix dans ses sorties sur l'initial balancing.
