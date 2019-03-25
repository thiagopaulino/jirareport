package br.com.jiratorio.integration.issueperiod

import br.com.jiratorio.assert.IssueAssert
import br.com.jiratorio.assert.IssuePeriodAssert
import br.com.jiratorio.base.Authenticator
import br.com.jiratorio.base.annotation.LoadStubs
import br.com.jiratorio.domain.entity.embedded.Changelog
import br.com.jiratorio.domain.entity.embedded.ColumnTimeAvg
import br.com.jiratorio.dsl.restAssured
import br.com.jiratorio.exception.ResourceNotFound
import br.com.jiratorio.extension.toLocalDate
import br.com.jiratorio.extension.toLocalDateTime
import br.com.jiratorio.factory.entity.BoardFactory
import br.com.jiratorio.repository.IssuePeriodRepository
import br.com.jiratorio.repository.IssueRepository
import io.restassured.http.ContentType
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.servlet.http.HttpServletResponse

@Tag("integration")
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CreateBasicIssuePeriodIntegrationTest @Autowired constructor(
    private val boardFactory: BoardFactory,
    private val authenticator: Authenticator,
    private val issuePeriodRepository: IssuePeriodRepository,
    private val issueRepository: IssueRepository
) {

    @Test
    @LoadStubs(["issues/basic-issues"])
    fun `create basic issue period`() {
        val board = authenticator.withDefaultUser {
            boardFactory.create("withBasicConfiguration")
        }

        val request = object {
            val startDate = "01/01/2019"
            val endDate = "31/01/2019"
        }

        restAssured {
            given {
                header(authenticator.defaultUserHeader())
                contentType(ContentType.JSON)
                body(request)
            }
            on {
                post("/boards/{id}/issue-periods", board.id)
            }
            then {
                statusCode(HttpServletResponse.SC_CREATED)
                header("location", containsString("/boards/1/issue-periods/1"))
            }
        }

        val issuePeriod = issuePeriodRepository.findById(1L)
            .orElseThrow(::ResourceNotFound)

        IssuePeriodAssert(issuePeriod).assertThat {
            hasStartDate(request.startDate.toLocalDate())
            hasEndDate(request.endDate.toLocalDate())

            hasAvgLeadTime(14.5)

            histogram().assertThat {
                hasMedian(11)
                hasPercentile75(18)
                hasPercentile90(18)
                hasChart(
                    1L to 0L, 2L to 0L, 3L to 0L, 4L to 0L, 5L to 0L, 6L to 0L, 7L to 0L, 8L to 0L, 9L to 0L, 10L to 0L,
                    11L to 1L, 12L to 0L, 13L to 0L, 14L to 0L, 15L to 0L, 16L to 0L, 17L to 0L, 18L to 1L
                )
            }

            hasLeadTimeBySize("Não informado" to 14.5)
            hasEstimated("Não informado" to 2)

            hasLeadTimeBySystem("Não informado" to 14.5)
            hasTasksBySystem("Não informado" to 2L)

            hasLeadTimeByType("Task" to 14.5)
            hasTasksByType("Task" to 2L)

            hasLeadTimeByProject("Não informado" to 14.5)
            hasTasksByProject("Não informado" to 2L)

            hasLeadTimeByPriority("Major" to 14.5)
            hasThroughputByPriority("Major" to 2L)

            hasIssuesCount(2)

            hasWipAvg(1.26)

            hasAvgPctEfficiency(0.0)

            hasEmptyDynamicCharts()

            containsColumnTimeAvgs(
                ColumnTimeAvg(columnName = "BACKLOG", avgTime = 4.0),
                ColumnTimeAvg(columnName = "TODO", avgTime = 2.0),
                ColumnTimeAvg(columnName = "WIP", avgTime = 10.0),
                ColumnTimeAvg(columnName = "ACCOMPANIMENT", avgTime = 4.0),
                ColumnTimeAvg(columnName = "DONE", avgTime = 0.0)
            )

            hasEmptyLeadTimeCompareChart()
        }

        val issue = issueRepository.findById(1L)
            .orElseThrow(::ResourceNotFound)

        IssueAssert(issue).assertThat {
            hasKey("JIRAT-1")
            hasIssueType("Task")
            hasCreator("Leonardo Ferreira")
            hasSystem(null)
            hasEpic(null)
            hasSummary("Calcular diferença de data de entrega com o primeiro due date")
            hasEstimated(null)
            hasProject(null)
            hasStartDate("01/01/2019 10:15".toLocalDateTime())
            hasEndDate("15/01/2019 11:20".toLocalDateTime())
            hasLeadTime(11)
            hasCreated("25/12/2018 11:49:35".toLocalDateTime())
            hasPriority("Major")

            hasChangelog(
                Changelog(
                    from = null,
                    to = "BACKLOG",
                    created = "25/12/2018 11:49:35".toLocalDateTime(),
                    leadTime = 6,
                    endDate = "01/01/2019 10:15".toLocalDateTime()
                ),
                Changelog(
                    from = "BACKLOG",
                    to = "TODO",
                    created = "01/01/2019 10:15".toLocalDateTime(),
                    leadTime = 1,
                    endDate = "01/01/2019 16:30".toLocalDateTime()
                ),
                Changelog(
                    from = "TODO",
                    to = "WIP",
                    created = "01/01/2019 16:30".toLocalDateTime(),
                    leadTime = 8,
                    endDate = "10/01/2019 13:45".toLocalDateTime()
                ),
                Changelog(
                    from = "WIP",
                    to = "ACCOMPANIMENT",
                    created = "10/01/2019 13:45".toLocalDateTime(),
                    leadTime = 4,
                    endDate = "15/01/2019 11:20".toLocalDateTime()
                ),
                Changelog(
                    from = "ACCOMPANIMENT",
                    to = "DONE",
                    created = "15/01/2019 11:20".toLocalDateTime(),
                    leadTime = 0,
                    endDate = "15/01/2019 11:20".toLocalDateTime()
                )
            )

            hasDeviationOfEstimate(null)
            hasDueDateHistory(null)

            hasImpedimentTime(0)

            hasDynamicFields(null)

            hasWaitTime(0)
            hasTouchTime(0)
            hasPctEfficiency(0.0)
        }
    }
}
