package ch.santos_alves.sonar.plugins.hooks;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import ch.santos_alves.sonar.plugins.connectors.JiraRestConnector;

public class DisplayIssuesInScanner implements PostJob {

	private static final Logger LOGGER = Loggers.get(DisplayIssuesInScanner.class);

	@Override
	public void describe(PostJobDescriptor descriptor) {
		descriptor.name("Display issues");
	}

	@Override
	public void execute(PostJobContext context) {
		String username = context.settings().getString("jira.bot.username");
		String password = context.settings().getString("jira.bot.password");
		String host = context.settings().getString("jira.host");
		String port = context.settings().getString("jira.port");
		Severity threshold = Severity.valueOf(context.settings().getString("severity.threshold"));
		JiraRestConnector jiraConnector = new JiraRestConnector(host, port, username, password);

		// issues are not accessible when the mode "issues" is not enabled
		// with the scanner property "sonar.analysis.mode=issues"
		if (context.analysisMode().isIssues()) {
			// all open issues
			for (PostJobIssue issue : context.issues()) {
				
				if (issue.severity().compareTo(threshold) > 0) {
					// issue severity higher than threshold
					String id;
					try {
						id = jiraKeyGenerator(issue);
						if(!jiraConnector.doesTicketExist(id)) {
							jiraConnector.createNewIssue(id, issue);
						} else {
							LOGGER.warn("An issue with calculated id {} already exists in JIRA", id);
						}
					} catch (NoSuchAlgorithmException e) {
						LOGGER.error("Could not calculate issue id.", e );
					}					
				} else {
					LOGGER.warn("Issue {} has a lower severity : {} < {}", issue.key(), issue.severity().toString(), threshold.toString());					
				}

				// just to illustrate, we dump some fields of the 'issue' in
				// sysout (bad, very bad)
				LOGGER.info("OPEN {} : {}({})", issue.ruleKey().toString(), issue.componentKey(), issue.line());
			}

			// all resolved issues
			for (PostJobIssue issue : context.resolvedIssues()) {
				LOGGER.info("RESOLVED {} : {}({})", issue.ruleKey(), issue.componentKey(), issue.line());
			}

		} else {
			LOGGER.info("Nothing to do. Analysis mode is publish.");
		}
	}

	//Generates an identifier used in JIRA
	private String jiraKeyGenerator(PostJobIssue issue) throws NoSuchAlgorithmException {
		String ruleKey = issue.ruleKey().toString();
		Integer issueLine = issue.line();
		String componentKey = issue.componentKey();
		String inputComponentKey = issue.inputComponent().key();
		String issueKey = issue.key();
		String issueMessage = issue.message();
		String severity = issue.severity().toString();

		LOGGER.info(
				"Issue properties (ruleKey, issueKey, inputComponentKey, componentKey, issueMessage, issueLine, severity) -> ({},{},{},{},{},{},{})",
				ruleKey, issueKey, inputComponentKey, componentKey, issueMessage, issueLine, severity);
		
		
		String md5 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(new String(ruleKey + ":" + issueKey + ":" + inputComponentKey + ":" + componentKey
				+ ":" + issueMessage + ":" + issueLine + ":" + severity).getBytes())); 
		
		return md5;
	}

}
