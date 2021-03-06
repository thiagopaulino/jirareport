package br.com.leonardoferreira.jirareport.service.impl;

import br.com.leonardoferreira.jirareport.domain.Board;
import br.com.leonardoferreira.jirareport.domain.Issue;
import br.com.leonardoferreira.jirareport.domain.IssuePeriod;
import br.com.leonardoferreira.jirareport.domain.form.IssuePeriodForm;
import br.com.leonardoferreira.jirareport.domain.vo.ChartAggregator;
import br.com.leonardoferreira.jirareport.domain.vo.IssueCountBySize;
import br.com.leonardoferreira.jirareport.domain.vo.IssuePeriodChart;
import br.com.leonardoferreira.jirareport.domain.vo.IssuePeriodDetails;
import br.com.leonardoferreira.jirareport.domain.vo.IssuePeriodList;
import br.com.leonardoferreira.jirareport.domain.vo.LeadTimeCompareChart;
import br.com.leonardoferreira.jirareport.exception.ResourceNotFound;
import br.com.leonardoferreira.jirareport.mapper.IssuePeriodMapper;
import br.com.leonardoferreira.jirareport.repository.IssuePeriodRepository;
import br.com.leonardoferreira.jirareport.service.BoardService;
import br.com.leonardoferreira.jirareport.service.ChartService;
import br.com.leonardoferreira.jirareport.service.IssuePeriodService;
import br.com.leonardoferreira.jirareport.service.IssueService;
import br.com.leonardoferreira.jirareport.service.WipService;
import br.com.leonardoferreira.jirareport.util.CalcUtil;
import br.com.leonardoferreira.jirareport.util.DateUtil;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author lferreira
 * @since 7/28/17 12:50 PM
 */
@Slf4j
@Service
public class IssuePeriodServiceImpl extends AbstractService implements IssuePeriodService {

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssuePeriodRepository issuePeriodRepository;

    @Autowired
    private ChartService chartService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private IssuePeriodMapper issuePeriodMapper;

    @Autowired
    private WipService wipService;

    @Override
    @Transactional
    public void create(final IssuePeriodForm issuePeriodForm, final Long boardId) {
        log.info("Method=create, issuePeriodForm={}, boardId={}", issuePeriodForm, boardId);

        delete(issuePeriodForm, boardId);

        Board board = boardService.findById(boardId);

        String jql = issueService.searchJQL(issuePeriodForm, board);
        List<Issue> issues = issueService.createByJql(jql, board);

        Double avgLeadTime = issues.parallelStream()
                .filter(i -> Objects.nonNull(i.getLeadTime()))
                .mapToLong(Issue::getLeadTime)
                .average().orElse(0D);

        Double avgPctEfficiency = issues.parallelStream()
                .filter(i -> Objects.nonNull(i.getPctEfficiency()))
                .mapToDouble(Issue::getPctEfficiency)
                .average().orElse(0D);

        ChartAggregator chartAggregator = chartService.buildAllCharts(issues, board);

        Double wipAvg = wipService.calcAvgWip(issuePeriodForm.getStartDate(), issuePeriodForm.getEndDate(),
                issues, CalcUtil.calcStartColumns(board));

        IssuePeriodDetails details = IssuePeriodDetails.builder()
                .boardId(boardId)
                .jql(jql)
                .wipAvg(wipAvg)
                .avgLeadTime(avgLeadTime)
                .issueCount(issues.size())
                .avgPctEfficiency(avgPctEfficiency)
                .build();

        IssuePeriod issuePeriod = issuePeriodMapper.fromJiraData(issuePeriodForm, issues,
                chartAggregator, details);

        issuePeriodRepository.save(issuePeriod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuePeriod> findByBoardId(final Long boardId) {
        log.info("Method=findByBoardId, boardId={}", boardId);

        List<IssuePeriod> issuePeriods = issuePeriodRepository.findByBoardId(boardId);
        issuePeriods.sort(DateUtil::sort);

        return issuePeriods;
    }

    @Override
    @Transactional(readOnly = true)
    public IssuePeriodChart buildCharts(final List<IssuePeriod> issuePeriods, final Board board) {
        log.info("Method=buildCharts, issuePeriods={}", issuePeriods);

        IssuePeriodChart issuePeriodChart = new IssuePeriodChart();
        for (IssuePeriod issuePeriod : issuePeriods) {
            issuePeriodChart.addLeadTime(issuePeriod);
            issuePeriodChart.addIssuesCount(issuePeriod);
        }

        IssueCountBySize issueCountBySize = chartService.buildIssueCountBySize(issuePeriods);
        issuePeriodChart.setIssueCountBySize(issueCountBySize);

        LeadTimeCompareChart leadTimeCompareChart = chartService.calcLeadTimeCompareByPeriod(issuePeriods, board);
        issuePeriodChart.setLeadTimeCompareChart(leadTimeCompareChart);

        return issuePeriodChart;
    }

    @Override
    @Transactional(readOnly = true)
    public IssuePeriod findById(final Long id) {
        log.info("Method=findById, id={}", id);

        return issuePeriodRepository.findById(id)
                .orElseThrow(ResourceNotFound::new);
    }

    @Override
    @Transactional
    public void remove(final Long id) {
        log.info("Method=remove, id={}", id);

        IssuePeriod issuePeriod = findById(id);
        delete(issuePeriod);
    }

    @Override
    @Transactional
    public void update(final Long issuePeriodId) {
        log.info("Method=update, issuePeriodId={}", issuePeriodId);

        IssuePeriod issuePeriod = findById(issuePeriodId);
        IssuePeriodForm issuePeriodForm = new IssuePeriodForm(issuePeriod.getStartDate(), issuePeriod.getEndDate());

        create(issuePeriodForm, issuePeriod.getBoardId());
    }

    @Override
    @Transactional(readOnly = true)
    public IssuePeriodList findIssuePeriodsAndCharts(final Long boardId) {
        Board board = boardService.findById(boardId);
        List<IssuePeriod> issuePeriods = findByBoardId(boardId);
        IssuePeriodChart issuePeriodChart = buildCharts(issuePeriods, board);

        return IssuePeriodList.builder()
                .issuePeriods(issuePeriods)
                .issuePeriodChart(issuePeriodChart)
                .build();
    }

    private void delete(final IssuePeriodForm issuePeriodForm, final Long boardId) {
        log.info("Method=delete, issuePeriodForm={}, boardId={}", issuePeriodForm, boardId);

        IssuePeriod issuePeriod = issuePeriodRepository.findByStartDateAndEndDateAndBoardId(issuePeriodForm.getStartDate(),
                issuePeriodForm.getEndDate(), boardId);

        if (issuePeriod != null) {
            delete(issuePeriod);
        }
    }

    private void delete(final IssuePeriod issuePeriod) {
        issuePeriodRepository.delete(issuePeriod);
        issueService.deleteAll(issuePeriod.getIssues());

    }
}
