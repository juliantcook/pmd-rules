package io.github.juliantcook.pmdrules;

import java.nio.file.FileSystems;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class GenerateDocs {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("One argument is required: The path to a rules folder.");
            System.exit(1);
        }
        long start = System.currentTimeMillis();
        RuleDocGenerator generator = new RuleDocGenerator();
        var rulesFolder = args[0];
        var ruleFiles = FileSystems.getDefault().getPath(rulesFolder)
                .toAbsolutePath().normalize().toFile().list();
        var filesPaths = stream(ruleFiles).map(f -> rulesFolder + f).collect(toList());
        var rulesets = generator.resolveRulesets(filesPaths);
        var markdowns = generator.generateRuleSetMarkdown(rulesets);
        for (List<String> markdown : markdowns) {
            markdown.forEach(System.out::println);
        }

        System.err.println("Generated docs in " + (System.currentTimeMillis() - start) + " ms");
    }
}
