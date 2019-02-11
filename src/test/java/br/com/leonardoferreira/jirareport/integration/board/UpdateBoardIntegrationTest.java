package br.com.leonardoferreira.jirareport.integration.board;

import br.com.leonardoferreira.jirareport.base.BaseIntegrationTest;
import br.com.leonardoferreira.jirareport.domain.Board;
import br.com.leonardoferreira.jirareport.domain.request.UpdateBoardRequest;
import br.com.leonardoferreira.jirareport.exception.ResourceNotFound;
import br.com.leonardoferreira.jirareport.factory.BoardFactory;
import br.com.leonardoferreira.jirareport.factory.UpdateBoardRequestFactory;
import br.com.leonardoferreira.jirareport.repository.BoardRepository;
import br.com.leonardoferreira.jirareport.util.DateUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UpdateBoardIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BoardFactory boardFactory;

    @Autowired
    private UpdateBoardRequestFactory updateBoardRequestFactory;

    @Autowired
    private BoardRepository boardRepository;

    @Test
    public void updateBoard() throws Exception {
        withDefaultUser(() -> boardFactory.create());
        UpdateBoardRequest request = updateBoardRequestFactory.build();

        // @formatter:off
        RestAssured
                .given()
                    .log().all()
                    .header(defaultUserHeader())
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .put("/boards/1")
                .then()
                    .log().all()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        // @formatter:on

        Board board = boardRepository.findById(1L)
                .orElseThrow(ResourceNotFound::new);
        Assertions.assertAll("boardContent",
                () -> Assertions.assertEquals(request.getName(), board.getName()),
                () -> Assertions.assertEquals(request.getStartColumn().toUpperCase(DateUtil.LOCALE_BR), board.getStartColumn()),
                () -> Assertions.assertEquals(request.getEndColumn().toUpperCase(DateUtil.LOCALE_BR), board.getEndColumn()),
                () -> Assertions.assertEquals(request.getFluxColumn().size(), board.getFluxColumn().size()),
                () -> Assertions.assertIterableEquals(request.getFluxColumn().stream().map(String::toUpperCase).collect(Collectors.toList()),
                        board.getFluxColumn()),
                () -> Assertions.assertIterableEquals(request.getTouchingColumns().stream().map(String::toUpperCase).collect(Collectors.toList()),
                        board.getTouchingColumns()),
                () -> Assertions.assertIterableEquals(request.getWaitingColumns().stream().map(String::toUpperCase).collect(Collectors.toList()),
                        board.getWaitingColumns()),
                () -> Assertions.assertEquals(request.getIgnoreIssueType().size(), board.getIgnoreIssueType().size()),
                () -> Assertions.assertEquals(request.getEpicCF(), board.getEpicCF()),
                () -> Assertions.assertEquals(request.getEstimateCF(), board.getEstimateCF()),
                () -> Assertions.assertEquals(request.getSystemCF(), board.getSystemCF()),
                () -> Assertions.assertEquals(request.getProjectCF(), board.getProjectCF()),
                () -> Assertions.assertEquals(request.getCalcDueDate(), board.getCalcDueDate()),
                () -> Assertions.assertEquals(request.getIgnoreWeekend(), board.getIgnoreWeekend()),
                () -> Assertions.assertEquals(request.getImpedimentColumns().size(), board.getImpedimentColumns().size()),
                () -> Assertions.assertEquals(request.getDynamicFields().size(), board.getDynamicFields().size()));
    }

    @Test
    public void updateNotFoundBoard() {
        // @formatter:off
        RestAssured
                .given()
                    .log().all()
                    .header(defaultUserHeader())
                    .contentType(ContentType.JSON)
                    .body(new UpdateBoardRequest())
                .when()
                    .put("/boards/999")
                .then()
                    .log().all()
                    .spec(notFoundSpec());
        // @formatter:on
    }
}
