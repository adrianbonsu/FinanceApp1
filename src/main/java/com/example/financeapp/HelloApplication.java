package com.example.financeapp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.*;

/** Offline JavaFX demo with local transaction storage. */
public class HelloApplication extends Application {
    private static final String PURPLE = "#563BB1", DARK = "#201A40", BG = "#F5F6FC";
    private static final String[] PAGES = {"Dashboard", "Transactions", "Direct Deposit",
            "Top Spending", "Plan", "Goals", "Money Mentor", "Settings"};
    private static final BigDecimal OPENING = new BigDecimal("2400.00");
    private final List<Txn> transactions = new ArrayList<>();
    private final List<String[]> messages = new ArrayList<>();
    private TransactionStore transactionStore;
    private Stage stage;
    private Scene scene;
    private String displayName = "Adrian", goalName = "New laptop";
    private BigDecimal budget = new BigDecimal("2200"), target = new BigDecimal("1500"), saved = new BigDecimal("900");
    private boolean reminders = true;

    private static final class Txn {
        final String description, category;
        final BigDecimal amount;
        Txn(String description, String category, String amount) {
            this.description = description; this.category = category;
            this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override public void init() throws IOException, SQLException {
        transactionStore = new TransactionStore();
        transactions.addAll(transactionStore.load());
        resetChat();
    }

    @Override public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Finora | Personal finance demo");
        stage.setMinWidth(850); stage.setMinHeight(650);
        scene = new Scene(new StackPane(), 1180, 820);
        stage.setScene(scene);
        login(); stage.show();
    }

    private void resetChat() {
        messages.clear();
        messages.add(new String[]{"Money Mentor", "Hi! I can explain this fictional budget, explore the savings goal, or suggest a spending exercise. Try an example below. I use scripted local replies, not a live AI model."});
    }

    private BigDecimal income() { return total(true); }
    private BigDecimal expenses() { return total(false); }
    private BigDecimal total(boolean income) {
        BigDecimal result = BigDecimal.ZERO;
        for (Txn t : transactions) if ((t.amount.signum() > 0) == income) result = result.add(t.amount.abs());
        return result;
    }
    private String money(BigDecimal value) { return NumberFormat.getCurrencyInstance(Locale.US).format(value); }

    private void login() {
        VBox intro = new VBox(22, title("Finora", 40), title("Make room for\nwhat matters.", 30),
                note("A calmer view of spending, planning, and saving."));
        intro.setPadding(new Insets(40)); intro.setPrefWidth(340);
        intro.setStyle("-fx-background-color: #E9E3FF;");
        VBox form = panel(title("Welcome to your demo", 28), note("Fictional data. No account or password required."));
        form.setMaxWidth(420); form.setMaxHeight(350);
        TextField name = new TextField(displayName); name.setId("demo-name");
        name.setAccessibleText("Demo display name");
        Label error = note(""); error.setId("login-error");
        Button enter = button("Explore demo", "demo-login", () -> {
            if (name.getText().trim().isEmpty() || name.getText().trim().length() > 40) {
                error.setText("Enter a fictional display name, 1 to 40 characters."); return;
            }
            displayName = name.getText().trim(); navigate("Dashboard");
        });
        enter.setDefaultButton(true);
        form.getChildren().addAll(new Label("Fictional display name"), name, error, enter,
                note("This is a UI prototype. Never enter real financial or personal information."));
        BorderPane root = new BorderPane(); root.setLeft(intro); root.setCenter(new StackPane(form));
        applyRoot(root);
    }

    private void applyRoot(Region root) {
        root.setStyle(root.getStyle() + "-fx-background-color: " + BG + "; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");
        scene.setRoot(root);
    }

