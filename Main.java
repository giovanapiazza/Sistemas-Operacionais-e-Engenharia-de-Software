import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // Configurações do simulador
        int tamanhoHeapKB = 100; 
        int minSizeBytes = 16;
        int maxSizeBytes = 1024; // 1 KB
        int totalRequisicoes = 10000;

        System.out.println("Iniciando simulação com Heap de " + tamanhoHeapKB + " KB");

        MemoryManager manager = new MemoryManager(tamanhoHeapKB);
        Random random = new Random();

        long startTime = System.currentTimeMillis();

        // Loop de simulação (Gerador de Requisições Aleatórias)
        for (int i = 0; i < totalRequisicoes; i++) {
            // Gera um tamanho aleatório entre o mínimo e o máximo estipulado
            int requestSize = random.nextInt((maxSizeBytes - minSizeBytes) + 1) + minSizeBytes;
            manager.allocate(requestSize);
        }

        long endTime = System.currentTimeMillis();

        // Exibe o resumo final
        manager.printSummary(endTime - startTime);
    }
}