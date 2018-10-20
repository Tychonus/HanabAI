package agents;

import hanabAI.*;
import java.util.Arrays;
import java.util.Stack;

/**
 * A Rule based AI agent, with the following rules outlined by the Piers
 * Strategy 1. If more than one life is left, and the deck has run out of cards,
 * Play a safe card, otherwise a random card if there is no safe cards 2. Play a
 * safe card 3. Tell any player about a useful card 4. If there are less than 4
 * hint tokens remaining, tell a player about a definitely unplayable card 5.
 * Discard a card known to be useless 6. Give a random player a random hint 7.
 * Discard a random card Created via a modified BasicAgent by Tim French
 * (https://github.com/drtnf/HanabAI)
 * 
 * @author Brad Milner
 **/
public class BradicalAgent implements Agent {

    private Colour[] colours;
    private int[] values;
    private boolean firstAction = true;
    private int numPlayers;
    private int index;

    // Used for discardOldest
    int[] playOrder = new int[4];

    /**
     * Default constructor, does nothing.
     **/
    public BradicalAgent() {
    }

    /**
     * Initialises variables on the first call to do action.
     * 
     * @param s the State of the game at the first action
     **/
    public void init(State s) {
        numPlayers = s.getPlayers().length;
        if (numPlayers > 3) {
            colours = new Colour[4];
            values = new int[4];
        } else {
            colours = new Colour[5];
            values = new int[5];
        }
        index = s.getNextPlayer();
        firstAction = false;
    }

    /**
     * Returns the name BradicalAgent.
     * 
     * @return the String "BradicalAgent"
     */
    public String toString() {
        return "BradicalAgent";
    }

    /**
     * Performs an action given a state. Assumes that they are the player to move.
     * 
     * @param s the current state of the game.
     * @return the action the player takes.
     **/
    public Action doAction(State s) {

        if (firstAction) {
            init(s);
            for (int j = 0; j < 4; j++) {
                playOrder[j] = j;
            }
        }
        // Assume players index is sgetNextPlayer()
        index = s.getNextPlayer();
        // get any hints
        try {
            getHints(s);
            Action a = null;
            // A semi safe "hail mary" if the game draws to a close
            if (a == null && s.getFuseTokens() > 1 && s.getFinalActionIndex() != -1) {
                System.out.println(Arrays.toString(playOrder));
                a = playKnown(s, playOrder);
                if (a == null) {
                    a = playGuess(s, playOrder);
                }
            }
            if (a == null) {
                a = playKnown(s, playOrder);
            }
            if (a == null) {
                a = tellAnyoneAboutUseful(s);
            }
            // Tries to provide smarter hints once tokens become scarce
            if (a == null && s.getHintTokens() < 4) {
                a = tellDispensible(s);
            }
            if (a == null)
                a = discardKnown(s);
            if (a == null)
                a = hintRandom(s);
            // Discards oldest card
            if (a == null && s.getFinalActionIndex() == -1)
                a = discardOldest(s);
            if (a == null)
                a = discardGuess(s);
            return a;

        } catch (IllegalActionException e) {
            e.printStackTrace();
            throw new RuntimeException("Something has gone very wrong");
        }
    }

    // updates colours and values from hints received
    public void getHints(State s) {
        try {
            State t = (State) s.clone();
            for (int i = 0; i < Math.min(numPlayers - 1, s.getOrder()); i++) {
                Action a = t.getPreviousAction();
                if ((a.getType() == ActionType.HINT_COLOUR || a.getType() == ActionType.HINT_VALUE)
                        && a.getHintReceiver() == index) {
                    boolean[] hints = t.getPreviousAction().getHintedCards();
                    for (int j = 0; j < hints.length; j++) {
                        if (hints[j]) {
                            if (a.getType() == ActionType.HINT_COLOUR)
                                colours[j] = a.getColour();
                            else
                                values[j] = a.getValue();
                        }
                    }
                }
                t = t.getPreviousState();
            }
        } catch (IllegalActionException e) {
            e.printStackTrace();
        }
    }