    private void navigate(String page) {
        BorderPane root = new BorderPane();
        VBox nav = new VBox(9); nav.setPadding(new Insets(24, 14, 24, 14)); nav.setPrefWidth(205);
        nav.setStyle("-fx-background-color: " + DARK + ";");
        Label brand = title("Finora", 28); brand.setTextFill(Color.WHITE);
        Label badge = note("DESKTOP DEMO"); badge.setTextFill(Color.web("#DAD2FF"));
        nav.getChildren().addAll(brand, badge, new Separator());
        for (String destination : PAGES) {
            Button b = button(destination, "nav-" + slug(destination), () -> navigate(destination));
            b.setAlignment(Pos.CENTER_LEFT); b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle(b.getStyle() + "-fx-background-color: " + (page.equals(destination) ? "#6650AA" : "transparent") + ";");
            b.setAccessibleText(destination + (page.equals(destination) ? ", current page" : ""));
            nav.getChildren().add(b);
        }
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        Label local = note("Fictional data only\nTransactions saved locally"); local.setTextFill(Color.web("#DAD2FF"));
        nav.getChildren().addAll(spacer, local, button("Leave demo", "logout", this::login));
        root.setLeft(nav);
        VBox content = new VBox(18); content.setPadding(new Insets(28));
        content.getChildren().addAll(note("FINORA  /  " + page.toUpperCase(Locale.ROOT)), title(page, 30),
                note("Demo workspace • USD • Fictional data • No bank connection"));
        switch (page) {
            case "Dashboard": dashboard(content); break;
            case "Transactions": transactionPage(content); break;
            case "Top Spending": spending(content); break;
            case "Plan": plan(content); break;
            case "Goals": goals(content); break;
            case "Direct Deposit": deposit(content); break;
            case "Money Mentor": mentor(content); break;
            case "Settings": settings(content); break;
            default: throw new IllegalArgumentException("Unknown page: " + page);
        }
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + BG + "; -fx-background-color: transparent;");
        root.setCenter(scroll); applyRoot(root);
    }

    private void dashboard(VBox content) {
        content.getChildren().add(title("Hello, " + displayName + ". Let's make a plan.", 22));
        content.getChildren().add(cards(
                metric("Demo account balance", money(OPENING.add(income()).subtract(expenses())), "Opening balance + income - expenses"),
                metric("Sample month income", money(income()), "Fictional deposits"),
                metric("Sample month spending", money(expenses()), "Recorded demo expenses")));
        content.getChildren().add(panel(title("Your next step", 21),
                note("You have " + money(budget.subtract(expenses())) + " remaining in your spending budget."),
                button("Review my plan", "dashboard-plan", () -> navigate("Plan"))));
        content.getChildren().add(panel(title("A goal worth saving for", 21),
                note(goalName + "  •  " + money(saved) + " of " + money(target)), progress(saved, target),
                note("Goal savings are a separate illustration, not deducted from the demo account."),
                button("View savings goal", "dashboard-goals", () -> navigate("Goals"))));
        content.getChildren().add(panel(title("Meet Money Mentor", 21),
                note("Explore example money conversations using this fictional workspace."),
                button("Ask Money Mentor", "dashboard-mentor", () -> navigate("Money Mentor"))));
        VBox recent = panel(title("Recent sample transactions", 21));
        for (int i = Math.max(0, transactions.size() - 3); i < transactions.size(); i++) recent.getChildren().add(transactionRow(transactions.get(i)));
        recent.getChildren().add(button("View all transactions", "dashboard-transactions", () -> navigate("Transactions")));
        content.getChildren().add(recent);
    }

    private void transactionPage(VBox content) {
        TextField search = new TextField(); search.setPromptText("Search description or category"); search.setId("transaction-search");
        search.setAccessibleText("Search demo transactions");
        ComboBox<String> filter = new ComboBox<>(); filter.getItems().addAll("All", "Income", "Expenses"); filter.setValue("All"); filter.setId("transaction-filter");
        VBox rows = new VBox(12); Label count = note(""); count.setId("transaction-count");
        Runnable refresh = () -> {
            rows.getChildren().clear(); int matches = 0;
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            for (Txn t : transactions) {
                boolean type = "All".equals(filter.getValue()) || ("Income".equals(filter.getValue()) ? t.amount.signum() > 0 : t.amount.signum() < 0);
                if (type && (t.description + " " + t.category).toLowerCase(Locale.ROOT).contains(query)) {
                    rows.getChildren().add(transactionRow(t)); matches++;
                }
            }
            count.setText(matches + " matching transactions");
            if (matches == 0) rows.getChildren().add(note("No matches. Try another search or filter."));
        };
        search.textProperty().addListener((o, old, value) -> refresh.run());
        filter.valueProperty().addListener((o, old, value) -> refresh.run()); refresh.run();
        content.getChildren().add(panel(search, filter, button("Add demo transaction", "add-transaction", this::addTransaction), count, rows));
    }

