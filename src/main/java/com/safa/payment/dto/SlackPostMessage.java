package com.safa.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlackPostMessage {

    @JsonProperty("username")
    private String username;

    @JsonProperty("icon_emoji")
    private String icon;

    @JsonProperty("channel")
    private String channelId;

    @JsonProperty("text")
    private String messageContent;

}
