import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthIntegrationTest {
    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004";
    }

    @Test
    public void shouldReturnOKWithValidToken() {
        String loginPayload = """
                    {
                        "email": "testuser@test.com",
                        "password": "password123"
                    }
                """;

        Response response = given() // setting up our test with content type + body
                .contentType("application/json")
                .body(loginPayload)
                .when() // now acting with a post request
                .post("/auth/login")
                .then() // check the status after response
                .statusCode(200)
                .body("token", notNullValue()) // take the body and check if it is not null
                .extract().response(); // then we extract the response itselfe and save it
        System.out.println("generated token: "+ response.jsonPath().getString("token")); // go into the body, which
        // is a json and take the field data with the name of token and print it
    }

    @Test
    public void shouldReturnUnauthorizedOnInvalidLogin() {
        String loginPayload = """
                    {
                        "email": "invalid@test.com",
                        "password": "wrongpassword"
                    }
                """;

        given() // setting up our test with content type + body
                .contentType("application/json")
                .body(loginPayload)
                .when() // now acting with a post request
                .post("/auth/login")
                .then() // check the status after response
                .statusCode(401);
    }
}
