# README

These are the documentation sources for PowSyBl-Metrix features.

Please keep them up to date with your developments.  
They are published on [powsybl-metrix.readthedocs.io](http://powsybl-metrix.readthedocs.io/) and pull requests are built and previewed automatically.

To build the docs locally, run the following commands:
~~~bash
pip install -r docs/requirements.txt
sphinx-build -a docs ./build-docs
~~~
Then open `build-docs/index.html` in your browser.

If you want to add links to another documentation, add the corresponding repository to the `conf.py` file.
In order to automatically get the version specified in the `pom.xml`, please use the same naming as the version: if you define the
PowSyBl-Core version with `<powsyblcore.version>`, then use `powsyblcore` as key. The specified URL should start with `https://` and end with `latest/` (the final `/` is mandatory).
For example, to add a link to the documentation of PowSyBl-Core, you need to add the following lines:
~~~python
# This parameter might already be present, just add the new value
intersphinx_mapping = {
    "powsyblcore": ("https://powsybl-core.readthedocs.io/en/latest/", None),
}
~~~

Then in your documentation file, you can add links to PowSyBl-Core documentation. If you want to link to a whole page,
use one of the following example:
~~~Markdown
- Let's talk about the power of {doc}`powsyblcore:simulation/loadflow/loadflow`. 
- Let's talk about the power of {doc}`loadflow <powsyblcore:simulation/loadflow/loadflow>`.
- Let's talk about the power of [Load Flow](inv:powsyblcore:std:doc#simulation/loadflow/loadflow).
~~~

If you want to link a specific part of a page, use one of those examples:
~~~Markdown
- Let's talk about the power of [time series](inv:#timeseries).
- Let's talk about the power of [time series](inv:powsyblcore#timeseries).
- Let's talk about the power of [calculated time series](inv:#calculated-timeseries).
- Let's talk about the power of [calculated time series](inv:powsyblcore:std:label:#calculated-timeseries).
- Let's talk about the power of [calculated time series](inv:powsyblcore:*:*:#calculated-timeseries).
~~~
*Note: for the last examples to work, there need to be a corresponding reference in the external documentation. 
For those examples, `(timeseries)=` and `(calculated-timeseries)=` have been added right before the corresponding titles 
in the [TimeSeries page](inv:powsyblcore:std:doc#data/timeseries). Another way to make it work is to use the `autosectionlabel` module in Sphinx to 
automatically generate anchors for each title.*

*Note²: if the build fails, try with the `-E` option to clear the cache:*
~~~bash
sphinx-build -a -E docs ./build-docs
~~~