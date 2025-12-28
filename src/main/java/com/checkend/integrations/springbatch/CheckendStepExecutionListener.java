package com.checkend.integrations.springbatch;

import com.checkend.Checkend;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Batch StepExecutionListener that captures step context and reports errors to Checkend.
 *
 * <p>This listener provides more granular error reporting at the step level,
 * including read/write/skip counts and step-specific context.
 *
 * <p>Usage in a Spring Batch step configuration:
 * <pre>{@code
 * @Bean
 * public Step myStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
 *     return new StepBuilder("myStep", jobRepository)
 *         .<Input, Output>chunk(100, transactionManager)
 *         .reader(reader())
 *         .processor(processor())
 *         .writer(writer())
 *         .listener(new CheckendStepExecutionListener())
 *         .build();
 * }
 * }</pre>
 */
public class CheckendStepExecutionListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        try {
            Map<String, Object> stepContext = buildStepContext(stepExecution);

            // Merge with existing context (preserve job-level context)
            Map<String, Object> currentContext = Checkend.getContext();
            if (currentContext.containsKey("spring_batch")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> batchContext = (Map<String, Object>) currentContext.get("spring_batch");
                batchContext.put("step", stepContext);
                Checkend.setContext(Map.of("spring_batch", batchContext));
            } else {
                Checkend.setContext(Map.of("spring_batch", Map.of("step", stepContext)));
            }
        } catch (Exception e) {
            Checkend.getLogger().error("Error setting Spring Batch step context", e);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        try {
            // Update context with final step metrics
            Map<String, Object> stepContext = buildStepContext(stepExecution);

            Map<String, Object> currentContext = Checkend.getContext();
            if (currentContext.containsKey("spring_batch")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> batchContext = (Map<String, Object>) currentContext.get("spring_batch");
                batchContext.put("step", stepContext);
                Checkend.setContext(Map.of("spring_batch", batchContext));
            }

            // Report step-level failures
            if (stepExecution.getStatus().isUnsuccessful()) {
                List<Throwable> exceptions = stepExecution.getFailureExceptions();

                if (!exceptions.isEmpty()) {
                    String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();

                    Map<String, Object> options = new HashMap<>();
                    options.put("tags", List.of(
                        "spring_batch",
                        jobName,
                        "step:" + stepExecution.getStepName()
                    ));

                    for (Throwable exception : exceptions) {
                        Checkend.notify(exception, options);
                    }
                }
            }
        } catch (Exception e) {
            Checkend.getLogger().error("Error reporting Spring Batch step exception", e);
        }

        // Return null to not modify the exit status
        return null;
    }

    private Map<String, Object> buildStepContext(StepExecution stepExecution) {
        Map<String, Object> context = new HashMap<>();

        // Step identity
        context.put("step_name", stepExecution.getStepName());
        context.put("step_execution_id", stepExecution.getId());
        context.put("status", stepExecution.getStatus().name());
        context.put("exit_code", stepExecution.getExitStatus().getExitCode());

        // Metrics
        context.put("read_count", stepExecution.getReadCount());
        context.put("write_count", stepExecution.getWriteCount());
        context.put("commit_count", stepExecution.getCommitCount());
        context.put("rollback_count", stepExecution.getRollbackCount());
        context.put("read_skip_count", stepExecution.getReadSkipCount());
        context.put("process_skip_count", stepExecution.getProcessSkipCount());
        context.put("write_skip_count", stepExecution.getWriteSkipCount());
        context.put("filter_count", stepExecution.getFilterCount());

        // Timing
        if (stepExecution.getStartTime() != null) {
            context.put("start_time", stepExecution.getStartTime().toString());
        }
        if (stepExecution.getEndTime() != null) {
            context.put("end_time", stepExecution.getEndTime().toString());
        }

        return context;
    }
}
