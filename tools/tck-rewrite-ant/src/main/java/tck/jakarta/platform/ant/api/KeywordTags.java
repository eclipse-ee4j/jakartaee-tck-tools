package tck.jakarta.platform.ant.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class KeywordTags {
    private static final Logger log = Logger.getLogger(KeywordTags.class.getName());
    private final HashMap<Path, KeywordTags.Tags> keywordMap = new HashMap<>();
    private static KeywordTags INSTANCE;
    private final Path tsHome;

    public static KeywordTags getInstance(Path tsHome) throws IOException {
        if (INSTANCE == null) {
            URL resURL = KeywordTags.class.getResource("/keyword.properties");
            try (InputStreamReader reader = new InputStreamReader(resURL.openStream())) {
                Properties commonArchives = new Properties();
                commonArchives.load(reader);
                parseKeywords(tsHome, commonArchives);
            }
        }
        return INSTANCE;
    }
    public static class Tags {
        private String[] tags;
        public Tags(String[] tags) {
            this.tags = tags;
        }
    }

    private KeywordTags(Path tsHome) {
        this.tsHome = tsHome;
    }

    private static void parseKeywords(Path tsHome, Properties keywords) {
        KeywordTags keywordTags = new KeywordTags(tsHome);

        /* The key is of the form pkg_root=keyword1 keyword2 ... e.g.:
        com/sun/ts/tests/appclient = appclient javaee javaee_web_profile_optional
         */
        for (String key : keywords.stringPropertyNames()) {
            String appPath = keywords.getProperty(key);
            // There can be multiple tags for a package root, separated by space
            String[] tags = appPath.split("\\s");
            //
            Path pkgRootPath = Path.of(key);
            Tags tagsInfo = new Tags(tags);
            keywordTags.keywordMap.put(pkgRootPath, tagsInfo);
        }
        INSTANCE = keywordTags;
    }

    /**
     * Get the common deployment method for a given package root.
     * @param pkgRoot
     * @return the deployment method info, possibly null if no common deployment is defined.
     */
    public List<String> getTags(Path pkgRoot) {
        List<String> tags = Collections.emptyList();
        if(pkgRoot.isAbsolute()) {
            Path src = tsHome.resolve("src");
            pkgRoot = src.relativize(pkgRoot);
        }
        do {
            if (keywordMap.containsKey(pkgRoot)) {
                KeywordTags.Tags tagInfo = keywordMap.get(pkgRoot);
                tags = List.of(tagInfo.tags);
                break;
            }
            pkgRoot = pkgRoot.getParent();
        } while (pkgRoot != null && !pkgRoot.getFileName().endsWith("com"));
        return tags;
    }
}
