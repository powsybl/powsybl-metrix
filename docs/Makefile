# Minimal makefile for Sphinx documentation
#

# You can set these variables from the command line, and also
# from the environment for the first two.
SPHINXOPTS    ?=
SPHINXBUILD   ?= sphinx-build
SOURCEDIR     = .
BUILDDIR      = _build

# Put it first so that "make" without argument is like "make help".
help:
	@$(SPHINXBUILD) -M help "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)

LANGUAGES     = fr
POTDIR        = $(BUILDDIR)/gettext

.PHONY: help Makefile gettext-update html-all

# Build the documentation in every language: _build/html-en and _build/html-fr
html-all:
	@$(SPHINXBUILD) -b html "$(SOURCEDIR)" "$(BUILDDIR)/html-en" $(SPHINXOPTS) $(O)
	@$(SPHINXBUILD) -b html -D language=fr "$(SOURCEDIR)" "$(BUILDDIR)/html-fr" $(SPHINXOPTS) $(O)

# Extract translatable messages from the English sources and update the .po catalogs
gettext-update:
	@$(SPHINXBUILD) -b gettext "$(SOURCEDIR)" "$(POTDIR)" $(SPHINXOPTS) $(O)
	sphinx-intl update -p "$(POTDIR)" $(addprefix -l ,$(LANGUAGES))


clean:
	@echo "Removing $(SOURCEDIR)/reference/api"
	@rm -rf "$(SOURCEDIR)/reference/api"
	@$(SPHINXBUILD) -M $@ "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)


# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option.  $(O) is meant as a shortcut for $(SPHINXOPTS).
%: Makefile
	@$(SPHINXBUILD) -M $@ "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)