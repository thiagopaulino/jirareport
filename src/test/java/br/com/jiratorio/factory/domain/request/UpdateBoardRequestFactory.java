package br.com.jiratorio.factory.domain.request;

import br.com.jiratorio.domain.DueDateType;
import br.com.jiratorio.domain.vo.DynamicFieldConfig;
import br.com.leonardoferreira.jbacon.JBacon;
import br.com.jiratorio.domain.ImpedimentType;
import br.com.jiratorio.domain.request.UpdateBoardRequest;
import com.github.javafaker.Faker;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateBoardRequestFactory extends JBacon<UpdateBoardRequest> {

    @Autowired
    private Faker faker;

    @Override
    protected UpdateBoardRequest getDefault() {
        UpdateBoardRequest updateBoardRequest = new UpdateBoardRequest();

        updateBoardRequest.setStartColumn(faker.lorem().word());
        updateBoardRequest.setEndColumn(faker.lorem().word());
        updateBoardRequest.setFluxColumn(faker.lorem().words());
        updateBoardRequest.setName(faker.lorem().word());
        updateBoardRequest.setIgnoreIssueType(faker.lorem().words());
        updateBoardRequest.setEpicCF(faker.expression("custom_field_#{number.number_between '1000','9999'}"));
        updateBoardRequest.setEstimateCF(faker.expression("custom_field_#{number.number_between '1000','9999'}"));
        updateBoardRequest.setSystemCF(faker.expression("custom_field_#{number.number_between '1000','9999'}"));
        updateBoardRequest.setProjectCF(faker.expression("custom_field_#{number.number_between '1000','9999'}"));
        updateBoardRequest.setIgnoreWeekend(faker.random().nextBoolean());
        updateBoardRequest.setImpedimentType(faker.options().option(ImpedimentType.class));
        updateBoardRequest.setImpedimentColumns(faker.lorem().words());
        updateBoardRequest.setDynamicFields(Arrays.asList(
                new DynamicFieldConfig("dnf_1", faker.expression("custom_field_#{number.number_between '1000','9999'}")),
                new DynamicFieldConfig("dnf_2", faker.expression("custom_field_#{number.number_between '1000','9999'}")),
                new DynamicFieldConfig("dnf_3", faker.expression("custom_field_#{number.number_between '1000','9999'}"))
        ));
        updateBoardRequest.setTouchingColumns(faker.lorem().words());
        updateBoardRequest.setWaitingColumns(faker.lorem().words());
        updateBoardRequest.setDueDateCF(faker.expression("custom_field_#{number.number_between '1000','9999'}"));
        updateBoardRequest.setDueDateType(faker.options().option(DueDateType.class));

        return updateBoardRequest;
    }

    @Override
    protected UpdateBoardRequest getEmpty() {
        return new UpdateBoardRequest();
    }

    @Override
    protected void persist(final UpdateBoardRequest updateBoardRequest) {
        throw new UnsupportedOperationException();
    }

}