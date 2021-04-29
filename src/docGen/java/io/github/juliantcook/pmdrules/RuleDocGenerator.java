package io.github.juliantcook.pmdrules;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetLoadException;
import net.sourceforge.pmd.RuleSetLoader;
import net.sourceforge.pmd.docs.EscapeUtils;
import net.sourceforge.pmd.docs.RuleSetUtils;
import net.sourceforge.pmd.lang.rule.RuleReference;
import net.sourceforge.pmd.lang.rule.XPathRule;
import net.sourceforge.pmd.properties.MultiValuePropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleDocGenerator {
    private static final Logger LOG = Logger.getLogger(RuleDocGenerator.class.getName());

    private static final String RULESET_INDEX_PERMALINK_PATTERN = "pmd_rules_${language.tersename}_${ruleset.name}.html";

    private static final String DEPRECATION_LABEL_SMALL = "<span style=\"border-radius: 0.25em; color: #fff; padding: 0.2em 0.6em 0.3em; display: inline; background-color: #d9534f; font-size: 75%;\">Deprecated</span> ";
    private static final String DEPRECATION_LABEL = "<span style=\"border-radius: 0.25em; color: #fff; padding: 0.2em 0.6em 0.3em; display: inline; background-color: #d9534f;\">Deprecated</span> ";
    private static final String DEPRECATED_RULE_PROPERTY_MARKER = "deprecated!";

    private static final String GITHUB_SOURCE_LINK = "https://github.com/juliantcook/pmd-rules/blob/master/src/main/java/";

    /** Maintains mapping from pmd terse language name to rouge highlighter language. */
    private static final Map<String, String> LANGUAGE_HIGHLIGHT_MAPPER = new HashMap<>();

    static {
        LANGUAGE_HIGHLIGHT_MAPPER.put("ecmascript", "javascript");
        LANGUAGE_HIGHLIGHT_MAPPER.put("pom", "xml");
        LANGUAGE_HIGHLIGHT_MAPPER.put("apex", "java");
        LANGUAGE_HIGHLIGHT_MAPPER.put("plsql", "sql");
    }

    public List<RuleSet> resolveRulesets(List<String> additionalRulesets) {
        if (additionalRulesets == null) {
            return Collections.emptyList();
        }

        List<RuleSet> rulesets = new ArrayList<>();
        RuleSetLoader ruleSetLoader = new RuleSetLoader();
        for (String filename : additionalRulesets) {
            try {
                // do not take rulesets from pmd-test or pmd-core
                if (!filename.contains("pmd-test") && !filename.contains("pmd-core")) {
                    rulesets.add(ruleSetLoader.loadFromResource(filename));
                } else {
                    LOG.fine("Ignoring ruleset " + filename);
                }
            } catch (RuleSetLoadException e) {
                // ignore rulesets, we can't read
                LOG.log(Level.WARNING, "ruleset file " + filename + " ignored (" + e.getMessage() + ")", e);
            }
        }
        return rulesets;
    }

    private static List<String> toLines(String s) {
        return Arrays.asList(s.split("\r\n|\n"));
    }

    static class Output {
        private final OutputStream outputStream;
        private Output(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        private void addAll(List<String> lines) {
            for (var line : lines) {
                add(line);
            }
        }

        private void add(String line) {
            try {
                outputStream.write((line + "\n").getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void generateRuleSetMarkdown(List<RuleSet> rulesets, OutputStream outputStream) {
        var lines = new Output(outputStream);
        var languageTersename = "java";
        for (RuleSet ruleset : rulesets) {
            String rulesetFilename = RuleSetUtils.getRuleSetFilename(ruleset);
            lines.add("---");
            for (Rule rule : getSortedRules(ruleset)) {
                lines.add("## " + rule.getName());
                lines.add("");

                if (rule instanceof RuleReference) {
                    RuleReference ref = (RuleReference) rule;
                    if (ruleset.getFileName().equals(ref.getRuleSetReference().getRuleSetFileName())) {
                        // rule renamed within same ruleset
                        lines.add(DEPRECATION_LABEL);
                        lines.add("");
                        lines.add("This rule has been renamed. Use instead: ["
                                + ref.getRule().getName() + "](" + "#" + ref.getRule().getName().toLowerCase(Locale.ROOT) + ")");
                        lines.add("");
                    } else {
                        // rule moved to another ruleset
                        String otherLink = RULESET_INDEX_PERMALINK_PATTERN
                                .replace("${language.tersename}", languageTersename)
                                .replace("${ruleset.name}", RuleSetUtils.getRuleSetFilename(ref.getRuleSetReference().getRuleSetFileName()));
                        lines.add(DEPRECATION_LABEL);
                        lines.add("");
                        lines.add("The rule has been moved to another ruleset. Use instead: ["
                                + ref.getRule().getName() + "](" + otherLink + "#" + ref.getRule().getName().toLowerCase(Locale.ROOT) + ")");
                        lines.add("");
                    }
                }

                if (rule.isDeprecated()) {
                    lines.add(DEPRECATION_LABEL);
                    lines.add("");
                }
                if (rule.getSince() != null) {
                    lines.add("**Since:** PMD " + rule.getSince());
                    lines.add("");
                }
                lines.add("**Priority:** " + rule.getPriority() + " (" + rule.getPriority().getPriority() + ")");
                lines.add("");

                if (rule.getMinimumLanguageVersion() != null) {
                    lines.add("**Minimum Language Version:** "
                            + rule.getLanguage().getName() + " " + rule.getMinimumLanguageVersion().getVersion());
                    lines.add("");
                }

                lines.addAll(EscapeUtils.escapeLines(toLines(stripIndentation(rule.getDescription()))));
                lines.add("");

                XPathRule xpathRule = asXPathRule(rule);
                if (xpathRule != null) {
                    lines.add("**This rule is defined by the following XPath expression:**");
                    lines.add("``` xpath");
                    lines.addAll(toLines(StringUtils.stripToEmpty(xpathRule.getXPathExpression())));
                    lines.add("```");
                } else {
                    lines.add("**This rule is defined by the following Java class:** "
                            + "[" + rule.getRuleClass() + "]("
                            + GITHUB_SOURCE_LINK + rule.getRuleClass().replaceAll("\\.", Matcher.quoteReplacement(File.separator))
                            + ".java"
                            + ")");
                }
                lines.add("");

                if (!rule.getExamples().isEmpty()) {
                    lines.add("**Example(s):**");
                    lines.add("");
                    for (String example : rule.getExamples()) {
                        lines.add("``` " + mapLanguageForHighlighting(languageTersename));
                        lines.addAll(toLines(StringUtils.stripToEmpty(example)));
                        lines.add("```");
                        lines.add("");
                    }
                }

                List<PropertyDescriptor<?>> properties = new ArrayList<>(rule.getPropertyDescriptors());
                // filter out standard properties
                properties.remove(Rule.VIOLATION_SUPPRESS_REGEX_DESCRIPTOR);
                properties.remove(Rule.VIOLATION_SUPPRESS_XPATH_DESCRIPTOR);
                properties.remove(XPathRule.XPATH_DESCRIPTOR);
                properties.remove(XPathRule.VERSION_DESCRIPTOR);

                if (!properties.isEmpty()) {
                    lines.add("**This rule has the following properties:**");
                    lines.add("");
                    lines.add("|Name|Default Value|Description|Multivalued|");
                    lines.add("|----|-------------|-----------|-----------|");
                    for (PropertyDescriptor<?> propertyDescriptor : properties) {
                        String description = propertyDescriptor.description();
                        final boolean isDeprecated = isDeprecated(propertyDescriptor);
                        if (isDeprecated) {
                            description = description.substring(DEPRECATED_RULE_PROPERTY_MARKER.length());
                        }

                        String defaultValue = determineDefaultValueAsString(propertyDescriptor, rule, true);

                        String multiValued = "no";
                        if (propertyDescriptor.isMultiValue()) {
                            MultiValuePropertyDescriptor<?> multiValuePropertyDescriptor =
                                    (MultiValuePropertyDescriptor<?>) propertyDescriptor;
                            multiValued = "yes. Delimiter is '"
                                    + multiValuePropertyDescriptor.multiValueDelimiter() + "'.";
                        }

                        lines.add("|" + EscapeUtils.escapeMarkdown(StringEscapeUtils.escapeHtml4(propertyDescriptor.name()))
                                + "|" + EscapeUtils.escapeMarkdown(StringEscapeUtils.escapeHtml4(defaultValue)) + "|"
                                + EscapeUtils.escapeMarkdown((isDeprecated ? DEPRECATION_LABEL_SMALL : "")
                                + StringEscapeUtils.escapeHtml4(description))
                                + "|" + EscapeUtils.escapeMarkdown(StringEscapeUtils.escapeHtml4(multiValued)) + "|");
                    }
                    lines.add("");
                }

                if (properties.isEmpty()) {
                    lines.add("**Use this rule by referencing it:**");
                } else {
                    lines.add("**Use this rule with the default properties by just referencing it:**");
                }
                lines.add("``` xml");
                lines.add("<rule ref=\"category/" + languageTersename + "/" + rulesetFilename + ".xml/" + rule.getName() + "\" />");
                lines.add("```");
                lines.add("");

                if (properties.stream().anyMatch(it -> !isDeprecated(it))) {
                    lines.add("**Use this rule and customize it:**");
                    lines.add("``` xml");
                    lines.add("<rule ref=\"category/" + languageTersename + "/" + rulesetFilename + ".xml/" + rule.getName() + "\">");
                    lines.add("    <properties>");
                    for (PropertyDescriptor<?> propertyDescriptor : properties) {
                        if (!isDeprecated(propertyDescriptor)) {
                            String defaultValue = determineDefaultValueAsString(propertyDescriptor, rule, false);
                            lines.add("        <property name=\"" + propertyDescriptor.name() + "\" value=\""
                                    + defaultValue + "\" />");
                        }
                    }
                    lines.add("    </properties>");
                    lines.add("</rule>");
                    lines.add("```");
                    lines.add("");
                }
            }
        }
    }

    private XPathRule asXPathRule(Rule rule) {
        if (rule instanceof XPathRule) {
            return (XPathRule) rule;
        } else if (rule instanceof RuleReference && ((RuleReference) rule).getRule() instanceof XPathRule) {
            return (XPathRule) ((RuleReference) rule).getRule();
        }
        return null;
    }

    private static boolean isDeprecated(PropertyDescriptor<?> propertyDescriptor) {
        return propertyDescriptor.description() != null
                && propertyDescriptor.description().toLowerCase(Locale.ROOT).startsWith(DEPRECATED_RULE_PROPERTY_MARKER);
    }

    private String determineDefaultValueAsString(PropertyDescriptor<?> propertyDescriptor, Rule rule, boolean pad) {
        String defaultValue = "";
        Object realDefaultValue = rule.getProperty(propertyDescriptor);
        @SuppressWarnings("unchecked") // just force it, we know it's the right type
        PropertyDescriptor<Object> captured = (PropertyDescriptor<Object>) propertyDescriptor;

        if (realDefaultValue != null) {
            defaultValue = captured.asDelimitedString(realDefaultValue);

            if (pad && propertyDescriptor.isMultiValue()) {
                @SuppressWarnings("unchecked") // multi valued properties are using a List
                MultiValuePropertyDescriptor<List<?>> multiPropertyDescriptor = (MultiValuePropertyDescriptor<List<?>>) propertyDescriptor;

                // surround the delimiter with spaces, so that the browser can wrap
                // the value nicely
                defaultValue = defaultValue.replaceAll(Pattern.quote(
                        String.valueOf(multiPropertyDescriptor.multiValueDelimiter())),
                        " " + multiPropertyDescriptor.multiValueDelimiter() + " ");
            }
        }
        return defaultValue;
    }

    private static String stripIndentation(String description) {
        if (description == null || description.isEmpty()) {
            return "";
        }

        String stripped = StringUtils.stripStart(description, "\n\r");
        stripped = StringUtils.stripEnd(stripped, "\n\r ");

        int indentation = 0;
        int strLen = stripped.length();
        while (Character.isWhitespace(stripped.charAt(indentation)) && indentation < strLen) {
            indentation++;
        }

        String[] lines = stripped.split("\\n");
        String prefix = StringUtils.repeat(' ', indentation);
        StringBuilder result = new StringBuilder(stripped.length());

        if (StringUtils.isNotEmpty(prefix)) {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (i > 0) {
                    result.append(StringUtils.LF);
                }
                result.append(StringUtils.removeStart(line, prefix));
            }
        } else {
            result.append(stripped);
        }
        return result.toString();
    }

    /**
     * Simply maps PMD languages to rouge languages
     *
     * @param languageTersename
     * @return
     * @see <a href="https://github.com/jneen/rouge/wiki/List-of-supported-languages-and-lexers">List of supported languages</a>
     */
    private static String mapLanguageForHighlighting(String languageTersename) {
        if (LANGUAGE_HIGHLIGHT_MAPPER.containsKey(languageTersename)) {
            return LANGUAGE_HIGHLIGHT_MAPPER.get(languageTersename);
        }
        return languageTersename;
    }

    private List<Rule> getSortedRules(RuleSet ruleset) {
        List<Rule> sortedRules = new ArrayList<>(ruleset.getRules());
        Collections.sort(sortedRules, new Comparator<Rule>() {
            @Override
            public int compare(Rule o1, Rule o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return sortedRules;
    }
}
