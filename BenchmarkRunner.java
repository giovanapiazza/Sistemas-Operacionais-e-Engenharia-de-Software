import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class BenchmarkRunner {
    public static void main(String[] args) {
        // Os três cenários de memória que provam a nossa teoria do gargalo
        int[] heapSizesKB = {30, 100, 500}; 
        // Os cenários de threads
        int[] cenariosThreads = {2, 4, 6, 8, 12, 16, 24, 32};
        
        // Reduzi para 5 repetições para o teste completo não demorar horas
        int repeticoesPorCenario = 5; 
        
        int totalRequisicoes = 100000; 
        int minSizeBytes = 16;
        int maxSizeBytes = 1024;

        String nomeArquivoCsv = "resultados_finais_multiplos_heaps.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(nomeArquivoCsv))) {
            // Novo cabeçalho do CSV
            writer.println("HeapSizeKB,Modo,Threads,Rodada,TempoMs");
            
            System.out.println("=== INICIANDO SUPER BENCHMARK ===");

            for (int heapSize : heapSizesKB) {
                System.out.println("\n==================================================");
                System.out.println(">>> INICIANDO BATERIA PARA HEAP DE " + heapSize + " KB <<<");
                System.out.println("==================================================");

                // -----------------------------------------------------------
                // 1. TESTE SEQUENCIAL (A nossa linha de base)
                // -----------------------------------------------------------
                System.out.println("\n[Modo Sequencial] Executando...");
                for (int rodada = 1; rodada <= repeticoesPorCenario; rodada++) {
                    long startTime = System.nanoTime();
                    
                    MemoryManager manager = new MemoryManager(heapSize);
                    Random random = new Random();
                    for (int i = 0; i < totalRequisicoes; i++) {
                        int requestSize = random.nextInt((maxSizeBytes - minSizeBytes) + 1) + minSizeBytes;
                        manager.allocate(requestSize);
                    }
                    
                    long endTime = System.nanoTime();
                    long tempoGasto = (endTime - startTime) / 1_000_000;
                    
                    // Salvamos informando que é "Sequencial" e Thread "1" (simbólico)
                    writer.println(heapSize + ",Sequencial,1," + rodada + "," + tempoGasto);
                    writer.flush();
                    System.out.print(tempoGasto + "ms ");
                }
                System.out.println();

                // -----------------------------------------------------------
                // 2. TESTE CONCORRENTE
                // -----------------------------------------------------------
                for (int threads : cenariosThreads) {
                    System.out.println("\n[Modo Concorrente] " + threads + " threads...");
                    for (int rodada = 1; rodada <= repeticoesPorCenario; rodada++) {
                        long startTime = System.nanoTime();
                        
                        ConcurrentSimulation.run(heapSize, minSizeBytes, maxSizeBytes, totalRequisicoes, threads);
                        
                        long endTime = System.nanoTime();
                        long tempoGasto = (endTime - startTime) / 1_000_000;
                        
                        writer.println(heapSize + ",Concorrente," + threads + "," + rodada + "," + tempoGasto);
                        writer.flush();
                        System.out.print(tempoGasto + "ms ");
                    }
                    System.out.println();
                }
            }
            
            System.out.println("\n=== TODOS OS TESTES FINALIZADOS! ===");
            System.out.println("Arquivo gerado: " + nomeArquivoCsv);

        } catch (IOException e) {
            System.err.println("Erro ao salvar CSV: " + e.getMessage());
        }
    }
}