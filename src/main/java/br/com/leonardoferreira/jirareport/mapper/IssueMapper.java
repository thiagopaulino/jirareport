package br.com.leonardoferreira.jirareport.mapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import br.com.leonardoferreira.jirareport.domain.embedded.Changelog;
import br.com.leonardoferreira.jirareport.domain.Holiday;
import br.com.leonardoferreira.jirareport.domain.Issue;
import br.com.leonardoferreira.jirareport.domain.Project;
import br.com.leonardoferreira.jirareport.service.HolidayService;
import br.com.leonardoferreira.jirareport.util.DateUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static br.com.leonardoferreira.jirareport.util.DateUtil.DEFAULT_FORMATTER;

/**
 * @author lferreira
 * @since 7/28/17 12:52 PM
 */
@Component
public class IssueMapper {

    private JsonParser jsonParser = new JsonParser();

    private final HolidayService holidayService;

    public IssueMapper(final HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    public List<Issue> parse(String rawText, Project project) {
        JsonElement response = jsonParser.parse(rawText);
        JsonArray issues = response.getAsJsonObject()
                .getAsJsonArray("issues");

        final List<String> holidays = holidayService.findAll()
                .stream().map(Holiday::getEnDate).collect(Collectors.toList());

        return StreamSupport.stream(issues.spliterator(), true)
                .map(issueRaw -> {
                    JsonObject issue = issueRaw.getAsJsonObject();

                    JsonObject fields = issue.get("fields").getAsJsonObject();
                    List<Changelog> changelog = getChangelog(issue, holidays);

                    String startDate = null;
                    String endDate = null;

                    for (Changelog cl : changelog) {
                        if (cl.getTo().equalsIgnoreCase(project.getStartColumn())) {
                            startDate = DateUtil.toENDate(cl.getCreated());
                        }

                        if (cl.getTo().equalsIgnoreCase(project.getEndColumn())) {
                            endDate = DateUtil.toENDate(cl.getCreated());
                        }
                    }

                    if (startDate == null || endDate == null) {
                        return null;
                    }

                    String epicField = project.getEpicCF();
                    String estimateField = project.getEstimateCF();

                    String epic = epicField.equals("") ? null : getAsStringSafe(fields.get(epicField));
                    String estimated = estimateField.equals("") ? null : (getAsStringSafe(fields.get(estimateField).isJsonNull() ?
                            null : fields.get(estimateField).getAsJsonObject().get("value")));
                    Long leadTime = daysDiff(startDate, endDate, holidays);

                    Issue issueVO = new Issue();
                    issueVO.setKey(issue.get("key").getAsString());

                    JsonObject creator = issue.getAsJsonObject("creator");
                    if (creator != null) {
                        issueVO.setCreator(creator.get("displayName").getAsString());
                    }

                    issueVO.setCreated(fields.get("created").getAsString());
                    issueVO.setUpdated(fields.get("updated").getAsString());
                    issueVO.setStartDate(DateUtil.displayFormat(startDate));
                    issueVO.setEndDate(DateUtil.displayFormat(endDate));
                    issueVO.setLeadTime(leadTime);
                    issueVO.setComponents(getComponents(fields, project));
                    issueVO.setEpic(epic);
                    issueVO.setEstimated(estimated);
                    issueVO.setSummary(fields.get("summary").getAsString());
                    issueVO.setChangelog(changelog);

                    return issueVO;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Changelog> getChangelog(JsonObject issue, List<String> holidays) {
        JsonArray histories = issue.getAsJsonObject("changelog").getAsJsonArray("histories");

        List<Changelog> collect = StreamSupport.stream(histories.spliterator(), true)
                .map(historyRaw -> {
                    JsonObject history = historyRaw.getAsJsonObject();
                    JsonObject item = getItem(history);
                    if (item != null) {
                        Changelog changelog = new Changelog();
                        changelog.setCreated(getDateAsString(history.get("created")));
                        changelog.setFrom(getAsStringSafe(item.get("from")));
                        changelog.setTo(getAsStringSafe(item.get("to")));

                        return changelog;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (int i = 0; i < collect.size(); i++) {
            Changelog current = collect.get(i);
            if (i + 1 == collect.size()) {
                current.setCreated(DateUtil.displayFormat(current.getCreated()));
                break;
            }

            Changelog next = collect.get(i + 1);
            current.setCycleTime(daysDiff(current.getCreated(), next.getCreated(), holidays));
            current.setCreated(DateUtil.displayFormat(current.getCreated()));
        }

        return collect;
    }

    private JsonObject getItem(JsonObject history) {
        JsonArray items = history.getAsJsonArray("items");

        return StreamSupport.stream(items.spliterator(), true)
                .map(itemRaw -> {
                    JsonObject item = itemRaw.getAsJsonObject();
                    if (!"status".equalsIgnoreCase(item.get("field").getAsString())) {
                        return null;
                    }

                    String fromString = getAsStringSafe(item.get("fromString"));
                    String toString = getAsStringSafe(item.get("toString"));

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("from", fromString);
                    jsonObject.addProperty("to", toString);
                    return jsonObject;
                }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private List<String> getComponents(final JsonObject fields, final Project project) {
        final JsonElement jsonElement = fields.get(project.getSystemCF());
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return new ArrayList<>();
        }

        if (jsonElement.isJsonArray()) {
            JsonArray components = jsonElement.getAsJsonArray();
            if (components == null) {
                return new ArrayList<>();
            }

            return StreamSupport.stream(components.spliterator(), true)
                    .map(component -> component.isJsonObject() ? component.getAsJsonObject().get("name").getAsString() : component.getAsString())
                    .collect(Collectors.toList());
        }

        if (jsonElement.isJsonObject()) {
            return Collections.singletonList(getAsStringSafe(jsonElement.getAsJsonObject().get("value")));
        }

        return Collections.singletonList(jsonElement.getAsString());
    }

    private String getAsStringSafe(JsonElement jsonElement) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }
        return jsonElement.getAsString();
    }

    private String getDateAsString(JsonElement jsonElement) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }
        return jsonElement.getAsString().substring(0, 10);
    }

    @SneakyThrows
    private Long daysDiff(String startDate, String endDate, List<String> holidays) {
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            return null;
        }

        Calendar start = Calendar.getInstance();
        start.setTime(new SimpleDateFormat(DEFAULT_FORMATTER).parse(startDate));
        Calendar end = Calendar.getInstance();
        end.setTime(new SimpleDateFormat(DEFAULT_FORMATTER).parse(endDate));
        Long workingDays = 0L;
        while (!start.after(end)) {
            int day = start.get(Calendar.DAY_OF_WEEK);
            if ((day != Calendar.SATURDAY) && (day != Calendar.SUNDAY) && !isHoliday(start, holidays)) {
                workingDays++;
            }
            start.add(Calendar.DATE, 1);
        }
        return workingDays;
    }

    private boolean isHoliday(Calendar day, List<String> holidays) {
        String aux = new SimpleDateFormat(DEFAULT_FORMATTER).format(day.getTime());
        return holidays.contains(aux);
    }


}
