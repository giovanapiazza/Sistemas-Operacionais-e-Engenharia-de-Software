import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MemoryManager {
    private int[] heap;
    private final Map<Integer, Block> allocations;
    private final Random random;
    private int nextId;

    private long totalAllocationRequests;
    private long totalRequestsAttended;
    private long totalAllocationFailures;
    private long totalAllocatedBytes;
    private long totalFreedBytes;
    private int totalRemovedVariables;
    private int compactionsCount;

    private static class Block {
        int id;
        int startIndex;
        int length;
        int requestedSizeBytes;

        Block(int id, int startIndex, int length, int requestedSizeBytes) {
            this.id = id;
            this.startIndex = startIndex;
            this.length = length;
            this.requestedSizeBytes = requestedSizeBytes;
        }
    }

    public MemoryManager(int sizeInKb) {
        int arraySize = (sizeInKb * 1024) / 4;

        this.heap = new int[arraySize];
        this.allocations = new HashMap<>();
        this.random = new Random();
        this.nextId = 1;
    }

    public synchronized int allocate(int sizeInBytes) {
        totalAllocationRequests++;

        if (sizeInBytes <= 0) {
            totalAllocationFailures++;
            return -1;
        }

        int requiredInts = (int) Math.ceil(sizeInBytes / 4.0);

        if (requiredInts > heap.length) {
            totalAllocationFailures++;
            return -1;
        }

        int startIndex = findBestFit(requiredInts);

        if (startIndex == -1) {
            freeRandomMemoryInternal();
            compactInternal();

            startIndex = findBestFit(requiredInts);

            if (startIndex == -1) {
                totalAllocationFailures++;
                return -1;
            }
        }

        int currentId = nextId++;

        Block newBlock = new Block(
                currentId,
                startIndex,
                requiredInts,
                sizeInBytes
        );

        allocations.put(currentId, newBlock);

        for (int i = startIndex; i < startIndex + requiredInts; i++) {
            heap[i] = currentId;
        }

        totalRequestsAttended++;
        totalAllocatedBytes += sizeInBytes;

        return currentId;
    }

    public synchronized boolean free(int allocationId) {
        Block block = allocations.remove(allocationId);

        if (block == null) {
            return false;
        }

        clearBlock(block);

        totalRemovedVariables++;
        totalFreedBytes += block.requestedSizeBytes;

        return true;
    }

    public synchronized int freeRandomMemory() {
        return freeRandomMemoryInternal();
    }

    public synchronized void compact() {
        compactInternal();
    }

    private int findBestFit(int requiredInts) {
        int bestStartIndex = -1;
        int bestBlockSize = Integer.MAX_VALUE;

        int i = 0;

        while (i < heap.length) {
            if (heap[i] != 0) {
                i++;
                continue;
            }

            int freeStart = i;
            int freeSize = 0;

            while (i < heap.length && heap[i] == 0) {
                freeSize++;
                i++;
            }

            if (freeSize >= requiredInts && freeSize < bestBlockSize) {
                bestStartIndex = freeStart;
                bestBlockSize = freeSize;

                if (freeSize == requiredInts) {
                    break;
                }
            }
        }

        return bestStartIndex;
    }

    private int freeRandomMemoryInternal() {
        if (allocations.isEmpty()) {
            return 0;
        }

        int targetToFree = (int) Math.ceil(heap.length * 0.30);
        int freedSpace = 0;
        int removedCount = 0;

        List<Integer> allocatedIds = new ArrayList<>(allocations.keySet());
        Collections.shuffle(allocatedIds, random);

        for (int id : allocatedIds) {
            if (freedSpace >= targetToFree) {
                break;
            }

            Block block = allocations.remove(id);

            if (block != null) {
                clearBlock(block);

                freedSpace += block.length;
                totalRemovedVariables++;
                totalFreedBytes += block.requestedSizeBytes;
                removedCount++;
            }
        }

        return removedCount;
    }

    private void clearBlock(Block block) {
        for (int i = block.startIndex; i < block.startIndex + block.length; i++) {
            heap[i] = 0;
        }
    }

    private void compactInternal() {
        compactionsCount++;

        int writeIndex = 0;

        List<Block> activeBlocks = new ArrayList<>(allocations.values());

        activeBlocks.sort((b1, b2) ->
                Integer.compare(b1.startIndex, b2.startIndex)
        );

        int[] compactedHeap = new int[heap.length];

        for (Block block : activeBlocks) {
            block.startIndex = writeIndex;

            for (int i = 0; i < block.length; i++) {
                compactedHeap[writeIndex++] = block.id;
            }
        }

        heap = compactedHeap;
    }

    public synchronized int getActiveAllocationsCount() {
        return allocations.size();
    }

    public synchronized void printSummary(long executionTimeMs) {
        double avgSize = totalRequestsAttended == 0
                ? 0
                : (double) totalAllocatedBytes / totalRequestsAttended;

        System.out.println("\n--- Resumo da Execução Concorrente ---");
        System.out.println("Tempo total de execução: " + executionTimeMs + " ms");
        System.out.println("Total de requisições tentadas: " + totalAllocationRequests);
        System.out.println("Total de requisições atendidas: " + totalRequestsAttended);
        System.out.println("Falhas por falta de memória: " + totalAllocationFailures);
        System.out.println("Tamanho médio das variáveis alocadas: "
                + String.format("%.2f", avgSize) + " bytes");
        System.out.println("Total de bytes alocados: " + totalAllocatedBytes);
        System.out.println("Total de bytes liberados: " + totalFreedBytes);
        System.out.println("Total de variáveis removidas: " + totalRemovedVariables);
        System.out.println("Blocos ainda ativos no final: " + allocations.size());
        System.out.println("Chamadas de compactação: " + compactionsCount);
    }
}
