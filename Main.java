public class Main {
    public static void main(String[] args) {
        int tamanhoHeapKB = 100;
        int minSizeBytes = 16;
        int maxSizeBytes = 1024;
        int totalRequisicoes = 10000;
        int numeroThreads = 8;

        int minHoldTimeMs = 10;
        int maxHoldTimeMs = 50;

        if (args.length >= 1) {
            numeroThreads = Integer.parseInt(args[0]);
        }

        if (args.length >= 2) {
            totalRequisicoes = Integer.parseInt(args[1]);
        }

        if (args.length >= 3) {
            tamanhoHeapKB = Integer.parseInt(args[2]);
        }

        if (numeroThreads <= 0) {
            throw new IllegalArgumentException(
                    "O número de threads deve ser maior que zero."
            );
        }

        System.out.println("Iniciando simulação concorrente com Heap de "
                + tamanhoHeapKB
                + " KB");

        System.out.println("Threads requisitantes: " + numeroThreads);
        System.out.println("Total de requisições planejadas: " + totalRequisicoes);

        MemoryManager manager = new MemoryManager(tamanhoHeapKB);

        Thread[] threads = new Thread[numeroThreads];

        int requestsPerThread = totalRequisicoes / numeroThreads;
        int remainingRequests = totalRequisicoes % numeroThreads;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numeroThreads; i++) {
            int requestsForThisThread =
                    requestsPerThread + (i < remainingRequests ? 1 : 0);

            RequesterThread requester = new RequesterThread(
                    i + 1,
                    manager,
                    requestsForThisThread,
                    minSizeBytes,
                    maxSizeBytes,
                    minHoldTimeMs,
                    maxHoldTimeMs
            );

            threads[i] = new Thread(
                    requester,
                    "RequesterThread-" + (i + 1)
            );

            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                System.out.println(
                        "Thread principal interrompida enquanto aguardava finalização."
                );

                break;
            }
        }

        long endTime = System.currentTimeMillis();

        manager.printSummary(endTime - startTime);
    }
}
