package ProjetoIntegrador.comum;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
@Configuration

public class JacksonConfig {
    @Bean Jackson2ObjectMapperBuilderCustomizer jsonEstrito() {
        return builder -> builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .featuresToDisable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .postConfigurer(mapper -> {
                for(CoercionInputShape tipo:new CoercionInputShape[]{CoercionInputShape.Integer,CoercionInputShape.Float,CoercionInputShape.Boolean})
                    mapper.coercionConfigFor(LogicalType.Textual).setCoercion(tipo,CoercionAction.Fail);
            });
    }
}
