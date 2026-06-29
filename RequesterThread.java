import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RequesterThread implements Runnable {
    private final int requesterId;
    private final ConcurrentMemoryManager memoryManager;
    private final int numberOfRequests;
    private final int minSizeBytes;
    private final int maxSizeBytes;
    private final Random random;

    public RequesterThread(
            int requesterId,
            ConcurrentMemoryManager memoryManager,
            int numberOfRequests,
            int minSizeBytes,
            int maxSizeBytes
    ) {
        this.requesterId = requesterId;
        this.memoryManager = memoryManager;
        this.numberOfRequests = numberOfRequests;
        this.minSizeBytes = minSizeBytes;
        this.maxSizeBytes = maxSizeBytes;
        this.random = new Random(System.nanoTime() + requesterId);
    }

    @Override
    public void run() {
        List<Integer> alocacoesAtivas = new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {
            int requestSize = random.nextInt((maxSizeBytes - minSizeBytes) + 1) + minSizeBytes;
            int id = memoryManager.allocate(requestSize);

            if (id != -1) {
                alocacoesAtivas.add(id);
            }

            if (i % 2 == 0 && !alocacoesAtivas.isEmpty()) {
                int idRemover = alocacoesAtivas.remove(0);
                memoryManager.free(idRemover);
            }
        }

        for (int id : alocacoesAtivas) {
            memoryManager.free(id);
        }

        System.out.println("Thread requisitante " + requesterId + " finalizada.");
    }
}
