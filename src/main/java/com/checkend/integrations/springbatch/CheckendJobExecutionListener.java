package com.checkend.integrations.springbatch;

import com.checkend.Checkend;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Batch JobExecutionListener that captures job context and reports errors to Checkend.
 *
 * <p>Usage in a Spring Batch job configuration:
 * <pre>{@code
 * @Bean
 * public Job myJob(JobRepository jobRepository) {
 *     return new JobBuilder("myJob", jobRepository)
 *         .listener(new CheckendJobExecutionListener())
 *         .start(myStep())
 *         .build();
 * }
 * }</pre>
 *
 * <p>Or as a Spring bean:
 * <pre>{@code
 * @Bean
 * public CheckendJobExecutionListener checkendJobListener() {
 *     return new CheckendJobExecutionListener();
 * }
 * }</pre>
 */
public class CheckendJobExecutionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        try {
            Map<String, Object> jobContext = buildJobContext(jobExecution);
            Checkend.setContext(Map.of("spring_batch", jobContext));
        } catch (Exception e) {
            Checkend.getLogger().error("Error setting Spring Batch job context", e);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
            // Update context with final status
            Map<String, Object> jobContext = buildJobContext(jobExecution);
            Checkend.setContext(Map.of("spring_batch", jobContext));

            // Report errors if job failed
            if (jobExecution.getStatus() == BatchStatus.FAILED) {
                List<Throwable> exceptions = jobExecution.getAllFailureExceptions();

                if (!exceptions.isEmpty()) {
                    Map<String, Object> options = new HashMap<>();
                    options.put("tags", List.of("spring_batch", jobExecution.getJobInstance().getJobName()));

                    // Report each exception
                    for (Throwable exception : exceptions) {
                        Checkend.notify(exception, options);
                    }
                } else {
                    // Job failed but no exception recorded - create a synthetic one
                    String exitDescription = jobExecution.getExitStatus().getExitDescription();
                    RuntimeException syntheticException = new RuntimeException(
                        "Spring Batch job failed: " + jobExecution.getJobInstance().getJobName() +
                        (exitDescription != null ? " - " + exitDescription : "")
                    );

                    Map<String, Object> options = new HashMap<>();
                    options.put("tags", List.of("spring_batch", jobExecution.getJobInstance().getJobName()));
                    Checkend.notify(syntheticException, options);
                }
            }
        } catch (Exception e) {
            Checkend.getLogger().error("Error reporting Spring Batch job exception", e);
        } finally {
            // Always clear thread-local context
            Checkend.clear();
        }
    }

    private Map<String, Object> buildJobContext(JobExecution jobExecution) {
        Map<String, Object> context = new HashMap<>();

        // Job instance info
        context.put("job_name", jobExecution.getJobInstance().getJobName());
        context.put("job_instance_id", jobExecution.getJobInstance().getInstanceId());

        // Job execution info
        context.put("job_execution_id", jobExecution.getId());
        context.put("status", jobExecution.getStatus().name());
        context.put("exit_code", jobExecution.getExitStatus().getExitCode());

        // Timing info
        if (jobExecution.getStartTime() != null) {
            context.put("start_time", jobExecution.getStartTime().toString());
        }
        if (jobExecution.getEndTime() != null) {
            context.put("end_time", jobExecution.getEndTime().toString());
        }
        if (jobExecution.getCreateTime() != null) {
            context.put("create_time", jobExecution.getCreateTime().toString());
        }

        // Job parameters (be careful with sensitive data)
        if (jobExecution.getJobParameters() != null) {
            Map<String, Object> params = new HashMap<>();
            jobExecution.getJobParameters().getParameters().forEach((key, value) -> {
                if (value != null) {
                    params.put(key, value.getValue() != null ? value.getValue().toString() : null);
                }
            });
            context.put("parameters", params);
        }

        return context;
    }
}
