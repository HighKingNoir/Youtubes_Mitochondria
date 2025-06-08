package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.Payment;
import Project_Noir.Athena.Model.PaymentEnum;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;


@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {


    @Query(value = "{ 'contentId': { $in: ?0 }, 'status': { $in: ?1 }, 'paymentDate': { $gte: ?2, $lte: ?3 } }",
            fields = "{ 'manaToCreator': 1 }")
    List<String> findManaToCreatorForContentAndStatusInDateRange(
            List<String> contentIds,
            List<PaymentEnum> statuses,
            Instant startDate,
            Instant endDate
    );

    List<Payment> findByPaymentIdIn(Collection<String> contentIds, PageRequest of);

    List<Payment> findByPaymentDateBeforeAndPaymentIdIn(Instant paymentDate, Collection<String> paymentIds, PageRequest of);
}
