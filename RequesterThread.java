import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RequesterThread implements Runnable {
    private final int requesterId;
    private final MemoryManager memoryManager;
    private final int numberOfRequests;
    private final int minSizeBytes;
    private final int maxSizeBytes;
    private final int minHoldTimeMs;
    private final int maxHoldTimeMs;
    private final int minIntervalBetweenRequestsMs;
    private final int maxIntervalBetweenRequestsMs;
    private final Random random;

    private static class LocalAllocation {
        int allocationId;
        long releaseAtMillis;

        LocalAllocation(int allocationId, long releaseAtMillis) {
            this.allocationId = allocationId;
            this.releaseAtMillis = releaseAtMillis;
        }
    }

    public RequesterThread(
            int requesterId,
            MemoryManager memoryManager,
            int numberOfRequests,
            int minSizeBytes,
            int maxSizeBytes,
            int minHoldTimeMs,
            int maxHoldTimeMs
    ) {
        this(
                requesterId,
                memoryManager,
                numberOfRequests,
                minSizeBytes,
                maxSizeBytes,
                minHoldTimeMs,
                maxHoldTimeMs,
                1,
                5
        );
    }

    public RequesterThread(
            int requesterId,
            MemoryManager memoryManager,
            int numberOfRequests,
            int minSizeBytes,
            int maxSizeBytes,
            int minHoldTimeMs,
            int maxHoldTimeMs,
            int minIntervalBetweenRequestsMs,
            int maxIntervalBetweenRequestsMs
    ) {
        this.requesterId = requesterId;
        this.memoryManager = memoryManager;
        this.numberOfRequests = numberOfRequests;
        this.minSizeBytes = minSizeBytes;
        this.maxSizeBytes = maxSizeBytes;
        this.minHoldTimeMs = minHoldTimeMs;
        this.maxHoldTimeMs = maxHoldTimeMs;
        this.minIntervalBetweenRequestsMs = minIntervalBetweenRequestsMs;
        this.maxIntervalBetweenRequestsMs = maxIntervalBetweenRequestsMs;
        this.random = new Random(System.nanoTime() + requesterId);
    }

    @Override
    public void run() {
        List<LocalAllocation> localAllocations = new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {
            int requestSize = randomBetween(minSizeBytes, maxSizeBytes);

            int allocationId = memoryManager.allocate(requestSize);

            if (allocationId != -1) {
                int holdTime = randomBetween(minHoldTimeMs, maxHoldTimeMs);
                long releaseAt = System.currentTimeMillis() + holdTime;

                localAllocations.add(
                        new LocalAllocation(allocationId, releaseAt)
                );
            }

            releaseExpiredAllocations(localAllocations);

            sleepQuietly(
                    randomBetween(
                            minIntervalBetweenRequestsMs,
                            maxIntervalBetweenRequestsMs
                    )
            );
        }

        for (LocalAllocation allocation : localAllocations) {
            memoryManager.free(allocation.allocationId);
        }

        System.out.println("Thread requisitante "
                + requesterId
                + " finalizada.");
    }

    private void releaseExpiredAllocations(List<LocalAllocation> localAllocations) {
        long now = System.currentTimeMillis();

        Iterator<LocalAllocation> iterator = localAllocations.iterator();

        while (iterator.hasNext()) {
            LocalAllocation allocation = iterator.next();

            if (now >= allocation.releaseAtMillis) {
                memoryManager.free(allocation.allocationId);
                iterator.remove();
            }
        }
    }

    private int randomBetween(int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException(
                    "Valor máximo não pode ser menor que o mínimo."
            );
        }

        return random.nextInt((max - min) + 1) + min;
    }

    private void sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
