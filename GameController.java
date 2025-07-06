package com.example.quiz_app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GameController {

    @FXML private Label lblQuestion;
    @FXML private RadioButton opt1, opt2, opt3, opt4;
    @FXML private Label lblTimer;
    @FXML private Button btnNext, btnExit;

    private ToggleGroup options;
    private List<Question> questions;
    private int currentIndex = 0;
    private int score = 0;
    private Timer timer;
    private int timeLeft = 15;

    @FXML
    public void initialize() {
        options = new ToggleGroup();
        opt1.setToggleGroup(options);
        opt2.setToggleGroup(options);
        opt3.setToggleGroup(options);
        opt4.setToggleGroup(options);

        loadQuestions();
        showQuestion();
        startTimer();
    }

    private void loadQuestions() {
        questions = new ArrayList<>();
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/quizdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    "Atif",
                    "arpita"
            );

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM questions ORDER BY RAND() LIMIT 5");

            while (rs.next()) {
                Question q = new Question(
                        rs.getString("question"),
                        rs.getString("option1"),
                        rs.getString("option2"),
                        rs.getString("option3"),
                        rs.getString("option4"),
                        rs.getString("correct_option") // This is 1, 2, 3 or 4
                );
                questions.add(q);
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            endQuiz();
            return;
        }

        Question q = questions.get(currentIndex);
        lblQuestion.setText((currentIndex + 1) + ". " + q.getQuestion());
        opt1.setText(q.getOption1());
        opt2.setText(q.getOption2());
        opt3.setText(q.getOption3());
        opt4.setText(q.getOption4());
        options.selectToggle(null);
        resetTimer();
    }

    @FXML
    private void handleNext() {
        RadioButton selected = (RadioButton) options.getSelectedToggle();
        if (selected != null) {
            String selectedText = selected.getText();

            // Correct option from DB is 1/2/3/4
            Question currentQuestion = questions.get(currentIndex);
            int correctOption = Integer.parseInt(currentQuestion.getAnswer());

            String correctText = switch (correctOption) {
                case 1 -> opt1.getText();
                case 2 -> opt2.getText();
                case 3 -> opt3.getText();
                case 4 -> opt4.getText();
                default -> "";
            };

            if (selectedText.equals(correctText)) {
                score++;
            }
        }

        currentIndex++;
        showQuestion();
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    private void endQuiz() {
        stopTimer();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Quiz Finished");
        dialog.setHeaderText("Your Score: " + score + "/" + questions.size());
        dialog.setContentText("Please enter your name:");

        dialog.showAndWait().ifPresent(name -> {
            saveOrUpdatePlayerScore(name, score);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Score Saved");
            alert.setHeaderText("Thanks for playing, " + name + "!");
            alert.setContentText("Your score (" + score + ") has been saved.");
            alert.showAndWait();

            Platform.exit();
        });
    }

    private void saveOrUpdatePlayerScore(String playerName, int score) {
        String url = "jdbc:mysql://localhost:3306/quizdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = "Atif";
        String password = "arpita";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String selectSQL = "SELECT score FROM players WHERE name = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
            selectStmt.setString(1, playerName);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                int existingScore = rs.getInt("score");
                if (score > existingScore) {
                    String updateSQL = "UPDATE players SET score = ? WHERE name = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
                    updateStmt.setInt(1, score);
                    updateStmt.setString(2, playerName);
                    updateStmt.executeUpdate();
                }
            } else {
                String insertSQL = "INSERT INTO players (name, score) VALUES (?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
                insertStmt.setString(1, playerName);
                insertStmt.setInt(2, score);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    timeLeft--;
                    lblTimer.setText("⏳ সময় বাকি: " + timeLeft + " সেকেন্ড");

                    if (timeLeft <= 0) {
                        currentIndex++;
                        showQuestion();
                    }
                });
            }
        }, 1000, 1000);
    }

    private void resetTimer() {
        timeLeft = 15;
    }

    private void stopTimer() {
        if (timer != null) timer.cancel();
    }
}
