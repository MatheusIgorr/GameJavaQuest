import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class QuestJewells extends Application {

    // Constantes do jogo
    private static final int NUM_REELS = 6;
    private static final int NUM_ROWS = 5;
    private static final int INITIAL_CREDITS = 1000;
    private static final int[] BET_VALUES = { 10, 20, 50, 100 };
    private static final int BONUS_COST = 500; // Custo para comprar o bônus

    // Elementos da interface
    private Label creditLabel;
    private Label winLabel;
    private Label freeSpinsLabel;
    private Label multiplierLabel;
    private Label betLabel;
    private Button spinButton;
    private Button autoSpinButton;
    private Button buyBonusButton;
    private Button stopAutoSpinButton;
    private Slider betSlider;
    private ImageView[][] reels = new ImageView[NUM_REELS][NUM_ROWS];
    private boolean[] spinning = new boolean[NUM_REELS];
    private StackPane[][] reelContainers = new StackPane[NUM_REELS][NUM_ROWS];
    private Pane rootPane;

    // Estado do jogo
    private int credits = INITIAL_CREDITS;
    private int winAmount = 0;
    private int freeSpins = 0;
    private double multiplier = 1.0;
    private int currentBet = BET_VALUES[0];
    private List<Double> activeMultipliers = new ArrayList<>();
    private boolean isAutoSpinning = false;
    private int autoSpinsRemaining = 0;

    // Símbolos do jogo
    private Image[] symbols = new Image[14]; // 10 símbolos + 4 multiplicadores
    private String[] symbolNames = { "Zeus", "Poseidon", "Hades", "Athena", "Hermes", "Diamond", "Crown", "Chalice",
            "Hourglass", "Scatter", "x2", "x5", "x10", "x100" };
    private int[] symbolValues = { 200, 150, 100, 80, 60, 50, 40, 30, 20, 0, 0, 0, 0, 0 };
    private Color[] symbolColors = {
            Color.GOLD, Color.BLUE, Color.PURPLE, Color.SILVER,
            Color.ORANGE, Color.CYAN, Color.YELLOW, Color.RED,
            Color.BROWN, Color.LIGHTBLUE, Color.GREEN, Color.ORANGE,
            Color.PURPLE, Color.RED
    };

    // Probabilidades dos símbolos (índice 9 - Scatter tem probabilidade mais baixa)
    private double[] symbolProbabilities = {
            0.12, 0.12, 0.12, 0.12, 0.12, // Símbolos normais
            0.12, 0.12, 0.12, 0.12,
            0.02, // Scatter - probabilidade reduzida (apenas 2%)
            0.02, 0.02, 0.02, 0.02 // Multiplicadores
    };

    @Override
    public void start(Stage primaryStage) {
        // Criar ícones programaticamente
        createSymbolIcons();

        // Layout principal com fundo temático
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0a2a, #1a1a4a);");
        rootPane = root;

        // Título com estilo dourado
        Label titleLabel = new Label(" Quest Jewells");
        titleLabel.setFont(Font.font("Times New Roman", FontWeight.EXTRA_BOLD, 36));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.8), 15, 0, 0, 0);");

        // Painel de informações com estilo ornamentado
        HBox infoPanel = new HBox(20);
        infoPanel.setAlignment(Pos.CENTER);
        infoPanel.setPadding(new Insets(10));
        infoPanel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 10; -fx-border-color: linear-gradient(to right, #d4af37, #ffd700); -fx-border-width: 2; -fx-border-radius: 10;");

        creditLabel = createOrnateLabel("CRÉDITOS: " + credits, Color.WHITE);
        winLabel = createOrnateLabel("PRÊMIO: " + winAmount, Color.GOLD);
        freeSpinsLabel = createOrnateLabel("RODADAS GRÁTIS: " + freeSpins, Color.LIGHTGREEN);
        multiplierLabel = createOrnateLabel("MULTIPLICADOR: x" + multiplier, Color.ORANGE);
        betLabel = createOrnateLabel("APOSTA: " + currentBet, Color.LIGHTBLUE);

        infoPanel.getChildren().addAll(creditLabel, winLabel, freeSpinsLabel, multiplierLabel, betLabel);

        // Painel de controle de aposta
        HBox betPanel = new HBox(10);
        betPanel.setAlignment(Pos.CENTER);
        betPanel.setPadding(new Insets(5));
        betPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 5;");

        Label betTitle = createOrnateLabel("Valor da Aposta:", Color.WHITE);

        betSlider = new Slider(0, BET_VALUES.length - 1, 0);
        betSlider.setMajorTickUnit(1);
        betSlider.setMinorTickCount(0);
        betSlider.setSnapToTicks(true);
        betSlider.setPrefWidth(200);
        betSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentBet = BET_VALUES[newVal.intValue()];
            betLabel.setText("APOSTA: " + currentBet);
            spinButton.setText("GIRAR (" + currentBet + " créditos)");
        });

        betPanel.getChildren().addAll(betTitle, betSlider);

        // Painel dos rolos (grade 6x5)
        GridPane reelsPanel = new GridPane();
        reelsPanel.setAlignment(Pos.CENTER);
        reelsPanel.setHgap(8);
        reelsPanel.setVgap(8);
        reelsPanel.setPadding(new Insets(15));
        reelsPanel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 15; -fx-border-color: linear-gradient(to right, #d4af37, #ffd700); -fx-border-width: 3; -fx-border-radius: 15;");

        for (int col = 0; col < NUM_REELS; col++) {
            for (int row = 0; row < NUM_ROWS; row++) {
                reels[col][row] = new ImageView();
                reels[col][row].setFitWidth(65);
                reels[col][row].setFitHeight(65);
                reels[col][row].setImage(symbols[0]);

                // Moldura ornamentada para cada símbolo
                StackPane reelContainer = createOrnateReelContainer();
                reelContainer.getChildren().add(reels[col][row]);
                reelContainers[col][row] = reelContainer;

                reelsPanel.add(reelContainer, col, row);
            }
        }

        // Painel de controles adicionais
        HBox controlPanel = new HBox(15);
        controlPanel.setAlignment(Pos.CENTER);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");

        // Botão de giro automático
        autoSpinButton = new Button("GIRO AUTOMÁTICO");
        styleButton(autoSpinButton, Color.DARKGREEN, Color.LIMEGREEN);
        autoSpinButton.setOnAction(e -> showAutoSpinDialog());

        // Botão de parar giro automático
        stopAutoSpinButton = new Button("PARAR AUTOMÁTICO");
        styleButton(stopAutoSpinButton, Color.DARKRED, Color.RED);
        stopAutoSpinButton.setDisable(true);
        stopAutoSpinButton.setOnAction(e -> stopAutoSpin());

        // Botão de comprar bônus
        buyBonusButton = new Button("COMPRAR BÔNUS (" + BONUS_COST + ")");
        styleButton(buyBonusButton, Color.DARKBLUE, Color.ROYALBLUE);
        buyBonusButton.setOnAction(e -> buyBonus());

        controlPanel.getChildren().addAll(autoSpinButton, stopAutoSpinButton, buyBonusButton);

        // Botão de girar com estilo dourado
        spinButton = new Button("GIRAR (" + currentBet + " créditos)");
        spinButton.setFont(Font.font("Times New Roman", FontWeight.BOLD, 20));
        spinButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #d4af37, #ffd700); -fx-text-fill: #2a0a0a; -fx-background-radius: 20; -fx-padding: 12 25 12 25;");
        spinButton.setPrefSize(250, 55);
        spinButton.setOnAction(e -> spinReels());

        // Efeito de hover no botão
        spinButton.setOnMouseEntered(e -> spinButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffd700, #ffaa00); -fx-text-fill: #2a0a0a; -fx-background-radius: 20; -fx-padding: 12 25 12 25; -fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.8), 10, 0, 0, 0);"));
        spinButton.setOnMouseExited(e -> spinButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #d4af37, #ffd700); -fx-text-fill: #2a0a0a; -fx-background-radius: 20; -fx-padding: 12 25 12 25;"));

        // Adicionar todos os elementos ao layout principal
        root.getChildren().addAll(titleLabel, infoPanel, betPanel, reelsPanel, controlPanel, spinButton);

        // Configurar a cena
        Scene scene = new Scene(root, 900, 800);
        primaryStage.setTitle("Gates of Olympus - Versão Premium");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void styleButton(Button button, Color baseColor, Color hoverColor) {
        button.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
        button.setStyle("-fx-background-color: " + toHex(baseColor) +
                "; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 15 8 15;");
        button.setPrefSize(180, 40);

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: " + toHex(hoverColor) +
                "; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 15 8 15;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + toHex(baseColor) +
                "; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 15 8 15;"));
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void showAutoSpinDialog() {
        // Diálogo simples para selecionar número de giros automáticos
        Stage dialog = new Stage();
        dialog.setTitle("Giros Automáticos");

        VBox dialogVbox = new VBox(20);
        dialogVbox.setAlignment(Pos.CENTER);
        dialogVbox.setPadding(new Insets(20));
        dialogVbox.setStyle("-fx-background-color: #1a1a4a;");

        Label titleLabel = new Label("Selecione o número de giros automáticos:");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        HBox spinnerBox = new HBox(10);
        spinnerBox.setAlignment(Pos.CENTER);

        Slider spinCountSlider = new Slider(5, 100, 10);
        spinCountSlider.setMajorTickUnit(10);
        spinCountSlider.setMinorTickCount(0);
        spinCountSlider.setSnapToTicks(true);
        spinCountSlider.setShowTickLabels(true);
        spinCountSlider.setShowTickMarks(true);
        spinCountSlider.setPrefWidth(200);

        Label spinCountLabel = new Label("10 giros");
        spinCountLabel.setTextFill(Color.WHITE);

        spinCountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            spinCountLabel.setText(value + " giros");
        });

        spinnerBox.getChildren().addAll(spinCountSlider, spinCountLabel);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button confirmButton = new Button("Confirmar");
        styleButton(confirmButton, Color.DARKGREEN, Color.LIMEGREEN);
        confirmButton.setOnAction(e -> {
            startAutoSpin((int) spinCountSlider.getValue());
            dialog.close();
        });

        Button cancelButton = new Button("Cancelar");
        styleButton(cancelButton, Color.DARKRED, Color.RED);
        cancelButton.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(confirmButton, cancelButton);

        dialogVbox.getChildren().addAll(titleLabel, spinnerBox, buttonBox);

        Scene dialogScene = new Scene(dialogVbox, 400, 200);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private void startAutoSpin(int numSpins) {
        autoSpinsRemaining = numSpins;
        isAutoSpinning = true;

        // Atualizar interface
        autoSpinButton.setDisable(true);
        stopAutoSpinButton.setDisable(false);
        spinButton.setDisable(true);
        betSlider.setDisable(true);
        buyBonusButton.setDisable(true);

        // Iniciar o primeiro giro automático
        spinReels();
    }

    private void stopAutoSpin() {
        isAutoSpinning = false;
        autoSpinsRemaining = 0;

        // Restaurar interface
        autoSpinButton.setDisable(false);
        stopAutoSpinButton.setDisable(true);
        spinButton.setDisable(false);
        betSlider.setDisable(false);
        buyBonusButton.setDisable(false);
    }

    private void buyBonus() {
        if (credits >= BONUS_COST) {
            credits -= BONUS_COST;
            creditLabel.setText("CRÉDITOS: " + credits);

            // Conceder 10 rodadas grátis
            freeSpins += 10;
            freeSpinsLabel.setText("RODADAS GRÁTIS: " + freeSpins);

            winLabel.setText("BÔNUS COMPRADO! 10 RODADAS GRÁTIS!");
            winLabel.setTextFill(Color.LIGHTGREEN);

            // Efeito visual especial
            createBonusEffect();
        } else {
            winLabel.setText("CRÉDITOS INSUFICIENTES PARA COMPRAR BÔNUS!");
            winLabel.setTextFill(Color.RED);
        }
    }

    private void createBonusEffect() {
        // Criar efeito visual especial para compra de bônus
        Circle glow = new Circle(450, 400, 0);
        glow.setFill(Color.TRANSPARENT);
        glow.setStroke(Color.GOLD);
        glow.setStrokeWidth(3);
        glow.setOpacity(0);

        rootPane.getChildren().add(glow);

        Timeline bonusEffect = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 0),
                        new KeyValue(glow.opacityProperty(), 0)),
                new KeyFrame(Duration.seconds(0.5),
                        new KeyValue(glow.radiusProperty(), 200),
                        new KeyValue(glow.opacityProperty(), 0.7)),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(glow.radiusProperty(), 400),
                        new KeyValue(glow.opacityProperty(), 0)));

        bonusEffect.setOnFinished(e -> rootPane.getChildren().remove(glow));
        bonusEffect.play();
    }

    private StackPane createOrnateReelContainer() {
        StackPane container = new StackPane();
        container.setPrefSize(70, 70);

        // Fundo com gradiente dourado
        Rectangle bg = new Rectangle(70, 70);
        bg.setArcWidth(15);
        bg.setArcHeight(15);
        bg.setFill(Color.TRANSPARENT);
        bg.setStroke(Color.web("#d4af37"));
        bg.setStrokeWidth(2);

        // Efeito de brilho interno
        Rectangle innerGlow = new Rectangle(60, 60);
        innerGlow.setArcWidth(10);
        innerGlow.setArcHeight(10);
        innerGlow.setFill(Color.TRANSPARENT);
        innerGlow.setStroke(Color.web("#ffd700"));
        innerGlow.setStrokeWidth(1);
        innerGlow.setOpacity(0.7);

        container.getChildren().addAll(bg, innerGlow);
        return container;
    }

    private Label createOrnateLabel(String text, Color color) {
        Label label = new Label(text);
        label.setFont(Font.font("Times New Roman", FontWeight.BOLD, 16));
        label.setTextFill(color);
        label.setStyle("-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.8), 3, 0, 1, 1);");
        return label;
    }

    private void createSymbolIcons() {
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = generateDetailedSymbolIcon(i);
        }
    }

    private Image generateDetailedSymbolIcon(int symbolIndex) {
        int size = 100;
        WritableImage image = new WritableImage(size, size);

        // Renderizar diferentes símbolos baseados no índice
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(size, size);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fundo circular com gradiente dourado
        gc.setFill(Color.rgb(20, 20, 40));
        gc.fillOval(5, 5, size - 10, size - 10);

        // Desenhar símbolo detalhado baseado no tipo
        gc.setFill(symbolColors[symbolIndex]);

        if (symbolIndex >= 10) {
            // Multiplicadores (bolhas coloridas)
            gc.setFill(symbolColors[symbolIndex]);
            gc.fillOval(15, 15, 70, 70);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", FontWeight.BOLD, 30));

            String multiplierText = symbolNames[symbolIndex];
            gc.fillText(multiplierText, size / 2 - 15, size / 2 + 10);
        } else {
            switch (symbolIndex) {
                case 0: // Zeus - raio detalhado
                    double[] xPoints = { size / 2, size / 2 - 10, size / 2 - 5, size / 2, size / 2 + 5, size / 2 + 10 };
                    double[] yPoints = { 15, 40, 40, 60, 40, 40 };
                    gc.fillPolygon(xPoints, yPoints, 6);

                    // Raios menores
                    gc.fillPolygon(new double[] { size / 2 - 15, size / 2 - 20, size / 2 - 10 },
                            new double[] { 25, 35, 35 }, 3);
                    gc.fillPolygon(new double[] { size / 2 + 15, size / 2 + 20, size / 2 + 10 },
                            new double[] { 25, 35, 35 }, 3);
                    break;

                case 1: // Poseidon - tridente detalhado
                    gc.fillRect(size / 2 - 3, 15, 6, 60);
                    // Partes do tridente
                    gc.fillRect(size / 2 - 15, 35, 30, 5);
                    gc.fillRect(size / 2 - 15, 45, 30, 5);
                    // Detalhes ornamentais
                    gc.fillOval(size / 2 - 8, 20, 5, 5);
                    gc.fillOval(size / 2 + 3, 20, 5, 5);
                    break;

                case 2: // Hades - elmo detalhado
                    gc.fillPolygon(new double[] { size / 2, size / 2 - 20, size / 2 + 20 },
                            new double[] { 20, 60, 60 }, 3);
                    // Detalhes no elmo
                    gc.setFill(Color.GRAY);
                    gc.fillRect(size / 2 - 15, 40, 30, 5);
                    gc.fillRect(size / 2 - 10, 50, 20, 5);
                    break;

                case 3: // Athena - coruja detalhada
                    gc.fillOval(size / 2 - 15, 20, 30, 30); // corpo
                    gc.fillOval(size / 2 - 20, 15, 15, 15); // olho esquerdo
                    gc.fillOval(size / 2 + 5, 15, 15, 15); // olho direito
                    // Detalhes das asas
                    gc.fillPolygon(new double[] { size / 2 - 20, size / 2 - 30, size / 2 - 25 },
                            new double[] { 30, 35, 45 }, 3);
                    gc.fillPolygon(new double[] { size / 2 + 20, size / 2 + 30, size / 2 + 25 },
                            new double[] { 30, 35, 45 }, 3);
                    break;

                case 4: // Hermes - asas detalhadas
                    gc.fillPolygon(new double[] { size / 2, size / 2 - 25, size / 2 - 10 },
                            new double[] { 35, 20, 50 }, 3);
                    gc.fillPolygon(new double[] { size / 2, size / 2 + 25, size / 2 + 10 },
                            new double[] { 35, 20, 50 }, 3);
                    // Detalhes nas asas
                    gc.setFill(Color.WHITE);
                    gc.fillPolygon(new double[] { size / 2 - 15, size / 2 - 20, size / 2 - 5 },
                            new double[] { 25, 35, 35 }, 3);
                    gc.fillPolygon(new double[] { size / 2 + 15, size / 2 + 20, size / 2 + 5 },
                            new double[] { 25, 35, 35 }, 3);
                    break;

                case 5: // Diamond - diamante facetado
                    gc.fillPolygon(new double[] { size / 2, size / 2 - 20, size / 2, size / 2 + 20 },
                            new double[] { 15, size / 2, size - 15, size / 2 }, 4);
                    // Facetas
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1);
                    gc.strokeLine(size / 2, 15, size / 2, size - 15);
                    gc.strokeLine(size / 2 - 20, size / 2, size / 2 + 20, size / 2);
                    gc.strokeLine(size / 2, 15, size / 2 + 20, size / 2);
                    gc.strokeLine(size / 2, 15, size / 2 - 20, size / 2);
                    gc.strokeLine(size / 2, size - 15, size / 2 + 20, size / 2);
                    gc.strokeLine(size / 2, size - 15, size / 2 - 20, size / 2);
                    break;

                case 6: // Crown - coroa detalhada
                    gc.fillPolygon(
                            new double[] { size / 2 - 25, size / 2 - 20, size / 2 - 15, size / 2 - 10, size / 2,
                                    size / 2 + 10, size / 2 + 15, size / 2 + 20, size / 2 + 25 },
                            new double[] { 45, 30, 40, 30, 40, 30, 40, 30, 45 }, 9);
                    gc.fillRect(size / 2 - 25, 45, 50, 10);
                    // Joias na coroa
                    gc.setFill(Color.RED);
                    gc.fillOval(size / 2 - 5, 30, 10, 10);
                    gc.setFill(Color.BLUE);
                    gc.fillOval(size / 2 - 20, 35, 8, 8);
                    gc.fillOval(size / 2 + 12, 35, 8, 8);
                    break;

                case 7: // Chalice - cálice detalhado
                    gc.fillRect(size / 2 - 5, 15, 10, 40); // haste
                    gc.fillOval(size / 2 - 15, 50, 30, 15); // base
                    gc.fillRect(size / 2 - 15, 30, 30, 20); // copo
                    // Detalhes
                    gc.setFill(Color.GOLD);
                    gc.fillRect(size / 2 - 15, 30, 30, 5);
                    gc.fillRect(size / 2 - 15, 45, 30, 5);
                    break;

                case 8: // Hourglass - ampulheta detalhada
                    gc.fillPolygon(new double[] { size / 2 - 15, size / 2 + 15, size / 2 + 10, size / 2 - 10 },
                            new double[] { 20, 20, 30, 30 }, 4);
                    gc.fillPolygon(new double[] { size / 2 - 15, size / 2 + 15, size / 2 + 10, size / 2 - 10 },
                            new double[] { 60, 60, 50, 50 }, 4);
                    gc.fillRect(size / 2 - 5, 30, 10, 20);
                    // Areia
                    gc.setFill(Color.BEIGE);
                    gc.fillPolygon(new double[] { size / 2 - 10, size / 2 + 10, size / 2 + 5, size / 2 - 5 },
                            new double[] { 30, 30, 40, 40 }, 4);
                    break;

                case 9: // Scatter - símbolo especial
                    // Estrela com círculo
                    gc.setFill(Color.LIGHTBLUE);
                    gc.fillOval(20, 20, 60, 60);
                    gc.setFill(Color.WHITE);
                    drawStar(gc, size / 2, size / 2, 25, 10);
                    gc.setFill(Color.GOLD);
                    gc.fillOval(30, 30, 40, 40);
                    gc.setFill(Color.WHITE);
                    gc.fillText("S", size / 2 - 5, size / 2 + 5);
                    break;
            }
        }

        // Borda dourada ornamentada
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeOval(5, 5, size - 10, size - 10);

        return canvas.snapshot(null, null);
    }

    private void drawStar(javafx.scene.canvas.GraphicsContext gc, double centerX, double centerY, double outerRadius,
            double innerRadius) {
        int points = 5;
        double[] xPoints = new double[points * 2];
        double[] yPoints = new double[points * 2];

        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI / points * i;
            double radius = i % 2 == 0 ? outerRadius : innerRadius;
            xPoints[i] = centerX + Math.cos(angle) * radius;
            yPoints[i] = centerY + Math.sin(angle) * radius;
        }

        gc.fillPolygon(xPoints, yPoints, points * 2);
    }

    private void spinReels() {
        // Se tiver rodadas grátis, use-as
        if (freeSpins > 0) {
            freeSpins--;
            freeSpinsLabel.setText("RODADAS GRÁTIS: " + freeSpins);
        } else {
            // Verificar se há créditos suficientes
            if (credits < currentBet) {
                winLabel.setText("CRÉDITOS INSUFICIENTES!");
                // Se estiver em modo automático, parar
                if (isAutoSpinning) {
                    stopAutoSpin();
                }
                return;
            }

            // Deduzir o custo da rodada
            credits -= currentBet;
            creditLabel.setText("CRÉDITOS: " + credits);
        }

        winLabel.setText("GIRANDO...");
        winLabel.setTextFill(Color.WHITE);

        // Limpar multiplicadores ativos
        activeMultipliers.clear();

        // Desabilitar o botão durante a rotação
        spinButton.setDisable(true);
        autoSpinButton.setDisable(true);
        buyBonusButton.setDisable(true);

        // Girar os rolos com animação
        for (int i = 0; i < NUM_REELS; i++) {
            spinning[i] = true;
            final int reelIndex = i;

            // Animação de rotação
            Timeline spinAnimation = new Timeline(
                    new KeyFrame(Duration.millis(100), e -> {
                        for (int row = 0; row < NUM_ROWS; row++) {
                            int randomSymbol = getWeightedRandomSymbol();
                            reels[reelIndex][row].setImage(symbols[randomSymbol]);
                        }
                    }));

            // Duração diferente para cada rolo para efeito cascata
            spinAnimation.setCycleCount(20 + (i * 2));
            spinAnimation.setOnFinished(e -> {
                spinning[reelIndex] = false;
                checkAllStopped();
            });

            // Atraso progressivo para criar efeito cascata
            PauseTransition delay = new PauseTransition(Duration.millis(i * 200));
            delay.setOnFinished(e -> spinAnimation.play());
            delay.play();
        }
    }

    private int getWeightedRandomSymbol() {
        // Gerar símbolos com probabilidades diferentes
        double rand = Math.random();
        double cumulativeProbability = 0.0;

        for (int i = 0; i < symbolProbabilities.length; i++) {
            cumulativeProbability += symbolProbabilities[i];
            if (rand <= cumulativeProbability) {
                return i;
            }
        }

        // Fallback - retornar o primeiro símbolo
        return 0;
    }

    private void checkAllStopped() {
        // Verificar se todos os rolos pararam
        for (boolean isSpinning : spinning) {
            if (isSpinning)
                return;
        }

        // Todos pararam, verificar resultado
        checkResult();
    }

    private void checkResult() {
        // Determinar qual símbolo está em cada posição
        int[][] results = new int[NUM_REELS][NUM_ROWS];
        int scatterCount = 0;

        for (int col = 0; col < NUM_REELS; col++) {
            for (int row = 0; row < NUM_ROWS; row++) {
                results[col][row] = getWeightedRandomSymbol();
                reels[col][row].setImage(symbols[results[col][row]]);

                // Verificar se é multiplicador
                if (results[col][row] >= 10) {
                    double multiplierValue = getMultiplierValue(results[col][row]);
                    activeMultipliers.add(multiplierValue);
                    createMultiplierGlow(col, row);
                }

                // Contar scatters
                if (results[col][row] == 9) { // Scatter é o índice 9
                    scatterCount++;
                }
            }
        }

        // Verificar scatters para rodadas grátis
        if (scatterCount >= 3) {
            int awardedSpins = 8 + (scatterCount - 3) * 2;
            freeSpins += awardedSpins;
            freeSpinsLabel.setText("RODADAS GRÁTIS: " + freeSpins);
            winLabel.setText(scatterCount + " SCATTERS! " + awardedSpins + " RODADAS GRÁTIS!");
            winLabel.setTextFill(Color.LIGHTBLUE);

            // Efeito visual especial para scatters
            highlightScatters(results);

            // Animação de relâmpago
            createLightningEffect();

            enableButtonsAfterSpin();
            return;
        }

        // Verificar combinações vencedoras
        List<int[]> winningPositions = new ArrayList<>();
        winAmount = 0;
        boolean hasWin = false;

        // Verificar linhas horizontais
        for (int row = 0; row < NUM_ROWS; row++) {
            int firstSymbol = results[0][row];
            if (firstSymbol >= 10)
                continue; // Ignorar multiplicadores
            if (firstSymbol == 9)
                continue; // Ignorar scatters

            int count = 1;
            for (int col = 1; col < NUM_REELS; col++) {
                if (results[col][row] == firstSymbol) {
                    count++;
                } else {
                    break;
                }
            }

            if (count >= 3) {
                // Calcular ganho base
                int baseWin = symbolValues[firstSymbol] * count;

                // Aplicar multiplicadores ativos
                double totalMultiplier = 1.0;
                for (double mult : activeMultipliers) {
                    totalMultiplier *= mult;
                }

                int win = (int) (baseWin * totalMultiplier);
                winAmount += win;
                hasWin = true;

                // Guardar posições vencedoras para efeito cascata
                for (int col = 0; col < count; col++) {
                    winningPositions.add(new int[] { col, row });
                }
            }
        }

        if (hasWin) {
            // Aplicar efeito cascata nas posições vencedoras
            applyAvalancheEffect(winningPositions, results);
        } else {
            // Sem ganhos, finalizar rodada
            credits += winAmount;
            winLabel.setText("TENTE NOVAMENTE!");
            winLabel.setTextFill(Color.WHITE);
            creditLabel.setText("CRÉDITOS: " + credits);
            enableButtonsAfterSpin();
        }
    }

    private void enableButtonsAfterSpin() {
        spinButton.setDisable(false);
        autoSpinButton.setDisable(false);
        buyBonusButton.setDisable(false);

        // Se estiver em modo automático, verificar se deve continuar
        if (isAutoSpinning) {
            autoSpinsRemaining--;

            if (autoSpinsRemaining > 0) {
                // Esperar um pouco antes do próximo giro automático
                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(e -> spinReels());
                delay.play();
            } else {
                // Parar o modo automático
                stopAutoSpin();
            }
        }
    }

    private void applyAvalancheEffect(List<int[]> positions, int[][] results) {
        // Animação de explosão para símbolos vencedores
        for (int[] pos : positions) {
            int col = pos[0];
            int row = pos[1];
            createExplosionEffect(col, row);
        }

        // Aguardar animação de explosão e depois aplicar efeito cascata
        PauseTransition pause = new PauseTransition(Duration.seconds(0.7));
        pause.setOnFinished(e -> {
            // Fazer símbolos caírem
            cascadeSymbols(positions, results);
        });
        pause.play();
    }

    private void cascadeSymbols(List<int[]> positions, int[][] originalResults) {
        // Para cada coluna, fazer os símbolos caírem
        for (int col = 0; col < NUM_REELS; col++) {
            // Encontrar posições vazias (que explodiram) nesta coluna
            List<Integer> emptyRows = new ArrayList<>();
            for (int[] pos : positions) {
                if (pos[0] == col) {
                    emptyRows.add(pos[1]);
                }
            }

            if (!emptyRows.isEmpty()) {
                // Ordenar linhas vazias
                Collections.sort(emptyRows);

                // Para cada linha vazia, fazer símbolos de cima caírem
                for (int emptyRow : emptyRows) {
                    for (int row = emptyRow; row > 0; row--) {
                        int newSymbol = originalResults[col][row - 1];
                        reels[col][row].setImage(symbols[newSymbol]);
                    }

                    // Preencher o topo com novo símbolo
                    int newTopSymbol = getWeightedRandomSymbol();
                    reels[col][0].setImage(symbols[newTopSymbol]);

                    // Animação de queda
                    createFallAnimation(col, emptyRow);
                }
            }
        }

        // Aguardar animação de queda e finalizar rodada
        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
        pause.setOnFinished(e -> {
            credits += winAmount;
            winLabel.setText("VITÓRIA! " + winAmount + " créditos");
            winLabel.setTextFill(Color.GOLD);
            creditLabel.setText("CRÉDITOS: " + credits);
            spinButton.setDisable(false);
        });
        pause.play();
    }

    private void createFallAnimation(int col, int row) {
        ImageView symbol = reels[col][row];

        // Salvar posição original
        double originalY = symbol.getLayoutY();

        // Mover símbolo para cima (fora da tela)
        symbol.setLayoutY(originalY - 80);

        // Animação de queda
        Timeline fall = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(symbol.layoutYProperty(), originalY)));
        fall.play();
    }

    private double getMultiplierValue(int symbolIndex) {
        switch (symbolIndex) {
            case 10:
                return 2.0;
            case 11:
                return 5.0;
            case 12:
                return 10.0;
            case 13:
                return 100.0;
            default:
                return 1.0;
        }
    }

    private void createMultiplierGlow(int col, int row) {
        Circle glow = new Circle(35);
        glow.setFill(Color.TRANSPARENT);

        // Cor diferente para cada multiplicador
        if (row % 4 == 0)
            glow.setStroke(Color.GREEN);
        else if (row % 4 == 1)
            glow.setStroke(Color.ORANGE);
        else if (row % 4 == 2)
            glow.setStroke(Color.PURPLE);
        else
            glow.setStroke(Color.RED);

        glow.setStrokeWidth(3);
        glow.setOpacity(0);

        reelContainers[col][row].getChildren().add(glow);

        // Animação de pulsação e rotação
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.opacityProperty(), 0),
                        new KeyValue(glow.rotateProperty(), 0)),
                new KeyFrame(Duration.seconds(0.5),
                        new KeyValue(glow.opacityProperty(), 0.8),
                        new KeyValue(glow.rotateProperty(), 180)),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(glow.opacityProperty(), 0),
                        new KeyValue(glow.rotateProperty(), 360)));
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    private void highlightScatters(int[][] results) {
        // Destacar símbolos scatter com efeito visual
        for (int col = 0; col < NUM_REELS; col++) {
            for (int row = 0; row < NUM_ROWS; row++) {
                if (results[col][row] == 9) { // Scatter é o índice 9
                    // Adicionar efeito de brilho
                    createGlowEffect(col, row);
                }
            }
        }
    }

    private void createGlowEffect(int col, int row) {
        Circle glow = new Circle(35);
        glow.setFill(Color.TRANSPARENT);
        glow.setStroke(Color.LIGHTBLUE);
        glow.setStrokeWidth(3);
        glow.setOpacity(0);

        reelContainers[col][row].getChildren().add(glow);

        // Animação de pulsação
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.opacityProperty(), 0)),
                new KeyFrame(Duration.seconds(0.5), new KeyValue(glow.opacityProperty(), 0.8)),
                new KeyFrame(Duration.seconds(1), new KeyValue(glow.opacityProperty(), 0)));
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    private void createLightningEffect() {
        // Criar efeito de relâmpago na tela
        Polyline lightning = new Polyline();
        lightning.getPoints().addAll(
                100.0, 50.0,
                50.0, 100.0,
                150.0, 150.0,
                100.0, 200.0,
                200.0, 250.0,
                150.0, 300.0,
                250.0, 350.0,
                200.0, 400.0,
                300.0, 450.0);
        lightning.setStroke(Color.YELLOW);
        lightning.setStrokeWidth(3);
        lightning.setOpacity(0);

        // Adicionar ao painel raiz
        if (rootPane instanceof Pane) {
            ((Pane) rootPane).getChildren().add(lightning);
        }

        // Animação do relâmpago
        Timeline flash = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(lightning.opacityProperty(), 0)),
                new KeyFrame(Duration.seconds(0.1), new KeyValue(lightning.opacityProperty(), 1)),
                new KeyFrame(Duration.seconds(0.2), new KeyValue(lightning.opacityProperty(), 0)),
                new KeyFrame(Duration.seconds(0.3), new KeyValue(lightning.opacityProperty(), 1)),
                new KeyFrame(Duration.seconds(0.4), new KeyValue(lightning.opacityProperty(), 0)));
        flash.setOnFinished(e -> {
            if (rootPane instanceof Pane) {
                ((Pane) rootPane).getChildren().remove(lightning);
            }
        });
        flash.play();
    }

    private void createExplosionEffect(int col, int row) {
        Circle explosion = new Circle(10);
        explosion.setFill(Color.TRANSPARENT);
        explosion.setStroke(Color.GOLD);
        explosion.setStrokeWidth(2);
        explosion.setOpacity(0);

        StackPane container = reelContainers[col][row];
        container.getChildren().add(explosion);

        // Criar partículas de explosão
        for (int i = 0; i < 8; i++) {
            createParticle(container, col, row, i * 45);
        }

        // Animação de explosão
        Timeline explode = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(explosion.opacityProperty(), 0),
                        new KeyValue(explosion.radiusProperty(), 10)),
                new KeyFrame(Duration.seconds(0.3),
                        new KeyValue(explosion.opacityProperty(), 1),
                        new KeyValue(explosion.radiusProperty(), 30)),
                new KeyFrame(Duration.seconds(0.6),
                        new KeyValue(explosion.opacityProperty(), 0),
                        new KeyValue(explosion.radiusProperty(), 40)));
        explode.setOnFinished(e -> container.getChildren().remove(explosion));
        explode.play();
    }

    private void createParticle(StackPane container, int col, int row, double angle) {
        Circle particle = new Circle(3);
        particle.setFill(Color.GOLD);
        particle.setOpacity(0);

        container.getChildren().add(particle);

        // Animação da partícula
        Timeline particleAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(particle.opacityProperty(), 0),
                        new KeyValue(particle.translateXProperty(), 0),
                        new KeyValue(particle.translateYProperty(), 0)),
                new KeyFrame(Duration.seconds(0.2),
                        new KeyValue(particle.opacityProperty(), 1),
                        new KeyValue(particle.translateXProperty(), Math.cos(Math.toRadians(angle)) * 20),
                        new KeyValue(particle.translateYProperty(), Math.sin(Math.toRadians(angle)) * 20)),
                new KeyFrame(Duration.seconds(0.5),
                        new KeyValue(particle.opacityProperty(), 0),
                        new KeyValue(particle.translateXProperty(), Math.cos(Math.toRadians(angle)) * 40),
                        new KeyValue(particle.translateYProperty(), Math.sin(Math.toRadians(angle)) * 40)));
        particleAnim.setOnFinished(e -> container.getChildren().remove(particle));
        particleAnim.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}