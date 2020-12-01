package com.example.pitTestcontainers.pets;

import javax.transaction.Transactional;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@SpringBootTest
@Testcontainers
@ExtendWith(SpringBootCleanup.class)
public class PetServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(PetServiceTest.class);

    private static final String LASSIE = "Lassie";
    private static final String GOLDEN = "Golden retriever";
    private static final LocalDate BIRTH_DATE = LocalDate.of(2020, 01, 01);

    @Container
    public final static PostgreSQLContainer postgresContainer;
    static {
        final Long pid = ProcessHandle.current().pid();
        MDC.put("pid", pid.toString());
        logger.error("--------- About to start new pg");

        postgresContainer = new PostgreSQLContainer("postgres"){
            @Override
            public void stop() {
                logger.error("--------- About to stop pg!");

                super.stop();
            }

            @Override
            public void start() {
                logger.error("--------- About to start pg!");

                super.start();
            }
        };
        logger.error("--------- New Container created: id={}", postgresContainer.getContainerId());
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        final Long pid = ProcessHandle.current().pid();
        MDC.put("pid", pid.toString());

        logger.error("--------- DYNAMIC PROPERTY REGISTRY url={} (running:{})",
                postgresContainer.getJdbcUrl(),
                postgresContainer.isRunning());

        logger.error("--------- Command: {}", System.getProperties().get("sun.java.command"));

        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    PetRepository petsRepo;

    PetService service;

    Pet pet1;

    @BeforeEach
    void setup() {
        service = new PetService(petsRepo);
        Pet pet = new Pet();
        pet.setName(LASSIE);
        pet.setType(GOLDEN);
        pet.setBirthDate(BIRTH_DATE);
        pet1 = petsRepo.save(pet);
    }

    @Test
    void shouldFindPetWithCorrectId() {
        logger.info("##################### Test1");


        Pet petFound = service.getPet(pet1.getId());
        assertEquals(LASSIE, petFound.getName());
        assertEquals(GOLDEN, petFound.getType());
        assertEquals(BIRTH_DATE, petFound.getBirthDate());
    }


    @Test
    void shouldInsertPetIntoDatabaseAndGenerateId() {
        logger.info("##################### Test2");

        LocalDate birthDate = LocalDate.now();
        Pet savedPet = service.savePet("Molly",birthDate,"Poodle" );

        assertTrue(savedPet.getId() != null);
        Pet petFound = service.getPet(savedPet.getId());
        assertEquals("Molly", petFound.getName());
        assertEquals("Poodle", petFound.getType());
        assertEquals(birthDate, petFound.getBirthDate());
    }


}
