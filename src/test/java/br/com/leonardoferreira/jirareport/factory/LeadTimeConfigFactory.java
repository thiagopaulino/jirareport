package br.com.leonardoferreira.jirareport.factory;

import br.com.leonardoferreira.jbacon.JBacon;
import br.com.leonardoferreira.jirareport.domain.LeadTimeConfig;
import br.com.leonardoferreira.jirareport.repository.LeadTimeConfigRepository;
import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LeadTimeConfigFactory extends JBacon<LeadTimeConfig> {

    @Autowired
    private Faker faker;

    @Autowired
    private LeadTimeConfigRepository leadTimeConfigRepository;

    @Autowired
    private BoardFactory boardFactory;

    @Override
    protected LeadTimeConfig getDefault() {
        LeadTimeConfig leadTimeConfig = new LeadTimeConfig();

        leadTimeConfig.setBoard(boardFactory.create());
        leadTimeConfig.setName(faker.lorem().word());
        leadTimeConfig.setStartColumn(faker.lorem().word());
        leadTimeConfig.setEndColumn(faker.lorem().word());

        return leadTimeConfig;
    }

    @Override
    protected LeadTimeConfig getEmpty() {
        return new LeadTimeConfig();
    }

    @Override
    protected void persist(final LeadTimeConfig leadTimeConfig) {
        leadTimeConfigRepository.save(leadTimeConfig);
    }

}
