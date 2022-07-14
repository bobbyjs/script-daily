package org.dreamcat.daily.script.poetry;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2022-05-19
 */
@Getter
@Setter
public class Ci extends Poetry {

    @JsonProperty("rhythmic")
    private String title;
}
