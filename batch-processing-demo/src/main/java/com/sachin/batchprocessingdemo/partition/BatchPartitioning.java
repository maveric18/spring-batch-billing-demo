package com.sachin.batchprocessingdemo.partition;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class BatchPartitioning implements Partitioner {

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int min = 1;
        int max = 1000;
        int targetSize = (max - min) / gridSize + 1;
        System.out.println("target Size==" + targetSize);
        Map<String, ExecutionContext> partitions = new HashMap<>();

        int number = 0;
        int start = min;
        int end = start + targetSize - 1;

        while (start <= max) {
            ExecutionContext executionContext = new ExecutionContext();
            partitions.put("partition " + number, executionContext);

            if (end >= max) {
                end = max;
            }

            executionContext.put("minValue ", start);
            executionContext.put("maxValue ", end);
            start += targetSize;
            end += targetSize;
            number++;
        }
        System.out.println("Partition result : " + partitions.toString());
        return partitions;
    }
}