    private Node transactionRow(Txn t) {
        Label amount = title((t.amount.signum() > 0 ? "+" : "-") + money(t.amount.abs()), 17);
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(12, new VBox(3, new Label(t.description), note(t.category)), gap, amount);
        row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #F6F5FC; -fx-background-radius: 8;"); return row;
    }

    private void addTransaction() {
        TextField description = new TextField(); TextField amount = new TextField();
        description.setId("new-description"); amount.setId("new-amount");
        ComboBox<String> type = new ComboBox<>(); type.getItems().addAll("Expense", "Income"); type.setValue("Expense");
        ComboBox<String> category = new ComboBox<>();
        try {
            category.getItems().addAll(transactionStore.categories());
        } catch (SQLException ex) {
            ex.printStackTrace();
            info("Database error", "Finora could not load expense categories.");
            return;
        }
        category.getSelectionModel().selectFirst();
        edit("Add demo transaction", new Node[]{new Label("Fictional description (1-60 characters)"), description,
                new Label("Amount in USD (positive, up to 1,000,000)"), amount, new Label("Type"), type, new Label("Expense category"), category}, () -> {
            String text = description.getText().trim();
            if (text.isEmpty() || text.length() > 60) throw new IllegalArgumentException("Enter a description of 1 to 60 characters.");
            BigDecimal value = positive(amount.getText());
            boolean income = "Income".equals(type.getValue());
            Txn transaction = new Txn(text, income ? "Income" : category.getValue(),
                    (income ? value : value.negate()).toPlainString());
            try {
                transactionStore.add(transaction);
                transactions.add(transaction);
            } catch (SQLException ex) {
                ex.printStackTrace();
                throw new IllegalArgumentException("Finora could not save this transaction. Please try again.", ex);
            }
        }, "Transactions");
    }