    // returns the value of the next playable card of the given colour
    public int playable(State s, Colour c) {
        java.util.Stack<Card> fw = s.getFirework(c);
        if (fw.size() == 5)
            return -1;
        else
            return fw.size() + 1;
    }

    // plays the first card known to be playable.
    public Action playKnown(State s, int[] playOrder) throws IllegalActionException {
        for (int i = 0; i < colours.length; i++) {
            if (colours[i] != null && values[i] == playable(s, colours[i])) {
                colours[i] = null;
                values[i] = 0;

                // Updates array for finding oldest card in hand
                int j = 1;
                int temp = playOrder[0];
                playOrder[0] = i;

                while (j < i) {
                    int newTemp = playOrder[j];
                    playOrder[j] = temp;
                    temp = newTemp;
                    j++;
                }

                return new Action(index, toString(), ActionType.PLAY, i);
            }
        }
        return null;
    }

    // discards the first card known to be unplayable.
    public Action discardKnown(State s) throws IllegalActionException {
        if (s.getHintTokens() != 8) {
            for (int i = 0; i < colours.length; i++) {
                if (colours[i] != null && values[i] > 0 && values[i] < playable(s, colours[i])) {
                    colours[i] = null;
                    values[i] = 0;
                    return new Action(index, toString(), ActionType.DISCARD, i);
                }
            }
        }
        return null;
    }

    /**
     * Discards the card which is held in the players hand the longest
     * 
     * @param s the state of the game
     * @return the action of discarding the card
     * @throws IllegalActionException
     */
    public Action discardOldest(State s) throws IllegalActionException {
        if (s.getHintTokens() != 8) {
            return new Action(index, toString(), ActionType.DISCARD, playOrder[0]);
        }
        return null;
    }

    /**
     * Starting from next player, cycles through all players, tells a hint regarding
     * a card that can be discarded
     * 
     * @param s the current state of the game
     * @return the action of providing the hint
     * @throws IllegalActionException
     */
    public Action tellDispensible(State s) throws IllegalActionException {
        if (s.getHintTokens() > 0) {
            for (int i = 1; i < numPlayers; i++) {
                // Makes next player the hintee
                int hintee = (index + 1) % numPlayers;
                Card[] hand = s.getHand(hintee);

                // Finds cards which can be discarded
                for (int j = 0; j < hand.length; j++) {
                    Card c = hand[j];

                    // If a firework is completed, return a colour hint
                    if (c != null && c.getValue() != playable(s, c.getColour())
                            && s.getFirework(c.getColour()) != null) {
                        Stack<Card> firework = s.getFirework(c.getColour());
                        if (firework.size() == 5) {
                            boolean[] col = new boolean[hand.length];
                            for (int k = 0; k < col.length; k++) {
                                col[k] = c.getColour().equals((hand[k] == null ? null : hand[k].getColour()));
                            }
                            return new Action(index, toString(), ActionType.HINT_COLOUR, hintee, col, c.getColour());
                        }
                    }
                    // If card can be discarded, return action of hint of value
                    if (c != null && c.getValue() != playable(s, c.getColour())) {
                        boolean[] val = new boolean[hand.length];
                        for (int k = 0; k < val.length; k++) {
                            val[k] = c.getValue() == (hand[k] == null ? -1 : hand[k].getValue());
                        }
                        return new Action(index, toString(), ActionType.HINT_VALUE, hintee, val, c.getValue());
                    }

                }
            }
        }

        return null;

    }

