package com.powsybl.metrix.integration.remedials;

import com.powsybl.commons.PowsyblException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.*;

public class RemedialReaderTest {

    @Test
    public void parseFileTest() throws URISyntaxException, IOException {
        File file = new File(getClass().getResource("/remedial.txt").toURI());
        String s = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        List<Remedial> remedials = RemedialReader.parseFile(s);
        List<Remedial> expectedList = Arrays.asList(
                new Remedial(2, "S_SO_1", Collections.emptyList(), Collections.singletonList("SOO1_SOO1_DJ_OMN"), Collections.emptyList()),
                new Remedial(3, "S_SO_1", Collections.emptyList(), Arrays.asList("SS1_SS1_DJ_OMN", "AUTRE"), Arrays.asList("SOO1_SOO1_DJ_OMN", "test")),
                new Remedial(4, "S_SO_2", Arrays.asList("contrainte_1", "contrainte_2"), Collections.singletonList("S_SO_3"), Collections.emptyList())
        );
        assertThat(remedials.size()).isEqualTo(expectedList.size());
        for (int i = 0; i < expectedList.size(); i++) {
            Remedial actual = remedials.get(i);
            Remedial expected = expectedList.get(i);
            assertThat(actual.getLineFile()).isEqualTo(expected.getLineFile());
            assertThat(actual.getContingency()).isEqualTo(expected.getContingency());
            assertThat(actual.getConstraint()).isEqualTo(expected.getConstraint());
            assertThat(actual.getBranchToOpen()).isEqualTo(expected.getBranchToOpen());
            assertThat(actual.getBranchToClose()).isEqualTo(expected.getBranchToClose());
        }
    }

    @Test
    public void parseEmptyFileTest() {
        String nullString = "";
        List<Remedial> remedials = RemedialReader.parseFile(nullString);
        assertThat(remedials.size()).isEqualTo(0);
    }

    @Test
    public void emptyRemedial() {
        assertThatCode(() -> {
            RemedialReader.checkFile(() -> new StringReader(""));
        }).doesNotThrowAnyException();
    }

    @Test
    public void missingEndOfLineHeaderRemedialCheck() {
        // Bad header #1
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "foo;1",
                        "cty1;2;FS.BIS1_FS.BIS1_DJ_OMN;+FVALDI1_FVALDI1_DJ_OMN;")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Missing ';' in remedial action file header");
    }

    @Test
    public void firstColumnIncorrectHeaderRemedialCheck() {
        // Bad header #1
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "foo;1;",
                        "cty1;2;FS.BIS1_FS.BIS1_DJ_OMN;+FVALDI1_FVALDI1_DJ_OMN;")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Malformed remedial action file header");
    }

    @Test
    public void numberOfElementIncorectHeaderRemedial() {
        // Bad header #2
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "NB;-1;",
                        "cty1;2;FS.BIS1_FS.BIS1_DJ_OMN;+FVALDI1_FVALDI1_DJ_OMN;")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Malformed remedial action file header");
    }

    @Test
    public void missingEndOfLineContentRemedialCheck() {
        // Bad content #0
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "NB;1;",
                        "cty1;;FS.BIS1_FS.BIS1_DJ_OMN;FVALDI1_FVALDI1_DJ_OMN")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Missing ';' in remedial action file, line 2");

    }

    @Test
    public void notEnoughColumnContentRemedialCheck() {
        // Bad content #0
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "NB;1;",
                        "cty1;2;FS.BIS1_FS.BIS1_DJ_OMN;FVALDI1_FVALDI1_DJ_OMN;",
                        "cty1;")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Malformed remedial action file, line : 3");
    }

    @Test
    public void badActionNumberContentRemedialCheck() {
        // Bad content #0
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "NB;1;",
                        "cty1;-2;FS.BIS1_FS.BIS1_DJ_OMN;FVALDI1_FVALDI1_DJ_OMN;")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Malformed number of actions in remedial action file, line : 2");
    }

    @Test
    public void emptySecondColumnContentRemedialCheck() {
        // Bad content #1
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "NB;1;",
                        "cty1;;FS.BIS1_FS.BIS1_DJ_OMN;FVALDI1_FVALDI1_DJ_OMN;")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Empty element in remedial action file, line : 2");
    }

    @Test
    public void emptyFirstColumnContentRemedialCheck() {
        // Bad content #2
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "NB;1;",
                        ";2;FS.BIS1_FS.BIS1_DJ_OMN;FVALDI1_FVALDI1_DJ_OMN;")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Empty element in remedial action file, line : 2");

    }

    @Test
    public void emptyColumnContentRemedialCheck() {
        // Bad content #3
        assertThatThrownBy(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "NB;1;",
                        "cty1;3;FS.BIS1_FS.BIS1_DJ_OMN;;FVALDI1_FVALDI1_DJ_OMN;")
        )))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Empty element in remedial action file, line : 2");
    }

    @Test
    public void remedialCheckOk() {
        // File ok
        assertThatCode(() -> RemedialReader.checkFile(() -> new StringReader(
                String.join(System.lineSeparator(),
                        "NB;1;",
                        "cty1;2;FS.BIS1_FS.BIS1_DJ_OMN;FVALDI1_FVALDI1_DJ_OMN;")
        ))).doesNotThrowAnyException();
    }

}
