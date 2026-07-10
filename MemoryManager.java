import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

class MemoryManager {
    private int[] heap;
    private Map<Integer, Block> allocations;
    private int nextId;
    private Random random;

    // Estatísticas
    private int totalRequestsAttended;
    private long totalAllocatedBytes;
    private int totalRemovedVariables;
    private int compactionsCount;

    // Representa um bloco alocado na memória
    private static class Block {
        int id;
        int startIndex;
        int length; // Em inteiros (cada inteiro = 4 bytes)

        Block(int id, int startIndex, int length) {
            this.id = id;
            this.startIndex = startIndex;
            this.length = length;
        }
    }

    public MemoryManager(int sizeInKb) {
        // Cada inteiro equivale a 4 bytes, logo: sizeInKb * 1024 bytes / 4 bytes = tamanho do vetor
        int arraySize = (sizeInKb * 1024) / 4;
        this.heap = new int[arraySize];
        this.allocations = new HashMap<>();
        this.nextId = 1;
        this.random = new Random();
    }

    // Agora retorna o ID alocado ou -1 se falhar
    public int allocate(int sizeInBytes) {
        // Converte o tamanho pedido em bytes para a quantidade de inteiros necessários
        int requiredInts = (int) Math.ceil(sizeInBytes / 4.0);
        
        int startIndex = findFreeSpace(requiredInts); // Usa Best-Fit

        // Se não houver espaço suficiente, chama o algoritmo de liberação e compactação
        if (startIndex == -1) {
            freeRandomMemory();
            compact();
            startIndex = findFreeSpace(requiredInts);
            
            // Se mesmo após limpar e compactar não couber, ignoramos a requisição
            if (startIndex == -1) return -1; 
        }

        // Realiza a alocação
        int currentId = nextId++;
        Block newBlock = new Block(currentId, startIndex, requiredInts);
        allocations.put(currentId, newBlock);

        // Escreve o ID em todas as posições da heap ocupadas pela variável
        for (int i = startIndex; i < startIndex + requiredInts; i++) {
            heap[i] = currentId;
        }

        // Atualiza estatísticas
        totalRequestsAttended++;
        totalAllocatedBytes += sizeInBytes;
        
        return currentId;
    }

    // Método para liberar variáveis ativamente 
    public void free(int id) {
        Block block = allocations.remove(id);
        if (block != null) {
            for (int i = block.startIndex; i < block.startIndex + block.length; i++) {
                heap[i] = 0; // Marca como espaço livre
            }
        }
    }

    // Busca espaço contíguo usando BEST-FIT
    private int findFreeSpace(int requiredInts) {
        int bestStartIndex = -1;
        int bestSize = Integer.MAX_VALUE; // Começa com um tamanho "infinito"

        int currentStartIndex = -1;
        int currentSize = 0;

        for (int i = 0; i < heap.length; i++) {
            if (heap[i] == 0) { // 0 indica espaço livre
                if (currentSize == 0) {
                    currentStartIndex = i;
                }
                currentSize++;
            } else {
                // Fechou um buraco. Verifica se ele serve e se é o melhor até agora
                if (currentSize >= requiredInts && currentSize < bestSize) {
                    bestSize = currentSize;
                    bestStartIndex = currentStartIndex;
                }
                currentSize = 0; // Reseta para procurar o próximo buraco
            }
        }

        // Verifica o último bloco (caso o vetor termine com um espaço livre)
        if (currentSize >= requiredInts && currentSize < bestSize) {
            bestStartIndex = currentStartIndex;
        }

        return bestStartIndex;
    }

    // Libera pelo menos 30% da memória aleatoriamente
    private void freeRandomMemory() {
        int targetToFree = (int) (heap.length * 0.30); // 30% do vetor de inteiros
        int freedSpace = 0;

        List<Integer> allocatedIds = new ArrayList<>(allocations.keySet());
        Collections.shuffle(allocatedIds, random);

        for (int id : allocatedIds) {
            if (freedSpace >= targetToFree) break;

            Block block = allocations.remove(id);
            if (block != null) {
                // Zera as posições na heap
                for (int i = block.startIndex; i < block.startIndex + block.length; i++) {
                    heap[i] = 0;
                }
                freedSpace += block.length;
                totalRemovedVariables++;
            }
        }
    }

    // Compactação: move todas as variáveis para a esquerda para eliminar fragmentação externa
    private void compact() {
        compactionsCount++;
        int writeIndex = 0;

        // Lista ordenada pelo startIndex para manter a coerência ao compactar
        List<Block> activeBlocks = new ArrayList<>(allocations.values());
        activeBlocks.sort((b1, b2) -> Integer.compare(b1.startIndex, b2.startIndex));

        // Zera o heap inteiro para reescrever
        this.heap = new int[heap.length];

        for (Block block : activeBlocks) {
            block.startIndex = writeIndex;
            for (int i = 0; i < block.length; i++) {
                heap[writeIndex++] = block.id;
            }
        }
    }

    public void printSummary(long executionTimeMs) {
        double avgSize = totalRequestsAttended == 0 ? 0 : (double) totalAllocatedBytes / totalRequestsAttended;
        System.out.println("\n--- Resumo da Execução Sequencial ---");
        System.out.println("Tempo total de execução: " + executionTimeMs + " ms");
        System.out.println("Total de requisições atendidas: " + totalRequestsAttended);
        System.out.println("Tamanho médio das variáveis alocadas: " + String.format("%.2f", avgSize) + " bytes");
        System.out.println("Total de variáveis removidas por Evicção/Fim: " + totalRemovedVariables);
        System.out.println("Chamadas de compactação: " + compactionsCount);
    }
}