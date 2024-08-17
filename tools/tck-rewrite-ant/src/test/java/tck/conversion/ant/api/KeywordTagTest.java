package tck.conversion.ant.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.api.KeywordTags;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class KeywordTagTest {
    static Path tsHome = Paths.get(System.getProperty("ts.home"));

    @Test
    public void testAppclientTags() throws IOException {
        KeywordTags keywordTags = KeywordTags.getInstance(tsHome);
        List<String> tags = keywordTags.getTags(Path.of("com/sun/ts/tests/appclient"));
        System.out.println(tags);
        Assertions.assertTrue(tags.contains("appclient"), "appclient tag found");
        Assertions.assertTrue(tags.contains("platform"), "platform tag found");
        Assertions.assertTrue(tags.contains("web_optional"), "web_optional tag found");
    }

    @Test
    public void testConnectorTags() throws IOException {
        KeywordTags keywordTags = KeywordTags.getInstance(tsHome);
        List<String> tags = keywordTags.getTags(Path.of("com/sun/ts/tests/connector"));
        System.out.println(tags);
        Assertions.assertTrue(tags.contains("connector"), "connector tag found");
        Assertions.assertTrue(tags.contains("platform"), "platform tag found");
        Assertions.assertTrue(tags.contains("connector_standalone"), "connector_standalone tag found");
        Assertions.assertTrue(tags.contains("connector_web"), "connector_web tag found");
        Assertions.assertTrue(tags.contains("web_optional"), "web_optional tag found");
    }

    @Test
    public void testConnector_resourceDefs_servlet() throws IOException {
        KeywordTags keywordTags = KeywordTags.getInstance(tsHome);
        List<String> tags = keywordTags.getTags(Path.of("com/sun/ts/tests/connector/resourceDefs/servlet"));
        System.out.println(tags);
        Assertions.assertTrue(tags.contains("platform"), "platform tag found");
        Assertions.assertTrue(tags.contains("connector_resourcedef_servlet_optional"), "connector_resourcedef_servlet_optional tag found");
    }

    @Test
    public void test_ejb_ee_timer() throws IOException {
        KeywordTags keywordTags = KeywordTags.getInstance(tsHome);
        List<String> tags = keywordTags.getTags(Path.of("com/sun/ts/tests/ejb/ee/timer"));
        System.out.println(tags);
        Assertions.assertTrue(tags.contains("platform_optional"), "platform_optional tag found");
        Assertions.assertTrue(tags.contains("ejb_1x_optional"), "ejb_1x_optional tag found");
        Assertions.assertTrue(tags.contains("web_optional"), "web_optional tag found");
    }

    @Test
    public void test_signature_javaee() throws IOException {
        KeywordTags keywordTags = KeywordTags.getInstance(tsHome);
        List<String> tags = keywordTags.getTags(Path.of("com/sun/ts/tests/signaturetest/javaee"));
        System.out.println(tags);
        Assertions.assertTrue(tags.contains("signature"), "signature tag found");
        Assertions.assertTrue(tags.contains("platform"), "platform tag found");
        Assertions.assertTrue(tags.contains("web"), "web tag found");
    }

}
