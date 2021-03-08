//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

//-----------------------------------------------------------------------------
// Projet        : Metrix
// Sous-ensemble : cte (copie de CTE de Convergence)
//-----------------------------------------------------------------------------
// (c) RTE 2001
//-----------------------------------------------------------------------------
// Description :
//   Classe de gestion des dictionnaires
//-----------------------------------------------------------------------------

#include "IoDico.h"

#include "error.h"
#include <metrix/log.h>

#include <cstring>
#include <fstream>
#include <iostream>
#include <limits>
#include <ostream>

#define inclu(e, v1, v2) ((v1) < (v2) ? (e) >= (v1) && (e) <= (v2) : (e) >= (v2) && (e) <= (v1))

using std::ifstream;
using std::string;

namespace err
{
std::string IoDico::path_ = ".";

IoDico::IoDico() = default;

IoDico& IoDico::instance()
{
    static IoDico static_instance;
    return static_instance;
}

void IoDico::add(const string& filename)
{
    if (filename.empty()) {
        throw ErrorI("Empty Dictionnary name");
    }

    // Construire le basename du file
    char* langue = getenv("METRIX_DICO_LANGUE");
    string nom;
    string fic;
    if (langue != nullptr) {
        nom = filename + string("_") + langue + ".dic";
        fic = findFile(nom);
    }
    if (langue == nullptr || fic.empty()) {
        nom = filename + ".dic";
    }

    filename_ = nom;

    // Lecture du file
    findAndReadFile(nom);
}

string IoDico::findFile(const string& file)
{
    if (file.empty()) {
        return file;
    }

    string fic;
    ifstream in;

    fic = path_ + '/' + file;
    in.open(fic.c_str());

    if (!in.good()) {
        fic = "";
    }

    return fic;
}

string IoDico::findAndReadFile(const string& file)
{
    string fic = findFile(file);

    if (fic.empty()) {
        throw ErrorI("File '" + file + "' not found !\nCheck environement");
    }

    readFile(fic.c_str());

    return fic;
}

void IoDico::readFile(const char* filename)
{
    // Ouvrir le file
    ifstream in(filename);

    // Lecture
    if (in.bad()) {
        throw ErrorI("Cannot open '" + std::string(filename) + "'");
    }

    // Traiter chaque ligne
    bool ok = true;
    string key, phrase;

    while (!in.eof() && ok) {
        ok = processLine(in, key, phrase);

        if (ok && !key.empty()) {
            if (map_.find(key) != map_.end()) {
                throw ErrorI("Dictionnary reading: " + std::string(filename) + ", key non unique : " + key);
            }

            map_[key] = phrase;
        }
    }

    if (!ok) {
        throw ErrorI("Incorrect format of '" + std::string(filename) + "'");
    }

    in.close();
}

//-----------------------------------------------------------------------------
// Traiter une ligne du file
//-----------------------------------------------------------------------------
// Lit le flot in, et valorise key et value
// Syntaxe :
// Ligne = Blancs? '\n'
//         Blancs? '//' Commentaire '\n'
//         Blancs? Cle Blancs Valeur Blancs? '\n'
//
// Blancs  : un ou plusieurs ' ' ou '\t'
// Blancs? : rien ou Blancs
//
// Retour : true si paire lue correctement, false sinon

bool IoDico::processLine(ifstream& in, string& key, string& value)
{
    enum {
        init,     // etat initial : blancs de debut de ligne
        valkey,   // valorisation de la key, jusqu'au blanc
        sep,      // blancs entre key et phrase
        valvalue, // valorisation de la phrase, jusqu'au \n
        fin,      // fini !
        err
    } etat;

    key = "";
    value = "";
    char c;

    for (etat = init; etat != fin; /* rien */) {
        if (!(in.get(c))) {
            if (etat == init) {
                etat = fin;
            }
            break;
        }

        switch (etat) {
            case init:
                if (c == '/') {
                    in.get(c);
                    if (c == '/') {                                       // 2eme '/' ?
                        in.ignore(std::numeric_limits<int>::max(), '\n'); // oui : fin de ligne ignoree
                    } else                                                // non : debut de key
                    {
                        key = string("/") + c;
                        etat = valkey;
                    }
                } else if (isspace(c) != 0) {
                    // do nothing
                } else {
                    key = c;
                    etat = valkey;
                }
                break;

            case valkey:
                if (isspace(c) != 0) {
                    etat = sep;
                } else {
                    key += c;
                }
                break;

            case sep:
                if (isspace(c) != 0) {
                    // do nothing
                } else {
                    value = c;
                    etat = valvalue;
                }
                break;

            case valvalue:
                if (c == '\n') // fin de ligne precedee par \ ?
                {
                    if (!value.empty() && value[value.size() - 1] == '\\') {
                        value.erase(value.size() - 1);
                    } else {
                        etat = fin; // non : fin de la value
                    }
                } else if (c == 'n') // traiter le cas ou dans le message on a rajoute des \n
                {
                    if (!value.empty() && value[value.size() - 1] == '\\') {
                        value.erase(value.size() - 1);
                        value += '\n';
                    } else {
                        value += c;
                    }

                } else if (c == 't') // traiter le cas ou dans le message on a rajoute des \t
                {
                    if (!value.empty() && value[value.size() - 1] == '\\') {
                        value.erase(value.size() - 1);
                        value += '\t';
                    } else {
                        value += c;
                    }
                } else {
                    value += c;
                }
                break;
            default:
                // shouldn't happen
                break;
        }
    }

    return (etat == fin);
}

// Retourne le message correspondant a la key passee
string
IoDico::msg(const char* msgId, const string& p1, const string& p2, const string& p3, const string& p4, const string& p5)
{
    const string* tab[5] = {&p1, &p2, &p3, &p4, &p5};
    string val;

    if (map_.find(msgId) == map_.end()) {
        val = string(msgId) + " (label inconnu) $1 $2 $3 $4 $5";
    } else {
        val = map_[msgId];
    }

    // Traitement des $
    const char *d1 = val.c_str(), *d2;
    string phrase;
    int num;
    while (true) {
        d2 = strchr(d1, '$');
        if (d2 == nullptr) {
            phrase += string(d1);
            break;
        }

        if (strlen(d2 + 1) == 0 || *(d2 + 1) == '$') {
            phrase += string(d1, d2 - d1 + 1);
            d1 = d2 + 2;
        } else if (isdigit(*(d2 + 1)) == 0) {
            phrase += string(d1, d2 - d1 + 1);
            d1 = d2 + 1;
        } else // $1 ou $2 ...
        {
            phrase += string(d1, d2 - d1); // copie pre-$
            sscanf(d2, "$%d", &num);
            if (inclu(num, 1, 5)) {
                phrase += *tab[num - 1]; // copie de la reference
            }
            d1 = d2 + 2; // on passe le $%
        }
    }

    return phrase;
}

} // namespace err
