package io.github.juliantcook.pmdrules;

import java.nio.file.FileSystems;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class GenerateDocs {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("One argument is required: The path to a rules folder.");
            System.exit(1);
        }
        long start = System.currentTimeMillis();
        var rulesFolder = args[0];
        var ruleFiles = FileSystems.getDefault().getPath(rulesFolder)
                .toAbsolutePath().normalize().toFile().list();
        var filesPaths = stream(ruleFiles).map(f -> rulesFolder + f).collect(toList());
        var generator = new RuleDocGenerator();
        var rulesets = generator.resolveRulesets(filesPaths);
        generator.generateRuleSetMarkdown(rulesets, System.out);
        System.err.println("Generated docs in " + (System.currentTimeMillis() - start) + " ms");
    }
}
