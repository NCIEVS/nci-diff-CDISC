package gov.nih.nci.evs.cdisc;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.LocalDate;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class DiffTest {
    @TempDir
    public File outFolder;

    @ParameterizedTest
    @CsvSource({
            "ADaM Terminology",
            "CDASH Terminology",
            "Define-XML Terminology",
            "Glossary Terminology",
            "Protocol Terminology",
            "QRS Terminology",
            "SDTM Terminology",
            "SEND Terminology"
    })
    public void testPrint(String conceptFilePrefix, @TempDir Path tempDir) throws IOException, URISyntaxException {
        Path outFile = tempDir.resolve(format("%s-changes.txt", conceptFilePrefix));
        Diff diff = new Diff();
        String newReport = new File(getClass().getResource(format("/fixtures/report-files/current/%s.txt", conceptFilePrefix)).toURI()).getPath();
        String oldReport = new File(getClass().getResource(format("/fixtures/report-files/previous/%s.txt", conceptFilePrefix)).toURI()).getPath();
        diff.init(newReport, oldReport, "2022-03-25", outFile.toString());
        diff.getChanges();
        diff.print();
        assertThat(outFile)
                .hasContent(
                        IOUtils.toString(
                                getClass().getResourceAsStream(format("/fixtures/report-files/expected/%s Changes.txt", conceptFilePrefix)), Charset.defaultCharset()));

    }
}
