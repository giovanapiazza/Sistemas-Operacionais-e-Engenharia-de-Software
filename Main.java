import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // Configurações do simulador
        int tamanhoHeapKB = 500; 
        int minSizeBytes = 16;
        int maxSizeBytes = 1024; // 1 KB
        int totalRequisicoes = 100000; 

        System.out.println("Iniciando simulação SEQUENCIAL (Best-Fit) com Heap de " + tamanhoHeapKB + " KB");

        MemoryManager manager = new MemoryManager(tamanhoHeapKB);
        Random random = new Random();

        long startTime = System.currentTimeMillis();

        // Loop de simulação de carga de trabalho (Apenas aloca!)
        for (int i = 0; i < totalRequisicoes; i++) {
            int requestSize = random.nextInt((maxSizeBytes - minSizeBytes) + 1) + minSizeBytes;
            manager.allocate(requestSize);
        }

        long endTime = System.currentTimeMillis();

        // Exibe o resumo final
        manager.printSummary(endTime - startTime);

        // Dispara a simulação paralela
        int numeroThreads = 6;
        ConcurrentSimulation.run(
                tamanhoHeapKB,
                minSizeBytes,
                maxSizeBytes,
                totalRequisicoes,
                numeroThreads
        );
    }
}