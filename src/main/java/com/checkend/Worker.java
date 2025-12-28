package com.checkend;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Background worker for async notice sending with rate limiting support.
 */
public final class Worker {
    private static final int[] RETRY_DELAYS_MS = {100, 200, 400};
    private static final int MAX_RETRIES = 3;
    private static final double BASE_THROTTLE = 1.05;
    private static final long MAX_THROTTLE_MS = 100_000; // 100 seconds
    private static final long DEFAULT_RATE_LIMIT_BACKOFF_MS = 60_000; // 1 minute

    private final Configuration config;
    private final Client client;
    private final Logger logger;
    private final BlockingQueue<Notice> queue;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    private final AtomicLong throttleDelayMs;
    private final AtomicLong rateLimitedUntil;

    public Worker(Configuration config, Client client) {
        this.config = config;
        this.client = client;
        this.logger = config.getLogger();
        this.queue = new LinkedBlockingQueue<>(config.getMaxQueueSize());
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "checkend-worker");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(true);
        this.throttleDelayMs = new AtomicLong(0);
        this.rateLimitedUntil = new AtomicLong(0);
        startWorker();
    }

    private void startWorker() {
        executor.submit(() -> {
            while (running.get() || !queue.isEmpty()) {
                try {
                    // Check if we're rate limited
                    long rateLimitEnd = rateLimitedUntil.get();
                    if (rateLimitEnd > 0) {
                        long waitTime = rateLimitEnd - System.currentTimeMillis();
                        if (waitTime > 0) {
                            logger.debug("Rate limited, waiting " + waitTime + "ms");
                            Thread.sleep(Math.min(waitTime, 1000));
                            continue;
                        } else {
                            rateLimitedUntil.set(0);
                            logger.info("Rate limit period ended, resuming");
                        }
                    }

                    // Apply throttle delay if set
                    long throttle = throttleDelayMs.get();
                    if (throttle > 0) {
                        Thread.sleep(throttle);
                    }

                    Notice notice = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (notice != null) {
                        sendWithRetry(notice);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void sendWithRetry(Notice notice) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                Client.Response response = client.send(notice);

                if (response.isSuccess()) {
                    logger.debug("Notice sent successfully");
                    decreaseThrottle();
                    return;
                }

                // Rate limited (429)
                if (response.isRateLimited()) {
                    long backoffMs = response.getRetryAfterMs(DEFAULT_RATE_LIMIT_BACKOFF_MS);
                    logger.warn("Rate limited by server, backing off for " + backoffMs + "ms");
                    rateLimitedUntil.set(System.currentTimeMillis() + backoffMs);
                    increaseThrottle();
                    // Re-queue the notice for later
                    queue.offer(notice);
                    return;
                }

                // Client errors (4xx except 429): Don't retry
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    logger.warn("Client error, not retrying: " + response.statusCode() + " - " + response.body());
                    return;
                }

                // Server errors (5xx): Retry with exponential backoff
                increaseThrottle();
                if (attempt < MAX_RETRIES - 1) {
                    logger.debug("Retrying after server error: " + response.statusCode());
                    Thread.sleep(RETRY_DELAYS_MS[attempt]);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                logger.error("Error sending notice: " + e.getMessage());
                increaseThrottle();
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAYS_MS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        logger.error("Failed to send notice after " + MAX_RETRIES + " attempts");
    }

    private void increaseThrottle() {
        long current = throttleDelayMs.get();
        long newDelay = current == 0 ? 100 : (long) (current * BASE_THROTTLE);
        throttleDelayMs.set(Math.min(newDelay, MAX_THROTTLE_MS));
    }

    private void decreaseThrottle() {
        long current = throttleDelayMs.get();
        if (current > 0) {
            long newDelay = (long) (current / BASE_THROTTLE);
            throttleDelayMs.set(newDelay < 10 ? 0 : newDelay);
        }
    }

    /**
     * Queue a notice for sending.
     * @return true if queued successfully, false if queue is full
     */
    public boolean enqueue(Notice notice) {
        if (!running.get()) {
            return false;
        }
        boolean added = queue.offer(notice);
        if (!added) {
            logger.warn("Queue full, notice dropped");
        }
        return added;
    }

    /**
     * Wait for all pending notices to be sent.
     */
    public void flush() {
        flush(30000);
    }

    /**
     * Wait for all pending notices to be sent with timeout.
     */
    public void flush(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!queue.isEmpty() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Stop the worker gracefully.
     */
    public void stop() {
        running.set(false);
        executor.shutdown();
        try {
            int shutdownTimeout = config.getShutdownTimeout();
            if (!executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if the worker is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Get the current queue size.
     */
    public int queueSize() {
        return queue.size();
    }

    /**
     * Check if the worker is currently rate limited.
     */
    public boolean isRateLimited() {
        return rateLimitedUntil.get() > System.currentTimeMillis();
    }

    /**
     * Get the current throttle delay in milliseconds.
     */
    public long getThrottleDelayMs() {
        return throttleDelayMs.get();
    }
}
