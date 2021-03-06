package br.com.leonardoferreira.jirareport.domain.vo.changelog;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * @author s2it_leferreira
 * @since 11/06/18 13:42
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraChangelogHistory {

    private String id;

    private JiraChangelogAuthor author;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private LocalDateTime created;

    private List<JiraChangelogItem> items;

}
