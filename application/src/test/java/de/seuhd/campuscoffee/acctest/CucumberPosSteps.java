package de.seuhd.campuscoffee.acctest;

import de.seuhd.campuscoffee.api.dtos.PosDto;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.PosService;
import io.cucumber.java.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.cucumber.datatable.DataTable;
import java.util.List;
import java.util.Map;
import io.cucumber.datatable.DataTable;
import java.util.Map;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

import static de.seuhd.campuscoffee.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the POS Cucumber tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
public class CucumberPosSteps {
    static final PostgreSQLContainer<?> postgresContainer;

    static {
        // share the same testcontainers instance across all Cucumber tests
        postgresContainer = getPostgresContainer();
        postgresContainer.start();
        // testcontainers are automatically stopped when the JVM exits
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        configurePostgresContainers(registry, postgresContainer);
    }

    @Autowired
    protected PosService posService;

    @LocalServerPort
    private Integer port;

    @Before
    public void beforeEach() {
        posService.clear();
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @After
    public void afterEach() {
        posService.clear();
    }

    private List<PosDto> createdPosList;
    private PosDto updatedPos;

    /**
     * Register a Cucumber DataTable type for PosDto.
     * @param row the DataTable row to map to a PosDto object
     * @return the mapped PosDto object
     */
    @DataTableType
    @SuppressWarnings("unused")
    public PosDto toPosDto(Map<String,String> row) {
        return PosDto.builder()
                .name(row.get("name"))
                .description(row.get("description"))
                .type(PosType.valueOf(row.get("type")))
                .campus(CampusType.valueOf(row.get("campus")))
                .street(row.get("street"))
                .houseNumber(row.get("houseNumber"))
                .postalCode(Integer.parseInt(row.get("postalCode")))
                .city(row.get("city"))
                .build();
    }

    // Given -----------------------------------------------------------------------

    @Given("the POS list contains the following elements")
    public void thePosListContainsTheFollowingElements(List<PosDto> posList) {
        createdPosList = createPos(posList);
        assertThat(createdPosList).size().isEqualTo(posList.size());
    }


    // TODO: Add Given step for new scenario

    // When -----------------------------------------------------------------------
    @When("I update the POS named {string} with the following values")
    public void iUpdateThePosNamedWithTheFollowingValues(String name, DataTable dataTable) {
        Map<String, String> updates = dataTable.asMaps().get(0);

        List<PosDto> currentPosList = retrievePos();

        PosDto posToUpdate = currentPosList.stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("POS not found in DB: " + name));

        Map<String, Object> updatePayload = Map.of(
                "id", posToUpdate.id(),
                "name", posToUpdate.name(),
                "description", updates.getOrDefault("description", posToUpdate.description()),
                "type", updates.containsKey("type") ? updates.get("type") : posToUpdate.type().name(),
                "campus", updates.containsKey("campus") ? updates.get("campus") : posToUpdate.campus().name(),
                "street", posToUpdate.street(),
                "houseNumber", posToUpdate.houseNumber(),
                "postalCode", posToUpdate.postalCode(),
                "city", posToUpdate.city()
        );

        updatedPos = given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .log().all()
                .when()
                .put("/api/pos/{id}", posToUpdate.id())
                .then()
                .log().all()
                .statusCode(200)
                .extract().as(PosDto.class);

        createdPosList = retrievePos();
    }


    // TODO: Add When step for new scenario

    // Then -----------------------------------------------------------------------

    @Then("the POS list should contain the updated POS")
    public void thePosListShouldContainTheUpdatedPos(List<PosDto> expectedPosList) {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrderElementsOf(expectedPosList);
    }

    // TODO: Add Then step for new scenario
}
