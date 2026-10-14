package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.GitLabClient;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabJob;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

class ReadTaskLogsGitLabOutboundPortImpl implements ReadTaskLogsGitLabOutboundPort {

    private static final Pattern ANSI_SEQUENCE = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");
    private static final Pattern SECTION_MARKER = Pattern.compile("section_(start|end):\\d+:[^\\r\\n]*\\r?");
    private static final String NO_LOG = "(no log available)\n";

    private final GitLabClient client;
    private final long maxJobLogSize;
    private final long maxTotalSize;

    ReadTaskLogsGitLabOutboundPortImpl(GitLabClient client, long maxJobLogSize, long maxTotalSize) {
        this.client = client;
        this.maxJobLogSize = maxJobLogSize;
        this.maxTotalSize = maxTotalSize;
    }

    @Override
    public String renderPipelineLogs(GitLabAccess access, String pipelineId) {
        List<GitLabJob> jobs = client.listJobs(access, pipelineId);
        if (jobs.isEmpty()) {
            return "No jobs in GitLab pipeline " + pipelineId + ".";
        }
        return renderJobs(access, jobs);
    }

    private String renderJobs(GitLabAccess access, List<GitLabJob> jobs) {
        StringBuilder content = new StringBuilder();
        long used = 0;
        for (int index = 0; index < jobs.size(); index++) {
            String section = renderJob(access, jobs.get(index));
            long sectionBytes = utf8Length(section);
            if (used + sectionBytes > maxTotalSize) {
                content.append(omittedMarker(jobs.size() - index));
                break;
            }
            content.append(section);
            used += sectionBytes;
        }
        return content.toString();
    }

    private String renderJob(GitLabAccess access, GitLabJob job) {
        return jobHeader(job) + jobBody(access, job);
    }

    private static String jobHeader(GitLabJob job) {
        return "=== Job " + job.name() + " (id " + job.id() + ", stage " + job.stage()
                + ", status " + job.status() + ") ===\n";
    }

    private String jobBody(GitLabAccess access, GitLabJob job) {
        return client.readJobLog(access, job.id())
                .map(this::renderLog)
                .orElse(NO_LOG);
    }

    private String renderLog(String rawLog) {
        String text = tailLimit(clean(rawLog), maxJobLogSize);
        return text.endsWith("\n") ? text : text + "\n";
    }

    private static String clean(String rawLog) {
        String withoutAnsi = ANSI_SEQUENCE.matcher(rawLog).replaceAll("");
        return SECTION_MARKER.matcher(withoutAnsi).replaceAll("").replace("\r\n", "\n");
    }

    private static String tailLimit(String text, long limit) {
        long size = utf8Length(text);
        if (size <= limit) {
            return text;
        }
        String tail = lastLinesWithin(text, limit);
        long removed = size - utf8Length(tail);
        return "[... " + removed + " bytes truncated ...]\n" + tail;
    }

    private static String lastLinesWithin(String text, long limit) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int offset = (int) (bytes.length - limit);
        String tail = new String(bytes, offset, (int) limit, StandardCharsets.UTF_8);
        int newline = tail.indexOf('\n');
        return newline < 0 ? tail : tail.substring(newline + 1);
    }

    private static String omittedMarker(int omittedJobs) {
        return "[... " + omittedJobs + " more jobs omitted: total log size limit reached ...]\n";
    }

    private static long utf8Length(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
