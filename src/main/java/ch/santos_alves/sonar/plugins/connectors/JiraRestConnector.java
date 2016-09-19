package ch.santos_alves.sonar.plugins.connectors;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JiraRestConnector {
	protected static final String APPLICATION_JSON = "application/json";
	private static final Logger log = Loggers.get(JiraRestConnector.class);
	private String botUsername;
	private String botPassword;
	private HttpClient client;

	public JiraRestConnector(String host, String port, String username, String password) {
		this.botPassword = password;
		this.botUsername = username;
		HttpClientOptions options = new HttpClientOptions();

		options.setDefaultHost(host);
		options.setDefaultPort(Integer.parseInt(port));

		this.client = Vertx.vertx().createHttpClient(options);
	}

	public Future<Boolean> createNewIssue(String issueKey, PostJobIssue issue) {
		Future<Boolean> response = Future.future();

		JsonObject request = new JsonObject();
		JsonObject fields = new JsonObject();
		JsonObject project = new JsonObject();
		JsonObject type = new JsonObject();

		request.put("fields", fields);

		fields.put("project", project);
		fields.put("summary", issue.ruleKey().rule());
		fields.put("description",
				issue.ruleKey().rule() + " found in " + issue.componentKey() + " @ line " + issue.line());
		fields.put("customfield_10002", issueKey);
		fields.put("issuetype", type);
		type.put("id", "10001");		
		
		// TODO: move it to config
		Map<String, String> projectsMapping = new HashMap<>();
		String[] parts = issue.componentKey().split(":");
		projectsMapping.put("ch.santos_alves.sonar.plugin:test-application", "PROJTEST");

		project.put("key", projectsMapping.get(parts[0] + ":" + parts[1]));

		log.info("Going to send request :  \r\n{}", request.encodePrettily());
		
		client.post("/rest/api/2/issue")
			.putHeader("Content-Type", APPLICATION_JSON)
			.putHeader("Authorization", getBasicAuthenticationString())
			.handler(handler -> {
					if (handler.statusCode() == 200) {
						log.info("Create Issue Response received with StatusCode 200");
						handler.bodyHandler(bodyHandler -> {
							JsonObject rep = new JsonObject(new String(bodyHandler.getBytes()));
							log.info("Create Issue returned : \r\n{}\r\n", rep.encodePrettily());
							response.complete(true);
						});
					} else {
						log.warn("Create issue response received {} : {}", handler.statusCode(),
								handler.statusMessage());
						response.complete(false);
					}

				}).exceptionHandler(handler -> {
					log.error("Something went wrong creating the issue in JIRA.", handler.getCause());
					response.complete(false);
				}).end(request.encode());

		/*
		 * { "update": { "worklog": [ { "add": { "timeSpent": "60m", "started":
		 * "2011-07-05T11:05:00.000+0000" } } ] }, "fields": { "project": {
		 * "id": "10000" }, "summary": "something's wrong", "issuetype": { "id":
		 * "10000" }, "assignee": { "name": "homer" }, "reporter": { "name":
		 * "smithers" }, "priority": { "id": "20000" }, "labels": [ "bugfix",
		 * "blitz_test" ], "timetracking": { "originalEstimate": "10",
		 * "remainingEstimate": "5" }, "security": { "id": "10000" },
		 * "versions": [ { "id": "10000" } ], "environment": "environment",
		 * "description": "description", "duedate": "2011-03-11", "fixVersions":
		 * [ { "id": "10001" } ], "components": [ { "id": "10000" } ],
		 * "customfield_30000": [ "10000", "10002" ], "customfield_80000": {
		 * "value": "red" }, "customfield_20000": "06/Jul/11 3:25 PM",
		 * "customfield_40000": "this is a text field", "customfield_70000": [
		 * "jira-administrators", "jira-software-users" ], "customfield_60000":
		 * "jira-software-users", "customfield_50000":
		 * "this is a text area. big text.", "customfield_10000": "09/Jun/81" }
		 * }
		 */

		return response;
	}

	private String getBasicAuthenticationString() {

		String auth ="Basic " + Base64.getEncoder().encodeToString(new StringBuilder(this.botUsername).append(":").append(this.botPassword).toString().getBytes());
		log.info("calculated : " + auth);
		return auth;
	}

	/**
	 * Calls JIRA to get if a given ticket was already opened in an async way
	 * 
	 * @param id
	 *            the sonar issue id
	 * 
	 * @return an async future object
	 */
	public Future<Boolean> asyncDoesTicketExist(String id) {
		Future<Boolean> future = Future.future();

		log.info("Going to check async if a ticket with sonar id url {} exists", id);
		
		JsonObject request = new JsonObject();
		JsonArray fields = new JsonArray();
		request.put("jql", "sonar-issue-id ~ '"+ id +"'");
		request.put("startAt", 0);
		request.put("maxResults", 15);
		request.put("fields", fields);
		
		fields.add("summary");
		fields.add("status");
		fields.add("assignee");
		
		
		client.post("/rest/api/2/search")
			.putHeader("Authorization", getBasicAuthenticationString())
			.handler(handler -> {
				log.info("Request sent and response received : {} : {} ", handler.statusCode(),	handler.statusMessage());

				if (handler.statusCode() >= 200  && handler.statusCode() < 300) {

					handler.bodyHandler(bodyHandler -> {
						JsonObject response = new JsonObject(new String(bodyHandler.getBytes()));

						log.info("Data received\r\n{}\r\n", response.encodePrettily());
						
						if (response.getInteger("total") == 0) {
							
							future.complete(false);
						} else {
							future.complete(true);
						}
					});
					
				} else {
					future.complete(false);
					log.error("Ouuuuupssss!!!!!!!!!!!!!!!!!!");
				}
			}).end(request.encode());

		return future;
	}

	/**
	 * Calls JIRA to get if a given ticket was already opened
	 * 
	 * @param id
	 *            the sonar issue id
	 * 
	 * @return true if issue already opened on Jira false otherwise
	 */
	public boolean doesTicketExist(String id) {

		Future<Boolean> future = asyncDoesTicketExist(id);

		log.info("Going to check synchronously if a ticket with sonar id url {} exists", id);

		while (!future.isComplete()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return future.result();
	}
}
