package io.proj3ct.telegramjokebot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.telegramjokebot.config.BotConfig;
import io.proj3ct.telegramjokebot.model.Joke;
import io.proj3ct.telegramjokebot.model.JokeRepository;
import io.proj3ct.telegramjokebot.model.User;
import io.proj3ct.telegramjokebot.model.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.*;
import org.telegram.telegrambots.meta.api.objects.commands.scope.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private BotConfig config;
    static final String HELP_TEXT = "This bot is created to send a random joke from the database each time you request it.\n\n" +
            "You can execute commands from the main menu on the left or by typing commands manually\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /joke to get a random joke\n\n" +
            "Type /settings to list available settings to configure\n\n" +
            "Type /help to see this message again\n";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JokeRepository jokeRepository;

    static final int MAX_JOKE_ID_MINUS_ONE = 3772;
    static final String NEXT_JOKE = "NEXT_JOKE";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/joke", "get a random joke"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotUserName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            // Set variables
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {

                case "/start" -> {
                    showStart(chatId, update.getMessage().getChat().getFirstName());
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        TypeFactory typeFactory = objectMapper.getTypeFactory();
                        List<Joke> jokeList = objectMapper.readValue(new File("db/stupidstuff.json"),
                                typeFactory.constructCollectionType(List.class, Joke.class));
                        jokeRepository.saveAll(jokeList);
                    } catch (Exception e) {
                        log.error(Arrays.toString(e.getStackTrace()));
                    }
                }

                case "/joke" -> {

                    var joke = getRandomJoke();

                    joke.ifPresent(randomJoke -> addButtonAndSendMessage(randomJoke.getBody(), chatId));


                }
                default -> commandNotFound(chatId);

            }

        }

        else if(update.hasCallbackQuery()){

            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.equals(NEXT_JOKE)) {

                var joke = getRandomJoke();

                joke.ifPresent(randomJoke -> addButtonAndSendMessage(randomJoke.getBody(), chatId));


            }

        }
    }

    private Optional<Joke> getRandomJoke(){
        var r = new Random();
        var randomId = r.nextInt(MAX_JOKE_ID_MINUS_ONE) + 1;

        return jokeRepository.findById(randomId);
    }

    private void addButtonAndSendMessage(String joke, long chatId){

        SendMessage message = new SendMessage();
        message.setText(joke);
        message.setChatId(chatId);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var inlinekeyboardButton = new InlineKeyboardButton();
        inlinekeyboardButton.setCallbackData(NEXT_JOKE);
        inlinekeyboardButton.setText(EmojiParser.parseToUnicode("next joke " + ":rolling_on_the_floor_laughing:"));
        rowInline.add(inlinekeyboardButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        send(message);

    }


    private void showStart(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode(
                "Hi, " + name + "! :smile:" + " Nice to meet you! I am a Simple Random Joke Bot created by Dmitrijs Finaskins from proj3c.io \n");
        sendMessage(answer, chatId);
    }

    private void commandNotFound(long chatId) {

        String answer = EmojiParser.parseToUnicode(
                "Command not recognized, please verify and try again :stuck_out_tongue_winking_eye: ");
        sendMessage(answer, chatId);

    }

    private void sendMessage(String textToSend, long chatId) {
        SendMessage message = new SendMessage(); // Create a message object object
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        send(message);
    }

    private void send(SendMessage msg) {
        try {
            execute(msg); // Sending our message object to user
        } catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }


}
