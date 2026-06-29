public class ConcurrentSimulation {
    public static void run(
            int tamanhoHeapKB,
            int minSizeBytes,
            int maxSizeBytes,
            int totalRequisicoes,
            int numeroThreads
    ) {
        System.out.println("\nIniciando simulação CONCORRENTE (Best-Fit) com Heap de " + tamanhoHeapKB + " KB");
        System.out.println("Threads requisitantes: " + numeroThreads);

        ConcurrentMemoryManager manager = new ConcurrentMemoryManager(tamanhoHeapKB);
        Thread[] threads = new Thread[numeroThreads];

        int requestsPerThread = totalRequisicoes / numeroThreads;
        int remainingRequests = totalRequisicoes % numeroThreads;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numeroThreads; i++) {
            int requestsForThisThread = requestsPerThread + (i < remainingRequests ? 1 : 0);

            RequesterThread requester = new RequesterThread(
                    i + 1,
                    manager,
                    requestsForThisThread,
                    minSizeBytes,
                    maxSizeBytes
            );

            threads[i] = new Thread(requester, "RequesterThread-" + (i + 1));
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread principal interrompida enquanto aguardava as threads concorrentes.");
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        manager.printSummary(endTime - startTime);
    }
}