    // gives hint of a playable card in a players hand
    // flips a coin to determine whether it is a colour hint or value hint
    // return null if no hint token left, or no playable cards
    public Action tellAnyoneAboutUseful(State s) throws IllegalActionException {
        if (s.getHintTokens() > 0) {
            for (int i = 1; i < numPlayers; i++) {
                int hintee = (index + 1) % numPlayers;
                Card[] hand = s.getHand(hintee);

                // Random if hint cycle starts from beginning or end of hand
                if (Math.random() > 0.5) {
                    for (int j = 0; j < hand.length; j++) {
                        Card c = hand[j];
                        // If card is playable
                        if (c != null && c.getValue() == playable(s, c.getColour())) {

                            if (Math.random() > 0.5) {
                                // Gives hint on colour
                                boolean[] col = new boolean[hand.length];
                                for (int k = 0; k < col.length; k++) {
                                    col[k] = c.getColour().equals((hand[k] == null ? null : hand[k].getColour()));
                                }
                                return new Action(index, toString(), ActionType.HINT_COLOUR, hintee, col,
                                        c.getColour());
                            }
                            // Gives hint on value
                            else {
                                boolean[] val = new boolean[hand.length];
                                for (int k = 0; k < val.length; k++) {
                                    val[k] = c.getValue() == (hand[k] == null ? -1 : hand[k].getValue());
                                }
                                return new Action(index, toString(), ActionType.HINT_VALUE, hintee, val, c.getValue());
                            }
                        }
                    }
                } else {
                    for (int j = 0; j < hand.length; j++) {
                        Card c = hand[j];
                        if (c != null && c.getValue() == playable(s, c.getColour())) {

                            if (Math.random() > 0.5) {

                                boolean[] col = new boolean[hand.length];
                                for (int k = 0; k < col.length; k++) {
                                    col[k] = c.getColour().equals((hand[k] == null ? null : hand[k].getColour()));
                                }
                                return new Action(index, toString(), ActionType.HINT_COLOUR, hintee, col,
                                        c.getColour());
                            } else {
                                boolean[] val = new boolean[hand.length];
                                for (int k = 0; k < val.length; k++) {
                                    val[k] = c.getValue() == (hand[k] == null ? -1 : hand[k].getValue());
                                }
                                return new Action(index, toString(), ActionType.HINT_VALUE, hintee, val, c.getValue());
                            }
                        }
                    }
                }
            }
        }
        return null;

    }

    // with probability 0.05 for each fuse token, play a random card
    public Action playGuess(State s, int[] playOrder) throws IllegalActionException {
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < s.getFuseTokens(); i++) {
            if (rand.nextDouble() < 0.05) {
                int cardIndex = rand.nextInt(colours.length);
                colours[cardIndex] = null;
                values[cardIndex] = 0;

                // Updates array for finding oldest card in hand
                int j = 1;
                int temp = playOrder[0];
                playOrder[0] = i;

                while (j < i) {
                    int newTemp = playOrder[j];
                    playOrder[j] = temp;
                    temp = newTemp;
                    j++;
                }

                return new Action(index, toString(), ActionType.PLAY, cardIndex);
            }
        }
        return null;
    }

    // discard a random card
    public Action discardGuess(State s) throws IllegalActionException {
        if (s.getHintTokens() != 8) {
            java.util.Random rand = new java.util.Random();
            int cardIndex = rand.nextInt(colours.length);
            colours[cardIndex] = null;
            values[cardIndex] = 0;
            return new Action(index, toString(), ActionType.DISCARD, cardIndex);
        }
        return null;
    }

    // gives random hint of a card in next players hand
    // flips a coin to determine whether it is a colour hint or value hint
    // return null if no hint token left
    public Action hintRandom(State s) throws IllegalActionException {
        if (s.getHintTokens() > 0) {
            int hintee = (index + 1) % numPlayers;
            Card[] hand = s.getHand(hintee);

            java.util.Random rand = new java.util.Random();
            int cardIndex = rand.nextInt(hand.length);
            while (hand[cardIndex] == null)
                cardIndex = rand.nextInt(hand.length);
            Card c = hand[cardIndex];

            if (Math.random() > 0.5) {// give colour hint
                boolean[] col = new boolean[hand.length];
                for (int k = 0; k < col.length; k++) {
                    col[k] = c.getColour().equals((hand[k] == null ? null : hand[k].getColour()));
                }
                return new Action(index, toString(), ActionType.HINT_COLOUR, hintee, col, c.getColour());
            } else {// give value hint
                boolean[] val = new boolean[hand.length];
                for (int k = 0; k < val.length; k++) {
                    if (hand[k] == null)
                        continue;
                    val[k] = c.getValue() == (hand[k] == null ? -1 : hand[k].getValue());
                }
                return new Action(index, toString(), ActionType.HINT_VALUE, hintee, val, c.getValue());
            }

        }

        return null;
    }

}
