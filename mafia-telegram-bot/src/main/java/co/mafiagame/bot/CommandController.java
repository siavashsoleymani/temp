/*
 *     Copyright (c) 2018 Isa Hekmatizadeh.
 *     This file is part of mafiagame.
 *
 *     Mafiagame is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Mafiagame is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Mafiagame.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.mafiagame.bot;

import co.mafiagame.bot.persistence.repository.AccountRepository;
import co.mafiagame.bot.telegram.TResult;
import co.mafiagame.bot.telegram.TUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * @author Esa Hekmatizadeh
 */
@RestController
public class CommandController {
    private static final Logger logger = LoggerFactory.getLogger(CommandController.class);
    @Value("${mafia.telegram.token}")
    private String telegramToken;
    @Value("${mafia.telegram.api.url}")
    private String telegramUrl;
    @Value("${mafia.telegram.use.webhook}")
    private Boolean webHookEnabled;
    private final CommandDispatcher commandDispatcher;
    private final RestTemplate restTemplate;
    private volatile long offset = 1;
    private final AccountRepository accountRepository;

    @Autowired
    public CommandController(CommandDispatcher commandDispatcher,
                             RestTemplate restTemplate, AccountRepository accountRepository) {
        this.commandDispatcher = commandDispatcher;
        this.restTemplate = restTemplate;
        this.accountRepository = accountRepository;
    }

    @Scheduled(fixedDelay = 200)
    public void init() {
        if (webHookEnabled)
            return;
        try {
            TResult tResult = restTemplate.getForObject(
                    telegramUrl + telegramToken + "/getUpdates?offset=" + (offset + 1),
                    TResult.class);
            if (Objects.isNull(tResult))
                return;
            tResult.getResult().forEach((update) -> {
                if (offset < update.getId()) {
                    offset = update.getId();
                    handleMessage(update);
                    logger.debug("offset set to {}", offset);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @PostMapping("/{token}/update")
    public void getUpdate(@PathVariable String token, @RequestBody TUpdate update) {
        logger.info("receive: {}", update);
        if (!validate(token)) {
            logger.warn("Suspicious connection with token {} and update {}", token, update);
            return;
        }
        handleMessage(update);
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @PostMapping("/sendMessageToAll")
    public void sendToAll(@RequestBody Map<String, String> map) {
        accountRepository.findAll()
                .forEach(a -> restTemplate.getForObject("https://api.telegram.org/"
                        + "bot"
                        + telegramToken
                        + "/sendMessage?chat_id="
                        + a.getTelegramUserId()
                        + "&text="
                        + map.get("text"), Object.class));
    }

    private void handleMessage(TUpdate update) {
        logger.info("receive: {}", update);
        if (Objects.nonNull(update.getMessage())) {
            commandDispatcher.handleMessage(update.getMessage());
        } else if (Objects.nonNull(update.getCallBackQuery())) {
            commandDispatcher.handleCallback(update.getCallBackQuery());
        }
    }

    private boolean validate(String token) {
        return telegramToken.equalsIgnoreCase(token);
    }

}
