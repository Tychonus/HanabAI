package agents;

import hanabAI.*;

/**
 * A rule based agent to play the game Hanabi, based on the Van Den Bergh
 * rulings Follows the given ruleset: - If lives > 1, and the certainty of a
 * card is >= 0.6, play it, else play a card that is known to be right - If we
 * are certain a card is useless, discard it - Give a hint on the next useful
 * card in sight - Discard the card most likely to be worthless
 * 
 * @author Brad Milner
 **/
public class BradAgent implements Agent {

    private Colour[] colours;
    private int[] values;
    private boolean firstAction = true;
    private int numPlayers;
    private int index;

    /**
     * Default constructor, does nothing.
     **/
    public BradAgent() {
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
     * Returns the name Bradical.
     * 
     * @return the String "Bradical"
     */
    public String toString() {
        return "Bradical";
    }

    /**
     * Performs an action given a state, following the rules outlined in class
     * comment
     * 
     * @param s the current state of the game.
     * @return the action the player takes.
     **/
    public Action doAction(State s) {
        if (firstAction) {
            init(s);
        }
        // Assume players index is sgetNextPlayer()
        index = s.getNextPlayer();
        // get any hints
        try {
            // plays actions in rule sequence
            getHints(s);
            // if(a==null) a = playProbablySafe(s); TODO IMPLEMENT BEFORE playKnown
            Action a = playKnown(s);
            if (a == null)
                a = discardKnown(s);
            // if(a==null) a = nextUsefulHint(s);
            // if(a==null) a = discardWorst(s);

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
    public Action playKnown(State s) throws IllegalActionException {
        for (int i = 0; i < colours.length; i++) {
            if (colours[i] != null && values[i] == playable(s, colours[i])) {
                colours[i] = null;
                values[i] = 0;
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

    // Gives the next player a hint based on most useful card
    // Hint is useul IF:
    // - It is about a card that is playable
    // - By revealing information about said card, can reveal info on other playable
    // cards
    // - If no useful hints can be made, move to next player
    public Action nextUsefUlHint(State s) throws IllegalActionException {
        if (s.getHintTokens() > 0) {

        }
        return null;
    }

}
