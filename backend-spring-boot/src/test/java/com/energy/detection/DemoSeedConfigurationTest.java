package com.energy.detection;

import com.energy.detection.config.DataSeeder;
import com.energy.detection.repository.TransformerRepository;
import com.energy.detection.dto.PasswordChangeRequest;
import com.energy.detection.dto.LoginRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoSeedConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withBean(TransformerRepository.class, () -> mock(TransformerRepository.class))
        .withUserConfiguration(DataSeeder.class);

    @Test void seedDisabledByDefaultAndWhenFalse() {
        runner.run(context -> assertEquals(0, context.getBeansOfType(DataSeeder.class).size()));
        runner.withPropertyValues("app.demo.seed-enabled=false")
            .run(context -> assertEquals(0, context.getBeansOfType(DataSeeder.class).size()));
    }

    @Test void syntheticSeedRequiresExplicitOptInAndDoesNotOverwriteExistingGrid() {
        runner.withPropertyValues("app.demo.seed-enabled=true").run(context -> {
            var repository = context.getBean(TransformerRepository.class);
            when(repository.count()).thenReturn(1L);
            context.getBean(DataSeeder.class).run();
            verify(repository).count();
            verifyNoMoreInteractions(repository);
        });
    }

    @Test void requestRepresentationsDoNotExposePasswords() {
        assertFalse(new PasswordChangeRequest("secret-current", "secret-new-value").toString().contains("secret"));
        assertFalse(new LoginRequestDTO("test", "secret-current").toString().contains("secret-current"));
    }
}
