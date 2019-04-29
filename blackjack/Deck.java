package blackjack;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class Deck {
/**
* @author mlg932
* Date Created: 8/28/2018
* Last Updated: 9/11/2018
*/
	
/*****Constants*****/
	private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
	private static final String[] SUITSYMBOLS = {"\u2665", "\u2666", "\u2660", "\u2663"};
	private static final int CARDWIDTH = 30;
	private static final int CARDHEIGHT = 50;
	
	private static final int [] VALUES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
	private static final String FILENAME = "cards.png";
	
	private ArrayList<Card> cards = new ArrayList<Card>();

/*****Get Methods*****/
	public static String[] getRanks() {
		return RANKS;
	}
	
	public static String[] getSuitSymbols() {
		return SUITSYMBOLS;
	}
	
	public static int getCardWidth() {
		return CARDWIDTH;
	}
	
	public static int getCardHeight() {
		return CARDHEIGHT;
	}
	
	//Constructor
	public Deck() {
		
		Random rand = new Random(); //creates random 
		
		try {
			InputStream input = getClass().getResourceAsStream(FILENAME);
			BufferedImage cardsImg = ImageIO.read(input);
			
			for(int suit = 0; suit < SUITSYMBOLS.length; suit++) {
				for(int rank = 0; rank < RANKS.length; rank++) {
					
					int pos = 0;
					if(cards.size() > 0) {
						pos = rand.nextInt(cards.size() + 1);
					}
					
					int x = rank*CARDWIDTH;
					int y = suit*CARDHEIGHT;
					Image img = cardsImg.getSubimage(x, y, CARDWIDTH, CARDHEIGHT);
					Card card = new Card(RANKS[rank], suit, VALUES[rank], img);
					
					cards.add(pos, card);
				} //end for rank
			} //end for suit
		} //end try 
		catch (IOException e) {
			String message = "Could not open image file" + FILENAME;
			JOptionPane.showMessageDialog(null, message);
		}
	} //end constructor
	
	public Card deal() {
		Card card = cards.remove(0);
		return card;
	} //end deal
	
	public int size() {
		return cards.size();
	}//end size
	
}
