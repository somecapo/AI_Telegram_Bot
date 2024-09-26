package com.javarush.telegram;


import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;


public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "**";
    public static final String TELEGRAM_BOT_TOKEN = "**";
    public static final String OPEN_AI_TOKEN = "**";
    private static final String WAITING_ANSWER = "ChatGPT думает...";

    private DialogMode currentMode;
    private final ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private final ArrayList<String> list = new ArrayList<>();
    private UserInfo userInfo;


    private UserInfo woman;

    private int questionCount;


    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String message = getMessageText();

        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");
            sendTextMessage(text);
            showMainMenu("главное меню", "/start",
                    "генерация Tinder-профля \uD83D\uDE0E", "/profile",
                    "сообщение для знакомства \uD83E\uDD70", "/opener",
                    "переписка от вашего имени \uD83D\uDE08", "/message",
                    "переписка со звездами 🔥", "/date",
                    "задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }

        //command GPT
        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadPrompt("gpt");

            String answer = chatGPT.sendMessage(prompt, message);
            Message msg = sendTextMessage(WAITING_ANSWER);
            updateTextMessage(msg, answer);
            return;
        }

        //command DATE
        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райн Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }

        if (currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();

            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор!\nТвоя задача пригласить девушку/парня на свидание ❤️ за 5 сообщений!");
                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }

            Message msg = sendTextMessage("Девушка набирает текст...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }

        //command MESSAGE
        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите вашу переписку",
                    "Следующее собщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }

        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);

                Message msg = sendTextMessage(WAITING_ANSWER);
                String answer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg, answer);
                return;
            }

            list.add(message);
            return;
        }

        //command PROFILE
        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            userInfo = new UserInfo();
            questionCount = 1;
            sendTextMessage("Сколько вам лет?");
            return;

        }

        if (currentMode == DialogMode.PROFILE && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    userInfo.age = message;

                    questionCount = 2;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 2:
                    userInfo.occupation = message;

                    questionCount = 3;
                    sendTextMessage("У вас есть Хобби?");
                    return;
                case 3:
                    userInfo.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Что вам не нравится в людях?");
                    return;
                case 4:
                    userInfo.annoys = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;

                case 5:
                    userInfo.goals = message;

                    String aboutMyself = userInfo.toString();
                    String prompt = loadPrompt("profile");
                    Message msg = sendTextMessage(WAITING_ANSWER);
                    String answer = chatGPT.sendMessage(prompt, aboutMyself);
                    updateTextMessage(msg, answer);
                    return;
            }
        }

        // command OPENER
        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            woman = new UserInfo();
            questionCount = 1;
            sendTextMessage("Имя девушки?");

            return;
        }

        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    woman.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько ей лет?");
                    return;

                case 2:
                    woman.age = message;
                    questionCount = 3;
                    sendTextMessage("Какие у нее хобби?");
                    return;

                case 3:
                    woman.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Кем она работает?");
                    return;

                case 4:
                    woman.occupation = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства");
                    return;

                case 5:
                    woman.goals = message;

                    String aboutFriend = message;
                    String prompt = loadPrompt("opener");

                    Message msg = sendTextMessage(WAITING_ANSWER);
                    String answer = chatGPT.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg, answer);
            }

        }


    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
