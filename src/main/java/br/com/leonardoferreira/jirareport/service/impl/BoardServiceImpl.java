package br.com.leonardoferreira.jirareport.service.impl;

import br.com.leonardoferreira.jirareport.aspect.annotation.ExecutionTime;
import br.com.leonardoferreira.jirareport.domain.Board;
import br.com.leonardoferreira.jirareport.domain.request.CreateBoardRequest;
import br.com.leonardoferreira.jirareport.domain.request.UpdateBoardRequest;
import br.com.leonardoferreira.jirareport.domain.response.BoardDetailsResponse;
import br.com.leonardoferreira.jirareport.domain.response.BoardResponse;
import br.com.leonardoferreira.jirareport.domain.vo.BoardStatus;
import br.com.leonardoferreira.jirareport.domain.vo.BoardStatusList;
import br.com.leonardoferreira.jirareport.exception.ResourceNotFound;
import br.com.leonardoferreira.jirareport.mapper.BoardMapper;
import br.com.leonardoferreira.jirareport.repository.BoardRepository;
import br.com.leonardoferreira.jirareport.service.BoardService;
import br.com.leonardoferreira.jirareport.service.ProjectService;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class BoardServiceImpl extends AbstractService implements BoardService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardMapper boardMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<BoardResponse> findAll(final Pageable pageable, final Board board) {
        log.info("Method=findAll");

        if (StringUtils.isEmpty(board.getOwner())) {
            board.setOwner(currentUser().getUsername());
        }

        if ("all".equals(board.getOwner())) {
            board.setOwner(null);
        }

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("owner", ExampleMatcher.GenericPropertyMatchers.exact())
                .withIgnoreNullValues()
                .withIgnoreCase();

        Example<Board> example = Example.of(board, matcher);

        Page<Board> boards = boardRepository.findAll(example, pageable);
        return boardMapper.toBoardResponse(boards);
    }

    @Override
    @Transactional
    public Long create(final CreateBoardRequest createBoardRequest) {
        log.info("Method=create, createBoardRequest={}", createBoardRequest);

        Board board = boardMapper.boardFromCreateBoardRequest(createBoardRequest);
        boardRepository.save(board);

        return board.getId();
    }

    @Override
    @Transactional
    public void delete(final Long id) {
        log.info("Method=delete, id={}", id);

        Board board = findById(id);
        boardRepository.deleteByIdAndOwner(board.getId(), currentUser().getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public Board findById(final Long id) {
        log.info("Method=findById, id={}", id);

        return boardRepository.findById(id)
                .orElseThrow(ResourceNotFound::new);
    }

    @Override
    @Transactional
    public void update(final Long boardId, final UpdateBoardRequest updateBoardRequest) {
        log.info("Method=board, updateBoardRequest={}", updateBoardRequest);

        Board board = findById(boardId);
        boardMapper.fromUpdateBoardRequest(board, updateBoardRequest);

        boardRepository.save(board);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findStatusFromBoardInJira(final Long boardId) {
        Board board = findById(boardId);
        return findStatusFromBoardInJira(board);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findStatusFromBoardInJira(final Board board) {
        log.info("Method=findStatusFromBoardInJira, board={}", board);

        List<BoardStatusList> listStatusesBoard = projectService.findStatusFromProject(board.getExternalId());

        return listStatusesBoard.stream()
                .map(BoardStatusList::getStatuses)
                .flatMap(Collection::stream)
                .map(BoardStatus::getName)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public BoardDetailsResponse findDetailsById(final Long id) {
        log.info("Method=findDetailsById, id={}", id);
        return boardMapper.toBoardResponseDetails(findById(id));
    }

    @Override
    @ExecutionTime
    @Transactional(readOnly = true)
    public List<String> findAllOwners() {
        log.info("Method=findAllOwners");
        return boardRepository.findAllOwners();
    }
}
