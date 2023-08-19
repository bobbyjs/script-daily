package org.dreamcat.daily.script.poetry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2022-05-19
 */
@Getter
@Setter
public class Shi extends Poetry {

    private String title;
    private String biography;
    private List<String> notes;
    private String volume;
    @JsonProperty("no#")
    private String no;
}
