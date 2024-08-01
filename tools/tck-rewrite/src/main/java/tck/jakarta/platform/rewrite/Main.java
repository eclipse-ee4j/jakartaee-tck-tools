package tck.jakarta.platform.rewrite;

import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Main based on https://docs.openrewrite.org/running-recipes/running-rewrite-without-build-tool-plugins
 *
 * @author Scott Marlow
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String include = null;
        String className = null;
        String convertFromPackage = null;

        Path projectDir = Paths.get(".");
        List<Path> classpath = emptyList();


        for (int looper = 0 ; looper < args.length; looper++) {
            String arg = args[looper];
            if ("-include".equals(arg) || "-i".equals(arg)) {
                include = args[++looper];
            } else if ("-class".equals(arg)) {
                className = args[++looper];
            } else if ("-package".equals(arg)) {
                convertFromPackage = args[++looper];
            } else if ("-run".equals(args[looper])) {
                if( include != null) {
                    projectDir = Paths.get(include).toAbsolutePath();
                    include = null;
                }
                // put any rewrite recipe jars on this main method's runtime classpath
                // and either construct the recipe directly or via an Environment
                Environment environment = Environment.builder().scanRuntimeClasspath().build();
                Recipe recipe = environment.activateRecipes("tck.jakarta.platform.rewrite.JavaTestToArquillianShrinkwrap",
                        "tck.jakarta.platform.rewrite.ConvertJavaTestNameRecipe",
                        "tck.jakarta.platform.rewrite.GenerateNewTestClassRecipe"
                        );

                // create a JavaParser instance with your classpath
                JavaParser javaParser = JavaParser.fromJavaVersion()
                        .classpath(classpath)
                        .build();
                // walk the directory structure where your Java sources are located
                // and create a list of them
                List<Path> sourcePaths = Files.find(projectDir, 999, (p, bfa) ->
                                bfa.isRegularFile() && p.getFileName().toString().endsWith(".java"))
                        .collect(Collectors.toList());

                ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

                // parser the source files into LSTs
                List<SourceFile> cus = javaParser.parse(sourcePaths, projectDir, ctx).collect(Collectors.toList());
                InMemoryLargeSourceSet sourceSet = new InMemoryLargeSourceSet(cus);

                // collect results
                RecipeRun run = recipe.run(sourceSet, ctx);
                List<Result> results = run.getChangeset().getAllResults();

                for (Result result : results) {
                    // print diffs to the console
                    // System.out.println(result.diff(projectDir));

                    // or overwrite the file on disk with changes.
                    Path resultPath = result.getAfter().getSourcePath();
                    // if resultPath is a related path, change to absolute path
                    if (resultPath.getParent() == null || !resultPath.toString().startsWith(File.separator)) {
                        resultPath = Path.of(projectDir.toString(),resultPath.toString());

                    }
                    Files.writeString(resultPath,
                            result.getAfter().printAll());
                }
            }
        }

    }


}
