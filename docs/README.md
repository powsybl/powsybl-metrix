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

## Translations (English / French)

English (the Markdown files in `docs/`) is the only source to edit by hand. The French version is a translation stored as gettext catalogs
in `docs/locales/fr/LC_MESSAGES/` (one `.po` file per page). It is published on [powsybl-metrix-fr.readthedocs.io](https://powsybl-metrix-fr.readthedocs.io/).

To build the French documentation locally:
~~~bash
sphinx-build -a -D language=fr docs ./build-docs-fr
~~~
Any sentence without translation is displayed in English.

To build both languages at once, run from the `docs` folder `make html-all` (on Windows: `make.bat html-all`):
the results are in `docs/_build/html-en/index.html` and `docs/_build/html-fr/index.html`.

After modifying English pages, update the catalogs (this also creates the catalogs of new pages) from the `docs` folder:
~~~bash
make gettext-update        # on Windows: make.bat gettext-update
~~~
Then translate in the `.po` files the entries with an empty `msgstr` and review the ones flagged `#, fuzzy` (English text changed since
the translation was done; remove the `fuzzy` flag once the translation is up to date). Do not modify `msgid` lines: change the English page instead.
Use the glossary in `docs/locales/glossary.md` for consistent terminology. Do not translate code, file names, parameter names or MyST roles/directives.

The `.pot` templates and `.mo` files are generated and must not be committed. The CI checks that catalogs are up to date and fully translated
(`docs/scripts/check_translations.sh`).
