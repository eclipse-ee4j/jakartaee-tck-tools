package com.sun.tdk.signaturetest.classpath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Release {
    public static final Release BOOT_CLASS_PATH = new Release('0', null);

    private final char version;
    private final String[] prefixes;

    private Release(char version, String... prefixes) {
        this.version = version;
        this.prefixes = prefixes;
    }

    /**
     * Finds version of the JDK API.
     * @param version 6, 7, 8, 9, ..., 11, ... 15
     * @return found version or null
     */
    public static Release find(int version) {
        if (version > 15) {
            return null;
        }
        char ch = (char) (version < 10 ? '0' + version : 'A' + (version - 10));
        return RELEASES.get(ch);
    }

    InputStream findClass(String name) {
        if (prefixes == null) {
            final String resourceName = name.replace('.', '/') + ".class";
            return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
        } else {
            for (String p : prefixes) {
                final String resourceName = "/META-INF/sigtest/" + p + "/" + name.replace('.', '/') + ".sig";
                InputStream is = Release.class.getResourceAsStream(resourceName);
                if (is != null) {
                    return is;
                }
            }
            return null;
        }

    }

    private static final Map<Character, Release> RELEASES;
    static {
        List<String> lines = new ArrayList<>();
        try {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(Release.class.getResourceAsStream("/META-INF/sigtest.ls")))) {
                for (;;) {
                    String l = r.readLine();
                    if (l == null) {
                        break;
                    }
                    lines.add(l);
                }
            }
        } catch (IOException ex) {
            throw new InternalError(ex);
        }

        Map<Character,List<String>> prefixes = new HashMap<>();
        for (String l : lines) {
            String[] versionsDir = l.split("/");
            String versions = versionsDir[0];
            for (int i = 0; i < versions.length(); i++) {
                Character ch = versions.charAt(i);
                List<String> arr = prefixes.get(ch);
                if (arr == null) {
                    arr = new ArrayList<>();
                    prefixes.put(ch, arr);
                }
                arr.add(l);
            }
        }

        Map<Character,Release> releases = new HashMap<>();
        for (Map.Entry<Character, List<String>> entry : prefixes.entrySet()) {
            Character key = entry.getKey();
            List<String> value = entry.getValue();
            releases.put(key, new Release(key, value.toArray(new String[0])));
        }

        RELEASES = releases;
    }
}
