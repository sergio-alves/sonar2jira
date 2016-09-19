package ch.santos_alves.sonar.plugins;

import org.sonar.api.Plugin;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.PropertyDefinition;

import ch.santos_alves.sonar.plugins.hooks.DisplayIssuesInScanner;

/**
 * This class is the entry point for all extensions. It is referenced in
 * pom.xml.
 */
public class Sonar2JiraPlugin implements Plugin {

	@Override
	public void define(Context context) {

		context.addExtension(DisplayIssuesInScanner.class);

		context.addExtensions(
				PropertyDefinition.builder("jira.bot.username").name("The bot username to log into JIRA")
						.description("To username to use to log into JIRA").defaultValue("sonarbot").build(),
				PropertyDefinition.builder("jira.bot.password").name("The bot password to log into JIRA")
						.description("To password to use to log into JIRA").defaultValue("sonarbot").build(),
				PropertyDefinition.builder("jira.host").name("The JIRA hostname").description("T")
						.defaultValue("localhost").build(),
				PropertyDefinition.builder("jira.port").name("The JIRA port").description("To JIRA port")
						.defaultValue("8080").build(),
				PropertyDefinition.builder("severity.threshold").name("Severity Threshold")
						.description("An issue will be opened in Jira to all issues that have severity high or equal.")
						.options(Severity.BLOCKER.toString(), Severity.CRITICAL.toString(), Severity.INFO.toString(),
								Severity.MAJOR.toString(), Severity.MINOR.toString())
						.defaultValue("CRITICAL").build());
	}
}
