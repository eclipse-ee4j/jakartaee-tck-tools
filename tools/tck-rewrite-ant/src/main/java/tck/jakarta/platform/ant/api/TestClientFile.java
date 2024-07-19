package tck.jakarta.platform.ant.api;

public class TestClientFile {
    private String name;
    private String pkg;
    private String content;

    public TestClientFile(String name, String pkg, String content) {
        this.name = name;
        this.pkg = pkg;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public String getPackage() {
        return pkg;
    }

    public String getContent() {
        return content;
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append(String.format("TestClientFile[%s, %s]\n", name, pkg));
        tmp.append("code:\n");
        tmp.append(content);
        return tmp.toString();
    }
}