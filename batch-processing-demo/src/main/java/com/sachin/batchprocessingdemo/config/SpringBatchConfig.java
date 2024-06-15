package com.sachin.batchprocessingdemo.config;

import com.sachin.batchprocessingdemo.CustomerWriter;
import com.sachin.batchprocessingdemo.entity.Customer;
import com.sachin.batchprocessingdemo.partition.BatchPartitioning;
import com.sachin.batchprocessingdemo.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
//@EnableBatchProcessing
@AllArgsConstructor
public class SpringBatchConfig {

    private JobRepository jobRepository;

//    private CustomerRepository customerRepository;    //used in normal writing

    private PlatformTransactionManager transactionManager;

    private CustomerWriter customerWriter;

    @Bean
    public FlatFileItemReader<Customer> customerReader() {
        FlatFileItemReader<Customer> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource("src/main/resources/customers.csv"));
        reader.setName("csvReader");
        reader.setLinesToSkip(1);
        reader.setLineMapper(customerLineMapper());
        return reader;
    }

    ;

    private LineMapper<Customer> customerLineMapper() {
        DefaultLineMapper<Customer> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames(new String[]{"id", "firstName", "lastName", "email", "gender", "contactNo", "country", "dob"});
        BeanWrapperFieldSetMapper<Customer> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Customer.class);
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
    }

    @Bean
    public CustomerProcessor customerProcessor() {
        return new CustomerProcessor();
    }

    //This was earlier method used for saving records, as it won't work for partition, as we save multiple records at same time
//    @Bean
//    public RepositoryItemWriter<Customer> customerWriter() {
//        RepositoryItemWriter<Customer> writer = new RepositoryItemWriter<>();
//        writer.setRepository(customerRepository);
//        writer.setMethodName("save");
//        return writer;
//    }

    @Bean
    public BatchPartitioning partitioner() {
        return new BatchPartitioning();
    }

    @Bean
    public PartitionHandler partitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setGridSize(4);
        partitionHandler.setTaskExecutor(taskExecutor());
        partitionHandler.setStep(slaveStep());
        return partitionHandler;
    }

    @Bean
    public Step slaveStep() {
        return new StepBuilder("slaveStep", jobRepository)
                .<Customer, Customer>chunk(250, transactionManager)
                .reader(customerReader())
                .processor(customerProcessor())
//                .writer(customerWriter())
                .writer(customerWriter)
//                .taskExecutor(taskExecutor()) //this does the job async, faster but sequence is not considered, for sequence we use Batch partitioning
                .build();
    }

    @Bean
    public Step masterStep() {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner(slaveStep().getName(), partitioner())
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    public Job runJob() {
        return new JobBuilder("importCustomers", jobRepository)
                .flow(masterStep())
                .end().build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
//        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
//        asyncTaskExecutor.setConcurrencyLimit(10);
        //above code was for async writing of records, below code is using partitions
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(4);
        executor.setCorePoolSize(4);
        executor.setQueueCapacity(4);
        return executor;
    }
}