    private Map<String, BigDecimal> categories() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        for (Txn t : transactions) if (t.amount.signum() < 0) values.put(t.category, values.getOrDefault(t.category, BigDecimal.ZERO).add(t.amount.abs()));
        return values;
    }
    private void spending(VBox content) {
        PieChart chart = new PieChart(); chart.setAnimated(false); chart.setPrefHeight(300); chart.setId("spending-chart");
        VBox details = panel(title("Where your demo money went", 21));
        for (Map.Entry<String, BigDecimal> entry : categories().entrySet()) {
            chart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue().doubleValue()));
            details.getChildren().add(new Label(entry.getKey() + "   " + money(entry.getValue())));
        }
        content.getChildren().addAll(panel(chart), details);
    }

    private void plan(VBox content) {
        BigDecimal remaining = budget.subtract(expenses());
        content.getChildren().add(cards(metric("Spending budget", money(budget), "Savings excluded"),
                metric("Spent", money(expenses()), "Sample month"), metric(remaining.signum() < 0 ? "Over budget" : "Remaining", money(remaining.abs()), "Budget minus expenses")));
        content.getChildren().add(panel(title("Monthly spending limit", 21), progress(expenses(), budget),
                note(remaining.signum() < 0 ? "The sample spending exceeds your limit." : "Your sample spending is within the limit."),
                button("Edit demo budget", "edit-budget", () -> {
                    TextField value = new TextField(budget.toPlainString()); value.setId("budget-amount");
                    edit("Edit spending budget", new Node[]{new Label("Monthly limit in USD"), value}, () -> budget = positive(value.getText()), "Plan");
                }), button("Discuss my budget", "plan-mentor", () -> { addExchange("Explain my budget"); navigate("Money Mentor"); })));
    }

    private void goals(VBox content) {
        content.getChildren().add(panel(title(goalName, 24), title(money(saved) + " saved", 28), progress(saved, target),
                note("Target: " + money(target) + " • Remaining: " + money(target.subtract(saved).max(BigDecimal.ZERO))),
                note("Illustrative savings tracker only. Adding savings does not move money or change transactions."),
                button("Edit goal", "edit-goal", () -> {
                    TextField name = new TextField(goalName), value = new TextField(target.toPlainString());
                    edit("Edit savings goal", new Node[]{new Label("Goal name"), name, new Label("Target in USD"), value}, () -> {
                        String text = name.getText().trim(); BigDecimal next = positive(value.getText());
                        if (text.isEmpty() || text.length() > 60) throw new IllegalArgumentException("Use a goal name of 1 to 60 characters.");
                        if (next.compareTo(saved) < 0) throw new IllegalArgumentException("Target cannot be below the amount already saved.");
                        goalName = text; target = next;
                    }, "Goals");
                }), button("Add demo savings", "add-savings", () -> {
                    TextField value = new TextField();
                    edit("Add illustrative savings", new Node[]{new Label("Contribution in USD"), value}, () -> {
                        BigDecimal next = saved.add(positive(value.getText()));
                        if (next.compareTo(target) > 0) throw new IllegalArgumentException("Contribution exceeds the remaining goal amount.");
                        saved = next;
                    }, "Goals");
                })));
    }

    private void deposit(VBox content) {
        content.getChildren().add(panel(title("Direct deposit preview", 22),
                note("No bank connected. This screen demonstrates the flow without collecting bank details."),
                new Label("Bank: Fictional Demo Bank"), new Label("Account: DEMO ONLY — no real account number"),
                button("Preview setup steps", "deposit-preview", () -> info("Direct deposit preview",
                        "In a future connected product: choose a verified provider, review permissions, then confirm with your employer. This demo cannot connect a bank or initiate deposits."))));
        VBox history = panel(title("Fictional deposit history", 21));
        for (Txn t : transactions) if (t.amount.signum() > 0) history.getChildren().add(transactionRow(t));
        content.getChildren().add(history);
    }

    private void mentor(VBox content) {
        content.getChildren().add(panel(title("Money Mentor • AI assistant demo", 23),
                note("OFFLINE / SCRIPTED — No live AI, API key, bank access, or financial actions. Educational examples only; not personalized financial, tax, legal, or investment advice."),
                note("Use fictional questions. Do not enter account numbers, passwords, card details, or other personal information.")));
        FlowPane examples = new FlowPane(8, 8);
        String[] prompts = {"Explain my budget", "How can I save for my goal?", "Where can I reduce spending?", "Which stock should I buy?"};
        for (String prompt : prompts) examples.getChildren().add(button(prompt, "example-" + slug(prompt), () -> { addExchange(prompt); navigate("Money Mentor"); }));
        VBox thread = new VBox(12); thread.setId("mentor-thread");
        for (String[] message : messages) thread.getChildren().add(panel(title(message[0], 16), note(message[1])));
        ScrollPane history = new ScrollPane(thread); history.setFitToWidth(true); history.setPrefHeight(310);
        history.setId("mentor-history"); history.setVvalue(1);
        thread.heightProperty().addListener((o, old, value) -> history.setVvalue(1));
        TextArea input = new TextArea(); input.setPromptText("Ask a fictional budget question (up to 500 characters)"); input.setPrefRowCount(2); input.setWrapText(true); input.setId("mentor-input"); input.setAccessibleText("Message Money Mentor demo");
        Label status = note(""); status.setId("mentor-status");
        Button send = button("Send demo message", "mentor-send", () -> {
            String question = input.getText().trim();
            if (question.isEmpty() || question.length() > 500) { status.setText("Enter a question of 1 to 500 characters."); return; }
            addExchange(question); navigate("Money Mentor");
        });
        content.getChildren().addAll(examples, history, input, status, new FlowPane(10, 10, send,
                button("Clear conversation", "mentor-clear", () -> {
                    messages.clear(); messages.add(new String[]{"Money Mentor", "Conversation cleared. Try a fictional budget or savings question."}); navigate("Money Mentor");
                })), note("Replies match keywords and can miss context. Unsupported questions receive a fallback. The chat keeps up to 20 recent messages in this session."));
    }

    private void addExchange(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        // A small demo privacy screen, not a production PII detector. Blocked input is not retained.
        boolean sensitive = q.matches("(?s).*\\d[\\d\\s-]{6,}\\d.*") || q.contains("password") || q.contains("routing") || q.contains("account number") || q.contains("ssn") || q.contains("social security") || q.contains("@");
        String answer;
        if (sensitive) answer = "Please do not share real personal or financial details. I have not added your input to the demo conversation. Use a fictional question about budgeting or saving instead.";
        else if (q.contains("stock") || q.contains("crypto") || q.contains("invest") || q.contains("tax") || q.contains("legal") || q.contains("guarantee"))
            answer = "I cannot recommend a specific investment, guarantee returns, or give personalized tax or legal advice. A qualified professional can help with those decisions. For this demo, try asking about the fictional spending budget.";
        else if (q.contains("transfer") || q.contains("send money") || q.contains("pay bill") || q.contains("connect bank"))
            answer = "I cannot move money, pay bills, or connect accounts. This is an offline UI demonstration. You can explore the Plan and Goals screens using fictional values.";
        else if (q.contains("save") || q.contains("goal") || q.contains("laptop")) {
            BigDecimal remaining = target.subtract(saved).max(BigDecimal.ZERO);
            answer = "Your fictional goal, " + goalName + ", has " + money(saved) + " saved toward " + money(target) + ". That leaves " + money(remaining) + ". "
                    + (remaining.signum() == 0 ? "The illustrated goal is complete!" : "A six-month illustration would require about " + money(remaining.divide(new BigDecimal("6"), 2, RoundingMode.UP)) + " per month. Check essential costs before choosing an amount.")
                    + " This is an example calculation, not a transfer or a prediction.";
        } else if (q.contains("reduce") || q.contains("spending") || q.contains("food")) {
            Map<String, BigDecimal> spending = categories();
            BigDecimal food = spending.getOrDefault("Groceries & Food", BigDecimal.ZERO)
                    .add(spending.getOrDefault("Coffee & Snacks", BigDecimal.ZERO));
            answer = "The sample month shows " + money(expenses()) + " in expenses, including " + money(food) + " for food. A hypothetical 10% reduction in food spending would free about " + money(food.multiply(new BigDecimal("0.10"))) + ". Try reviewing optional purchases while protecting essentials. This does not change your plan automatically.";
        } else if (q.contains("budget") || q.contains("income") || q.contains("balance"))
            answer = "In this fictional month, income is " + money(income()) + " and expenses are " + money(expenses()) + ". The spending limit is " + money(budget) + ", leaving " + money(budget.subtract(expenses())) + " (a negative amount means over budget). The account includes a separate " + money(OPENING) + " opening balance. Goal savings are tracked separately. These totals only describe the demo entries; future bills are not forecast.";
        else answer = "This scripted demo did not recognize that topic. Try: Explain my budget; How can I save for my goal?; or Where can I reduce spending? I cannot verify real financial facts or provide personalized recommendations.";
        messages.add(new String[]{"You", sensitive ? "[Input withheld by demo privacy check]" : question});
        messages.add(new String[]{"Money Mentor • scripted reply", answer});
        while (messages.size() > 20) messages.remove(0);
    }

    private void settings(VBox content) {
        TextField name = new TextField(displayName); name.setId("settings-name");
        CheckBox reminder = new CheckBox("Show demo budget reminder preference"); reminder.setSelected(reminders);
        Label status = note(""); status.setId("settings-status");
        content.getChildren().add(panel(title("Demo profile", 21), new Label("Fictional display name"), name, reminder,
                note("Currency: USD. Reminder preference is illustrative; no notifications are sent."),
                button("Save session preferences", "settings-save", () -> {
                    String value = name.getText().trim();
                    if (value.isEmpty() || value.length() > 40) { status.setText("Use a name of 1 to 40 characters."); return; }
                    displayName = value; reminders = reminder.isSelected(); status.setText("Saved for this session.");
                }), status));
        content.getChildren().add(panel(title("Reset for the next test participant", 21), note("Restores the session preferences, goal, budget, and chat. Saved transactions stay in the database."),
                button("Reset demo", "reset-demo", () -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Reset demo settings and chat?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.initOwner(stage); confirm.setHeaderText("Reset demo?");
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        budget = new BigDecimal("2200"); target = new BigDecimal("1500"); saved = new BigDecimal("900"); goalName = "New laptop"; displayName = "Adrian"; reminders = true; resetChat(); navigate("Dashboard");
                    }
                })));
    }

    private BigDecimal positive(String text) {
        if (!text.trim().matches("[0-9]{1,7}(\\.[0-9]{1,2})?")) throw new IllegalArgumentException("Enter a positive USD amount, with up to two decimal places and no currency symbols.");
        BigDecimal value = new BigDecimal(text.trim()).setScale(2);
        if (value.signum() <= 0 || value.compareTo(new BigDecimal("1000000")) > 0) throw new IllegalArgumentException("Amount must be greater than zero and at most 1,000,000.");
        return value;
    }
    private void edit(String heading, Node[] fields, Runnable save, String page) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.initOwner(stage); dialog.setTitle(heading);
        VBox form = panel(fields); form.setPrefWidth(390); Label error = note(""); error.setId("form-error");
        form.getChildren().addAll(error, note("Fictional data only."));
        dialog.getDialogPane().setContent(form); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            try { save.run(); } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); e.consume(); }
        });
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) navigate(page);
    }
    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message); alert.initOwner(stage); alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }
    private String slug(String text) { return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("-$", ""); }
    private Button button(String text, String id, Runnable action) {
        Button b = new Button(text); b.setId(id); b.setAccessibleText(text); b.setWrapText(true);
        b.setMinHeight(40); b.setStyle("-fx-background-color: " + PURPLE + "; -fx-text-fill: white; -fx-background-radius: 9; -fx-padding: 10 14; -fx-cursor: hand;");
        b.setOnAction(e -> action.run()); return b;
    }
    private Label title(String text, int size) {
        Label label = new Label(text); label.setFont(Font.font("Segoe UI", FontWeight.BOLD, size)); label.setTextFill(Color.web(DARK)); label.setWrapText(true); return label;
    }
    private Label note(String text) {
        Label label = new Label(text); label.setWrapText(true); label.setTextFill(Color.web("#535B76")); label.setMinHeight(Region.USE_PREF_SIZE); return label;
    }
    private VBox panel(Node... children) {
        VBox box = new VBox(13, children); box.setPadding(new Insets(22)); box.setMinWidth(0);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E4E5F0; -fx-border-radius: 14;"); return box;
    }
    private VBox metric(String name, String value, String detail) { return panel(note(name), title(value, 27), note(detail)); }
    private TilePane cards(Node... nodes) {
        TilePane pane = new TilePane(12, 12); pane.setPrefColumns(3); pane.setPrefTileWidth(260);
        for (Node node : nodes) { ((Region) node).setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); pane.getChildren().add(node); } return pane;
    }
    private ProgressBar progress(BigDecimal value, BigDecimal maximum) {
        ProgressBar bar = new ProgressBar(Math.max(0, Math.min(1, value.doubleValue() / maximum.doubleValue())));
        bar.setMaxWidth(Double.MAX_VALUE); bar.setStyle("-fx-accent: " + PURPLE + ";");
        bar.setAccessibleText(money(value) + " of " + money(maximum)); return bar;
    }
    public static void main(String[] args) { launch(args); }

    private static final class TransactionStore {
        // The migration assigns the seeded expenses to this demo profile.
        private static final int DEMO_USER = 2026781;
        private final String url;

        TransactionStore() throws IOException, SQLException {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path directory = localAppData == null || localAppData.isBlank()
                    ? Path.of(System.getProperty("user.home"), ".finora")
                    : Path.of(localAppData, "Finora");
            Files.createDirectories(directory);
            url = "jdbc:sqlite:" + directory.resolve("money_mentor.db");
            initialize();
        }

        private Connection connect() throws SQLException {
            Connection connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
                return connection;
            } catch (SQLException ex) {
                connection.close();
                throw ex;
            }
        }

        private void initialize() throws IOException, SQLException {
            try (Connection connection = connect(); Statement statement = connection.createStatement()) {
                statement.execute("BEGIN IMMEDIATE");
                try {
                    int version;
                    try (var result = statement.executeQuery("PRAGMA user_version")) {
                        version = result.getInt(1);
                    }
                    if (version == 0) {
                        importDemoSchema(statement);
                        statement.executeUpdate("""
                                CREATE TABLE demo_income (
                                    income_id INTEGER PRIMARY KEY,
                                    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                    description TEXT NOT NULL,
                                    amount NUMERIC NOT NULL CHECK (amount > 0)
                                )
                                """);
                        try (var insert = connection.prepareStatement(
                                "INSERT INTO demo_income (user_id, description, amount) VALUES (?, 'Demo salary', 3200)")) {
                            insert.setInt(1, DEMO_USER);
                            insert.executeUpdate();
                        }
                        statement.execute("PRAGMA user_version = 1");
                    } else if (version != 1) {
                        throw new SQLException("Unsupported Finora database version: " + version);
                    }
                    statement.execute("COMMIT");
                } catch (SQLException | IOException ex) {
                    try {
                        statement.execute("ROLLBACK");
                    } catch (SQLException rollbackError) {
                        ex.addSuppressed(rollbackError);
                    }
                    throw ex;
                }
            }
        }

        private void importDemoSchema(Statement statement) throws IOException, SQLException {
            var resource = HelloApplication.class.getResourceAsStream("/money_mentor_db.sql");
            if (resource == null) {
                throw new IOException("Missing money_mentor_db.sql from the application resources.");
            }
            try (var reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
                StringBuilder sql = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    String text = line.strip();
                    // The seed script uses whole-line comments and ends statements on a separate line or at line end.
                    if (text.isEmpty() || text.startsWith("--") || text.equals("PRAGMA foreign_keys = ON;")) {
                        continue;
                    }
                    sql.append(text).append('\n');
                    if (text.endsWith(";")) {
                        statement.execute(sql.toString());
                        sql.setLength(0);
                    }
                }
                if (!sql.isEmpty()) {
                    throw new IOException("Incomplete statement in money_mentor_db.sql.");
                }
            }
        }

        List<Txn> load() throws SQLException {
            String sql = """
                    SELECT COALESCE(NULLIF(e.notes, ''), v.name, 'Expense') AS description,
                           COALESCE(c.name, 'Uncategorized') AS category, -e.amount AS amount,
                           1 AS sort_group, e.expense_id AS sort_id
                      FROM expenses e
                      LEFT JOIN categories c ON c.category_id = e.category_id
                      LEFT JOIN vendors v ON v.vendor_id = e.vendor_id
                     WHERE e.user_id = ?
                    UNION ALL
                    SELECT description, 'Income', amount, 0, income_id
                      FROM demo_income WHERE user_id = ?
                    ORDER BY sort_group, sort_id
                    """;
            List<Txn> items = new ArrayList<>();
            try (Connection connection = connect(); var query = connection.prepareStatement(sql)) {
                query.setInt(1, DEMO_USER);
                query.setInt(2, DEMO_USER);
                try (var rows = query.executeQuery()) {
                    while (rows.next()) {
                        items.add(new Txn(rows.getString("description"), rows.getString("category"),
                                rows.getBigDecimal("amount").toPlainString()));
                    }
                }
            }
            return items;
        }

        List<String> categories() throws SQLException {
            List<String> names = new ArrayList<>();
            try (Connection connection = connect(); Statement statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT name FROM categories WHERE user_id IS NULL ORDER BY name")) {
                while (rows.next()) {
                    names.add(rows.getString(1));
                }
            }
            return names;
        }

        void add(Txn transaction) throws SQLException {
            boolean income = transaction.amount.signum() > 0;
            String sql = income
                    ? "INSERT INTO demo_income (user_id, description, amount) VALUES (?, ?, ?)"
                    : """
                      INSERT INTO expenses (user_id, notes, amount, category_id, expense_date)
                      SELECT ?, ?, ?, category_id, DATE('now', 'localtime')
                        FROM categories WHERE name = ? AND user_id IS NULL
                       ORDER BY category_id LIMIT 1
                      """;
            try (Connection connection = connect(); var insert = connection.prepareStatement(sql)) {
                insert.setInt(1, DEMO_USER);
                insert.setString(2, transaction.description);
                insert.setBigDecimal(3, transaction.amount.abs());
                if (!income) {
                    insert.setString(4, transaction.category);
                }
                if (insert.executeUpdate() != 1) {
                    throw new SQLException("No matching demo expense category.");
                }
            }
        }
    }
}
