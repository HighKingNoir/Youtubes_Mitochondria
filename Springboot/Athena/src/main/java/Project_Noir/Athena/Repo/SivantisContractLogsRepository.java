package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.ContractFunctionEnum;
import Project_Noir.Athena.Model.ContractStatusEnum;
import Project_Noir.Athena.Model.SivantisContractLogs;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SivantisContractLogsRepository extends MongoRepository<SivantisContractLogs, String> {
    List<SivantisContractLogs> findByCreationDateBefore(Instant creationDate, PageRequest of);

    @Query("{ 'contractTransactionReceipt.contractStatusEnum' : 'Error' }")
    List<SivantisContractLogs> findByContractTransactionReceiptStatus(Pageable pageable);

    @Query("{ 'creationDate' : { $lt : ?0 }, 'contractTransactionReceipt.contractStatusEnum' : 'Error' }")
    List<SivantisContractLogs> findByCreationDateBeforeAndContractTransactionReceiptStatus(Instant creationDate, Pageable pageable);

    @Query("{'contractFunctionEnum' : { $in : ?0 }, 'contractTransactionReceipt.contractStatusEnum' : 'Error'}")
    List<SivantisContractLogs> findByContractFunctionEnumInAndContractStatusEnumError(List<ContractFunctionEnum> functionEnums, PageRequest of);

    @Query("{'contractFunctionEnum' : { $in : ?0 }, 'contractTransactionReceipt.contractStatusEnum' : 'Error', 'creationDate' : { $lt : ?1 }}")
    List<SivantisContractLogs> findByContractFunctionEnumInAndContractStatusEnumErrorAndCreationDateBefore(
            List<ContractFunctionEnum> functionEnums, Instant creationDate, PageRequest of);

    List<SivantisContractLogs> findByManaToCompanyGreaterThanAndCreationDateBetween(
            Double minManaToCompany,
            Instant startDate,
            Instant endDate
    );

    List<SivantisContractLogs> findByCreationDateBetween(
            Instant startDate,
            Instant endDate
    );

    Long countByContractTransactionReceipt_ContractStatusEnum(ContractStatusEnum status);
}
