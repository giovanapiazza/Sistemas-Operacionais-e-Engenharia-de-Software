import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ConcurrentMemoryManager {
    private int[] heap;
    private final Map<Integer, ConcurrentBlock> allocations;
    private int nextId;
    private final Random random;

    private int totalRequestsAttempted;
    private int totalRequestsAttended;
    private long totalAllocatedBytes;
    private int totalRemovedVariables;
    private int compactionsCount;
    private int totalFailures;

    private static class ConcurrentBlock {
        int id;
        int startIndex;
        int length;

        ConcurrentBlock(int id, int startIndex, int length) {
            this.id = id;
            this.startIndex = startIndex;
            this.length = length;
        }
    }

    public ConcurrentMemoryManager(int sizeInKb) {
        int arraySize = (sizeInKb * 1024) / 4;
        this.heap = new int[arraySize];
        this.allocations = new HashMap<>();
        this.nextId = 1;
        this.random = new Random();
    }

    // REMOVIDO o 'synchronized' do método para permitir busca paralela!
    public int allocate(int sizeInBytes) {
        int requiredInts = (int) Math.ceil(sizeInBytes / 4.0);

        if (requiredInts <= 0 || requiredInts > heap.length) {
            synchronized (this) { totalFailures++; }
            return -1;
        }

        // Laço de Repetição: Se formos roubados, voltamos para o início e tentamos de novo!
        while (true) {
            // 1. BUSCA OTIMISTA: Procura o melhor buraco FORA do lock (Em paralelo)
            int candidateIndex = findFreeSpace(requiredInts);

            // 2. Entra no lock apenas para validar ou resolver falta de memória
            synchronized (this) {
                
                // Caso 1: Achamos um candidato lá fora e ele CONTINUA LIVRE. Sucesso!
                if (candidateIndex != -1 && isSpaceStillFree(candidateIndex, requiredInts)) {
                    totalRequestsAttempted++;
                    return executeAllocation(candidateIndex, requiredInts, sizeInBytes);
                }

                // Caso 2: Memória LOTADA (candidateIndex == -1). 
                // Precisamos verificar de novo, liberar espaço e compactar.
                if (candidateIndex == -1) {
                    totalRequestsAttempted++;
                    
                    // Double-check: outra thread pode ter compactado enquanto esperávamos na porta
                    int doubleCheck = findFreeSpace(requiredInts);
                    
                    if (doubleCheck == -1) {
                        freeRandomMemoryInternal();
                        compactInternal();
                        doubleCheck = findFreeSpace(requiredInts);
                    }

                    if (doubleCheck != -1) {
                        return executeAllocation(doubleCheck, requiredInts, sizeInBytes);
                    } else {
                        totalFailures++;
                        return -1;
                    }
                }
                
                // Caso 3 (A MÁGICA): candidateIndex não era -1, mas isSpaceStillFree deu FALSO.
                // Isso significa que outra thread "roubou" nosso buraco.
                // O código simplesmente ignora, o bloco synchronized termina aqui, 
                // e o laço 'while' faz a thread tentar a busca novamente FORA do lock!
            }
        }
    }

    // Método auxiliar para realizar a gravação dos dados (Sempre chamado dentro do synchronized)
    private int executeAllocation(int startIndex, int requiredInts, int sizeInBytes) {
        int currentId = nextId++;
        ConcurrentBlock newBlock = new ConcurrentBlock(currentId, startIndex, requiredInts);
        allocations.put(currentId, newBlock);

        for (int i = startIndex; i < startIndex + requiredInts; i++) {
            heap[i] = currentId;
        }

        totalRequestsAttended++;
        totalAllocatedBytes += sizeInBytes;
        return currentId;
    }

    // Validação ultra rápida se o bloco candidato continua livre (Sempre chamado dentro do synchronized)
    private boolean isSpaceStillFree(int startIndex, int requiredInts) {
        if (startIndex + requiredInts > heap.length) return false;
        for (int i = startIndex; i < startIndex + requiredInts; i++) {
            if (heap[i] != 0) {
                return false; // Alguém ocupou esse pedaço enquanto estávamos fora do lock
            }
        }
        return true;
    }

    // Liberação de memória precisa continuar totalmente sincronizada
    public synchronized void free(int id) {
        ConcurrentBlock block = allocations.remove(id);
        if (block != null) {
            for (int i = block.startIndex; i < block.startIndex + block.length; i++) {
                heap[i] = 0;
            }
        }
    }

    // Modificado sutilmente para garantir segurança ao ler a referência do heap sem lock
    private int findFreeSpace(int requiredInts) {
        int[] currentHeap = this.heap; // Proteção local contra troca de referência no compact()
        int bestStartIndex = -1;
        int bestSize = Integer.MAX_VALUE;

        int currentStartIndex = -1;
        int currentSize = 0;

        for (int i = 0; i < currentHeap.length; i++) {
            if (currentHeap[i] == 0) {
                if (currentSize == 0) {
                    currentStartIndex = i;
                }
                currentSize++;
            } else {
                if (currentSize >= requiredInts && currentSize < bestSize) {
                    bestSize = currentSize;
                    bestStartIndex = currentStartIndex;
                }
                currentSize = 0;
            }
        }

        if (currentSize >= requiredInts && currentSize < bestSize) {
            bestStartIndex = currentStartIndex;
        }

        return bestStartIndex;
    }

    private void freeRandomMemoryInternal() {
        int targetToFree = (int) (heap.length * 0.30);
        int freedSpace = 0;

        List<Integer> allocatedIds = new ArrayList<>(allocations.keySet());
        Collections.shuffle(allocatedIds, random);

        for (int id : allocatedIds) {
            if (freedSpace >= targetToFree) {
                break;
            }

            ConcurrentBlock block = allocations.remove(id);

            if (block != null) {
                for (int i = block.startIndex; i < block.startIndex + block.length; i++) {
                    heap[i] = 0;
                }

                freedSpace += block.length;
                totalRemovedVariables++;
            }
        }
    }

    private void compactInternal() {
        compactionsCount++;
        int writeIndex = 0;

        List<ConcurrentBlock> activeBlocks = new ArrayList<>(allocations.values());
        activeBlocks.sort((b1, b2) -> Integer.compare(b1.startIndex, b2.startIndex));

        this.heap = new int[heap.length];

        for (ConcurrentBlock block : activeBlocks) {
            block.startIndex = writeIndex;

            for (int i = 0; i < block.length; i++) {
                heap[writeIndex++] = block.id;
            }
        }
    }

    public synchronized void printSummary(long executionTimeMs) {
        double avgSize = totalRequestsAttended == 0 ? 0 : (double) totalAllocatedBytes / totalRequestsAttended;
        System.out.println("\n--- Resumo da Execução Concorrente ---");
        System.out.println("Tempo total de execução: " + executionTimeMs + " ms");
        System.out.println("Total de requisições tentadas: " + totalRequestsAttempted);
        System.out.println("Total de requisições atendidas: " + totalRequestsAttended);
        System.out.println("Falhas por falta de memória: " + totalFailures);
        System.out.println("Tamanho médio das variáveis alocadas: " + String.format("%.2f", avgSize) + " bytes");
        System.out.println("Total de variáveis removidas por Evicção/Fim: " + totalRemovedVariables);
        System.out.println("Blocos ainda ativos no final: " + allocations.size());
        System.out.println("Chamadas de compactação: " + compactionsCount);
    }
}