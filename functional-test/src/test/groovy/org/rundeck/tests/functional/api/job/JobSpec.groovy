package org.rundeck.tests.functional.api.job

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.jobs.CreateJobResponse
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class JobSpec extends BaseContainer {

    static final String PROJECT_NAME = "TestJobs"
    static final MAPPER = new ObjectMapper()

    def setupSpec() {
        startEnvironment()
        setupProjectArchiveDirectoryResource(PROJECT_NAME, "/projects-import/TestJobs")
    }

    def "Runs workflow steps"() {
        given:
        def adhoc = runJobAndWait('9b43e4ab-7ff2-4159-9fc7-7437901914f7', ["options": ["opt2": "a"]])
        def entries = adhoc['entries'].collect { it.log }
        def node = adhoc['entries'].findResult { it.node }
        expect:
        entries == ['hello there',
                    'option opt1: testvalue',
                    'option opt1: testvalue',
                    "node: $node",
                    'option opt2: a',
                    'this is script 2, opt1 is testvalue',
                    'hello there',
                    'this is script 1, opt1 is testvalue',]
    }

    def "Create a job with multiple steps"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
              </dispatch>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
                <command>
                  <exec>echo 1</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(PROJECT_NAME, jobXml, client)

        then:
        response.successful
        def jr = MAPPER.readValue(response.body().string(), CreateJobResponse.class)
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        jobDetails.sequence.commands[0].exec == "echo 0"
        jobDetails.sequence.commands[1].exec == "echo 1"
    }

    def "Create a job with a job schedule enabled"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
              </dispatch>
              <scheduleEnabled>true</scheduleEnabled>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(PROJECT_NAME, jobXml, client)

        then:
        response.successful
        def jr = MAPPER.readValue(response.body().string(), CreateJobResponse.class)
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        jobDetails.scheduleEnabled == true
    }

    def "Create a job with a job schedule disabled"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
              </dispatch>
              <scheduleEnabled>false</scheduleEnabled>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(PROJECT_NAME, jobXml, client)

        then:
        response.successful
        def jr = MAPPER.readValue(response.body().string(), CreateJobResponse.class)
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        !jobDetails.scheduleEnabled
    }

    def "Create a job with a job execution enabled"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
              </dispatch>
              <executionEnabled>true</executionEnabled>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(PROJECT_NAME, jobXml, client)

        then:
        response.successful
        def jr = MAPPER.readValue(response.body().string(), CreateJobResponse.class)
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        jobDetails.executionEnabled
    }

    def "Create a job with a job execution disabled"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
              </dispatch>
              <executionEnabled>false</executionEnabled>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(PROJECT_NAME, jobXml, client)

        then:
        response.successful
        def jr = MAPPER.readValue(response.body().string(), CreateJobResponse.class)
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        !jobDetails.executionEnabled
    }
}
