package com.checkend.integrations.quartz;

import com.checkend.Checkend;
import com.checkend.filters.SanitizeFilter;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Quartz JobListener that captures job context and reports errors to Checkend.
 *
 * <p>Usage with Quartz Scheduler:
 * <pre>{@code
 * Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
 * scheduler.getListenerManager().addJobListener(new CheckendJobListener());
 * }</pre>
 *
 * <p>Or with a specific group:
 * <pre>{@code
 * scheduler.getListenerManager().addJobListener(
 *     new CheckendJobListener(),
 *     GroupMatcher.groupEquals("myGroup")
 * );
 * }</pre>
 */
public class CheckendJobListener implements JobListener {
    private static final String LISTENER_NAME = "CheckendJobListener";

    private final String name;

    public CheckendJobListener() {
        this.name = LISTENER_NAME;
    }

    public CheckendJobListener(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        try {
            Map<String, Object> jobContext = buildJobContext(context);
            Checkend.setContext(Map.of("quartz", jobContext));
        } catch (Exception e) {
            Checkend.getLogger().error("Error setting Quartz job context", e);
        }
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // Clear context when job is vetoed
        Checkend.clear();
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        try {
            if (jobException != null) {
                // Build options with job context
                Map<String, Object> options = new HashMap<>();
                options.put("tags", java.util.List.of("quartz", context.getJobDetail().getKey().getGroup()));

                // Report the exception
                Checkend.notify(jobException, options);
            }
        } catch (Exception e) {
            Checkend.getLogger().error("Error reporting Quartz job exception", e);
        } finally {
            // Always clear thread-local context
            Checkend.clear();
        }
    }

    private Map<String, Object> buildJobContext(JobExecutionContext context) {
        Map<String, Object> jobContext = new HashMap<>();

        // Job key info
        jobContext.put("job_name", context.getJobDetail().getKey().getName());
        jobContext.put("job_group", context.getJobDetail().getKey().getGroup());
        jobContext.put("job_class", context.getJobDetail().getJobClass().getName());

        // Trigger info
        if (context.getTrigger() != null) {
            jobContext.put("trigger_name", context.getTrigger().getKey().getName());
            jobContext.put("trigger_group", context.getTrigger().getKey().getGroup());
        }

        // Execution info
        jobContext.put("fire_instance_id", context.getFireInstanceId());
        jobContext.put("fire_time", context.getFireTime() != null ? context.getFireTime().toInstant().toString() : null);
        jobContext.put("refire_count", context.getRefireCount());
        jobContext.put("recovering", context.isRecovering());

        // Sanitize job data map (may contain sensitive info)
        if (context.getMergedJobDataMap() != null && !context.getMergedJobDataMap().isEmpty()) {
            Map<String, Object> dataMap = new HashMap<>();
            for (String key : context.getMergedJobDataMap().keySet()) {
                Object value = context.getMergedJobDataMap().get(key);
                if (value != null) {
                    dataMap.put(key, value.toString());
                }
            }
            Set<String> filterKeys = Checkend.getConfiguration() != null
                ? Checkend.getConfiguration().getFilterKeys()
                : Set.of();
            jobContext.put("job_data", SanitizeFilter.filter(dataMap, filterKeys));
        }

        return jobContext;
    }
}
