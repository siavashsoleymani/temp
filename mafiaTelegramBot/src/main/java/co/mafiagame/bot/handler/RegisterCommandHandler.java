package co.mafiagame.bot.handler;

import co.mafiagame.bot.Room;
import co.mafiagame.bot.persistence.domain.Account;
import co.mafiagame.bot.telegram.SendMessage;
import co.mafiagame.bot.telegram.SendMessageWithRemoveKeyboard;
import co.mafiagame.bot.telegram.TMessage;
import co.mafiagame.bot.telegram.TReplyKeyboardRemove;
import co.mafiagame.bot.util.MessageHolder;
import co.mafiagame.engine.Constants;
import co.mafiagame.engine.GameMood;
import co.mafiagame.engine.Player;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Esa Hekmatizadeh
 */
@Component
public class RegisterCommandHandler extends TelegramCommandHandler {

    @Override
    protected Collection<String> getCommandString() {
        return Arrays.asList(Constants.Command.REGISTER,
            MessageHolder.get("register", MessageHolder.Lang.FA));
    }

    @Override
    public void execute(TMessage message) {
        if (!message.isGroup()) {
            MessageHolder.Lang lang = getLang(message);
            sendMessage(message, "register.not.allowed.in.private", lang, false);
            return;
        }
        Room room = gameContainer.room(message.getChat().getId());
        if (Objects.isNull(room)) {
            MessageHolder.Lang lang = getLang(message);
            sendMessage(message, "register.before.start.error", lang, true);
            return;
        }
        if (room.getGame().getGameMood() != GameMood.NOT_STARTED) {
            sendMessage(message, "register.after.game.started.error", room.getLang(), true);
            return;
        }
        Account account = accountCache.get(message.getFrom().getId());
        if (Objects.isNull(account))
            account = accountRepository.save(new Account(message.getFrom()).setLang(room.getLang()));
        register(message, room, account);

    }

    private void register(TMessage message, Room room, Account account) {
        boolean started = room.getGame().registerPlayer(String.valueOf(message.getFrom().getId()));
        client.send(new SendMessageWithRemoveKeyboard()
            .setReplyMarkup(new TReplyKeyboardRemove()
                .setSelective(true))
            .setReplyToMessageId(message.getId())
            .setChatId(room.getRoomId())
            .setText(MessageHolder.get("player.successfully.registered", room.getLang(),
                accountCache.get(message.getFrom().getId()).fullName())));
        gameContainer.putUserRoom(message.getFrom().getId(), message.getChat().getId());
        room.getAccounts().add(account);
        accountCache.put(account.getTelegramUserId(), account);
        if (started) {
            sendMessage(message, "game.started", room.getLang(), false);
            room.getAccounts().forEach(a -> client.send(new SendMessage()
                .setChatId(a.getTelegramUserId())
                .setText(roleMsg(room, a))
            ));
        }
    }

    public String roleMsg(Room room, Account a) {
        switch (room.getGame().player(String.valueOf(a.getTelegramUserId())).getRole()) {
            case CITIZEN:
                return MessageHolder.get("your.role.is.citizen", room.getLang());
            case DOCTOR:
                return MessageHolder.get("your.role.is.doctor", room.getLang());
            case DETECTIVE:
                return MessageHolder.get("your.role.is.detective", room.getLang());
            case MAFIA:
                return MessageHolder.get("your.role.is.mafia", room.getLang()) + "\n" +
                    MessageHolder.get("mafia.are.players", room.getLang(),
                        room.getGame().mafias().stream()
                            .map(Player::getUserId)
                            .map(Integer::valueOf)
                            .map(room::findPlayer)
                            .map(Optional::get)
                            .map(Account::fullName)
                            .collect(Collectors.joining(
                                MessageHolder.get("and", room.getLang()))));
        }
        throw new IllegalStateException();
    }
}