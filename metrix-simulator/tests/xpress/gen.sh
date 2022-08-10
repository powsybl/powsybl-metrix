#!/bin/bash

for fort in $(find . -name 'fort.json');
do
    echo fort: $fort
    jq -c '.files[0].attributes |= [{"name":"SOLVERCH","type":"INTEGER","valueCount":1,"firstIndexMaxValue":1,"secondIndexMaxValue":1,"firstValueIndex":1,"lastValueIndex":1,"values":[6]}] + .' "$fort" >"${fort}.test"
done
