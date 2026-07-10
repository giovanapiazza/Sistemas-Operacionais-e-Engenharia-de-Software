import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================");
        System.out.println("   SIMULADOR DE GERENCIAMENTO DE MEMÓRIA (HEAP)   ");
        System.out.println("==================================================");

        // Coleta de dados do usuário
        System.out.print("Digite o tamanho da Heap (em KB): ");
        int tamanhoHeapKB = scanner.nextInt();

        System.out.print("Digite o tamanho mínimo da alocação (em bytes): ");
        int minSizeBytes = scanner.nextInt();

        System.out.print("Digite o tamanho máximo da alocação (em bytes): ");
        int maxSizeBytes = scanner.nextInt();

        System.out.print("Digite o número total de requisições: ");
        int totalRequisicoes = scanner.nextInt();

        System.out.print("Digite o número de threads para a simulação paralela: ");
        int numeroThreads = scanner.nextInt();

        System.out.println("\n--------------------------------------------------");

        // 1. Execução Sequencial
        System.out.println("Iniciando simulação SEQUENCIAL (Best-Fit) com Heap de " + tamanhoHeapKB + " KB");
        MemoryManager manager = new MemoryManager(tamanhoHeapKB);
        Random random = new Random();

        long startTimeSeq = System.currentTimeMillis();
        for (int i = 0; i < totalRequisicoes; i++) {
            int requestSize = random.nextInt((maxSizeBytes - minSizeBytes) + 1) + minSizeBytes;
            manager.allocate(requestSize);
        }
        long endTimeSeq = System.currentTimeMillis();
        manager.printSummary(endTimeSeq - startTimeSeq);

        System.out.println("--------------------------------------------------");

        // 2. Execução Paralela
        ConcurrentSimulation.run(
                tamanhoHeapKB,
                minSizeBytes,
                maxSizeBytes,
                totalRequisicoes,
                numeroThreads
        );
        
        scanner.close();
    }
}