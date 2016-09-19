package ch.santos_alves.sonar.plugins;

import org.sonar.api.Plugin;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.PropertyDefinition;

import ch.santos_alves.sonar.plugins.hooks.DisplayIssuesInScanner;
import ch.santos_alves.sonar.plugins.hooks.DisplayQualityGateStatus;


/**
 * This class is the entry point for all extensions. It is referenced in
 * pom.xml.
 */
public class Sonar2JiraPlugin implements Plugin {

	@Override
	public void define(Context context) {
		// tutorial on hooks
		// http://docs.sonarqube.org/display/DEV/Adding+Hooks
		context.addExtensions(DisplayIssuesInScanner.class, DisplayQualityGateStatus.class);

		// tutorial on languages
		// context.addExtensions(FooLanguage.class, FooQualityProfile.class);

		// tutorial on measures
		// context
		// .addExtensions(ExampleMetrics.class, SetSizeOnFilesSensor.class,
		// ComputeSizeAverage.class, ComputeSizeRating.class);

		// tutorial on rules
		// context.addExtensions(JavaRulesDefinition.class,
		// CreateIssuesOnJavaFilesSensor.class);
		// context.addExtensions(FooLintRulesDefinition.class,
		// FooLintIssuesLoaderSensor.class);

		// tutorial on settings
		// context
		// .addExtensions(ExampleProperties.definitions())
		// .addExtension(SayHelloFromScanner.class);

		// tutorial on web extensions
		// context.addExtensions(ExampleFooter.class, ExampleWidget.class);

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
