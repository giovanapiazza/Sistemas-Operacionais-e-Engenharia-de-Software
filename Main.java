import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // Configurações do simulador
        int tamanhoHeapKB = 100; 
        int minSizeBytes = 16;
        int maxSizeBytes = 1024; // 1 KB
        
        // Aumentei o número de requisições para 100.000 para forçar a CPU,
        // já que não teremos pausas (sleep) aqui. Faça o mesmo na versão paralela!
        int totalRequisicoes = 100000; 

        System.out.println("Iniciando simulação SEQUENCIAL (Best-Fit) com Heap de " + tamanhoHeapKB + " KB");

        MemoryManager manager = new MemoryManager(tamanhoHeapKB);
        Random random = new Random();
        
        // Lista para simular o controle de ciclo de vida das variáveis
        List<Integer> alocacoesAtivas = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Loop de simulação de carga de trabalho
        for (int i = 0; i < totalRequisicoes; i++) {
            
            // 1. Gera tamanho e Aloca
            int requestSize = random.nextInt((maxSizeBytes - minSizeBytes) + 1) + minSizeBytes;
            int id = manager.allocate(requestSize);
            
            if (id != -1) {
                alocacoesAtivas.add(id);
            }

            // 2. Simula o "Hold Time": A cada X iterações, a variável mais velha é devolvida.
            // Isso simula variáveis nascendo e morrendo de forma justa.
            if (i % 2 == 0 && !alocacoesAtivas.isEmpty()) {
                int idRemover = alocacoesAtivas.remove(0); // Pega a mais antiga da lista
                manager.free(idRemover);
            }
        }

        // 3. Ao final da execução, libera o que sobrou na memória
        for (int id : alocacoesAtivas) {
            manager.free(id);
        }

        long endTime = System.currentTimeMillis();

        // Exibe o resumo final
        manager.printSummary(endTime - startTime);

        int numeroThreads = 8;

        ConcurrentSimulation.run(
                tamanhoHeapKB,
                minSizeBytes,
                maxSizeBytes,
                totalRequisicoes,
                numeroThreads
        );
    }
}
