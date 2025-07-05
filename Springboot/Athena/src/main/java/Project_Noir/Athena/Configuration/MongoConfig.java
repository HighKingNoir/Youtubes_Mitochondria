package Project_Noir.Athena.Configuration;

import org.bson.types.Decimal128;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                // Convert BigDecimal → Decimal128
                new org.springframework.core.convert.converter.Converter<BigDecimal, Decimal128>() {
                    public Decimal128 convert(@NotNull BigDecimal source) {
                        return new Decimal128(source);
                    }
                },
                // Convert Decimal128 → BigDecimal
                new org.springframework.core.convert.converter.Converter<Decimal128, BigDecimal>() {
                    public BigDecimal convert(@NotNull Decimal128 source) {
                        return source.bigDecimalValue();
                    }
                }
        ));
    }
}