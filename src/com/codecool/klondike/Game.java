package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Game extends Pane {

    private List<Card> deck;

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;

    public Game() {
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
    }

    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        card = card.getContainingPile().getTopCard();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            if (card.isFaceDown()) {
                card.flip();
            }
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();

        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();
        if (activePile.getPileType() == Pile.PileType.STOCK) {
            return;
        } else if (activePile.getPileType() == Pile.PileType.TABLEAU && !card.isFaceDown()) {

            draggedCards.addAll(activePile.getCards());
            for (Card activePileCard : activePile.getCards()) {
                if (activePileCard.equals(card)) {
                    break;
                }
                draggedCards.remove(activePileCard);
            }
        } else if (!card.isFaceDown()) {
            draggedCards.add(card);
        } else {
            return;
        }

        card.getDropShadow().setRadius(20);
        card.getDropShadow().setOffsetX(10);
        card.getDropShadow().setOffsetY(10);

        card.toFront();
        card.setTranslateX(offsetX);
        card.setTranslateY(offsetY);
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        List<Pile> bothPiles = new ArrayList<>(tableauPiles);
        bothPiles.addAll(foundationPiles);
        Pile pile = getValidIntersectingPile(card, bothPiles);
        //TODO
        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            //draggedCards = null;
        }
        if (isGameWon()) showModal("Congratulations!");
    };

    public boolean isGameWon() {
        for (Pile pile: foundationPiles)
            if (pile.numOfCards() != 13) return false;
        return true;
    }

    private void showModal(String msg) {
        final Stage dialog = new Stage();
        Text text = new Text(msg);
        Scene dialogScene;

        dialog.initModality(Modality.APPLICATION_MODAL);
        VBox dialogVbox = new VBox(20);
        text.setStyle("-fx-font: 24 arial;");
        dialogVbox.getChildren().add(text);
        dialogScene = new Scene(dialogVbox, 250, 50);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        stockPile.clear();
        Collections.reverse(discardPile.getCards());
        for (Card card : discardPile.getCards()) {
            stockPile.addCard(card);
            if (!card.isFaceDown()) {
                card.flip();
            }
        }
        discardPile.clear();
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.getPileType().equals(Pile.PileType.TABLEAU)) {
            if (destPile.isEmpty()) {
                return card.getRank() == 13;
            } else {
                return (Card.isOppositeColor(card, destPile.getTopCard()) &&
                        Card.isHigherRank(destPile.getTopCard(), card));
            }
        } else if (destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (destPile.isEmpty()) {
                return card.getRank() == 1;
            } else {
                return (Card.isSameSuit(card, destPile.getTopCard()) &&
                        Card.isHigherRank(card, destPile.getTopCard()));
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = card.getContainingPile();
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }

    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        Button restartBtn = new Button("Restart");
        restartBtn.setStyle("-fx-font: 18 arial; -fx-base: #666666;");
        getChildren().add(restartBtn);
        restartBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                restart();
            }
        });


        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        Iterator<Card> deckIterator = deck.iterator();
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        int numberOfPile = 0;
        int numberOfCard = 0;
        int startPile = 0;
        int countOfCardsToDeal = 27;
        int countOfPiles = 7;

        for (Card card : deck) {
            if (numberOfCard > countOfCardsToDeal) {
                stockPile.addCard(card);
                addActionToCard(card);
            } else {
                tableauPiles.get(numberOfPile).addCard(card);
                addActionToCard(card);
                numberOfPile++;

                if (numberOfPile == countOfPiles) {
                    startPile++;
                    numberOfPile = startPile;
                }
            }

            if (numberOfPile == startPile + 1 || numberOfCard == countOfCardsToDeal)
                card.flip();

            numberOfCard++;
        }
    }

    private Card addActionToCard(Card card) {
        addMouseEventHandlers(card);
        getChildren().add(card);
        return card;
    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    private void restart() {
        clearPane();
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
    }

    private void clearPane() {
        stockPile.clear();
        discardPile.clear();
        foundationPiles.clear();
        tableauPiles.clear();
        this.getChildren().clear();
    }
}
