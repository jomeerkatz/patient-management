import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class PatientIntegrationTest {
    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004";
    }
    @Test
    public  void shouldReturnPatientsWithValidToken() {
        String loginPayload = """
                    {
                        "email": "testuser@test.com",
                        "password": "password123"
                    }
                """;

        String token = given() // define everything we need for the request: set up
                .contentType("application/json")
                .body(loginPayload)
                .when() // execute this request
                .post("/auth/login")
                .then() // after response, we check the status code, and check if the status code is 200
                .statusCode(200)
                .extract() // extract out
                .jsonPath()
                .get("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(200)
                .body("patients", notNullValue()); // we assert, that we have a response and in the body is a field with patients data
    }
}