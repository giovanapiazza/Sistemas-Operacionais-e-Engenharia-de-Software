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

    public synchronized int allocate(int sizeInBytes) {
        totalRequestsAttempted++;

        int requiredInts = (int) Math.ceil(sizeInBytes / 4.0);

        if (requiredInts <= 0 || requiredInts > heap.length) {
            totalFailures++;
            return -1;
        }

        int startIndex = findFreeSpace(requiredInts);

        if (startIndex == -1) {
            freeRandomMemoryInternal();
            compactInternal();
            startIndex = findFreeSpace(requiredInts);

            if (startIndex == -1) {
                totalFailures++;
                return -1;
            }
        }

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

    public synchronized void free(int id) {
        ConcurrentBlock block = allocations.remove(id);

        if (block != null) {
            for (int i = block.startIndex; i < block.startIndex + block.length; i++) {
                heap[i] = 0;
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

    private int findFreeSpace(int requiredInts) {
        int bestStartIndex = -1;
        int bestSize = Integer.MAX_VALUE;
        int currentStartIndex = -1;
        int currentSize = 0;

        for (int i = 0; i < heap.length; i++) {
            if (heap[i] == 0) {
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
}
