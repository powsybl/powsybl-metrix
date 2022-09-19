/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.powsybl.computation.CommandExecution;
import com.powsybl.computation.InputFile;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputDataGenerator;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

class MetrixInputDataGeneratorTest {
    private MetrixInputDataGenerator gen;
    private List<String> results;

    @BeforeEach
    public void initMetrixInputDataGenerator() {
        //GIVEN
        results = new ArrayList<>();
        gen = new MetrixInputDataGeneratorBuilder().conf(metrixConfig())
                .path(Paths.get("/testOut"))
                .fsu(fileSystem()).create();
    }

    @Test
    void initMetrixInputDataGeneratorWithoutFsu() {
        // GIVEN
        results = new ArrayList<>();

        // WHEN
        gen = new MetrixInputDataGenerator(metrixConfig(), Paths.get("/testOut"), null);

        // THEN
        Assertions.assertThat(gen.files).isEqualTo(MetrixInputDataGenerator.FileSystemUtils.DEFAULT);
    }

    @Test
    void copyDicShouldCopyFilesMetrixAnyDotDic() {
        // WHEN
        List<InputFile> actual = new ArrayList<>();
        gen.copyDic(actual);

        // THEN
        Assertions.assertThat(actual).hasSize(1);
        Assertions.assertThat(actual.get(0).getName(0)).isEqualTo("METRIXb.dic");
        Assertions.assertThat(results).hasSize(1);
        String fSep = FileSystems.getDefault().getSeparator();
        Assertions.assertThat(results.get(0)).isEqualTo("METRIXb.dic->" + fSep + "testOut" + fSep + "METRIXb.dic");
    }

    @Test
    void generateInputFileZipDoNotWriteFile() throws IOException {
        Path remedialActionFile = null;
        MetrixVariantProvider variantProvider = null;
        Network network = null;
        ContingenciesProvider contingenciesProvider = null;
        MetrixParameters parameters = null;
        MetrixDslData metrixDslData = null;

        //WHEN
        gen.generateInputFileZip(remedialActionFile, variantProvider, network, contingenciesProvider, parameters, metrixDslData);

        //THEN
        Assertions.assertThat(results).isEmpty();
    }

    @Test
    void generateInputFileZipDoNotCopyAdditionnalFiles() throws IOException {
        //GIVEN
        Path remedialActionFile = Paths.get("remedialActionFile");
        MetrixVariantProvider variantProvider = null;
        Network network = null;
        ContingenciesProvider contingenciesProvider = null;
        MetrixParameters parameters = null;
        MetrixDslData metrixDslData = null;

        //WHEN
        gen.generateInputFileZip(remedialActionFile, variantProvider, network, contingenciesProvider, parameters, metrixDslData);

        //THEN
        Assertions.assertThat(results).isEmpty();
    }

    @Test
    void generateInputFileZipCallVariantWriter() throws IOException {
        //GIVEN
        gen = new MetrixInputDataGeneratorBuilder().conf(metrixConfig())
                .path(Paths.get("/testOut"))
                .fsu(fileSystem())
                .writeVariantsInLogger((variants, writer, variantRange) -> results.add(variants.firstVariant() + " " + variants.lastVariant()))
                .create();

        Path remedialActionFile = null;
        MetrixVariantProvider variantProvider = new MetrixVariantProvider() {
            @Override
            public Range<Integer> getVariantRange() {
                return Range.open(1, 3);
            }

            @Override
            public TimeSeriesIndex getIndex() {
                return null;
            }

            @Override
            public Set<String> getMappedBreakers() {
                return null;
            }

            @Override
            public void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader, Path workingDir) {

            }
        };
        Network network = null;
        ContingenciesProvider contingenciesProvider = null;
        MetrixParameters parameters = null;
        MetrixDslData metrixDslData = null;

        //WHEN
        gen.generateInputFileZip(remedialActionFile, variantProvider, network, contingenciesProvider, parameters, metrixDslData);

