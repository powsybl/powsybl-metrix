#include "parades_configuration.h"

#include "converter.h"
#include "cte.h"
#include "parametres.h"
#include <err/IoDico.h>
#include <err/error.h>
#include <metrix/log.h>

#include <cstring>
#include <fstream>
#include <sstream>

namespace config
{
ParadesConfiguration::ParadesConfiguration(const std::string& pathname)
{
    std::ifstream fic(pathname);
    if (!fic) {
        // Sometimes parades are irrelevant but in that case, the file should be present and empty
        LOG_ALL(error) << err::ioDico().msg("ERRCurTopoLancementStd");
        return;
    }

    std::string line;
    std::string sub_line;
    std::istringstream iss;
    iss.exceptions(std::ifstream::eofbit | std::ifstream::badbit);

    try {
        if (!getline(fic, line)) {
            throw ErrorI(err::ioDico().msg("ERRLectureFichier", pathname));
        }
        iss.str(line);
        getline(iss, sub_line, ';');
        if (sub_line != "NB") {
            throw ErrorI(err::ioDico().msg("ERRMotCleNbVar", pathname));
        }
        // nb de parades
        getline(iss, sub_line, ';');
        auto nbParades = convert::toInt(sub_line);

        for (int num_parade = 0; num_parade < nbParades; num_parade++) {
            if (!getline(fic, line)) {
                throw ErrorI(err::ioDico().msg("ERRLectureFichier", pathname));
            }

            if (line.empty()) {
                // end of file before reading all parades
                return;
            }

            ParadeDef new_parade;

            iss.str(line);
            getline(iss, sub_line, ';');
            rtrim(sub_line);

            if (sub_line.find('|') != std::string::npos) {
                std::istringstream parser(sub_line);
                getline(parser, sub_line, '|');

                std::string constraint;
                while (sub_line.find('|') != std::string::npos) {
                    getline(parser, constraint, '|');
                    rtrim(constraint);
                    new_parade.constraints.insert(constraint);
                }
                getline(parser, constraint);
                rtrim(constraint);
                new_parade.constraints.insert(constraint);
            }
            new_parade.incident_name = sub_line;

            getline(iss, sub_line, ';');
            auto nbCouplages = convert::toInt(sub_line);

            for (int num_couplage = 0; num_couplage < nbCouplages; num_couplage++) {
                getline(iss, sub_line, ';');
                rtrim(sub_line);


                new_parade.couplings.push_back(sub_line);
            }

            parades_.push_back(std::move(new_parade));
        }
    } catch (const std::ios_base::failure& e) {
        LOG(error) << e.what();
        throw ErrorI(err::ioDico().msg("ERRLectFicPointVirgule", pathname));
    } catch (const err::Error& err) {
        // do nothing more: propagate error
        throw err;
    } catch (const std::exception& e) {
        LOG(error) << e.what();
        throw ErrorI(err::ioDico().msg("ERRLectureFichier", pathname)); // propagate error
    } catch (...) {
        LOG(error) << "Unknown error";
        throw ErrorI(err::ioDico().msg("ERRLectureFichier", pathname)); // propagate error
    }
}
} // namespace config