package br.com.leonardoferreira.jirareport.service.impl;

import br.com.leonardoferreira.jirareport.domain.Board;
import br.com.leonardoferreira.jirareport.domain.LeadTimeConfig;
import br.com.leonardoferreira.jirareport.domain.request.LeadTimeConfigRequest;
import br.com.leonardoferreira.jirareport.domain.response.LeadTimeConfigResponse;
import br.com.leonardoferreira.jirareport.exception.ResourceNotFound;
import br.com.leonardoferreira.jirareport.mapper.LeadTimeConfigMapper;
import br.com.leonardoferreira.jirareport.repository.LeadTimeConfigRepository;
import br.com.leonardoferreira.jirareport.service.BoardService;
import br.com.leonardoferreira.jirareport.service.LeadTimeConfigService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class LeadTimeConfigServiceImpl extends AbstractService implements LeadTimeConfigService {

    @Autowired
    private LeadTimeConfigRepository leadTimeConfigRepository;

    @Autowired
    private BoardService boardService;

    @Autowired
    private LeadTimeConfigMapper leadTimeConfigMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LeadTimeConfig> findAllByBoardId(final Long boardId) {
        log.info("Method=findAllByBoardId, boardId={}", boardId);
        return leadTimeConfigRepository.findByBoardId(boardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadTimeConfigResponse> findAll(final Long boardId) {
        log.info("Method=findAllByBoardId, boardId={}", boardId);
        List<LeadTimeConfig> leadTimeConfigs = leadTimeConfigRepository.findByBoardId(boardId);

        return leadTimeConfigMapper.toResponse(leadTimeConfigs);
    }

    @Override
    @Transactional
    public Long create(final Long boardId, final LeadTimeConfigRequest leadTimeConfigRequest) {
        log.info("Method=create, boardId={}, leadTimeConfigRequest={}", boardId, leadTimeConfigRequest);

        Board board = boardService.findById(boardId);
        LeadTimeConfig leadTimeConfig = leadTimeConfigMapper.toLeadTimeConfig(leadTimeConfigRequest, board.getId());

        leadTimeConfigRepository.save(leadTimeConfig);

        return leadTimeConfig.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public LeadTimeConfigResponse findByBoardAndId(final Long boardId, final Long id) {
        log.info("Method=findByBoardAndId, boardId={}, id={}", boardId, id);

        LeadTimeConfig leadTimeConfig = leadTimeConfigRepository.findByIdAndBoardId(id, boardId)
                .orElseThrow(ResourceNotFound::new);
        return leadTimeConfigMapper.toResponse(leadTimeConfig);
    }

    @Override
    @Transactional
    public void update(final Long boardId, final Long id, final LeadTimeConfigRequest leadTimeConfigRequest) {
        log.info("Method=update, boardId={}, id={}, leadTimeConfigRequest={}", boardId, id, leadTimeConfigRequest);

        LeadTimeConfig leadTimeConfig = leadTimeConfigRepository.findByIdAndBoardId(id, boardId)
                .orElseThrow(ResourceNotFound::new);

        leadTimeConfigMapper.updateFromRequest(leadTimeConfig, leadTimeConfigRequest);
        leadTimeConfigRepository.save(leadTimeConfig);
    }

    @Override
    @Transactional
    public void deleteByBoardAndId(final Long boardId, final Long id) {
        log.info("Method=deleteByBoardAndId, boardId={}, id={}", boardId, id);

        LeadTimeConfig leadTimeConfig = leadTimeConfigRepository.findByIdAndBoardId(id, boardId)
                .orElseThrow(ResourceNotFound::new);
        leadTimeConfigRepository.delete(leadTimeConfig);
    }
}