        //THEN
        Assertions.assertThat(results).containsExactly("1 3");
    }

    @Test
    void generateInputFileZipCallNetworkWriter() throws IOException {
        //GIVEN
        Supplier<MetrixInputData> expected = () -> null;
        gen = new MetrixInputDataGeneratorBuilder().conf(metrixConfig())
                .path(Paths.get("/testOut"))
                .fsu(fileSystem())
                .createMetrixInputData((parameters, metrixDslData, metrixNetwork) -> expected)
                .writeNetworkInLogger((metrixInputData, angleDePerteFixe) -> {
                    if (metrixInputData == expected) {
                        results.add("ok");
                    }
                })
                .create();

        Path remedialActionFile = null;
        MetrixVariantProvider variantProvider = null;
        Network network = null;
        ContingenciesProvider contingenciesProvider = null;
        MetrixParameters parameters = null;
        MetrixDslData metrixDslData = null;

        //WHEN
        gen.generateInputFileZip(remedialActionFile, variantProvider, network, contingenciesProvider, parameters, metrixDslData);

        //THEN
        Assertions.assertThat(results).containsExactly("ok");
    }

    @Test
    void generateMetrixInputDataSimpleCommand() throws IOException {
        //GIVEN
        Path remedialActionFile = null;
        MetrixVariantProvider variantProvider = null;
        Network network = null;
        ContingenciesProvider contingenciesProvider = null;
        MetrixParameters parameters = null;
        MetrixDslData metrixDslData = null;

        //WHEN
        List<CommandExecution> commands = gen.generateMetrixInputData(remedialActionFile, variantProvider, network, contingenciesProvider, parameters, metrixDslData);

        //THEN
        Assertions.assertThat(commands.size()).isEqualTo(1);
        Assertions.assertThat(commands.get(0).getCommand().getInputFiles().size()).isEqualTo(2);
        Assertions.assertThat(commands.get(0).getCommand().getOutputFiles().size()).isEqualTo(2);
        Assertions.assertThat(commands.get(0).getCommand().getId()).isEqualTo("metrix");
    }

    @Test
    void generateMetrixInputDataMultipleVariantsCommand() throws IOException {
        //GIVEN
        gen = new MetrixInputDataGeneratorBuilder().conf(metrixConfig())
                .path(Paths.get("/testOut"))
                .fsu(fileSystem())
                .writeVariantsInLogger((variants, writer, variantRange) -> results.add(variants.firstVariant() + " " + variants.lastVariant()))
                .create();

        Path remedialActionFile = null;
        MetrixVariantProvider variantProvider = new MetrixVariantProvider() {
            @Override
            public Range<Integer> getVariantRange() {
                return Range.open(1, 5);
            }

            @Override
            public TimeSeriesIndex getIndex() {
                return null;
            }

            @Override
            public Set<String> getMappedBreakers() {
                return null;
            }

            @Override
            public void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader, Path workingDir) {

            }
        };
        Network network = null;
        ContingenciesProvider contingenciesProvider = null;
        MetrixParameters parameters = null;
        MetrixDslData metrixDslData = null;

        //WHEN
        List<CommandExecution> commands = gen.generateMetrixInputData(remedialActionFile, variantProvider, network, contingenciesProvider, parameters, metrixDslData);

        //THEN
        Assertions.assertThat(commands.size()).isEqualTo(1);
        Assertions.assertThat(commands.get(0).getCommand().getInputFiles().size()).isEqualTo(2);
        Assertions.assertThat(commands.get(0).getCommand().getOutputFiles().size()).isEqualTo(6);
        Assertions.assertThat(commands.get(0).getCommand().getId()).isEqualTo("metrix");
    }

    private MetrixInputDataGenerator.FileSystemUtils fileSystem() {
        return new MetrixInputDataGenerator.FileSystemUtils() {
            @Override
            public boolean isRegularFile(Path p) {
                return true;
            }

            @Override
            public Stream<Path> list(Path p) {
                return Stream.of(Paths.get("a.txt"), Paths.get("METRIXb.dic"), Paths.get("c.dic"));
            }

            @Override
            public Path copy(Path src, Path dest) {
                results.add(src.toString() + "->" + dest.toString());
                return null;
            }
        };
    }

    private MetrixConfig metrixConfig() {
        return new MetrixConfig() {
            @Override
            public Path getHomeDir() {
                return Paths.get("/test");
            }
        };
    }

    interface CreateNetwork {
        MetrixNetwork createNetwork(Path remedialActionFile, MetrixVariantProvider variantProvider, Network network, ContingenciesProvider contingenciesProvider, MetrixParameters parameters);
    }

    interface WriteVariantsInLogger {
        void writeVariantsInLogger(MetrixVariantProvider.Variants variants, MetrixVariantsWriter writer, Range<Integer> variantRange) throws IOException;
    }

    interface CreateMetrixInputData {
        Supplier<MetrixInputData> createMetrixInputData(MetrixParameters parameters, MetrixDslData metrixDslData, MetrixNetwork metrixNetwork);
    }

    interface WriteNetworkInLogger {
        void writeNetworkInLogger(Supplier<MetrixInputData> metrixInputData, boolean angleDePerteFixe) throws IOException;
    }

    static class MetrixInputDataGeneratorBuilder {
        MetrixConfig conf;
        Path path;
        MetrixInputDataGenerator.FileSystemUtils fsu;
        CreateNetwork createNetwork;
        WriteVariantsInLogger writeVariantsInLogger;
        CreateMetrixInputData createMetrixInputData;
        WriteNetworkInLogger writeNetworkInLogger;

        public MetrixInputDataGeneratorBuilder conf(MetrixConfig conf) {
            this.conf = conf;
            return this;
        }

        public MetrixInputDataGeneratorBuilder path(Path path) {
            this.path = path;
            return this;
        }

        public MetrixInputDataGeneratorBuilder fsu(MetrixInputDataGenerator.FileSystemUtils fsu) {
            this.fsu = fsu;
            return this;
        }

        public MetrixInputDataGeneratorBuilder writeVariantsInLogger(WriteVariantsInLogger writeVariantsInLogger) {
            this.writeVariantsInLogger = writeVariantsInLogger;
            return this;
        }

        public MetrixInputDataGeneratorBuilder createMetrixInputData(CreateMetrixInputData createMetrixInputData) {
            this.createMetrixInputData = createMetrixInputData;
            return this;
        }

        public MetrixInputDataGeneratorBuilder writeNetworkInLogger(WriteNetworkInLogger writeNetworkInLogger) {
            this.writeNetworkInLogger = writeNetworkInLogger;
            return this;
        }

        public MetrixInputDataGenerator create() {
            return new MetrixInputDataGenerator(conf, path, null, fsu) {
                @Override
                protected MetrixNetwork createNetwork(Path remedialActionFile, MetrixVariantProvider variantProvider, Network network, ContingenciesProvider contingenciesProvider, MetrixParameters parameters) {
                    return createNetwork != null ? createNetwork.createNetwork(remedialActionFile, variantProvider, network, contingenciesProvider, parameters) : null;
                }

                @Override
                protected void writeVariantsInLogger(MetrixVariantProvider.Variants variants, MetrixVariantsWriter writer, Range<Integer> variantRange) throws IOException {
                    if (writeVariantsInLogger != null) {
                        writeVariantsInLogger.writeVariantsInLogger(variants, writer, variantRange);
                    }
                }

                @Override
                protected Supplier<MetrixInputData> createMetrixInputData(MetrixParameters parameters, MetrixDslData metrixDslData, MetrixNetwork metrixNetwork) {
                    return createMetrixInputData != null ? createMetrixInputData.createMetrixInputData(parameters, metrixDslData, metrixNetwork) : null;
                }

                @Override
                protected void writeNetworkInLogger(Supplier<MetrixInputData> metrixInputData, boolean angleDePerteFixe) throws IOException {
                    if (writeNetworkInLogger != null) {
                        writeNetworkInLogger.writeNetworkInLogger(metrixInputData, angleDePerteFixe);
                    }
                }
            };
        }
    }
}
